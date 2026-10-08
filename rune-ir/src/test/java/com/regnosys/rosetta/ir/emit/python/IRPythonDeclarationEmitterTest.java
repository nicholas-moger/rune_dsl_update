package com.regnosys.rosetta.ir.emit.python;

import com.regnosys.rosetta.ir.adapter.IREnumNode;
import com.regnosys.rosetta.ir.adapter.IREnumValueNode;
import com.regnosys.rosetta.ir.adapter.IRFieldNode;
import com.regnosys.rosetta.ir.adapter.IRMetadata;
import com.regnosys.rosetta.ir.adapter.IRModelNode;
import com.regnosys.rosetta.ir.adapter.IRNodeImpl;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.Cardinality;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.emit.EmitterException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IRPythonDeclarationEmitterTest {

    private final IRPythonDeclarationEmitter emitter = new IRPythonDeclarationEmitter();

    // ── IR builders (mirror IRSamples constructor usage) ─────────────────────────
    private static IRType typeRef(String name) {
        return new IRTypeNode(name, IRKind.STRUCT, List.of(), Optional.empty(),
                false, Optional.empty(), IRMetadata.EMPTY);
    }
    private static IRField field(String name, String typeName, Cardinality card) {
        return new IRFieldNode(name, typeRef(typeName), card, Optional.empty(), IRMetadata.EMPTY);
    }
    private static IRTypeNode struct(String name, Optional<IRType> base, List<IRField> fields) {
        return new IRTypeNode(name, IRKind.STRUCT, fields, base, false, Optional.empty(), IRMetadata.EMPTY);
    }
    private static IREnumNode enumNode(String name, String... valueNames) {
        List<IREnumValue> vs = Arrays.stream(valueNames)
                .map(v -> (IREnumValue) new IREnumValueNode(v, Optional.empty(), Optional.empty(), IRMetadata.EMPTY))
                .toList();
        return new IREnumNode(name, vs, Optional.empty(), IRMetadata.EMPTY);
    }
    private static IRTypeNode choice(String name, String... optionTypeNames) {
        // A choice option: the adapter sets both the field name AND its type name to the option type name.
        List<IRField> options = Arrays.stream(optionTypeNames)
                .map(t -> field(t, t, Cardinality.ONE_TO_ONE))
                .toList();
        return new IRTypeNode(name, IRKind.CHOICE, options, Optional.empty(), false, Optional.empty(), IRMetadata.EMPTY);
    }
    private String oneField(String typeName) {
        return emitter.emit(struct("T", Optional.empty(), List.of(field("a", typeName, Cardinality.ONE_TO_ONE))));
    }

    // ── STRUCT ───────────────────────────────────────────────────────────────────
    @Test void structWithFieldsAndBaseUsesSimpleNames() {
        IRTypeNode s = struct("ns.Trade", Optional.of(typeRef("ns.Product")), List.of(
                field("tradeDate", "date", Cardinality.ONE_TO_ONE),
                field("price", "number", Cardinality.ZERO_TO_ONE),
                field("legs", "Leg", Cardinality.ZERO_TO_MANY)));
        assertEquals("@dataclass\nclass Trade(Product):\n    trade_date: date = None\n    price: Optional[Decimal] = None\n    legs: List[Leg] = None",
                emitter.emit(s));
    }
    @Test void eachCardinalityWraps() {
        String p = "@dataclass\nclass T:\n    a: ";
        assertEquals(p + "int = None", oneFieldCard(Cardinality.ONE_TO_ONE));
        assertEquals(p + "Optional[int] = None", oneFieldCard(Cardinality.ZERO_TO_ONE));
        assertEquals(p + "List[int] = None", oneFieldCard(Cardinality.ONE_TO_MANY));
        assertEquals(p + "List[int] = None", oneFieldCard(Cardinality.ZERO_TO_MANY));
    }
    private String oneFieldCard(Cardinality c) {
        return emitter.emit(struct("T", Optional.empty(), List.of(field("a", "int", c))));
    }
    @Test void eachBasicTypeMaps() {
        String p = "@dataclass\nclass T:\n    a: ";
        assertEquals(p + "bool = None", oneField("boolean"));
        assertEquals(p + "str = None", oneField("string"));
        assertEquals(p + "Decimal = None", oneField("number"));
        assertEquals(p + "int = None", oneField("int"));
        assertEquals(p + "date = None", oneField("date"));
        assertEquals(p + "datetime = None", oneField("dateTime"));
        assertEquals(p + "datetime = None", oneField("zonedDateTime"));
        assertEquals(p + "time = None", oneField("time"));
    }
    @Test void dottedBaseReducedToSimple() {
        assertEquals("@dataclass\nclass Child(Base):\n    pass",
                emitter.emit(struct("a.b.Child", Optional.of(typeRef("x.y.Base")), List.of())));
    }
    @Test void emptyStructNoBase() {
        assertEquals("@dataclass\nclass Empty:\n    pass", emitter.emit(struct("Empty", Optional.empty(), List.of())));
    }
    @Test void emptyStructWithBase() {
        assertEquals("@dataclass\nclass Empty(Base):\n    pass",
                emitter.emit(struct("Empty", Optional.of(typeRef("Base")), List.of())));
    }

    // ── ENUM ─────────────────────────────────────────────────────────────────────
    @Test void enumValueIsTheMemberName() {
        assertEquals("class Color(Enum):\n    RED = \"RED\"\n    GREEN = \"GREEN\"",
                emitter.emit(enumNode("ns.Color", "RED", "GREEN")));
    }
    @Test void emptyEnum() {
        assertEquals("class Color(Enum):\n    pass", emitter.emit(enumNode("Color")));
    }

    // ── CHOICE ───────────────────────────────────────────────────────────────────
    @Test void twoOptionChoiceIsUnion() {
        assertEquals("PayoutBase = Union[CashPayout, SecurityPayout]",
                emitter.emit(choice("ns.PayoutBase", "CashPayout", "SecurityPayout")));
    }
    @Test void oneOptionChoiceIsUnion() {
        assertEquals("Y = Union[X]", emitter.emit(choice("Y", "X")));
    }
    @Test void threeOptionChoice() {
        assertEquals("X = Union[A, B, C]", emitter.emit(choice("X", "A", "B", "C")));
    }
    @Test void dottedChoiceAndOptionNamesReducedToSimple() {
        assertEquals("PayoutBase = Union[Cash, Security]",
                emitter.emit(choice("a.b.PayoutBase", "x.Cash", "y.z.Security")));
    }
    @Test void basicTypeOptionMapsViaTypeMap() {
        // Grammar-permitted (corpus-absent): a basic-type option maps like a struct field, not a bare name.
        assertEquals("X = Union[Decimal, str]", emitter.emit(choice("X", "number", "string")));
    }
    @Test void declinesZeroOptionChoice() {
        IRTypeNode empty = new IRTypeNode("Empty", IRKind.CHOICE, List.of(),
                Optional.empty(), false, Optional.empty(), IRMetadata.EMPTY);
        assertThrows(EmitterException.class, () -> emitter.emit(empty));
    }
    @Test void choiceReflectsUnionArgs() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run skipped");
        String src = emitter.emit(choice("PayoutBase", "CashPayout", "SecurityPayout"));
        // import typing (DECL_PREAMBLE binds the names, not the module); option stubs first (Union args are eager).
        String setup = "import typing\nclass CashPayout: pass\nclass SecurityPayout: pass\n";
        assertEquals("['CashPayout', 'SecurityPayout']",
                runDecl(setup, src, "[t.__name__ for t in typing.get_args(PayoutBase)]"));
    }

    // ── SANITIZE keyword collisions (trailing underscore) ────────────────────────
    @Test void sanitizesKeywordClassName() {
        assertEquals("@dataclass\nclass global_:\n    pass", emitter.emit(struct("global", Optional.empty(), List.of())));
    }
    @Test void sanitizesKeywordFieldName() {
        assertEquals("@dataclass\nclass T:\n    global_: int = None",
                emitter.emit(struct("T", Optional.empty(), List.of(field("global", "int", Cardinality.ONE_TO_ONE)))));
    }
    @Test void sanitizesKeywordReturnField() {
        // `return` is the third real corpus collision (rune-fpml type ReturnLeg), same field-name position as `global`.
        assertEquals("@dataclass\nclass T:\n    return_: Decimal = None",
                emitter.emit(struct("T", Optional.empty(), List.of(field("return", "number", Cardinality.ONE_TO_ONE)))));
    }
    @Test void sanitizesKeywordFieldType() {
        assertEquals("@dataclass\nclass T:\n    a: global_ = None",
                emitter.emit(struct("T", Optional.empty(), List.of(field("a", "global", Cardinality.ONE_TO_ONE)))));
    }
    @Test void sanitizesKeywordBaseName() {
        assertEquals("@dataclass\nclass T(global_):\n    pass",
                emitter.emit(struct("T", Optional.of(typeRef("global")), List.of())));
    }
    @Test void sanitizesKeywordAmongSafeFields() {
        assertEquals("@dataclass\nclass T:\n    global_: int = None\n    count: int = None",
                emitter.emit(struct("T", Optional.empty(), List.of(
                        field("global", "int", Cardinality.ONE_TO_ONE),
                        field("count", "int", Cardinality.ONE_TO_ONE)))));
    }
    @Test void sanitizesKeywordEnumValueKeepsOriginalValue() {
        // Identifier is mangled (None_), but the VALUE stays the original Rune name ("None").
        assertEquals("class E(Enum):\n    None_ = \"None\"", emitter.emit(enumNode("E", "None")));
    }
    @Test void sanitizesKeywordChoiceName() {
        assertEquals("global_ = Union[A, B]", emitter.emit(choice("global", "A", "B")));
    }
    @Test void sanitizesKeywordOptionType() {
        assertEquals("X = Union[None_]", emitter.emit(choice("X", "None")));
    }

    // ── DECLINES (EmitterException) ──────────────────────────────────────────────
    @Test void declinesNonNounKind() {
        assertThrows(EmitterException.class,
                () -> emitter.emit(new IRNodeImpl("F", IRKind.FUNCTION, List.of(), Optional.empty(), IRMetadata.EMPTY)));
    }
    /**
     * A MODEL node (v3.3 seat 8, PR #644 — the property gate) carries a namespace's own facts: its version,
     * its qualifiable roots, its with-meta uses. None of that is a Python DECLARATION, so the emitter
     * declines it BY KIND through the switch's default arm — stated here rather than left to the arm's
     * silence, so a future MODEL arm cannot be added without this test being read.
     */
    @Test void declinesTheModelKind() {
        assertThrows(EmitterException.class, () -> emitter.emit(new IRModelNode("test.model")));
    }
    @Test void declinesBlankFieldType() {
        assertThrows(EmitterException.class,
                () -> emitter.emit(struct("T", Optional.empty(), List.of(field("a", "", Cardinality.ONE_TO_ONE)))));
    }
    @Test void declinesNonAsciiIdentifier() {
        // A non-ASCII letter (é) — conservatively declined. (UTF-8 source; or use "café".)
        assertThrows(EmitterException.class,
                () -> emitter.emit(struct("café", Optional.empty(), List.of())));
    }
    @Test void declinesFieldKeywordCollision() {
        // `global` -> global_ collides with a literal `global_` sibling field: decline, don't silently drop one.
        assertThrows(EmitterException.class, () -> emitter.emit(struct("T", Optional.empty(), List.of(
                field("global", "int", Cardinality.ONE_TO_ONE),
                field("global_", "int", Cardinality.ONE_TO_ONE)))));
    }
    @Test void snakeCasedFieldCollisionDeclines() {
        // fooBar and foo_bar both snake to foo_bar -> requireFresh declines (fail-closed).
        assertThrows(EmitterException.class, () -> emitter.emit(
                struct("Clash", Optional.empty(), List.of(
                        field("fooBar", "int", Cardinality.ONE_TO_ONE),
                        field("foo_bar", "int", Cardinality.ONE_TO_ONE)))));
    }
    @Test void declinesEnumKeywordCollision() {
        // `None` -> None_ collides with a literal `None_` sibling member.
        assertThrows(EmitterException.class, () -> emitter.emit(enumNode("E", "None", "None_")));
    }
    @Test void declinesEnumReservedSunderMember() {
        // G2: a `_x_` member (single leading AND trailing underscore = a "_sunder_") sanitizes unchanged (a valid
        // ASCII identifier, not a keyword) but `enum.Enum` RESERVES the sunder shape — the metaclass raises a
        // ValueError at class creation. Decline rather than emit a crashing class.
        assertThrows(EmitterException.class, () -> emitter.emit(enumNode("E", "_x_")));
    }
    @Test void declinesEnumReservedDunderMember() {
        // G2: a `__x__` (dunder) member also sanitizes unchanged, but `enum.Enum` treats a dunder as a normal
        // attribute, NOT a member — it is SILENTLY DROPPED from the enum. Decline rather than silently lose a
        // member (the requireFresh "never drop a member" rationale).
        assertThrows(EmitterException.class, () -> emitter.emit(enumNode("E", "__x__")));
    }
    @Test void declinesEnumReservedMroMember() {
        // J1 (round-4): CPython 3.13 additionally reserves the bare name `mro` for enum members —
        // `class C(Enum): mro='mro'` raises `ValueError: invalid enum member name(s) 'mro'` at import.
        // It has no sunder/dunder shape (not caught by isSunder/isDunder), so it is added explicitly
        // to isEnumReserved. `mro` as a struct field is harmless (not added to isDunder).
        assertThrows(EmitterException.class, () -> emitter.emit(enumNode("E", "mro")));
        // A struct field named mro still emits (it is NOT a struct-level reserved name).
        assertEquals("@dataclass\nclass T:\n    mro: int = None",
                emitter.emit(struct("T", Optional.empty(), List.of(field("mro", "int", Cardinality.ONE_TO_ONE)))));
    }
    @Test void enumSingleUnderscoreSidesStillEmit() {
        // GUARD: a single leading underscore (`_x`), a single trailing underscore (the keyword-sanitize form
        // `global_`), and a bare `_` are NOT Enum-reserved (only sunder/dunder are), so they still emit.
        assertEquals("class E(Enum):\n    _x = \"_x\"\n    global_ = \"global\"\n    _ = \"_\"",
                emitter.emit(enumNode("E", "_x", "global", "_")));
    }
    @Test void declinesStructWithDunderFieldName() {
        // H2 (round-3): a dunder-shaped field name (__init__, __eq__, ...) collides with the @dataclass/object
        // machinery (`@dataclass class X: __init__: Foo = None` → X() raises TypeError at construction) and
        // must decline. Sunder-shaped field names (_x_) are NOT declined — sunder is enum-specific.
        //
        // This test is RED before the fix: emitStruct has no dunder guard.
        assertThrows(EmitterException.class, () -> emitter.emit(struct("T", Optional.empty(),
                List.of(field("__init__", "int", Cardinality.ONE_TO_ONE)))));
    }
    @Test void declinesStructWithDunderClassName() {
        // H2: a dunder-shaped class name also declines.
        assertThrows(EmitterException.class,
                () -> emitter.emit(struct("__X__", Optional.empty(), List.of())));
    }
    @Test void declinesStructWithDunderBaseName() {
        // H2: a dunder-shaped base class name also declines.
        assertThrows(EmitterException.class,
                () -> emitter.emit(struct("T", Optional.of(typeRef("__Base__")), List.of())));
    }
    @Test void structSunderFieldStillEmits() {
        // GUARD: sunder (_x_) is Enum-reserved only — struct fields with sunder shape still emit.
        assertEquals("@dataclass\nclass T:\n    _x_: int = None",
                emitter.emit(struct("T", Optional.empty(),
                        List.of(field("_x_", "int", Cardinality.ONE_TO_ONE)))));
    }

    // ── parse-and-run reflection (CPython) ───────────────────────────────────────
    /** The shared oracle's gate (#630): {@code python --version} exits 0 within 10 s. */
    private static boolean pythonAvailable() {
        return PythonOracle.pythonAvailable();
    }

    /**
     * Assembles DECL_PREAMBLE (future-import first) + setup + decl, prints repr(reflectExpr), returns stdout.
     *
     * <p>Runs via a temp file rather than {@code python -c} because Windows command-line argument passing
     * strips double-quotes from the {@code -c} payload (e.g. {@code RED = "RED"} becomes {@code RED = RED}),
     * which causes a {@code NameError} inside the Enum metaclass even though the emitted Python is valid.
     */
    private static String runDecl(String setup, String declSource, String reflectExpr) throws Exception {
        String program = IRPythonDeclarationEmitter.DECL_PREAMBLE + "\n" + setup + declSource
                + "\nprint(repr(" + reflectExpr + "))";
        Path tmp = Files.createTempFile("decl_test_", ".py");
        try {
            Files.writeString(tmp, program, StandardCharsets.UTF_8);
            // #630: the shared oracle - its budget, its concurrent drain, the elapsed on a timeout (the private 30 s runner replaced)
            PythonOracle.Result r = PythonOracle.run(new ProcessBuilder("python", tmp.toString()), "decl: " + reflectExpr);
            if (r.exit() != 0) { throw new AssertionError("python failed:\n" + program + "\n--- output ---\n" + r.output()); }
            return r.output().strip();
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test void structImportsAndReflectsShape() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run skipped");
        String src = emitter.emit(struct("Trade", Optional.empty(), List.of(
                field("tradeDate", "date", Cardinality.ONE_TO_ONE),
                field("price", "number", Cardinality.ZERO_TO_ONE),
                field("legs", "Leg", Cardinality.ZERO_TO_MANY))));
        assertEquals("[('legs', 'List[Leg]'), ('price', 'Optional[Decimal]'), ('trade_date', 'date')]",
                runDecl("", src, "sorted(vars(Trade).get('__annotations__', {}).items())"));
    }

    @Test void structBaseReflectsByName() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run skipped");
        String src = emitter.emit(struct("Child", Optional.of(typeRef("Base")),
                List.of(field("x", "int", Cardinality.ONE_TO_ONE))));
        // A base is eager, so define the stub AFTER DECL_PREAMBLE and BEFORE the subject.
        assertEquals("['Base']", runDecl("class Base: pass\n", src, "[b.__name__ for b in Child.__bases__]"));
    }

    @Test void enumMembersAndCountReflect() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run skipped");
        String src = emitter.emit(enumNode("Color", "RED", "GREEN"));
        assertEquals("[('RED', 'RED'), ('GREEN', 'GREEN')]", runDecl("", src, "[(m.name, m.value) for m in Color]"));
        assertEquals("2", runDecl("", src, "len(list(Color))"));
    }

    @Test void keywordFieldStructImportsAndReflects() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run skipped");
        String src = emitter.emit(struct("Rec", Optional.empty(),
                List.of(field("global", "int", Cardinality.ONE_TO_ONE))));
        assertEquals("[('global_', 'int')]",
                runDecl("", src, "sorted(vars(Rec).get('__annotations__', {}).items())"));
    }

    @Test void keywordEnumImportsValueLookupResolvesOriginal() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run skipped");
        String src = emitter.emit(enumNode("E", "None", "Flat"));
        // members + values reflect the sanitized identifier with the ORIGINAL value
        assertEquals("[('None_', 'None'), ('Flat', 'Flat')]", runDecl("", src, "[(m.name, m.value) for m in E]"));
        // value-lookup by the original Rune name resolves the member (the faithfulness leg)
        assertEquals("'None_'", runDecl("", src, "E('None').name"));
        assertEquals("2", runDecl("", src, "len(list(E))"));
    }

    // ── @dataclass instantiability (CPython) ─────────────────────────────────────
    @Test void dataclassInstantiatesWithDefaultsAndKwargs() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run skipped");
        String src = emitter.emit(struct("Foo", Optional.empty(), List.of(
                field("bar", "string", Cardinality.ONE_TO_ONE),
                field("price", "number", Cardinality.ZERO_TO_ONE),
                field("legs", "Leg", Cardinality.ZERO_TO_MANY))));
        // Every field defaults to None: Foo() constructs (bar None, price None, legs None); kwargs set what's passed.
        assertEquals("(None, None, 'x', [1, 2])",
                runDecl("", src, "(Foo().bar, Foo().legs, Foo(bar='x').bar, Foo(bar='x', legs=[1, 2]).legs)"));
    }

    @Test void dataclassInterleavedOptionalThenRequiredConstructs() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run skipped");
        // optional-scalar THEN required-scalar: the intuitive 'default only the optional' mapping would emit
        // `a: Optional[int] = None` then `b: int` (no default) → TypeError at class definition. Defaulting EVERY
        // field (b: int = None) is what makes this interleaving emittable; prove the class defines + constructs.
        String src = emitter.emit(struct("S", Optional.empty(), List.of(
                field("a", "int", Cardinality.ZERO_TO_ONE),
                field("b", "int", Cardinality.ONE_TO_ONE))));
        assertEquals("(None, None, 5)", runDecl("", src, "(S().a, S().b, S(b=5).b)"));
    }

    @Test void dataclassInheritanceComposesBaseAndOwnFields() throws Exception {
        Assumptions.assumeTrue(pythonAvailable(), "python not on PATH — parse-and-run skipped");
        // Base and Child both @dataclass; Child(Base) composes the inherited `id` + own `extra` in one ctor.
        String src = emitter.emit(struct("Base", Optional.empty(),
                        List.of(field("id", "string", Cardinality.ONE_TO_ONE)))) + "\n\n"
                + emitter.emit(struct("Child", Optional.of(typeRef("Base")),
                        List.of(field("extra", "int", Cardinality.ZERO_TO_ONE))));
        assertEquals("('x', 3)", runDecl("", src, "(Child(id='x', extra=3).id, Child(id='x', extra=3).extra)"));
    }
}
