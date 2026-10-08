package com.regnosys.rosetta.ir.emit.python;

import com.regnosys.rosetta.ir.core.Cardinality;
import com.regnosys.rosetta.ir.core.IREnum;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.emit.EmitterException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Emits the <strong>declaration</strong> ("noun") side of the neutral IR to <strong>Python</strong>: a Rune
 * STRUCT becomes an instantiable <strong>{@code @dataclass}</strong> data object (its auto-generated
 * {@code __init__} is the constructor), a Rune ENUM becomes a Python {@code Enum}, a Rune CHOICE
 * becomes a Python {@code Union} type alias. The companion to the step-4 expression ("verb") emitter
 * {@code IRPythonEmitter}; together they give the Python target both halves. Verified by structural
 * equivalence (no functional-equivalence harness needed for shapes).
 *
 * <p><strong>Identifier policy.</strong> A <strong>struct field</strong> name (a value-level identifier) is first
 * converted to Pythonic <strong>{@code snake_case}</strong> ({@code dayCountFraction → day_count_fraction}) and then
 * keyword-sanitized (see {@link PythonIdentifiers#snakeName}); <strong>enum members</strong> and <strong>type / class /
 * base names</strong> keep their source casing and are keyword-sanitized only ({@link PythonIdentifiers#sanitize} /
 * {@link PythonIdentifiers#safeName}). Keyword sanitization appends a trailing underscore to any of the 35 hard Python
 * keywords ({@code global → global_} for a field, {@code None → None_} for an enum member — the PEP-8 idiom; because
 * snake_case lowercases first, a capitalized-keyword field name such as {@code None} normalizes to the non-keyword
 * {@code none}, so it needs no underscore). Any other Python-unsafe name — non-ASCII, blank, or not matching
 * {@code [A-Za-z_][A-Za-z0-9_]*} — still <strong>declines</strong> the whole declaration via {@link EmitterException}
 * (never a {@code SyntaxError} module), as do a <strong>non-noun kind</strong> and a 0-option CHOICE. If two sibling
 * field / enum-member identifiers collide after this normalization — from keyword sanitization ({@code global} and a
 * literal {@code global_} together), from snake_casing two distinct field names onto one ({@code fooBar} and
 * {@code foo_bar} both → {@code foo_bar}), or from two identical inputs — the declaration declines rather than silently
 * drop a member (choice options are exempt — a {@code Union} dedups).
 * An ENUM member whose (sanitized) identifier has an {@code enum.Enum}-reserved <em>sunder</em> ({@code _x_}) or
 * <em>dunder</em> ({@code __x__}) shape also declines (see {@link PythonIdentifiers#isEnumReserved}) — such a
 * member would crash or be silently dropped by the {@code Enum} metaclass; sunder is enum-specific (struct
 * fields / class names with sunder shape still emit). A <strong>dunder-shaped</strong> ({@code __x__}) struct
 * field name, class name, or base name also declines (see {@link PythonIdentifiers#isDunder}): a {@code @dataclass}
 * field named {@code __init__} or {@code __eq__} conflicts with the auto-generated constructor/equality methods,
 * causing {@code TypeError} at instantiation, so the declaration emitter declines those rather than emit a
 * broken class.
 *
 * <p><strong>Data object, not validator.</strong> The {@code @dataclass} is a pure <em>data object</em> — it
 * enforces no constraints. Required-ness, cardinality, type-format, and per-condition rules are a future
 * <em>separate</em> validator layer, mirroring Rune's Java codegen (which splits the nullable data POJO from
 * its validator classes so an object can be constructed from external JSON and validated separately). To that
 * end <strong>every field defaults to {@code = None}</strong> — scalars and collections alike. This removes
 * the {@code @dataclass} "non-default argument follows default argument" ordering hazard (Rune freely
 * interleaves required and optional fields) and keeps the class body free of any executable call. We
 * deliberately do <em>not</em> emit {@code field(default_factory=list)} for collections: that would place
 * {@code field} / {@code list} references in the class body where a same-named Rune field would shadow them
 * (neither is a Python keyword, so {@link PythonIdentifiers#sanitize} passes them through), and a bare
 * {@code = []} is itself a {@code @dataclass} {@code ValueError}. An unset collection is therefore
 * {@code None}, not {@code []}. A non-{@code Optional} field defaulted to {@code None} (a required scalar
 * {@code bar: str = None}, every {@code legs: List[T] = None}) is runtime-clean (PEP 563 lazy annotation) but is
 * flagged by a static type-checker by default; full type-strictness is the future validator / {@code py.typed}
 * layer's concern, consistent with the data-object-not-validator split.
 *
 * <p><strong>Precondition.</strong> {@link #emit} expects a top-level declaration node (a real {@code IRTypeNode}
 * with fields / {@code IREnumNode} with values), not a field-type reference stub; a stub STRUCT would emit an
 * empty class.
 */
public final class IRPythonDeclarationEmitter {

    /**
     * The module-level imports the emitted declarations assume. {@code from __future__ import annotations}
     * (PEP 563, which MUST be the module's first statement) makes field annotations lazy strings, so a class
     * whose field references a not-yet-defined user type still imports. {@code from dataclasses import dataclass}
     * is bundled so an emitted {@code @dataclass} struct resolves its decorator without a per-caller import. The
     * constant intentionally bundles the full declaration import set (unlike the minimal expression
     * {@code MODULE_PREAMBLE}); a module emitter would compute a minimal per-module set and hoist the single
     * future import to line 1.
     */
    public static final String DECL_PREAMBLE =
            "from __future__ import annotations\n"
            + "from dataclasses import dataclass\n"
            + "from enum import Enum\n"
            + "from typing import List, Optional, Union\n"
            + "from decimal import Decimal\n"
            + "from datetime import date, datetime, time";

    /** Rune built-in type spelling → Python type. Keyed on name (the adapter defaults a basic type's kind to STRUCT). */
    private static final Map<String, String> BASIC_TYPES = Map.of(
            "boolean", "bool",
            "string", "str",
            "number", "Decimal",
            "int", "int",
            "date", "date",
            "dateTime", "datetime",
            "zonedDateTime", "datetime",
            "time", "time");

    /** Body of an empty class or enum: a single indented {@code pass} statement. */
    private static final String BODY_PASS = "\n    pass";

    /**
     * Lowers a top-level declaration node to Python source.
     *
     * @param node a canonical declaration node (STRUCT, ENUM, or CHOICE); other kinds decline
     * @return the Python class/enum/type-alias source (no trailing newline)
     * @throws EmitterException for a non-noun kind, a 0-option CHOICE, a non-ASCII / blank / bad-character
     *         identifier, or two sibling field / enum-member names that collide after identifier normalization
     *         (snake_casing for fields, then keyword sanitization) — a hard-keyword name is <em>normalized</em>,
     *         not declined — see the class-level "Identifier policy"
     */
    public String emit(IRNode node) {
        return switch (node.kind()) {
            case STRUCT -> // Unguarded cast: every STRUCT-kinded adapter node is an IRType (unlike ENUM-kinded reference stubs — see the ENUM arm).
                    emitStruct((IRType) node);
            case ENUM   -> node instanceof IREnum en ? emitEnum(en) : unsupported(node.kind());
            case CHOICE -> // Unguarded cast like STRUCT: every CHOICE-kinded adapter node is an IRType.
                    emitChoice((IRType) node);
            default     -> unsupported(node.kind());
        };
    }

    /**
     * Lowers a STRUCT to an instantiable {@code @dataclass}: the {@code @dataclass} decorator, then each field as
     * {@code <name>: <type> = None} (the uniform unset default — see the class-level "Data object, not validator"
     * note). An empty STRUCT emits {@code @dataclass\nclass X:\n    pass}.
     *
     * <p>Dunder-shaped names ({@code __x__}) for the class, base, or any field decline via
     * {@link EmitterException}: a {@code @dataclass} with a dunder field (e.g. {@code __init__}) conflicts with
     * auto-generated special methods and raises {@code TypeError} at instantiation. Sunder-shaped names ({@code _x_})
     * are NOT declined here — sunder is Enum-reserved only (see {@link PythonIdentifiers#isEnumReserved}).
     */
    private String emitStruct(IRType type) {
        String name = PythonIdentifiers.safeName(type.name());
        requireNotDunder(name, "STRUCT class name");
        StringBuilder out = new StringBuilder("@dataclass\nclass ").append(name);
        type.baseType().ifPresent(base -> {
            String baseName = PythonIdentifiers.safeName(base.name());
            requireNotDunder(baseName, "STRUCT base class name");
            out.append('(').append(baseName).append(')');
        });
        out.append(':');
        List<IRField> fields = type.fields();
        if (fields.isEmpty()) {
            return out.append(BODY_PASS).toString();
        }
        Set<String> seen = new HashSet<>();
        String where = "fields of " + name;
        for (IRField field : fields) {
            // The field name is Pythonic snake_cased, then keyword-sanitized (dayCountFraction -> day_count_fraction;
            // a hard keyword like global -> global_) — see PythonIdentifiers#snakeName.
            String snaked = PythonIdentifiers.snakeName(field.name());
            requireNotDunder(snaked, "STRUCT field name among " + where);
            String id = requireFresh(seen, snaked, where);
            out.append("\n    ").append(id).append(": ").append(pythonFieldType(field)).append(" = None");
        }
        return out.toString();
    }

    /** Declines a dunder-shaped ({@code __x__}) identifier — see the class-level javadoc and {@link PythonIdentifiers#isDunder}. */
    private static void requireNotDunder(String id, String context) {
        if (PythonIdentifiers.isDunder(id)) {
            throw new EmitterException("IRPythonDeclarationEmitter: " + context + " has a dunder shape \""
                    + id + "\" — dunder names collide with @dataclass/object machinery");
        }
    }

    /** Maps a type reference's name to its Python type: a built-in (via {@link #BASIC_TYPES}) or a user-type class
     * reference (a safe identifier). Keyed on the SIMPLE name — built-ins are never namespaced. */
    private static String pythonType(IRType type) {
        String typeName = PythonIdentifiers.simpleName(type.name());
        String py = BASIC_TYPES.get(typeName);
        return py != null ? py : PythonIdentifiers.sanitize(typeName);
    }

    private String pythonFieldType(IRField field) {
        String py = pythonType(field.type());
        return switch (field.cardinality()) {
            case ONE_TO_ONE -> py;
            case ZERO_TO_ONE -> "Optional[" + py + "]";
            case ONE_TO_MANY, ZERO_TO_MANY -> "List[" + py + "]";
        };
    }

    private String emitEnum(IREnum en) {
        String name = PythonIdentifiers.safeName(en.name());
        StringBuilder out = new StringBuilder("class ").append(name).append("(Enum):");
        List<IREnumValue> values = en.values();
        if (values.isEmpty()) {
            return out.append(BODY_PASS).toString();
        }
        Set<String> seen = new HashSet<>();
        String where = "enum members of " + name;
        for (IREnumValue v : values) {
            // Identifier may be sanitized (None -> None_); the VALUE is the ORIGINAL Rune name (escaping-free,
            // unique -> no Enum alias collapse), so a keyword member emits e.g. `None_ = "None"`.
            String member = PythonIdentifiers.sanitize(v.name());
            // A sunder (_x_) / dunder (__x__) shape passes sanitize but is NOT a usable enum.Enum member (a sunder
            // crashes the metaclass at class creation; a dunder is silently dropped) — decline rather than emit
            // invalid / lossy Python. Scoped to enum members: struct fields / class names are not Enum-reserved.
            if (PythonIdentifiers.isEnumReserved(member)) {
                throw new EmitterException("IRPythonDeclarationEmitter: enum.Enum reserves the member identifier \""
                        + member + "\" (a single-underscore sunder / double-underscore dunder shape) among " + where);
            }
            member = requireFresh(seen, member, where);
            out.append("\n    ").append(member).append(" = \"").append(v.name()).append('"');
        }
        return out.toString();
    }

    /** A Rune CHOICE → a Python {@code Union} type alias over its option types ({@code Name = Union[A, B]}).
     * A 0-option CHOICE declines via {@link EmitterException}; a 1-option CHOICE emits {@code Union[X]}. */
    private String emitChoice(IRType choice) {
        List<IRField> options = choice.fields();
        if (options.isEmpty()) {
            throw new EmitterException(
                    "IRPythonDeclarationEmitter cannot emit a 0-option CHOICE: " + choice.name());
        }
        String joined = options.stream()
                .map(o -> pythonType(o.type()))
                .collect(Collectors.joining(", "));
        return PythonIdentifiers.safeName(choice.name()) + " = Union[" + joined + "]";
    }

    /**
     * Adds {@code id} to {@code seen} and returns it; declines if it was already present — i.e. if two sibling
     * field / enum-member identifiers collide. This catches (a) keyword sanitization collapsing a hard keyword
     * {@code K} and a literal {@code K_} onto one identifier (e.g. fields {@code global} and {@code global_}),
     * (b) snake_casing two distinct <em>field</em> names onto one (e.g. {@code fooBar} and {@code foo_bar} both →
     * {@code foo_bar}; fields snake_case via {@link PythonIdentifiers#snakeName}, enum members do not), and
     * (c) two identical input names. Any of these declines, pre-empting a silently-dropped field/member rather than
     * emitting a duplicate Python attribute / enum member (all corpus-absent today; an invariant on the identifier
     * transform, not a recovery of any real case).
     *
     * <p>Choice options are deliberately NOT guarded here. A collision there needs two distinct option <em>types</em>
     * whose names sanitize alike (a keyword-named type and its {@code _}-twin) — which can only occur alongside an
     * out-of-scope cross-declaration {@code class K_} name collision (a module-emitter concern; see the package
     * notes). Within one declaration the {@code Union} merely deduplicates equal arguments, so the alias stays
     * locally valid Python.
     */
    private static String requireFresh(Set<String> seen, String id, String where) {
        if (!seen.add(id)) {
            throw new EmitterException(
                    "IRPythonDeclarationEmitter: identifier normalization produced a colliding identifier among "
                            + where);
        }
        return id;
    }

    private static String unsupported(IRKind kind) {
        throw new EmitterException("IRPythonDeclarationEmitter does not lower IR kind " + kind);
    }
}
