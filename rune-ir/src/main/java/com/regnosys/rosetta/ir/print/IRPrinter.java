package com.regnosys.rosetta.ir.print;

import com.regnosys.rosetta.ir.core.IRAliasLink;
import com.regnosys.rosetta.ir.core.IRAnnotationUse;
import com.regnosys.rosetta.ir.core.IRDocReference;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IREnum;
import com.regnosys.rosetta.ir.core.IREnumSynonym;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRFunctionSignature;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRLabel;
import com.regnosys.rosetta.ir.core.IRModel;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRQualifiableConfig;
import com.regnosys.rosetta.ir.core.IRQualificationFunction;
import com.regnosys.rosetta.ir.core.IRRuleReference;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.ir.core.IRTypeParameter;
import com.regnosys.rosetta.ir.core.IRWithMetaUse;
import com.regnosys.rosetta.ir.expr.BinaryOp;
import com.regnosys.rosetta.ir.expr.Existence;
import com.regnosys.rosetta.ir.expr.FieldAccess;
import com.regnosys.rosetta.ir.expr.IRApply;
import com.regnosys.rosetta.ir.expr.IRConditional;
import com.regnosys.rosetta.ir.expr.IRConversion;
import com.regnosys.rosetta.ir.expr.IREmptyLiteral;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.IRConstruct;
import com.regnosys.rosetta.ir.expr.IRLambdaOp;
import com.regnosys.rosetta.ir.expr.IRListConstruct;
import com.regnosys.rosetta.ir.expr.IRListOp;
import com.regnosys.rosetta.ir.expr.IRMetaAccess;
import com.regnosys.rosetta.ir.expr.IROnlyExists;
import com.regnosys.rosetta.ir.expr.IRPipe;
import com.regnosys.rosetta.ir.expr.IRLiteral;
import com.regnosys.rosetta.ir.expr.IRReference;
import com.regnosys.rosetta.ir.expr.IRPointFreeApply;
import com.regnosys.rosetta.ir.expr.IRAllAnyCompare;
import com.regnosys.rosetta.ir.expr.IRChoiceOptionNav;
import com.regnosys.rosetta.ir.expr.IRDispatchInputRef;
import com.regnosys.rosetta.ir.expr.IRSynItemNav;
import com.regnosys.rosetta.ir.expr.IRClosureParam;
import com.regnosys.rosetta.ir.expr.IRMetaOutputApply;
import com.regnosys.rosetta.ir.expr.IRSymbolNav;
import com.regnosys.rosetta.ir.expr.IRToString;
import com.regnosys.rosetta.ir.expr.IRVariable;
import com.regnosys.rosetta.ir.expr.Let;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Renders the neutral IR to a deterministic MLIR-style text form.
 *
 * <p>Example (declaration IR):
 * <pre>{@code
 * ENUM test.model.DirectionEnum
 *   VALUE Up
 *   VALUE Down displayName "DOWN"
 * }</pre>
 *
 * <p>Instances are immutable and safe to share; each {@code print} call builds
 * its own buffer.
 *
 * <h2>Known model holes (surfaced, not resolved)</h2>
 * <p>The following gaps in the underlying IR model are printed faithfully but
 * not corrected by the printer:
 * <ul>
 *   <li><b>JVM-boxed {@code IRLiteral.value}:</b> the {@code value} field is
 *       typed as {@code Object}, boxing the numeric/boolean/string payload.
 *       The printer dispatches on {@link com.regnosys.rosetta.ir.expr.IRLiteral#literalKind()}
 *       to cast safely; a mismatch between {@code literalKind} and the actual
 *       runtime type would throw at print time.</li>
 *   <li><b>Optionality / {@code isAbstract=false}:</b> optionality is a genuinely-computed
 *       neutral fact — {@code PRESENT} for most nodes, but {@code OPTIONAL} for the empty-literal
 *       leaf, optional-bearing navigation chains, and to-string over an optional child (as
 *       {@link #cardinalityBracket} confirms by printing {@code OPTIONAL}). Only {@code isAbstract=false}
 *       is a true hardcoded constant (the adapter's current coverage). Both flags are printed verbatim,
 *       not corrected. Note: emitters consume {@code OPTIONAL} in fail-closed declines (e.g. arithmetic /
 *       ordered-comparison in {@code IRPythonEmitter}) — do not mistake those branches for dead code.</li>
 *   <li><b>{@code empty} vs {@code absent} orthogonality:</b>
 *       {@link com.regnosys.rosetta.ir.expr.IREmptyLiteral} represents the
 *       Rune {@code empty} keyword (an explicit empty list literal), while
 *       {@code Optionality.OPTIONAL} represents an absent single value —
 *       two distinct concepts that both print truthfully but whose semantic
 *       relationship is left to the reader.</li>
 *   <li><b>{@code IRApply} per-arg cardinality absent:</b>
 *       {@link com.regnosys.rosetta.ir.expr.IRApply} carries arguments as a
 *       flat list with no per-argument target-parameter cardinality. The printer
 *       emits each arg sub-tree faithfully; argument cardinality must be inferred
 *       from context by downstream consumers.</li>
 *   <li><b>{@code IREnum} values are local, the parent is a reference:</b> the adapter neither walks
 *       nor flattens parent enumerations, so a Rune {@code enum Foo extends Bar} prints Foo's local
 *       values only. The parent itself is NOT absent since the declaration-IR enrichment: it is
 *       printed as a {@code parent} reference line, and a consumer that wants the inherited values
 *       walks it.</li>
 * </ul>
 *
 * <h2>The declaration facts (decision D55)</h2>
 * <p>Beside the structure, a declaration node carries its namespace, a reference's resolved facts,
 * the documentation, the annotations, the doc references, the condition names, the exact bounds, the
 * type arguments, the labels, the rule references and an enum value's synonyms. Each is printed as
 * an indented detail line under its node's header — and ONLY when it is non-default (an
 * {@link Optional} that is present, a list that is non-empty, a boolean that is true), so a node
 * built through a pre-enrichment constructor prints byte-for-byte what it printed before.
 *
 * <h2>The type gate (v3.3 seat 7, PR #643)</h2>
 * <p>A {@code typeAlias} DECLARATION prints as its own {@code TYPE_ALIAS} node — its body, its
 * declared parameters and the arguments the body wrote — and any type reference that resolves to an
 * alias prints the COLLAPSED chain after a {@code =>} arrow on its reference line. Both obey the same
 * non-default-only rule, so no existing golden moves.
 *
 * <h2>The property gate (v3.3 seat 8, PR #644)</h2>
 * <p>Three facts join, each under the same non-default-only rule. A type's {@code condition} line gains
 * its KIND token ({@code condition "Positive" : DataRule}) when the producer stated kinds — the tokens are
 * index-parallel to the names or absent altogether, so a node that states none prints exactly what it
 * printed before. A TYPE_ALIAS reference that carries its CHAIN prints one {@code alias} rung line per
 * link under the reference line, outermost-first, with that rung's parameters and its conditions. And a
 * {@link IRModel} prints as its own {@code MODEL <namespace>} node: the model-level facts the data-type
 * emitter's derived files read, which no type, enum or alias node carries.
 */
public final class IRPrinter {

    private static final String INDENT_UNIT = "  ";

    /** Components deliberately excluded from the dump (the spec's excluded-component allowlist). */
    private static final Set<String> EXCLUDED_COMPONENTS = Set.of("sourceRange", "metadata", "nodeId");

    private final boolean debugIds;

    /** Creates a printer with {@code --debug-ids} off (the default for goldens). */
    public IRPrinter() {
        this(false);
    }

    /**
     * Creates a printer with node-id ({@code @id}) emission controlled by {@code debugIds}.
     *
     * @param debugIds when {@code true}, expression nodes also render their
     *                 deterministic {@link com.regnosys.rosetta.ir.expr.NodeId};
     *                 off by default to keep declaration goldens low-noise.
     */
    public IRPrinter(boolean debugIds) {
        this.debugIds = debugIds;
    }

    /**
     * Renders a whole model's top-level declaration nodes in source order.
     *
     * <p>Example:
     * <pre>{@code
     * List<IRNode> nodes = new AstToIRAdapter().adaptModel(model);
     * String text = new IRPrinter().printAll(nodes);
     * }</pre>
     *
     * @param nodes the adapter output in source order; each element is printed at depth 0
     * @return the concatenated rendering, each node followed by its own trailing newline
     */
    public String printAll(List<? extends IRNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (IRNode node : nodes) {
            printNode(node, 0, sb);
        }
        return sb.toString();
    }

    /**
     * Renders a single declaration node (and its children) to text.
     *
     * <p>Example:
     * <pre>{@code
     * IRNode node = new AstToIRAdapter().adaptData("com.example", rDataType);
     * String text = new IRPrinter().print(node);
     * // e.g. "STRUCT com.example.Trade [abstract=false]\n  FIELD legs : Leg [ZERO_TO_MANY]\n"
     * }</pre>
     *
     * @param node a declaration IR node (never an {@link com.regnosys.rosetta.ir.expr.IRExpr})
     * @return the rendered text, one node per logical line, newline-terminated
     */
    public String print(IRNode node) {
        StringBuilder sb = new StringBuilder();
        printNode(node, 0, sb);
        return sb.toString();
    }

    private void printNode(IRNode node, int depth, StringBuilder sb) {
        switch (node.kind()) {
            case STRUCT, CHOICE -> printType((IRType) node, depth, sb);
            // IRKind.TYPE_ALIAS is produced both by IRTypeNode alias DECLARATIONS (since the type
            // gate) and by IRNodeImpl for a kind the adapter does not model, so guard the cast the
            // same way the ENUM arm does rather than risk a ClassCastException.
            case TYPE_ALIAS -> { if (node instanceof IRType t) printTypeAlias(t, depth, sb); else printGeneric(node, depth, sb); }
            // IRKind.ENUM is produced both by IREnumNode declarations and by IRTypeNode type
            // references (a field/base whose target is an enum); only the former is an IREnum, so
            // guard the cast and fall back to printGeneric rather than risk a ClassCastException.
            case ENUM -> { if (node instanceof IREnum en) printEnum(en, depth, sb); else printGeneric(node, depth, sb); }
            // IRKind.MODEL is produced by IRModelNode (the property gate, PR #644); guard the cast the same
            // way the ENUM arm does, so a node that merely CLAIMS the kind is dumped, never mis-cast.
            case MODEL -> { if (node instanceof IRModel model) printModel(model, depth, sb); else printGeneric(node, depth, sb); }
            case FIELD -> printField((IRField) node, depth, sb);
            case ENUM_VALUE -> printEnumValue((IREnumValue) node, depth, sb);
            default -> printGeneric(node, depth, sb);
        }
    }

    private void printType(IRType node, int depth, StringBuilder sb) {
        String keyword = node.kind() == IRKind.CHOICE ? "CHOICE " : "STRUCT ";
        String base = node.baseType().map(b -> " : " + b.name()).orElse("");
        line(sb, depth, keyword + node.name() + base + " [abstract=" + node.isAbstract() + ']');
        int facts = depth + 1;
        printOptional(sb, facts, "namespace", node.namespace());
        printOptional(sb, facts, "resolvedName", node.resolvedName());
        node.baseType().ifPresent(b -> printReference(sb, facts, "baseTypeRef", b));
        printOptional(sb, facts, "definition", node.definition());
        printDocReferences(sb, facts, node.docReferences());
        printAnnotations(sb, facts, node.annotations());
        printConditions(sb, facts, node);
        for (IRField field : node.fields()) {
            printNode(field, depth + 1, sb);
        }
    }

    /**
     * A {@code typeAlias} DECLARATION (the type gate, v3.3 seat 7 / PR #643):
     * {@code TYPE_ALIAS <name> : <the body's written name>} (the suffix is dropped when the alias has
     * no body), then its facts one indented line each — the namespace, one {@code param} line per
     * declared parameter (its own type reference, the arguments its own type call wrote - PR #644 - and its
     * documentation when the model wrote one, indented under it), the body's own reference line, the arguments the body wrote, the
     * documentation, the doc references, the annotations and the condition names. As everywhere in
     * this printer, each line appears ONLY when the fact is non-default.
     */
    private void printTypeAlias(IRType node, int depth, StringBuilder sb) {
        String base = node.baseType().map(b -> " : " + b.name()).orElse("");
        line(sb, depth, "TYPE_ALIAS " + node.name() + base);
        int facts = depth + 1;
        printOptional(sb, facts, "namespace", node.namespace());
        for (IRTypeParameter parameter : node.typeParameters()) {
            line(sb, facts, "param " + parameter.name() + " : " + parameter.type().name());
            printReference(sb, facts + 1, "typeRef", parameter.type());
            for (IRTypeArgument argument : parameter.typeArguments()) {
                line(sb, facts + 1, typeArgument(argument));
            }
            printOptional(sb, facts + 1, "definition", parameter.definition());
        }
        node.baseType().ifPresent(b -> printReference(sb, facts, "baseTypeRef", b));
        for (IRTypeArgument argument : node.baseTypeArguments()) {
            line(sb, facts, typeArgument(argument));
        }
        printOptional(sb, facts, "definition", node.definition());
        printDocReferences(sb, facts, node.docReferences());
        printAnnotations(sb, facts, node.annotations());
        printConditions(sb, facts, node);
    }

    private void printField(IRField node, int depth, StringBuilder sb) {
        line(sb, depth, "FIELD " + node.name() + " : " + node.type().name()
                + " [" + node.cardinality() + "]");
        int facts = depth + 1;
        printReference(sb, facts, "typeRef", node.type());
        node.bounds().ifPresent(bounds -> line(sb, facts, "bounds " + bounds.display()));
        if (node.isOverride()) {
            line(sb, facts, "override");
        }
        for (IRTypeArgument argument : node.typeArguments()) {
            line(sb, facts, typeArgument(argument));
        }
        printOptional(sb, facts, "definition", node.definition());
        printDocReferences(sb, facts, node.docReferences());
        printAnnotations(sb, facts, node.annotations());
        for (IRLabel label : node.labels()) {
            line(sb, facts, "label " + quoted(label.label())
                    + label.forPath().map(p -> " for " + p.display()).orElse("")
                    + label.asPath().map(p -> " as " + p.display()).orElse(""));
        }
        for (IRRuleReference reference : node.ruleReferences()) {
            line(sb, facts, ruleReference(reference));
        }
    }

    private void printEnum(IREnum node, int depth, StringBuilder sb) {
        line(sb, depth, "ENUM " + node.name());
        int facts = depth + 1;
        printOptional(sb, facts, "namespace", node.namespace());
        node.parent().ifPresent(parent ->
                line(sb, facts, "parent " + parent.name() + resolvedSuffix(parent)));
        printOptional(sb, facts, "definition", node.definition());
        printDocReferences(sb, facts, node.docReferences());
        printAnnotations(sb, facts, node.annotations());
        for (IREnumValue value : node.values()) {
            printNode(value, depth + 1, sb);
        }
    }

    private void printEnumValue(IREnumValue node, int depth, StringBuilder sb) {
        String suffix = node.displayName()
                .map(dn -> " displayName \"" + escape(dn) + "\"")
                .orElse("");
        line(sb, depth, "VALUE " + node.name() + suffix);
        int facts = depth + 1;
        printOptional(sb, facts, "definition", node.definition());
        printDocReferences(sb, facts, node.docReferences());
        printAnnotations(sb, facts, node.annotations());
        for (IREnumSynonym synonym : node.synonyms()) {
            line(sb, facts, synonymLine(synonym));
        }
    }

    /**
     * THE MODEL NODE (v3.3 seat 8, PR #644 — the property gate): {@code MODEL <namespace>}, then the
     * model-level facts the data-type emitter's DERIVED files read and no type node carries, one indented
     * line each and each only when stated — the namespace's documentation ({@code package-info.java}), the
     * version stamped on every POJO and {@code *Meta}, the {@code isProduct} / {@code isEvent} roots, the
     * {@code [qualification]} functions with their first input's type, every function's signature (its inputs
     * and its output printed as the FIELD lines they are, so a {@code [metadata …]} annotation on one — the
     * wrapper set's third source — is visible), and every {@code with-meta} use with its entries and its
     * argument's inferred type, or its NAMED refusal, which is a fact and not a silence.
     */
    private void printModel(IRModel node, int depth, StringBuilder sb) {
        line(sb, depth, "MODEL " + node.namespace());
        int facts = depth + 1;
        printOptional(sb, facts, "definition", node.definition());
        printOptional(sb, facts, "version", node.version());
        for (IRQualifiableConfig config : node.qualifiableConfigs()) {
            line(sb, facts, qualifiableKeyword(config.kind()) + " root " + config.rootType().name());
            printReference(sb, facts + 1, "rootTypeRef", config.rootType());
        }
        for (IRQualificationFunction function : node.qualificationFunctions()) {
            line(sb, facts, "qualification " + function.name() + " : " + function.firstInputType().name());
            printReference(sb, facts + 1, "firstInputTypeRef", function.firstInputType());
        }
        for (IRFunctionSignature signature : node.functionSignatures()) {
            line(sb, facts, "function " + signature.name());
            for (IRField input : signature.inputs()) {
                line(sb, facts + 1, "input");
                printNode(input, facts + 2, sb);
            }
            if (signature.output().isPresent()) {
                line(sb, facts + 1, "output");
                printNode(signature.output().get(), facts + 2, sb);
            }
        }
        for (IRWithMetaUse use : node.withMetaUses()) {
            line(sb, facts, withMetaLine(use));
            use.argumentType().ifPresent(type -> printReference(sb, facts + 1, "argumentTypeRef", type));
            for (IRTypeArgument argument : use.typeArguments()) {
                line(sb, facts + 1, typeArgument(argument));
            }
        }
    }

    /** The model's own keyword for a qualifiable configuration — {@code IS_PRODUCT} is written {@code isProduct}. */
    private static String qualifiableKeyword(String kind) {
        return switch (kind) {
            case "IS_PRODUCT" -> "isProduct";
            case "IS_EVENT" -> "isEvent";
            // IRQualifiableConfig admits no third token; print the raw one rather than drop the declaration.
            default -> kind;
        };
    }

    /**
     * {@code with-meta [reference, id] : number} — the entries as the model wrote them and the argument's
     * inferred type (its leaf). When the workspace could not type the argument the line states the refusal
     * instead, {@code with-meta [scheme] refused "nothing"}: the old generator's silent skip made a fact.
     */
    private static String withMetaLine(IRWithMetaUse use) {
        StringBuilder out = new StringBuilder("with-meta [")
                .append(String.join(", ", use.entryNames())).append(']');
        use.argumentType().ifPresent(type -> out.append(" : ").append(type.name()));
        use.refusal().ifPresent(refusal -> out.append(" refused ").append(quoted(refusal)));
        return out.toString();
    }

    // -------------------------------------------------------------------------
    // The declaration facts (decision D55) — each printed only when non-default
    // -------------------------------------------------------------------------

    /** {@code <label> "<value>"} when the optional is present; nothing otherwise. */
    private static void printOptional(StringBuilder sb, int depth, String label, Optional<String> value) {
        value.ifPresent(v -> line(sb, depth, label + " " + quoted(v)));
    }

    /**
     * {@code <label> <KIND> ns="…" name="…"} for a type REFERENCE that resolved; nothing for one that
     * did not (it is carried by the written name the header line already prints). A TYPE_ALIAS
     * reference whose chain the adapter COLLAPSED adds the leaf after a {@code =>} arrow (the type
     * gate, PR #643), e.g.
     * {@code typeRef TYPE_ALIAS ns="a.b" name="Int3" => BASIC_TYPE int[digits="3", fractionalDigits="0"]}.
     *
     * <p>Since the property gate (PR #644) the reference's alias CHAIN, when it carries one, follows as one
     * indented {@code alias} rung line per link — and those lines print even for an unresolved reference,
     * which has no header line of its own: a fact carried is a fact shown.
     */
    private static void printReference(StringBuilder sb, int depth, String label, IRType reference) {
        boolean resolved = reference.namespace().isPresent() || reference.resolvedName().isPresent();
        if (resolved) {
            line(sb, depth, label + " " + reference.kind() + resolvedSuffix(reference)
                    + reference.effectiveBase().map(IRPrinter::effectiveBaseSuffix).orElse(""));
        } else if (reference.aliasChain().isEmpty()) {
            return;
        }
        // the chain's rungs (PR #644), outermost-first, one indented line each under the reference line. A
        // chain on an UNRESOLVED reference still prints — a fact carried is a fact shown, never dropped for
        // want of a header.
        for (IRAliasLink link : reference.aliasChain()) {
            line(sb, depth + 1, aliasLine(link));
        }
    }

    /**
     * One RUNG of a {@code typeAlias} chain as the use site walks it (v3.3 seat 8, PR #644 — the property
     * gate): the alias's qualified name, the parameters it declares and the conditions it carries with their
     * kinds — the two facts the type-format validator reads (it wires every condition of every rung, and
     * REFUSES the whole validator when a PARAMETERISED rung carries one). Each section appears only when the
     * rung has it, as everywhere in this printer:
     * {@code alias "a.b.Int3" params [digits] conditions ["Positive" : DataRule, <unnamed> : OneOf]}.
     */
    private static String aliasLine(IRAliasLink link) {
        StringBuilder out = new StringBuilder("alias ").append(quoted(link.qualifiedName()));
        if (!link.parameterNames().isEmpty()) {
            out.append(" params [").append(String.join(", ", link.parameterNames())).append(']');
        }
        List<Optional<String>> names = link.conditionNames();
        if (!names.isEmpty()) {
            out.append(" conditions [");
            for (int i = 0; i < names.size(); i++) {
                if (i > 0) {
                    out.append(", ");
                }
                out.append(conditionDisplay(names.get(i), link.conditionKinds(), i));
            }
            out.append(']');
        }
        return out.toString();
    }

    /**
     * One {@code condition} line per condition name, in source order, each with its KIND token after a
     * {@code :} when the producer stated kinds (v3.3 seat 8, PR #644). The kinds are index-parallel to the
     * names or EMPTY — {@code IRAliasLink.checkConditionKinds} refuses anything else — so a node that states
     * none prints byte-for-byte what it printed before the gate.
     */
    private static void printConditions(StringBuilder sb, int depth, IRType node) {
        List<Optional<String>> names = node.conditionNames();
        for (int i = 0; i < names.size(); i++) {
            line(sb, depth, "condition " + conditionDisplay(names.get(i), node.conditionKinds(), i));
        }
    }

    /** {@code "<name>" : <Kind>} / {@code <unnamed> : <Kind>} — the kind dropped when the producer stated none. */
    private static String conditionDisplay(Optional<String> name, List<String> kinds, int index) {
        return name.map(IRPrinter::quoted).orElse("<unnamed>")
                + (index < kinds.size() ? " : " + kinds.get(index) : "");
    }

    private static String resolvedSuffix(IRType reference) {
        return reference.namespace().map(ns -> " ns=" + quoted(ns)).orElse("")
                + reference.resolvedName().map(n -> " name=" + quoted(n)).orElse("");
    }

    /**
     * {@code  => <KIND> <qualifiedName>[<param>=<"literal">, …]} — the leaf a TYPE_ALIAS chain
     * collapses to and the arguments in force there. The bracket is dropped when the substitution
     * left no argument (the {@code typeAlias ISODate: date} shape), as everywhere else here: nothing
     * is printed for a fact at its default. An effective argument is LITERAL by the record's own law,
     * so {@code literalValue} is always there to read.
     */
    private static String effectiveBaseSuffix(IREffectiveBase base) {
        StringBuilder out = new StringBuilder(" => ").append(base.kind()).append(' ')
                .append(base.qualifiedName());
        if (!base.arguments().isEmpty()) {
            out.append('[');
            for (int i = 0; i < base.arguments().size(); i++) {
                IRTypeArgument argument = base.arguments().get(i);
                if (i > 0) {
                    out.append(", ");
                }
                out.append(argument.parameter()).append('=').append(argument.negated() ? "-" : "")
                        .append(quoted(argument.literalValue().orElseThrow()));
            }
            out.append(']');
        }
        return out.toString();
    }

    /** {@code typeArg <parameter> = <name | "literal" | -"literal">} - the record carries EXACTLY ONE of the two values. */
    private static String typeArgument(IRTypeArgument argument) {
        String head = "typeArg " + argument.parameter();
        if (argument.nameValue().isPresent()) {
            return head + " = " + argument.nameValue().get();
        }
        return head + " = " + (argument.negated() ? "-" : "") + quoted(argument.literalValue().orElseThrow());
    }

    /** {@code ruleReference "<name>" | empty}, with its scope path and the rule it resolves to. */
    private static String ruleReference(IRRuleReference reference) {
        StringBuilder out = new StringBuilder("ruleReference ");
        out.append(reference.ruleName().map(IRPrinter::quoted).orElse("empty"));
        reference.forPath().ifPresent(path -> out.append(" for ").append(path.display()));
        if (reference.resolvedNamespace().isPresent() || reference.resolvedName().isPresent()) {
            out.append(" ->");
            reference.resolvedNamespace().ifPresent(ns -> out.append(" ns=").append(quoted(ns)));
            reference.resolvedName().ifPresent(n -> out.append(" name=").append(quoted(n)));
        }
        return out.toString();
    }

    /** {@code synonym "<value>" sources [a, b] definition "…" pattern "m" -> "r" removeHtml}. */
    private static String synonymLine(IREnumSynonym synonym) {
        StringBuilder out = new StringBuilder("synonym ").append(quoted(synonym.value()));
        if (!synonym.sources().isEmpty()) {
            out.append(" sources [").append(String.join(", ", synonym.sources())).append(']');
        }
        synonym.definition().ifPresent(d -> out.append(" definition ").append(quoted(d)));
        if (synonym.patternMatch().isPresent() || synonym.patternReplace().isPresent()) {
            out.append(" pattern ").append(quoted(synonym.patternMatch().orElse("")))
                    .append(" -> ").append(quoted(synonym.patternReplace().orElse("")));
        }
        if (synonym.removeHtml()) {
            out.append(" removeHtml");
        }
        return out.toString();
    }

    private static void printAnnotations(StringBuilder sb, int depth, List<IRAnnotationUse> annotations) {
        for (IRAnnotationUse use : annotations) {
            StringBuilder out = new StringBuilder("annotation ").append(use.key());
            if (!use.arguments().isEmpty()) {
                out.append(" [");
                for (int i = 0; i < use.arguments().size(); i++) {
                    IRAnnotationUse.Argument argument = use.arguments().get(i);
                    if (i > 0) {
                        out.append(", ");
                    }
                    out.append(argument.key())
                            .append(argument.attributeRef() ? " -> " : " = ")
                            .append(quoted(argument.value()));
                }
                out.append(']');
            }
            line(sb, depth, out.toString());
        }
    }

    /**
     * One {@code docReference} header line per reference (its {@code regulatory} flag and its scope
     * path), then its remaining parts one indented line each — a corpus carries both the text the
     * model wrote and, when resolved, the declaration's own facts.
     */
    private void printDocReferences(StringBuilder sb, int depth, List<IRDocReference> references) {
        for (IRDocReference reference : references) {
            line(sb, depth, "docReference" + (reference.regulatory() ? " [regulatory]" : "")
                    + reference.path().map(p -> " for " + p.display()).orElse(""));
            int parts = depth + 1;
            printOptional(sb, parts, "body", reference.body());
            for (IRDocReference.Corpus corpus : reference.corpora()) {
                line(sb, parts, "corpus " + quoted(corpus.reference())
                        + corpus.resolved().map(IRPrinter::corpusDeclaration).orElse(""));
            }
            for (IRDocReference.Segment segment : reference.segments()) {
                line(sb, parts, "segment " + quoted(segment.name()) + " = " + quoted(segment.value()));
            }
            for (IRDocReference.Rationale rationale : reference.rationales()) {
                line(sb, parts, "rationale" + rationale.text().map(t -> " " + quoted(t)).orElse("")
                        + rationale.author().map(a -> " author " + quoted(a)).orElse(""));
            }
            printOptional(sb, parts, "structuredProvision", reference.structuredProvision());
            printOptional(sb, parts, "provision", reference.provision());
            if (reference.reportedField()) {
                line(sb, parts, "reportedField");
            }
            for (IRDocReference.NamedArg arg : reference.namedArgs()) {
                line(sb, parts, "arg " + quoted(arg.name()) + " = " + quoted(arg.value()));
            }
        }
    }

    private static String corpusDeclaration(IRDocReference.Corpus.Declaration declaration) {
        StringBuilder out = new StringBuilder(" -> ");
        declaration.typeKeyword().ifPresent(keyword -> out.append(keyword).append(' '));
        out.append(quoted(declaration.name()));
        declaration.displayName().ifPresent(n -> out.append(" displayName ").append(quoted(n)));
        declaration.definition().ifPresent(d -> out.append(" definition ").append(quoted(d)));
        return out.toString();
    }

    /** A deterministically-escaped quoted token. */
    private static String quoted(String value) {
        return "\"" + escape(value) + "\"";
    }

    /**
     * Non-declining fallback for IR kinds not yet modelled by a dedicated
     * renderer (e.g. FUNCTION/RULE before their adapters land). Emits the kind,
     * the name, and every non-allowlisted record component so a brand-new kind
     * is always dumped rather than dropped.
     *
     * <p>A child-bearing component (an {@link IRNode}, or a {@code List}/{@code Optional} of them) recurses
     * through {@link #printNode} — the SAME path the modelled cases use — so a nested node is dumped
     * structurally rather than via a record's reflective {@code toString()}. That is deliberate: a reflective
     * {@code String.valueOf} of a child record would splice in the excluded infrastructure components
     * ({@code sourceRange}/{@code metadata}/{@code nodeId}) and a non-deterministic {@code IRMetadata}
     * identity hash, breaking the dump's determinism. Scalar components keep their {@code name = value} form.
     */
    private void printGeneric(IRNode node, int depth, StringBuilder sb) {
        line(sb, depth, node.kind() + " " + node.name()
                + " [unmodeled " + node.getClass().getSimpleName() + "]");
        if (node instanceof Record) {
            for (RecordComponent rc : node.getClass().getRecordComponents()) {
                String name = rc.getName();
                if (EXCLUDED_COMPONENTS.contains(name) || name.equals("name") || name.equals("kind")) {
                    continue;
                }
                printComponent(name, invoke(rc, node), depth + 1, sb);
            }
        }
    }

    /**
     * Renders one fallback component. A value that is (or contains) an {@link IRNode} recurses through
     * {@link #printNode} so the child is dumped via the deterministic node path; any other value is rendered
     * as {@code name = value}.
     */
    private void printComponent(String name, Object value, int depth, StringBuilder sb) {
        if (value instanceof IRNode child) {
            line(sb, depth, name + " =");
            printNode(child, depth + 1, sb);
        } else if (value instanceof Optional<?> opt && opt.isPresent() && opt.get() instanceof IRNode child) {
            line(sb, depth, name + " =");
            printNode(child, depth + 1, sb);
        } else if (value instanceof List<?> list && list.stream().anyMatch(IRNode.class::isInstance)) {
            line(sb, depth, name + " =");
            for (Object element : list) {
                if (element instanceof IRNode child) {
                    printNode(child, depth + 1, sb);
                } else {
                    line(sb, depth + 1, String.valueOf(element));
                }
            }
        } else {
            line(sb, depth, name + " = " + value);
        }
    }

    private static Object invoke(RecordComponent rc, Object target) {
        try {
            return rc.getAccessor().invoke(target);
        } catch (ReflectiveOperationException e) {
            return "<unreadable:" + rc.getName() + ">";
        }
    }

    private static void line(StringBuilder sb, int depth, String text) {
        sb.append(INDENT_UNIT.repeat(depth)).append(text).append('\n');
    }

    /**
     * Renders an expression IR tree to text (a print-only debug aid; the
     * expression form is not goldened). Example:
     * <pre>{@code
     * IRLiteral.NUMBER("0.05") : number [SINGLE, PRESENT]
     * }</pre>
     *
     * <p><b>Stack safety:</b> this is a straightforward recursive walk over
     * {@link IRExpr#children()}; it assumes bounded tree depth (true for real Rune
     * expressions). A pathologically deep tree could overflow the stack — acceptable
     * for a debug aid, revisited only if a real input hits it.
     *
     * @param expr the expression root to render; must not be {@code null}
     * @return the rendered text, one node per logical line, newline-terminated
     */
    public String print(IRExpr expr) {
        StringBuilder sb = new StringBuilder();
        printExpr(expr, 0, sb);
        return sb.toString();
    }

    private void printExpr(IRExpr e, int depth, StringBuilder sb) {
        line(sb, depth, exprHeader(e));
        for (IRExpr child : e.children()) {
            printExpr(child, depth + 1, sb);
        }
    }

    private String exprHeader(IRExpr e) {
        String id = debugIds ? " @" + e.nodeId() : "";
        return exprLabel(e) + id + " : " + RTypeFormatter.format(e.type()) + " " + cardinalityBracket(e);
    }

    private String exprLabel(IRExpr e) {
        return switch (e.kind()) {
            case LITERAL -> {
                IRLiteral l = (IRLiteral) e;
                yield "IRLiteral." + l.literalKind() + "(\"" + escape(literalValue(l)) + "\")";
            }
            case EMPTY_LITERAL ->
                "IREmptyLiteral." + ((IREmptyLiteral) e).source();
            case VARIABLE -> {
                IRVariable v = (IRVariable) e;
                yield "IRVariable." + v.variableKind() + "(\"" + escape(v.name()) + "\")";
            }
            case REFERENCE -> {
                IRReference r = (IRReference) e;
                yield "IRReference." + r.referenceKind() + "(\"" + escape(r.target()) + "\")";
            }
            case APPLY -> "IRApply";
            case BINARY_OP -> "BinaryOp." + ((BinaryOp) e).op();
            case EXISTENCE -> {
                Existence x = (Existence) e;
                yield "Existence." + x.op() + (x.modifier() != null ? "(" + x.modifier() + ")" : "");
            }
            case FIELD_ACCESS -> "FieldAccess(\"" + escape(((FieldAccess) e).feature()) + "\")";
            case META_ACCESS -> {
                IRMetaAccess ma = (IRMetaAccess) e;
                yield "IRMetaAccess(\"" + escape(ma.feature()) + "\" ["
                        + String.join("+", ma.metaQualifiers()) + "])";
            }
            case LIST_OP -> "IRListOp." + ((IRListOp) e).op();
            case LIST_CONSTRUCT -> "IRListConstruct";
            case CONDITIONAL ->
                "IRConditional" + (((IRConditional) e).elseBranch() == null ? " [no-else]" : "");
            case LET -> "Let(\"" + escape(((Let) e).binder()) + "\")";
            case TO_STRING -> "IRToString";
            case POINT_FREE_APPLY ->
                "IRPointFreeApply(\"" + escape(((IRPointFreeApply) e).callee()) + "\")";
            case CONSTRUCT ->
                "IRConstruct(\"" + escape(((IRConstruct) e).typeName()) + "\")"
                        + (((IRConstruct) e).spread() ? " [spread]" : "");
            case LAMBDA_OP ->
                "IRLambdaOp." + ((IRLambdaOp) e).op()
                        + (((IRLambdaOp) e).binderName() == null ? ""
                                : "(\"" + escape(((IRLambdaOp) e).binderName()) + "\")");
            case CONVERSION -> {
                IRConversion conv = (IRConversion) e;
                yield "IRConversion." + conv.conversionKind()
                        + (conv.targetTypeName() == null ? ""
                                : "(\"" + escape(conv.targetTypeName()) + "\")");
            }
            case PIPE -> "IRPipe[n=" + ((IRPipe) e).spineLength() + "]";
            case ONLY_EXISTS -> "IROnlyExists[n=" + ((IROnlyExists) e).pathCount() + "]";
            case SYMBOL_NAV -> "IRSymbolNav[" + escape(((IRSymbolNav) e).symbolKind()) + "]";
            case CLOSURE_PARAM -> "IRClosureParam[" + escape(
                    ((IRClosureParam) e).paramName()) + "]";
            case META_OUTPUT_APPLY -> "IRMetaOutputApply[" + escape(
                    ((IRMetaOutputApply) e).calleeName()) + "]";
            case ALL_ANY_COMPARE -> "IRAllAnyCompare[" + escape(((IRAllAnyCompare) e).modifier())
                    + " " + escape(((IRAllAnyCompare) e).op()) + "]";
            case SYN_ITEM_NAV -> "IRSynItemNav[" + escape(((IRSynItemNav) e).featureName()) + "]";
            case META_ITEM_NAV -> "IRMetaItemNav[" + escape(
                    ((com.regnosys.rosetta.ir.expr.IRMetaItemNav) e).featureName()) + "]";
            case CHOICE_OPTION_NAV -> "IRChoiceOptionNav[" + escape(
                    ((IRChoiceOptionNav) e).headName()) + " -> " + escape(
                    ((IRChoiceOptionNav) e).optionName()) + "]";
            case DISPATCH_INPUT_REF -> "IRDispatchInputRef[" + escape(
                    ((IRDispatchInputRef) e).inputName()) + "]";
            case RECORD_RECEIVER_NAV -> "IRRecordReceiverNav[" + escape(
                    ((com.regnosys.rosetta.ir.expr.IRRecordReceiverNav) e).featureName())
                    + " : " + escape(
                    ((com.regnosys.rosetta.ir.expr.IRRecordReceiverNav) e).recordTypeName())
                    + "]";
            case QUALIFIER_ITEM_NAV -> "IRQualifierItemNav[" + escape(
                    ((com.regnosys.rosetta.ir.expr.IRQualifierItemNav) e).qualifierName())
                    + "]";
            case QUALIFIER_RECEIVER_NAV -> "IRQualifierReceiverNav[" + escape(
                    ((com.regnosys.rosetta.ir.expr.IRQualifierReceiverNav) e).qualifierName())
                    + "]";
            case CHOICE_RECEIVER_NAV -> "IRChoiceReceiverNav[" + escape(
                    ((com.regnosys.rosetta.ir.expr.IRChoiceReceiverNav) e).optionName())
                    + " : " + escape(
                    ((com.regnosys.rosetta.ir.expr.IRChoiceReceiverNav) e).choiceName())
                    + "]";
            case SWITCH_OP -> "IRSwitchOp[n=" + ((com.regnosys.rosetta.ir.expr.IRSwitchOp) e).caseCount()
                    + (((com.regnosys.rosetta.ir.expr.IRSwitchOp) e).hasDefault() ? " default" : "") + "]";
            case DEFAULT_OP -> "IRDefaultOp";
            case MEMBERSHIP_OP -> "IRMembershipOp."
                    + ((com.regnosys.rosetta.ir.expr.IRMembershipOp) e).op();
            case COLLECT_OP -> "IRCollectOp."
                    + ((com.regnosys.rosetta.ir.expr.IRCollectOp) e).op()
                    + (((com.regnosys.rosetta.ir.expr.IRCollectOp) e).hasBody() ? "[body]" : "");
            case OUTPUT_REF -> "IROutputRef[" + escape(
                    ((com.regnosys.rosetta.ir.expr.IROutputRef) e).outputName()) + "]";
            case META_PARAM_REF -> "IRMetaParamRef[" + escape(
                    ((com.regnosys.rosetta.ir.expr.IRMetaParamRef) e).paramName()) + "]";
            case RULE_INPUT_NAV -> "IRRuleInputNav[" + escape(
                    ((com.regnosys.rosetta.ir.expr.IRRuleInputNav) e).featureName()) + "]";
            case IMPLICIT_ATTR_NAV -> "IRImplicitAttrNav[" + escape(
                    ((com.regnosys.rosetta.ir.expr.IRImplicitAttrNav) e).attributeName()) + "]";
            case CONDITION_INSTANCE -> "IRConditionInstance[" + escape(
                    ((com.regnosys.rosetta.ir.expr.IRConditionInstance) e).typeName()) + "]";
            case WITH_META_OP -> "IRWithMetaOp[n="
                    + ((com.regnosys.rosetta.ir.expr.IRWithMetaOp) e).entryCount() + "]";
            case JOIN_OP -> "IRJoinOp"
                    + (((com.regnosys.rosetta.ir.expr.IRJoinOp) e).hasSeparator() ? "[sep]" : "");
            case OUTPUT_ALIAS_NAV -> "IROutputAliasNav[" + escape(
                    ((com.regnosys.rosetta.ir.expr.IROutputAliasNav) e).headName())
                    + " -> " + escape(
                    ((com.regnosys.rosetta.ir.expr.IROutputAliasNav) e).featureName())
                    + "]";
            case LIBRARY_APPLY -> "IRLibraryApply[" + escape(
                    ((com.regnosys.rosetta.ir.expr.IRLibraryApply) e).calleeName()) + "]";
            // Scope note (deliberate): unlike printGeneric (which reflectively dumps components for
            // unmodelled IRKind variants), this default emits only the kind name + class — no
            // reflective scalar dump. Justification: IRExprKind is closed and every member is modelled
            // by an arm above, and there is no generic fallback expression record. The output stays
            // non-blank (non-declining) so the coverage test keeps an unhandled kind visible without
            // crashing. If a new IRExprKind ever lands, the implementer must consciously add its renderer here.
            default -> e.kind() + " [unmodeled " + e.getClass().getSimpleName() + "]";
        };
    }

    private String cardinalityBracket(IRExpr e) {
        return switch (e.kind()) {
            case FIELD_ACCESS -> {
                FieldAccess fa = (FieldAccess) e;
                yield "[card=" + fa.cardinality() + ", feat=" + fa.featureCardinality()
                        + ", " + fa.optionality() + "]";
            }
            default -> "[" + e.cardinality() + ", " + e.optionality() + "]";
        };
    }

    /**
     * Extracts the printable string value from a literal, dispatching on its
     * {@link IRLiteral#literalKind()}. NUMBER uses {@link BigDecimal#toPlainString()} to
     * avoid scientific notation; INT and BOOLEAN use {@code toString()}; STRING is returned
     * as-is (the caller passes it through {@link #escape(String)} for quoting).
     */
    private String literalValue(IRLiteral l) {
        Object v = l.value();
        return switch (l.literalKind()) {
            case NUMBER -> ((BigDecimal) v).toPlainString();
            case INT    -> v.toString();   // BigInteger
            case BOOLEAN -> v.toString();  // Boolean
            case STRING  -> (String) v;
        };
    }

    /** Minimal, deterministic string escaping for quoted tokens. */
    static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 2);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
