package com.regnosys.rosetta.ir.adapter;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.ir.core.IRAliasLink;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.ir.core.IRTypeParameter;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.3 seat 7 (PR #643 — the type gate), the ADAPTER half: a {@code typeAlias} DECLARATION adapted to a TYPE_ALIAS
 * {@link IRTypeNode}, and every alias REFERENCE carrying its chain COLLAPSED at that use site
 * ({@link IREffectiveBase}). One two-namespace fixture witnesses every arm of the law — the registry leaf seeded
 * with its own constraints, the string constraints in canonical order, the alias-of-alias chain (across a
 * namespace), a use-site literal binding a parameter and a bare use leaving it absent, a negated literal, and a
 * body naming a data type.
 *
 * <p><b>Which arm the builtins take here:</b> the fixture is parsed from STRINGS ALONE — the model library's
 * {@code basictypes.rosetta} is NOT loaded — so {@code int} / {@code number} / {@code string} / {@code date} /
 * {@code boolean} resolve to NO declaration and the walk takes the by-NAME registry arm (the law's arm h), the
 * same arm the IR-declaration fixtures of seat 5 take. The declaration arm (g) — a body resolving to a
 * {@code basicType} / {@code recordType} DECLARATION — is the one a library-loaded workspace takes; it reaches
 * the SAME registry by the declaration's own name, and a model-declared one is {@code nothing}.
 */
class AstToIRAdapterTypeAliasTest {

    private static final String OTHER = """
            namespace seat7.other
            version "1.0.0"

            typeAlias Text35: <"At most 35 characters."> string(maxLength: 35)

            type Far: <"A far type.">
                id string (1..1)
            """;

    private static final String ALIASES = """
            namespace seat7.aliases
            version "1.0.0"

            import seat7.other.*

            typeAlias Int3: <"Three digits."> int(digits: 3)

            typeAlias Big: number(digits: 19, fractionalDigits: 0)

            typeAlias Day: date

            typeAlias Flag: boolean

            typeAlias Twice: Text35

            typeAlias Code: string(minLength: 3, maxLength: 3, pattern: "[A-Z]{3}")

            typeAlias Scaled(scale int <"The scale.">): <"A scaled number."> number(fractionalDigits: scale, digits: 10)

            typeAlias Fixed(n number(fractionalDigits: 0, min: -1), s string(maxLength: 5)): number(digits: n)

            typeAlias Neg: number(min: -5)

            typeAlias FarAlias: Far

            typeAlias Checked: string(maxLength: 10)
                condition Size:
                    item exists
                condition:
                    item exists

            typeAlias ChainInner(scale int): number(fractionalDigits: scale)

            typeAlias ChainOuter: <"The outer link."> ChainInner(scale: 0)
                condition Range:
                    item exists

            type Holder:
                i Int3 (1..1)
                big Big (0..1)
                text Text35 (1..1)
                twice Twice (1..1)
                day Day (1..1)
                flag Flag (1..1)
                code Code (1..1)
                scaled Scaled(scale: 2) (1..1)
                bare Scaled (1..1)
                neg Neg (1..1)
                far FarAlias (1..1)
                plain Far (1..1)
                chained ChainOuter (1..1)
            """;

    private record Fixture(RModel other, RModel aliases, RWorkspace workspace) {
    }

    private final AstToIRAdapter adapter = new AstToIRAdapter();

    private static Fixture fixture() {
        RModel other = AstBuilder.buildFromString(OTHER, "seat7-other.rosetta");
        RModel aliases = AstBuilder.buildFromString(ALIASES, "seat7-aliases.rosetta");
        RWorkspace workspace = RWorkspace.build(List.of(other, aliases)).workspace();
        return new Fixture(other, aliases, workspace);
    }

    // ------------------------------------------------------------------------------------------- the fixture

    @Test
    void theFixtureParsedEveryAliasDeclaration() {
        Fixture f = fixture();
        assertEquals(List.of("Text35"), aliasNames(f.other()));
        assertEquals(List.of("Int3", "Big", "Day", "Flag", "Twice", "Code", "Scaled", "Fixed", "Neg", "FarAlias",
                        "Checked", "ChainInner", "ChainOuter"),
                aliasNames(f.aliases()), "a dropped alias is a silently narrower witness");
    }

    // ------------------------------------------------------------------------------- the DECLARATION node

    @Test
    void aTypeAliasRootElementAdaptsToATypeAliasNodeCarryingItsBody() {
        Fixture f = fixture();
        IRTypeNode int3 = aliasNode(f.aliases(), "Int3");

        assertEquals("seat7.aliases.Int3", int3.name());
        assertEquals(IRKind.TYPE_ALIAS, int3.kind());
        assertEquals(Optional.of("seat7.aliases"), int3.namespace());
        assertEquals(Optional.of("Three digits."), int3.definition());
        assertEquals(Optional.empty(), int3.resolvedName());
        assertEquals(List.of(), int3.fields());
        assertFalse(int3.isAbstract(), "an alias declares nothing abstract");
        assertEquals(List.of(), int3.docReferences());
        assertEquals(List.of(), int3.annotations());
        assertEquals(List.of(), int3.conditionNames());
        assertEquals(List.of(), int3.typeParameters());
        assertEquals(Optional.empty(), int3.effectiveBase(),
                "the collapse is a property of a USE SITE — a declaration node states none");

        IRType body = int3.baseType().orElseThrow();
        assertEquals("int", body.name());
        assertEquals(IRKind.BASIC_TYPE, body.kind(), "the registry arm classes an unresolved builtin name");
        assertEquals(Optional.of("int"), body.resolvedName());
        assertEquals(Optional.empty(), body.namespace());
        assertEquals(Optional.empty(), body.effectiveBase(),
                "only a TYPE_ALIAS reference collapses — this body names a builtin");
        assertEquals(List.of("digits=3"), asWritten(int3.baseTypeArguments()));
    }

    @Test
    void aParametricAliasCarriesItsParametersAndItsBodyArgumentsAsWritten() {
        Fixture f = fixture();
        IRTypeNode scaled = aliasNode(f.aliases(), "Scaled");

        assertEquals("seat7.aliases.Scaled", scaled.name());
        assertEquals(Optional.of("A scaled number."), scaled.definition());
        assertEquals(1, scaled.typeParameters().size());
        IRTypeParameter scale = scaled.typeParameters().get(0);
        assertEquals("scale", scale.name());
        assertEquals("int", scale.type().name());
        assertEquals(IRKind.BASIC_TYPE, scale.type().kind());
        assertEquals(Optional.of("The scale."), scale.definition());
        assertEquals(List.of(), scale.typeArguments(), "a bare `int` parameter wrote no arguments of its own");

        assertEquals(List.of("fractionalDigits=name:scale", "digits=10"), asWritten(scaled.baseTypeArguments()),
                "the body's arguments are carried AS WRITTEN, in the body's own order");
        assertEquals("number", scaled.baseType().orElseThrow().name());
    }

    /**
     * A parameter's OWN type call's arguments (PR #644, the banked cq SF-1 of #643): grammar-admitted
     * ({@code typeParameter: typeParameterValidID typeCall definable?}), and before this fact the adapter dropped them -
     * a {@code number(fractionalDigits: 0)} parameter lost its integer grain silently.
     */
    @Test
    void aParameterCarriesTheArgumentsItsOwnTypeCallWrote() {
        Fixture f = fixture();
        IRTypeNode fixed = aliasNode(f.aliases(), "Fixed");
        assertEquals(2, fixed.typeParameters().size());
        IRTypeParameter n = fixed.typeParameters().get(0);
        IRTypeParameter s = fixed.typeParameters().get(1);
        assertEquals("number", n.type().name());
        assertEquals(List.of("fractionalDigits=0", "min=-1"), asWritten(n.typeArguments()),
                "the parameter's arguments AS WRITTEN, in the call's own order, the sign kept");
        assertEquals("string", s.type().name());
        assertEquals(List.of("maxLength=5"), asWritten(s.typeArguments()));
        assertEquals(List.of("digits=name:n"), asWritten(fixed.baseTypeArguments()),
                "the body passes the parameter through by name, as before");
    }

    @Test
    void anAliasWithoutABodyCarriesNoBaseAndNoArguments() {
        RTypeAlias headless = new RTypeAlias();
        headless.setName("Headless");
        IRTypeNode node = adapter.adaptTypeAlias("seat7.detached", headless);
        assertEquals("seat7.detached.Headless", node.name());
        assertEquals(Optional.empty(), node.baseType());
        assertEquals(List.of(), node.baseTypeArguments());
        assertEquals(Optional.empty(), node.effectiveBase());
    }

    @Test
    void anAliasConditionIsCarriedByNameInSourceOrder() {
        Fixture f = fixture();
        IRTypeNode checked = aliasNode(f.aliases(), "Checked");
        assertEquals(List.of(Optional.of("Size"), Optional.empty()), checked.conditionNames());
        assertEquals(List.of("DataRule", "DataRule"), checked.conditionKinds(),
                "the property gate (PR #644) states a kind for EVERY condition, named ones included");
    }

    // --------------------------------------------------------------- the EFFECTIVE BASE, arm by arm

    @Test
    void aBuiltinBodyCollapsesThroughTheRegistrySeededWithItsOwnConstraints() {
        Fixture f = fixture();
        IRTypeNode holder = dataNode(f.aliases(), "Holder");

        IREffectiveBase int3 = base(holder, "i");
        assertEquals(IRKind.BASIC_TYPE, int3.kind());
        assertEquals("int", int3.name());
        assertEquals(Optional.empty(), int3.namespace());
        assertEquals(List.of("digits=3", "fractionalDigits=0"), arguments(int3),
                "the registry's own int carries fractionalDigits 0 — the body's digits overlays it");

        IREffectiveBase big = base(holder, "big");
        assertEquals("number", big.name());
        assertEquals(List.of("digits=19", "fractionalDigits=0"), arguments(big));

        IREffectiveBase day = base(holder, "day");
        assertEquals(IRKind.RECORD_TYPE, day.kind());
        assertEquals("date", day.name());
        assertEquals(List.of(), arguments(day));

        IREffectiveBase flag = base(holder, "flag");
        assertEquals(IRKind.BASIC_TYPE, flag.kind());
        assertEquals("boolean", flag.name());
        assertEquals(List.of(), arguments(flag));
    }

    @Test
    void aStringAliasStatesItsConstraintsInCanonicalOrder() {
        Fixture f = fixture();
        IREffectiveBase code = base(dataNode(f.aliases(), "Holder"), "code");
        assertEquals(IRKind.BASIC_TYPE, code.kind());
        assertEquals("string", code.name());
        assertEquals(List.of("minLength=3", "maxLength=3", "pattern=\"[A-Z]{3}\""), arguments(code),
                "minLength before maxLength before pattern — the canonical order both halves walk");
    }

    @Test
    void anAliasOfAnAliasCollapsesToTheLeafOfTheChainAcrossNamespaces() {
        Fixture f = fixture();
        IRTypeNode holder = dataNode(f.aliases(), "Holder");

        IREffectiveBase twice = base(holder, "twice");
        assertEquals(IRKind.BASIC_TYPE, twice.kind());
        assertEquals("string", twice.name());
        assertEquals(List.of("maxLength=35"), arguments(twice));

        IRType text = fieldType(holder, "text");
        assertEquals(IRKind.TYPE_ALIAS, text.kind());
        assertEquals(Optional.of("seat7.other"), text.namespace());
        assertEquals(Optional.of("Text35"), text.resolvedName());
        assertEquals(List.of("maxLength=35"), arguments(text.effectiveBase().orElseThrow()));

        IRType body = aliasNode(f.aliases(), "Twice").baseType().orElseThrow();
        assertEquals("Text35", body.name());
        assertEquals(IRKind.TYPE_ALIAS, body.kind());
        assertEquals(Optional.of("seat7.other"), body.namespace());
        assertEquals(List.of("maxLength=35"), arguments(body.effectiveBase().orElseThrow()),
                "the alias's own body reference collapses too");
    }

    @Test
    void aUseSiteLiteralBindsTheParameterAndABareUseLeavesItAbsent() {
        Fixture f = fixture();
        IRTypeNode holder = dataNode(f.aliases(), "Holder");

        IREffectiveBase bound = base(holder, "scaled");
        assertEquals("number", bound.name());
        assertEquals(List.of("digits=10", "fractionalDigits=2"), arguments(bound),
                "scale: 2 reaches fractionalDigits through the body's name value");
        assertEquals(Optional.of("2"), bound.argument("fractionalDigits"));

        IREffectiveBase bare = base(holder, "bare");
        assertEquals(List.of("digits=10"), arguments(bare),
                "an unbound parameter stays ABSENT — an effective base carries literals only");
        assertEquals(Optional.empty(), bare.argument("fractionalDigits"));
    }

    @Test
    void aNegatedLiteralKeepsItsMagnitudeAndItsSign() {
        Fixture f = fixture();
        IREffectiveBase neg = base(dataNode(f.aliases(), "Holder"), "neg");
        assertEquals(List.of(new IRTypeArgument("min", Optional.empty(), Optional.of("5"), true)),
                neg.arguments());
        assertEquals(Optional.of("-5"), neg.argument("min"));
    }

    @Test
    void anAliasBodyNamingADataTypeCollapsesToThatStruct() {
        Fixture f = fixture();
        IREffectiveBase far = base(dataNode(f.aliases(), "Holder"), "far");
        assertEquals(IRKind.STRUCT, far.kind());
        assertEquals("Far", far.name());
        assertEquals(Optional.of("seat7.other"), far.namespace());
        assertEquals("seat7.other.Far", far.qualifiedName());
        assertEquals(List.of(), arguments(far));
    }

    // ------------------------------------------------------- the WALKED chain, beside the collapsed base

    /**
     * v3.3 seat 8 (PR #644 — the property gate): a use site states BOTH halves of its alias chain — the chain
     * COLLAPSED ({@code effectiveBase}, what a Java type is derived from) and the chain WALKED
     * ({@code aliasChain}, the rungs the type-format validator wires conditions from and refuses a whole validator
     * file over). Outermost first: the alias the reference names, then the alias its body names.
     */
    @Test
    void anAliasReferenceCarriesItsWalkedChainOutermostFirstBesideItsCollapsedBase() {
        Fixture f = fixture();
        IRType chained = fieldType(dataNode(f.aliases(), "Holder"), "chained");
        assertEquals(IRKind.TYPE_ALIAS, chained.kind());
        IREffectiveBase base = chained.effectiveBase().orElseThrow();
        assertEquals("number", base.name());
        assertEquals(List.of("fractionalDigits=0"), arguments(base),
                "the collapsed base is unmoved by the chain riding beside it — the outer's literal binds the"
                        + " inner's parameter exactly as before");

        List<IRAliasLink> chain = chained.aliasChain();
        assertEquals(List.of("ChainOuter", "ChainInner"), chain.stream().map(IRAliasLink::name).toList(),
                "outermost first — the alias the reference names, then the alias its body names");

        IRAliasLink outer = chain.get(0);
        assertEquals(Optional.of("seat7.aliases"), outer.namespace());
        assertEquals("seat7.aliases.ChainOuter", outer.qualifiedName());
        assertEquals(List.of(), outer.parameterNames());
        assertFalse(outer.isParameterised(), "the outer link declares no parameter");
        assertEquals(List.of(Optional.of("Range")), outer.conditionNames());
        assertEquals(List.of("DataRule"), outer.conditionKinds());

        IRAliasLink inner = chain.get(1);
        assertEquals(List.of("scale"), inner.parameterNames());
        assertTrue(inner.isParameterised(),
                "a PARAMETERISED alias in the chain is one half of the whole-validator refusal law");
        assertEquals(List.of(), inner.conditionNames());
        assertEquals(List.of(), inner.conditionKinds());
    }

    @Test
    void aOneLinkChainACrossNamespaceChainAndANonAliasReferenceEachReadTheirOwnChain() {
        Fixture f = fixture();
        IRTypeNode holder = dataNode(f.aliases(), "Holder");

        assertEquals(List.of("Int3"), chainNames(fieldType(holder, "i")),
                "a reference to an alias whose body is a builtin walks exactly one link");
        assertEquals(List.of("Twice", "Text35"), chainNames(fieldType(holder, "twice")),
                "the chain crosses the namespace with the body it follows");
        assertEquals(List.of("Text35"), chainNames(fieldType(holder, "text")));
        assertEquals(List.of(), fieldType(holder, "plain").aliasChain(),
                "a reference that is no alias walks no chain — the record refuses one there");
        assertEquals(List.of(), fieldType(dataNode(f.other(), "Far"), "id").aliasChain());
    }

    @Test
    void anAliasDeclarationWalksNoChainWhileItsOwnBodyReferenceDoes() {
        Fixture f = fixture();
        IRTypeNode twice = aliasNode(f.aliases(), "Twice");
        assertEquals(List.of(), twice.aliasChain(),
                "the chain is a property of a USE SITE — a declaration node states none");
        assertEquals(List.of("Text35"), chainNames(twice.baseType().orElseThrow()),
                "the alias's own body IS a use site, so its reference walks the rest of the chain");
        assertEquals(List.of(), aliasNode(f.aliases(), "Int3").baseType().orElseThrow().aliasChain(),
                "a body naming a builtin walks nothing");
    }

    // ------------------------------------------------------------------------------ the absences, enforced

    @Test
    void aReferenceThatIsNoAliasCarriesNoEffectiveBase() {
        Fixture f = fixture();
        IRTypeNode holder = dataNode(f.aliases(), "Holder");
        IRType plain = fieldType(holder, "plain");
        assertEquals(IRKind.STRUCT, plain.kind());
        assertEquals(Optional.empty(), plain.effectiveBase());

        IRType id = fieldType(dataNode(f.other(), "Far"), "id");
        assertEquals(IRKind.BASIC_TYPE, id.kind());
        assertEquals(Optional.empty(), id.effectiveBase());
    }

    @Test
    void anEffectiveBaseIsRefusedOnAReferenceThatIsNoTypeAlias() {
        IREffectiveBase base = new IREffectiveBase(IRKind.STRUCT, "Far", Optional.of("seat7.other"), List.of());
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> IRTypeNode.reference("Far", IRKind.STRUCT, Optional.of("seat7.other"), Optional.of("Far"),
                        Optional.of(base)));
        assertEquals("type Far of kind STRUCT carries an effective base"
                + " - only a TYPE_ALIAS reference collapses to one", refused.getMessage());
    }

    // ---------------------------------------------------------------------------------------- the helpers

    private static List<String> aliasNames(RModel model) {
        return model.rootElements().stream().filter(RTypeAlias.class::isInstance).map(RTypeAlias.class::cast)
                .map(RTypeAlias::name).toList();
    }

    private IRTypeNode aliasNode(RModel model, String name) {
        RTypeAlias ast = model.rootElements().stream().filter(RTypeAlias.class::isInstance)
                .map(RTypeAlias.class::cast).filter(a -> a.name().equals(name)).findFirst().orElseThrow();
        return adaptedType(model, ast);
    }

    private IRTypeNode dataNode(RModel model, String name) {
        RDataType ast = model.rootElements().stream().filter(RDataType.class::isInstance)
                .map(RDataType.class::cast).filter(d -> d.name().equals(name)).findFirst().orElseThrow();
        return adaptedType(model, ast);
    }

    private IRTypeNode adaptedType(RModel model, RRootElement element) {
        IRNode node = adapter.adaptRootElement(model.namespace(), element).orElseThrow();
        return assertInstanceOf(IRTypeNode.class, node);
    }

    private static IRType fieldType(IRTypeNode type, String fieldName) {
        IRField field = type.fields().stream().filter(x -> x.name().equals(fieldName)).findFirst().orElseThrow();
        return field.type();
    }

    private static IREffectiveBase base(IRTypeNode type, String fieldName) {
        IRType reference = fieldType(type, fieldName);
        assertEquals(IRKind.TYPE_ALIAS, reference.kind(), fieldName + " is declared over an alias");
        return reference.effectiveBase().orElseThrow(() -> new AssertionError(
                "the alias chain of " + fieldName + " was not collapsed"));
    }

    /** {@code parameter=value}, a negated literal keeping its {@code -}; an effective base holds literals only. */
    private static List<String> arguments(IREffectiveBase base) {
        return base.arguments().stream()
                .map(a -> a.parameter() + "=" + (a.negated() ? "-" : "") + a.literalValue().orElseThrow())
                .toList();
    }

    /** The names of the alias declarations a reference walked, outermost first. */
    private static List<String> chainNames(IRType reference) {
        return reference.aliasChain().stream().map(IRAliasLink::name).toList();
    }

    /** The same, for arguments AS WRITTEN — where a value may pass a parameter through by name. */
    private static List<String> asWritten(List<IRTypeArgument> written) {
        return written.stream()
                .map(a -> a.parameter() + "=" + a.nameValue().map(n -> "name:" + n)
                        .orElseGet(() -> (a.negated() ? "-" : "") + a.literalValue().orElseThrow()))
                .toList();
    }
}
