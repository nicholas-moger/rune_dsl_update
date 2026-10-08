package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.parser.ChaosParseExpectations;
import com.regnosys.rosetta.testutil.CorpusCatalogue;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import com.regnosys.rosetta.symbols.diagnostics.LinkingDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.diagnostics.ValidationDiagnostic;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V1 of the drop-in parity program's Leg V (PR #440): the corpus diagnostic
 * gate. Pins the fork's linking + validation ERROR stream over the two
 * mission corpora as per-{@link DiagnosticCategory} /
 * per-{@link ValidationIssueCode} budget tables, asserted EXACTLY as full-map
 * equality — missing keys, extra keys, and per-value drift all fail loudly
 * (nothing is absorbed getOrDefault-style).
 *
 * <p><b>Population (the PR #443 #4-class-(c) decision — ADOPT-TOPOLOGY):</b>
 * each corpus loads the UPSTREAM BUILD CLOSURE, exactly the composition the
 * D11 generator cells have always used ({@code D11CorpusRegressionTest}:
 * builtins fail-loud-first since Copilot R7 F1 · transitive CDM P2.1.1 ·
 * transitive iso20022 PR #155 · transitive fpml PR #184/#185) and exactly what
 * upstream's own mojo builds (the builtin library models are injected into
 * every build; drr's union source root unpacks the cdm-java + iso
 * rosetta-source jars): {@code drr} = builtins + cdm-5.38.0 + iso20022-1.38.0
 * + drr-6.34.1 (the 5.37.0→5.38.0 / 1.37.0→1.38.0 substitutions, measured
 * byte-neutral at PR #440) and {@code cdm} = builtins + rune-fpml-1.5.3
 * (cdm6's pin, resolution-only) + BOTH cdm cells (the merged-two-cell space is
 * a deliberate fork testing convenience — since PR #445 the {@link
 * CellPreference} same-cell resolution preference, driven by the
 * {@code CELL_ROOTS} this gate passes to {@link RWorkspace#build(List, List)},
 * makes each cell resolve as upstream's isolated build would). Closure roots
 * missing while the corpus is present fail LOUD (the D11 law); the
 * end state ("linking ERRORS → 0 on valid models") is only meaningful against
 * this population — the V0 oracle streams were captured over it. Before the
 * #443 re-seed this gate loaded the bare per-corpus roots
 * ({@link SymbolTableSnapshotTest}'s population, which deliberately keeps
 * measuring the fork's own per-corpus symbol surface): thousands of
 * dependency-topology rows (ISOCurrencyCodeEnum 748× etc.) were structurally
 * unhealable there.
 *
 * <p><b>Shrink-only contract:</b> every validation-parity wave that heals a
 * diagnostic class ratchets its budget row DOWN (or deletes it at zero) in the
 * same commit — the budget tables ARE the Leg V burn-down board. An upward
 * move, a new category, or an under-budget count all fail loudly: any drift is
 * a measured event, never silent. History: the PR #440 seed (the bare-roots
 * population) reconciled with the PR #438 census to the digit (linking cdm
 * 11,401 / drr 20,716; validation ERROR-only cdm 1 / drr 239); the PR #441 #5
 * wave took the first validation ratchet (drr TYPE_ERROR 235 → 171, the 64
 * string-concat false positives); the PR #442 Annex-B(B-1) wave EMPTIED the
 * drr validation table (175 → 0); the PR #443 class-(c) population re-seed
 * (this epoch) re-measured every table against the upstream build closure —
 * the per-table javadocs below carry the re-seed story.
 *
 * <p><b>The end state (the V0 oracles):</b> the released 9.83.0 plugin's own
 * mojo stream over these corpora is ZERO errors — cdm/6.20.6 = 0 errors + 801
 * warnings in seven message classes (bank {@code target/439-v0-oracle/},
 * local) and drr/6.34.1 = 0 errors + 3,786 warnings in nine classes (bank
 * {@code target/440-v0-oracle-drr/}, local). The gate closes when both ERROR
 * tables here are EMPTY on the models upstream validates; each wave
 * additionally proves its warning messages byte-identical to upstream's (the
 * V0 banks are the oracle — WARNINGs are deliberately NOT budgeted here
 * because landing upstream's validators GROWS the warning stream toward the
 * oracle's, so a warning pin would fight the program). Since the #454 Annex-A
 * wave the cdm table carries the HONEST FIXTURE FLOOR (the tests/ fixture's
 * arity error — see the VALIDATION_ERROR_BUDGETS comment; at the v3.1
 * close-out's per-cell seed it is TYPE_ERROR 1 in each of the ten cdm cells,
 * the pre-seed "2-row" being the merged two-cell count): the board reads
 * all-zero ON VALID MODELS, the fixture floor standing exactly like the
 * linking tables' per-cell 3 + 1 (the pre-seed "8" over the merged corpus;
 * cdm 6.21.0+ and drr 7.x add upstream's own two unresolved imports). Since
 * the #455 warning-family waves the
 * NON-NAMING warning surface is per-site (file, message) IDENTICAL to both
 * V0 banks — cdm 801/801 exact and drr 3,783/3,786 (the 3 recorded iosco
 * bare-single under-fires; the tests/ fixture's codeImplementation line is
 * the same population-artifact class as the error floor). The #456
 * naming-surface alignment retired the LAST fork-fired/bank-absent class
 * (the former type-alias case arm's 48 rows — the builtins'
 * {@code typeAlias int/productType/eventType/calculation} and iso20022's 40
 * dtcc-rds aliases; upstream's naming checks cover Data/Choice, enums,
 * functions, conditions and non-option non-annotation attributes ONLY, at
 * the released message bytes — see {@code NamingValidator}), so the
 * per-site identity now holds over the WHOLE warning surface: over-fires
 * ZERO on valid models both corpora, the oracle streams fully matched on
 * BOTH surfaces with no class qualifier. The #457 list-of-lists item wave
 * (the {@code CardinalityComputer.outputIsListOfLists} mirror of upstream's
 * un-flattened {@code then extract} model — see
 * {@code LoLItemCardinalityWaveTest}) healed the 3 recorded iosco
 * bare-single under-fires, the LAST divergence in EITHER direction:
 * <b>cdm 801/801 and drr 3,786/3,786 — the fork's whole diagnostic surface
 * is per-site identical to the V0 oracle streams on valid models, complete
 * in both directions.</b>
 *
 * <p><b>Seeding / ratchet:</b>
 * {@code mvn -f rune-parser/pom.xml test -Dtest=CorpusDiagnosticGateTest
 * -Dcorpus.diag.print=true -Dmaven.build.cache.enabled=false} prints each
 * budget map's literal BODY as valid Java, one {@code [V1-SEED]}-prefixed
 * line per row — strip the prefix and paste as the {@code Map.ofEntries(...)}
 * argument list of the corresponding constant (since the v3.1 close-out seed
 * the per-cell entries share the four {@code *_FLOOR} constants; a cell whose
 * printed body equals a floor points at the constant, any other body is
 * written out as printed).
 *
 * <p>Skips (JUnit assumption) when the local corpus tree is absent (CI /
 * fresh clone), like {@code Corpus983BaselineManifestTest}.
 */
class CorpusDiagnosticGateTest {

    /**
     * Per-corpus root lists in LOAD ORDER (builtins → transitive closures → the
     * corpus's own tree — the D11 cell-loader order). The LAST entry is the
     * corpus's own root: absent ⇒ the whole gate assumption-skips (CI / fresh
     * clone, like {@code Corpus983BaselineManifestTest}); a CLOSURE root absent
     * while the own root exists fails LOUD (a broken local corpus clone — the
     * D11 fail-loud law).
     */
    /**
     * Corpora this gate measures. The CELLS within them, and each cell's dependency
     * closure, are DERIVED from {@code test-corpus/corpus-cells.tsv}; only the
     * choice of which corpora to gate is stated here.
     *
     * <p>iso20022 and rune-fpml are deliberately out of scope, as they were before
     * the 2026-08-14 rework — extending the gate to them is a coverage change and
     * belongs in its own commit, not folded into a defect fix. (ISO additionally
     * keeps 47 duplicate {@code .rosetta} copies under {@code target/}, which a
     * naive cell-dir load would pull in twice.)
     *
     * <p><b>chaos (v3.2 PR-2) is deliberately ungated here too</b> — the charter § 3
     * decision: the chaos cell's diagnostic contract is the L1 admission gate
     * ({@code scripts/chaos-expander/l1-gate.py} — every file admitted per-row by the
     * executing 9.83.0 oracle against the frozen 25-row EXPECTED census), not this
     * budget-map gate, whose zero-budget claim is measured against the ORACLE's V0
     * streams on the vendored corpora. Chaos files are DESIGNED to carry errors
     * (dangling refs, rival captures, BOM refusals); folding them into a shrink-only
     * budget would either freeze deliberate breakage as budget or mask real drift.
     * Its cell row (deps {@code -}) loads harmlessly through {@code CorpusCatalogue}:
     * the corpus filter above simply never selects it.
     */
    private static final Set<String> GATED_CORPORA = Set.of("cdm", "drr");

    /**
     * The cells this gate builds, one workspace each.
     *
     * <p><b>This replaced a half-dynamic list that failed silently.</b> The previous
     * shape globbed each corpus's own directory ({@code ../test-corpus/cdm}) so a new
     * cell auto-loaded, while HARDCODING the transitive closure per version so the new
     * cell's dependencies did NOT. When cdm 6.21.0 and drr 7.3.0 landed they were
     * loaded without rune-fpml 2.1.1 and cdm 6.21.0 respectively, producing
     * ENUM_NOT_FOUND=1143 / SYMBOL_NOT_FOUND=143 / TYPE_ERROR=398 — thousands of
     * unresolved references that read exactly like engine defects and were nothing of
     * the kind.
     *
     * <p><b>Closures are per CELL, never per corpus.</b> cdm 6.20.6 pins rune-fpml
     * 1.5.3 while cdm 6.21.0 pins 2.1.1, so "the cdm closure" does not exist: merging
     * them would put two definitions of every fpml type in one space, and picking one
     * would starve the cell pinning the other. One workspace per cell also mirrors how
     * upstream actually builds each release, which is what the budgets are measured
     * against.
     */
    private static List<CorpusCatalogue.Cell> gatedCells() {
        return CorpusCatalogue.activeCatalogue().stream()
                .filter(c -> GATED_CORPORA.contains(c.corpus()))
                .toList();
    }

    /**
     * PR #445 — per-corpus CELL roots for {@link RWorkspace#build(List, List)}:
     * one entry per upstream build closure inside the merged population, at
     * the FINEST closure granularity (the cdm corpus's shared load root
     * splits into its two cells here — upstream builds cdm-5.38.0 and
     * cdm-6.20.6 separately, and the same-file/same-cell resolution
     * preference emulates exactly that isolation inside the merged space; see
     * {@link CellPreference}). The drr entries mirror its load roots — that
     * population is already one-closure-per-root.
     */
    // CELL_ROOTS is gone. It existed to emulate per-build-closure isolation INSIDE a
    // merged per-corpus workspace, via CellPreference's same-cell resolution
    // preference. Now that each cell gets its own workspace, the load roots ARE the
    // cell roots — builtins, the resolved closure, then the cell's own tree — so the
    // isolation is structural rather than emulated, and there is no second list to
    // keep in step. See CorpusCatalogue#loadRoots.

    /**
     * Per-corpus linking ERROR budgets by category. Shrink-only; delete rows at
     * zero.
     *
     * <p><b>The PR #443 population re-seed (the #4-class-(c) decision):</b>
     * against the upstream build closure, entire categories DISSOLVED as pure
     * population artifacts — cdm: TYPE_NOT_FOUND 3,489 / ANNOTATION_NOT_FOUND
     * 1,233 / IMPORT_UNRESOLVED 214 all → 0 (builtins + fpml), SYMBOL 449 → 310,
     * ENUM 5,942 → 5,883; drr: TYPE 5,056 / ANNOTATION 130 / IMPORT 462 /
     * SUPER_TYPE 1 / SEGMENT_DEFINITION 13 all → 0, SYMBOL 1,852 → 133 (Abs =
     * cdm.base.math resolved), ENUM 13,131 → 10,457. The re-seed triage probe
     * proved ZERO fabrications: every cdm row is own-file; drr's new rows
     * (DISPATCH_PARAM 28, EXTERNAL_SOURCE 3, EXTERNAL_TYPE 1, SYMBOL 43 of the
     * 133, ENUM 1,800 cdm-closure + 3,269 iso-closure of the 10,457, ENUM_VALUE
     * 20 of the 57) are the closure models' OWN diagnostics now counted in the
     * union space — exactly what upstream's mojo validates in the real union
     * build — and every drr own-file count moved DOWN (ENUM own 5,388, SYMBOL
     * own 90, ENUM_VALUE own 37, RULE own 3).
     *
     * <p><b>The #4-class-(a) wave (same PR, the ratchet after the re-seed):</b>
     * the mis-interpretation family dies. The channel census matched every
     * ENUM row 1:1 to a disguised-nav {@code REnumValueRef} and split it:
     * inputFeature-bound-but-diagnosed (3,598 cdm + 5,418 drr — the engine's
     * function-input channel binds but never cleared, the 2026-06-27 channel's
     * documented follow-on) and lexically-resolvable-but-unbound (alias heads
     * 964 + 765; data-type-condition heads, the dominant "other" bucket 1,082
     * + 4,127). The wave adds the clearing to the input channel, a new
     * TYPING-ONLY condition-context channel with the same clearing, and a
     * CLEARING-ONLY alias-head arm — clearing-only because exposing
     * typed-alias navs moved 2 drr FUNCTION goldens off byte-parity at this
     * PR's cp gate (the generator's meta-coercion placement shifts when a
     * `<alias> -> <meta leaf>` arm's type becomes known, where upstream's own
     * fully-typed pipeline emits the golden placement — a latent
     * generator-seat divergence recorded for the typed-alias-nav follow-on
     * wave; the witness carriers: drr ExtractReferenceEntity + hkma
     * Extract_ReferenceEntityFormat). Measured: cdm ENUM 5,883 → 687, drr
     * ENUM 10,457 → 349 (the closure models' own rows die the same way);
     * SYMBOL stays (the alias-TYPING cascade that would fire Cat 9's clearing
     * is deferred with the bind). The residual rows (ENUM 687/349 · SYMBOL
     * 310/133 · the small categories) are the NEXT census's subject —
     * genuinely-unresolvable names, chained-alias shapes awaiting the typing
     * wave, and still-uncovered head shapes (record-type leaves, output
     * heads).
     *
     * <p><b>The PR #444 residual waves (four mechanisms, one census):</b>
     * (1) DISPATCH-BASE INHERITANCE — a dispatch variant ({@code func
     * Foo(param Enum -> VALUE):}) resolves its body against the BASE
     * {@code func Foo:} signature (upstream getSymbolParentScope builds every
     * Function scope from the base-aware getInputs/getOutput; the base =
     * same-file first-in-document-order operations-empty sibling, upstream's
     * own cross-file TODO quirk preserved — {@code RFunction.dispatchBase()}):
     * the variant scope registers base inputs + output
     * (LexicalResolutionPass), healing DISPATCH_PARAM cdm 58 → 2 / drr 28 → 0
     * (row DELETED; the cdm 2 = the cells' own {@code tests/} fixture
     * {@code TestFunc2(directionEnum: ...)} whose base genuinely lacks the
     * param — upstream diagnoses it too) and SYMBOL cdm 310 → 252 / drr 133 →
     * 104 (bare base-input references — {@code DateDifference(startDate,
     * endDate)} in the daycount variants), and the engine's input-channel walk
     * consults the base (TypeInferenceEngine.enclosingFunctionInput), killing
     * the dispatchInput ENUM rows. (2) RECORD-MEMBER LEAVES, CLEARING-ONLY —
     * {@code endDate -> year} / {@code reportingTimestamp -> date} navs whose
     * head types as a builtin RECORD and whose leaf is one of its features
     * (day/month/year · date/time/timezone): record features are not
     * RAttributes, so the input/alias/condition channels' attribute lookup
     * could never verify them; a membership test
     * (TypeDirectedResolver.isRecordFeatureOnType) now clears the stale
     * tried-enum diagnostics, binding NOTHING (the #443 typing-exposure law).
     * (3) OUTPUT-HEADED NAVS, CLEARING-ONLY — {@code <output> -> <feature>}
     * where the leaf resolves on the output's type (23 cdm + 15 drr, every
     * one leaf-resolving at the census). (4) INHERITED ENUM VALUES —
     * upstream scopes enum-value references against getAllEnumValues (own +
     * inherited); the fork's global pass read own values only, so DRR's
     * {@code PartyIdentifierFormatEnum -> Lei} (declared two supertype levels
     * up: extends PartyIdentifierFormat2Enum extends LeiIdentifierFormatEnum)
     * mass-failed — the pass-safe supertype walk
     * (GlobalResolutionPass.superEnumOf, each hop resolved in its declaring
     * file's scope) kills ENUM_VALUE drr 57 → 0 (row DELETED). Measured
     * totals: cdm 1,071 → 827 · drr 574 → 293 (ENUM cdm 687 → 557 / drr 349
     * → 182 — the census's 66+33 dispatch record navs, 32+16 dispatch
     * attribute navs, 1+97 plain record navs, 23+15 output navs, plus a
     * 8+6-row cascade where variant alias expressions type once their symbols
     * resolve and the #443 alias arm then clears their navs); validation
     * tables byte-unchanged (cdm 2 · drr EMPTY — the waves armed NOTHING).
     *
     * <p><b>The PR #445 same-cell preference wave:</b> the merged cdm
     * population's cross-cell mis-resolution family dies. The #445 census
     * traced the cdm {@code leafMissOn} residual mass to one DOMINANT
     * mechanism — ~245 of ~250 rows with a measured same-cell heal:
     * cdm-6.20.6 references resolving shared names into the 5.38.0 cell
     * (first-registered wins), with a 6.20.6 declaration carrying the leaf
     * ({@code product -> economicTerms} ×92, the {@code Payout}/{@code
     * Underlier}/{@code Observable} choice-option navs, {@code
     * Trade.tradeLot}…; 4 heal=none rows — incl the 2-row {@code reference}
     * metadata face — are distinct mechanisms that stayed in the residue) —
     * plus the namespace-LAYOUT drift face (CDM 6 moved
     * types like {@code PriceQuantity} across namespaces, so a c5 reference
     * could bind a c6-only namespace through an earlier import). The fix
     * ({@link CellPreference} + the {@link RWorkspace#build(List, List)}
     * cell roots this gate now passes): every resolution seat prefers
     * same-file, then same-cell candidates, with the historical
     * registration-order walk as the cross-cell fallback — emulating exactly
     * the per-closure isolation upstream's builds have natively. Three
     * follow-on faces measured at the seed and fixed in the same wave: the
     * external-class kind gate now admits CHOICE declarations (CDM 6's
     * restructured Payout/Product/Underlier/Asset carry external synonyms —
     * upstream's choice-as-data scope admits them) and re-resolves
     * function-shadowed names at its type position (CalculationPeriod /
     * ReturnAmount / DeliveryAmount — the resolveTypeCall shadowing class;
     * BOTH corpus EXTERNAL_TYPE rows DELETE, cdm 9 → 0 and drr 1 → 0), and
     * the faithful (thenAware) cardinality path takes upstream's
     * deep-feature rule (isFeatureMulti || isMulti(receiver)) so the c6
     * {@code instrument ->> instrumentType} carriers stopped arming the
     * all-any check. Measured: cdm 827 → 536 (ENUM 557 → 295 · SYMBOL 252 →
     * 232 · EXTERNAL_TYPE row DELETED) · drr 293 → 292 (EXTERNAL_TYPE row
     * DELETED; everything else byte-identical — drr has no cell collision);
     * the cdm validation MISSING_ATTRIBUTE topology row DELETES (the 6.20.6
     * EligibleCollateralCriteria extends now resolves inside its own cell).
     *
     * <p><b>The PR #446 choice-option nav wave:</b> the census's dominant
     * residual block dies. A {@code <head> -> <OptionName>} nav whose head
     * types as a CHOICE and whose leaf names one of the choice's OPTIONS
     * ({@code payout -> InterestRatePayout} · {@code underlier -> Product} ·
     * {@code observable -> Asset}) is plain attribute navigation upstream —
     * a choice's feature scope IS its options-as-attributes
     * ({@code RChoiceType.asRDataType()}: {@code ChoiceOption extends
     * Attribute}, name = the written type-reference text, cardinality
     * (0..1)) — but the fork's {@code findAttributeOnType} applies the
     * {@code ->>} common-attribute rule on choices and can never admit an
     * option. The #446 mechanism probe verified the fix per-member BEFORE
     * design: 140 of the 295 residual cdm ENUM rows admit under upstream's
     * option-membership rule — condition-context 66 (Payout 36 · Underlier
     * 20 · Asset 4 · Observable 3 · RateSpecification 2 · InterestRateIndex
     * 1) + input-headed 29 + alias-headed 45 (Underlier 30 · Payout 15) —
     * with ZERO non-admitting choice-headed rows and ZERO drr carriers (the
     * family is cdm-only). The fix
     * ({@code TypeDirectedResolver.isChoiceOptionOnType} consulted at the
     * four engine channels — input / alias / output / condition) is
     * CLEARING-ONLY per the #444 record-member contract: the engine's Phase
     * X1 T10 arms bind {@code resolvedChoiceOption} in ITEM-headed contexts
     * only, and the bind at these channels (narrowed-type exposure,
     * generator-visible) is deferred to the typed-alias follow-on wave.
     * Measured: cdm ENUM 295 → 155; everything else byte-identical (the
     * residue: alias/headTypeMissing 101 · lambda noHead 51 · the 2-row
     * reference-metadata face · 1 basic-type alias leaf).
     *
     * <p><b>The PR #447 typed-alias-nav wave (BIND-MODE):</b> the #443 alias
     * arm flips clearing-only → BIND ({@code resolvedInputFeature}, exactly
     * like the input channel), so an alias-headed nav TYPES as its leaf
     * attribute and CHAINED aliases converge in the fixed-point iteration —
     * the census's alias/headTypeMissing block (101 cdm + 84 drr) dies, the
     * NOTHING-typed alias leaves re-type, and the newly-typed pipe chains
     * cascade through the existing ITEM-headed T10 and Category-13/14
     * expected-type arms (the lambda ENUM noHead and SYMBOL families the
     * #443 bind-mode probe measured −22/−58). Landed WITH the generator's
     * meta-coercion seat aligned per the TYPING-EXPOSURE law (the F-β
     * conditional-base ladder's all-arms-agree wrapper join now runs on
     * typed bases too — the engine's {@code getInferredType(base).type()}
     * read strips the meta annotation, so the walk decides; and the
     * resolved-comparand child-requalifier gained the #391 pipe-item rung —
     * both #443 witnesses, ExtractReferenceEntity + hkma
     * Extract_ReferenceEntityFormat, byte-identical at the cp gate).
     * Measured: cdm ENUM 155 → 59, SYMBOL 232 → 185; drr ENUM 182 → 11,
     * SYMBOL 104 → 46; validation budgets BYTE-UNCHANGED (cdm 1 · drr
     * EMPTY) — the thousands of newly-typed alias expressions armed
     * NOTHING.
     *
     * <p><b>The PR #448 bare-enum-value expected-type wave (Cat 16,
     * CLEARING-ONLY):</b> the census's dominant residual block dies. A bare
     * name in an expected-ENUM position — a constructor value ({@code style:
     * European}), a function-call argument ({@code MapAncillaryParty(..,
     * DeterminingParty)}), a conditional arm / list element / switch-case
     * result / transparent-unary chain / map-then-reduce lambda body the
     * expected type flows through, or a {@code default} right-hand side — is
     * admitted by upstream's expected-type scoping
     * ({@code ExpectedTypeProvider.getExpectedTypeFromContainer}'s recursion +
     * {@code RosettaScopeProvider}'s implicit enum-value features, parent-wins
     * {@code ReversedSimpleScope}). The #448 census classified the family
     * per-member BEFORE design (probe: 127 cdm + 45 drr enum-value-candidate
     * rows across positions ctorValue 64 · callArg 84 · conditional 19 ·
     * switchCase 2 · eq/default 3); the Cat 16 arm
     * ({@code expectedEnumViaPosition}, the pruned port of upstream's
     * recursion) admits per-POSITION and clears WITHOUT binding — the #448
     * bind-mode probe measured the bind moving SIX cdm-6.20.6 FUNCTION
     * goldens through two generator seats (the elseless-conditional hoist
     * gate + the super-enum-declared value qualifying by its declaring
     * PARENT where golden qualifies by the expected CHILD, the #211/#358
     * flatten law; dump banked {@code target/448-bindmode-dump/}), so the
     * bind is the recorded typed follow-on per the #447 precedent. Measured:
     * cdm SYMBOL 185 → 59 · drr SYMBOL 46 → 2, everything else
     * BYTE-IDENTICAL; validation budgets BYTE-UNCHANGED (cdm 1 · drr EMPTY)
     * — the wave armed NOTHING. The residue is per-member classified: the 58
     * cdm + 1 drr candidate-free rows (the cells' own {@code tests/}
     * fixtures + names no corpus enum declares — upstream diagnoses them
     * too) and TWO {@code =} right-operand rows whose LEFT sibling does not
     * yet TYPE as its enum (Cat 14's seat — the typed-alias/bind follow-on's
     * cascade class, not a Cat 16 miss).
     *
     * <p><b>The PR #449 clearing-to-BIND wave (choice-option + output-head
     * BINDs):</b> the #446 choice-option arms (input / alias / output /
     * condition channels) and the #444 output-head arm flip clearing-only →
     * BIND: a choice-option nav binds {@code resolvedChoiceOption} and types
     * as the matched option; an output-headed nav binds
     * {@code resolvedInputFeature} and types as its leaf — so aliases whose
     * BODIES are those navs finally TYPE, and their consumer navs bind via
     * the #447 alias arm in the same fixed point. The #449 census decoded the
     * residual alias:MISSING family as EXACTLY this cascade (the alias HEAD
     * matched but the body was cleared-not-typed: cdm5's output-bodied
     * {@code alias payout} × 6 rows both corpora + cdm6's choice-bodied
     * payout-option aliases × 19 rows); the live-experiment probe (the #447
     * form) measured the bind at the seed — cdm ENUM 59 → 34 · SYMBOL 59 →
     * 55 (the eq.R {@code Commodity} cascade + THREE ctor-value rows the
     * #448 census had misclassified as candidate-free floor, actually
     * cascade-blocked implicit-feature reads) · drr ENUM 11 → 5 — and the
     * full 55-param cp BYTE-EQUAL to the #437 SOT (ZERO generator movement:
     * unlike the #447 alias bind, these binds need NO generator seat).
     * Validation budgets BYTE-UNCHANGED (cdm 1 · drr EMPTY) — the wave armed
     * NOTHING. The residue: cdm ENUM 34 = noHead 31 (switch-narrowed item
     * features + lambda item/meta faces) + the reference-metadata face 2 +
     * alias:nothing 1; cdm SYMBOL 55 = the candidate-free floor; drr ENUM 5
     * = noHead 4 + the reference face 1; drr SYMBOL 2 = the eq.L floor row +
     * the eq.R rule-chain cascade (its LEFT is an elseless-conditional
     * extract chain, not a choice/output nav — correctly untouched).
     *
     * <p><b>The PR #450 decoded-fork-gaps wave</b> retired the two remaining
     * REAL fork-gap categories the #449 census had decoded (both linker
     * scoping, both mechanism-verified by a live workspace probe BEFORE
     * design): <b>EXTERNAL_SOURCE_NOT_FOUND (cdm 7 → 0 + drr 3 → 0, both
     * rows DELETED)</b> — upstream's extends cross-ref on an external
     * synonym source is typed {@code [RosettaSynonymSource|QualifiedName]}
     * and {@code RosettaExternalSynonymSource} EXTENDS
     * {@code RosettaSynonymSource} in the Ecore model, so the plain
     * body-less legacy form is an admissible target; the fork's plain form
     * registered fine (the recorded "not registered at all" hypothesis was
     * REFUTED by the probe) but the admission required the external form
     * only — every failing row was exactly an extends ref whose target is
     * the PLAIN form (FIS_BASE / FpML / ORE / CreateiQ;
     * {@code RExternalSynonymSource} now extends {@code RSynonymSource},
     * the upstream hierarchy mirror, and the admission is the supertype —
     * and <b>RULE_NOT_FOUND (drr 3 → 0, row DELETED)</b> — a ruleReference
     * position scopes to the {@code RosettaRule} ECLASS only, and the iosco
     * {@code payment.OtherPayment} witness hit the kind-blind wildcard
     * walk's FIRST namespace ({@code ...cde.base.payment}, holding only the
     * same-named TYPE — no rule of that name exists there) before the
     * version namespace holding the RULE; the rule-kind-filtered ladder
     * ({@code resolveQualifiedOrLocalOfKind}, the byte-untouched sibling
     * shape) empties the first hit and binds upstream's version-matched
     * target. Measured: healed EXACTLY the 13 banked rows / new ZERO / the
     * 98 survivors byte-identical; the full 55-param cp BYTE-EQUAL to the
     * #437 SOT (zero generator movement — the {@code ann.rule()} generator
     * consumers measured inert for the 3 new bindings); validation budgets
     * BYTE-UNCHANGED (cdm 1 · drr EMPTY) — the wave armed NOTHING. The
     * residue is now floor + decoded-typing families ONLY: cdm 91 =
     * DISPATCH 2 (fixture floor) + ENUM 34 (noHead 31 · reference-metadata
     * face 2 · alias:nothing 1) + SYMBOL 55 (the candidate-free floor);
     * drr 7 = ENUM 5 (noHead 4 · face 1) + SYMBOL 2 (eq.L floor · eq.R
     * rule-chain cascade).
     *
     * <p><b>The PR #451 noHead-families wave</b> retired the decoded-typing
     * residue in one sweep — the per-row source census + a live AST probe at
     * the banked #450 witnesses split the 88 rows into FOUR mechanisms
     * BEFORE design, and all four landed together: <b>(1) switch-case item
     * narrowing</b> — a non-default case with a data/choice-option guard
     * DEFINES the implicit variable as the GUARD's type (upstream
     * {@code ImplicitVariableUtil.findContainerDefiningImplicitVariable} +
     * {@code safeTypeOfImplicitVariable}), so bare refs and disguised navs
     * inside {@code fpmlProduct switch fpml.CreditDefaultSwap then ...}
     * resolve as features of the narrowed type (the engine's
     * {@code narrowedSwitchCaseItemType} walk, consulted first by
     * {@code getEnclosingItemType} + Cat 8's literal-item branch; NAME
     * guards resolve speculatively at the linker via the kind-gated ladder
     * — {@code GlobalResolutionPass.resolveSwitchGuardType}, type-like
     * nodes only — and per-subject at Cat 4: enum subjects keep values,
     * choice subjects store the matched OPTION's type node); <b>(2)
     * record-member leaves</b> at the item-headed 2-step
     * ({@code stepDate -> date} on zonedDateTime item features — the #444
     * clearing contract extended to the Cat-10 chain); <b>(3)
     * metadata-face leaves</b> ({@code partyReference -> reference} on a
     * {@code [metadata reference]} attribute, {@code identifier -> scheme}
     * on {@code [metadata scheme]} — upstream's
     * {@code getMetaDescriptions} feature admission, clearing-only via
     * {@code isMetaFaceOnAttribute} at the 2-step + the condition
     * channel); <b>(4) choice options as item features</b> —
     * {@code payouts extract InterestRatePayout exists} (bare, Cat 9
     * clearing-only) and {@code Asset -> Commodity} on a
     * {@code type BasketConstituent extends Observable} item (the
     * choice-VIEW widening: a data type extending a choice inherits its
     * options-as-attributes upstream, so {@code choiceViewOfType} feeds
     * the L10 option-of-option arm + the widened membership helpers).
     * Measured at the seed: healed EXACTLY 88 / new ZERO / the 10
     * survivors byte-identical — <b>cdm 91 → 8</b> (ENUM_NOT_FOUND 34 → 0,
     * row DELETED; SYMBOL_NOT_FOUND 55 → 6 = the tests/ fixture floor
     * exactly, 3 rows × 2 cells) and <b>drr 7 → 2</b> (ENUM_NOT_FOUND
     * 5 → 0, row DELETED; the max-lambda {@code timestamp -> date} rows
     * healed through the record rung, so the func-call-headed max argument
     * types). Validation budgets BYTE-UNCHANGED (cdm 1 · drr EMPTY) — the
     * wave armed NOTHING. The residue is the pure fixture/eq floor: cdm 8
     * = DISPATCH 2 + SYMBOL 6 (test-func.rosetta ×2 cells); drr 2 =
     * SYMBOL 2 (the eq.L floor row + the eq.R rule-chain cascade).
     *
     * <p><b>The PR #452 Cat-16 BIND wave (zero budget movement — the tables
     * above are byte-unchanged):</b> the #448 bare-enum-value expected-type
     * arm flips clearing-only → BIND (the recorded typed follow-on), landed
     * WITH its two #448-measured generator seats (the getOrDefault-arg hoist
     * gate's bound-arm admission + the ctor-value/default-RHS child
     * requalification — facet cat16BindEnumSeats, the #215 same-instance
     * law). Measured at the seed: budgets IDENTICAL (the #448 record's
     * no-cascade-dependency prediction held), the row-set diff vs the banked
     * #451 census = healed ZERO / new ZERO / the 10 survivors
     * byte-identical, and the dumping 55-param cp BYTE-EQUAL to the #437
     * SOT (the 6 banked #448 divergence carriers all healed by the seats).
     */
    // ---------------------------------------------------------------------------
    // 2026-08-14: RE-KEYED FROM PER-CORPUS TO PER-CELL.
    //
    // The pre-expansion tables were keyed "cdm" and "drr" over MERGED workspaces
    // (cdm 5.38.0 + 6.20.6 in one space; both drr cells in another). Cells of the
    // same corpus pin different dependency versions, so a merged space cannot be
    // built correctly once a corpus carries more than one closure — see gatedCells().
    // Each cell now gets its own workspace and its own budget row.
    //
    // The superseded per-corpus values, for the record:
    //   cdm  DISPATCH_PARAM_NOT_FOUND=2, SYMBOL_NOT_FOUND=6
    //        (total 8 after the PR #451 noHead-families wave: ENUM_NOT_FOUND 34 -> 0
    //         row DELETED, SYMBOL_NOT_FOUND 55 -> 6 = the fixture floor)
    //   drr  EMPTY since the PR #458 eq-row wave (SYMBOL_NOT_FOUND 2 -> 0, row
    //        DELETED — the #448-recorded eq rows healed by the two pass-6 clearing
    //        arms: the implicit-item META feature + the equality-sibling expected
    //        enum value; the mojo-seam swap proof measured them as the ONLY drr
    //        linking over-fires, and the literal DRR pom swap builds green on the
    //        fork plugin's failOnValidationError default). The drr linking-ERROR
    //        surface is ZERO on valid models.
    //
    // Shrink-only still applies PER ROW: ratchet in the same commit as the wave that
    // moved it, and delete rows at zero.
    // ---------------------------------------------------------------------------
    // ---------------------------------------------------------------------------
    // v3.1 CLOSE-OUT (2026-08-28) — SEEDED FOR ALL TWENTY GATED CELLS from one
    // -Dcorpus.diag.print=true -Dcorpus.diag.detail=true run at the #605 head.
    // The parser's standing 3F was this test's two rows: the 2026-08-14 per-cell
    // re-key keyed FIVE cells with EMPTY tables — four of them non-empty on the
    // linking side and three on the validation side when measured (drr 6.34.1
    // is genuinely empty on both) — and left FIFTEEN gated cells unkeyed
    // (gatedCells() = the active catalogue filtered to cdm + drr = 20), which
    // the requireNonNull in the assertion would have NPE'd on had the FIRST
    // keyed cell's assertEquals not fired first (the seed run's failure was
    // that assertion, not an NPE). Every row below was READ from the detail
    // lines, not just counted — and two of them are SEED rows, not ratchets:
    // IMPORT_UNRESOLVED 2 on cdm 6.21.0+/drr 7.x enters the tables here at its
    // measured value (the shrink-only contract binds movement AFTER a seed):
    //   * every cdm cell — the cell's OWN tests/ fixture
    //     (tests/src/test/resources/rosetta/test-func.rosetta, a file upstream's
    //     mojo never compiles): SYMBOL_NOT_FOUND 3 (directionEnum :17, startDate
    //     :19, endDate :19) + DISPATCH_PARAM_NOT_FOUND 1 (directionEnum :15) —
    //     the honest fixture floor the superseded per-corpus values above
    //     recorded as 6 + 2 over the MERGED two-cell corpus;
    //   * cdm 6.21.0 / 6.22.0 / 6.23.0 and, through their cdm 6.21.0 closure,
    //     drr 7.0.0–7.3.0 — IMPORT_UNRESOLVED 2: upstream's own
    //     `import cdm.base.staticdata.codelist.*` in
    //     ingest-fpml-confirmation-datetime-func.rosetta:5 and
    //     ingest-fpml-confirmation-other-func.rosetta:10, a namespace no file in
    //     the closure declares (the #567 upstream-own IMPORT refusal — the fork's
    //     verdict is byte-for-byte upstream's on these cells);
    //   * drr 5.61.0 – 6.38.0 — ZERO: the drr linking-ERROR surface is zero on
    //     valid models, exactly as the #458 note above records.
    // Shrink-only per row, delete rows at zero; a 26th cdm or drr catalogue cell
    // (GATED_CORPORA — iso20022 and rune-fpml are not gated here) adds its own
    // entry — the requireNonNull in the assertion is deliberate (an unkeyed
    // cell is a loud failure, never a silent pass).
    // ---------------------------------------------------------------------------
    /** The cdm cells' own tests/ fixture floor (linking half) — see the note above. */
    private static final Map<DiagnosticCategory, Integer> CDM_FIXTURE_LINKING_FLOOR = Map.of(
            DiagnosticCategory.DISPATCH_PARAM_NOT_FOUND, 1,
            DiagnosticCategory.SYMBOL_NOT_FOUND, 3);

    /** The fixture floor plus the two upstream-own unresolved imports of cdm 6.21.0+. */
    private static final Map<DiagnosticCategory, Integer> CDM_621_LINKING_FLOOR = Map.of(
            DiagnosticCategory.DISPATCH_PARAM_NOT_FOUND, 1,
            DiagnosticCategory.IMPORT_UNRESOLVED, 2,
            DiagnosticCategory.SYMBOL_NOT_FOUND, 3);

    /** drr 7.x inherits ONLY the two unresolved imports through its cdm 6.21.0 closure. */
    private static final Map<DiagnosticCategory, Integer> DRR7_LINKING_FLOOR = Map.of(
            DiagnosticCategory.IMPORT_UNRESOLVED, 2);

    private static final Map<String, Map<DiagnosticCategory, Integer>> LINKING_ERROR_BUDGETS =
            Map.ofEntries(
                    Map.entry("cdm-5.38.0", CDM_FIXTURE_LINKING_FLOOR),
                    Map.entry("cdm-5.39.0", CDM_FIXTURE_LINKING_FLOOR),
                    Map.entry("cdm-6.20.2", CDM_FIXTURE_LINKING_FLOOR),
                    Map.entry("cdm-6.20.3", CDM_FIXTURE_LINKING_FLOOR),
                    Map.entry("cdm-6.20.4", CDM_FIXTURE_LINKING_FLOOR),
                    Map.entry("cdm-6.20.5", CDM_FIXTURE_LINKING_FLOOR),
                    Map.entry("cdm-6.20.6", CDM_FIXTURE_LINKING_FLOOR),
                    Map.entry("cdm-6.21.0", CDM_621_LINKING_FLOOR),
                    Map.entry("cdm-6.22.0", CDM_621_LINKING_FLOOR),
                    Map.entry("cdm-6.23.0", CDM_621_LINKING_FLOOR),
                    Map.entry("drr-5.61.0", Map.of()),
                    Map.entry("drr-6.34.1", Map.of()),
                    Map.entry("drr-6.35.0", Map.of()),
                    Map.entry("drr-6.36.0", Map.of()),
                    Map.entry("drr-6.37.0", Map.of()),
                    Map.entry("drr-6.38.0", Map.of()),
                    Map.entry("drr-7.0.0", DRR7_LINKING_FLOOR),
                    Map.entry("drr-7.1.0", DRR7_LINKING_FLOOR),
                    Map.entry("drr-7.2.0", DRR7_LINKING_FLOOR),
                    Map.entry("drr-7.3.0", DRR7_LINKING_FLOOR));

    /**
     * Per-corpus validation ERROR budgets by issue code. Shrink-only; delete
     * rows at zero.
     *
     * <p>The PR #442 Annex-B(B-1) wave EMPTIED the drr table (175 → 0: the
     * 171 TYPE_ERROR were all attribute overrides the pre-#442 seat compared
     * by raw type-call NAME STRING — upstream's subtype-based
     * checkAttributeOverride admits every one as a legal restriction; the 4
     * DUPLICATE_ELEMENT_NAME came from the retired fork-native
     * duplicate-condition-name check, which has NO upstream analogue). The
     * cdm MISSING_ATTRIBUTE 1 was a MERGED-CORPUS TOPOLOGY ARTIFACT, not a
     * validator defect: this population loads BOTH cdm cells into one symbol
     * space, and the 6.20.6 {@code EligibleCollateralCriteria}'s {@code extends
     * CollateralCriteriaBase} resolved to the 5.38.0 cell's same-FQN type,
     * which genuinely lacks {@code collateralCriteria} — the row HEALED at
     * PR #445 exactly as recorded: the {@link CellPreference} same-cell
     * resolution preference resolves the extends inside the 6.20.6 cell,
     * emulating upstream's per-cell build (row DELETED).
     *
     * <p><b>The PR #443 population re-seed armed one previously-declining
     * check:</b> the {@code =}/{@code <>} all-any cardinality check (the #437
     * listEqualsCardinalityModifier facet) declines while either operand's
     * type is MISSING; the closure resolved two carriers' types and the check
     * fired ERROR where the V0 oracles show upstream SILENT (zero matches for
     * the message in either bank) — both measured fork-native false positives:
     * (1) drr {@code regulation-common-trade-payment-func.rosetta:94}
     * {@code [ISOCurrencyCodeEnum -> …] <> otherPayment -> currency} — the
     * right side parses as a disguised-chain REnumValueRef resolved through
     * the {@code resolvedInputFeature} channel, and
     * {@code CardinalityComputer.disguisedChainCardinality}'s inputFeature arm
     * read the LEAF only (currency 0..1 → SINGLE), blind to the
     * {@code otherPayment (1..*)} head upstream ORs in
     * ({@code isFeatureMulti || isMulti(receiver)} → both sides multi →
     * silent) — HEALED in the same PR by the class-(a) wave's head-aware arm
     * ({@code lexicalHeadCardinality}), the drr table EMPTY again; (2) cdm
     * {@code ingest-fpml-confirmation-workflowstep-func.rosetta:471}
     * {@code alias intentToAllocate: partyTradeInformation extract
     * intentToAllocate = True} — the bare name inside the extract body
     * resolved to the enclosing ALIAS itself (RShortcut self-reference, multi
     * via its extract-over-multi expression) where upstream's lexical scoping
     * binds the implicit ITEM's feature (fpml intentToAllocate, single →
     * silent) — HEALED at PR #453, the alias-self scoping-precedence wave:
     * upstream's {@code getSymbolParentScope} FILTERS an alias's own name
     * from its body's symbol scope (a ShortcutDeclaration removes every
     * same-named descr from its parent scope, vendored
     * RosettaScopeProvider:401-402; lambda params sit BELOW the boundary and
     * still shadow) and filters the function OUTPUT from non-post-condition
     * bodies (:405-406 — feature-identity-based, post-conditions keep it).
     * {@code LexicalResolutionPass} now applies both filters at the pass-5
     * lookup (facet aliasSelfScopeFilter); the #442-law collision census
     * measured the whole blast radius — 4 alias-self carriers (this one + 3
     * silent drr {@code reportingSide} sites), 0 pre-condition output refs, 7
     * post-condition output refs (which keep binding) — and every carrier
     * re-binds to the implicit item's attribute through the engine's existing
     * Cat 9 arm, exactly like its already-clearing sibling branches: the
     * probe measured the linking tables IDENTICAL (this wave's whole linking
     * row-set = the 10 banked survivors byte-for-byte) and THIS row's carrier
     * silent, EMPTYING the table. The generator agrees byte-identically after
     * its {@code isAliasReference} name-fallback learned the same exclusion
     * (the probe's round 1 caught the 4 carriers rendering self-CALLS once
     * the self-BIND — which the #372 aliasSelfShadowItemFeature arm had been
     * translating to the golden item-feature nav — stopped arriving; round 2
     * = 55/55 byte-equal to the #437 SOT). BOTH validation tables are now
     * EMPTY — the Leg V validation ERROR board reads all-zero for the first
     * time; the V0 oracle streams (0 errors both corpora) are fully matched
     * on the ERROR surface.
     */
    /** The cdm cells' own tests/ fixture floor (validation half) — see the note inside the table. */
    private static final Map<ValidationIssueCode, Integer> CDM_FIXTURE_VALIDATION_FLOOR = Map.of(
            ValidationIssueCode.TYPE_ERROR, 1);

    private static final Map<String, Map<ValidationIssueCode, Integer>> VALIDATION_ERROR_BUDGETS =
            Map.ofEntries(
                    // The #454 Annex-A wave's HONEST FIXTURE FLOOR — the exact
                    // validation analogue of the linking tables' fixture floor
                    // (8 over the pre-seed merged corpus; 3 + 1 per cell since): the
                    // cells' own tests/ fixture (test-func.rosetta ×2 cells, the
                    // SAME file carrying the SYMBOL_NOT_FOUND 6) calls the
                    // one-input TestFunc1 with two unresolvable arguments
                    // (`TestFunc1(startDate, endDate)`), and the new upstream
                    // call-site arity check honestly diagnoses `Expected 1
                    // argument, but got 2 instead` there. Upstream's own builds
                    // never see the file (tests/src/test/resources is not a mojo
                    // source root), so the V0 oracle's zero-error contract is
                    // untouched: the ERROR surface on the models upstream
                    // validates (src/main/rosetta closures) is still ZERO both
                    // corpora — the board is all-zero ON VALID MODELS with the
                    // fixture floor standing, exactly like the linking board.
                    // 2026-08-14: RE-KEYED PER CELL (see LINKING_ERROR_BUDGETS).
                    // Superseded per-corpus values, for the record:
                    //   cdm  TYPE_ERROR=2  (the #454 Annex-A fixture floor above)
                    //   drr  EMPTY — the #443 head-aware arm healed the armed carrier;
                    //        ZERO held through the #454 Annex-A wave (the probe's first
                    //        round measured 3,013 arity false positives here — the
                    //        synthesized-input phantom + the missing lazy
                    //        implicit-argument admission — all healed pre-commit).
                    // v3.1 CLOSE-OUT (2026-08-28): seeded for all twenty gated cells from
                    // the same print as LINKING_ERROR_BUDGETS. Every cdm cell carries the
                    // #454 fixture floor at its PER-CELL size — TYPE_ERROR 1, the
                    // `TestFunc1(startDate, endDate)` arity diagnostic at
                    // tests/src/test/resources/rosetta/test-func.rosetta:19 ("Expected 1
                    // argument, but got 2 instead"; the merged corpus counted it twice);
                    // every drr cell is EMPTY. Shrink-only per row; delete rows at zero.
                    Map.entry("cdm-5.38.0", CDM_FIXTURE_VALIDATION_FLOOR),
                    Map.entry("cdm-5.39.0", CDM_FIXTURE_VALIDATION_FLOOR),
                    Map.entry("cdm-6.20.2", CDM_FIXTURE_VALIDATION_FLOOR),
                    Map.entry("cdm-6.20.3", CDM_FIXTURE_VALIDATION_FLOOR),
                    Map.entry("cdm-6.20.4", CDM_FIXTURE_VALIDATION_FLOOR),
                    Map.entry("cdm-6.20.5", CDM_FIXTURE_VALIDATION_FLOOR),
                    Map.entry("cdm-6.20.6", CDM_FIXTURE_VALIDATION_FLOOR),
                    Map.entry("cdm-6.21.0", CDM_FIXTURE_VALIDATION_FLOOR),
                    Map.entry("cdm-6.22.0", CDM_FIXTURE_VALIDATION_FLOOR),
                    Map.entry("cdm-6.23.0", CDM_FIXTURE_VALIDATION_FLOOR),
                    Map.entry("drr-5.61.0", Map.of()),
                    Map.entry("drr-6.34.1", Map.of()),
                    Map.entry("drr-6.35.0", Map.of()),
                    Map.entry("drr-6.36.0", Map.of()),
                    Map.entry("drr-6.37.0", Map.of()),
                    Map.entry("drr-6.38.0", Map.of()),
                    Map.entry("drr-7.0.0", Map.of()),
                    Map.entry("drr-7.1.0", Map.of()),
                    Map.entry("drr-7.2.0", Map.of()),
                    Map.entry("drr-7.3.0", Map.of()));

    /** The 26 active-catalogue cells the census measures, BY NAME (round 2, cq SF-4) — read from the census's own print at commit 6. */
    private static final Set<String> ACTIVE_CELLS = Set.of(
            "cdm-5.38.0", "cdm-5.39.0", "cdm-6.20.2", "cdm-6.20.3",
            "cdm-6.20.4", "cdm-6.20.5", "cdm-6.20.6", "cdm-6.21.0",
            "cdm-6.22.0", "cdm-6.23.0", "chaos-1.1.0", "drr-5.61.0",
            "drr-6.34.1", "drr-6.35.0", "drr-6.36.0", "drr-6.37.0",
            "drr-6.38.0", "drr-7.0.0", "drr-7.1.0", "drr-7.2.0",
            "drr-7.3.0", "iso20022-1.38.0", "rune-fpml-1.6.0", "rune-fpml-2.0.0",
            "rune-fpml-2.1.0", "rune-fpml-2.1.1");

    /**
     * v3.2 seat 4 (PR #625), round 1 — THE WARNING CENSUS of the seat's new family:
     * {@link ValidationIssueCode#QUALIFICATION_ROOT_MISMATCH} (upstream's "Input type does not match
     * qualification root type.", ported at seat 4 from ONE declaration with the meta generator's root)
     * counted per FILE over EVERY active-catalogue cell: the 20 cdm / drr cells this gate builds plus
     * iso20022, the four rune-fpml cells and the chaos cell, built the same way (26). The vendored cells
     * fire ZERO — the front page's per-site identity over the V0 warning banks is re-measured for the new
     * family here (no vendored model declares a losing qualifier). The chaos cell fires exactly the
     * released plugin's count: 20 occurrences over ELEVEN s22 files
     * ({@code target/chaos-expander/logs/l1-run13.log}, local — the chaos register's "8 files" was a
     * miscount, corrected at round 1 from the log), the variants whose own root lost the first-wins race.
     * Asserted as full-map equality: a new file, a lost file or a moved count all fail loudly. The chaos cell's
     * 22 {@code a5bom} files are skipped as the released plugin refuses them whole — the shared loader's skip
     * predicate ({@link SymbolTestCorpus#loadCorpus(Path, java.util.function.Predicate)}, ONE walk for every
     * caller — round 2, cq SF-3). Round 2 also pins the SET of cells, not their count ({@link #ACTIVE_CELLS},
     * cq SF-4 — a one-for-one cell swap kept the size at 26), and refuses two models sharing a basename in one
     * cell, since the per-file map is basename-keyed and would merge them (cq NIT-5).
     */
    @Test
    void qualificationRootWarning_censusOverEveryActiveCell_matchesTheReleasedPlugin() throws IOException {
        Map<String, Map<String, Integer>> perCell = new TreeMap<>();
        for (Map.Entry<String, RLinkingResult> e : buildAll().entrySet()) {
            assertBasenamesUnique(e.getKey(), e.getValue().workspace().files());
            perCell.put(e.getKey(), qualificationRootWarnings(e.getValue()));
        }
        for (CorpusCatalogue.Cell cell : CorpusCatalogue.activeCatalogue()) {
            if (GATED_CORPORA.contains(cell.corpus())) {
                continue;
            }
            List<Path> roots = CorpusCatalogue.loadRoots(cell);
            Path ownRoot = roots.get(roots.size() - 1);
            Assumptions.assumeTrue(Files.exists(ownRoot),
                    "cell absent: " + ownRoot + " — census skipped (CI / fresh clone)");
            List<RModel> files = new ArrayList<>();
            for (Path rootPath : roots) {
                files.addAll(SymbolTestCorpus.loadCorpus(rootPath, ChaosParseExpectations::isExpectedRefusal));
            }
            assertBasenamesUnique(cell.dirName(), files);
            perCell.put(cell.dirName(), qualificationRootWarnings(RWorkspace.build(files, roots)));
        }
        assertEquals(ACTIVE_CELLS, perCell.keySet(), "the 26 active-catalogue cells, by NAME (a swapped cell keeps the count)");
        assertEquals(26, perCell.size(), "every active-catalogue cell is measured: " + perCell.keySet());
        Map<String, Map<String, Integer>> expected = new TreeMap<>();
        for (String cell : perCell.keySet()) {
            expected.put(cell, Map.of());
        }
        // chaos-1.1.0 (v3.2 seat 10, D49): the s22 Product race UNMOVED (11 files, 20) and the
        // fresh s28 EVENT race (21 files, 42 — every s28 variant holding its two qualifiers
        // LOSES, because the A7 order-of-load axis's aaa-chaos-s28-a7first file WON: upstream's
        // first-wins follows the file name lexically — the axis's own finding, measured at
        // l1-s10-run3; zz-chaos-s28-a7last loses like the rest). 32 files / 62 occurrences,
        // from target/v32-seat10-instruments/qualroot-per-file.py over the oracle log (local).
        expected.put("chaos-1.1.0", Map.ofEntries(
                Map.entry("chaos-s22-a1o2.rosetta", 2), Map.entry("chaos-s22-a1o3.rosetta", 2),
                Map.entry("chaos-s22-a2alias.rosetta", 2), Map.entry("chaos-s22-a2dangle.rosetta", 2),
                Map.entry("chaos-s22-a2qual.rosetta", 2), Map.entry("chaos-s22-a2wild.rosetta", 2),
                Map.entry("chaos-s22-a3half-p2.rosetta", 2), Map.entry("chaos-s22-a3hub-p2.rosetta", 2),
                Map.entry("chaos-s22-a3third-p2.rosetta", 1), Map.entry("chaos-s22-a3third-p3.rosetta", 1),
                Map.entry("chaos-s22-base.rosetta", 2),
                Map.entry("chaos-s28-a1o1.rosetta", 2), Map.entry("chaos-s28-a1o2.rosetta", 2),
                Map.entry("chaos-s28-a1o3.rosetta", 2), Map.entry("chaos-s28-a1o4.rosetta", 2),
                Map.entry("chaos-s28-a2alias.rosetta", 2), Map.entry("chaos-s28-a2dangle.rosetta", 2),
                Map.entry("chaos-s28-a2qual.rosetta", 2), Map.entry("chaos-s28-a2wild.rosetta", 2),
                Map.entry("chaos-s28-a3half-p1.rosetta", 2), Map.entry("chaos-s28-a3hub-p2.rosetta", 2),
                Map.entry("chaos-s28-a3third-p1.rosetta", 2), Map.entry("chaos-s28-a4none.rosetta", 2),
                Map.entry("chaos-s28-a4snap.rosetta", 2),
                // chaos-s28-a5bom.rosetta carries the two qualifiers too and the released plugin
                // counts 2 more there after its BOM error-recovery (62 in the log); the fork never
                // loads a § 4b refused file, so the loaded population's figure is 60.
                Map.entry("chaos-s28-a5crlf.rosetta", 2), Map.entry("chaos-s28-a5mixed.rosetta", 2),
                Map.entry("chaos-s28-a5uni.rosetta", 2), Map.entry("chaos-s28-a9comment.rosetta", 2),
                Map.entry("chaos-s28-a9tabs.rosetta", 2), Map.entry("chaos-s28-base.rosetta", 2),
                Map.entry("zz-chaos-s28-a7last.rosetta", 2)));
        if (Boolean.getBoolean("corpus.diag.print")) {
            for (Map.Entry<String, Map<String, Integer>> e : perCell.entrySet()) {
                System.out.println("[QUAL-ROOT-CENSUS] " + e.getKey() + " " + e.getValue());
            }
        }
        assertEquals(expected, perCell, "QUALIFICATION_ROOT_MISMATCH per cell per file");
        assertEquals(60, perCell.get("chaos-1.1.0").values().stream().mapToInt(Integer::intValue).sum(),
                "the released plugin's 62 occurrences (20 s22 + 42 s28) less the 2 on the refused s28 BOM file");
    }

    /** Round 2 (cq NIT-5): the per-file map is keyed by BASENAME, so two models sharing one in a cell would merge — refused up front. */
    private static void assertBasenamesUnique(String cell, List<RModel> models) {
        Map<String, Long> counts = models.stream()
                .map(m -> m.sourceRange().file() == null ? "<no file>" : Path.of(m.sourceRange().file()).getFileName().toString())
                .collect(Collectors.groupingBy(f -> f, TreeMap::new, Collectors.counting()));
        List<String> duplicated = counts.entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey).toList();
        assertTrue(duplicated.isEmpty(), cell + ": models sharing a basename would merge in the per-file census: " + duplicated);
    }

    private static Map<String, Integer> qualificationRootWarnings(RLinkingResult result) {
        Map<String, Integer> byFile = new TreeMap<>();
        for (ValidationDiagnostic d : result.workspace().validationDiagnostics()) {
            if (d.issueCode() == ValidationIssueCode.QUALIFICATION_ROOT_MISMATCH) {
                byFile.merge(Path.of(d.range().file()).getFileName().toString(), 1, Integer::sum);
            }
        }
        return byFile;
    }

    /** One workspace build per CELL per JVM — both tests read the same results. */
    private static Map<String, RLinkingResult> results;

    private static synchronized Map<String, RLinkingResult> buildAll() throws IOException {
        if (results != null) {
            return results;
        }
        Map<String, RLinkingResult> built = new LinkedHashMap<>();
        for (CorpusCatalogue.Cell cell : gatedCells()) {
            List<Path> roots = CorpusCatalogue.loadRoots(cell);
            // The cell's own tree is LAST by contract (CorpusCatalogueTest pins it).
            Path ownRoot = roots.get(roots.size() - 1);
            Assumptions.assumeTrue(Files.exists(ownRoot),
                    "cell absent: " + ownRoot + " — gate skipped (CI / fresh clone)");
            List<RModel> files = new java.util.ArrayList<>();
            for (Path rootPath : roots) {
                if (!Files.isDirectory(rootPath)) {
                    throw new AssertionError("[V1] cell '" + cell.dirName()
                            + "': closure root missing while the cell is present: " + rootPath
                            + " — the population is the upstream build closure (see the class "
                            + "javadoc); a partial local corpus clone would fabricate "
                            + "dependency-topology diagnostics. The closure is declared in "
                            + CorpusCatalogue.TSV_REL_PATH + ".");
                }
                files.addAll(SymbolTestCorpus.loadCorpus(rootPath));
            }
            // One workspace per cell: the load roots ARE the cell roots, so closure
            // isolation is structural rather than emulated inside a merged space.
            built.put(cell.dirName(), RWorkspace.build(files, roots));
        }
        if (Boolean.getBoolean("corpus.diag.print")) {
            printSeedTables(built);
        }
        if (Boolean.getBoolean("corpus.diag.detail")) {
            printDiagnosticDetail(built);
        }
        results = built;
        return results;
    }

    /**
     * Lists the INDIVIDUAL error diagnostics per cell ({@code -Dcorpus.diag.detail=true}).
     *
     * <p>The seed tables give category TOTALS, which is all you need to ratchet a
     * budget but not enough to judge whether a move is a real regression or a missing
     * dependency root. When the 2026-08-14 band expansion moved these numbers, the
     * totals alone could not distinguish "the engine mis-resolves this model" from
     * "this cell was loaded without part of its closure" — the two have identical
     * shapes at category granularity and completely different meanings.
     */
    private static void printDiagnosticDetail(Map<String, RLinkingResult> all) {
        for (Map.Entry<String, RLinkingResult> e : all.entrySet()) {
            for (LinkingDiagnostic d : e.getValue().linkingDiagnostics()) {
                if (d.severity() == Severity.ERROR) {
                    System.out.println("[V1-DETAIL] " + e.getKey() + " LINK " + d.category()
                            + " name=" + d.unresolvedName() + " at " + d.range()
                            + " :: " + d.message());
                }
            }
            for (ValidationDiagnostic d : e.getValue().workspace().validationDiagnostics()) {
                if (d.severity() == Severity.ERROR) {
                    System.out.println("[V1-DETAIL] " + e.getKey() + " VALID " + d.issueCode()
                            + " at " + d.range() + " :: " + d.message());
                }
            }
        }
    }

    @Test
    void linking_error_budgets_hold() throws IOException {
        for (Map.Entry<String, RLinkingResult> e : buildAll().entrySet()) {
            Map<String, Integer> measured = new TreeMap<>();
            for (LinkingDiagnostic d : e.getValue().linkingDiagnostics()) {
                if (d.severity() == Severity.ERROR) {
                    measured.merge(d.category().name(), 1, Integer::sum);
                }
            }
            Map<String, Integer> budget = new TreeMap<>();
            Objects.requireNonNull(LINKING_ERROR_BUDGETS.get(e.getKey()),
                            "no LINKING_ERROR_BUDGETS table for corpus '" + e.getKey()
                                    + "' — every CORPORA key needs a budget table")
                    .forEach((cat, n) -> budget.put(cat.name(), n));
            assertEquals(budget, measured,
                    "[V1] " + e.getKey() + " linking ERROR budgets drifted — every move is a "
                            + "measured event: ratchet the budget row(s) in LINKING_ERROR_BUDGETS "
                            + "in the same commit as the wave that moved them (shrink-only; "
                            + "delete rows at zero). Re-seed print: -Dcorpus.diag.print=true");
        }
    }

    @Test
    void validation_error_budgets_hold() throws IOException {
        for (Map.Entry<String, RLinkingResult> e : buildAll().entrySet()) {
            Map<String, Integer> measured = new TreeMap<>();
            for (ValidationDiagnostic d : e.getValue().workspace().validationDiagnostics()) {
                if (d.severity() == Severity.ERROR) {
                    measured.merge(d.issueCode().name(), 1, Integer::sum);
                }
            }
            Map<String, Integer> budget = new TreeMap<>();
            Objects.requireNonNull(VALIDATION_ERROR_BUDGETS.get(e.getKey()),
                            "no VALIDATION_ERROR_BUDGETS table for corpus '" + e.getKey()
                                    + "' — every CORPORA key needs a budget table")
                    .forEach((code, n) -> budget.put(code.name(), n));
            assertEquals(budget, measured,
                    "[V1] " + e.getKey() + " validation ERROR budgets drifted — ratchet the "
                            + "budget row(s) in VALIDATION_ERROR_BUDGETS in the same commit as "
                            + "the wave that moved them (shrink-only; delete rows at zero). "
                            + "Re-seed print: -Dcorpus.diag.print=true");
        }
    }

    /**
     * v3.1 C1 (MF-4) — THE ORDERING QUESTION C1 DECLINED TO ANSWER BY FIAT.
     *
     * <p>Upstream's symbol parent chain puts the file scope LAST, so a lexical
     * binding shadows a same-named global enumeration; the fork's phase order
     * resolves globals FIRST. C1 could have flipped that, but flipping is a
     * behaviour change on cells that are byte-EXACT today, so
     * {@code LexicalResolutionPass} COUNTS the disagreement instead and the
     * decision waits for evidence.
     *
     * <p>The counter shipped with the count and NOTHING THAT READ IT — an
     * unwired meter, which is indistinguishable from a meter reading zero, and
     * exactly the silent-instrument class this whole phase exists to kill. This
     * test is the reader.
     *
     * <p>A non-zero value is not a failure of the engine; it is the flip
     * decision ARRIVING, with the carriers named. If it fires: run
     * {@code -Dcorpus.diag.detail=true}, find the colliding names, and decide
     * the phase order deliberately — do not silence this by raising a budget.
     */
    @Test
    void no_head_shadowing_conflicts_across_the_band() throws IOException {
        Map<String, Integer> conflicts = new TreeMap<>();
        for (Map.Entry<String, RLinkingResult> e : buildAll().entrySet()) {
            int n = e.getValue().workspace().headShadowingConflicts();
            if (n != 0) {
                conflicts.put(e.getKey(), n);
            }
        }
        assertEquals(Map.of(), conflicts,
                "[V1] a lexical binding disagrees with the global enumeration pass 4 chose, on "
                        + conflicts.size() + " cell(s). Upstream would prefer the LEXICAL one "
                        + "(its parent chain puts the file scope last, the fork resolves globals "
                        + "first). C1 counted this rather than flipping it because flipping moves "
                        + "byte-EXACT cells. A non-zero count is the evidence that decision was "
                        + "waiting for — triage the carriers with -Dcorpus.diag.detail=true and "
                        + "decide the order deliberately; do NOT budget it away.");
    }

    /**
     * Prints the two budget-map literal BODIES as valid Java (each line prefixed
     * {@code [V1-SEED] } for grep; strip the prefix and paste as the
     * {@code Map.ofEntries(...)} argument list of the corresponding constant —
     * see the class javadoc's seeding note on the shared floor constants).
     */
    private static void printSeedTables(Map<String, RLinkingResult> all) {
        printSeedMap("LINKING_ERROR_BUDGETS", "DiagnosticCategory", all, result -> {
            Map<String, Integer> counts = new TreeMap<>();
            for (LinkingDiagnostic d : result.linkingDiagnostics()) {
                if (d.severity() == Severity.ERROR) {
                    counts.merge(d.category().name(), 1, Integer::sum);
                }
            }
            return counts;
        });
        printSeedMap("VALIDATION_ERROR_BUDGETS", "ValidationIssueCode", all, result -> {
            Map<String, Integer> counts = new TreeMap<>();
            for (ValidationDiagnostic d : result.workspace().validationDiagnostics()) {
                if (d.severity() == Severity.ERROR) {
                    counts.merge(d.issueCode().name(), 1, Integer::sum);
                }
            }
            return counts;
        });
    }

    private static void printSeedMap(String constantName, String enumName,
            Map<String, RLinkingResult> all,
            java.util.function.Function<RLinkingResult, Map<String, Integer>> counter) {
        System.out.println("[V1-SEED] // " + constantName + " body:");
        int corpusIdx = 0;
        for (Map.Entry<String, RLinkingResult> e : all.entrySet()) {
            Map<String, Integer> counts = counter.apply(e.getValue());
            int total = counts.values().stream().mapToInt(Integer::intValue).sum();
            System.out.println("[V1-SEED] \"" + e.getKey() + "\", Map.ofEntries(   // total "
                    + total);
            int i = 0;
            for (Map.Entry<String, Integer> c : counts.entrySet()) {
                String comma = (++i < counts.size()) ? "," : "";
                System.out.println("[V1-SEED]         Map.entry(" + enumName + "."
                        + c.getKey() + ", " + c.getValue() + ")" + comma);
            }
            String corpusComma = (++corpusIdx < all.size()) ? "," : "";
            System.out.println("[V1-SEED] )" + corpusComma);
        }
    }
}
