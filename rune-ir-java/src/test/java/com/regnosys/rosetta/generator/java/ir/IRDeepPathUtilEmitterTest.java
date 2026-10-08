package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.object.deeppath.DeepPathUtilGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.rosetta.model.lib.ModelSymbolId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE DEEP-PATH UTIL MEMBER, HELD BYTE-EQUAL TO THE OLD GENERATOR (v3.3 seat 9, PR #645 commit 14).
 *
 * <p><b>THE ORACLE IS {@code DeepPathUtilGenerator}'s OWN OUTPUT</b> for the SAME parsed model on the SAME tree -
 * {@code generateClasses(model, version, map)} into a local map, the entry read back by the key
 * {@link IRTypeUnit#outputKey} computes, which is asserted here to BE
 * {@code JavaTypeTranslator.toDeepPathUtilJavaClass}'s own path (the two spellings of one law, held together per
 * type rather than trusted). {@code IRModelMetaEmitterTest}'s shape, one member over.
 *
 * <p><b>THE MEMBER THAT MAY SAY NO FILE, HELD BOTH WAYS.</b> Every data type of every workspace here is asked, and
 * each is asserted TWICE: an ELIGIBLE type's text is byte-equal to the oracle's file at its key, and a NOT-ELIGIBLE
 * type answers {@link Optional#empty()} AND the old generator wrote no file at that key. An absence agreed by one
 * route alone is not an absence - it is the {@code noFileWhereLegacyWrote} the D11 shadow books RED.
 *
 * <p><b>THE POPULATION IS ASSERTED PER GROUP</b> (Rule 4 / LAW 84): the data types asked, the files compared, the
 * ELIGIBLE count and the DEPENDENCY-BEARING count, stated rather than derived.
 *
 * <p><b>WHAT THE HOLD-OUT SET CANNOT WITNESS, AND THE FIXTURE THAT DOES</b> - MEASURED at this class's first
 * run and stated rather than assumed. The nine hold-out groups whose golden set carries a
 * {@code util/*DeepPathUtil.java} hold <b>39 data types between them and NOT ONE of them is ELIGIBLE</b>: all 13
 * of those goldens belong to a {@code choice}, whose util is the CHOICE unit's (the next PR) and no part of this
 * member's population, and no hold-out group anywhere declares a data type with a {@code one-of} condition. So
 * the hold-out battery witnesses exactly one law - the NO-FILE answer, 39 absences held both ways - and not one
 * rendering arm. Every arm is {@link #DEEP_PATH_INJECTION}'s: {@code Obs} injects TWO sibling utils and descends
 * through both (the {@code HashSet} order law and the DEEPER arm, whose corpus witness is cdm6's
 * {@code ObservableDeepPathUtil}), {@code MetaOuter} unwraps a {@code [metadata reference]} alternative (the
 * {@code "Type coercion"} step, cdm6's {@code IndexDeepPathUtil}), {@code MultiOuter} carries a MULTI feature,
 * {@code LangOuter} the #306 law, {@code KeyOuter} the keyword escape and {@code IdxA} the EMPTY class. The
 * corpus bar above all of them is the D11 {@code UNIT SHADOW[DEEP_PATH_UTIL]} gate over every data type of
 * 26 cells.
 */
class IRDeepPathUtilEmitterTest {

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    /**
     * What one workspace's comparison read: the data types ASKED, the files compared, the ELIGIBLE types and the
     * ones that inject at least one sibling util.
     */
    private record Tally(int types, int files, int eligible, int withDependencies) {
    }

    /** What the whole run witnessed, across every workspace - the four arms the member's laws turn on. */
    private static final class Witnessed {
        int maxDependencies;
        int metaUnwraps;
        int multiFeatures;
        int emptyClasses;
        int deeperArms;

        @Override
        public String toString() {
            return "maxDependencies=" + maxDependencies + " metaUnwraps=" + metaUnwraps
                    + " multiFeatures=" + multiFeatures + " emptyClasses=" + emptyClasses
                    + " deeperArms=" + deeperArms;
        }
    }

    // ------------------------------------------------------------------------------------- the three fixtures

    @Test
    void theSeatFixturesRenderByteEqualToTheOldGeneratorOnEveryValidatedDataType() {
        Witnessed witnessed = new Witnessed();
        IRPropertyModelTest.Fixture props = IRPropertyModelTest.fixture();
        Tally p = compare("seat8.props", props.gm(), props.index(), witnessed);
        assertEquals(p.types(), p.files() + (p.types() - p.eligible()),
                "every data type was either compared or agreed absent");

        IRPropertyModelTest.Fixture collide = IRPropertyModelTest.collisionFixture();
        Tally c = compare("seat9.collide", collide.gm(), collide.index(), witnessed);

        IRPropertyModelTest.Fixture setters = IRPropertyModelTest.settersFixture();
        Tally s = compare("seat9.setters", setters.gm(), setters.index(), witnessed);
        System.out.println("IR deep-path emitter over the three seat fixtures: props=" + p
                + " collide=" + c + " setters=" + s + " :: " + witnessed);
        assertTrue(p.types() > 0 && c.types() > 0 && s.types() > 0,
                "each fixture declares at least one data type");
    }

    // ----------------------------------------------------------------- the hold-out groups that HAVE a util

    /**
     * EVERY HOLD-OUT GROUP WHOSE GOLDEN SET CARRIES A {@code util/*DeepPathUtil.java}, ENUMERATED BY NAME
     * (Rule 4). The list was READ off {@code rune-java-generator/src/test/resources/holdout-goldens} - the groups
     * with at least one golden under a {@code util/} directory - and every one has its sources committed beside it
     * under {@code .../holdout}. The shapes they carry, and why each is here:
     * <ul>
     *   <li>{@code deep-path-util-injection} and {@code -edge} - the DIRECT arm over a nested choice, the MULTI
     *       feature ({@code chooseTags} returning {@code List<String>}, {@code mapC} / {@code getMulti} / the
     *       {@code Collections.<String>emptyList()} terminal) and a cross-namespace alternative;</li>
     *   <li>{@code func-meta-deep-path-multi} - a MULTI feature whose item is a {@code FieldWithMetaInteger},
     *       i.e. the meta-wrapped REPRESENTATIVE (not a meta-wrapped alternative);</li>
     *   <li>{@code void-mapping-deep-tok} - the SINGLE meta-wrapped terminal
     *       ({@code return FieldWithMetaVoid.builder().build();});</li>
     *   <li>{@code choice-switch-in-lambda}, {@code -bare-item}, {@code -edge}, {@code only-exists-item-root} and
     *       {@code pojo} - the EMPTY class of an eligible type with zero deep features.</li>
     * </ul>
     * Note the goldens are the RELEASED PLUGIN's bar and are held by the D11 rings; what this class holds is the
     * member against {@code DeepPathUtilGenerator}'s own output on the same tree, which is the two-producer read.
     */
    static final List<String> HOLDOUT_GROUPS = List.of(
            "choice-switch-in-lambda", "choice-switch-in-lambda-bare-item", "choice-switch-in-lambda-edge",
            "deep-path-util-injection", "deep-path-util-injection-edge", "func-meta-deep-path-multi",
            "only-exists-item-root", "pojo", "void-mapping-deep-tok");

    @Test
    void everyHoldOutGroupWithADeepPathGoldenRendersByteEqualToTheOldGenerator() {
        assertEquals(9, HOLDOUT_GROUPS.size(), "the enumerated population IS nine groups");
        assertEquals(HOLDOUT_GROUPS.size(), Set.copyOf(HOLDOUT_GROUPS).size(), "no group is named twice");

        Witnessed witnessed = new Witnessed();
        Map<String, Tally> read = new LinkedHashMap<>();
        for (String group : HOLDOUT_GROUPS) {
            IRPropertyModelTest.HoldOutWorkspace workspace = IRPropertyModelTest.holdOutWorkspace(group);
            read.put(group, compare(group, workspace.gm(), workspace.index(), witnessed));
        }
        int types = 0;
        int files = 0;
        int eligible = 0;
        int withDependencies = 0;
        for (Map.Entry<String, Tally> entry : read.entrySet()) {
            Tally tally = entry.getValue();
            assertTrue(tally.types() > 0, entry.getKey() + ": declares at least one data type");
            assertEquals(tally.eligible(), tally.files(), entry.getKey()
                    + ": ONE file per ELIGIBLE data type - an eligible type always renders (the zero-feature one"
                    + " renders the EMPTY class), and no other type writes a file");
            types += tally.types();
            files += tally.files();
            eligible += tally.eligible();
            withDependencies += tally.withDependencies();
        }
        System.out.println("IR deep-path emitter over the " + HOLDOUT_GROUPS.size() + " hold-out groups with a"
                + " deep-path golden: types=" + types + " files=" + files + " eligible=" + eligible
                + " withDependencies=" + withDependencies + " :: " + witnessed);
        for (Map.Entry<String, Tally> entry : read.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
        }
        assertEquals(eligible, files, "every ELIGIBLE data type of every group was compared as ONE file");

        // WHAT THIS BATTERY WITNESSES, MEASURED AND STATED (LAW 75 / 84, and a scope-honesty disclosure rather
        // than a hope): 39 data types, ZERO of them eligible - so ZERO rendered files. Every one of the 13
        // deep-path goldens in these nine groups belongs to a CHOICE, whose util is the CHOICE unit's (the next
        // PR) and not this member's, and NO hold-out group anywhere declares a data type with a `one-of`
        // condition. The battery is therefore the NO-FILE law's witness and nothing else: 39 absences the member
        // claimed and the old generator's own output confirms, per type, both ways. Every RENDERING arm is the
        // seat-9 injection fixture's below, and the corpus bar is the D11 UNIT SHADOW[DEEP_PATH_UTIL] gate over
        // all 19,316 data types of 26 cells.
        assertEquals(39, types, "the nine groups declare thirty-nine data types between them");
        assertEquals(0, eligible, "and NOT ONE of them is eligible - every deep-path golden in the hold-out"
                + " corpus belongs to a CHOICE. If this ever reads non-zero a group grew an eligible data type"
                + " and this battery's claim (the NO-FILE law alone) must be re-read, not quietly widened");
        assertEquals(0, files, "so the battery renders nothing and compares nothing - it holds absences");
        assertEquals(0, witnessed.maxDependencies + witnessed.metaUnwraps + witnessed.multiFeatures
                + witnessed.emptyClasses + witnessed.deeperArms,
                "and witnesses no rendering arm at all: " + witnessed);
    }

    // ------------------------------------------------ THE DEPENDENCY / DEEPER-ARM / META-UNWRAP FIXTURE (LAW 76)

    /**
     * THE THREE ARMS NO HOLD-OUT GROUP REACHES.
     *
     * <p>{@code Obs} is the {@code ObservableDeepPathUtil} shape in miniature: two alternatives whose targets
     * ({@code Idx}, {@code Ast}) are THEMSELVES eligible and carry the deep feature {@code tag}, so {@code Obs}'s
     * {@code chooseTag} DESCENDS through both and the class injects TWO sibling utils - the only place in this
     * build where the {@code HashSet} iteration-order law is readable at all (the corpus witness lives in the cdm6
     * cells). {@code IdxA} / {@code IdxB} / {@code AstA} / {@code AstB} are eligible with ZERO deep features, so
     * they render the EMPTY class beside it.
     *
     * <p>{@code MetaOuter} is the {@code IndexDeepPathUtil.interestRateIndex} shape: an alternative carrying
     * {@code [metadata reference]}, whose guard is the {@code ReferenceWithMetaTag} wrapper and which must be
     * unwrapped through the {@code "Type coercion"} null-ternary {@code getValue()} step BEFORE the feature is
     * read off it.
     */
    private static final String DEEP_PATH_INJECTION = """
            namespace seat9.deeppath
            version "1.0.0"

            type Tag: <"the shared leaf - NOT eligible (no one-of), so it descends no further.">
                [metadata key]
                code string (1..1)

            type IdxA: <"eligible, zero deep features - the EMPTY class.">
                shared Tag (0..1)
                p string (0..1)
                condition: one-of

            type IdxB:
                shared Tag (0..1)
                q string (0..1)
                condition: one-of

            type Idx: <"eligible, and its OWN deep feature is `shared` - which makes Obs descend into it.">
                ia IdxA (0..1)
                ib IdxB (0..1)
                condition: one-of

            type AstA:
                shared Tag (0..1)
                r string (0..1)
                condition: one-of

            type AstB:
                shared Tag (0..1)
                s string (0..1)
                condition: one-of

            type Ast:
                aa AstA (0..1)
                ab AstB (0..1)
                condition: one-of

            type Obs: <"TWO injected sibling utils - the >= 2 dependency witness.">
                idx Idx (0..1)
                ast Ast (0..1)
                condition: one-of

            type Leafy: <"a MULTI attribute - NOT eligible itself, so it is only ever a descend target.">
                tags string (0..*)
                lone string (0..1)

            type MultiOuter: <"a MULTI deep feature - mapC / getMulti / the Collections.<X>emptyList() terminal.">
                la Leafy (0..1)
                lb Leafy (0..1)
                condition: one-of

            type MetaOuter: <"a [metadata reference] alternative - the 'Type coercion' unwrap witness.">
                wrapped Tag (0..1)
                    [metadata reference]
                plain Tag (0..1)
                condition: one-of

            type Error: <"a type whose simple name IS an implicitly-imported java.lang class (the #306 law).">
                code string (1..1)

            type LangOuter: <"the #306 law at a rendered TYPE position - Error is never imported.">
                e1 Error (0..1)
                e2 Error (0..1)
                condition: one-of

            type KeyOuter: <"an attribute whose name IS a Java keyword - the escape law's witness.">
                volatile Tag (0..1)
                other Tag (0..1)
                condition: one-of
            """;

    /**
     * The injection fixture's workspace, built fresh - SHARED with {@code IRDerivedFactsTest}, which holds the two
     * new reconciled families over it because no hold-out group reaches the arms they gate.
     */
    record InjectionFixture(RModel model, GeneratorModel gm, IRTypeIndex index) {

        /** The workspace's models, in load order - the {@code HoldOutWorkspace} accessor's shape. */
        List<RModel> models() {
            return List.of(model);
        }
    }

    static InjectionFixture injectionFixture() {
        RModel model = AstBuilder.buildFromString(DEEP_PATH_INJECTION, "seat9-deeppath.rosetta");
        RWorkspace workspace = RWorkspace.build(List.of(model)).workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> true);
        IRDeclarationReconciler pass = new IRDeclarationReconciler(gm);
        return new InjectionFixture(model, gm, new IRTypeIndex(workspace, pass.adapter(),
                new IRDeclarationReconciler(gm, pass.adapter())));
    }

    @Test
    void theInjectionFixtureWitnessesTwoDependenciesTheDeeperArmAndTheMetaUnwrap() {
        InjectionFixture fixture = injectionFixture();
        RModel model = fixture.model();
        GeneratorModel gm = fixture.gm();
        IRTypeIndex index = fixture.index();

        Witnessed witnessed = new Witnessed();
        Tally tally = compare("seat9.deeppath", gm, index, witnessed);
        System.out.println("IR deep-path emitter over the injection fixture: " + tally + " :: " + witnessed);
        assertEquals(14, tally.types(), "the fixture declares fourteen data types");
        assertEquals(11, tally.eligible(), "eleven are eligible - Tag, Error and Leafy declare no one-of");
        assertEquals(11, tally.files(),
                "and each of the eleven rendered ONE file, byte-equal to the old generator");
        assertEquals(1, tally.withDependencies(), "Obs alone injects sibling utils");

        // the three arms, NAMED in the rendered text so the fixture cannot silently stop exercising them
        IRDerivedFacts facts = new IRDerivedFacts(index, IRDerivedLie.NONE);
        IRDeepPathUtilEmitter emitter = new IRDeepPathUtilEmitter(index, facts);

        String obs = emitter.render(index.node("seat9.deeppath", dataTypeOf(model, "Obs")));
        assertTrue(obs.contains("private final IdxDeepPathUtil idxDeepPathUtil;")
                        && obs.contains("private final AstDeepPathUtil astDeepPathUtil;"),
                "THE DEPENDENCY ARM: Obs injects BOTH sibling utils - " + obs);
        assertTrue(obs.contains("@Inject"), "and declares the @Inject constructor - " + obs);
        assertTrue(obs.contains("idxDeepPathUtil.chooseShared(")
                        && obs.contains("astDeepPathUtil.chooseShared("),
                "THE DEEPER ARM: both alternatives descend through their own util - " + obs);
        assertEquals(2, emitter.dependencyCanonicals(index.node("seat9.deeppath", dataTypeOf(model, "Obs"))).size(),
                "the dependency fact reads TWO, which is the >= 2 order law's only witness in this build");

        String metaOuter = emitter.render(index.node("seat9.deeppath", dataTypeOf(model, "MetaOuter")));
        assertTrue(metaOuter.contains("\"Type coercion\""),
                "THE META UNWRAP: the [metadata reference] alternative unwraps before its feature is read - "
                        + metaOuter);
        assertTrue(metaOuter.contains("== null ? null : "),
                "and the unwrap is the null-ternary getValue() step - " + metaOuter);

        String idxA = emitter.render(index.node("seat9.deeppath", dataTypeOf(model, "IdxA")));
        assertFalse(idxA.contains("\tpublic "), "THE EMPTY CLASS: an eligible zero-feature type renders no"
                + " member at all - " + idxA);
        assertFalse(idxA.contains("import "), "and no import, not even the static one - " + idxA);
        assertTrue(idxA.contains("public class IdxADeepPathUtil {"), idxA);

        String langOuter = emitter.render(index.node("seat9.deeppath", dataTypeOf(model, "LangOuter")));
        assertTrue(langOuter.contains("MapperS<seat9.deeppath.Error>"),
                "THE #306 LAW: a java.lang-colliding type is written FULLY QUALIFIED at every TYPE position - "
                        + langOuter);
        assertFalse(langOuter.contains("import seat9.deeppath.Error;"),
                "and is never imported - " + langOuter);

        String keyOuter = emitter.render(index.node("seat9.deeppath", dataTypeOf(model, "KeyOuter")));
        assertTrue(keyOuter.contains("MapperS<Tag> _volatile ="),
                "THE KEYWORD ESCAPE: a guard variable whose name is a Java keyword takes a leading underscore - "
                        + keyOuter);

        String multiOuter = emitter.render(index.node("seat9.deeppath", dataTypeOf(model, "MultiOuter")));
        assertTrue(multiOuter.contains("public List<String> chooseTags(")
                        && multiOuter.contains("mapC(\"getTags\"")
                        && multiOuter.contains("return Collections.<String>emptyList();"),
                "THE MULTI FEATURE: List<X> / mapC / getMulti / the empty-list terminal - " + multiOuter);

        assertTrue(witnessed.maxDependencies >= 2 && witnessed.metaUnwraps > 0 && witnessed.deeperArms > 0
                        && witnessed.emptyClasses > 0 && witnessed.multiFeatures > 0,
                "the fixture witnessed every arm it exists for - and it is the ONLY witness of any of them at"
                        + " this level, because no hold-out group declares an ELIGIBLE data type: " + witnessed);
    }

    /**
     * The member contract from this side: the deep-path util is the ONE member that may answer NO FILE BY LAW, and
     * it does so for EXACTLY the types the old generator writes no file for - asserted both ways in {@link
     * #compare}, and stated here as the member's own claim over a fixture that has a not-eligible type in it.
     */
    @Test
    void theOnlyEmptyAnswerIsForATypeTheOldGeneratorWritesNoFileFor() {
        InjectionFixture fixture = injectionFixture();
        RModel model = fixture.model();
        IRTypeIndex index = fixture.index();
        IRTypeUnit.MemberEmitter emitter =
                new IRDeepPathUtilEmitter(index, new IRDerivedFacts(index, IRDerivedLie.NONE));

        IRTypeNode tag = index.node("seat9.deeppath", dataTypeOf(model, "Tag"));
        assertTrue(emitter.emit(tag).isEmpty(), "Tag declares no one-of, so it writes NO FILE BY LAW");
        IRTypeNode obs = index.node("seat9.deeppath", dataTypeOf(model, "Obs"));
        assertTrue(emitter.emit(obs).isPresent(), "an ELIGIBLE type always has a text");
        assertTrue(IRTypeUnit.Member.DEEP_PATH_UTIL.mayWriteNoFile(),
                "and this member is the only one the unit lets answer so");
    }

    /** The named data type of a model, or a failure - the population is enumerated, never globbed. */
    private static RDataType dataTypeOf(RModel model, String name) {
        for (var element : model.rootElements()) {
            if (element instanceof RDataType dataType && name.equals(dataType.name())) {
                return dataType;
            }
        }
        throw new AssertionError("the fixture declares no type named " + name);
    }

    // --------------------------------------------------------------------------------------------- the driver

    /**
     * Hold the DEEP_PATH_UTIL member against {@code DeepPathUtilGenerator} over EVERY model of one workspace. The
     * old generator runs ONCE, over every model, into its own map; its own generation errors are asserted EMPTY
     * (the deep-path family refuses nothing at 9.83), so a refusal here is a finding and never a skipped key.
     */
    private Tally compare(String what, GeneratorModel gm, IRTypeIndex index, Witnessed witnessed) {
        List<RModel> models = new ArrayList<>();
        gm.workspace().files().forEach(models::add);

        Map<String, String> oracle = new LinkedHashMap<>();
        DeepPathUtilGenerator utilGen = new DeepPathUtilGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        // THE OLD GENERATOR DOES REFUSE (READ at the first run of this class, seat8.props: a top-level `choice`
        // with [metadata template] - TemplatedChoice - is a declared U015 decline). Its refusals are indexed BY
        // TARGET PATH rather than asserted away, because only a DATA TYPE's refusal would matter here: the member
        // has no named counterpart for one, so a data type among them is a finding and a CHOICE among them is the
        // choice unit's business (the next PR) and no business of this population.
        Set<String> oracleRefusedKeys = new LinkedHashSet<>();
        for (RModel model : models) {
            for (GenerationException error : utilGen.generateClasses(model, gm.version(model), oracle)) {
                oracleRefusedKeys.add(String.valueOf(error.getTargetPath()).replace('\\', '/'));
            }
        }

        IRDerivedFacts facts = new IRDerivedFacts(index, IRDerivedLie.NONE);
        IRDeepPathUtilEmitter emitter = new IRDeepPathUtilEmitter(index, facts);
        int types = 0;
        int files = 0;
        int eligible = 0;
        int withDependencies = 0;
        for (RModel model : models) {
            for (var element : model.rootElements()) {
                if (!(element instanceof RDataType dataType)) {
                    continue;
                }
                types++;
                IRTypeNode node = index.node(model.namespace(), dataType);
                String key = IRTypeUnit.outputKey(node, IRTypeUnit.Member.DEEP_PATH_UTIL);
                // THE TWO SPELLINGS OF THE KEY LAW, held together rather than trusted.
                String translatorKey = TYPE_TRANSLATOR
                        .toDeepPathUtilJavaClass(new ModelSymbolId(gm.namespace(dataType), dataType.name()))
                        .getCanonicalName().withForwardSlashes() + ".java";
                assertEquals(translatorKey, key, what + " / " + dataType.name()
                        + ": IRTypeUnit.outputKey and JavaTypeTranslator.toDeepPathUtilJavaClass are ONE law");
                assertFalse(oracleRefusedKeys.contains(key), what + " / " + key
                        + ": the old generator REFUSED a DATA TYPE's util - the member has no named counterpart"
                        + " for such a refusal at this commit, so this is a finding, never a skipped key");
                Optional<String> text = emitter.emit(node);
                String expected = oracle.get(key);
                if (text.isEmpty()) {
                    // NO FILE BY LAW - and the OTHER route must agree, which is the half no compare column sees
                    assertNull(expected, what + " / " + key + ": the member answered NO FILE BY LAW where the old"
                            + " generator WROTE the file - the route disagreement the D11 shadow books as"
                            + " noFileWhereLegacyWrote");
                    continue;
                }
                eligible++;
                assertNotNull(expected, what + " / " + key
                        + ": the member rendered a file the old generator did not write");
                assertEquals(expected, text.get(), what + " / " + key
                        + ": the WHOLE deep-path util, byte for byte off the old generator's own generateClasses"
                        + " output for the same parsed model");
                files++;
                List<String> dependencies = emitter.dependencyCanonicals(node);
                if (!dependencies.isEmpty()) {
                    withDependencies++;
                }
                witnessed.maxDependencies = Math.max(witnessed.maxDependencies, dependencies.size());
                if (text.get().contains("\"Type coercion\"")) {
                    witnessed.metaUnwraps++;
                }
                if (text.get().contains("Collections.<")) {
                    witnessed.multiFeatures++;
                }
                if (!text.get().contains("\tpublic ")) {
                    witnessed.emptyClasses++;
                }
                for (String arm : emitter.armDecisions(node)) {
                    if (arm.contains("=deeper|")) {
                        witnessed.deeperArms++;
                    }
                }
            }
        }
        return new Tally(types, files, eligible, withDependencies);
    }
}
