package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.object.ModelMetaGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.validators.CardinalityValidatorGenerator;
import com.regnosys.rosetta.generator.java.object.validators.OnlyExistsValidatorGenerator;
import com.regnosys.rosetta.generator.java.object.validators.TypeFormatValidatorGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.testutil.ChaosCell;

/**
 * v3.2 SEAT 11 (PR #632, M1 / D50) — THE FILE-SCOPE FIRST-CLAIM SEAT SUITE: the released plugin's
 * import law pinned at the six data-type kinds the seat moved (the POJO, the XMETA, the CARDINALITY /
 * ONLY_EXISTS / TYPE_FORMAT validators, the DATA_RULE), each witness a shape the seat's five oracle
 * groups ({@code holdout/type-named-*}, 232 goldens pinned from rosetta-maven-plugin:9.83.0 BEFORE the
 * code — {@code target/v32-seat11-instruments/scratch/oracle-s11a.status}, local) or the chaos cell's
 * M1 rows carry.
 *
 * <p><b>The law</b> ({@code ImportingStringConcatenation} over a {@code JavaFileScope}, vendored):
 * the file's own top-level class claims its simple name FIRST; then every TYPE the file writes claims
 * its simple name at its first write in TEXT ORDER; a later different canonical with a taken simple
 * name is written fully qualified and never imported; {@code java.lang} names are implicitly taken.
 * The fork carried it as {@code ImportCollisionResolver} (PR #227) at the function bodies and the
 * data-rule body alone — the five data-type generators and the data-rule template wrote every type
 * name literally and imported every canonical: a duplicate or own-name import beside a bare name,
 * SILENT (no LOUD counter) and NON-COMPILING (the chaos s32 rows, 105 of 105 by javac). Now every type
 * write of the six kinds is a sentinel ({@code ImportCollisionResolver.typeRefOrBare}, the templates'
 * {@code <t.X>} token maps), resolved ONCE per class text from the own class as the seed
 * ({@code ImportCollisionResolver.resolveClass}), the losers dropped from the import list.
 *
 * <p>a1 — a type named {@code List}: its five files write {@code java.util.List} canonical and import
 * it nowhere (the own class claims first). a2 — the sibling ORDERS: {@code HolderModelFirst} (the
 * model List first in text order — {@code java.util.List} canonical, no import; the model List is
 * same-package, bare) and {@code HolderJavaFirst} (a multi string first — {@code java.util.List}
 * imported, the model List canonical). a3 — the class HEADER's library token beats the data class
 * ({@code ValidatorValidator implements Validator<census.seat11rosetta.Validator>}) and the data class
 * beats a body token ({@code ValidatorMeta}'s {@code @RosettaMeta(model=Validator.class)} claims
 * first, the library Validator canonical in the body). a4 — the THREE UPSTREAM LITERALS kept literal,
 * each measured on a golden of the released plugin and each its own NON-COMPILING Java (the compile
 * gate's upstream-bug pins, FINOS-issue riders #6 / #7 in the D11 waiver header - #8, the
 * {@code RosettaModelObject} shadow, is the FOURTH class, pinned at the compile gate and not by a4): the only-exists
 * validator's {@code Map.Entry::getValue} / {@code ::getKey} beside a model type named {@code Map}; the
 * list setter's {@code toCollection(()->new ArrayList<>())} beside a model type named
 * {@code ArrayList} (canonical at every OTHER site); the same-package nested builder
 * {@code List.ListBuilder} written bare under {@code import java.util.List}. a5 — the control: a type
 * whose names collide with nothing renders every type bare and keeps every import (a class text with
 * no collision resolves to the pre-seat bytes — the invariant the 34,686 vendored goldens witness at
 * the chain). a6 — the DATA_RULE kind: {@code ListNonEmpty extends Validator<List>} claims the model
 * List at its header, {@code java.util.List<ValidationResult<?>>} canonical in the body.
 * corpus_c1 — the chaos cell's 105 M1 rows (chaos/s32: the five kinds × 21 placement variants),
 * byte-identical to their goldens as a SET on the default route (the D11's own print at the code head:
 * {@code scratch/d11-a.status}, 105 now-matching on BOTH routes; the rows deleted from the declared
 * baseline at commit 4).
 *
 * <p>Both routes: the IR provider swaps FIVE generators (enum, POJO, choice, metafield, function -
 * {@code IRGenerationProviderImpl}); the POJO's IR subclass reconciles its declaration IR and then delegates
 * the render to the Phase-1 {@code generateClasses} (decision L-001), and the other five kinds here have no
 * IR twin at all - so the six kinds render through the same code on {@code -Pir-on} (LAW 77 by identity;
 * round 1's spec MF-1 / cq MF-1 re-cut the REASON, the verdict unmoved), the D11 both routes the
 * witness at d11-a / d11-b. No test here gates on the IR provider, so the ON-route seat step of the
 * chain runs every test again with 0 skipped.
 */
class FileScopeSeatTest {

    // =========================================================================
    // Fixtures (the oracle groups' shapes in miniature)
    // =========================================================================

    private static final String LIST = """
            namespace census.seat11list
            version "0.0.0"

            type List: <"A data type named List - the chaos s32 shape: its own class claims the file scope first.">
                items string (0..*)
                caption string (0..1)
                condition NonEmpty: <"The sixth kind - a data rule on the List-named type.">
                    items exists

            type HolderModelFirst: <"The model List FIRST in text order: it claims the simple name, java.util.List is written canonical.">
                subject List (0..1)
                names string (0..*)
                others List (0..*)

            type HolderJavaFirst: <"A multi string FIRST: java.util.List claims the simple name, the model List is written canonical.">
                names string (0..*)
                subject List (0..1)
                others List (0..*)
            """;

    private static final String ROSETTA = """
            namespace census.seat11rosetta
            version "0.0.0"

            type Validator: <"A data type named after the library's Validator - the class header's token vs the data class.">
                x string (0..1)
            """;

    private static final String UTIL = """
            namespace census.seat11util
            version "0.0.0"

            type Map: <"A data type named Map - the only-exists validator's Map.Entry literal.">
                k string (0..1)

            type ArrayList: <"A data type named ArrayList with a multi attribute - the list setter's diamond literal.">
                xs string (0..*)
            """;

    private static final String PLAIN = """
            namespace census.seat11plain
            version "0.0.0"

            type Plain: <"The control - names that collide with nothing.">
                a string (0..1)
                bs string (0..*)
            """;

    // =========================================================================
    // a1 — the List-named type: java.util.List canonical, imported nowhere, at all five kinds
    // =========================================================================

    @Test
    void a1_typeNamedList_writesJavaUtilListCanonical_importsItNowhere_atAllFiveKinds() throws IOException {
        Map<String, String> out = render(LIST, "seat11list");
        for (String path : List.of("census/seat11list/List.java",
                "census/seat11list/meta/ListMeta.java",
                "census/seat11list/validation/ListValidator.java",
                "census/seat11list/validation/ListTypeFormatValidator.java",
                "census/seat11list/validation/exists/ListOnlyExistsValidator.java")) {
            String f = file(out, path);
            assertContains(f, "java.util.List<", path);
            assertAbsent(f, "import java.util.List;", path);
        }
        // the POJO's own imports keep every winner (ArrayList, Objects, Consumer, Collectors) - only the loser is dropped
        String pojo = file(out, "census/seat11list/List.java");
        assertContains(pojo, "import java.util.ArrayList;", "the POJO");
        assertContains(pojo, "import java.util.Objects;", "the POJO");
        // the meta's two List-typed method headers, each through the template's <t.List> token (lane L4 makes one literal)
        String meta = file(out, "census/seat11list/meta/ListMeta.java");
        assertContains(meta, "public java.util.List<Validator<? super List>> dataRules(ValidatorFactory factory) {", "the meta");
        assertContains(meta, "public java.util.List<Function<? super List, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {",
                "the meta");
        // the only-exists validator's ONE java.util.List write is the cast at its existence check (lane L6 makes it bare)
        String onlyExists = file(out, "census/seat11list/validation/exists/ListOnlyExistsValidator.java");
        assertContains(onlyExists, "ExistenceChecker.isSet((java.util.List<String>) o.getItems())", "the only-exists validator");
    }

    // =========================================================================
    // a2 — the sibling ORDERS: text order decides, nothing simpler
    // =========================================================================

    @Test
    void a2_siblingOrders_theFirstWriteInTextOrderClaims() throws IOException {
        Map<String, String> out = render(LIST, "seat11list");
        String modelFirst = file(out, "census/seat11list/HolderModelFirst.java");
        assertContains(modelFirst, "java.util.List<String> getNames();", "HolderModelFirst");
        assertContains(modelFirst, "java.util.List<? extends List> getOthers();", "HolderModelFirst");
        assertAbsent(modelFirst, "import java.util.List;", "HolderModelFirst");
        assertAbsent(modelFirst, "census.seat11list.List", "HolderModelFirst (same-package model List: bare)");
        String javaFirst = file(out, "census/seat11list/HolderJavaFirst.java");
        assertContains(javaFirst, "import java.util.List;", "HolderJavaFirst");
        assertContains(javaFirst, "List<String> getNames();", "HolderJavaFirst");
        assertContains(javaFirst, "census.seat11list.List getSubject();", "HolderJavaFirst");
        assertContains(javaFirst, "List<? extends census.seat11list.List> getOthers();", "HolderJavaFirst");
    }

    // =========================================================================
    // a3 — the header's token vs the data class, both directions
    // =========================================================================

    @Test
    void a3_classHeaderTokenBeatsTheDataClass_andTheDataClassBeatsABodyToken() throws IOException {
        Map<String, String> out = render(ROSETTA, "seat11rosetta");
        String validator = file(out, "census/seat11rosetta/validation/ValidatorValidator.java");
        assertContains(validator, "public class ValidatorValidator implements Validator<census.seat11rosetta.Validator>",
                "ValidatorValidator");
        assertContains(validator, "import com.rosetta.model.lib.validation.Validator;", "ValidatorValidator");
        assertAbsent(validator, "import census.seat11rosetta.Validator;", "ValidatorValidator");
        String meta = file(out, "census/seat11rosetta/meta/ValidatorMeta.java");
        assertContains(meta, "@RosettaMeta(model=Validator.class)", "ValidatorMeta");
        assertContains(meta, "import census.seat11rosetta.Validator;", "ValidatorMeta");
        assertContains(meta, "com.rosetta.model.lib.validation.Validator<? super Validator>", "ValidatorMeta");
        assertAbsent(meta, "import com.rosetta.model.lib.validation.Validator;", "ValidatorMeta");
    }

    // =========================================================================
    // a4 — the three upstream literals, kept literal (bug-compat; the compile gate's pins)
    // =========================================================================

    @Test
    void a4_theThreeUpstreamLiterals_areKeptLiteral() throws IOException {
        Map<String, String> util = render(UTIL, "seat11util");
        String mapOnlyExists = file(util, "census/seat11util/validation/exists/MapOnlyExistsValidator.java");
        assertContains(mapOnlyExists, ".filter(Map.Entry::getValue)", "MapOnlyExistsValidator (the Map.Entry literal)");
        assertContains(mapOnlyExists, ".map(Map.Entry::getKey)", "MapOnlyExistsValidator (the Map.Entry literal)");
        assertContains(mapOnlyExists, "java.util.Map<String, Boolean> fieldExistenceMap", "MapOnlyExistsValidator");
        assertAbsent(mapOnlyExists, "import java.util.Map;", "MapOnlyExistsValidator");
        String arrayList = file(util, "census/seat11util/ArrayList.java");
        assertContains(arrayList, "toCollection(()->new ArrayList<>())", "ArrayList (the diamond literal)");
        assertContains(arrayList, "new java.util.ArrayList<>()", "ArrayList (every other site canonical)");
        assertAbsent(arrayList, "import java.util.ArrayList;", "ArrayList");
        Map<String, String> list = render(LIST, "seat11list");
        String javaFirst = file(list, "census/seat11list/HolderJavaFirst.java");
        assertContains(javaFirst, "import java.util.List;", "HolderJavaFirst");
        assertContains(javaFirst, "List.ListBuilder getOrCreateSubject();", "HolderJavaFirst (the nested builder, bare)");
    }

    // =========================================================================
    // a5 — the control: no collision, every type bare, the NAMED imports kept (round 1's cq NIT-4: the name
    // says what is asserted; lane L8 proves the control able to fail)
    // =========================================================================

    @Test
    void a5_control_noCollision_everyTypeBare_namedImportsKept() throws IOException {
        Map<String, String> out = render(PLAIN, "seat11plain");
        for (String path : List.of("census/seat11plain/Plain.java",
                "census/seat11plain/meta/PlainMeta.java",
                "census/seat11plain/validation/PlainValidator.java",
                "census/seat11plain/validation/PlainTypeFormatValidator.java",
                "census/seat11plain/validation/exists/PlainOnlyExistsValidator.java")) {
            String body = classText(file(out, path));
            assertAbsent(body, "java.util.", path + " (a canonical name in the class text)");
            assertAbsent(body, "com.rosetta.", path + " (a canonical name in the class text)");
            assertAbsent(body, "com.google.", path + " (a canonical name in the class text)");
            assertAbsent(body, "census.seat11plain.", path + " (a canonical name in the class text)");
        }
        String pojo = file(out, "census/seat11plain/Plain.java");
        assertContains(pojo, "import java.util.List;", "the control POJO");
        assertContains(pojo, "import java.util.ArrayList;", "the control POJO");
        assertContains(pojo, "List<String> getBs();", "the control POJO");
        String meta = file(out, "census/seat11plain/meta/PlainMeta.java");
        assertContains(meta, "import java.util.List;", "the control meta");
        assertContains(meta, "public List<Validator<? super Plain>> dataRules(ValidatorFactory factory)", "the control meta");
    }

    // =========================================================================
    // a6 — the DATA_RULE kind
    // =========================================================================

    @Test
    void a6_dataRuleKind_theHeaderClaimsTheModelList_theBodyWritesJavaUtilListCanonical() throws IOException {
        Map<String, String> out = render(LIST, "seat11list");
        String rule = file(out, "census/seat11list/validation/datarule/ListNonEmpty.java");
        assertContains(rule, "public interface ListNonEmpty extends Validator<List> {", "ListNonEmpty");
        assertContains(rule, "import census.seat11list.List;", "ListNonEmpty");
        assertContains(rule, "public java.util.List<ValidationResult<?>> getValidationResults(RosettaPath path, List list)",
                "ListNonEmpty");
        assertAbsent(rule, "import java.util.List;", "ListNonEmpty");
    }

    // =========================================================================
    // corpus_c1 — the chaos cell's M1 rows, byte-identical as a SET (default route)
    // =========================================================================

    private static final Set<String> M1_FILE_NAMES = Set.of("List.java", "ListMeta.java", "ListValidator.java",
            "ListTypeFormatValidator.java", "ListOnlyExistsValidator.java");

    @Test
    void corpus_c1_theChaosCellsM1Rows_byteIdenticalToTheirGoldens_asASet() throws IOException {
        Path goldens = ChaosCell.goldens();
        Assumptions.assumeTrue(Files.isDirectory(goldens) && Files.isDirectory(ChaosCell.sources()),
                "the chaos cell is absent - corpus_c1 skipped");
        Path s32 = goldens.resolve("chaos").resolve("s32");
        List<String> carriers;
        try (var stream = Files.walk(s32)) {
            carriers = stream.filter(Files::isRegularFile)
                    .filter(p -> M1_FILE_NAMES.contains(p.getFileName().toString()))
                    .map(p -> goldens.relativize(p).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        }
        assertEquals(105, carriers.size(), "the M1 population is 21 placement variants x 5 kinds: " + carriers);

        var cell = new D11CorpusRegressionTest.CellSpec("chaos", ChaosCell.row().version(), ChaosCell.root());
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(), D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var tt = new JavaTypeTranslator(typeUtil);
        var pojoGen = new ModelObjectGenerator(gm, tt, typeUtil);
        var metaGen = new ModelMetaGenerator(gm, tt);
        var cardinalityGen = new CardinalityValidatorGenerator(gm, tt, typeUtil);
        var onlyExistsGen = new OnlyExistsValidatorGenerator(gm, tt, typeUtil);
        var typeFormatGen = new TypeFormatValidatorGenerator(gm, tt, typeUtil);
        Map<String, String> out = new LinkedHashMap<>();
        List<GenerationException> errors = new ArrayList<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model) && model.namespace().startsWith("chaos.s32")) {
                String version = gm.version(model);
                errors.addAll(pojoGen.generateClasses(model, version, out));
                errors.addAll(metaGen.generateClasses(model, version, out));
                errors.addAll(cardinalityGen.generateClasses(model, version, out));
                errors.addAll(onlyExistsGen.generateClasses(model, version, out));
                errors.addAll(typeFormatGen.generateClasses(model, version, out));
            }
        }
        List<String> failures = new ArrayList<>();
        for (GenerationException e : errors) {
            failures.add("generation error: " + e.getTargetPath() + " - " + e.getMessage());
        }
        for (String carrier : carriers) {
            String generated = out.get(carrier);
            if (generated == null) {
                failures.add(carrier + ": not generated");
                continue;
            }
            String golden = Files.readString(goldens.resolve(carrier)).replace("\r\n", "\n");
            if (!golden.equals(generated.replace("\r\n", "\n"))) {
                failures.add(carrier + ": differs from its golden");
            }
        }
        System.out.println("[FILESCOPE] chaos s32 M1 carriers byte-identical: " + (carriers.size() - failures.size())
                + "/" + carriers.size());
        assertTrue(failures.isEmpty(), "the M1 set (" + failures.size() + "/" + carriers.size() + " not identical):\n  "
                + String.join("\n  ", failures));
    }

    // =========================================================================
    // Harness — the hold-out bars' own all-kinds pipeline over an in-memory model
    // =========================================================================

    private static Map<String, String> render(String source, String label) throws IOException {
        List<Path> builtinFiles = HoldOutByteCompareTest.resolveBuiltinFiles();
        Assumptions.assumeTrue(!builtinFiles.isEmpty(), "rune-dsl builtins absent - the seat suite needs them");
        List<RModel> models = new ArrayList<>();
        for (Path p : builtinFiles) {
            models.add(AstBuilder.buildFromFile(p));
        }
        RModel m = AstBuilder.buildFromString(source, "seat11-" + label + ".rosetta");
        Set<RModel> group = Collections.newSetFromMap(new IdentityHashMap<>());
        group.add(m);
        models.add(m);
        HoldOutByteCompareTest.GenerationRun run = HoldOutByteCompareTest.generateAllKindsFromModels(models, group);
        assertTrue(run.errors().isEmpty(), "no generation error is expected on the seat fixtures: " + run.errorMessages());
        for (Map.Entry<String, String> e : run.output().entrySet()) {   // round 1's cq SF-4: no sentinel reaches an emitted file
            org.junit.jupiter.api.Assertions.assertFalse(com.regnosys.rosetta.generator.java.template.ImportCollisionResolver.hasSentinel(e.getValue()),
                    "a first-claim sentinel leaked into " + e.getKey());
        }
        return run.output();
    }

    private static String file(Map<String, String> out, String path) {
        String s = out.get(path);
        assertTrue(s != null, "not generated: " + path + " (have: " + out.keySet() + ")");
        return s.replace("\r\n", "\n");
    }

    /** Everything after the import block — the text the first-claim law resolves. */
    private static String classText(String file) {
        int last = file.lastIndexOf("\nimport ");
        if (last < 0) {
            return file;
        }
        int eol = file.indexOf('\n', last + 1);
        return eol < 0 ? "" : file.substring(eol + 1);
    }

    private static void assertContains(String text, String needle, String where) {
        assertTrue(text.contains(needle), where + ": expected <" + needle + "> in:\n" + text);
    }

    private static void assertAbsent(String text, String needle, String where) {
        assertFalse(text.contains(needle), where + ": did not expect <" + needle + "> in:\n" + text);
    }
}
