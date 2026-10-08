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
import com.regnosys.rosetta.generator.java.object.ModelMetaGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ir.adapter.AstToIRAdapter;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.rosetta.model.lib.ModelSymbolId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE {@code *Meta} MEMBER, HELD BYTE-EQUAL TO THE OLD GENERATOR (v3.3 seat 9, PR #645 commit 13).
 *
 * <p><b>THE ORACLE IS {@code ModelMetaGenerator}'s OWN OUTPUT</b> for the SAME parsed model on the SAME tree -
 * {@code generateClasses(model, version, map)} into a local map, the entry read back by the key
 * {@link IRTypeUnit#outputKey} computes, which is asserted here to BE
 * {@code JavaTypeTranslator.toJavaMetaDataClass}'s own path (the two spellings of one law, held together per type
 * rather than trusted). That is the only oracle that makes this a TWO-PRODUCER comparison: the goldens are the
 * released plugin's bar and are held by {@code HoldOutByteCompareTest} and by the D11 rings, where they belong.
 *
 * <p><b>THE POPULATION IS ASSERTED PER GROUP</b> (Rule 4 / LAW 84): the data types compared, the files compared and
 * the QUALIFY ROOTS exercised, stated rather than derived. The last is what makes this more than a condition-ref
 * test: {@code getQualifyFunctions} is {@code Collections.emptyList()} on all but the first-wins qualifiable roots
 * of the workspace, so a run that exercised no root would have proved only the empty arm.
 *
 * <p><b>WHAT IS NOT COMPARED HERE.</b> The old generator writes a {@code *Meta} for every {@code Data} AND every
 * {@code choice}; the type unit's population is the DATA TYPES alone (a choice takes the inherited path on every
 * pass, {@code IRUnitPass}'s routing law), so the choices' meta files sit in the oracle map uncompared - as they do
 * on the D11 {@code UNIT SHADOW[META]} line, whose population is the cell's data-type count.
 */
class IRModelMetaEmitterTest {

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    /** What one workspace's comparison read: the data types compared, the files compared, the qualify roots. */
    private record Tally(int types, int files, int qualifyRoots, int qualifyEntries) {
    }

    // ------------------------------------------------------------------------------------- the three fixtures

    @Test
    void theSeatFixturesRenderByteEqualToTheOldGeneratorOnEveryValidatedDataType() {
        // ONE fixture instance per workspace: fixture() builds a fresh workspace on every call, so the generator
        // model and the index must come from the SAME one or the comparison would read two different trees
        IRPropertyModelTest.Fixture props = IRPropertyModelTest.fixture();
        Tally p = compare("seat8.props", props.gm(), props.index(), modelIndexOf(props));
        assertTrue(p.types() > 0, "the fixture declares at least one data type");
        assertEquals(p.types(), p.files(), "the meta is ONE file per data type - no member, no refusal");

        IRPropertyModelTest.Fixture collide = IRPropertyModelTest.collisionFixture();
        Tally c = compare("seat9.collide", collide.gm(), collide.index(), modelIndexOf(collide));
        assertEquals(c.types(), c.files());

        IRPropertyModelTest.Fixture setters = IRPropertyModelTest.settersFixture();
        Tally s = compare("seat9.setters", setters.gm(), setters.index(), modelIndexOf(setters));
        assertEquals(s.types(), s.files());
        System.out.println("IR meta emitter over the three seat fixtures: props=" + p
                + " collide=" + c + " setters=" + s);
    }

    // ----------------------------------------------------------------------- the hold-out groups that HAVE a meta

    /**
     * EVERY HOLD-OUT GROUP WHOSE GOLDEN SET CARRIES A {@code meta/*Meta.java}, ENUMERATED BY NAME (Rule 4). The
     * list was READ off {@code rune-java-generator/src/test/resources/holdout-goldens} - the groups with at least
     * one golden under a {@code meta/} directory - and every one of them has its sources committed beside it under
     * {@code .../holdout}. Among them are the shapes the member's laws turn on, and this test would be a weaker
     * statement without each:
     * <ul>
     *   <li>the six {@code qualify-*} batteries and {@code zz-qualify-order-probe-c} - the ONLY witnesses of the
     *       qualify wing outside the cdm cells: the first-wins ROOT, the load order x in-file order, and a
     *       cross-namespace first input;</li>
     *   <li>{@code pojo-inheritance} and {@code hero-model} - the deep supertype chains the ROOT-FIRST condition-ref
     *       order stands on;</li>
     *   <li>the seven {@code alias-conditions*} batteries - the condition-bearing types;</li>
     *   <li>the five {@code type-named-*} batteries and {@code void-mapping-name-collision} - the #306 java.lang
     *       collision law, on the subject type AND on a condition's DECLARING type;</li>
     *   <li>{@code name-escaping} and {@code reserved-names} - the keyword-escape law, which the subject type's FQN
     *       takes and the condition refs' FQNs deliberately do NOT (see {@code IRModelMetaEmitter}'s seam note).</li>
     * </ul>
     */
    private static final List<String> HOLDOUT_GROUPS = List.of(
            "alias-conditions", "alias-conditions-filescope", "alias-conditions-header", "alias-conditions-meta",
            "alias-conditions-reserved", "alias-conditions-scope", "alias-conditions-twins",
            "arg-coercion-bare-local", "choice-switch-in-lambda", "choice-switch-in-lambda-bare-item",
            "choice-switch-in-lambda-edge", "closure-param-duplicate", "closure-param-duplicate-reads",
            "deep-path-util-injection", "deep-path-util-injection-edge", "enum-unicode-display",
            "enum-unicode-display-edge", "expr-bool-nav-logical", "expr-date-subtract", "expr-date-time-add",
            "expr-misc-primaries", "expr-nested-complex", "expr-sort-min-max", "extract-meta-elem-deref-function",
            "func-alias-assign-output", "func-alias-creation-lhs", "func-bulk-as-key", "func-ctor-as-key-meta",
            "func-ctor-as-key-ref", "func-dispatch-collision", "func-meta-add-value", "func-meta-as-key-set",
            "func-meta-choice-ignore", "func-meta-deep-path-multi", "func-meta-passthrough", "func-one-of-static",
            "func-set-single-basic", "func-set-single-complex", "func-single-to-list-set", "hero-model",
            "list-literal-add-item-coerce", "meta-ladder-alias-rung", "meta-ladder-alias-rung-edge",
            "name-escaping", "only-exists-item-root", "pojo", "pojo-bulk-meta-drop", "pojo-bulk-meta-kind",
            "pojo-bulk-value-narrow", "pojo-inheritance", "pojo-number-ladder", "qualify-cross-namespace-input",
            "qualify-event-and-product", "qualify-first-wins", "qualify-order-probe-a", "qualify-order-probe-b",
            "report-override", "report-rules-split", "report-simple-rule-valid", "report-withsource-shadow",
            "report-withtype-qualified", "report-withtype-rule-shadow", "report-withtype-shadow", "reserved-names",
            "rule-meta-output-unwrap", "tostring-over-default-enum", "type-named-annotations", "type-named-guava",
            "type-named-list", "type-named-rosetta", "type-named-util", "void-mapping-basic-record",
            "void-mapping-basic-record-edge", "void-mapping-collapsed-receiver", "void-mapping-deep-tok",
            "void-mapping-exists-then-clean", "void-mapping-name-collision", "void-mapping-render-builder",
            "void-mapping-render-edge", "void-mapping-segment-conditional-set", "void-meta-output-set",
            "withmeta-wrapped-argument", "withmeta-wrapped-argument-edge", "x36enum-split-p1",
            "zz-qualify-order-probe-c");

    /** The batteries that MUST exercise the qualify wing - a run in which none did would prove only the empty arm. */
    private static final List<String> QUALIFY_GROUPS = List.of(
            "qualify-cross-namespace-input", "qualify-event-and-product", "qualify-first-wins",
            "qualify-order-probe-a", "qualify-order-probe-b", "zz-qualify-order-probe-c");

    @Test
    void everyHoldOutGroupWithAMetaGoldenRendersByteEqualToTheOldGenerator() {
        assertEquals(85, HOLDOUT_GROUPS.size(), "the enumerated population IS eighty-five groups");
        assertEquals(HOLDOUT_GROUPS.size(), Set.copyOf(HOLDOUT_GROUPS).size(), "no group is named twice");
        assertTrue(HOLDOUT_GROUPS.containsAll(QUALIFY_GROUPS), "the qualify batteries are among them");
        assertTrue(HOLDOUT_GROUPS.containsAll(IRPropertyModelTest.HOLDOUT_POJO_GROUPS),
                "the five pojo batteries are among them - the POJO member's own enumeration, reused");

        Map<String, Tally> read = new LinkedHashMap<>();
        for (String group : HOLDOUT_GROUPS) {
            IRPropertyModelTest.HoldOutWorkspace workspace = IRPropertyModelTest.holdOutWorkspace(group);
            read.put(group, compare(group, workspace.gm(), workspace.index(), modelIndexOf(workspace.gm())));
        }
        // THE POPULATION PER GROUP, STATED (LAW 84). A group that grew or lost a data type moves a number here
        // rather than passing silently, and the meta is ONE file per data type with no refusal and no absence.
        int types = 0;
        int files = 0;
        int roots = 0;
        int entries = 0;
        for (Map.Entry<String, Tally> entry : read.entrySet()) {
            Tally tally = entry.getValue();
            assertTrue(tally.types() > 0, entry.getKey() + ": declares at least one data type");
            assertEquals(tally.types(), tally.files(), entry.getKey()
                    + ": the meta is ONE file per data type - the member never refuses and never says no-file");
            types += tally.types();
            files += tally.files();
            roots += tally.qualifyRoots();
            entries += tally.qualifyEntries();
        }
        System.out.println("IR meta emitter over the " + HOLDOUT_GROUPS.size() + " hold-out groups with a meta"
                + " golden: types=" + types + " files=" + files + " qualifyRoots=" + roots
                + " qualifyEntries=" + entries);
        for (Map.Entry<String, Tally> entry : read.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
        }
        assertEquals(types, files, "every data type of every group was compared as ONE file");

        // THE QUALIFY WING WAS ACTUALLY ENTERED (LAW 75 / 76's shape: an instrument blind to the arm it claims):
        // the six qualify batteries between them must carry at least one ROOT with at least one function, or this
        // whole run agreed about `Collections.emptyList()` and nothing else.
        int qualifyRootsInQualifyGroups = 0;
        int qualifyEntriesInQualifyGroups = 0;
        for (String group : QUALIFY_GROUPS) {
            qualifyRootsInQualifyGroups += read.get(group).qualifyRoots();
            qualifyEntriesInQualifyGroups += read.get(group).qualifyEntries();
        }
        System.out.println("  the qualify batteries " + QUALIFY_GROUPS + " exercised roots="
                + qualifyRootsInQualifyGroups + " entries=" + qualifyEntriesInQualifyGroups);
        assertTrue(qualifyRootsInQualifyGroups > 0,
                "the qualify batteries exercised NO root - the non-empty arm of getQualifyFunctions was never"
                        + " rendered, so this run proved only the empty one");
        assertTrue(qualifyEntriesInQualifyGroups > 0,
                "the qualify batteries exercised no qualify FUNCTION - a root with an empty list renders the same"
                        + " text a non-root does");
    }

    /**
     * The member contract: the meta answers a PRESENT text for every validated data type. It may never say "no file
     * by law", which only the deep-path util may ({@link IRTypeUnit.Member#mayWriteNoFile}) - and a unit whose META
     * member said it would be REFUSED BY NAME, which is what {@code IRTypeUnitTest} states from the unit's side.
     */
    @Test
    void theMetaMemberNeverAnswersNoFileByLaw() {
        IRPropertyModelTest.Fixture f = IRPropertyModelTest.fixture();
        IRDerivedFacts facts = new IRDerivedFacts(f.index(), IRDerivedLie.NONE);
        IRTypeUnit.MemberEmitter emitter = new IRModelMetaEmitter(facts, modelIndexOf(f), "0.0.0");
        int answers = 0;
        for (var element : f.lib().rootElements()) {
            if (!(element instanceof RDataType dataType)) {
                continue;
            }
            IRTypeNode node = f.index().node(f.lib().namespace(), dataType);
            Optional<String> text = emitter.emit(node);
            assertTrue(text.isPresent(), "the META member never answers NO FILE BY LAW");
            answers++;
        }
        assertTrue(answers > 0, "the fixture exercised at least one type");
    }


    // --------------------------------------------------- THE #306 java.lang COLLISION FIXTURE (commit 13, LAW 76)

    /**
     * THE FIXTURE LANE M7 EXISTS FOR, and the reason it had to be written (READ, not assumed): the #306 law writes
     * a {@code java.lang}-colliding type FULLY QUALIFIED at every TYPE position and imports it nowhere (golden iso
     * {@code ErrorMeta}), and the SAME law applies to each condition ref's DECLARING type. Neither arm is reached
     * by any of the 88 workspaces above - {@code type-named-*} collide with imported LIBRARY types
     * ({@code java.util}, Guava, Rosetta), not with {@code java.lang}, and the corpus's only witnesses are the iso
     * cells. So the first run of lane M7 read GREEN: the mutation moved no byte, which is a lane nobody can read
     * rather than a law nobody broke. This fixture is the witness that makes it readable.
     *
     * <p>{@code Error} is an implicitly-imported {@code java.lang} class, so:
     * <ul>
     *   <li>{@code ErrorMeta} exercises the SUBJECT arm - {@code @RosettaMeta(model=seat9.metalang.Error.class)},
     *       the canonical at every TYPE position and no data-class import - AND the DECLARING arm for its own
     *       condition, which it declares itself;</li>
     *   <li>{@code Sub extends Error} exercises the DECLARING arm across an inheritance boundary: its
     *       {@code dataRules} carries {@code ErrorCodeSet} typed to the canonical {@code seat9.metalang.Error}
     *       with no import, while {@code Sub} itself does not collide and is imported normally.</li>
     * </ul>
     */
    private static final String COLLIDE_JAVA_LANG = """
            namespace seat9.metalang
            version "1.0.0"

            type Error: <"a type whose simple name IS an implicitly-imported java.lang class (the #306 law).">
                code string (1..1)
                condition CodeSet:
                    code exists

            type Sub extends Error: <"a subtype that INHERITS the colliding type's condition.">
                extra string (0..1)
            """;

    @Test
    void theJavaLangCollisionLawHoldsOnTheSubjectTypeAndOnAConditionDeclaringType() {
        RModel model = AstBuilder.buildFromString(COLLIDE_JAVA_LANG, "seat9-metalang.rosetta");
        RWorkspace workspace = RWorkspace.build(List.of(model)).workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> true);
        IRDeclarationReconciler pass = new IRDeclarationReconciler(gm);
        IRTypeIndex index = new IRTypeIndex(workspace, pass.adapter(),
                new IRDeclarationReconciler(gm, pass.adapter()));
        Tally tally = compare("seat9.metalang", gm, index,
                new IRModelIndex(workspace, pass.adapter()));
        assertEquals(2, tally.types(), "the fixture declares Error and Sub");
        assertEquals(2, tally.files(), "both metas were compared, byte for byte, against the old generator");

        // the two arms, NAMED in the rendered text so the fixture cannot silently stop exercising them
        IRDerivedFacts facts = new IRDerivedFacts(index, IRDerivedLie.NONE);
        IRModelMetaEmitter emitter =
                new IRModelMetaEmitter(facts, new IRModelIndex(workspace, pass.adapter()), "1.0.0");
        String errorMeta = emitter.render(index.node("seat9.metalang", dataTypeOf(model, "Error")));
        assertTrue(errorMeta.contains("@RosettaMeta(model=seat9.metalang.Error.class)"),
                "THE SUBJECT ARM: the colliding type is fully qualified at every TYPE position - " + errorMeta);
        assertFalse(errorMeta.contains("import seat9.metalang.Error;"),
                "THE SUBJECT ARM: and it is never imported - " + errorMeta);
        String subMeta = emitter.render(index.node("seat9.metalang", dataTypeOf(model, "Sub")));
        assertTrue(subMeta.contains("factory.<seat9.metalang.Error>create(ErrorCodeSet.class)"),
                "THE DECLARING ARM: an inherited ref is typed to its DECLARING type, fully qualified because that"
                        + " type collides - " + subMeta);
        assertFalse(subMeta.contains("import seat9.metalang.Error;"),
                "THE DECLARING ARM: and the declaring type is not imported either - " + subMeta);
        assertTrue(subMeta.contains("import seat9.metalang.Sub;"),
                "while Sub itself does NOT collide and IS imported - " + subMeta);
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
     * Hold the META member against {@code ModelMetaGenerator} over EVERY model of one workspace. The old generator
     * runs ONCE, over every model, into its own map; a generation error of its own would be a refusal the member
     * has no counterpart for at this commit (the meta generator refuses nothing), so the list is asserted EMPTY
     * rather than indexed by target path.
     */
    private Tally compare(String what, GeneratorModel gm, IRTypeIndex index, IRModelIndex modelIndex) {
        List<RModel> models = new ArrayList<>();
        gm.workspace().files().forEach(models::add);

        Map<String, String> oracle = new LinkedHashMap<>();
        ModelMetaGenerator metaGen = new ModelMetaGenerator(gm, TYPE_TRANSLATOR);
        Set<String> oracleRefusals = new LinkedHashSet<>();
        for (RModel model : models) {
            for (GenerationException error : metaGen.generateClasses(model, gm.version(model), oracle)) {
                oracleRefusals.add(String.valueOf(error.getTargetPath()) + " -> " + error.getMessage());
            }
        }
        assertEquals(Set.of(), oracleRefusals, what
                + ": the old meta generator refused a file - the member has no NAMED counterpart for a meta"
                + " refusal at this commit, so a refusal here is a finding, never a silently skipped key");

        IRDerivedFacts facts = new IRDerivedFacts(index, IRDerivedLie.NONE);
        int types = 0;
        int files = 0;
        int qualifyRoots = 0;
        int qualifyEntries = 0;
        for (RModel model : models) {
            // the emitter is built PER MODEL with that model's version stamp, exactly as IRTypeUnitWiring.unitFor
            // builds it per generateClassesAsIR call - the meta writes the stamp into its own javadoc
            IRModelMetaEmitter emitter = new IRModelMetaEmitter(facts, modelIndex,
                    gm.version(model) == null ? "" : gm.version(model));
            for (var element : model.rootElements()) {
                if (!(element instanceof RDataType dataType)) {
                    continue;
                }
                types++;
                IRTypeNode node = index.node(model.namespace(), dataType);
                String key = IRTypeUnit.outputKey(node, IRTypeUnit.Member.META);
                // THE TWO SPELLINGS OF THE KEY LAW, held together rather than trusted: the unit's own composition
                // and JavaTypeTranslator.toJavaMetaDataClass's canonical path, which is what JavaClassGenerator
                // writes the file under on the OFF route.
                String translatorKey = TYPE_TRANSLATOR
                        .toJavaMetaDataClass(new ModelSymbolId(gm.namespace(dataType), dataType.name()))
                        .getCanonicalName().withForwardSlashes() + ".java";
                assertEquals(translatorKey, key, what + " / " + dataType.name()
                        + ": IRTypeUnit.outputKey and JavaTypeTranslator.toJavaMetaDataClass are ONE law");
                String expected = oracle.get(key);
                assertNotNull(expected, what + " / " + key
                        + ": the old generator neither wrote nor refused this key - the path law disagrees");
                assertEquals(expected, emitter.emit(node).orElseThrow(), what + " / " + key
                        + ": the WHOLE meta file, byte for byte off the old generator's own generateClasses"
                        + " output for the same parsed model");
                files++;
                List<String> qualify = IRModelMetaEmitter.qualifyFunctionClasses(node, modelIndex);
                if (!qualify.isEmpty()) {
                    qualifyRoots++;
                    qualifyEntries += qualify.size();
                }
            }
        }
        return new Tally(types, files, qualifyRoots, qualifyEntries);
    }

    /**
     * The workspace-wide MODEL index of a fixture, on the fixture's OWN pass adapter.
     */
    private static IRModelIndex modelIndexOf(IRPropertyModelTest.Fixture fixture) {
        return new IRModelIndex(fixture.gm().workspace(), fixture.passReconciler().adapter());
    }

    /**
     * The workspace-wide MODEL index of a hold-out workspace, on a FRESH adapter. {@code HoldOutWorkspace} does not
     * carry its pass reconciler, and it does not need to: {@link IRModelIndex} reads only
     * {@link AstToIRAdapter#adaptModelNode}, whose facts (the qualifiable configurations, the
     * {@code [qualification]} functions and their first inputs' RESOLVED QUALIFIED NAMES) are compared to the type
     * nodes BY NAME and never by identity, so no comparison in this member crosses the two adapters.
     */
    private static IRModelIndex modelIndexOf(GeneratorModel gm) {
        return new IRModelIndex(gm.workspace(), new IRDeclarationReconciler(gm).adapter());
    }
}
