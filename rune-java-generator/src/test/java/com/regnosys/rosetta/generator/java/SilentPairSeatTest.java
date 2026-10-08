package com.regnosys.rosetta.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.function.DroppingDispatchFunctionGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.object.validators.TypeFormatValidatorGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;

/**
 * v3.2 seat 3 — the chaos census's SILENT PAIR: family F12 "silent-whole-function-no-emit" (12
 * declared D11 rows, the s04 {@code C4Speed} dispatch groups in twelve namespaces + the 13th
 * {@code x13half/p1} row filed under F16) and family F9 "condition-wiring-silent-skip" (24 rows:
 * the s08 {@code C8EvenC8NonNeg} data-rule classes and the {@code C8PricedTypeFormatValidator}
 * wiring). The seat landed COUNTERS FIRST — two {@link SilentDegradation.Site}s that refuse with
 * the file's own path — and then the fix; this suite is the witness for both halves.
 *
 * <p>THE MEASURED MECHANISMS (the seat census, both routes byte-identical):
 * <ul>
 *   <li><b>F12</b> — {@code FunctionGenerator} keyed its dispatch groups by BARE NAME across every
 *       generated model; upstream groups same-FILE siblings. Thirteen same-named groups folded
 *       into one file (13× the members) and twelve functions vanished with no counter. The fix
 *       keys the group by (model, name); the counter is an ACCOUNTING pass at the generator's
 *       boundary (every non-variant function has a file or an attributed error).</li>
 *   <li><b>F9</b> — the data-rule walk cased {@code Data} and {@code choice} owners only, and the
 *       type-format validator's alias-condition wing was recorded as "post-9.83" on an
 *       absence-of-carrier inference the released-9.83.0 goldens refute. The fix streams
 *       {@code typeAlias} owners (subject = the alias's base Java type), builds the wing (the
 *       {@code @Inject} fields, {@code runConditions}, the {@code Streams.concat} form), widens
 *       the condition-context predicate to alias owners, types the alias instance for the
 *       literal-widening rung, and — the PARSER half — scopes alias-condition expressions in the
 *       lexical resolution pass (a function CALLED inside an alias condition had reached the
 *       generator symbol-empty).</li>
 * </ul>
 *
 * <p>The fixtures below mirror the seat's ORACLE groups ({@code holdout/alias-conditions},
 * {@code alias-conditions-meta}, {@code func-dispatch-namespaces} — pinned from the released
 * 9.83.0 plugin, byte-locked whole by {@code HoldOutByteCompareTest}); their expected strings are
 * the golden forms, so each assertion is a mutation witness, not a guess — {@code type Single}
 * and its a9 assertions are the one exception, backed by the chaos golden
 * {@code C8PricedTypeFormatValidator} (the one-loop form) rather than a hold-out group. The
 * round-1 review's collision shapes mirror the oracle groups {@code alias-conditions-scope},
 * {@code alias-conditions-twins} and {@code alias-conditions-reserved} the same way (a12–a14),
 * the round-2 review's file-scope shapes {@code alias-conditions-filescope} (a15, a16) and the
 * banked {@code alias-conditions-boilerplate} (a17 — the fork refuses it; a18, round 3, the
 * java.lang-named condition class refused at both seats), and the two register
 * belts the round-1 review asked for carry their own fixtures (control7, control8), the writer
 * seam's cross-kind arm a seeded output map (control9 — the lane set's run-3 catch). The chaos
 * carriers are byte-locked whole over every placement variant (the corpus locks), the three
 * counters are proven able to fire (control5 / control6 / a17 / a18), the default route's populations
 * are pinned (control2a) and LAW 77 holds on the IR route (control2b, its positive control an
 * IR-only mutation lane).
 */
class SilentPairSeatTest {

    private static final Path REPO_ROOT =
            Path.of(System.getProperty("user.dir")).resolve("..").normalize();

    private static final List<Path> BUILTINS_SEARCH_ROOTS = List.of(
            REPO_ROOT.resolve("test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-dsl/rune-runtime/src/main/resources/model"),
            REPO_ROOT.resolve("rune-runtime/src/main/resources/model"));

    static boolean builtinsAvailable() {
        return BUILTINS_SEARCH_ROOTS.stream().anyMatch(Files::isDirectory);
    }

    private static final Path CHAOS_ROOT = com.regnosys.rosetta.testutil.ChaosCell.root();
    private static final Path CHAOS_GOLDEN = CHAOS_ROOT.resolve("rosetta-source/src/generated/java");

    static boolean chaosAvailable() {
        return Files.isDirectory(CHAOS_GOLDEN)
                && Files.isDirectory(CHAOS_ROOT.resolve("rosetta-source/src/main/rosetta"));
    }

    static boolean irProviderOnClasspath() {
        try {
            Class.forName("com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static boolean builtinsAndIrProviderAvailable() {
        return builtinsAvailable() && irProviderOnClasspath();
    }

    /** The placement families of the two seats: s08 the twelve; s04 fourteen since chaos-1.1.0 (the twelve + x13half + a8pkg, below). */
    private static final List<String> FAMILIES_12 = List.of(
            "a1o1", "a1o2", "a1o3", "a1o4", "a2alias", "a2dangle", "a2qual", "a2wild",
            "a3half", "a3hub", "a3third", "base");
    /**
     * chaos-1.1.0 (v3.2 seat 10, D49): s04 carries a FOURTEENTH — the A8 output-package axis's {@code a8pkg}
     * variant (a rival TYPE in {@code <ns>.functions} beside the seed's same-named function; C4Speed emits beside it
     * byte-identical). Round 1: the c6 verify gate's catch — the lock had counted thirteen (the whole-suite law).
     */
    private static final List<String> FAMILIES_14 = List.of(
            "a1o1", "a1o2", "a1o3", "a1o4", "a2alias", "a2dangle", "a2qual", "a2wild",
            "a3half", "a3hub", "a3third", "a8pkg", "base", "x13half");

    // =========================================================================
    // The F9 fixture — the oracle group alias-conditions (+ the meta and param shapes)
    // =========================================================================

    private static final String F9_MODEL = """
            namespace census.seat3f9
            version "0.0.0.test"

            metaType scheme string

            typeAlias EvenNat: <"int-grained alias with one condition on the implicit item.">
                number(fractionalDigits: 0)
                condition NonNeg:
                    item >= 0

            typeAlias Pct: <"number alias with two conditions.">
                number(min: 0)
                condition Capped:
                    item <= 100
                condition NotHalf:
                    item <> 50

            typeAlias Code3: <"string alias with a condition.">
                string(minLength: 3, maxLength: 3)
                condition NotPlaceholder:
                    item <> "XXX"

            typeAlias Flag: <"boolean alias with a condition.">
                boolean
                condition MustHold:
                    item = True

            typeAlias Nested: <"alias OF a conditioned alias, carrying its own condition.">
                EvenNat
                condition Small:
                    item < 1000

            typeAlias Unnamed: <"an UNNAMED condition on an alias.">
                number(fractionalDigits: 0)
                condition:
                    item > 0

            typeAlias Plain: <"an alias with NO condition.">
                number(min: 0)

            func IsOk: <"a function an alias condition depends on.">
                inputs:
                    v number (1..1)
                output:
                    ok boolean (1..1)
                set ok: v > 0

            typeAlias Checked: <"an alias whose condition CALLS a function with the implicit item.">
                number
                condition Ok:
                    IsOk(item)

            typeAlias Bounded(max int): <"a PARAMETERISED alias with a condition - REFUSED (the parameters wing is post-9.83).">
                number(max: max)
                condition UnderMax:
                    item <= max

            type Holder: <"alias-typed attributes at every cardinality, plus the type's OWN condition.">
                one EvenNat (1..1)
                opt EvenNat (0..1)
                many EvenNat (0..*)
                some EvenNat (1..*)
                pct Pct (0..1)
                pcts Pct (0..*)
                code Code3 (0..1)
                flag Flag (0..1)
                nested Nested (0..1)
                nesteds Nested (0..*)
                unnamed Unnamed (0..1)
                checked Checked (0..1)
                plain Plain (0..1)
                condition HolderOk:
                    one > 0

            type Single: <"ONE multi conditioned-alias attribute - the bare loop index (the chaos C8Priced form).">
                evens EvenNat (0..*)

            type Bare: <"NO alias-conditioned attribute - the simple form.">
                qty Plain (0..1)
                text string (0..1)
                    [metadata scheme]

            type MetaHolder: <"conditioned-alias attributes under [metadata scheme], single and multi.">
                schemed EvenNat (0..1)
                    [metadata scheme]
                schemeds EvenNat (0..*)
                    [metadata scheme]

            type ParamHolder: <"parameterised-alias attributes - the validator refuses too.">
                bounded Bounded(max: 10) (0..1)

            typeAlias Pa: <"round 1: a condition class named Path - the wing's own class-scope identifier.">
                number(fractionalDigits: 0)
                condition th:
                    item >= 0

            typeAlias Re: <"round 1: a condition class named Results - the wing's own method-scope identifier.">
                number(fractionalDigits: 0)
                condition sults:
                    item >= 0

            type Clash: <"round 1 (oracle group alias-conditions-scope): multi attributes named after the wing's method-scope identifiers and its loop index.">
                results EvenNat (0..*)
                o EvenNat (0..*)
                i EvenNat (0..*)

            type Loop: <"round 1: ONE multi attribute named i - the index and the local want one name.">
                i EvenNat (0..*)

            type FieldClash: <"round 1: a multi attribute named after the injected condition field.">
                evenNatNonNeg EvenNat (0..*)

            type Keywords: <"round 1 (oracle group alias-conditions-reserved): Java-keyword attribute names.">
                new EvenNat (0..*)
                final EvenNat (0..*)

            type PathClash: <"round 1: the injected field wants the name path.">
                pa Pa (0..1)
                pas Pa (0..*)

            type ResultsClash: <"round 1: the injected field wants the name results.">
                re Re (0..*)

            type Both: <"round 1 (oracle group alias-conditions-twins): two same-named aliases from two namespaces - the condition classes share a simple name.">
                left census.seat3f9.a.EvenNat (0..*)
                right census.seat3f9.b.EvenNat (0..1)
            """;

    /** The twin namespaces of {@code type Both} — each declares its own {@code EvenNat} + {@code NonNeg}. */
    /** Round 2 (oracle group alias-conditions-filescope): the file scope's claims — the own class name, the written types. */
    private static final String F9_FILESCOPE = """
            namespace census.seat3fs
            version "0.0.0.test"

            typeAlias Nat: <"int-grained alias with one condition.">
                number(fractionalDigits: 0)
                condition NonNeg:
                    item >= 0

            typeAlias OwnTypeFormat: <"an alias whose condition class is named OwnTypeFormatValidator - the validator's OWN simple name.">
                number(fractionalDigits: 0)
                condition Validator:
                    item >= 0

            type Own: <"the validator wires a condition class of its own simple name.">
                v OwnTypeFormat (0..1)
                vs OwnTypeFormat (0..*)

            type Imported: <"multi attributes named after types the file writes.">
                Streams Nat (0..*)
                ArrayList Nat (0..*)
                Inject Nat (0..*)
                Integer Nat (0..*)
            """;

    /** Round 2 (oracle group alias-conditions-boilerplate, BANKED — the fork refuses): condition classes named after boilerplate imports. */
    private static final String F9_BOILER = """
            namespace census.seat3fb
            version "0.0.0.test"

            typeAlias Validation: <"an alias whose condition class is named ValidationResult - a boilerplate import written after the fields.">
                number(fractionalDigits: 0)
                condition Result:
                    item >= 0

            typeAlias Array: <"an alias whose condition class is named ArrayList - the wing's own import.">
                number(fractionalDigits: 0)
                condition List:
                    item >= 0

            type Boiler: <"attributes wired to condition classes named after boilerplate imports.">
                validation Validation (0..*)
                array Array (0..*)
            """;

    /** Round 3 (cq MF-1): a condition class named after the java.lang element type the validator's multi local writes. */
    private static final String F9_JAVALANG = """
            namespace census.seat3fj
            version "0.0.0.test"

            typeAlias Int: <"an alias whose condition class is named Integer - the java.lang element type of the validator's multi local, never imported.">
                number(fractionalDigits: 0)
                condition eger:
                    item >= 0

            type Boxed: <"a multi attribute wired to a condition class named after its own java.lang element type.">
                xs Int (0..*)
            """;

    private static final String F9_TWIN_A = """
            namespace census.seat3f9.a
            version "0.0.0.test"

            typeAlias EvenNat: <"the same alias + condition name as census.seat3f9.b's.">
                number(fractionalDigits: 0)
                condition NonNeg:
                    item >= 0
            """;

    private static final String F9_TWIN_B = """
            namespace census.seat3f9.b
            version "0.0.0.test"

            typeAlias EvenNat: <"the same alias + condition name as census.seat3f9.a's.">
                number(fractionalDigits: 0)
                condition NonNeg:
                    item >= 0
            """;

    // =========================================================================
    // The F12 fixtures — the oracle group func-dispatch-namespaces (four files)
    // =========================================================================

    private static final String F12_A = """
            namespace census.seat3f12.a
            version "0.0.0.test"

            enum ModeA:
                Fast
                Slow

            func Speed: <"dispatch BASE in namespace a.">
                inputs:
                    mode ModeA (1..1)
                    x number (1..1)
                output:
                    y number (1..1)

            func Speed(mode: ModeA -> Fast):
                set y: x * 2

            func Speed(mode: ModeA -> Slow):
                set y: x / 2
            """;

    private static final String F12_B = """
            namespace census.seat3f12.b
            version "0.0.0.test"

            enum ModeB:
                Fast
                Slow

            func Speed: <"dispatch BASE in namespace b - the same group name as a.">
                inputs:
                    mode ModeB (1..1)
                    x number (1..1)
                output:
                    y number (1..1)

            func Speed(mode: ModeB -> Fast):
                set y: x * 3

            func Speed(mode: ModeB -> Slow):
                set y: x / 3
            """;

    private static final String F12_C = """
            namespace census.seat3f12.c
            version "0.0.0.test"

            func Speed: <"a STANDARD function carrying the group's name in a third namespace.">
                inputs:
                    x number (1..1)
                output:
                    y number (1..1)
                set y: x * 4
            """;

    /** control7's shape (round-1 cq SF-1): a base in one file, its variants in ANOTHER file of the same namespace. */
    private static final String F12_CROSSFILE_BASE = """
            namespace census.seat3f12x.cross
            version "0.0.0.test"

            enum ModeX:
                Fast
                Slow

            func Speed: <"a base whose variants live in another file of this namespace.">
                inputs:
                    mode ModeX (1..1)
                    x number (1..1)
                output:
                    y number (1..1)
            """;

    private static final String F12_CROSSFILE_VARIANTS = """
            namespace census.seat3f12x.cross
            version "0.0.0.test"

            func Speed(mode: ModeX -> Fast): <"a variant whose base is in another file.">
                set y: x * 6

            func Speed(mode: ModeX -> Slow):
                set y: x / 6
            """;

    /** control8's shape (round-1 cq SF-1): two same-named STANDARD functions in two files of one namespace. */
    private static final String F12_DUPLICATE_ONE = """
            namespace census.seat3f12y.dup
            version "0.0.0.test"

            func Speed: <"the first of two same-named standard functions in one namespace.">
                inputs:
                    x number (1..1)
                output:
                    y number (1..1)
                set y: x * 7
            """;

    private static final String F12_DUPLICATE_TWO = """
            namespace census.seat3f12y.dup
            version "0.0.0.test"

            func Speed: <"the second - its file would overwrite the first's.">
                inputs:
                    x number (1..1)
                output:
                    y number (1..1)
                set y: x * 8
            """;

    private static final String F12_D = """
            namespace census.seat3f12.d
            version "0.0.0.test"

            enum ModeD:
                Fast
                Slow

            func Speed(mode: ModeD -> Fast): <"a VARIANT declared BEFORE its base.">
                set y: x * 5

            func Speed(mode: ModeD -> Slow):
                set y: x / 5

            func Speed: <"dispatch BASE in namespace d, declared after its variants.">
                inputs:
                    mode ModeD (1..1)
                    x number (1..1)
                output:
                    y number (1..1)
            """;

    // =========================================================================
    // F9 — the alias-owned data-rule class
    // =========================================================================

    @Test
    @EnabledIf("builtinsAvailable")
    void a1_aliasConditionClass_intSubject() throws IOException {
        String code = datarule("EvenNatNonNeg.java");
        assertContains(code, "public interface EvenNatNonNeg extends Validator<Integer> {");
        assertContains(code, "String DEFINITION = \"item >= 0\";");
        assertContains(code, "getValidationResults(RosettaPath path, Integer evenNat)");
        assertContains(code, "ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, \"EvenNat\", path, DEFINITION)");
        assertContains(code, "return greaterThanEquals(MapperS.of(evenNat), MapperS.of(0), CardinalityOperator.All);");
        assertAbsent(code, "import java.math.BigDecimal;");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a2_aliasConditionClass_bigDecimalSubject_widensTheLiteral() throws IOException {
        String code = datarule("PctCapped.java");
        assertContains(code, "extends Validator<BigDecimal> {");
        assertContains(code, "import java.math.BigDecimal;");
        assertContains(code, "return lessThanEquals(MapperS.of(pct), MapperS.of(BigDecimal.valueOf(100)), CardinalityOperator.All);");
        String second = datarule("PctNotHalf.java");
        // the whole comparison as the golden has it — the group's only CardinalityOperator.Any
        assertContains(second, "return notEqual(MapperS.of(pct), MapperS.of(BigDecimal.valueOf(50)), CardinalityOperator.Any);");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a3_stringAndBooleanSubjects() throws IOException {
        String code3 = datarule("Code3NotPlaceholder.java");
        assertContains(code3, "extends Validator<String> {");
        assertContains(code3, "return notEqual(MapperS.of(code3), MapperS.of(\"XXX\"), CardinalityOperator.Any);");
        String flag = datarule("FlagMustHold.java");
        assertContains(flag, "extends Validator<Boolean> {");
        assertContains(flag, "return areEqual(MapperS.of(flag), MapperS.of(true), CardinalityOperator.All);");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a4_unnamedAliasCondition_takesTheCountAllNamedLaw() throws IOException {
        String code = datarule("UnnamedDataRule0.java");
        assertContains(code, "@RosettaDataRule(\"UnnamedDataRule0\")");
        assertContains(code, "\tString NAME = \"UnnamedDataRule0\";\n\tString DEFINITION = \"item > 0\";");
        assertContains(code, "getValidationResults(RosettaPath path, Integer unnamed)");
        // the count-all-named law the test is named for: the two STRING positions carry the ALIAS
        // name, not the rule's (round-1 cq SF-7)
        assertContains(code, "ValidationResult.success(NAME, ValidationResult.ValidationType.DATA_RULE, \"Unnamed\", path, DEFINITION)");
        assertContains(code, "ValidationResult.failure(NAME, ValidationResult.ValidationType.DATA_RULE, \"Unnamed\", path, DEFINITION, failureMessage)");
        assertContains(code, "return greaterThan(MapperS.of(unnamed), MapperS.of(0), CardinalityOperator.All);");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a5_functionCalledInsideAnAliasCondition_isInjectedAndInvokedOnTheItem() throws IOException {
        String code = datarule("CheckedOk.java");
        assertContains(code, "import census.seat3f9.functions.IsOk;");
        assertContains(code, "@Inject protected IsOk isOk;");
        assertContains(code, "return ComparisonResult.ofNullSafe(MapperS.of(isOk.evaluate(checked)));");
        assertAbsent(code, "IsOk.evaluate(");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a6_nestedAlias_ownsItsOwnClass_andTheOwnerTypeKeepsItsOwnCondition() throws IOException {
        String nested = datarule("NestedSmall.java");
        assertContains(nested, "extends Validator<Integer> {");
        assertContains(nested, "return lessThan(MapperS.of(nested), MapperS.of(1000), CardinalityOperator.All);");
        String holder = datarule("HolderHolderOk.java");
        assertContains(holder, "extends Validator<Holder> {");
        assertContains(holder, "MapperS.of(holder).<Integer>map(\"getOne\", _holder -> _holder.getOne())");
    }

    // =========================================================================
    // F9 — the type-format validator's alias-condition wing
    // =========================================================================

    @Test
    @EnabledIf("builtinsAvailable")
    void a7_validatorWing_fieldsInFirstAppearanceOrder_andTheStreamsConcatForm() throws IOException {
        String code = validator("HolderTypeFormatValidator.java");
        String fields = "\t@Inject\n\tprotected EvenNatNonNeg evenNatNonNeg;\n"
                + "\t@Inject\n\tprotected PctCapped pctCapped;\n"
                + "\t@Inject\n\tprotected PctNotHalf pctNotHalf;\n"
                + "\t@Inject\n\tprotected Code3NotPlaceholder code3NotPlaceholder;\n"
                + "\t@Inject\n\tprotected FlagMustHold flagMustHold;\n"
                + "\t@Inject\n\tprotected NestedSmall nestedSmall;\n"
                + "\t@Inject\n\tprotected UnnamedDataRule0 unnamedDataRule0;\n"
                + "\t@Inject\n\tprotected CheckedOk checkedOk;\n\n"
                + "\tprivate List<ComparisonResult> getComparisonResults(Holder o) {";
        assertContains(code, fields);
        assertContains(code, "import com.google.common.collect.Streams;");
        assertContains(code, "import java.util.ArrayList;");
        assertContains(code, "import javax.inject.Inject;");
        assertContains(code, "\t\treturn Streams.concat(getComparisonResults(o)\n\t\t\t\t.stream()");
        assertContains(code, "\t\t\t\trunConditions(path, o).stream()\n\t\t\t)\n\t\t\t.collect(toList());");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a8_validatorWing_singleAndMulti_theNumberingLaw_andTheAliasHierarchyOrder() throws IOException {
        String code = validator("HolderTypeFormatValidator.java");
        assertContains(code, "\t\tresults.addAll(evenNatNonNeg.getValidationResults(path.newSubPath(\"one\"), o.getOne()));");
        assertContains(code, "\t\tfinal List<Integer> many = o.getMany();\n\t\tif (many != null) {\n"
                + "\t\t\tfor (int i0 = 0; i0 < many.size(); i0++) {\n"
                + "\t\t\t\tresults.addAll(evenNatNonNeg.getValidationResults(path.newSubPath(\"many\").withIndex(i0), many.get(i0)));\n"
                + "\t\t\t}\n\t\t}");
        assertContains(code, "for (int i1 = 0; i1 < some.size(); i1++)");
        assertContains(code, "\t\tfinal List<BigDecimal> pcts = o.getPcts();");
        assertContains(code, "\t\t\t\tresults.addAll(pctCapped.getValidationResults(path.newSubPath(\"pcts\").withIndex(i2), pcts.get(i2)));\n"
                + "\t\t\t\tresults.addAll(pctNotHalf.getValidationResults(path.newSubPath(\"pcts\").withIndex(i2), pcts.get(i2)));");
        // the alias hierarchy: the OUTER alias's condition first, then the inner's
        assertContains(code, "\t\tresults.addAll(nestedSmall.getValidationResults(path.newSubPath(\"nested\"), o.getNested()));\n"
                + "\t\tresults.addAll(evenNatNonNeg.getValidationResults(path.newSubPath(\"nested\"), o.getNested()));");
        assertContains(code, "for (int i3 = 0; i3 < nesteds.size(); i3++)");
        // the wing is independent of the constraint envelope: the boolean alias wires without a check entry
        assertContains(code, "\t\tresults.addAll(flagMustHold.getValidationResults(path.newSubPath(\"flag\"), o.getFlag()));");
        assertAbsent(code, "checkNumber(\"flag\"");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a9_validatorWing_oneLoop_keepsTheBareIndex_theChaosForm() throws IOException {
        String code = validator("SingleTypeFormatValidator.java");
        assertContains(code, "\t\tfinal List<Integer> evens = o.getEvens();\n\t\tif (evens != null) {\n"
                + "\t\t\tfor (int i = 0; i < evens.size(); i++) {\n"
                + "\t\t\t\tresults.addAll(evenNatNonNeg.getValidationResults(path.newSubPath(\"evens\").withIndex(i), evens.get(i)));");
        assertAbsent(code, "int i0 = 0");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a10_validatorWing_metaWrapped_hoistsTheWrapperAndDerefsNullSafely() throws IOException {
        String code = validator("MetaHolderTypeFormatValidator.java");
        assertContains(code, "import com.rosetta.model.metafields.FieldWithMetaInteger;");
        assertContains(code, "\t\tfinal FieldWithMetaInteger fieldWithMetaInteger0 = o.getSchemed();\n"
                + "\t\tresults.addAll(evenNatNonNeg.getValidationResults(path.newSubPath(\"schemed\"), (fieldWithMetaInteger0 == null ? null : fieldWithMetaInteger0.getValue())));");
        assertContains(code, "\t\tfinal List<? extends FieldWithMetaInteger> schemeds = o.getSchemeds();\n\t\tif (schemeds != null) {\n"
                + "\t\t\tfor (int i = 0; i < schemeds.size(); i++) {\n"
                + "\t\t\t\tfinal FieldWithMetaInteger fieldWithMetaInteger1 = schemeds.get(i);\n"
                + "\t\t\t\tresults.addAll(evenNatNonNeg.getValidationResults(path.newSubPath(\"schemeds\").withIndex(i), (fieldWithMetaInteger1 == null ? null : fieldWithMetaInteger1.getValue())));");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void a11_simpleForm_unchangedBesideTheWing() throws IOException {
        String code = validator("BareTypeFormatValidator.java");
        assertAbsent(code, "@Inject");
        assertAbsent(code, "runConditions");
        assertAbsent(code, "Streams");
        assertContains(code, "public class BareTypeFormatValidator implements Validator<Bare> {\n\n\tprivate List<ComparisonResult> getComparisonResults(Bare o) {");
        assertContains(code, "\t}\n\n\t@Override\n\tpublic List<ValidationResult<?>> getValidationResults(RosettaPath path, Bare o) {\n\t\treturn getComparisonResults(o)\n\t\t\t.stream()");
    }

    /**
     * a12 (round-1 cq MF-2, oracle group alias-conditions-scope): upstream's scope law under
     * collision — an attribute-named local that a parent scope already holds is escaped with a
     * {@code _} prefix ({@code _results}, {@code _o} against the method's own identifiers;
     * {@code _evenNatNonNeg} against the injected field); same-named requests in one body scope
     * are numbered from 0 with the loop index created BEFORE the attribute local ({@code i0..i2}
     * the indices, {@code i3} the local named i; {@code i1} / {@code i0} for the one-attribute
     * Loop). Every expected string is the golden's FORM under the fixture's own identifiers
     * ({@code evenNatNonNeg} where the golden reads {@code natNonNeg} — round-2 cq N-8).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a12_validatorWing_scopeCollisions_theEscapeAndNumberingLaws() throws IOException {
        String clash = validator("ClashTypeFormatValidator.java");
        assertContains(clash, "\t\tfinal List<Integer> _results = o.getResults();\n\t\tif (_results != null) {\n"
                + "\t\t\tfor (int i0 = 0; i0 < _results.size(); i0++) {\n"
                + "\t\t\t\tresults.addAll(evenNatNonNeg.getValidationResults(path.newSubPath(\"results\").withIndex(i0), _results.get(i0)));");
        assertContains(clash, "\t\tfinal List<Integer> _o = o.getO();\n\t\tif (_o != null) {\n\t\t\tfor (int i1 = 0; i1 < _o.size(); i1++) {");
        assertContains(clash, "\t\tfinal List<Integer> i3 = o.getI();\n\t\tif (i3 != null) {\n"
                + "\t\t\tfor (int i2 = 0; i2 < i3.size(); i2++) {\n"
                + "\t\t\t\tresults.addAll(evenNatNonNeg.getValidationResults(path.newSubPath(\"i\").withIndex(i2), i3.get(i2)));");
        assertContains(clash, "\t\tList<ValidationResult<?>> results = new ArrayList();");
        String loop = validator("LoopTypeFormatValidator.java");
        assertContains(loop, "\t\tfinal List<Integer> i1 = o.getI();\n\t\tif (i1 != null) {\n"
                + "\t\t\tfor (int i0 = 0; i0 < i1.size(); i0++) {\n"
                + "\t\t\t\tresults.addAll(evenNatNonNeg.getValidationResults(path.newSubPath(\"i\").withIndex(i0), i1.get(i0)));");
        String field = validator("FieldClashTypeFormatValidator.java");
        assertContains(field, "\t@Inject\n\tprotected EvenNatNonNeg evenNatNonNeg;\n");
        assertContains(field, "\t\tfinal List<Integer> _evenNatNonNeg = o.getEvenNatNonNeg();\n\t\tif (_evenNatNonNeg != null) {\n"
                + "\t\t\tfor (int i = 0; i < _evenNatNonNeg.size(); i++) {\n"
                + "\t\t\t\tresults.addAll(evenNatNonNeg.getValidationResults(path.newSubPath(\"evenNatNonNeg\").withIndex(i), _evenNatNonNeg.get(i)));");
    }

    /**
     * a13 (round-1 cq MF-3, oracle group alias-conditions-twins): two condition classes of ONE
     * simple name from two namespaces — the class-scope fields are numbered from 0 in
     * first-appearance order, the first holds the import and the second is written fully
     * qualified; each attribute calls its own.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a13_validatorWing_twinConditionClasses_numberedFieldsAndTheFqnInline() throws IOException {
        String both = validator("BothTypeFormatValidator.java");
        assertContains(both, "import census.seat3f9.a.validation.datarule.EvenNatNonNeg;\n");
        assertAbsent(both, "import census.seat3f9.b.validation.datarule.EvenNatNonNeg;");
        assertAbsent(both, "import census.seat3f9.validation.datarule.EvenNatNonNeg;");
        assertContains(both, "\t@Inject\n\tprotected EvenNatNonNeg evenNatNonNeg0;\n"
                + "\t@Inject\n\tprotected census.seat3f9.b.validation.datarule.EvenNatNonNeg evenNatNonNeg1;\n");
        assertContains(both, "\t\t\t\tresults.addAll(evenNatNonNeg0.getValidationResults(path.newSubPath(\"left\").withIndex(i), left.get(i)));");
        assertContains(both, "\t\tresults.addAll(evenNatNonNeg1.getValidationResults(path.newSubPath(\"right\"), o.getRight()));");
        // the twin namespaces' own datarule classes exist, one each, beside the main namespace's
        Render r = render();
        assertTrue(r.output().containsKey("census/seat3f9/a/validation/datarule/EvenNatNonNeg.java")
                && r.output().containsKey("census/seat3f9/b/validation/datarule/EvenNatNonNeg.java")
                && r.output().containsKey("census/seat3f9/validation/datarule/EvenNatNonNeg.java"),
                r.output().keySet().toString());
    }

    /**
     * a14 (round-1 cq MF-2, oracle group alias-conditions-reserved): a Java-keyword attribute name
     * is escaped ({@code _new}, {@code _final}); a condition class named after the wing's own
     * identifiers renumbers or escapes exactly as upstream's scopes do — a field {@code Path}
     * numbers the class-scope pair ({@code path0} the parameter at EVERY path position of the
     * file, {@code path1} the field); a field {@code Results} pushes the method's local to
     * {@code _results}.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a14_validatorWing_reservedNames_keywordsAndTheWingsOwnIdentifiers() throws IOException {
        String kw = validator("KeywordsTypeFormatValidator.java");
        assertContains(kw, "\t\tfinal List<Integer> _new = o.getNew();\n\t\tif (_new != null) {\n"
                + "\t\t\tfor (int i0 = 0; i0 < _new.size(); i0++) {\n"
                + "\t\t\t\tresults.addAll(evenNatNonNeg.getValidationResults(path.newSubPath(\"new\").withIndex(i0), _new.get(i0)));");
        assertContains(kw, "\t\tfinal List<Integer> _final = o.getFinal();\n\t\tif (_final != null) {\n\t\t\tfor (int i1 = 0; i1 < _final.size(); i1++) {");
        String pc = validator("PathClashTypeFormatValidator.java");
        assertContains(pc, "\t@Inject\n\tprotected Path path1;\n");
        assertContains(pc, "\tprivate List<ValidationResult<?>> runConditions(RosettaPath path0, PathClash o) {\n"
                + "\t\tList<ValidationResult<?>> results = new ArrayList();\n"
                + "\t\tresults.addAll(path1.getValidationResults(path0.newSubPath(\"pa\"), o.getPa()));\n"
                + "\t\tfinal List<Integer> pas = o.getPas();\n\t\tif (pas != null) {\n"
                + "\t\t\tfor (int i = 0; i < pas.size(); i++) {\n"
                + "\t\t\t\tresults.addAll(path1.getValidationResults(path0.newSubPath(\"pas\").withIndex(i), pas.get(i)));");
        assertContains(pc, "\tpublic List<ValidationResult<?>> getValidationResults(RosettaPath path0, PathClash o) {");
        assertContains(pc, "return failure(\"PathClash\", ValidationResult.ValidationType.TYPE_FORMAT, \"PathClash\", path0, \"\", res.getError());");
        assertContains(pc, "return success(\"PathClash\", ValidationResult.ValidationType.TYPE_FORMAT, \"PathClash\", path0, \"\");");
        assertContains(pc, "\t\t\t\trunConditions(path0, o).stream()");
        assertAbsent(pc, "RosettaPath path,");
        String rc = validator("ResultsClashTypeFormatValidator.java");
        assertContains(rc, "\t@Inject\n\tprotected Results results;\n");
        assertContains(rc, "\tprivate List<ValidationResult<?>> runConditions(RosettaPath path, ResultsClash o) {\n"
                + "\t\tList<ValidationResult<?>> _results = new ArrayList();\n"
                + "\t\tfinal List<Integer> re = o.getRe();\n\t\tif (re != null) {\n"
                + "\t\t\tfor (int i = 0; i < re.size(); i++) {\n"
                + "\t\t\t\t_results.addAll(results.getValidationResults(path.newSubPath(\"re\").withIndex(i), re.get(i)));\n"
                + "\t\t\t}\n\t\t}\n\t\treturn _results;\n\t}\n");
        // the simple form is untouched by the path threading: `path` at every position
        assertContains(validator("BareTypeFormatValidator.java"), "getValidationResults(RosettaPath path, Bare o)");
    }

    /**
     * a15 (round-2 cq SF-4, oracle group alias-conditions-filescope): a condition class of the
     * validator's OWN simple name — the class header claims the file's first simple name, so the
     * dependency is written fully qualified and never imported (golden OwnTypeFormatValidator).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a15_validatorWing_ownClassName_theFileScopesFirstClaim() throws IOException {
        Render r = renderModels(List.<String[]>of(new String[] {"seat3fs.rosetta", F9_FILESCOPE}), "census.seat3fs", false);
        String own = pick(r, "census/seat3fs/validation/OwnTypeFormatValidator.java");
        // round-6 cq SF-2 / spec N-1: the OWN_CLASS witness clause is UNRENDERABLE at both seats (the validator's refusal is
        // guarded by its headerClaims, which always holds the class's own name; the data-rule seat stamps only IMPORT /
        // JAVA_LANG) - lane X witnesses the CLAIM through the wiring below; the wording is pinned here beside its siblings'
        // (a17 / a18) so the constant cannot drift unnoticed the day the guard changes
        assertEquals(" (oracle group alias-conditions-filescope)", SilentDegradation.ClaimOrigin.OWN_CLASS.witnessClause);
        assertContains(own, "\t@Inject\n\tprotected census.seat3fs.validation.datarule.OwnTypeFormatValidator ownTypeFormatValidator;\n");
        assertAbsent(own, "import census.seat3fs.validation.datarule.OwnTypeFormatValidator;");
        assertContains(own, "\t\tresults.addAll(ownTypeFormatValidator.getValidationResults(path.newSubPath(\"v\"), o.getV()));\n");
        assertContains(own, "\t\tfinal List<Integer> vs = o.getVs();\n\t\tif (vs != null) {\n\t\t\tfor (int i = 0; i < vs.size(); i++) {\n");
    }

    /**
     * a16 (round-2 spec MF-1 — the file scope, oracle group alias-conditions-filescope): an
     * attribute local named after a type the file WRITES — an import (Streams, ArrayList, Inject)
     * or a java.lang element type its multi local declares (Integer) — escapes with a {@code _}
     * prefix; the loop indices number from 0 across the four (golden ImportedTypeFormatValidator).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a16_validatorWing_localsNamedAfterWrittenTypes_escapeInTheFileScope() throws IOException {
        Render r = renderModels(List.<String[]>of(new String[] {"seat3fs.rosetta", F9_FILESCOPE}), "census.seat3fs", false);
        String imported = pick(r, "census/seat3fs/validation/ImportedTypeFormatValidator.java");
        assertContains(imported, "\t@Inject\n\tprotected NatNonNeg natNonNeg;\n");
        assertContains(imported, "\t\tfinal List<Integer> _Streams = o.getStreams();\n\t\tif (_Streams != null) {\n"
                + "\t\t\tfor (int i0 = 0; i0 < _Streams.size(); i0++) {\n"
                + "\t\t\t\tresults.addAll(natNonNeg.getValidationResults(path.newSubPath(\"Streams\").withIndex(i0), _Streams.get(i0)));\n");
        assertContains(imported, "\t\tfinal List<Integer> _ArrayList = o.getArrayList();\n\t\tif (_ArrayList != null) {\n\t\t\tfor (int i1 = 0; i1 < _ArrayList.size(); i1++) {\n");
        assertContains(imported, "\t\tfinal List<Integer> _Inject = o.getInject();\n\t\tif (_Inject != null) {\n\t\t\tfor (int i2 = 0; i2 < _Inject.size(); i2++) {\n");
        assertContains(imported, "\t\tfinal List<Integer> _Integer = o.getInteger();\n\t\tif (_Integer != null) {\n\t\t\tfor (int i3 = 0; i3 < _Integer.size(); i3++) {\n");
    }

    /**
     * a17 (round 2, oracle group alias-conditions-boilerplate — BANKED, the fork REFUSES the shape):
     * a condition class named after a boilerplate type the validator writes AFTER its fields
     * ({@code ValidationResult}, {@code ArrayList}) — the released plugin claims the condition
     * class first and writes the boilerplate type fully qualified everywhere; the fork's template
     * cannot, so the validator is withheld at {@code BOILERPLATE_NAME_COLLISION}, by TYPE and SITE,
     * with its own path — and so is the data-rule class named {@code ValidationResult} itself, which
     * the fork used to emit importing the runtime's {@code ValidationResult} (javac rejects the
     * import-vs-own-name clash; the oracle writes the runtime type fully qualified inside it — the
     * finding this fixture surfaced). The data rule named {@code ArrayList} (no such datarule import)
     * still emits; nothing else is counted.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a17_validatorWing_conditionClassNamedAfterABoilerplateImport_isRefused() throws IOException {
        Render r = renderModels(List.<String[]>of(new String[] {"seat3fb.rosetta", F9_BOILER}), "census.seat3fb", false);
        List<SilentDegradation.Refusal> refusals = r.refusalsAt(SilentDegradation.Site.BOILERPLATE_NAME_COLLISION);
        // round-5 spec SF-2 / cq SF-3: the witness clause is DECLARED once (SilentDegradation.ClaimOrigin) and asserted at
        // both seats below by the claim's ORIGIN; the wording pinned here so the declaration cannot drift silently
        assertEquals(" (oracle group alias-conditions-boilerplate)", SilentDegradation.ClaimOrigin.IMPORT.witnessClause);
        assertEquals(2, refusals.size(), "the validator AND the ValidationResult data rule refuse, once each: " + r.errors());
        assertTrue(refusals.stream().anyMatch(e -> "census/seat3fb/validation/BoilerTypeFormatValidator.java".equals(e.getTargetPath())
                && e.getMessage().contains("census.seat3fb.validation.datarule.ValidationResult")
                && e.getMessage().contains(SilentDegradation.ClaimOrigin.IMPORT.witnessClause)), refusals.toString());
        assertTrue(refusals.stream().anyMatch(e -> "census/seat3fb/validation/datarule/ValidationResult.java".equals(e.getTargetPath())
                && e.getMessage().contains("com.rosetta.model.lib.validation.ValidationResult")
                && e.getMessage().contains(SilentDegradation.ClaimOrigin.IMPORT.witnessClause)), refusals.toString());
        assertEquals(2, r.errors().size(), "no other error: " + r.errors());
        assertTrue(r.output().keySet().stream().noneMatch(k -> k.endsWith("BoilerTypeFormatValidator.java")
                || k.endsWith("datarule/ValidationResult.java")), "a refused file must not be emitted: " + r.output().keySet());
        assertContains(pick(r, "census/seat3fb/validation/datarule/ArrayList.java"), "public interface ArrayList extends Validator<Integer>");
        assertEquals(2, r.counts().getOrDefault(SilentDegradation.Site.BOILERPLATE_NAME_COLLISION, 0));
        assertEquals(0, r.counts().getOrDefault(SilentDegradation.Site.TYPE_ALIAS_CONDITION_DROPPED, 0));
    }

    /**
     * a18 (round 3, cq MF-1): a condition class named after the java.lang element type the
     * validator's multi local writes ({@code final List<Integer> _xs}) — never imported, so the
     * round-2 claim map could not see it: the validator was emitted importing
     * {@code datarule.Integer} beside a {@code List<Integer>} that means {@code java.lang.Integer}
     * (javac: incompatible types), silently, and the data rule named {@code Integer} declared
     * {@code Validator<Integer>} over its own name. Both seats now REFUSE the shape at
     * {@code BOILERPLATE_NAME_COLLISION}, by TYPE and SITE, each with its own path — the
     * validator through the claim seeded from its wiring plans (lane AD), the data rule through
     * its java.lang subject (lane AE); nothing else is counted (the harness renders data rules,
     * type-format validators and functions — two condition kinds and the function kind — so
     * nothing else is emitted either). No oracle
     * golden: the released plugin would write {@code java.lang.Integer} fully qualified on both
     * files (the boilerplate law), which the two templates cannot — a refusal witness, like a17.
     * Both messages name the witness that EXISTS — the seat fixture, no oracle golden carrying the
     * shape — from the claim's recorded ORIGIN, one declaration for both seats
     * ({@link SilentDegradation.ClaimOrigin}; round-5 spec SF-2 / cq SF-3): the clause asserted at
     * both seats here and in a17 (IMPORT), each seat's render crossed by its own lane (AF / AG).
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void a18_conditionClassNamedAfterItsJavaLangElementType_isRefusedAtBothSeats() throws IOException {
        Render r = renderModels(List.<String[]>of(new String[] {"seat3fj.rosetta", F9_JAVALANG}), "census.seat3fj", false);
        List<SilentDegradation.Refusal> refusals = r.refusalsAt(SilentDegradation.Site.BOILERPLATE_NAME_COLLISION);
        assertEquals(" (no oracle golden carries this shape; seat fixture a18)", SilentDegradation.ClaimOrigin.JAVA_LANG.witnessClause);
        // round-6 cq SF-3 / spec N-2: the claim is ONE typed pair at both seats (SilentDegradation.Claim); its compact
        // constructor refuses a null half - the NPE the data-rule seat's two locals could have thrown on the refusal path
        assertThrows(NullPointerException.class, () -> new SilentDegradation.Claim("java.lang.Integer", null));
        assertThrows(NullPointerException.class, () -> new SilentDegradation.Claim(null, SilentDegradation.ClaimOrigin.JAVA_LANG));
        assertEquals(2, refusals.size(), "the validator AND the Integer data rule refuse, once each: " + r.errors());
        assertTrue(refusals.stream().anyMatch(e -> "census/seat3fj/validation/BoxedTypeFormatValidator.java".equals(e.getTargetPath())
                && e.getMessage().contains("java.lang.Integer")
                && e.getMessage().contains(SilentDegradation.ClaimOrigin.JAVA_LANG.witnessClause)), refusals.toString());
        assertTrue(refusals.stream().anyMatch(e -> "census/seat3fj/validation/datarule/Integer.java".equals(e.getTargetPath())
                && e.getMessage().contains("java.lang.Integer")
                && e.getMessage().contains(SilentDegradation.ClaimOrigin.JAVA_LANG.witnessClause)), refusals.toString());
        assertEquals(2, r.errors().size(), "no other error: " + r.errors());
        assertTrue(r.output().keySet().stream().noneMatch(k -> k.endsWith("BoxedTypeFormatValidator.java")
                || k.endsWith("datarule/Integer.java")), "a refused file must not be emitted: " + r.output().keySet());
        assertTrue(r.output().isEmpty(), "the harness renders data rules, type-format validators and functions only, and both of this fixture's files refuse: " + r.output().keySet());
        assertEquals(2, r.counts().getOrDefault(SilentDegradation.Site.BOILERPLATE_NAME_COLLISION, 0));
        assertEquals(0, r.counts().getOrDefault(SilentDegradation.Site.TYPE_ALIAS_CONDITION_DROPPED, 0));
    }

    /**
     * control6 (LAW 81) — the F9 counter is proven able to fire: the PARAMETERISED alias condition
     * (the released 9.83.0 plugin's self-comparison defect) is REFUSED at BOTH consumer seats,
     * with the file's own path and the site named; nothing is emitted for either file.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void control6_parameterisedAliasCondition_isRefusedLoudlyAtBothSeats() throws IOException {
        Render r = render();
        // by TYPE and SITE (round-1 cq SF-7): a plain GenerationException carrying the same text
        // would not pass here
        List<SilentDegradation.Refusal> refusals = r.refusalsAt(SilentDegradation.Site.TYPE_ALIAS_CONDITION_DROPPED);
        assertEquals(2, refusals.size(), "exactly the datarule + the validator refuse: " + r.errors());
        assertEquals(2, r.errors().size(),
                "the F9 fixture's ONLY errors are the two refusals (the total-error pin): " + r.errors());
        assertTrue(refusals.stream().anyMatch(e -> "census/seat3f9/validation/datarule/BoundedUnderMax.java".equals(e.getTargetPath())
                && e.getMessage().contains("PARAMETERISED typeAlias Bounded(max")), refusals.toString());
        assertTrue(refusals.stream().anyMatch(e -> "census/seat3f9/validation/ParamHolderTypeFormatValidator.java".equals(e.getTargetPath())
                && e.getMessage().contains("ParamHolder.bounded")), refusals.toString());
        assertTrue(r.output().keySet().stream().noneMatch(k -> k.endsWith("BoundedUnderMax.java")
                || k.endsWith("ParamHolderTypeFormatValidator.java")), "a refused file must not be emitted");
        assertEquals(2, r.counts().getOrDefault(SilentDegradation.Site.TYPE_ALIAS_CONDITION_DROPPED, 0),
                "the register counts both refusals");
        assertEquals(0, r.counts().getOrDefault(SilentDegradation.Site.FUNCTION_NOT_EMITTED, 0),
                "the F12 counter stays silent on the F9 fixture");
    }

    // =========================================================================
    // F12 — same-named dispatch groups across namespaces
    // =========================================================================

    @Test
    @EnabledIf("builtinsAvailable")
    void b1_sameNamedDispatchGroups_inTwoNamespaces_emitTwoFilesWithTheirOwnMembers() throws IOException {
        Render r = renderF12();
        String a = pick(r, "census/seat3f12/a/functions/Speed.java");
        String b = pick(r, "census/seat3f12/b/functions/Speed.java");
        assertEquals(2, count(a, "@Inject protected Speed."), "namespace a wires its two variants only:\n" + a);
        assertEquals(2, count(b, "@Inject protected Speed."), "namespace b wires its two variants only:\n" + b);
        assertContains(a, "multiply(MapperS.of(x), MapperS.of(BigDecimal.valueOf(2)))");
        assertContains(b, "multiply(MapperS.of(x), MapperS.of(BigDecimal.valueOf(3)))");
        assertAbsent(a, "BigDecimal.valueOf(3)");
        assertContains(a, "public BigDecimal evaluate(ModeA mode, BigDecimal x) {\n\t\tswitch (mode) {");
        // BOTH variants as the golden has them (round-1 cq SF-7): FAST multiplies, SLOW divides,
        // each wired once and dispatched by its own case
        assertContains(a, "\t@Inject protected Speed.SpeedFAST speedFast;\n\t@Inject protected Speed.SpeedSLOW speedSlow;");
        assertContains(a, "\t\t\tcase FAST:\n\t\t\t\treturn speedFast.evaluate(mode, x);\n\t\t\tcase SLOW:\n\t\t\t\treturn speedSlow.evaluate(mode, x);");
        assertContains(a, "public static abstract class SpeedFAST implements RosettaFunction {");
        assertContains(a, "public static abstract class SpeedSLOW implements RosettaFunction {");
        assertContains(a, "y = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply(MapperS.of(x), MapperS.of(BigDecimal.valueOf(2))).get();");
        assertContains(a, "y = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>divide(MapperS.of(x), MapperS.of(BigDecimal.valueOf(2))).get();");
        assertEquals(0, r.counts().getOrDefault(SilentDegradation.Site.FUNCTION_NOT_EMITTED, 0),
                "every function accounted for: " + r.errors());
        assertTrue(r.errors().isEmpty(), r.errors().toString());
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void b2_standardFunctionSharingTheGroupName_emitsAsAPlainFunction() throws IOException {
        String c = pick(renderF12(), "census/seat3f12/c/functions/Speed.java");
        assertContains(c, "@ImplementedBy(Speed.SpeedDefault.class)\npublic abstract class Speed implements RosettaFunction {");
        assertContains(c, "multiply(MapperS.of(x), MapperS.of(BigDecimal.valueOf(4)))");
        assertAbsent(c, "switch (mode)");
    }

    @Test
    @EnabledIf("builtinsAvailable")
    void b3_variantsDeclaredBeforeTheirBase_stillGroup() throws IOException {
        String d = pick(renderF12(), "census/seat3f12/d/functions/Speed.java");
        assertEquals(2, count(d, "@Inject protected Speed."), d);
        assertContains(d, "public BigDecimal evaluate(ModeD mode, BigDecimal x) {\n\t\tswitch (mode) {");
        assertContains(d, "\t@Inject protected Speed.SpeedFAST speedFast;\n\t@Inject protected Speed.SpeedSLOW speedSlow;");
        assertContains(d, "y = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>multiply(MapperS.of(x), MapperS.of(BigDecimal.valueOf(5))).get();");
        assertContains(d, "y = MapperMaths.<BigDecimal, BigDecimal, BigDecimal>divide(MapperS.of(x), MapperS.of(BigDecimal.valueOf(5))).get();");
    }

    /**
     * control5 (LAW 81) — the F12 counter is proven able to fire: a generator that DROPS every
     * dispatch group's file (the class of defect the bare-name key produced) is caught by the
     * accounting pass — one FUNCTION_NOT_EMITTED refusal per dropped base, each carrying the
     * base's own target path, none for the plain function that did emit.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void control5_accountingCounter_firesForEveryDroppedFunction_withItsPath() throws IOException {
        SilentDegradation.reset();
        try {
            List<RModel> models = new ArrayList<>();
            models.add(model(F12_A, "seat3f12a.rosetta"));
            models.add(model(F12_B, "seat3f12b.rosetta"));
            models.add(model(F12_C, "seat3f12c.rosetta"));
            models.add(model(F12_D, "seat3f12d.rosetta"));
            models.addAll(loadBuiltinsOnly());
            RWorkspace workspace = RWorkspace.build(models).workspace();
            GeneratorModel gm = new GeneratorModel(workspace, m -> m.namespace().startsWith("census.seat3f12"));
            JavaTypeUtil typeUtil = new JavaTypeUtil();
            JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
            // the mutation lives in the generator's own package (the override is package-private):
            // every dispatch group's file is dropped and no error raised
            FunctionGenerator dropping = new DroppingDispatchFunctionGenerator(gm, tt, typeUtil);
            Map<String, String> out = new LinkedHashMap<>();
            List<GenerationException> errors = dropping.generateWithErrors(out);
            List<String> paths = errors.stream().map(GenerationException::getTargetPath).sorted().toList();
            assertEquals(List.of(
                    "census/seat3f12/a/functions/Speed.java",
                    "census/seat3f12/b/functions/Speed.java",
                    "census/seat3f12/d/functions/Speed.java"), paths,
                    "one refusal per dropped dispatch base, attributed to its own path: " + errors);
            assertTrue(errors.stream().allMatch(e -> e instanceof SilentDegradation.Refusal r
                    && r.site() == SilentDegradation.Site.FUNCTION_NOT_EMITTED), errors.toString());
            assertTrue(out.containsKey("census/seat3f12/c/functions/Speed.java"), "the plain function still emits");
            assertEquals(3, SilentDegradation.counts().get(SilentDegradation.Site.FUNCTION_NOT_EMITTED));
        } finally {
            // the register is JVM-global and the module reuses forks (round-1 cq SF-7): a failed
            // control must not leak its count into the next class
            SilentDegradation.reset();
        }
    }

    /**
     * control7 (round-1 cq SF-1, the shape the seat's oracle REFUSED): a dispatch group whose base
     * is declared in ANOTHER file of the same namespace — the released 9.83.0 plugin cannot link
     * the variants ("Couldn't resolve reference to Attribute 'mode'"), and the fork used to render
     * a dispatch class from the first variant's signature AT THE BASE'S PATH, overwriting the base's
     * own emission. It refuses at {@code DISPATCH_BASE_MISSING} with the group's path; the base in
     * its own file still emits as the plain function it is; the accounting pass stays silent.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void control7_dispatchGroupWithItsBaseInAnotherFile_isRefusedAtTheRegister() throws IOException {
        Render r = renderModels(List.<String[]>of(
                new String[] {"seat3f12xbase.rosetta", F12_CROSSFILE_BASE},
                new String[] {"seat3f12xvariants.rosetta", F12_CROSSFILE_VARIANTS}), "census.seat3f12x", false);
        List<SilentDegradation.Refusal> refusals = r.refusalsAt(SilentDegradation.Site.DISPATCH_BASE_MISSING);
        assertEquals(1, refusals.size(), "the baseless group refuses exactly once: " + r.errors());
        assertEquals("census/seat3f12x/cross/functions/Speed.java", refusals.get(0).getTargetPath(),
                "the refusal carries the group's own path");
        assertEquals(1, r.errors().size(), "no other error: " + r.errors());
        assertTrue(r.output().containsKey("census/seat3f12x/cross/functions/Speed.java"),
                "the base in its own file emits as a plain function: " + r.output().keySet());
        assertAbsent(pick(r, "census/seat3f12x/cross/functions/Speed.java"), "switch (mode)");
        assertEquals(1, r.counts().getOrDefault(SilentDegradation.Site.DISPATCH_BASE_MISSING, 0));
        assertEquals(0, r.counts().getOrDefault(SilentDegradation.Site.FUNCTION_NOT_EMITTED, 0),
                "the base emitted; the variants are not counted by the accounting pass");
        if (irProviderOnClasspath()) {
            // round-2 cq N-5: the belt lives in the shared base and no route subclass overrides the
            // grouping — asserted on the IR route, not argued
            Render ir = withIr(() -> renderModels(List.<String[]>of(
                    new String[] {"seat3f12xbase.rosetta", F12_CROSSFILE_BASE},
                    new String[] {"seat3f12xvariants.rosetta", F12_CROSSFILE_VARIANTS}), "census.seat3f12x", true));
            assertEquals(1, ir.refusalsAt(SilentDegradation.Site.DISPATCH_BASE_MISSING).size(),
                    "the IR route refuses the same group: " + ir.errors());
            assertEquals(r.output().keySet(), ir.output().keySet(), "and emits the same files");
        }
    }

    /**
     * control8 (round-1 cq SF-1): two same-named STANDARD functions in two files of one namespace
     * resolve to ONE path — the writer seam refuses the second at {@code FUNCTION_PATH_COLLISION}
     * instead of overwriting the first; the accounting pass, which checks path presence only,
     * stays silent (the path IS present) — the refusal is the belt it cannot be.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void control8_twoFunctionsAtOnePath_theSecondWriterIsRefused() throws IOException {
        Render r = renderModels(List.<String[]>of(
                new String[] {"seat3f12yone.rosetta", F12_DUPLICATE_ONE},
                new String[] {"seat3f12ytwo.rosetta", F12_DUPLICATE_TWO}), "census.seat3f12y", false);
        List<SilentDegradation.Refusal> refusals = r.refusalsAt(SilentDegradation.Site.FUNCTION_PATH_COLLISION);
        assertEquals(1, refusals.size(), "the second writer refuses exactly once: " + r.errors());
        assertEquals("census/seat3f12y/dup/functions/Speed.java", refusals.get(0).getTargetPath());
        assertEquals(1, r.errors().size(), "no other error: " + r.errors());
        String emitted = r.output().get("census/seat3f12y/dup/functions/Speed.java");
        assertNotNull(emitted, "the first writer's file stands: " + r.output().keySet());
        assertContains(normalize(emitted), "BigDecimal.valueOf(7)");
        assertAbsent(normalize(emitted), "BigDecimal.valueOf(8)");
        assertEquals(1, r.counts().getOrDefault(SilentDegradation.Site.FUNCTION_PATH_COLLISION, 0));
        assertEquals(0, r.counts().getOrDefault(SilentDegradation.Site.FUNCTION_NOT_EMITTED, 0),
                "the path is present, so the accounting pass is blind to the overwrite by construction");
        // round-2 cq SF-3: the seam's own record names the FIRST writer, not a cause it does not check
        assertTrue(refusals.get(0).getMessage().contains("written earlier in this run by function"), refusals.get(0).getMessage());
        if (irProviderOnClasspath()) {
            // round-2 cq N-5: the writer seam is the shared base's on every route — asserted on the IR route
            Render ir = withIr(() -> renderModels(List.<String[]>of(
                    new String[] {"seat3f12yone.rosetta", F12_DUPLICATE_ONE},
                    new String[] {"seat3f12ytwo.rosetta", F12_DUPLICATE_TWO}), "census.seat3f12y", true));
            assertEquals(1, ir.refusalsAt(SilentDegradation.Site.FUNCTION_PATH_COLLISION).size(),
                    "the IR route refuses the second writer too: " + ir.errors());
        }
    }

    /**
     * control9 (round-2 cq SF-3; the lane set's catch at commit 9 — lane S went GREEN once the seam's own
     * first-writer record answered control8 ahead of the shared-map check): a file of ANOTHER kind already
     * at a function's path — the shared output map is filled by every class generator before this one
     * runs, so a {@code type} of the function's simple name declared in its {@code .functions}
     * sub-namespace lands first (the fork used to clobber it silently). The seam refuses at the SAME site
     * with the truthful message, the seeded file stands, and the accounting pass stays silent (the path IS
     * present). The seed IS what another generator's emission is to this seam; the harness renders no POJOs.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void control9_fileOfAnotherKindAtTheFunctionsPath_isRefusedAtTheSeam() throws IOException {
        String path = "census/seat3f12/a/functions/Speed.java";
        Render r = renderModels(f12Files(), "census.seat3f12", false, Map.of(path, "// another generator's file"));
        List<SilentDegradation.Refusal> refusals = r.refusalsAt(SilentDegradation.Site.FUNCTION_PATH_COLLISION);
        assertEquals(1, refusals.size(), "the seeded path refuses exactly once: " + r.errors());
        assertEquals(path, refusals.get(0).getTargetPath());
        assertTrue(refusals.get(0).getMessage().contains("emitted earlier in this run by another generator"),
                refusals.get(0).getMessage());
        assertEquals(1, r.errors().size(), "no other error: " + r.errors());
        assertEquals("// another generator's file", r.output().get(path), "the seeded file stands");
        assertEquals(F12_FUNCTIONS, functionKeys(r).size(),
                "the other three groups emit beside the seeded file: " + functionKeys(r));
        assertEquals(1, r.counts().getOrDefault(SilentDegradation.Site.FUNCTION_PATH_COLLISION, 0));
        assertEquals(0, r.counts().getOrDefault(SilentDegradation.Site.FUNCTION_NOT_EMITTED, 0),
                "the accounting pass is silent on BOTH of its grounds — the path is present AND the refusal"
                        + " is attributed to it (round-3 cq N-5: this assertion cannot tell the two apart)");
        if (irProviderOnClasspath()) {
            // round-3 cq SF-2: the seam is the shared base's on every route — asserted on the IR route too (LAW 77)
            Render ir = withIr(() -> renderModels(f12Files(), "census.seat3f12", true, Map.of(path, "// another generator's file")));
            assertEquals(1, ir.refusalsAt(SilentDegradation.Site.FUNCTION_PATH_COLLISION).size(),
                    "the IR route refuses the seeded path too: " + ir.errors());
            assertEquals("// another generator's file", ir.output().get(path), "the seeded file stands on the IR route");
        }
    }

    // =========================================================================
    // The corpus locks — every declared carrier, whole, over every placement variant
    // =========================================================================

    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c1_C4Speed_allFourteenVariants() throws IOException {
        lockChaos("s04", "C4Speed", "functions", FAMILIES_14);
    }

    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c2_C8EvenC8NonNeg_allTwelveVariants() throws IOException {
        lockChaos("s08", "C8EvenC8NonNeg", "datarule", FAMILIES_12);
    }

    @Test
    @EnabledIf("chaosAvailable")
    void corpus_c3_C8PricedTypeFormatValidator_allTwelveVariants() throws IOException {
        lockChaos("s08", "C8PricedTypeFormatValidator", "validation", FAMILIES_12);
    }

    // ---- controls (LAW 81): the lock can fail, the family pin can fail, the IR route agrees ----

    /** L3 positive control: a doctored golden IS reported by the lock (a byte moved in the wiring). */
    @Test
    @EnabledIf("chaosAvailable")
    void control3_doctoredGolden_isReportedByTheLock() throws IOException {
        assertNotNull(chaosOutput, "chaos cell generation did not run — corpus unavailable?");
        String carrier = "chaos/s08/base/validation/C8PricedTypeFormatValidator.java";
        String generated = chaosOutput.get(carrier);
        assertNotNull(generated, "not generated: " + carrier);
        String golden = normalize(Files.readString(CHAOS_GOLDEN.resolve(carrier)));
        assertTrue(compareCarrier(carrier, golden, generated) == null,
                "the undoctored carrier must lock green before the doctor is applied");
        String doctored = golden.replace(".withIndex(i)", ".withIndex(i0)");
        assertTrue(!doctored.equals(golden), "the doctor must change a byte");
        String verdict = compareCarrier(carrier, doctored, generated);
        assertNotNull(verdict, "a doctored golden MUST be reported by the lock");
        assertContains(verdict, ".withIndex(i0)");
    }

    /** The placement-family identity pin proven able to fail (over the 14-family s04 seat). */
    @Test
    void control4_placementFamilyPin_canFail() {
        assertEquals("a3half", placementFamily("a3half/p2"));
        assertEquals("x13half", placementFamily("x13half/p1"));
        assertEquals("a8pkg", placementFamily("a8pkg"));
        List<String> good = FAMILIES_14.stream().map(v -> "chaos/s99/" + v + "/p1/functions/X.java").toList();
        assertTrue(placementFamilyVerdict("s99", good, "functions", FAMILIES_14) == null, "the enumerated fourteen must pass");
        List<String> doctored = new ArrayList<>(good);
        doctored.set(doctored.indexOf("chaos/s99/a3hub/p1/functions/X.java"), "chaos/s99/a3half/p2/functions/X.java");
        String verdict = placementFamilyVerdict("s99", doctored, "functions", FAMILIES_14);
        assertNotNull(verdict, "a doctored population MUST be reported");
        String found = verdict.substring(verdict.indexOf("found "));
        assertTrue(!found.contains("a3hub"), "the missing family must be absent from the found list: " + found);
        assertTrue(found.indexOf("a3half") != found.lastIndexOf("a3half"), "the doubled family must appear twice: " + found);
    }

    /**
     * control2a (round-2 cq SF-2 — the population pins split from the identity clause): the DEFAULT
     * route renders both fixture sets without a refusal and at their declared populations — the
     * preconditions the identity control needs, red on their own whenever a mutation changes WHAT
     * the default route emits (lanes A / C / K / L: a refusal, or 13 / 27 / 26 against the F9 pin
     * of 25), so the identity row below stays about the ROUTES.
     */
    @Test
    @EnabledIf("builtinsAvailable")
    void control2a_defaultRoute_populationPins() throws IOException {
        Render def = renderF12();
        assertTrue(def.errors().isEmpty(), "default-route generation errors: " + def.errors());
        List<String> defFns = functionKeys(def);
        assertEquals(F12_FUNCTIONS, defFns.size(),
                "the F12 fixtures declare " + F12_FUNCTIONS + " function files; the default route rendered " + defFns);
        Render f9 = render();
        assertEquals(F9_FILES, f9.output().size(), "the F9 fixture declares " + F9_FILES
                + " emitted files (the control's population pin — round-1 spec SF-5: two empty sets"
                + " would agree vacuously); the default route rendered " + f9.output().keySet());
    }

    /**
     * control2b — LAW 77: the IR route renders every fixture byte-identically to the default
     * route — the F12 fixtures through the IR-route function generator (the grouping lives in the
     * shared base; the bodies go through the IR expression compiler and its leaf emitter), the F9
     * fixtures through the shared data-rule and type-format generators. Runs only with the IR
     * provider on the classpath ({@code -Pir-on} — the chain's ON-route seat step). Its positive
     * control is lane V, an IR-ONLY mutation (the leaf emitter's multiply rendered as divide): the
     * default route unmoved, this row red (round-2 cq SF-2).
     */
    @Test
    @EnabledIf("builtinsAndIrProviderAvailable")
    void control2b_irRoute_rendersEveryFixtureIdenticallyToTheDefaultRoute() throws IOException {
        Render def = renderF12();
        Render ir = renderF12Ir();
        assertEquals(def.errors().size(), ir.errors().size(),
                "the two routes must refuse alike: " + def.errors() + " vs " + ir.errors());
        List<String> defFns = functionKeys(def);
        List<String> irFns = functionKeys(ir);
        assertEquals(defFns, irFns, "the two routes must emit the SAME function set");
        for (String key : defFns) {
            assertEquals(normalize(def.output().get(key)), normalize(ir.output().get(key)),
                    "the IR route must agree with the default route for " + key);
        }
        Render f9 = render();
        Render f9Ir = renderIr();
        assertEquals(f9.output().keySet(), f9Ir.output().keySet(), "the F9 fixture's file set must agree across routes");
        for (String key : f9.output().keySet()) {
            assertEquals(normalize(f9.output().get(key)), normalize(f9Ir.output().get(key)),
                    "the IR route must agree with the default route for " + key);
        }
    }

    /** The F12 fixtures' function-file count — the control's population pin (a fixture added without bumping it fails). */
    private static final int F12_FUNCTIONS = 4;

    /**
     * The F9 fixtures' emitted-file count over the three kinds the harness renders (datarules,
     * type-format validators, functions; the two parameterised refusals emit nothing): the
     * control's population pin for the F9 half (round-1 spec SF-5).
     */
    private static final int F9_FILES = 25;

    private static List<String> functionKeys(Render r) {
        return r.output().keySet().stream()
                .filter(k -> k.contains("/functions/"))
                .sorted()
                .toList();
    }

    // =========================================================================
    // Fixture harness — multi-model, three kinds, both routes, the register read per render
    // =========================================================================

    /**
     * One render: the emitted files, the TYPED generation errors (round-1 cq SF-7: a refusal is
     * checked by its class and site, never by its message text) and the register's counts.
     */
    private record Render(Map<String, String> output, List<GenerationException> errors,
                          Map<SilentDegradation.Site, Integer> counts) {
        /** The refusals recorded at {@code site} — by type and site, the message never consulted. */
        List<SilentDegradation.Refusal> refusalsAt(SilentDegradation.Site site) {
            return errors.stream()
                    .filter(e -> e instanceof SilentDegradation.Refusal r && r.site() == site)
                    .map(e -> (SilentDegradation.Refusal) e)
                    .toList();
        }
    }

    private static Render rendered;
    private static Render renderedIr;
    private static Render renderedF12;
    private static Render renderedF12Ir;

    private static Render render() throws IOException {
        if (rendered == null) {
            rendered = renderModels(f9Files(), "census.seat3f9", false);
        }
        return rendered;
    }

    private static Render renderIr() throws IOException {
        if (renderedIr == null) {
            renderedIr = withIr(() -> renderModels(f9Files(), "census.seat3f9", true));
        }
        return renderedIr;
    }

    /** The F9 fixture: the main file plus the two twin namespaces (round 1, a13). */
    private static List<String[]> f9Files() {
        return List.<String[]>of(new String[] {"seat3f9.rosetta", F9_MODEL},
                new String[] {"seat3f9a.rosetta", F9_TWIN_A}, new String[] {"seat3f9b.rosetta", F9_TWIN_B});
    }

    private static Render renderF12() throws IOException {
        if (renderedF12 == null) {
            renderedF12 = renderModels(f12Files(), "census.seat3f12", false);
        }
        return renderedF12;
    }

    private static Render renderF12Ir() throws IOException {
        if (renderedF12Ir == null) {
            renderedF12Ir = withIr(() -> renderModels(f12Files(), "census.seat3f12", true));
        }
        return renderedF12Ir;
    }

    private static List<String[]> f12Files() {
        return List.<String[]>of(new String[] {"seat3f12a.rosetta", F12_A}, new String[] {"seat3f12b.rosetta", F12_B},
                new String[] {"seat3f12c.rosetta", F12_C}, new String[] {"seat3f12d.rosetta", F12_D});
    }

    private interface RenderCall {
        Render call() throws IOException;
    }

    private static Render withIr(RenderCall call) throws IOException {
        String previous = System.getProperty(IRGeneration.PROPERTY);
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            assertNotNull(IRGeneration.providerOrNull(),
                    "the IR provider must be resolvable under -Pir-on, else this is not an ON-route render");
            return call.call();
        } finally {
            if (previous == null) {
                System.clearProperty(IRGeneration.PROPERTY);
            } else {
                System.setProperty(IRGeneration.PROPERTY, previous);
            }
        }
    }

    private static RModel model(String source, String fileName) {
        RModel m = AstBuilder.buildFromString(source, fileName);
        m.setVersion("0.0.0.test");
        return m;
    }

    private static Render renderModels(List<String[]> files, String namespacePrefix, boolean irRoute) throws IOException {
        return renderModels(files, namespacePrefix, irRoute, Map.of());
    }

    /**
     * The same render over an output map ANOTHER generator has already written into ({@code seed}) —
     * the production shape: {@code JavaCodeGenerator} fills one map with every class generator's files
     * before the function generator runs (round-2 cq SF-3; control9).
     */
    private static Render renderModels(List<String[]> files, String namespacePrefix, boolean irRoute,
                                       Map<String, String> seed) throws IOException {
        List<RModel> models = new ArrayList<>();
        for (String[] f : files) {
            models.add(model(f[1], f[0]));
        }
        models.addAll(loadBuiltinsOnly());
        RWorkspace workspace = RWorkspace.build(models).workspace();
        GeneratorModel gm = new GeneratorModel(workspace, m -> m.namespace().startsWith(namespacePrefix));
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(typeUtil);
        FunctionGenerator fg = irRoute
                ? IRGeneration.functionGenerator(gm, tt, typeUtil)
                : new FunctionGenerator(gm, tt, typeUtil);
        if (irRoute) {
            assertTrue(!fg.getClass().equals(FunctionGenerator.class),
                    "the seam must hand back the IR-route FunctionGenerator, got " + fg.getClass());
        }
        DataRuleGenerator dataRules = new DataRuleGenerator(gm, tt, typeUtil);
        TypeFormatValidatorGenerator typeFormat = new TypeFormatValidatorGenerator(gm, tt, typeUtil);
        SilentDegradation.reset();
        Map<String, String> out = new LinkedHashMap<>(seed);
        List<GenerationException> errors = new ArrayList<>();
        if (irRoute) {
            // round-1 cq MF-1: the F9 generators have NO route subclass today, so the production
            // seam (IRGeneration.generateClasses -> the provider's generateClassesAsIR) hands them
            // back UNCLAIMED — asserted ONCE, on the first generated model, so the day the provider
            // claims one, control2b's F9 half stops being a same-object comparison and must be
            // re-read as a real cross-route compare. Round-2 cq SF-1: the probe used to sit inside
            // the per-model loop with a register reset after it, which wiped every earlier model's
            // counts on the IR route — the reset is gone (the probe is a bare instanceof test that
            // writes nothing) and Render.counts() now reads every model's refusals on both routes.
            RModel first = workspace.files().stream().filter(gm::shouldGenerate).findFirst().orElseThrow();
            assertTrue(IRGeneration.providerOrNull().generateClassesAsIR(dataRules, first, gm.version(first),
                    new LinkedHashMap<>()) == null,
                    "the IR provider now CLAIMS DataRuleGenerator — control2b's F9 half is a real"
                    + " cross-route compare from here on; re-read it");
            assertTrue(IRGeneration.providerOrNull().generateClassesAsIR(typeFormat, first, gm.version(first),
                    new LinkedHashMap<>()) == null,
                    "the IR provider now CLAIMS TypeFormatValidatorGenerator — control2b's F9 half is"
                    + " a real cross-route compare from here on; re-read it");
        }
        for (RModel model : workspace.files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                // the production dispatch seam on BOTH routes (round-1 cq MF-1) — never the
                // generator's own generateClasses, which the IR route would bypass
                errors.addAll(IRGeneration.generateClasses(dataRules, model, version, out));
                errors.addAll(IRGeneration.generateClasses(typeFormat, model, version, out));
            }
        }
        errors.addAll(fg.generateWithErrors(out));
        Map<SilentDegradation.Site, Integer> counts = SilentDegradation.counts();
        SilentDegradation.reset();
        return new Render(out, errors, counts);
    }

    /**
     * A data rule of the F9 fixture's MAIN namespace, by full key — the twin namespaces (a13)
     * declare same-named classes, so a bare suffix would be ambiguous.
     */
    private static String datarule(String fileName) throws IOException {
        return pick(render(), "census/seat3f9/validation/datarule/" + fileName);
    }

    private static String validator(String fileName) throws IOException {
        return pick(render(), "census/seat3f9/validation/" + fileName);
    }

    private static String pick(Render r, String suffix) {
        String fileName = suffix.substring(suffix.lastIndexOf('/') + 1);
        // an error attributed to this file by its TARGET PATH, or an unattributed one whose
        // text names the file (a null-path refusal — round-1 cq N-7)
        List<GenerationException> own = r.errors().stream()
                .filter(e -> (e.getTargetPath() != null && e.getTargetPath().endsWith("/" + suffix))
                        || (e.getTargetPath() == null && String.valueOf(e.getMessage()).contains(fileName)))
                .toList();
        assertTrue(own.isEmpty(),
                "the generator reported errors for " + fileName + " (a broken fixture must fail"
                + " loudly, not skip): " + own);
        List<String> matches = r.output().keySet().stream()
                .filter(k -> k.endsWith("/" + suffix) || k.equals(suffix))
                .sorted()
                .toList();
        assertTrue(!matches.isEmpty(), "not generated: " + suffix + " (have: " + r.output().keySet() + ")");
        assertEquals(1, matches.size(), "exactly ONE emitted file may match " + suffix
                + " (round-1 cq N-7: an ambiguous suffix is a harness error, never a first-match pick): " + matches);
        return normalize(r.output().get(matches.get(0)));
    }

    private static int count(String code, String needle) {
        int n = 0;
        for (int at = code.indexOf(needle); at >= 0; at = code.indexOf(needle, at + needle.length())) {
            n++;
        }
        return n;
    }

    private static void assertContains(String code, String needle) {
        assertTrue(code.contains(needle), "expected <" + needle + "> in:\n" + code);
    }

    private static void assertAbsent(String code, String needle) {
        assertTrue(!code.contains(needle), "did NOT expect <" + needle + "> in:\n" + code);
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n");
    }

    /**
     * {@code null} when the generated text byte-matches the golden (newline-normalised); else the
     * named difference — the ONE comparison every whole-class lock and the L3 control go through.
     */
    private static String compareCarrier(String carrier, String golden, String generated) {
        String gen = normalize(generated);
        return golden.equals(gen) ? null
                : carrier + ": differs from its golden — " + firstDifference(golden, gen);
    }

    private static String firstDifference(String golden, String generated) {
        String[] g = golden.split("\n");
        String[] r = generated.split("\n");
        int n = Math.min(g.length, r.length);
        for (int i = 0; i < n; i++) {
            if (!g[i].equals(r[i])) {
                return "line " + (i + 1) + " golden <" + g[i].strip() + "> vs generated <" + r[i].strip() + ">";
            }
        }
        return "lengths differ: golden " + g.length + " lines vs generated " + r.length + " lines";
    }

    private static List<RModel> loadBuiltinsOnly() throws IOException {
        Map<String, Path> resolved = new LinkedHashMap<>();
        for (Path root : BUILTINS_SEARCH_ROOTS) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (var stream = Files.walk(root)) {
                stream.filter(p -> p.toString().endsWith(".rosetta"))
                      .forEach(p -> resolved.putIfAbsent(p.getFileName().toString(), p));
            }
        }
        List<String> failures = new ArrayList<>();
        List<RModel> models = new ArrayList<>();
        resolved.values().stream()
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .forEach(p -> {
                    try {
                        models.add(AstBuilder.buildFromFile(p));
                    } catch (Exception e) {
                        failures.add(p + " - " + e);
                    }
                });
        if (!failures.isEmpty()) {
            throw new AssertionError("[SilentPairSeatTest] builtins parse failures: "
                    + String.join("; ", failures));
        }
        return models;
    }

    // =========================================================================
    // The chaos cell — generated once (three kinds), every declared carrier locked whole
    // =========================================================================

    private static Map<String, String> chaosOutput;
    private static List<GenerationException> chaosGenErrors;

    @BeforeAll
    static void generateChaosCell() throws IOException {
        if (chaosAvailable()) {
            List<GenerationException> errs = new ArrayList<>();
            chaosOutput = generateCell(
                    new D11CorpusRegressionTest.CellSpec("chaos", "1.0.0", CHAOS_ROOT), errs);
            chaosGenErrors = errs;
        }
    }

    private static Map<String, String> generateCell(D11CorpusRegressionTest.CellSpec cell,
            List<GenerationException> errors) throws IOException {
        // loadCellCorpusCached is the D11 loader's package-private STATIC-cache read (its javadoc:
        // "FunctionTransitiveIso20022ClosureTest locks the closure semantics against THIS loader");
        // it reads no per-instance state and no JUnit lifecycle sets anything it needs, so a bare
        // instance is the documented way to reach it (round-1 cq N-7)
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        var gm = new GeneratorModel(corpus.workspace(),
                D11CorpusRegressionTest.emissionFilter(cell));
        var typeUtil = new JavaTypeUtil();
        var typeTranslator = new JavaTypeTranslator(typeUtil);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var dataRules = new DataRuleGenerator(gm, typeTranslator, typeUtil);
        var typeFormat = new TypeFormatValidatorGenerator(gm, typeTranslator, typeUtil);
        Map<String, String> output = new LinkedHashMap<>();
        for (RModel model : corpus.workspace().files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                // the production dispatch seam (round-1 cq MF-1), as renderModels
                errors.addAll(IRGeneration.generateClasses(ruleGen, model, version, output));
                errors.addAll(IRGeneration.generateClasses(reportGen, model, version, output));
                errors.addAll(IRGeneration.generateClasses(dataRules, model, version, output));
                errors.addAll(IRGeneration.generateClasses(typeFormat, model, version, output));
            }
        }
        errors.addAll(funcGen.generateWithErrors(output));
        return output;
    }

    /** {@code a3half/p2} -> {@code a3half}; {@code base} -> {@code base}. */
    private static String placementFamily(String variant) {
        int slash = variant.indexOf('/');
        return slash < 0 ? variant : variant.substring(0, slash);
    }

    /**
     * {@code null} when {@code carriers} (golden-relative paths under {@code chaos/<seat>/}) hold every
     * placement FAMILY of {@code expectedFamilies} exactly once; else the named difference.
     */
    private static String placementFamilyVerdict(String seat, List<String> carriers, String parentDir,
            List<String> expectedFamilies) {
        String prefix = "chaos/" + seat + "/";
        String marker = "/" + parentDir + "/";
        List<String> families = carriers.stream()
                .map(c -> c.substring(prefix.length(), c.lastIndexOf(marker)))
                .map(v -> v.endsWith("/validation") ? v.substring(0, v.length() - "/validation".length()) : v)
                .map(SilentPairSeatTest::placementFamily)
                .sorted()
                .toList();
        List<String> expected = expectedFamilies.stream().sorted().toList();
        return expected.equals(families) ? null
                : "expected one carrier per placement family " + expected + ", found " + families;
    }

    /**
     * Byte-lock every placement variant of one declared class against the chaos goldens. A carrier
     * the generator refused, did not emit, or emitted differently fails by NAME.
     */
    private static void lockChaos(String seat, String simpleName, String parentDir, List<String> families)
            throws IOException {
        assertNotNull(chaosOutput, "chaos cell generation did not run — corpus unavailable?");
        List<String> carriers = carriersOf(seat, simpleName, parentDir);
        assertEquals(families.size(), carriers.size(),
                "expected the declared " + families.size() + " placement variants of " + simpleName
                + " under chaos/" + seat + ", found " + carriers);
        String familyVerdict = placementFamilyVerdict(seat, carriers, parentDir, families);
        assertTrue(familyVerdict == null,
                "the carriers of " + simpleName + " must be one per placement family: " + familyVerdict);
        List<String> failures = new ArrayList<>();
        for (String carrier : carriers) {
            String verdict = lockOne(carrier);
            if (verdict != null) {
                failures.add(verdict);
            }
        }
        assertTrue(failures.isEmpty(), "carriers of " + simpleName + " not byte-identical to the chaos"
                + " goldens (" + failures.size() + "/" + families.size() + "):\n  "
                + String.join("\n  ", failures));
    }

    private static List<String> carriersOf(String seat, String simpleName, String parentDir) throws IOException {
        Path seatRoot = CHAOS_GOLDEN.resolve("chaos").resolve(seat);
        try (var stream = Files.walk(seatRoot)) {
            return stream
                    .filter(p -> p.getFileName().toString().equals(simpleName + ".java"))
                    .filter(p -> p.getParent().getFileName().toString().equals(parentDir))
                    .map(p -> CHAOS_GOLDEN.relativize(p).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        }
    }

    /** {@code null} when one carrier byte-matches its golden; else the named failure. */
    private static String lockOne(String carrier) throws IOException {
        // attributed by TARGET PATH; an unattributed (null-path) error whose text names the carrier
        // counts too (round-1 cq N-7)
        List<GenerationException> own = chaosGenErrors.stream()
                .filter(e -> carrier.equals(e.getTargetPath())
                        || (e.getTargetPath() == null && String.valueOf(e.getMessage()).contains(carrier)))
                .toList();
        if (!own.isEmpty()) {
            return carrier + ": generator errors " + own;
        }
        String generated = chaosOutput.get(carrier);
        if (generated == null) {
            return carrier + ": not generated";
        }
        Path goldenPath = CHAOS_GOLDEN.resolve(carrier);
        if (!Files.isRegularFile(goldenPath)) {
            return carrier + ": golden missing at " + goldenPath;
        }
        return compareCarrier(carrier, normalize(Files.readString(goldenPath)), generated);
    }
}
