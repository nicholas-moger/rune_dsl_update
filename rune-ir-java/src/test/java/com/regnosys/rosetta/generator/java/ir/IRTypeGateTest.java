package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ir.adapter.IRFieldNode;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.ir.core.IRTypeParameter;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.RBasicType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.3 seat 7 (PR #643) - THE TYPE GATE, WITNESSED. Two halves, both of them here.
 *
 * <p>THE DERIVATION ({@link IRJavaTypeNames}): every rung of the number ladder, every builtin and record class, the
 * {@code Void} a model-declared basic type takes, the keyword-escaping of a namespace segment, the bare name of an
 * empty namespace - and every REFUSAL by name, because a derivation that cannot refuse is a derivation that guesses.
 *
 * <p>THE RECONCILE ({@link IRDeclarationReconciler}): the legs the old generator has and the IR route does NOT - a
 * name declared nowhere, and its three-entry alias table - read RED by the fact's own name, so the gate can say which
 * leg answered rather than only that something disagreed; a LYING effective base is caught by the arguments AND by
 * the Java type it would have produced; and the {@code typeAlias} declaration reconcile is red when a fact is dropped.
 */
class IRTypeGateTest {

    // ---------------------------------------------------------------------------- the derivation, fact by fact

    @Test
    void theNumberLadderReadsEveryRungFromTheArgumentsAlone() {
        assertEquals("java.lang.Integer", of(basic("number"), lit("digits", "3"), lit("fractionalDigits", "0")));
        assertEquals("java.lang.Integer", of(basic("number"), lit("digits", "9"), lit("fractionalDigits", "0")));
        assertEquals("java.lang.Long", of(basic("number"), lit("digits", "10"), lit("fractionalDigits", "0")));
        assertEquals("java.lang.Long", of(basic("number"), lit("digits", "18"), lit("fractionalDigits", "0")));
        assertEquals("java.math.BigInteger", of(basic("number"), lit("digits", "19"), lit("fractionalDigits", "0")));
        assertEquals("java.math.BigDecimal", of(basic("number"), lit("digits", "3")),
                "fractionalDigits absent is not integer-grained: the ladder does not apply");
        assertEquals("java.math.BigDecimal", of(basic("number"), lit("digits", "3"), lit("fractionalDigits", "2")));
        assertEquals("java.math.BigDecimal", of(basic("number")));
    }

    @Test
    void theRegistrySeedsTheLadderSoABareIntIsAnInteger() {
        assertEquals("java.lang.Integer", of(basic("int")),
                "the registry's int carries fractionalDigits: 0 and an unconstrained digit count, which reads 9");
        assertEquals("java.lang.Long", of(basic("int"), lit("digits", "18")));
        assertEquals("java.math.BigInteger", of(basic("int"), lit("digits", "19")));
        assertEquals("java.math.BigDecimal", of(basic("int"), lit("fractionalDigits", "2")),
                "a use site may overlay the seed - and then it is no longer integer-grained");
        assertEquals("java.lang.Integer", of(basic("int"), name("digits", "someParameter")),
                "an argument that passes a parameter through by NAME binds nothing at a use site");
        assertEquals("java.lang.Integer", of(basic("int"), neg("min", "5"), lit("max", "9")),
                "min and max constrain no Java type - but they must read");
    }

    @Test
    void everyBuiltinAndRecordClassIsNamed() {
        assertEquals("java.lang.String", of(basic("string")));
        assertEquals("java.lang.Boolean", of(basic("boolean")));
        assertEquals("java.time.LocalTime", of(basic("time")));
        assertEquals("java.lang.Void", of(basic("nothing")));
        assertEquals("java.lang.Object", of(basic("any")));
        assertEquals("com.rosetta.model.lib.records.Date", of(recordRef("date")));
        assertEquals("java.time.LocalDateTime", of(recordRef("dateTime")));
        assertEquals("java.time.ZonedDateTime", of(recordRef("zonedDateTime")));
        assertEquals("java.lang.Void", of(basic("c15token")),
                "a MODEL-declared basic type is `nothing` - the released 9.83.0 plugin's own law");
        assertEquals("java.lang.Void", of(recordRef("c15stamp")), "and so is a model-declared record type");
    }

    @Test
    void aDeclaredTypeIsNamedByItsEscapedNamespace() {
        assertEquals("cdm.base.datetime.AdjustableDate",
                IRJavaTypeNames.of(declared("AdjustableDate", IRKind.STRUCT, "cdm.base.datetime", "AdjustableDate"), List.of()));
        assertEquals("seat7.pack.Colour",
                IRJavaTypeNames.of(declared("Colour", IRKind.ENUM, "seat7.pack", "Colour"), List.of()));
        assertEquals("seat7.pack.Either",
                IRJavaTypeNames.of(declared("Either", IRKind.CHOICE, "seat7.pack", "Either"), List.of()));
        assertEquals("fpml._new.x.Name",
                IRJavaTypeNames.of(declared("Name", IRKind.STRUCT, "fpml.new.x", "Name"), List.of()),
                "a namespace segment that is a Java keyword takes the escape JavaPackageName gives it");
        assertEquals("Bare", IRJavaTypeNames.of(declared("Bare", IRKind.STRUCT, "", "Bare"), List.of()),
                "an EMPTY namespace spells the bare name");
        assertEquals("seat7.pack.Resolved",
                IRJavaTypeNames.of(declared("pack.Resolved", IRKind.STRUCT, "seat7.pack", "Resolved"), List.of()),
                "the RESOLVED name is the class, whatever the model wrote");
    }

    @Test
    void anAliasReferenceIsTypedByItsCollapsedChainAndNotByItsUseSiteArgumentsTwice() {
        IRType scaled = alias("Scaled", new IREffectiveBase(IRKind.BASIC_TYPE, "number", Optional.empty(),
                List.of(lit("digits", "3"), lit("fractionalDigits", "0"))));
        assertEquals("java.lang.Integer", IRJavaTypeNames.of(scaled, List.of()));
        assertEquals("java.lang.Integer", IRJavaTypeNames.of(scaled, List.of(lit("digits", "19"))),
                "the use-site arguments are substituted into the effective base already - applying them again is a lie");
        assertEquals("java.lang.String", IRJavaTypeNames.of(alias("Text", new IREffectiveBase(IRKind.BASIC_TYPE,
                "string", Optional.empty(), List.of(lit("maxLength", "35")))), List.of()));
        assertEquals("com.rosetta.model.lib.records.Date", IRJavaTypeNames.of(alias("ISODate",
                new IREffectiveBase(IRKind.RECORD_TYPE, "date", Optional.empty(), List.of())), List.of()));
        assertEquals("seat7.pack.Target", IRJavaTypeNames.of(alias("Aliased",
                new IREffectiveBase(IRKind.STRUCT, "Target", Optional.of("seat7.pack"), List.of())), List.of()),
                "a chain may end on a declaration, and then the declaration is the class");
    }

    // ------------------------------------------------------------------------------------- the refusals, by name

    @Test
    void anUnresolvedReferenceIsRefusedRatherThanTypedByAWorkspaceFallback() {
        assertTrue(refusal(() -> IRJavaTypeNames.of(
                        IRTypeNode.reference("Ghost", IRKind.STRUCT, Optional.empty(), Optional.empty()), List.of()))
                        .contains("IR type gate: unresolved reference 'Ghost'"),
                "the three UNLINKED workspace fallbacks of the old generator are refused on the IR route (its post-resolution"
                        + " bypass never reaches here - it is caught on .kind / .legacyDeclaration / .javaType)");
        assertTrue(refusal(() -> IRJavaTypeNames.of(
                IRTypeNode.reference("Nameless", IRKind.BASIC_TYPE, Optional.empty(), Optional.empty()), List.of()))
                .contains("unresolved reference 'Nameless'"), "a builtin with no resolved name names nothing");
        assertTrue(refusal(() -> IRJavaTypeNames.of(
                IRTypeNode.reference("Half", IRKind.ENUM, Optional.of("seat7.pack"), Optional.empty()), List.of()))
                .contains("unresolved reference 'Half'"), "a namespace without a name resolves nothing");
    }

    @Test
    void anAliasChainThatDidNotCollapseIsRefused() {
        assertTrue(refusal(() -> IRJavaTypeNames.of(IRTypeNode.reference("Cyclic", IRKind.TYPE_ALIAS,
                        Optional.of("seat7.pack"), Optional.of("Cyclic")), List.of()))
                        .contains("IR type gate: the alias chain of 'Cyclic' did not collapse"),
                "the absence of an effective base IS the fact - a backend refuses such a reference");
    }

    @Test
    void aKindThatNamesNoValueTypeIsRefusedByName() {
        assertTrue(refusal(() -> IRJavaTypeNames.of(
                IRTypeNode.reference("scheme", IRKind.META_TYPE, Optional.empty(), Optional.of("scheme")), List.of()))
                .contains("a reference of kind META_TYPE names no value type: 'scheme'"));
        assertTrue(refusal(() -> IRJavaTypeNames.of(
                IRTypeNode.reference("id", IRKind.FIELD, Optional.empty(), Optional.of("id")), List.of()))
                .contains("a reference of kind FIELD names no value type: 'id'"));
    }

    /**
     * THE ESCAPE REFUSAL (PR #644, the banked cq NIT-2 of #643): a namespace segment that no {@code _} prefix makes a
     * Java identifier ({@code JavaPackageName.escape} throws) is a NAMED refusal carrying {@link IRJavaTypeNames.Reason#ESCAPE},
     * not an {@code IllegalStateException} thrown past the reconcile. A keyword segment is NOT a refusal - it escapes.
     */
    @Test
    void aNamespaceNoPrefixCanEscapeIsRefusedByName() {
        IRJavaTypeNames.Refusal refused = assertThrows(IRJavaTypeNames.Refusal.class,
                () -> IRJavaTypeNames.of(declared("X", IRKind.STRUCT, "seat7.bad-segment", "X"), List.of()));
        assertTrue(refused.getMessage().contains("IR type gate: the namespace 'seat7.bad-segment' of 'X' cannot be escaped to a Java package"),
                refused.getMessage());
        assertEquals(IRJavaTypeNames.Reason.ESCAPE, refused.reason());
        assertTrue(refused.getCause() instanceof IllegalStateException, "the old generator's own throw is carried as the cause");
        assertEquals("fpml._new.x.Name", IRJavaTypeNames.of(declared("Name", IRKind.STRUCT, "fpml.new.x", "Name"), List.of()),
                "a keyword segment escapes - it is no refusal");
    }

    /**
     * A REFUSAL CARRIES ITS REASON (PR #644, the banked cq NIT-3 of #643): the reconcile's {@code .javaType} fact reads
     * {@code REFUSED:<reason>}, so two refusals for different reasons never read EQUAL - and the OLD GENERATOR'S side is
     * classed from its own throw sites to the same vocabulary, so a paired refusal reads the same token on both halves.
     */
    @Test
    void aRefusalCarriesItsReasonAndTheTwoHalvesShareTheVocabulary() {
        assertEquals(IRJavaTypeNames.Reason.PATTERN, reason(() -> of(basic("pattern"))));
        assertEquals(IRJavaTypeNames.Reason.ARGUMENT, reason(() -> of(basic("number"), lit("digits", "ten"))));
        assertEquals(IRJavaTypeNames.Reason.UNRESOLVED, reason(() -> IRJavaTypeNames.of(
                IRTypeNode.reference("Ghost", IRKind.STRUCT, Optional.empty(), Optional.empty()), List.of())));
        assertEquals(IRJavaTypeNames.Reason.COLLAPSE, reason(() -> IRJavaTypeNames.of(IRTypeNode.reference("Cyclic",
                IRKind.TYPE_ALIAS, Optional.of("seat7.pack"), Optional.of("Cyclic")), List.of())));
        assertEquals(IRJavaTypeNames.Reason.NO_VALUE_TYPE, reason(() -> IRJavaTypeNames.of(
                IRTypeNode.reference("scheme", IRKind.META_TYPE, Optional.empty(), Optional.of("scheme")), List.of())));
        // NO_JAVA_TYPE is the defensive arm: every builtin the default registry knows is mapped or refused above it,
        // so it is unreachable over that registry and has no witness here - it is stated in the enum so the fold
        // can never hide a registry row the D4 table maps to nothing
        assertEquals("REFUSED:pattern", IRJavaTypeNames.Reason.PATTERN.token());
        assertNotEquals(IRJavaTypeNames.Reason.PATTERN.token(), IRJavaTypeNames.Reason.ESCAPE.token(),
                "two reasons, two tokens - the fold this replaces read them EQUAL");

        // the old generator's side, classed from its own throw sites - no message is read
        assertEquals("REFUSED:pattern",
                IRJavaTypeNames.legacyRefusalToken(RBasicType.PATTERN, new IllegalStateException("whatever it said")));
        assertEquals("REFUSED:pattern", IRJavaTypeNames.legacyRefusalToken(
                new RAliasType("P", java.util.Map.of(), RBasicType.PATTERN), new IllegalStateException("through an alias")),
                "the leaf is read through the alias chain");
        assertEquals("REFUSED:escape",
                IRJavaTypeNames.legacyRefusalToken(RBasicType.BOOLEAN, new IllegalStateException("`a-b` cannot be escaped")),
                "the only other IllegalStateException a resolved reference reaches is JavaPackageName.escape's");
        assertEquals("REFUSED:legacy-UnsupportedOperationException",
                IRJavaTypeNames.legacyRefusalToken(RBasicType.ANY, new UnsupportedOperationException("x")),
                "an unforeseen throw is classed by its own name - it can never read equal to an IR reason");
    }

    @Test
    void patternAndAnUnparseableArgumentAreRefused() {
        assertTrue(refusal(() -> of(basic("pattern"))).contains("IR type gate: pattern has no Java type"));
        assertTrue(refusal(() -> of(basic("number"), lit("digits", "ten")))
                .contains("IR type gate: unparseable argument 'digits' = 'ten'"));
        assertTrue(refusal(() -> of(basic("number"), lit("fractionalDigits", "9999999999999999999")))
                .contains("unparseable argument 'fractionalDigits'"), "an int slot that overflows is stated intent dropped");
        assertTrue(refusal(() -> of(basic("number"), lit("min", "abc")))
                .contains("IR type gate: unparseable argument 'min' = 'abc'"));
        assertTrue(refusal(() -> of(basic("number"), lit("fractionalDigits", "0"), lit("max", "x9")))
                .contains("unparseable argument 'max'"), "a decimal slot constrains no type but must still read");
    }

    // --------------------------------------------------------------------------------- the reconcile, leg by leg

    private static final String LIB = """
            namespace seat7.pack
            version "1.0.0"

            type Target:
                id string (1..1)
            """;

    private static final String FALLBACKS = """
            namespace seat7.fallback
            version "1.0.0"

            type Fallbacks:
                ghost Ghost (1..1)
                calc calculation (1..1)
            """;

    private static final String ALIASES = """
            namespace seat7.aliases
            version "1.0.0"

            typeAlias Small: number(digits: 3, fractionalDigits: 0)

            typeAlias Scaled(scale int): number(digits: 10, fractionalDigits: scale)

            typeAlias Fixed(n number(fractionalDigits: 0)): number(digits: n)

            type Carrier:
                v Small (1..1)
            """;

    /**
     * An attribute typed {@code pattern} - the ONE builtin both halves REFUSE (the D4 table has no Java mapping for
     * it): the reconcile must read the SAME reason token on both, {@code REFUSED:pattern} (PR #644, cq NIT-3).
     */
    private static final String PATTERNS = """
            namespace seat7.patterns
            version "1.0.0"

            type Matcher:
                p pattern (1..1)
            """;

    /**
     * A type whose attribute names {@code Target} - a data type the workspace DOES declare. This model is parsed
     * outside the workspace, so the linker never sets the call's {@code referencedTypeId}: exactly the shape the old
     * generator's simple-name workspace scan (L3) exists to answer.
     */
    private static final String DETACHED = """
            namespace seat7.detached
            version "1.0.0"

            type Shadow:
                t Target (1..1)
            """;

    /**
     * A type whose attribute names {@code Small} - a {@code typeAlias} the workspace DOES declare - parsed outside the
     * workspace, so the linker never sets the call's {@code referencedTypeId}: the shape the old generator's alias search
     * (L4) answers, because its simple-name scan (L3) matches data types, enums and choices only.
     */
    private static final String DETACHED_ALIAS = """
            namespace seat7.detachedalias
            version "1.0.0"

            type Shade:
                s Small (1..1)
            """;

    private record Fixture(RModel lib, RModel fallbacks, RModel aliases, RModel patterns, GeneratorModel gm) {
    }

    private static Fixture fixture() {
        RModel lib = AstBuilder.buildFromString(LIB, "seat7-lib.rosetta");
        RModel fallbacks = AstBuilder.buildFromString(FALLBACKS, "seat7-fallbacks.rosetta");
        RModel aliases = AstBuilder.buildFromString(ALIASES, "seat7-aliases.rosetta");
        RModel patterns = AstBuilder.buildFromString(PATTERNS, "seat8-patterns.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(lib, fallbacks, aliases, patterns)).workspace();
        return new Fixture(lib, fallbacks, aliases, patterns, new GeneratorModel(ws, m -> true));
    }

    private static IRModelObjectGenerator generator(Fixture f) {
        JavaTypeUtil tu = new JavaTypeUtil();
        return new IRModelObjectGenerator(f.gm(), new JavaTypeTranslator(tu), tu);
    }

    /**
     * A NAME DECLARED NOWHERE. Both halves agree that nothing resolved it - the {@code .resolution} fact is GREEN,
     * and saying so is the point: the divergence is not which leg answered but WHAT it answered. The old generator
     * types the attribute {@code java.lang.Object} (its {@code RMissingType} maps there); the IR route refuses, and
     * {@code .javaType} is red by name.
     */
    @Test
    void aNameDeclaredNowhereIsRedOnTheJavaTypeAndGreenOnTheLeg() {
        Fixture f = fixture();
        List<String> mismatches = messages(generator(f).reconcile(f.fallbacks()));
        assertEquals(1, named(mismatches, "field ghost.type.javaType").size(),
                "the Java type of an unresolved reference disagrees: " + mismatches);
        assertTrue(named(mismatches, "field ghost.type.javaType").get(0).contains("the source says java.lang.Object")
                        && named(mismatches, "field ghost.type.javaType").get(0).contains("the IR says REFUSED"),
                "Object on one side, a named refusal on the other: " + mismatches);
        assertEquals(List.of(), named(mismatches, "field ghost.type.resolution"),
                "BOTH halves read `unresolved` here - the leg agrees, the answer does not");
    }

    /**
     * THE OLD GENERATOR'S L2 ALIAS TABLE: a workspace WITHOUT the model library types {@code calculation} through a
     * three-entry hard-coded map ({@code calculation} / {@code productType} / {@code eventType} to {@code string}).
     * The linker resolved nothing and the registry has no {@code calculation}, so the IR reference is unresolved -
     * and the {@code .resolution} fact names the leg: {@code workspace-fallback} against {@code unresolved}.
     */
    @Test
    void theOldGeneratorsAliasTableIsRedByTheLegsOwnName() {
        Fixture f = fixture();
        List<String> mismatches = messages(generator(f).reconcile(f.fallbacks()));
        List<String> leg = named(mismatches, "field calc.type.resolution");
        assertEquals(1, leg.size(), "the leg that answered must be a fact of its own: " + mismatches);
        assertTrue(leg.get(0).contains("the source says workspace-fallback") && leg.get(0).contains("the IR says unresolved"),
                leg.get(0));
        List<String> java = named(mismatches, "field calc.type.javaType");
        assertEquals(1, java.size(), "and the Java type it invented is red beside it: " + mismatches);
        assertTrue(java.get(0).contains("the source says java.lang.String") && java.get(0).contains("the IR says REFUSED"),
                java.get(0));
    }

    /**
     * THE OLD GENERATOR'S L3 SIMPLE-NAME FIRST MATCH: a type call the linker never resolved, whose written name
     * happens to match a data type declared SOMEWHERE in the loaded workspace, is answered by a workspace scan. The
     * model is parsed OUTSIDE the workspace, so its {@code referencedTypeId} is unset exactly as the L3 leg's own
     * population is - the AST expresses it without any mutation of a frozen node.
     */
    @Test
    void theOldGeneratorsSimpleNameFirstMatchIsRedByTheLegsOwnName() {
        Fixture f = fixture();
        RModel detached = AstBuilder.buildFromString(DETACHED, "seat7-detached.rosetta");
        List<String> mismatches = messages(generator(f).reconcile(detached));
        List<String> leg = named(mismatches, "field t.type.resolution");
        assertEquals(1, leg.size(), "the workspace scan must be named, not silently accepted: " + mismatches);
        assertTrue(leg.get(0).contains("the source says workspace-fallback") && leg.get(0).contains("the IR says unresolved"),
                leg.get(0));
        assertTrue(named(mismatches, "field t.type.javaType").get(0).contains("the source says seat7.pack.Target"),
                "the old generator typed it from the scan; the IR route refuses: " + mismatches);
    }

    /**
     * THE OLD GENERATOR'S L4 ALIAS SEARCH (PR #643 round 1, spec SF-2): a type call the linker never resolved, whose
     * written name matches a {@code typeAlias} declared SOMEWHERE in the loaded workspace, is answered by the alias search
     * ({@code resolveAliasNodeFromWorkspace}) - the L3 scan matches data types, enums and choices only, so an alias name falls
     * to this leg. The
     * old generator then types the attribute through the alias's body ({@code Small} = {@code number(digits: 3,
     * fractionalDigits: 0)} - {@code Integer}); the IR route refuses, and both the leg and the invented Java type are red.
     */
    @Test
    void theOldGeneratorsAliasSearchIsRedByTheLegsOwnName() {
        Fixture f = fixture();
        RModel detached = AstBuilder.buildFromString(DETACHED_ALIAS, "seat7-detached-alias.rosetta");
        List<String> mismatches = messages(generator(f).reconcile(detached));
        List<String> leg = named(mismatches, "field s.type.resolution");
        assertEquals(1, leg.size(), "the alias search must be named, not silently accepted: " + mismatches);
        assertTrue(leg.get(0).contains("the source says workspace-fallback") && leg.get(0).contains("the IR says unresolved"),
                leg.get(0));
        List<String> java = named(mismatches, "field s.type.javaType");
        assertEquals(1, java.size(), "and the Java type the alias search invented is red beside it: " + mismatches);
        assertTrue(java.get(0).contains("the source says java.lang.Integer") && java.get(0).contains("the IR says REFUSED"),
                "the old generator typed it through the alias's body (the L4 leg, not the L3 scan); the IR route refuses: " + java.get(0));
    }

    /**
     * A LYING EFFECTIVE BASE: the same alias reference, re-stated with {@code digits: 19} where the source says
     * {@code 3}. Two independent facts catch it - the reconciler's OWN walk of the chain ({@code effectiveBase
     * .arguments}, and the belt {@code effectiveBase.number} anchored to the parser's solver) and the Java type the
     * lie would have produced ({@code BigInteger} where the old generator says {@code Integer}).
     */
    @Test
    void aLyingEffectiveBaseIsRedOnTheArgumentsAndOnTheJavaType() {
        Fixture f = fixture();
        IRDeclarationReconciler r = new IRDeclarationReconciler(f.gm());
        RDataType ast = dataType(f.aliases(), "Carrier");
        IRTypeNode honest = r.adapter().adaptData(f.aliases().namespace(), ast);
        assertEquals(List.of(), r.reconcileData(f.aliases().namespace(), ast, honest), "the source's own answer is green");

        IRField field = honest.fields().get(0);
        IRType lying = IRTypeNode.reference(field.type().name(), IRKind.TYPE_ALIAS, field.type().namespace(),
                field.type().resolvedName(), Optional.of(new IREffectiveBase(IRKind.BASIC_TYPE, "number",
                        Optional.empty(), List.of(lit("digits", "19"), lit("fractionalDigits", "0")))));
        IRTypeNode mangled = withSingleField(honest, new IRFieldNode(field.name(), lying, field.cardinality(),
                field.sourceRange(), field.metadata(), field.bounds(), field.isOverride(), field.typeArguments(),
                field.definition(), field.docReferences(), field.annotations(), field.labels(), field.ruleReferences()));
        List<String> mismatches = r.reconcileData(f.aliases().namespace(), ast, mangled);
        assertEquals(1, named(mismatches, "field v.type.effectiveBase.arguments").size(),
                "the reconciler's own walk of the chain catches it: " + mismatches);
        assertTrue(named(mismatches, "field v.type.javaType").get(0).contains("the source says java.lang.Integer")
                        && named(mismatches, "field v.type.javaType").get(0).contains("the IR says java.math.BigInteger"),
                "and so does the Java type it would have emitted: " + mismatches);
        assertEquals(1, named(mismatches, "field v.type.effectiveBase.number").size(),
                "the belt, anchored to the parser's own solver, is a third producer: " + mismatches);
    }

    /**
     * THE {@code typeAlias} DECLARATION RECONCILE: the adapter's node agrees with the source on every fact, and a node
     * whose declared parameters were DROPPED is red by that fact's own name.
     */
    @Test
    void theTypeAliasDeclarationReconcilesAndADroppedParameterIsRed() {
        Fixture f = fixture();
        IRDeclarationReconciler r = new IRDeclarationReconciler(f.gm());
        RTypeAlias ast = f.aliases().rootElements().stream().filter(RTypeAlias.class::isInstance)
                .map(RTypeAlias.class::cast).filter(a -> a.name().equals("Scaled")).findFirst().orElseThrow();
        IRTypeNode node = r.adapter().adaptTypeAlias(f.aliases().namespace(), ast);
        assertEquals(List.of(), r.reconcileTypeAlias(f.aliases().namespace(), ast, node),
                "every fact of the alias declaration agrees with the source");
        assertTrue(r.stats()[1] > 10, "a reconcile that asserts nothing is not a gate: " + r.stats()[1]);

        IRTypeNode dropped = new IRTypeNode(node.name(), node.kind(), node.fields(), node.baseType(),
                node.isAbstract(), node.sourceRange(), node.metadata(), node.namespace(), node.resolvedName(),
                node.definition(), node.docReferences(), node.annotations(), node.conditionNames(),
                node.effectiveBase(), List.of(), node.baseTypeArguments());
        List<String> mismatches = r.reconcileTypeAlias(f.aliases().namespace(), ast, dropped);
        assertEquals(1, named(mismatches, "typeParameters.size").size(),
                "a dropped parameter is red by that fact's own name: " + mismatches);
    }

    /**
     * A PARAMETER'S OWN TYPE ARGUMENTS (PR #644, the banked cq SF-1 of #643): {@code Fixed(n number(fractionalDigits: 0))}
     * reconciles GREEN with the arguments carried - the parameter's Java type is integer-grained on BOTH halves - and a
     * node whose parameter DROPPED them is red twice: on the new {@code param n.typeArguments} fact by name, and on
     * {@code param n.type.javaType}, where the IR side would have read {@code BigDecimal} against the source's
     * integer type. Before this fact the second red was silent: the IR half was handed {@code List.of()}.
     */
    @Test
    void aParametersOwnTypeArgumentsAreAFactAndDroppingThemIsRedOnTheJavaTypeToo() {
        Fixture f = fixture();
        IRDeclarationReconciler r = new IRDeclarationReconciler(f.gm());
        RTypeAlias ast = f.aliases().rootElements().stream().filter(RTypeAlias.class::isInstance)
                .map(RTypeAlias.class::cast).filter(a -> a.name().equals("Fixed")).findFirst().orElseThrow();
        IRTypeNode node = r.adapter().adaptTypeAlias(f.aliases().namespace(), ast);
        assertEquals(List.of("fractionalDigits"), node.typeParameters().get(0).typeArguments().stream()
                .map(IRTypeArgument::parameter).toList(), "the adapter carries the parameter's own arguments");
        assertEquals(List.of(), r.reconcileTypeAlias(f.aliases().namespace(), ast, node),
                "with the arguments carried, every fact of the parameter agrees with the source");

        IRTypeParameter n = node.typeParameters().get(0);
        IRTypeNode dropped = new IRTypeNode(node.name(), node.kind(), node.fields(), node.baseType(),
                node.isAbstract(), node.sourceRange(), node.metadata(), node.namespace(), node.resolvedName(),
                node.definition(), node.docReferences(), node.annotations(), node.conditionNames(),
                node.effectiveBase(), List.of(new IRTypeParameter(n.name(), n.type(), n.definition(), List.of())),
                node.baseTypeArguments());
        List<String> mismatches = r.reconcileTypeAlias(f.aliases().namespace(), ast, dropped);
        assertEquals(1, named(mismatches, "param n.typeArguments").size(),
                "the dropped arguments are red by the fact's own name: " + mismatches);
        List<String> java = named(mismatches, "param n.type.javaType");
        assertEquals(1, java.size(), "and the Java type the IR side would have derived without them is red too: " + mismatches);
        assertTrue(java.get(0).contains("the source says java.lang.Integer") && java.get(0).contains("the IR says java.math.BigDecimal"),
                "the old generator applies the parameter's own arguments; the IR half without them is not integer-grained: " + java.get(0));
    }

    /**
     * THE PAIRED REFUSAL (PR #644, cq NIT-3): an attribute typed {@code pattern} is refused by BOTH halves, and the
     * {@code .javaType} fact must read the same REASON on both - {@code REFUSED:pattern} - so the reconcile is GREEN
     * for the right reason, and a fold of the tokens (both halves reading a bare {@code REFUSED}) would be no witness.
     */
    @Test
    void aPatternAttributeIsRefusedByBothHalvesForTheSameReason() {
        Fixture f = fixture();
        IRDeclarationReconciler r = new IRDeclarationReconciler(f.gm());
        RDataType ast = dataType(f.patterns(), "Matcher");
        IRTypeNode node = r.adapter().adaptData(f.patterns().namespace(), ast);
        assertEquals(List.of(), r.reconcileData(f.patterns().namespace(), ast, node),
                "both halves refuse pattern with the same reason token");

        // the IR half LYING about the reason: the same attribute re-stated as an unresolved reference reads RED on the
        // Java type with BOTH tokens named - the fact asserts the reason, not the fold
        IRField field = node.fields().get(0);
        IRTypeNode lying = withSingleField(node, new IRFieldNode(field.name(),
                IRTypeNode.reference("pattern", IRKind.BASIC_TYPE, Optional.empty(), Optional.empty()), field.cardinality(),
                field.sourceRange(), field.metadata(), field.bounds(), field.isOverride(), field.typeArguments(),
                field.definition(), field.docReferences(), field.annotations(), field.labels(), field.ruleReferences()));
        List<String> java = named(r.reconcileData(f.patterns().namespace(), ast, lying), "field p.type.javaType");
        assertEquals(1, java.size(), java.toString());
        assertTrue(java.get(0).contains("the source says REFUSED:pattern") && java.get(0).contains("the IR says REFUSED:unresolved"),
                "two refusals for different reasons read RED by their reasons: " + java.get(0));
    }

    // ------------------------------------------------------------------------------------------------- helpers

    /** The reason of the {@link IRJavaTypeNames.Refusal} the call must raise. */
    private static IRJavaTypeNames.Reason reason(Runnable call) {
        return assertThrows(IRJavaTypeNames.Refusal.class, call::run).reason();
    }

    private static String of(IRType reference, IRTypeArgument... arguments) {
        return IRJavaTypeNames.of(reference, List.of(arguments));
    }

    private static IRType basic(String name) {
        return IRTypeNode.reference(name, IRKind.BASIC_TYPE, Optional.empty(), Optional.of(name));
    }

    private static IRType recordRef(String name) {
        return IRTypeNode.reference(name, IRKind.RECORD_TYPE, Optional.empty(), Optional.of(name));
    }

    private static IRType declared(String written, IRKind kind, String namespace, String resolvedName) {
        return IRTypeNode.reference(written, kind, Optional.of(namespace), Optional.of(resolvedName));
    }

    private static IRType alias(String written, IREffectiveBase base) {
        return IRTypeNode.reference(written, IRKind.TYPE_ALIAS, Optional.of("seat7.pack"), Optional.of(written),
                Optional.of(base));
    }

    private static IRTypeArgument lit(String parameter, String value) {
        return new IRTypeArgument(parameter, Optional.empty(), Optional.of(value), false);
    }

    private static IRTypeArgument neg(String parameter, String value) {
        return new IRTypeArgument(parameter, Optional.empty(), Optional.of(value), true);
    }

    private static IRTypeArgument name(String parameter, String value) {
        return new IRTypeArgument(parameter, Optional.of(value), Optional.empty(), false);
    }

    /** The message of the {@link GenerationException} the call must raise. */
    private static String refusal(Runnable call) {
        return assertThrows(GenerationException.class, call::run).getMessage();
    }

    private static List<String> messages(List<GenerationException> errors) {
        List<String> messages = new ArrayList<>();
        for (GenerationException error : errors) {
            messages.add(error.getMessage());
        }
        return messages;
    }

    /** Every mismatch naming {@code fact} - the fact's own name, which is what a mutation lane greps. */
    private static List<String> named(List<String> messages, String fact) {
        List<String> hits = new ArrayList<>();
        for (String message : messages) {
            if (message.contains(": " + fact + " - ")) {
                hits.add(message);
            }
        }
        return hits;
    }

    private static RDataType dataType(RModel model, String name) {
        return model.rootElements().stream().filter(RDataType.class::isInstance).map(RDataType.class::cast)
                .filter(d -> d.name().equals(name)).findFirst().orElseThrow();
    }

    private static IRTypeNode withSingleField(IRTypeNode node, IRField field) {
        return new IRTypeNode(node.name(), node.kind(), List.of(field), node.baseType(), node.isAbstract(),
                node.sourceRange(), node.metadata(), node.namespace(), node.resolvedName(), node.definition(),
                node.docReferences(), node.annotations(), node.conditionNames(), node.effectiveBase(),
                node.typeParameters(), node.baseTypeArguments());
    }
}
