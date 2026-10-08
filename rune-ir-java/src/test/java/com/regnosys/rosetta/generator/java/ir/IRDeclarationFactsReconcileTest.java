package com.regnosys.rosetta.generator.java.ir;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ir.adapter.IREnumNode;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.Cardinality;
import com.regnosys.rosetta.ir.core.IRAnnotationUse;
import com.regnosys.rosetta.ir.core.IRBounds;
import com.regnosys.rosetta.ir.core.IRDocReference;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.ir.core.IRTypeParameter;
import com.regnosys.rosetta.symbols.RWorkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.3 seat 5 (decision D55) - THE DECLARATION FACTS, WITNESSED AND RECONCILED. One two-namespace fixture carries
 * every fact the enriched declaration IR states; each fact has (a) a WITNESS here - the IR says what the model
 * wrote - and (b) the RECONCILE: {@link IRDeclarationReconciler} re-reads the source itself and must agree, fact by
 * fact, through the three wired generators. The mutation lanes ({@code lanes-s5c4.sh}) drop each fact in the adapter
 * in turn and read THIS class RED on that fact's own name - the proof the reconcile can fail.
 */
class IRDeclarationFactsReconcileTest {

    private static final String OTHER = """
            namespace seat5.other
            version "1.0.0"

            type Far: <"A far type.">
                id string (1..1)

            enum FarEnum: <"A far enum.">
                A
                B
            """;

    private static final String FACTS = """
            namespace seat5.facts
            version "1.0.0"

            import seat5.other.*

            body Organisation Org1
            corpus Agreement Org1 "Agreement 1" Agr1
            segment name

            typeAlias Short: string(maxLength: 35)

            typeAlias Scaled(scale int): number(digits: 10, fractionalDigits: scale)

            typeAlias Longer: Short

            typeAlias Small: number(digits: 3, fractionalDigits: 0)

            typeAlias Tiny: <"A tiny count."> int(digits: 2)
                condition Positive:
                    item > 0

            enum Base: <"The base enum.">
                [docReference Org1 Agr1 name "base"]
                One displayName "one" <"The first.">
                    [synonym FpML value "ONE" definition "the one" pattern "O.*" "O" removeHtml]
                Two
                    [deprecated]

            enum Child extends Base:
                Three

            choice Pick: <"A pick.">
                Far <"The far option.">
                Near

            type Near: <"A near type.">
                [metadata key]
                [rootType]
                [docReference Org1 Agr1 name "near" provision "The provision."]
                name string (1..1) <"The name.">
                    [metadata scheme]
                    [label "The name label"]
                far Far (0..2)
                    [ruleReference FarRule]
                    [ruleReference for id empty]
                tags Short (2..*)
                amount number(digits: 18, fractionalDigits: 2) (0..1)
                    [docReference for amount Org1 Agr1 name "amount"]
                kind FarEnum (1..1)
                pick Pick (0..1)
                longer Longer (0..1)
                scaled Scaled(scale: 2) (0..1)
                small Small (0..1)
                big number(digits: 19, fractionalDigits: 0) (0..1)
                tiny Tiny (0..1)
                condition NameSet: <"The name is set.">
                    name exists
                condition:
                    far exists

            type Sub extends Near:
                override name string (1..1)

            type OverChoice extends Pick:
                extra string (0..1)

            reporting rule FarRule from Near:
                extract far
            """;

    private record Fixture(RModel other, RModel facts, GeneratorModel gm) {
    }

    private static Fixture fixture() {
        RModel other = AstBuilder.buildFromString(OTHER, "seat5-other.rosetta");
        RModel facts = AstBuilder.buildFromString(FACTS, "seat5-facts.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(other, facts)).workspace();
        return new Fixture(other, facts, new GeneratorModel(ws, m -> true));
    }

    private static IRTypeNode data(Fixture f, IRDeclarationReconciler r, String name) {
        RDataType ast = f.facts().rootElements().stream().filter(RDataType.class::isInstance).map(RDataType.class::cast)
                .filter(d -> d.name().equals(name)).findFirst().orElseThrow();
        return r.adapter().adaptData(f.facts().namespace(), ast);
    }

    private static IRField field(IRTypeNode type, String name) {
        return type.fields().stream().filter(x -> x.name().equals(name)).findFirst().orElseThrow();
    }

    private static IRTypeNode alias(Fixture f, IRDeclarationReconciler r, String name) {
        RTypeAlias ast = f.facts().rootElements().stream().filter(RTypeAlias.class::isInstance).map(RTypeAlias.class::cast)
                .filter(a -> a.name().equals(name)).findFirst().orElseThrow();
        return r.adapter().adaptTypeAlias(f.facts().namespace(), ast);
    }

    /** {@code [kind, name, namespace, arguments]} of a reference's collapsed chain - the fact an emitter types from. */
    private static List<Object> base(IRType reference) {
        IREffectiveBase base = reference.effectiveBase().orElseThrow();
        return List.of(base.kind(), base.name(), base.namespace(),
                base.arguments().stream().map(IRDeclarationFactsReconcileTest::argument).toList());
    }

    private static List<Object> base(IRField field) {
        return base(field.type());
    }

    /** {@code parameter=literal} as written (a negated literal keeps its sign); a pass-through parameter reads {@code <name>}. */
    private static String argument(IRTypeArgument argument) {
        return argument.parameter() + "=" + argument.nameValue().map(n -> "<" + n + ">")
                .orElseGet(() -> (argument.negated() ? "-" : "") + argument.literalValue().orElse("?"));
    }

    private static String java(IRTypeNode type, String fieldName) {
        IRField f = field(type, fieldName);
        return IRJavaTypeNames.of(f.type(), f.typeArguments());
    }

    // ------------------------------------------------------------------------------- the witnesses, fact by fact

    @Test
    void theFixtureParsedEveryDeclaration() {
        Fixture f = fixture();
        long data = f.facts().rootElements().stream().filter(RDataType.class::isInstance).count();
        long choices = f.facts().rootElements().stream().filter(RChoice.class::isInstance).count();
        long enums = f.facts().rootElements().stream().filter(REnumeration.class::isInstance).count();
        assertEquals(List.of(3L, 1L, 2L), List.of(data, choices, enums),
                "a dropped declaration is a silently narrower witness");
    }

    @Test
    void aTypeReferenceCarriesItsTrueKindItsNamespaceAndItsOwnName() {
        Fixture f = fixture();
        IRTypeNode near = data(f, new IRDeclarationReconciler(f.gm()), "Near");
        IRType far = field(near, "far").type();
        assertEquals(List.of("Far", IRKind.STRUCT, Optional.of("seat5.other"), Optional.of("Far"), Optional.of("seat5.other.Far")),
                List.of(far.name(), far.kind(), far.namespace(), far.resolvedName(), far.resolvedQualifiedName()));
        assertEquals(IRKind.ENUM, field(near, "kind").type().kind());
        assertEquals(Optional.of("seat5.other"), field(near, "kind").type().namespace());
        assertEquals(IRKind.CHOICE, field(near, "pick").type().kind());
        assertEquals(Optional.of("seat5.facts"), field(near, "pick").type().namespace());
        assertEquals(IRKind.TYPE_ALIAS, field(near, "tags").type().kind(), "a type alias is no STRUCT");
        assertEquals(Optional.of("seat5.facts"), field(near, "tags").type().namespace());
        assertEquals(IRKind.BASIC_TYPE, field(near, "name").type().kind(), "a basic type is no STRUCT");
        assertEquals(IRKind.BASIC_TYPE, field(near, "amount").type().kind());
    }

    @Test
    void theDocumentationIsCarriedOnTypesFieldsOptionsEnumsAndValues() {
        Fixture f = fixture();
        IRDeclarationReconciler r = new IRDeclarationReconciler(f.gm());
        IRTypeNode near = data(f, r, "Near");
        assertEquals(Optional.of("A near type."), near.definition());
        assertEquals(Optional.of("The name."), field(near, "name").definition());
        assertEquals(Optional.empty(), field(near, "far").definition());
        IREnumNode base = enumOf(f, r, "Base");
        assertEquals(Optional.of("The base enum."), base.definition());
        assertEquals(Optional.of("The first."), base.values().get(0).definition());
        assertEquals(Optional.of("A pick."), choiceOf(f, r).definition());
        assertEquals(Optional.of("The far option."), choiceOf(f, r).fields().get(0).definition());
    }

    @Test
    void overrideTheBaseTypeAndTheEnumParentAreCarried() {
        Fixture f = fixture();
        IRDeclarationReconciler r = new IRDeclarationReconciler(f.gm());
        IRTypeNode sub = data(f, r, "Sub");
        assertTrue(field(sub, "name").isOverride());
        assertFalse(field(data(f, r, "Near"), "name").isOverride());
        IRType base = sub.baseType().orElseThrow();
        assertEquals(List.of("Near", IRKind.STRUCT, Optional.of("seat5.facts"), Optional.of("Near")),
                List.of(base.name(), base.kind(), base.namespace(), base.resolvedName()));
        IRType choiceBase = data(f, r, "OverChoice").baseType().orElseThrow();
        assertEquals(List.of("Pick", IRKind.CHOICE, Optional.of("seat5.facts"), Optional.of("Pick")),
                List.of(choiceBase.name(), choiceBase.kind(), choiceBase.namespace(), choiceBase.resolvedName()),
                "a data type that extends a CHOICE states it");
        IRType parent = enumOf(f, r, "Child").parent().orElseThrow();
        assertEquals(List.of("Base", IRKind.ENUM, Optional.of("seat5.facts"), Optional.of("Base")),
                List.of(parent.name(), parent.kind(), parent.namespace(), parent.resolvedName()));
        assertEquals(Optional.empty(), enumOf(f, r, "Base").parent());
        assertEquals(List.of("Three"), enumOf(f, r, "Child").values().stream().map(IREnumValue::name).toList(),
                "the values are the LOCAL values - the parent is a reference, not a flattening");
    }

    @Test
    void theAnnotationsAreCarriedAsWritten() {
        Fixture f = fixture();
        IRDeclarationReconciler r = new IRDeclarationReconciler(f.gm());
        IRTypeNode near = data(f, r, "Near");
        assertEquals(List.of("metadata:key", "rootType"), near.annotations().stream().map(IRAnnotationUse::key).toList());
        assertEquals(List.of("metadata:scheme"), field(near, "name").annotations().stream().map(IRAnnotationUse::key).toList());
        assertEquals(List.of("deprecated"), enumOf(f, r, "Base").values().get(1).annotations().stream().map(IRAnnotationUse::key).toList());
    }

    @Test
    void aDocReferenceIsCarriedWholeWithItsCorpusResolvedInTheReferencingScope() {
        Fixture f = fixture();
        IRTypeNode near = data(f, new IRDeclarationReconciler(f.gm()), "Near");
        IRDocReference ref = near.docReferences().get(0);
        assertEquals(Optional.of("Org1"), ref.body());
        assertEquals(List.of(new IRDocReference.Segment("name", "near")), ref.segments());
        assertEquals(Optional.of("The provision."), ref.provision());
        IRDocReference.Corpus corpus = ref.corpora().get(0);
        assertEquals("Agr1", corpus.reference());
        IRDocReference.Corpus.Declaration resolved = corpus.resolved().orElseThrow();
        assertEquals(List.of(Optional.of("Agreement"), "Agr1", Optional.of("Agreement 1")),
                List.of(resolved.typeKeyword(), resolved.name(), resolved.displayName()));
        IRDocReference pathed = field(near, "amount").docReferences().get(0);
        assertEquals(Optional.of("amount"), pathed.path().map(p -> p.display()));
    }

    @Test
    void theConditionNamesTheExactBoundsAndTheTypeArgumentsAreCarried() {
        Fixture f = fixture();
        IRTypeNode near = data(f, new IRDeclarationReconciler(f.gm()), "Near");
        assertEquals(List.of(Optional.of("NameSet"), Optional.empty()), near.conditionNames());
        assertEquals(Optional.of(new IRBounds(BigInteger.ZERO, Optional.of(BigInteger.TWO))), field(near, "far").bounds());
        assertEquals(Cardinality.ZERO_TO_MANY, field(near, "far").cardinality(), "the bucket stays beside the exact bounds");
        assertEquals(Optional.of(new IRBounds(BigInteger.TWO, Optional.empty())), field(near, "tags").bounds());
        assertEquals("(2..*)", field(near, "tags").bounds().orElseThrow().display());
        assertEquals(List.of("digits=18", "fractionalDigits=2"), field(near, "amount").typeArguments().stream()
                .map(a -> a.parameter() + "=" + a.literalValue().orElse("?")).toList());
    }

    // --------------------------------------------------- the type gate (v3.3 seat 7, PR #643): the alias facts

    /**
     * THE EFFECTIVE BASE, PER USE SITE: the alias name never reaches a generated byte, the collapsed base does. Each
     * alias attribute states the declaration its chain ENDS on and the arguments in force there after substitution -
     * a bare alias ({@code Short}), an alias of an alias ({@code Longer}), a PARAMETRIC alias whose use site binds its
     * parameter ({@code Scaled(scale: 2)}) and an integer-grained one ({@code Small}) - while an INLINE builtin
     * carries no base at all, only the arguments the model wrote.
     */
    @Test
    void everyAliasAttributeCarriesItsChainCollapsedAtThisUseSite() {
        Fixture f = fixture();
        IRTypeNode near = data(f, new IRDeclarationReconciler(f.gm()), "Near");
        assertEquals(List.of(IRKind.BASIC_TYPE, "string", Optional.empty(), List.of("maxLength=35")),
                base(field(near, "tags")), "typeAlias Short: string(maxLength: 35)");
        assertEquals(List.of(IRKind.BASIC_TYPE, "string", Optional.empty(), List.of("maxLength=35")),
                base(field(near, "longer")), "typeAlias Longer: Short - the chain collapses through the inner alias");
        assertEquals(List.of(IRKind.BASIC_TYPE, "number", Optional.empty(), List.of("digits=10", "fractionalDigits=2")),
                base(field(near, "scaled")), "the use site's literal binds the alias's own parameter");
        assertEquals(List.of(IRKind.BASIC_TYPE, "number", Optional.empty(), List.of("digits=3", "fractionalDigits=0")),
                base(field(near, "small")));
        assertEquals(List.of(IRKind.BASIC_TYPE, "int", Optional.empty(), List.of("digits=2", "fractionalDigits=0")),
                base(field(near, "tiny")),
                "typeAlias Tiny: int(digits: 2) - no library is loaded, so `int` is the registry's, whose own fractionalDigits: 0 SEEDS the base");
        assertEquals(Optional.empty(), field(near, "big").type().effectiveBase(),
                "an inline builtin is no alias: it carries no effective base");
        assertEquals(List.of("digits=19", "fractionalDigits=0"), field(near, "big").typeArguments().stream()
                .map(IRDeclarationFactsReconcileTest::argument).toList());
        assertEquals(List.of("scale=2"), field(near, "scaled").typeArguments().stream()
                .map(IRDeclarationFactsReconcileTest::argument).toList(), "the use-site argument is carried as written");
    }

    /**
     * THE {@code typeAlias} DECLARATION ADAPTED: its parameters, its body as a REFERENCE and the arguments the body
     * wrote AS WRITTEN - a literal, or one of the alias's own parameters passed through by NAME, which binds nothing
     * here. The declaration node itself states NO effective base: the collapse is a property of a use site.
     */
    @Test
    void aTypeAliasDeclarationCarriesItsParametersItsBodyAndTheArgumentsAsWritten() {
        Fixture f = fixture();
        IRDeclarationReconciler r = new IRDeclarationReconciler(f.gm());
        IRTypeNode scaled = alias(f, r, "Scaled");
        assertEquals(List.of("seat5.facts.Scaled", IRKind.TYPE_ALIAS, Optional.of("seat5.facts"), 0),
                List.of(scaled.name(), scaled.kind(), scaled.namespace(), scaled.fields().size()));
        assertEquals(Optional.empty(), scaled.effectiveBase(), "a DECLARATION never carries the collapse");
        assertEquals(List.of("scale"), scaled.typeParameters().stream().map(IRTypeParameter::name).toList());
        assertEquals(List.of("int", IRKind.BASIC_TYPE, Optional.of("int")),
                List.of(scaled.typeParameters().get(0).type().name(), scaled.typeParameters().get(0).type().kind(),
                        scaled.typeParameters().get(0).type().resolvedName()));
        assertEquals("number", scaled.baseType().orElseThrow().name());
        assertEquals(List.of("digits=10", "fractionalDigits=<scale>"),
                scaled.baseTypeArguments().stream().map(IRDeclarationFactsReconcileTest::argument).toList(),
                "the body's arguments AS WRITTEN - the pass-through parameter is a NAME, not a literal");
        IRTypeNode longer = alias(f, r, "Longer");
        assertEquals(List.of("Short", IRKind.TYPE_ALIAS, List.of()),
                List.of(longer.baseType().orElseThrow().name(), longer.baseType().orElseThrow().kind(),
                        longer.baseTypeArguments()));
        assertEquals(List.of(IRKind.BASIC_TYPE, "string", Optional.empty(), List.of("maxLength=35")),
                base(longer.baseType().orElseThrow()),
                "the body names another alias: that REFERENCE carries the chain collapsed, the declaration does not");
        IRTypeNode tiny = alias(f, r, "Tiny");
        assertEquals(List.of(Optional.of("A tiny count."), List.of(Optional.of("Positive")), List.of("digits=2")),
                List.of(tiny.definition(), tiny.conditionNames(),
                        tiny.baseTypeArguments().stream().map(IRDeclarationFactsReconcileTest::argument).toList()),
                "an alias carries its documentation, its conditions' names and its body's arguments as written");
        assertEquals(List.of("int", IRKind.BASIC_TYPE, Optional.of("int"), Optional.empty()),
                List.of(tiny.baseType().orElseThrow().name(), tiny.baseType().orElseThrow().kind(),
                        tiny.baseType().orElseThrow().resolvedName(), tiny.baseType().orElseThrow().namespace()),
                "the body `int` is a registry builtin here (no library loaded): named, in no namespace");
    }

    /** THE EMITTER'S JAVA TYPE, from the IR facts alone: the ladder, the string leaf and the inline builtin. */
    @Test
    void theJavaTypeOfEveryAliasAttributeIsDerivedFromTheIRAlone() {
        Fixture f = fixture();
        IRTypeNode near = data(f, new IRDeclarationReconciler(f.gm()), "Near");
        assertEquals(List.of("java.lang.String", "java.lang.String", "java.math.BigDecimal", "java.lang.Integer",
                        "java.math.BigInteger", "java.lang.String", "seat5.other.Far", "seat5.other.FarEnum",
                        "seat5.facts.Pick", "java.lang.Integer"),
                List.of(java(near, "tags"), java(near, "longer"), java(near, "scaled"), java(near, "small"),
                        java(near, "big"), java(near, "name"), java(near, "far"), java(near, "kind"),
                        java(near, "pick"), java(near, "tiny")));
    }

    @Test
    void anEnumValuesSynonymsAndAFieldsLabelsAndRuleReferencesAreCarried() {
        Fixture f = fixture();
        IRDeclarationReconciler r = new IRDeclarationReconciler(f.gm());
        var synonym = enumOf(f, r, "Base").values().get(0).synonyms().get(0);
        assertEquals(List.of(List.of("FpML"), "ONE", Optional.of("the one"), Optional.of("O.*"), Optional.of("O"), true),
                List.of(synonym.sources(), synonym.value(), synonym.definition(), synonym.patternMatch(), synonym.patternReplace(), synonym.removeHtml()));
        IRTypeNode near = data(f, r, "Near");
        assertEquals(List.of("The name label"), field(near, "name").labels().stream().map(l -> l.label()).toList());
        var references = field(near, "far").ruleReferences();
        assertEquals(List.of(Optional.of("FarRule"), Optional.of("seat5.facts"), Optional.of("FarRule"), false),
                List.of(references.get(0).ruleName(), references.get(0).resolvedNamespace(), references.get(0).resolvedName(), references.get(0).isEmptyKeyword()));
        assertEquals(List.of(Optional.of("id"), true),
                List.of(references.get(1).forPath().map(p -> p.display()), references.get(1).isEmptyKeyword()));
    }

    // ------------------------------------------------------------------------------------------- the reconcile

    /** Every fact of every declaration agrees with the source - read by the reconciler itself, not by the adapter. */
    @Test
    void everyDeclarationFactReconcilesThroughTheThreeWiredGenerators() {
        Fixture f = fixture();
        JavaTypeUtil tu = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(tu);
        IRModelObjectGenerator data = new IRModelObjectGenerator(f.gm(), tt, tu);
        IRChoiceObjectGenerator choice = new IRChoiceObjectGenerator(f.gm(), tt, tu, data);
        IREnumGenerator enums = new IREnumGenerator(f.gm());
        List<GenerationException> errors = new ArrayList<>();
        for (RModel model : List.of(f.other(), f.facts())) {
            Map<String, String> out = new LinkedHashMap<>();
            errors.addAll(data.generateClassesAsIR(model, "1.0.0", out));
            errors.addAll(choice.generateClassesAsIR(model, "1.0.0", out));
            errors.addAll(enums.generateClassesAsIR(model, "1.0.0", out));
        }
        assertEquals(List.of(), errors.stream().map(GenerationException::getMessage).toList(),
                "the declaration IR must agree with the source on every fact");
        assertEquals(List.of(4, 1, 3), List.of(data.declarationReconcileStats()[0], choice.declarationReconcileStats()[0],
                enums.declarationReconcileStats()[0]), "declarations reconciled: data types / choices / enums");
        assertEquals(List.of(0, 0, 0), List.of(data.declarationReconcileStats()[2], choice.declarationReconcileStats()[2],
                enums.declarationReconcileStats()[2]), "mismatches");
        assertTrue(data.declarationReconcileStats()[1] > 100 && choice.declarationReconcileStats()[1] > 10
                && enums.declarationReconcileStats()[1] > 20, "a reconcile that asserts nothing is not a gate: "
                + data.declarationReconcileStats()[1] + " / " + choice.declarationReconcileStats()[1] + " / " + enums.declarationReconcileStats()[1]);
        // v3.3 seat 7 (PR #643 - the type gate): the typeAlias declarations ride the data-type pass, counted apart
        int[] aliases = data.typeAliasReconcileStats();
        assertEquals(5, aliases[0], "the fixture declares five type aliases: Short, Scaled, Longer, Small, Tiny");
        assertEquals(0, aliases[2], "the type-alias IR must agree with the source on every fact");
        assertTrue(aliases[1] > aliases[0], "a reconcile that asserts nothing is not a gate: " + aliases[1]);
        assertEquals(4, data.declarationReconcileStats()[0],
                "the alias population is the OTHER reconciler's: the data-type count is unmoved by it");
    }

    /**
     * THE RECONCILE'S OWN POPULATION (LAW 84): a declaration is counted by {@code attempt()} BEFORE its adapter runs - never
     * by the reconcile's close - and a throw on the way is booked as a MISMATCH, so a throwing declaration can neither leave
     * the count nor leave the gate green. The D11 host holds the count equal to its own count of the cell's declarations.
     */
    @Test
    void aDeclarationIsCountedBeforeItsAdapterRunsAndAThrowIsAMismatch() {
        Fixture f = fixture();
        IRDeclarationReconciler r = new IRDeclarationReconciler(f.gm());
        IRTypeNode near = data(f, r, "Near");
        RDataType ast = f.facts().rootElements().stream().filter(RDataType.class::isInstance).map(RDataType.class::cast)
                .filter(d -> d.name().equals("Near")).findFirst().orElseThrow();
        assertEquals(List.of(), r.reconcileData(f.facts().namespace(), ast, near));
        assertEquals(0, r.stats()[0], "the reconcile's close books facts and mismatches - NEVER the declaration");
        assertTrue(r.stats()[1] > 0 && r.stats()[2] == 0, "facts asserted, no mismatch");
        int facts = r.stats()[1];
        r.attempt();
        r.threw();
        assertEquals(List.of(1, facts, 1), List.of(r.stats()[0], r.stats()[1], r.stats()[2]),
                "an attempted declaration whose adapter threw: counted, and a mismatch");
    }

    private static final String BROKEN = """
            namespace seat5.broken
            version "1.0.0"

            type BrokenType:
                [deprecated]
                id string (1..1)

            choice BrokenChoice:
                [deprecated]
                BrokenType

            enum BrokenEnum:
                [deprecated]
                A
            """;

    /**
     * THE WIRING, PER GENERATOR (PR #641 round 1, cq SF-1): a declaration whose adapter THROWS is fed through each of the
     * three wired generators' reconcile. It must be COUNTED (the attempt is booked before the adapter runs), booked as a
     * MISMATCH (the catch's {@code threw()}) and surfaced as a generation error - delete {@code attempt()} or
     * {@code threw()} from ANY of the three and this is RED for that generator. The throw is real: the model is parsed
     * OUTSIDE the workspace (so it is not frozen) and each declaration's annotation loses its name, which
     * {@code IRAnnotationUse} refuses.
     *
     * <p>v3.3 seat 6 (PR #642 - round 2 NIT-1): the witness also reads the CAUSE it names, per generator. Asserting the
     * wrapper's message and the stats alone would stay green if the adapter threw for some unrelated reason - the
     * fixture would still "throw", the counters would still book a mismatch, and the test would no longer be witnessing
     * the refusal it says it is. The cause must BE the {@code Objects.requireNonNull(name, "name")} of
     * {@code IRAnnotationUse}'s canonical constructor: a {@link NullPointerException} whose message is {@code name}.
     */
    @Test
    void aThrowingDeclarationIsCountedAndIsAMismatchThroughEachOfTheThreeGenerators() {
        Fixture f = fixture();
        RModel broken = AstBuilder.buildFromString(BROKEN, "seat5-broken.rosetta");
        int stripped = 0;
        for (var element : broken.rootElements()) {
            List<com.regnosys.rosetta.ast.annotations.RAnnotationRef> refs =
                    element instanceof RDataType d ? d.annotationRefs()
                            : element instanceof RChoice c ? c.annotationRefs()
                            : element instanceof REnumeration e ? e.annotationRefs() : List.of();
            for (var ref : refs) {
                ref.setAnnotationName(null);
                stripped++;
            }
        }
        assertEquals(3, stripped, "the broken fixture must carry one annotation per declaration - or nothing throws");
        JavaTypeUtil tu = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(tu);
        IRModelObjectGenerator data = new IRModelObjectGenerator(f.gm(), tt, tu);
        IRChoiceObjectGenerator choice = new IRChoiceObjectGenerator(f.gm(), tt, tu, data);
        IREnumGenerator enums = new IREnumGenerator(f.gm());
        List<List<GenerationException>> errors = List.of(data.reconcile(broken), choice.reconcile(broken), enums.reconcile(broken));
        List<int[]> stats = List.of(data.declarationReconcileStats(), choice.declarationReconcileStats(), enums.declarationReconcileStats());
        List<String> names = List.of("data BrokenType", "choice BrokenChoice", "enum BrokenEnum");
        // v3.3 seat 8 (PR #644, the property gate): the data generator ALSO runs the property pass over every data type
        // AND choice of the model, and each of its throws is a named error of its own - so the data generator's list
        // carries the declaration throw first and the two property-pass throws after it, in pass order; the property
        // counters book both (attempted 2 / facts 0 / mismatches 2), apart from the declaration counters below
        List<List<String>> expected = List.of(
                List.of("AST->IR adapter failed for data BrokenType", "AST->IR property model failed for BrokenType",
                        "AST->IR property model failed for BrokenChoice"),
                List.of("AST->IR adapter failed for choice BrokenChoice"),
                List.of("AST->IR adapter failed for enum BrokenEnum"));
        assertEquals(List.of(2, 0, 2), List.of(data.propertyReconcileStats()[0], data.propertyReconcileStats()[1],
                data.propertyReconcileStats()[2]), "the property pass attempted the type and the choice, both throws mismatches");
        for (int i = 0; i < 3; i++) {
            assertEquals(expected.get(i),
                    errors.get(i).stream().map(GenerationException::getMessage).toList(), "the throw is a generation error: " + names.get(i));
            Throwable cause = errors.get(i).get(0).getCause();
            assertEquals(List.of(NullPointerException.class, "name"),
                    List.of(cause.getClass(), String.valueOf(cause.getMessage())),
                    "the generation error CARRIES the refusal it names - IRAnnotationUse rejecting the null annotation"
                            + " name, not some other adapter throw: " + names.get(i));
            assertEquals(List.of(1, 0, 1), List.of(stats.get(i)[0], stats.get(i)[1], stats.get(i)[2]),
                    "attempted 1 / facts 0 / mismatches 1 - counted BEFORE the adapter, the throw a mismatch: " + names.get(i));
        }
    }

    private static IREnumNode enumOf(Fixture f, IRDeclarationReconciler r, String name) {
        REnumeration ast = f.facts().rootElements().stream().filter(REnumeration.class::isInstance).map(REnumeration.class::cast)
                .filter(e -> e.name().equals(name)).findFirst().orElseThrow();
        return r.adapter().adaptEnum(f.facts().namespace(), ast);
    }

    private static IRTypeNode choiceOf(Fixture f, IRDeclarationReconciler r) {
        RChoice ast = f.facts().rootElements().stream().filter(RChoice.class::isInstance).map(RChoice.class::cast)
                .findFirst().orElseThrow();
        return r.adapter().adaptChoice(f.facts().namespace(), ast);
    }
}
