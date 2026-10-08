package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * W42 sweep leg B (hold-out corpora), slice 1 — the golden-free acceptance bars.
 *
 * <p>Runs the fork's FULL generator set over model groups the engine has NEVER gated on
 * (upstream test fixtures; provenance in {@code src/test/resources/holdout/PROVENANCE.md})
 * and asserts, per group:
 * <ol>
 *   <li><b>Crash-freedom</b> — parse + workspace-link + generate complete without a thrown
 *       exception, and every list-returning generator's per-object failure list is EMPTY
 *       (MetaFieldGenerator has no failure-list API — its failures surface as thrown
 *       exceptions, caught by the same bar). A hold-out generation error is a real
 *       robustness finding, not tolerable debt — there is no waiver taxonomy here by
 *       design: a miss is a facet lead, never a new bucket.</li>
 *   <li><b>Determinism</b> — the ENTIRE pipeline (parse → link → generate) run twice from
 *       scratch produces byte-identical output maps (same paths, same content).</li>
 *   <li><b>File-level sanity</b> — every emitted entry has a {@code .java} relative path and
 *       non-blank content. (Full structural sanity — a real javac — and the byte-compare
 *       against 9.83.0-plugin-pinned goldens are the leg-B slice-2 bars; the oracle
 *       scaffolding is deliberately NOT this test's concern. See the sweep design brief §B
 *       and the oracle-provenance caveat in PROVENANCE.md.)</li>
 * </ol>
 *
 * <p>Corpus loading mirrors {@link D11CorpusRegressionTest}'s cell loader: builtins are
 * resolved by filename-union across the same search roots and parsed into the workspace
 * first (hold-out models reference {@code string}/{@code int}/{@code [metadata ...]} with no
 * explicit import), then the group's own files. Emission is filtered to the group's models
 * by IDENTITY (builtins resolve but are not emitted) — no reliance on model-path APIs.
 *
 * <p>Skips (does not fail) when the builtins are absent (CI / fresh clone), mirroring the
 * corpus-absent convention of the D11 gate.
 *
 * <p>Version stamping: since PR #415 this bar DELEGATES to the byte bar's battery
 * ({@code HoldOutByteCompareTest.generateAllKindsFromFiles}), which stamps
 * version-ABSENT models to {@code 0.0.0} (the version law, byte-bar javadoc) —
 * the slice-1 no-stamp note is retired; immaterial for these golden-free bars
 * (the stamp is constant across both determinism runs).
 */
class HoldOutGenerationTest {

    private static final Path HOLDOUT_ROOT = Path.of("src/test/resources/holdout");

    /**
     * Pinned per-group emission counts (Freezing pattern — the Seat-1 #402 OBS-1 control):
     * without these, a regression that silently drops a group's emission to zero would
     * still pass (determinism is trivially true over two empty maps). Re-pin DELIBERATELY
     * — with the change explained in the commit — when generation legitimately changes.
     */
    /*
     * ALL counts DELIBERATELY re-pinned at PR #415: generateOnce now delegates to the
     * byte bar's FULL battery (generateAllKindsFromModels — one battery SOT; the
     * slice-1 copy had silently forked from it when the wave kinds joined at #405-#412,
     * so this bar was not exercising validator/XMeta/datarule crash-freedom on
     * hold-out inputs). For every oracle-comparable group the new count EQUALS its
     * golden-file count (HoldOutByteCompareTest proves identical=N, extra=0,
     * missing=0 — hero-model 49, name-escaping 38, pojo 35, pojo-inheritance 43,
     * report-override 45, reserved-names 18); the two oracle-rejected formatter-INPUT
     * groups measured UNCHANGED at 1 under the full battery (func-only fixtures — no
     * types, so the wave kinds add nothing). The #410 pojo-inheritance 10→11 re-pin
     * note (the override meta-inheritance union) is subsumed — its wrapper is one of
     * the 43.
     */
    private static final Map<String, Integer> EXPECTED_FILE_COUNTS = Map.ofEntries(
            // v3.2 seat 11 (PR #632, M1 / D50 — THE ORACLE FIRST): the five type-named-* groups pinned from the
            // released plugin BEFORE the code (target/v32-seat11-instruments/scratch/oracle-s11a.status @ 283ec5eff,
            // local; deterministic x2) — a data type named after every library token the six data-type templates
            // write: list 17 = List / HolderModelFirst / HolderJavaFirst (3 × (POJO + meta + 3 validators) = 15) + the
            // data rule ListNonEmpty + the function UseList; util 60 = 12 types × 5; guava 20 = 4 × 5; rosetta 85 =
            // 17 × 5; annotations 50 = 10 × 5. Counts = the golden counts (the #415 one-battery law).
            Map.entry("type-named-list", 17),
            Map.entry("type-named-util", 60),
            Map.entry("type-named-guava", 20),
            Map.entry("type-named-rosetta", 85),
            Map.entry("type-named-annotations", 50),
            // v3.2 seat 12 (PR #633, D52 - THE ORACLE FIRST, before any heal code): the three heals' groups pinned from
            // the released plugin, deterministic x2 (target/v32-seat12-instruments/scratch/oracle-s12h2.status, local) -
            // closure-param-duplicate = M12 (`reduce a, a`, the plugin ACCEPTS and numbers the pair: the census's refutation
            // of the s95 seed's verdict, pinned as bytes), rule-meta-output-unwrap = M8a (a rule extracting a function call
            // whose output is `string [metadata scheme]`, single / multi / under a then), only-exists-item-root = M6 (the
            // `item ->` only-exists root in a data-type condition and in a then-extract lambda, with the bare / explicit
            // controls). Counts = the golden counts (the #415 one-battery law; measured, never guessed).
            Map.entry("closure-param-duplicate", 9),
            // round 1 (cq MF-1): the duplicated pair READ through a navigation off the parameter and through a call
            // taking it - pinned from the released plugin BEFORE the fix (oracle-s12r1b.status; deterministic x2)
            Map.entry("closure-param-duplicate-reads", 11),
            // v3.2 seat 13 (PR #634, D53 - the closing seat, commit 4 - THE ORACLE FIRST): the seat's six groups, every one
            // pinned from the released plugin before its heal (target/v32-seat13-instruments/scratch/oracle-s13r7b.status,
            // oracle-s13r10a.status, oracle-s13c4d.status, local).
            Map.entry("arg-coercion-bare-local", 9),
            Map.entry("conv-bigint-alias-long", 3),
            Map.entry("conv-int-literal-long-output", 4),
            Map.entry("list-literal-add-item-coerce", 10),
            Map.entry("tostring-over-default-enum", 11),
            Map.entry("extract-meta-elem-deref-function", 9),
            Map.entry("rule-meta-output-unwrap", 13),
            Map.entry("only-exists-item-root", 33),
            // PR #418 (leg-E burn 4) — the four new oracle-pinned groups; counts =
            // the golden counts (the #415 one-battery law; measured, never guessed).
            Map.entry("expr-alias-default", 1),
            Map.entry("expr-conversions-valid", 2),
            Map.entry("expr-sort-min-max", 6),
            Map.entry("expr-switch", 1),
            // PR #420 (leg-E burn 6) — the five new oracle-pinned groups (all five
            // HEALED byte-exact in the same PR); counts = the golden counts
            // (HoldOutByteCompareTest proves identical=N, extra=0, missing=0:
            // functional-ops 3 funcs; list-literal + logical 1 func each;
            // misc-primaries 12 = 2 types × [POJO + Meta + Validator +
            // TypeFormatValidator + OnlyExistsValidator] + 1 datarule (the Baz
            // optional-choice condition) + 1 func; nested-complex 6 = 1 type ×
            // the same 5 kinds + 1 func).
            Map.entry("expr-functional-ops", 3),
            Map.entry("expr-list-literal", 1),
            Map.entry("expr-logical", 1),
            Map.entry("expr-misc-primaries", 12),
            Map.entry("expr-nested-complex", 6),
            // PR #421 (leg-E burn 7) — the with-meta group: the one VALID fixture of
            // the six-fixture triage; count = the golden count (the func + the
            // FieldWithMetaString metafield the with-meta expression pulls in).
            Map.entry("expr-with-meta", 2),
            // PR #422 (leg-E burn 8) — the two VALID TWINS of the generation-erroring
            // WiderPool pins (both refused fixtures triaged-invalid; twins
            // byte-identical FIRST-RUN, zero heal): func-extends-valid = BaseFunc +
            // ChildFunc, both standalone (extends is validation-only at 9.83.0);
            // report-simple-rule-valid = the Trade POJO + Meta + 3 validators + the
            // 2 rule classes. Counts = the golden counts.
            Map.entry("func-extends-valid", 2),
            Map.entry("report-simple-rule-valid", 7),
            // PR #422 (leg-E burn 8) — the two bulk-arm witness groups (the #412
            // recorded-corner burn): pojo-bulk-meta-drop = Parent + Child + 2 Meta +
            // 5 validators + FieldWithMetaString + ChildTypeFormatValidator... counts
            // = the golden counts (meta-drop: upstream does NOT specialize — the
            // child re-emits the parent's meta shape, 11 files; value-narrow: the
            // int-under-number override, the FIRST bulk ADD_VALUE/SET_VALUE and
            // braced-lambda witnesses, 12 files).
            Map.entry("pojo-bulk-meta-drop", 11),
            Map.entry("pojo-bulk-meta-kind", 12),
            Map.entry("pojo-bulk-value-narrow", 12),
            // PR #424 (the leg-C findings #2/#3 heals) — both fixtures oracle-VALID,
            // counts = the golden counts: pojo-number-ladder = the A POJO + meta +
            // 3 validators (facet inlineNumberLadder's byte witness);
            // expr-date-subtract = the Test POJO stack + the TestQualifier func
            // (facet dateArithTyping/dateArithWitness's byte witness).
            Map.entry("pojo-number-ladder", 5),
            Map.entry("expr-date-subtract", 6),
            // PR #426 (the leg-C findings heal wave) — the six heal-fixture groups,
            // all oracle-VALID at the #426 oracle stage (58 goldens deterministic ×2);
            // counts = the golden counts (the #415 one-battery law;
            // HoldOutByteCompareTest proves identical=N, extra=0, missing=0):
            // expr-bool-nav-logical 8 = Foo POJO stack (5) + the 3 lift funcs (#10);
            // expr-date-time-add 11 = FuncIn/FoncOut stacks (10) + Calc (the #424
            // date+time ADD decline, healed); func-bulk-as-key 12 = WithMeta/OtherType
            // stacks (9) + ReferenceWithMetaWithMeta + asKeyUsage + OtherTypeMeta
            // (#15); func-dispatch-collision 9 = MathInput stack (5) + Math enum +
            // MathFunc + AddOne + SubOne (#13); func-single-to-list-set 16 = the three
            // POJO stacks (15) + AliasOther (#12); func-zero-input-alias 2 = Adder +
            // AddOne (#14).
            Map.entry("expr-bool-nav-logical", 8),
            Map.entry("expr-date-time-add", 11),
            Map.entry("func-bulk-as-key", 12),
            Map.entry("func-dispatch-collision", 9),
            Map.entry("func-single-to-list-set", 16),
            Map.entry("func-zero-input-alias", 2),
            // PR #433 (the findings #21/#22 meta-channel heal wave) — the eleven
            // function-IO meta witness groups, upstream-FunctionGeneratorTest-verbatim
            // models, all oracle-VALID (60 goldens deterministic ×2, ALL byte-identical
            // at the wave close); counts = the golden counts. The two residual-#21
            // groups (as-key-set 12, coercion 3) were BANKED at
            // target/433-heal/deferred-groups/ and JOINED at PR #434 with their
            // faces' heals (facets fnIoMetaAsKeyValueHoist + fnIoMetaListRewrap —
            // the #433 wave-sizing cut's fully-fueled opener).
            Map.entry("func-meta-add-value", 7),
            Map.entry("func-meta-as-key-set", 12),
            Map.entry("func-meta-call-scheme", 3),
            Map.entry("func-meta-choice-ignore", 7),
            Map.entry("func-meta-coercion", 3),
            Map.entry("func-meta-deep-path-multi", 24),
            Map.entry("func-meta-max", 2),
            Map.entry("func-meta-min", 2),
            Map.entry("func-meta-passthrough", 7),
            Map.entry("func-meta-scheme-arith", 2),
            Map.entry("func-meta-scheme-nav", 2),
            Map.entry("func-meta-sort", 2),
            Map.entry("func-meta-sum", 2),
            // PR #434 (the findings #23/#24 record heal) — the four record witness
            // groups, upstream-FunctionGeneratorTest-verbatim models, all
            // oracle-VALID (8 goldens deterministic ×2); counts = the golden
            // counts: ctor-null = handlesNullWhenConstructingRecords (Foo
            // zonedDateTime + Bar date); ctor = recordConstructorExpression
            // (CreateDate); datetime-members = testAccessToDateTimeMembers
            // (GetDate + GetTime); zoned-members = testAccessToZonedDateTimeMembers
            // (GetDate + GetTime + GetZone).
            Map.entry("func-record-ctor", 1),
            Map.entry("func-record-ctor-null", 2),
            Map.entry("func-record-datetime-members", 2),
            Map.entry("func-record-zoned-members", 3),
            // PR #435 (the finding #25 alias heal) — the two alias-rooted-SET witness
            // groups, upstream-FunctionGeneratorTest-verbatim models, both
            // oracle-VALID (32 goldens deterministic ×2 at the #434 bank,
            // target-434-oracle2.log); counts = the golden counts:
            // creation-lhs = shouldGenerateFunctionWithCreationLHSUsingAlias
            // (ExtractBar — the fused single-segment prefix); assign-output =
            // shouldGenerateFunctionWithAliasAssignOutput (UpdateBarId — the
            // two-segment fuse + the id-leaf resolution against the alias's
            // terminal type). BANKED at target/434-heal/deferred-groups/ by the
            // #434 wave-sizing cut; JOINED here with the #435 heals (facets
            // aliasRootedSetSegmentResolution + aliasRootedSetFusedTargetPrefix).
            Map.entry("func-alias-assign-output", 16),
            Map.entry("func-alias-creation-lhs", 16),
            // PR #436 (the six-singles heal wave, findings #26–#31) — the six witness
            // groups, upstream-FunctionGeneratorTest models (the five headerless ones
            // + the verbatim-header 058), all oracle-VALID (15 goldens deterministic
            // ×2); counts = the golden counts: named-func-ref 8 = Incr +
            // IsAnswerToTheUniverse + ClosestToTen + F1–F5 (#26); call-single-to-list
            // 3 = A + B + C (#27); self-call 1 = Rec (#28); filter-null-pred 1 = Test
            // (#29); java-lang-self 1 = Boolean (#30); java-keyword-attr 1 = This
            // (#31).
            Map.entry("func-call-single-to-list", 3),
            Map.entry("func-filter-null-pred", 1),
            Map.entry("func-java-keyword-attr", 1),
            Map.entry("func-java-lang-self", 1),
            Map.entry("func-named-func-ref", 8),
            Map.entry("func-self-call", 1),
            // PR #437 (the board-clearing heal wave, findings #32–#38 + the 2
            // validation pins) — the nine witness groups, upstream-
            // FunctionGeneratorTest/RosettaBlueprintTest models, all oracle-VALID
            // (58 goldens deterministic ×2, ALL byte-identical first-gate-run);
            // counts = the golden counts: rule-recursion 1 = FacRule (#32);
            // ctor-as-key-meta 12 = Foo/Bar stacks + ReferenceWithMetaFoo + Test
            // (#35); one-of-static 12 = A/B stacks + TestOnlyExists + TestOneOf
            // (#36); extract-empty-arm 1 = Foo (#37); ctor-as-key-ref 12 =
            // TypeWithKey/OtherType stacks + ReferenceWithMetaTypeWithKey +
            // CreateOtherType (#35); omitted-param 1 = F1 (#38);
            // set-single-complex 11 = Bar/Foo stacks + FuncFoo (#33);
            // set-single-basic 6 = Baz stack + FuncFoo (#33); if-literal-call 2 =
            // A + B (#34).
            // PR #624 (v3.2 seat 3, the silent pair F9 + F12) — the three oracle-pinned
            // sub-shape groups (released 9.83.0 plugin, deterministic ×2); counts = the golden
            // counts (HoldOutByteCompareTest proves identical=N, extra=0, missing=0 at the fix
            // head): alias-conditions 21 = Holder/Bare stacks (5 + 5) + FieldWithMetaString
            // + IsOk + 8 alias-owned datarules + HolderHolderOk; alias-conditions-meta 8 =
            // MetaHolder stack (5) + FieldWithMetaInteger + UseNat + NatNonNeg;
            // func-dispatch-namespaces 7 = three enums + four Speed classes (a, b, d dispatch
            // groups + c's plain function). A fourth pinned group, alias-conditions-param (6
            // goldens — the PARAMETERISED alias condition, which the released plugin emits as a
            // self-comparison), was WITHDRAWN from the gate at the fix commit: the fork REFUSES
            // that shape, and this bar has no waiver by design; its oracle output is banked at
            // target/v32-seat3-instruments/oracle-param/ (local) and the refusal is pinned by
            // SilentPairSeatTest.
            Map.entry("alias-conditions", 21),
            Map.entry("alias-conditions-meta", 8),
            Map.entry("func-dispatch-namespaces", 7),
            // PR #624 round 1 (the code-quality review's MF-2 / MF-3): the scope-law groups,
            // oracle-pinned the same way — alias-conditions-scope 21 = Clash / Loop / FieldClash /
            // SingleNames stacks (4 × 5) + NatNonNeg (attributes named results / o / i / natNonNeg:
            // the `_` escape and the index-before-local numbering); alias-conditions-twins 7 =
            // Both stack (5) + the two same-named EvenNatNonNeg classes of namespaces a and b
            // (numbered fields, the second FQN-inlined); alias-conditions-reserved 18 = Keywords /
            // PathClash / ResultsClash stacks (3 × 5) + NatNonNeg + Path + Results (Java-keyword
            // attribute names; condition classes named after the wing's own identifiers).
            Map.entry("alias-conditions-scope", 21),
            Map.entry("alias-conditions-twins", 7),
            Map.entry("alias-conditions-reserved", 18),
            // PR #624 round 2 (the spec review's MF-1 / the code-quality review's SF-4): the FILE-scope
            // group, oracle-pinned the same way — alias-conditions-filescope 22 = Own / Imported /
            // Written / WrittenSingle stacks (4 × 5) + NatNonNeg + the condition class
            // OwnTypeFormatValidator (its FIRST pin, 12, predates the Written / WrittenSingle types —
            // PROVENANCE; round-3 cq N-2) (the validator's own
            // simple name, written fully qualified; attribute locals named after the types the file
            // writes — Streams / ArrayList / Inject / Integer — escaped `_`). Its sibling
            // alias-conditions-boilerplate (a condition class named after a boilerplate import the
            // validator writes AFTER its fields: upstream claims the condition class first and writes
            // the boilerplate type fully qualified) is BANKED at target/v32-seat3-instruments/
            // oracle-boiler/ (local): the fork REFUSES that shape (BOILERPLATE_NAME_COLLISION, pinned
            // by SilentPairSeatTest a17), and this bar has no waiver by design.
            Map.entry("alias-conditions-filescope", 22),
            // PR #624 round 3 (the code-quality review's SF-3): the HEADER-claim group — a condition
            // class named after the validator's data class (`PayLoad`) and one named `Inject`, both
            // written fully qualified by the released plugin (the class header claims those names
            // before the first field): alias-conditions-header 12 = PayLoad / Injected stacks (2 × 5)
            // + the two condition classes.
            Map.entry("alias-conditions-header", 12),
            // v3.2 seat 4 (F7 + F10): the ten order-dependence groups pinned from the released plugin BEFORE
            // the code - counts = the golden counts (the #415 one-battery law; measured, never guessed).
            Map.entry("report-withtype-shadow", 28),
            Map.entry("report-withtype-qualified", 26),
            Map.entry("report-rules-split", 28),
            Map.entry("report-withsource-shadow", 27),
            Map.entry("qualify-first-wins", 13),
            Map.entry("qualify-event-and-product", 12),
            Map.entry("qualify-cross-namespace-input", 7),
            Map.entry("qualify-order-probe-a", 18),
            Map.entry("qualify-order-probe-b", 18),
            Map.entry("zz-qualify-order-probe-c", 18),
            // v3.2 seat 4, round 1 (the code-quality review MF-1): the report type shadowed by a same-named reporting
            // rule declared FIRST - the released plugin resolves `with type` by KIND (a Data cross-reference); pinned
            // BEFORE the fork's kind-gated ladder (scratch/oracle-s4d.status; the fork REFUSED it at TYPE_NOT_FOUND first).
            Map.entry("report-withtype-rule-shadow", 15),
            // v3.2 seat 5 (F6 + F3), commit 2: the six oracle groups pinned from the released plugin BEFORE the code
            // (scratch/oracle-s5a.status; the withmeta edge group re-pinned after the plugin REFUSED `with-meta` on a
            // multi-cardinality argument - an upstream validation rule); counts = the golden counts, measured.
            Map.entry("conv-bigint-statement", 7),
            Map.entry("conv-bigint-statement-edge", 8),   // round 1: + IdentityOut + EscapedName (scratch/oracle-s5b.status); round 2: + MultiWhole (oracle-s5c)
            Map.entry("withmeta-wrapped-argument", 17),
            Map.entry("withmeta-wrapped-argument-edge", 24),   // round 1: + KeyedHolder (5 files) + AliasKeyed (scratch/oracle-s5b.status); round 2: + CallKeyed (oracle-s5c)
            Map.entry("meta-ladder-alias-rung", 17),
            Map.entry("meta-ladder-alias-rung-edge", 16),
            // v3.2 seat 6 (F5), commit 2: the two deep-path oracle groups pinned from the released plugin BEFORE the code
            // (target/v32-seat6-instruments/scratch/oracle-s6a.status the core group; oracle-s6b.status the edge group,
            // re-pinned after the plugin REFUSED its first cut on a grammar law - "Usage of `then` is mandatory" at a
            // parenthesised count / exists over an extract); counts = the golden counts, measured.
            Map.entry("deep-path-util-injection", 44),
            Map.entry("deep-path-util-injection-edge", 64),
            // v3.2 seat 7 (F11), commit 2: the two choice-switch-in-lambda oracle groups pinned from the released plugin
            // BEFORE the code (target/v32-seat7-instruments/scratch/oracle-s7c.status the core group, 45 goldens;
            // oracle-s7a.status the edge group, 58); at commit 2 the counts were the FORK's emitted population at the
            // pre-fix head (37 / 50 - the golden count LESS the 8 + 8 DECLARED refusals, scratch/battery-pre.status);
            // RE-PINNED to the golden counts at commit 4 as the refusals healed (scratch/d11-fix4.status).
            Map.entry("choice-switch-in-lambda", 45),
            Map.entry("choice-switch-in-lambda-edge", 58),
            // v3.2 seat 7 (F11), commit 7: the THIRD oracle group - the seat-31 e1 shape (a bare implicit-ITEM case body over a
            // MODEL-CHOICE lambda item), pinned from the released plugin AFTER the chain of record s7a caught e1's pin of the
            // instanceof fall-through as "today's bytes" (22 goldens, deterministic x2; the fork's widened arm law renders the
            // plugin's option-getter form byte-identically - the v3.1 decline lock had pinned a WRONG emission). Its edge (a bare
            // item in BOTH arms, the join `any`) was REFUSED by the plugin at typing and is a receipt under
            // target/v32-seat7-instruments/refused-groups/, not a group.
            Map.entry("choice-switch-in-lambda-bare-item", 22),
            // v3.2 seat 9 (F1 / F8 / F13, the adjudication cluster), commit 3: FIVE oracle groups pinned from the released
            // plugin BEFORE the code (target/v32-seat9-instruments/scratch/oracle-s9a.status; the F1 edge group re-run as
            // oracle-s9b after the released grammar REFUSED its first cut on an attribute named `e` - the seat-8 banked
            // lexer refusal met again). F1: enum-unicode-display (the a5uni shape + ASCII / non-ASCII / absent display
            // names + a non-ASCII definition, 9 goldens) and -edge (a quote, a backslash, a tab in a display name - RAW in
            // the released plugin's @RosettaEnumValue annotation, non-compiling; a non-ASCII enum-value synonym - RAW in
            // @RosettaSynonym; 7 goldens). F8: void-mapping-basic-record (a model-declared basicType and recordType at
            // optional / list / required seats, 15 goldens - java.lang.Void at every seat, List<Void> for the list) and
            // -edge (under a typeAlias, a PARAMETERISED basic type, [metadata scheme] / [metadata reference] - the
            // released plugin mints FieldWithMetaVoid / ReferenceWithMetaVoid, the CDM 6.20.6 PERMANENT waiver's own file
            // shape - a condition over the Void attribute (`exists(MapperS.<Void>ofNull())`) and a Void function input
            // and output; 24 goldens). F13: x36enum-split-p1 - the L1-CLEAN half of the chaos s17 rival-enum split ALONE
            // (its wildcard import of p2 dangling): 8 goldens, deterministic x2 - the half the chaos pin script had
            // EXCLUDED as a "split partner" HAS definable goldens, and the fork's eight emissions are byte-identical to
            // them (ds9m2-off/*/nogolden vs holdout-goldens/x36enum-split-p1). Its two siblings were REFUSED by the
            // plugin and are banked under target/v32-seat9-instruments/refused-groups/ with the mojo's run1.log:
            // x36enum-split-p2 alone (twelve unresolved references - C17ActionEnum / C17Held / Buy / Amend / Sell) and
            // x36enum-split-pair (BOTH halves in one invocation: exactly the two `Sell` errors the L1 census pinned,
            // "Execution failed due to a severe validation error" - the mojo's all-or-nothing gate MEASURED: an
            // invocation with one link error emits NOTHING for any file).
            Map.entry("enum-unicode-display", 9),
            Map.entry("enum-unicode-display-edge", 7),
            Map.entry("void-mapping-basic-record", 15),
            // 24 -> 25 at round 1: MetaCarrier.TokPresent, a META-annotated Void feature under exists, re-pinned from
            // the released plugin (the hasMeta guard's witness - cq SF-4; target/v32-seat9-instruments/scratch/oracle-s9d.status)
            Map.entry("void-mapping-basic-record-edge", 25),
            Map.entry("x36enum-split-p1", 8),
            // v3.2 seat 9 (F8), the render-law commit: TWO more oracle groups pinned from the released plugin BEFORE the
            // render-law code (target/v32-seat9-instruments/scratch/oracle-s9c.status, each deterministic x2; round 1's
            // catch: that receipt's status line shows the two render-law FILES modified at pin time - the temporary probe
            // of t-adj-probe1.log.err, 05:20 - while the pre-fix bar t-holdout-r0.log at 05:30 still reads 5/13 and 10/12,
            // so no render-law code was live when the groups were pinned) - the `nothing` render
            // law at the seats the edge group does not reach. void-mapping-render-edge (13 goldens): a Void-typed
            // function INPUT under exists / is absent / single exists (`exists(MapperS.<Void>ofNull())` - the IR route's
            // scalar-parameter existence claim seat), a Void feature under is absent and a Void LIST feature under exists,
            // a Void value into a non-Void scalar output (`r = null`), a Void list into a MULTI output
            // (`rs = Collections.<Void>emptyList()`) and ADDED to one (`rs.addAll(Collections.<Void>emptyList())`).
            // void-mapping-render-builder (12 goldens): a Void value into a MODEL-typed output (`h = null` - no toBuilder
            // wrap, upstream's mapExpressionIfNotNull) and into a model-typed attribute of a deep-path output
            // (`.setHolder(null)`).
            Map.entry("void-mapping-render-edge", 13),
            Map.entry("void-mapping-render-builder", 12),
            // round 1 (target/v32-seat9-instruments/scratch/oracle-s9d.status, each deterministic x2): void-mapping-render-hoist
            // (3 goldens) - a Void CONDITIONAL operand under exists (`r = exists(MapperS.<Void>ofNull()).get()`, no hoisted
            // local), a Void conditional value SET into a Void output (`r = null`) and ADDED to a Void multi output
            // (`rs.addAll(Collections.<Void>emptyList())`): upstream discards the whole compiled builder at its coercion's
            // early exit (cq SF-3 / SF-7); void-mapping-name-collision (5 goldens) - a model-declared `basicType string`
            // ACCEPTED by the released plugin, the attribute rendered `String`: the fork's name lookup (the builtin wins)
            // is upstream's rule (cq SF-8).
            Map.entry("void-mapping-render-hoist", 3),
            Map.entry("void-mapping-name-collision", 5),
            // round 2 (target/v32-seat9-instruments/scratch/oracle-s9f.status @ 5e84fc2c3, deterministic x2): void-meta-output-set
            // (7 goldens) - a Void-typed value (a model-declared basicType feature, no with-meta construction) SET into a
            // scheme-annotated Void output; the released plugin renders the EMPTY wrapper, `t = toBuilder(FieldWithMetaVoid
            // .builder().build())` - the value DROPPED at its coercion. The fork renders `toBuilder(<the Void value>)`, which
            // does not compile: DECLARED at the byte bar and the compile gate (the seat's META-OUTPUT decline hands the
            // value to the plain path - the shape the c12 sweep's L19 could not witness). The three with-meta shapes of the
            // same oracle run (with-meta-plain-output 7, with-meta-conditional-plain-output 7, with-meta-under-exists 2) stay
            // PARKED under target/v32-seat9-instruments/oracle-r2/: each golden set carries ReferenceWithMetaVoid.java, a
            // file the fork does not emit at all, and the byte bar fails a MISSING file whatever the pins say - BANKED with
            // their receipts, the heal (a with-meta construction over `nothing` minting the Void wrapper, upstream's
            // ReferenceWithMetaVoid the fork skips by the #405 decision) a seat of its own.
            // v3.2 seat 13 (PR #634, D53, commit 4): 7 -> 6, SetMetaVoid REFUSED at R13 (a declared refusal emits nothing).
            Map.entry("void-meta-output-set", 6),
            // round 2 (scratch/oracle-s9g2.status, deterministic x2): void-mapping-render-hoist-second (2 goldens) - a Void
            // conditional under exists FOLLOWED by a non-Void conditional in the same expression (upstream's discarded
            // builder consumes NO name: the kept one is the bare `ifThenElseResult` - the law the round-1 discarded compiles
            // broke, cq MF-1 / spec MF-2; and THE ORACLE'S OWN CATCH: the kept literal-armed conditional under `=` is a
            // `final MapperS<String>` hoist upstream where the fork renders a plain String that does not compile at
            // `areEqual` - an OLDER defect, DECLARED at the two bars, banked) and an ELSELESS Void conditional ADDED (the
            // same single addAll, spec SF-1);
            // void-mapping-deep-tok (19 goldens) - a scheme-annotated Void feature reached by the DEEP path over a choice
            // (the FieldWithMetaVoid util getter; cq SF-1 - the plain-type receiver REFUSED upstream,
            // scratch/oracle-s9g-refused-deeptok-plain.status: "Couldn't resolve reference to Attribute 'tok'").
            Map.entry("void-mapping-render-hoist-second", 2),
            Map.entry("void-mapping-deep-tok", 19),
            // round 3 (scratch/oracle-s9i.status the segment-conditional-set group; oracle-s9i2.status the empty-else group and
            // the collapsed-receiver's FIRST cut; oracle-s9i3.status the collapsed-receiver's pin of record after its redesign -
            // all @ 28cd53841 with only fixture dirs and the registry changed, each deterministic x2 - the code-quality
            // review's MF-1, the spec review's SF-6, the spec review's NIT-4): void-mapping-segment-conditional-set (6
            // goldens) - a Void CONDITIONAL value SET into a single-hop SEGMENT leaf (`set c -> tok: if flag then t else u`),
            // which upstream coerces to the bare `c.setTok(null);` (no hoist) where the fork's pathed-conditional arm
            // hoisted a Void-typed `ifThenElseResult` local (the operation seat's THIRD path); void-mapping-collapsed-receiver
            // (12 goldens) - a Void feature HOP over a conditional RECEIVER (`(if flag then c1 else c2) -> tok`): upstream's
            // collapsed receiver DOES consume the `ifThenElseResult` name (the two-op CollapsedReceiverThenConditional's
            // second conditional is `ifThenElseResult1`), so the fork's drain-the-local / keep-the-name at the general path
            // is CORRECT here - the counter advances on both sides (the spec review's SF-6, refuting round 2's "leaks a
            // number" mechanism claim for this shape by measurement); void-mapping-empty-else (2 goldens) - the `empty`-else
            // leg of round 2's SF-1 (`if flag then t else empty`), the same single addAll / `r = null` as the elseless one.
            Map.entry("void-mapping-segment-conditional-set", 6),
            Map.entry("void-mapping-collapsed-receiver", 12),
            Map.entry("void-mapping-empty-else", 2),
            // round 3 (scratch/oracle-s9i5.status @ 28cd53841, deterministic x2 - the pin of record; oracle-s9i4.status the
            // SUPERSEDED first cut, 1 golden, whose comparison-operand second conditional hit the literal-armed defect of the
            // declared pin - the spec review's SF-6, the code-quality review's MF-3): void-mapping-exists-then-clean (6 goldens)
            // - a Void conditional under `exists` SET into one leaf, FOLLOWED by a CLEAN pathed string conditional SET into the
            // sibling leaf. Pinned AFTER the round-3 segment fix was in the tree (oracle-s9i5's own status line - round 4, spec
            // MF-1 / rule6 SF-1): its goldens are the released plugin's and the law it witnesses is round 2's COMMITTED one, so
            // the fork's tree could taint neither. Upstream coerces the Void exists
            // operand to `exists(MapperS.<Void>ofNull())` WITHOUT compiling it, so the kept conditional is the BARE
            // `final MapperS<String> ifThenElseResult` (index 0 - the discarded operand mints no name). render-hoist-second's
            // ExistsThenConditional carries the same shape but is a DECLARED byte pin (its literal-armed comparison does not
            // compile), so it cannot witness the counter; this file compiles and DOES - the naming law's clean witness (lane
            // L14 re-introduces round 1's compile and numbers the kept conditional `ifThenElseResult1`, turning it red).
            // The second op is a pathed STRING SET (`set p -> s: if a = b then "y" else "z"`) whose conditional both
            // sides render as a plain `String ifThenElseResult` (the leaf type, NOT a comparison operand - so it
            // compiles and byte-matches, unlike render-hoist-second's literal-armed comparison); the exists result is
            // SET into a sibling boolean leaf. 6 goldens (the Pair output's POJO / meta / validators + the function).
            Map.entry("void-mapping-exists-then-clean", 6),
            Map.entry("report-rule-recursion", 1),
            Map.entry("func-ctor-as-key-meta", 12),
            Map.entry("func-one-of-static", 12),
            Map.entry("func-extract-empty-arm", 1),
            Map.entry("func-ctor-as-key-ref", 12),
            Map.entry("func-omitted-param", 1),
            Map.entry("func-set-single-complex", 11),
            Map.entry("func-set-single-basic", 6),
            Map.entry("func-if-literal-call", 2),
            // PR #416 (leg-E burn 2) — the default-op facet group: one func, one file
            // (oracle-pinned golden; HEALED byte-exact at PR #417 — the byte bar's
            // PINNED_FACET_LEADS entry retired with the heal).
            // (Map.ofEntries since #416 — Map.of caps at 10 pairs.)
            Map.entry("expr-default-op", 1),
            Map.entry("formatting-nestedConstructor", 1),
            Map.entry("formatting-onlyExists", 1),
            Map.entry("formatting-typeAlias", 0),
            Map.entry("formatting-typeAliasWithDocumentation", 0),
            Map.entry("hero-model", 49),
            Map.entry("name-escaping", 38),
            // PR #415 (leg E) — the reserved-instance-name group: 3 types × (POJO +
            // Validator + TypeFormatValidator + OnlyExistsValidator + datarule) + 3
            // XMeta = 18, oracle-pinned byte-identical (reserved-names goldens).
            Map.entry("reserved-names", 18),
            Map.entry("pojo", 35),
            Map.entry("pojo-inheritance", 43),
            Map.entry("report-override", 45));


    static Stream<Path> holdoutGroups() throws IOException {
        try (var stream = Files.list(HOLDOUT_ROOT)) {
            return stream.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList().stream();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("holdoutGroups")
    void holdout_group_generates_cleanly_and_deterministically(Path groupDir) throws IOException {
        // The byte bar's resolver — the local copy retired with the battery fork (PR #415).
        List<Path> builtinFiles = HoldOutByteCompareTest.resolveBuiltinFiles();
        Assumptions.assumeTrue(!builtinFiles.isEmpty(),
                "rune-dsl builtins absent — hold-out skipped");

        Map<String, String> first = generateOnce(groupDir, builtinFiles);
        Map<String, String> second = generateOnce(groupDir, builtinFiles);

        // Determinism: the full pipeline, run twice from scratch, byte-agrees.
        Assertions.assertEquals(first.keySet(), second.keySet(),
                groupDir.getFileName() + ": the two runs emitted different file sets");
        for (var e : first.entrySet()) {
            Assertions.assertEquals(e.getValue(), second.get(e.getKey()),
                    groupDir.getFileName() + ": content differs between runs for " + e.getKey());
        }

        // File-level sanity.
        for (var e : first.entrySet()) {
            Assertions.assertTrue(e.getKey().endsWith(".java"),
                    groupDir.getFileName() + ": non-.java output path " + e.getKey());
            Assertions.assertFalse(e.getValue().isBlank(),
                    groupDir.getFileName() + ": blank content for " + e.getKey());
            // v3.2 seat 11 round 1 (cq SF-4): no first-claim sentinel reaches an emitted file - the belt over the
            // whole battery (U+E000 is invisible in most diffs; a leak in a kind with no golden would be silent)
            Assertions.assertFalse(com.regnosys.rosetta.generator.java.template.ImportCollisionResolver.hasSentinel(e.getValue()),
                    groupDir.getFileName() + ": a first-claim sentinel leaked into " + e.getKey());
        }

        // Pinned emission count — see EXPECTED_FILE_COUNTS.
        Integer expected = EXPECTED_FILE_COUNTS.get(groupDir.getFileName().toString());
        Assertions.assertNotNull(expected,
                groupDir.getFileName() + ": new hold-out group — pin its expected file count");
        Assertions.assertEquals(expected.intValue(), first.size(),
                groupDir.getFileName() + ": emission count moved (re-pin deliberately if the"
                        + " generation change is intended)");

        System.out.println("HOLDOUT " + groupDir.getFileName() + ": " + first.size()
                + " files emitted, deterministic, zero generation errors");
    }

    /**
     * One full pipeline run: parse builtins + group, link, generate ALL kinds — since
     * PR #415 a DELEGATE to {@link HoldOutByteCompareTest#generateAllKindsFromFiles}
     * (one battery SOT; this bar's slice-1 copy had silently forked from the byte
     * bar's battery as the wave kinds joined at #405–#412, leaving validator/XMeta/
     * datarule crash-freedom unexercised on hold-out inputs). Delegation also
     * version-stamps absent versions to {@code 0.0.0} like the byte bar (the old
     * no-stamp note is retired — immaterial for these bars, identical across runs).
     */
    private static Map<String, String> generateOnce(Path groupDir, List<Path> builtinFiles)
            throws IOException {
        // Full-path ordering (mirrors the D11 cell walk) — filename-only ordering is ambiguous for
        // nested dirs / same-named files (Copilot #402 R1); the group's replay pin, if any, first
        // (v3.2 seat 4 — HoldOutByteCompareTest.GOLDEN_QUALIFIABLE_ROOT_ORDER_PINS)
        List<Path> groupFiles = HoldOutByteCompareTest.groupFiles(groupDir);
        Assertions.assertFalse(groupFiles.isEmpty(),
                groupDir.getFileName() + ": no .rosetta files in the group");

        HoldOutByteCompareTest.GenerationRun run =
                HoldOutByteCompareTest.generateAllKindsFromFiles(groupFiles, builtinFiles);
        // v3.2 seat 7: the ONE consult of the declared fork refusals (HoldOutByteCompareTest.DECLARED_REFUSALS).
        return HoldOutByteCompareTest.admitDeclaredRefusals(groupDir.getFileName().toString(), run);
    }
}
