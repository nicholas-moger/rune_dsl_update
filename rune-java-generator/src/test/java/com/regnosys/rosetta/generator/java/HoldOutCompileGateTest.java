package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * W42 sweep leg B — the CompileGate bar (PR #412; banked at slice 1, deliberately
 * deferred WITH the builder-compat facet lead at slice 2 — see
 * the development audit "2026-07-16-holdout-slice2-results" §5).
 *
 * <p>Real {@code javac} over the fork's FULL-battery hold-out output: every generated
 * file of every oracle-comparable group must compile together (per group) against the
 * module's own test classpath (rune-runtime + guava — the same runtime surface the
 * generated code targets). While the byte bar ({@link HoldOutByteCompareTest}) proves
 * byte-identity for every non-pinned golden (229 goldens post-#417: ALL 229 identical
 * — the #416 default-op pinned lead healed at #417; a future pinned lead is
 * byte-DIVERGENT by definition until its heal), compilability of the identical files
 * is inherited from the oracle by
 * identity — this bar's marginal value is END-TO-END verification of exactly the
 * failure mode the PR #410 pinned lead exhibited (the fork emitted a non-compiling
 * {@code instanceof BigInteger} on a final class): any future facet regression that
 * byte-diverges INTO non-compiling territory now fails two independent ways.
 *
 * <p>The two oracle-rejected formatter-INPUT groups sit out: their references are
 * deliberately unresolvable, so their (single-file) outputs reference types that do
 * not exist — refusal is upstream's MOJO-layer concern (the documented design
 * difference; slice 1's crash-freedom bars cover them).
 *
 * <p>Skips when the builtins are absent (CI / fresh clone), the hold-out convention.
 */
class HoldOutCompileGateTest {

    /**
     * Files whose NON-compilation is upstream's own, byte-pinned behaviour (the
     * bug-compat law) — Freezing-pattern, shrink-only. PR #415: upstream 9.83.0
     * emits, for a data-rule whose subject type is named {@code Result}, a Default
     * class that escapes the {@code ComparisonResult} local to {@code _result} but
     * leaves the {@code getOrDefault} guard reading the LITERAL {@code result} —
     * which binds the model instance (no such method): NON-COMPILING JAVA, straight
     * from the released plugin (oracle-pinned golden
     * {@code reserved-names/.../ResultValueExists.java}; the FINOS-issue rider sits
     * with the draft in the waiver-file header). The fork reproduces the bytes, so
     * this gate documents the file instead of failing on upstream's bug.
     *
     * <p>PR #624 (v3.2 seat 3, round 1 — oracle group {@code alias-conditions-scope}): a
     * {@code Data} type with an attribute named {@code o}. Upstream's POJO renders
     * {@code equals(Object o)} in the implementation AND the builder class, and inside
     * each reads the FIELD {@code o} as the bare identifier — which the parameter
     * shadows: {@code ListEquals.listEquals(o, _that.getO())} → "Object cannot be
     * converted to List<?>" twice ({@code Clash.java:162} / {@code :393}). NON-COMPILING
     * JAVA straight from the released 9.83.0 plugin (the golden is byte-identical, the
     * group 21/21); the fork reproduces the bytes. A FINOS-issue rider beside the
     * {@code ResultValueExists} one. The other three types of the group compile.
     *
     * <p>PR #632 (v3.2 seat 11, M1 / D50 — the five {@code type-named-*} oracle groups, a data type named after
     * every library token the six data-type templates write): FOUR of the five groups do not compile and every
     * error on the TWELVE pinned goldens is upstream's own — three upstream literal classes at four groups, itemised at
     * the entries below, plus the {@code RosettaModelObject} shadow's four folded cascades (two {@code annotation
     * interface not applicable} on the annotation-named types, two {@code Validator<T>} incompatibilities in its
     * validators — all four vanish with the one cyclic line, {@code x1.errors} 23 → 9); the fork's own {@code UseList}
     * (16 of the list group's 32 errors) is the byte bar's declared pin, not upstream's;
     * the goldens are byte-identical to the fork's output (the byte bar's one mismatch, {@code UseList}, is the
     * fork's and sits in the pinned-lead set). FINOS-ISSUE RIDERS #6 / #7 / #8 in the D11 waiver header.
     */
    private static final Map<String, Set<String>> EXPECTED_NON_COMPILING_UPSTREAM_BUG = Map.of(
            "reserved-names", Set.of("test/reservednames/validation/datarule/ResultValueExists.java"),
            "alias-conditions-scope", Set.of("test/aliasscope/Clash.java"),
            // PR #630 (v3.2 seat 9, F1 / D46 — oracle group enum-unicode-display-edge): the released plugin splices an
            // enum value's displayName into the @RosettaEnumValue annotation RAW, so `Quote displayName "say \"hi\""`
            // renders `displayName = "say "hi""` — NON-COMPILING JAVA straight from the released 9.83.0 plugin (javac
            // stops at that value's parse error, `')' expected` at line 17; the Backslash value's illegal escape and
            // the Tab value's raw tab — which compiles — sit behind it). The fork reproduces the bytes since the D46
            // fix (one escape law, at the constructor argument alone; the golden is byte-identical, the group 7/7).
            // FINOS-ISSUE RIDER #4 in the D11 waiver header. The group's SynonymEnum and EdgeCarrier compile.
            "enum-unicode-display-edge", Set.of("test/enumuniedge/EscapeEnum.java"),
            // PR #630 (v3.2 seat 9, F8 / D47 — oracle group void-mapping-render-builder, pinned for the `nothing` render
            // law's builder / setter arms): a function whose model-typed OUTPUT is named `o` — the released plugin's
            // prune lambda in assignOutput is the fixed `.map(o -> o.prune())`, which javac rejects as "variable o is
            // already defined in method assignOutput(...)" (IntoDeep.java:48): NON-COMPILING JAVA straight from the
            // released 9.83.0 plugin, the #624 `Clash.java` class (an attribute named `o`) met at the function-output
            // seat. The fork reproduces the bytes (the group 12/12); the fixture's `o` was not chosen for it — the seat's
            // catch. FINOS-ISSUE RIDER #5 in the D11 waiver header. The group's IntoHolder (output `h`) compiles.
            "void-mapping-render-builder", Set.of("test/voidbuilder/functions/IntoDeep.java"),
            // v3.2 seat 11 (PR #632, M1 / D50): MEASURED file by file with javac over the goldens at the c4 head
            // (target/v32-seat11-instruments/scratch/javac/j1.summary + j1-type-named-*.errors, local — the gate prints
            // 20 errors per group, the instrument every one: list 32 / util 4 / guava 0 / rosetta 23 / annotations 14).
            // THREE upstream classes. (i) the same-package NESTED BUILDER written bare when its outer lost the simple
            // name — upstream's isAlreadyAccessible shortcut: `List.ListBuilder` under `import java.util.List`
            // (HolderJavaFirst, 16 = `cannot find symbol class ListBuilder in interface java.util.List` ×15 + one
            // cascade); `RosettaDataType.RosettaDataTypeBuilder` / `RuneDataType.RuneDataTypeBuilder` where the
            // ANNOTATION in the class header claimed the outer's name (AnnotationRefs, 14); `RosettaModelObject.
            // RosettaModelObjectBuilder` where the library super-interface claimed it (RosettaRefs, 7). (ii) a library
            // LITERAL outside the first-claim law: the list setter's `toCollection(()->new ArrayList<>())` beside a
            // model type named ArrayList (`cannot use '<>' with non-generic class` — ArrayList.java:221 and
            // UtilRefs.java:763, each writing java.util.ArrayList canonical at every OTHER site) and the only-exists
            // validator's `Map.Entry::getValue` / `::getKey` beside a model type named Map (MapOnlyExistsValidator,
            // `cannot find symbol variable Entry`). (iii) the nested builder's OWN name shadowing the imported library
            // super-interface inside the class: a type named RosettaModelObject renders `interface
            // RosettaModelObjectBuilder extends RosettaModelObject, RosettaModelObjectBuilder` under `import
            // com.rosetta.model.lib.RosettaModelObjectBuilder` — `cyclic inheritance` (RosettaModelObject.java) — and
            // FIVE sibling files CASCADE from it (its three validators' `Validator<RosettaModelObject>`, the two
            // RosettaRefs validators' `(RosettaModelObject)` cast: `cannot find symbol class RosettaModelObject`),
            // measured to vanish with that ONE line fixed in a scratch copy (scratch/javac/x1.errors: 23 → 9, the nine
            // all RosettaModelObject / RosettaRefs). The fork reproduces every byte (D50: the first-claim law at six
            // kinds, the three literals kept literal). Guava's four types compile whole.
            "type-named-list", Set.of("holdout/typenamedlist/HolderJavaFirst.java"),
            "type-named-util", Set.of("holdout/typenamedutil/ArrayList.java", "holdout/typenamedutil/UtilRefs.java",
                    "holdout/typenamedutil/validation/exists/MapOnlyExistsValidator.java"),
            "type-named-annotations", Set.of("holdout/typenamedannotations/AnnotationRefs.java"),
            "type-named-rosetta", Set.of("holdout/typenamedrosetta/RosettaModelObject.java",
                    "holdout/typenamedrosetta/RosettaRefs.java",
                    "holdout/typenamedrosetta/validation/RosettaModelObjectValidator.java",
                    "holdout/typenamedrosetta/validation/RosettaModelObjectTypeFormatValidator.java",
                    "holdout/typenamedrosetta/validation/exists/RosettaModelObjectOnlyExistsValidator.java",
                    "holdout/typenamedrosetta/validation/RosettaRefsValidator.java",
                    "holdout/typenamedrosetta/validation/exists/RosettaRefsOnlyExistsValidator.java"));

    /**
     * Files whose non-compilation is a PINNED FORK facet lead (the byte bar's
     * {@code PINNED_FACET_LEADS} twin — same Freezing shrink-only contract, OPPOSITE
     * semantics from the upstream-bug set above: here the ORACLE golden compiles and
     * the FORK's divergent output does not; the heal un-pins BOTH sets in the same
     * change). EMPTY since PR #419: both #418 leads (the sort-min-max shadow lambda
     * + the conversions raw-rune-name signatures) healed first-run from their banked
     * maps — see the byte bar's {@code PINNED_FACET_LEADS} javadoc for the heal
     * narrative; the set stays as the mechanism for future fork leads. RE-OPENED #625 (v3.2 seat 4,
     * commit 2 — three non-compiling report functions of the order-dependence groups); EMPTIED at #625
     * (commit 3, the fix — all three compile); RE-OPENED #626 (v3.2 seat 5, commit 2 — the six F6 + F3 oracle
     * groups' 24 non-compiling files: `bad type in conditional expression` / `BigInteger cannot be converted to
     * BigDecimal` / `FieldWithMetaString cannot be converted to String` / the with-meta stub's `toBuilder()` on a
     * MapperS — the byte bar's twin set, measured at the pre-fix head); RE-OPENED #627 (v3.2 seat 6, commit 2 — the two
     * deep-path oracle groups' 16 non-compiling files: `cannot find symbol` at the placeholder's unqualified choose call
     * on every one, `cannot infer type-variable(s) F` where the `*ToList` map method's lambda returned a MapperS, `int
     * cannot be dereferenced` on InCondition's count SET — measured at the pre-fix head); EMPTIED of them at #627
     * (commit 4 — all 16 compile at the fix, commit 3, on its first measured run; InLambda the one declared pin that
     * stays); RE-OPENED #628 (v3.2 seat 7, commit 2 — the byte bar's twin set: the two control forms the fork emitted
     * without refusing, both non-compiling at the pre-fix head ({@code scratch/battery-pre2.status}): the enum-switch
     * ternary's bare {@code Red} (`cannot find symbol: variable Red`), the literal-switch alias signature's raw
     * {@code string} (`cannot find symbol: class string`)); EMPTIED of them at #628 (commit 4 — both compile at the fix,
     * commit 3; the compile half witnessed by the pins-emptied runs {@code scratch/battery-v6.log} and the chain's
     * G-Z half, {@code HOLDOUT-COMPILE choice-switch-in-lambda: 45 files compiled clean}); RE-OPENED #630 round 2
     * (commit 14 — the byte bar's twin set, two entries: {@code SetMetaVoid} (`no suitable method found for
     * toBuilder(java.lang.Void)` — a Void value SET into a scheme-annotated Void output) and {@code ExistsThenConditional}
     * (`areEqual … cannot be applied to given types` — a literal-armed conditional the fork hands to areEqual as a String),
     * both the fork's own non-compiling renders on legal shapes the round-2 oracle accepted, un-pinned with their byte
     * pins at the F8 lineage's last-leg seat — these two STAY, DECLARED; the reasons are locked in
     * {@code EXPECTED_PINNED_LEAD_DIAGNOSTIC}). Round 3 added no compile-gate pin. RE-OPENED #632 (v3.2 seat 11,
     * M1 / D50 — one entry: {@code UseList}, the FUNCTION kind's library-token literals beside a model type named
     * {@code List} — `reference to List is ambiguous`; DECLARED with its byte pin, the heal D50's banked
     * function-generator seat).
     */
    private static final Map<String, Set<String>> EXPECTED_NON_COMPILING_PINNED_LEAD = Map.of(
            // v3.2 seat 5 (F6 + F3), commit 2: every mismatching file of the six groups is a NON-COMPILING emission the
            // fork produced in silence (the chaos cell's 37 declared rows carry the same three shapes). Un-pinned at
            // the fix commits (arm (a) commit 3, arm (c) commit 5, arm (d) commit 7 - every conv-bigint file compiles).
            // v3.2 seat 5, arm (c): six of the seven meta-ladder files compile at the fix; InLambda stays pinned - the
            // byte bar's twin note names its two F4-family mechanisms (`List<FieldWithMetaString> cannot be converted
            // to List<String>` at the multi SET, the bare `c` in the filter predicate). BANKED with the golden.
            // v3.2 seat 13 (PR #634, D53, commit 4): InLambda LEFT this map too - the R12 heal's element deref after
            // the block lambda closed both F4-family gaps and the file compiles (the c4g battery's own catch).
            // v3.2 seat 13 (PR #634, D53, commit 4): the seat-9 round-2 twins SetMetaVoid and ExistsThenConditional
            // LEFT this map - SetMetaVoid is a declared REFUSAL now (HoldOutByteCompareTest.DECLARED_REFUSALS, R13:
            // nothing is emitted to compile), ExistsThenConditional is HEALED and compiles.
            // v3.2 seat 11 (PR #632, M1 / D50): the byte bar's twin — the FUNCTION kind's library-token literals.
            // UseList's signature writes `List<String>` as a literal beside a model input typed by a data type named
            // List and imports BOTH (`import holdout.typenamedlist.List;` + `import java.util.List;`): javac
            // `reference to List is ambiguous` ×15 + `a type with the same simple name is already defined by the
            // single-type-import` (the fork's c4 emission, target/v32-seat11-instruments/scratch/holdout-c4-dump/
            // type-named-list/, local). The released plugin claims java.util.List at the RETURN type — the first
            // write in text order — and writes the model List canonical (`holdout.typenamedlist.List l`), importing
            // it nowhere. DECLARED; the heal is D50's banked FUNCTION kind (the signature and body tokens as
            // sentinels at the function generator), a seat of its own.
            "type-named-list", Set.of("holdout/typenamedlist/functions/UseList.java"));

    /**
     * Round 1 (the spec review's SF-4): a pinned lead must fail to compile FOR ITS DECLARED REASON — every substring
     * listed here must appear in some javac error naming the file, so a pin whose defect silently changes shape (a
     * different error at the same file) is reported rather than carried. Measured at the round-1 head; the InLambda
     * twin note in the byte bar names the mechanism. (v3.2 seat 6's sixteen deep-path pins — `cannot find symbol` at the
     * placeholder's unqualified choose call, measured at the pre-fix head — went with their pins at #627 commit 4.)
     * The map is consulted for EVERY pin, the upstream-bug set's included: PR #630 (v3.2 seat 9, F1 / D46) pins
     * EscapeEnum's reason — javac's `')' expected` at the raw-quoted displayName, measured at the fix content
     * ({@code target/v32-seat9-instruments/scratch/t-holdout-f1b.log}, local) — and IntoDeep's, the prune lambda's
     * `o` shadowing the output parameter `o` ({@code scratch/t-holdout-r1.log}, the render-law content). Round 3 (the
     * code-quality review's SF-3 / rule6 NIT-4): the two round-2 fork-lead pins' reasons, measured at the round-2
     * content — SetMetaVoid's `no suitable method found for toBuilder(java.lang.Void)`
     * ({@code scratch/t-holdout-r2a.log}) and ExistsThenConditional's `areEqual … cannot be applied to given types`
     * where the fork hands a String to areEqual ({@code scratch/t-targeted-r2a.log}) — so every pin the byte bar's
     * twin set carries now sits inside this guard. PR #632 (v3.2 seat 11, M1 / D50): every pin of the four
     * {@code type-named-*} groups carries its measured reason (javac over the goldens at the c4 head,
     * {@code target/v32-seat11-instruments/scratch/javac/j1-type-named-*.errors}, local) — the five rosetta CASCADE
     * files by the symbol they cannot find, the cyclic type itself; {@code Map.ofEntries} since the thirteen took
     * the map past {@code Map.of}'s ten pairs.
     */
    private static final Map<String, List<String>> EXPECTED_PINNED_LEAD_DIAGNOSTIC = Map.ofEntries(
            Map.entry("test/enumuniedge/EscapeEnum.java", List.of("')' expected")),
            Map.entry("test/voidbuilder/functions/IntoDeep.java", List.of("variable o is already defined")),
            // v3.2 seat 11 (PR #632, M1 / D50) — the fork's one lead, then the upstream-bug pins by class
            Map.entry("holdout/typenamedlist/functions/UseList.java", List.of("reference to List is ambiguous")),
            Map.entry("holdout/typenamedlist/HolderJavaFirst.java", List.of("cannot find symbol", "class ListBuilder")),
            Map.entry("holdout/typenamedutil/ArrayList.java", List.of("cannot use '<>' with non-generic class")),
            Map.entry("holdout/typenamedutil/UtilRefs.java", List.of("cannot use '<>' with non-generic class")),
            Map.entry("holdout/typenamedutil/validation/exists/MapOnlyExistsValidator.java",
                    List.of("cannot find symbol", "variable Entry")),
            Map.entry("holdout/typenamedannotations/AnnotationRefs.java",
                    List.of("class RosettaDataTypeBuilder", "class RuneDataTypeBuilder")),
            Map.entry("holdout/typenamedrosetta/RosettaModelObject.java", List.of("cyclic inheritance involving")),
            Map.entry("holdout/typenamedrosetta/RosettaRefs.java", List.of("class RosettaModelObjectBuilder")),
            // the five CASCADE files: the symbol they cannot find is the cyclic type itself
            Map.entry("holdout/typenamedrosetta/validation/RosettaModelObjectValidator.java",
                    List.of("cannot find symbol", "class RosettaModelObject")),
            Map.entry("holdout/typenamedrosetta/validation/RosettaModelObjectTypeFormatValidator.java",
                    List.of("cannot find symbol", "class RosettaModelObject")),
            Map.entry("holdout/typenamedrosetta/validation/exists/RosettaModelObjectOnlyExistsValidator.java",
                    List.of("cannot find symbol", "class RosettaModelObject")),
            Map.entry("holdout/typenamedrosetta/validation/RosettaRefsValidator.java",
                    List.of("cannot find symbol", "class RosettaModelObject")),
            Map.entry("holdout/typenamedrosetta/validation/exists/RosettaRefsOnlyExistsValidator.java",
                    List.of("cannot find symbol", "class RosettaModelObject")));

    static Stream<Path> compileGateGroups() throws IOException {
        try (var stream = Files.list(HoldOutByteCompareTest.HOLDOUT_ROOT)) {
            return stream.filter(Files::isDirectory)
                    .filter(p -> !HoldOutByteCompareTest.ORACLE_REJECTED
                            .contains(p.getFileName().toString()))
                    .sorted(java.util.Comparator.comparing(p -> p.getFileName().toString()))
                    .toList().stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("compileGateGroups")
    void holdout_group_output_compiles(Path groupDir) throws IOException {
        List<Path> builtinFiles = HoldOutByteCompareTest.resolveBuiltinFiles();
        Assumptions.assumeTrue(!builtinFiles.isEmpty(),
                "rune-dsl builtins absent — compile gate skipped");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Assumptions.assumeTrue(compiler != null,
                "system java compiler unavailable (JRE-only environment) — compile gate skipped");
        Assumptions.assumeTrue(released983RuntimeJar() != null,
                "released rune-runtime:9.83.0 jar absent from the local repository — compile"
                        + " gate skipped (pin it via scripts/holdout-oracle, the oracle law)");

        String group = groupDir.getFileName().toString();
        Map<String, String> generated =
                HoldOutByteCompareTest.generateAllKinds(groupDir, builtinFiles);
        if (generated.isEmpty()) {
            return; // the alias-only groups emit nothing — nothing to compile
        }

        // The pins — BOTH sets unioned (the upstream-bug set + the fork pinned-lead
        // set; shrink-only each): the whole group compiles as ONE unit set (sibling
        // files legitimately import a pinned class — javac resolves the SYMBOL from
        // the erroring source, so only the pinned file itself errors). Every ERROR
        // diagnostic must belong to a pinned file, and every pinned file must still
        // error — a heal/rebaseline that makes one compile must remove its pin (from
        // whichever set holds it) in the same change.
        Set<String> pinned = new java.util.HashSet<>(
                EXPECTED_NON_COMPILING_UPSTREAM_BUG.getOrDefault(group, Set.of()));
        pinned.addAll(EXPECTED_NON_COMPILING_PINNED_LEAD.getOrDefault(group, Set.of()));
        for (String pin : pinned) {
            Assertions.assertTrue(generated.containsKey(pin),
                    group + ": pinned file (upstream-bug or pinned-lead) missing from the generated output: "
                            + pin + " — the pin is stale, re-triage");
        }

        List<String> errors = compileInMemory(group, generated);
        // v3.2 seat 7: a DECLARED fork refusal (HoldOutByteCompareTest.DECLARED_REFUSALS) leaves its class OUT of the
        // output, so a sibling that references it cannot resolve the symbol (the edge group's BagMeta registers the
        // refused datarule BagTexts) - the refusal's CONSEQUENCE, admitted exactly as far as the declaration reaches: a
        // `cannot find symbol` diagnostic naming the refused class. It leaves with the declaration at the fix commit.
        Set<String> refusedClasses = new java.util.HashSet<>();
        for (HoldOutByteCompareTest.DeclaredRefusal d : HoldOutByteCompareTest.declaredRefusals(group)) {
            String p = d.targetPath();
            refusedClasses.add(p.substring(p.lastIndexOf('/') + 1, p.length() - ".java".length()));
        }
        List<String> unpinnedErrors = errors.stream()
                .filter(e -> pinned.stream().noneMatch(e::contains))
                .filter(e -> !(e.contains("cannot find symbol")
                        && refusedClasses.stream().anyMatch(c -> e.contains("class " + c))))
                .toList();
        if (!unpinnedErrors.isEmpty()) {
            StringBuilder sb = new StringBuilder(group + ": generated output FAILED to"
                    + " compile — " + unpinnedErrors.size() + " unpinned error(s):\n");
            unpinnedErrors.stream().limit(20).forEach(e -> sb.append("  ").append(e).append('\n'));
            Assertions.fail(sb.toString());
        }
        for (String pin : pinned) {
            Assertions.assertTrue(errors.stream().anyMatch(e -> e.contains(pin)),
                    group + ": a pinned file COMPILED — " + pin
                            + " — un-pin it (EXPECTED_NON_COMPILING_UPSTREAM_BUG or EXPECTED_NON_COMPILING_PINNED_LEAD) in the same change");
            for (String needle : EXPECTED_PINNED_LEAD_DIAGNOSTIC.getOrDefault(pin, List.of())) {
                Assertions.assertTrue(errors.stream().anyMatch(e -> e.contains(pin) && e.contains(needle)),
                        group + ": the pinned file " + pin + " fails to compile, but not for its declared reason — no error"
                                + " naming it contains `" + needle + "`; the errors: " + errors);
            }
        }
        System.out.println("HOLDOUT-COMPILE " + group + ": " + (generated.size() - pinned.size())
                + " files compiled clean"
                + (pinned.isEmpty() ? "" : " + " + pinned.size()
                        + " pin(s) enforced non-compiling (upstream-bug or pinned-lead)"));
    }

    /**
     * Compile the generated sources in memory against {@link #gateClasspath()} and
     * return the ERROR diagnostics (empty = clean). Package-private:
     * {@code WiderPoolCompileGateTest} classifies the parser-snippet pool with the
     * same compiler harness.
     */
    static List<String> compileInMemory(String label, Map<String, String> generated)
            throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<JavaFileObject> units = new ArrayList<>();
        for (Map.Entry<String, String> e : generated.entrySet()) {
            units.add(new InMemorySource(e.getKey(), e.getValue()));
        }

        Path classOut = Path.of("target", "holdout-compile-gate", label);
        deleteRecursively(classOut);
        Files.createDirectories(classOut);

        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            fm.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(classOut));
            List<String> options = List.of(
                    "-classpath", gateClasspath(),
                    "-proc:none");
            Boolean ok = compiler.getTask(null, fm, diagnostics, options, null, units).call();

            List<String> errors = new ArrayList<>();
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    errors.add((d.getSource() != null ? d.getSource().getName() : "<no source>")
                            + ":" + d.getLineNumber() + " " + d.getMessage(null));
                }
            }
            if (!Boolean.TRUE.equals(ok) && errors.isEmpty()) {
                errors.add("<compiler task returned false with no ERROR diagnostics>");
            }
            return errors;
        }
    }

    /**
     * The gate classpath: the RELEASED {@code rune-runtime:9.83.0} jar (the oracle law
     * — the runtime surface the goldens/generated code target; the module's own
     * dependency is the vendored {@code 0.0.0.main-SNAPSHOT}, a post-9.83 tree whose
     * locally-built jar is NOT the released surface) plus every non-rune-runtime entry
     * of the test classpath (guava, jakarta.inject, …).
     */
    // Package-private SOT (like released983RuntimeJar()): UpstreamPortHarness (PR #414
    // leg-C port) compiles + loads generated snippets against the SAME gate classpath.
    static String gateClasspath() {
        Path released = released983RuntimeJar();
        StringBuilder cp = new StringBuilder(released.toString());
        for (String entry : System.getProperty("java.class.path")
                .split(java.io.File.pathSeparator)) {
            if (!entry.replace('\\', '/').contains("/rune-runtime/")) {
                cp.append(java.io.File.pathSeparator).append(entry);
            }
        }
        return cp.toString();
    }

    /**
     * The released 9.83.0 runtime jar in the local repository, or null when absent.
     * Package-private SOT for the gate runtime path — {@code WiderPoolCompileGateTest}
     * shares it (Copilot #412 R1: one implementation, no drift).
     */
    static Path released983RuntimeJar() {
        Path jar = Path.of(System.getProperty("user.home"), ".m2", "repository",
                "org", "finos", "rune", "rune-runtime", "9.83.0", "rune-runtime-9.83.0.jar");
        return Files.isRegularFile(jar) ? jar : null;
    }

    /** An in-memory generated source file keyed by its emission-relative path. */
    private static final class InMemorySource extends SimpleJavaFileObject {
        private final String content;

        InMemorySource(String relPath, String content) {
            super(URI.create("string:///" + relPath.replace('\\', '/')), Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
        }
    }
}
