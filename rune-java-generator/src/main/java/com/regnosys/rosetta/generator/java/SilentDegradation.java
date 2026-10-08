package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.generator.GenerationException;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * v3.1 phase C, C0 item 1 — the LOUD register: every place the emitter is known to
 * degrade silently, counted.
 *
 * <p><b>The problem it exists to end.</b> The band's 1,559 mismatches were invisible for
 * three months behind a green suite because the engine, at a seat it has no rule for,
 * does not fail — it guesses. It echoes an unresolved name as a bare Java identifier, it
 * collapses an unresolvable type to {@code Object}, and until v3.1 C0 it rendered a resolvable
 * type-keyed switch as a ternary comparing against a type name (a REFUSAL since —
 * {@link Site#TYPE_SWITCH_TERNARY_STUB}, the register's first refusal site), until v3.2 seat 7 a literal
 * guard over a lambda-bound subject ({@link Site#LITERAL_SWITCH_TERNARY_STUB} since), and until v3.2
 * seat 12 every other switch it had no ladder for (a REFUSAL since — the
 * {@link Site#SWITCH_TERNARY_STUB} residual, the tail after those two guarded sub-sites). Each produces
 * plausible-looking Java that is wrong, and in every case verified so far, does not even
 * compile. Nothing in the pipeline says so.
 *
 * <p><b>What this is.</b> A counter per degradation site, reset and read per (cell, kind)
 * by the D11 harness, so the 25x2 matrix answers one question per site: does it fire on a
 * cell we consider CLEAN? A site that fires only on band cells is a defect with no
 * legitimate carrier — it becomes a hard refusal ({@code GenerationException}, surfaced by
 * D11's existing {@code assertNoUnwaiveredGenErrors} vocabulary) rather than silent
 * output. A site that fires on a clean cell is triaged first: something green depends on
 * it, and converting it blind would manufacture a regression. That ordering — count, then
 * refuse — is the plan's § 4 safety argument, and it is why this class counts before
 * anything throws.
 *
 * <p><b>What it is NOT.</b> Not a diagnostic channel, not a log, and never emission-
 * affecting: recording is a counter increment and a bounded sample of witnesses. Its
 * numbers do not enter any parity claim; they are the burn-down meter for C1-C3, which is
 * why {@link Site} carries the stage that is expected to retire each one.
 *
 * <p>Thread-safe by construction (atomics + a synchronized witness list) so a future
 * parallel generation cannot corrupt a count — a wrong count here would be the same class
 * of silent wrongness the register exists to end.
 */
public final class SilentDegradation {

    /**
     * A place where the emitter knowingly produces output it cannot justify.
     *
     * <p>Each constant names the seat, what it emits instead of the right answer, and the
     * stage that owns retiring it. Adding one is cheap and expected — the register is the
     * catalogue of what we know is unsound, so a site discovered mid-programme belongs
     * here rather than in a comment.
     */
    public enum Site {
        /**
         * A symbol reference whose name the resolver never bound, rendered as a bare Java
         * identifier ({@code MapperS.of(MarginAffiliate)} where the golden has
         * {@code MapperS.of(PartyRoleEnum.MARGIN_AFFILIATE)}). The identifier is unbound,
         * so the file does not compile. Root cause E1/E2; retired by C1's resolution
         * rebuild, which binds the name or refuses.
         */
        UNRESOLVED_SYMBOL_ECHO,

        /**
         * A type-keyed {@code switch} that reached the generic ternary render, which
         * compared the subject against the TYPE NAME as if it were a value
         * ({@code Objects.equals(fpml.FloatingRateCalculation, MapperS.of(fpmlRate))}).
         * A type name is not a Java expression; the code that ternary emitted never
         * compiled - the render is history since v3.1 C0 HERE (the residual path refuses per
         * type-keyed case at this site) and since v3.2 seat 12 at the tail ({@code SWITCH_TERNARY_STUB},
         * the un-guarded rest). Root cause E5 (the alias
         * seat had no instanceof-ladder renderer); retired by C3b.
         */
        TYPE_SWITCH_TERNARY_STUB,

        /**
         * A model type whose declaring model could not be reached, collapsed to
         * {@code java.lang.Object}. Every consumer downstream then types against Object —
         * the report-output collapse the root-cause audit records
         * ({@code ReportFunction<TransactionReportInstruction, Object>}). Root cause E2's
         * third surface; retired by C1 (resolution) and C2 (the typed layer).
         */
        TYPE_COLLAPSED_TO_OBJECT,

        /**
         * A {@code [meta = value]} constructor entry whose meta NAME the parser left {@code null}:
         * no setter can be named from it, and the name table's {@code null} passthrough would have
         * rendered the literal {@code .setnull(…)}. Unreachable from a well-formed model (every
         * corpus entry carries its name) — kept as a refusal so a parser defect surfaces loudly
         * instead of as un-compilable Java. Seat-21 review (MF-2).
         */
        WITH_META_ENTRY_UNNAMED,

        /**
         * v3.2 seat 3 (the chaos census's family F12, "silent-whole-function-no-emit"): a
         * function root element that is NOT a dispatch variant and for which the function
         * generator's run produced NEITHER a file NOR a generation error. The measured
         * carrier was the chaos cell's {@code C4Speed}: thirteen same-named dispatch groups in
         * thirteen namespaces were keyed by BARE NAME into one group, one file was emitted at
         * the last base's path carrying every namespace's members, and twelve functions
         * vanished without a counter (D11 {@code missingOutput=12}, every {@code LOUD} counter
         * zero — the exact class L4 exists to forbid). Recorded by the ACCOUNTING pass at the
         * end of {@code FunctionGenerator#generateWithErrors}: every non-variant function must
         * be accounted for by a file or by an error, so the class can never be silent again
         * whatever mechanism drops the file. The refusal carries the function's own target
         * path, so D11's waiver-aware gate reports it as a missing output WITH ITS REASON.
         * Dispatch VARIANTS are folded into their group's file by design (upstream's
         * {@code streamObjects} excludes {@code FunctionDispatch}) and are not counted.
         */
        FUNCTION_NOT_EMITTED,

        /**
         * v3.2 seat 3 (the chaos census's family F9, "condition-wiring-silent-skip"): a
         * {@code typeAlias} carrying its own {@code condition}s reached a consumer that dropped
         * them without a word — the data-rule walk, which cased only {@code Data} and
         * {@code choice} owners (no {@code <Alias><Condition>} class), and the type-format
         * validator, whose alias-condition wing ({@code @Inject} + {@code runConditions}) was
         * recorded at PR #403 as "post-9.83, 0 of 4,736 goldens" and deliberately not built —
         * a premise the released-9.83.0 chaos golden {@code C8PricedTypeFormatValidator}
         * REFUTES (the wing is 9.83.0 behaviour; the vendored corpus simply has no alias with
         * a condition, so nothing ever carried it). At the counter commit the site fired for
         * EVERY alias condition at both consumer seats (12 / 12 on the chaos cell, both routes);
         * since the fix (the alias-owned class, the wing, the parser's alias-condition scope)
         * both seats emit, and the site fires ONLY for the PARAMETERISED sub-wing — a condition
         * whose owner alias declares type parameters, which is post-9.83 and which the released
         * 9.83.0 plugin renders as a self-comparison (oracle group alias-conditions-param,
         * banked): refused whole at the data-rule seat (that one class) and at the validator
         * seat (the WHOLE validator of the type walking it — the two seats' blast radii differ
         * and are documented at each seat). No corpus or chaos carrier; the seat fixture and the
         * banked oracle group are its witnesses.
         *
         * <p>Not a net, unlike {@link #FUNCTION_NOT_EMITTED}: this site is SHAPE-specific. An
         * alias-condition shape the wing cannot build (a declined expression, a missing type
         * call, a hierarchy cut at {@code TypeFormatConstraintScan.MAX_DEPTH}) surfaces as an
         * ATTRIBUTED generation error through {@code JavaClassGenerator}'s per-object boundary —
         * loud, with the file's own path, but not counted here.
         */
        TYPE_ALIAS_CONDITION_DROPPED,

        /**
         * v3.2 seat 3, the round-1 code-quality review (SF-1): a function file about to be
         * written at a path {@code FunctionGenerator} has ALREADY written in this run — two
         * same-named function root elements of one namespace in different files, each a
         * standard function (or a base). The accounting pass checks path PRESENCE and cannot
         * see an overwrite, so the generator's one writer seam refuses the second file instead
         * of replacing the first silently. Upstream's per-resource writer is last-writer-wins
         * on this shape, but its validator forbids it before generation (a duplicate function
         * name is a model error), so no valid model reaches either behaviour: a belt on invalid
         * input, refused with the second function's own path. No corpus or chaos carrier; seat
         * fixture witness only (control8).
         */
        FUNCTION_PATH_COLLISION,

        /**
         * v3.2 seat 3, the round-1 code-quality review (SF-1): a per-file dispatch group whose
         * BASE declaration is not in the group's own file — variants in one file, the base in
         * another file of the same namespace. Upstream resolves a dispatch base among the file's
         * siblings only ({@code EcoreUtil2.getSiblingsOfType}) and the released 9.83.0 plugin
         * REFUSES the split at link time (the seat's oracle group func-dispatch-crossfile,
         * oracle-rejected); the fork had rendered the dispatch class from the first variant's
         * signature (a variant declares no inputs or output) and, at the base's path, would have
         * overwritten the base's own emission. Refused with the group's path before any file is
         * written. No corpus or chaos carrier; seat fixture witness only (control7).
         */
        DISPATCH_BASE_MISSING,

        /**
         * v3.2 seat 3 (F9), round 2 — the type-format validator's alias-condition wing: a
         * condition class whose simple name is a boilerplate type the validator writes AFTER its
         * {@code @Inject} fields ({@code ValidationResult}, {@code ArrayList}, {@code List},
         * {@code Lists}, {@code ComparisonResult}, {@code RosettaPath}, {@code Streams}, a
         * java.lang element type). The released 9.83.0 plugin claims the condition class FIRST
         * (text order) and writes the boilerplate type fully qualified everywhere — {@code new
         * java.util.ArrayList()}, {@code com.rosetta.model.lib.validation.ValidationResult}
         * (oracle group alias-conditions-boilerplate, its goldens banked locally — not in the
         * hold-out bar); the fork's template writes those
         * names literally, so the shape is REFUSED rather than emitted with bytes that differ.
         * A name the class header claims first (the class's own name, {@code Validator}, the
         * data class, {@code Inject}) is not this shape: there the condition class is written
         * fully qualified on both sides (oracle groups alias-conditions-filescope and
         * alias-conditions-header). The java.lang element type is claimed from the validator's
         * wiring plans (round 3 — never imported, so the import set cannot carry it), and the
         * data-rule seat refuses its own java.lang SUBJECT type the same way. No corpus or chaos
         * carrier; seat fixture witness only (a17, a18).
         */
        BOILERPLATE_NAME_COLLISION,
        /**
         * v3.2 seat 4 (PR #625, F7): a report's {@code with type} or {@code with source} reached the
         * generator WITHOUT a linker-resolved id. The linker's TYPE_NOT_FOUND / EXTERNAL_SOURCE_NOT_FOUND
         * is the loud answer; the generator REFUSES rather than resolve the name by a workspace-wide
         * first match — the search that handed the chaos s07 report functions the FIRST namespace's
         * report type and rules (eleven byte rows, non-compiling emissions). Witnessed by the seat
         * suite's unresolvable-reference fixtures; the chaos cell and the corpora print 0.
         */
        REPORT_REFERENCE_UNRESOLVED,
        /**
         * v3.2 seat 7 (PR #628, round 2 - the code-quality review's MF-1): the LOUD register's LITERAL twin. A
         * LITERAL-guarded {@code switch} whose subject is bound by an enclosing inline function (the lambda's
         * implicit item, a closure parameter, a then body's item) that reached the generic ternary render would
         * compare the guard against a MAPPER render ({@code Objects.equals("r", item)}) - Java that COMPILES and
         * is ALWAYS FALSE, the wrong-result emission the seat's literal-keyed control measured before the
         * literal-guard block existed. The block renders the witnessed shapes; every shape it declines (a number
         * or boolean literal arm under literal guards, a non-admitted arm kind, a closure-parameter subject the
         * block declines - any parameter but the FIRST, which it admits) REFUSES here instead of emitting silently
         * wrong Java. A closure-parameter subject the block ADMITS renders the block and still leaves its ALIAS
         * signature to the raw-name fallback (the alias signature walk signs it under NAME guards only) - BANKED
         * with the oracle group {@code alias-literal-switch-number-boolean-arms}, chartered to pin it FIRST (round
         * 3, the code-quality review's MF-1: this clause had said the opposite of the banking). "Any case" means a
         * MIXED guard set (a literal guard beside a NAME guard, the seat-2 banked shape) over such a subject is
         * claimed HERE, before the type-keyed refusal the name guard alone would reach - the register reads 0 at
         * this site on every cell and both routes at the chain of record (round 3, the cq review's NIT-3). No corpus
         * carrier (the vendored census pins zero literal-guarded lambda-direct switches; the chaos {@code switched}
         * alias renders the block); witnessed by the seat suite's control5, which proves the site able to fire.
         */
        LITERAL_SWITCH_TERNARY_STUB,
        /**
         * v3.2 seat 9 (PR #630, F13 / D48 — the twelfth site): a QUALIFIED enum-value reference
         * ({@code C17SideEnum -> Sell}) whose enumeration resolved but whose VALUE did not — the linker
         * left it unbound exactly as upstream does and reported {@code ENUM_VALUE_NOT_FOUND} at the
         * reference's own range — used to render a Java constant from the raw value-name string
         * ({@code C17SideEnum.SELL}, a constant the enum does not declare): a NON-COMPILING emission with
         * no refusal and every counter at 0, the E2 wrong-enum class the chaos s17 rival-enum split was
         * seeded to catch, hidden behind the golden-free declaration until the seat's commit-2 instrument
         * dumped the fork's side of the seventeen golden-free files (two of them carried it: p2's
         * {@code C17Sift} and {@code C17BooksC17Agree}). The render seat REFUSES here when the linker's
         * verdict is on record ({@code GeneratorModel.isReportedUnresolved} — the resolver's own diagnostic,
         * keyed by the reference's range and the value name, never a guess), and COUNTS here — the fallback
         * kept — when the value is unbound WITHOUT a diagnostic (an AST built outside the linker: the IR
         * compiler's own unit fixture, which stays green; expected 0 on every cell and both routes). ONE SITE, TWO
         * refusal seats, on both routes: the IR adapter declines the shape ({@code valueNameFallback}) to this
         * render; and, since the round-1 review (spec SF-1, D48 decision 1), the DISPATCH seat - {@code FunctionGenerator}
         * used to render a dispatch value the linker left unbound into the routing {@code case} label and the
         * variant class name (a non-compiling class with no counter), REFUSED here on the linker's
         * category-filtered verdict for BOTH unbound paths (the value not found - {@code ENUM_VALUE_NOT_FOUND}; the
         * parameter not found - {@code DISPATCH_PARAM_NOT_FOUND}, round 2): zero chaos / vendored carriers, the seat
         * suite's a6 / a7 and lane L11 the witnesses. The
         * released plugin refuses p2 WHOLE for the same two references ("Couldn't resolve reference to
         * RosettaFeature 'Sell'", the mojo's all-or-nothing gate — {@code target/v32-seat9-instruments/
         * refused-groups/x36enum-split-pair/run1.log}, local); the fork's licence stays per ELEMENT (D48), so
         * p2's other seven files still emit golden-free and p1 — whose eight goldens the released plugin
         * DOES define when p1 is judged alone (the chaos pin script had excluded it as a split partner) — is
         * byte-identical. Witnessed by the chaos cell (2 on FUNCTION + DATA_RULE, both routes) and the seat suite.
         */
        ENUM_VALUE_NAME_ECHO,
        /**
         * v3.2 seat 12 (PR #633, D52 - COUNTERS FIRST, the thirteenth site): the RESIDUAL of the switch ternary - the
         * REFUSAL that replaced it. Upstream
         * compiles EVERY {@code switch} as a statement builder (a hoisted {@code switchArgument} if-ladder; a mid-expression
         * consumer collapses it as {@code ifThenElseResult}, a lambda body as a block lambda) and the goldens carry ZERO
         * chained ternaries; the fork renders the ladder at its blessed seats only (the deep-then handshake, the ctor
         * instanceof ladder, the data-rule whole-body return ladder, the SET-position assignment, the alias ladders, the
         * four lambda block renderers) and every other switch fell, until v3.2 seat 12, to
         * {@code Objects.equals(<raw source name>, <Mapper>) ? .. : ..} - the guard the raw {@code Red} /
         * {@code Long} / {@code long} of the source, the argument a Mapper
         * render: non-compiling or always false, never upstream's bytes, with NO counter. Two sites already guard
         * sub-classes of that fall-through ({@link #TYPE_SWITCH_TERNARY_STUB} a guard naming a TYPE, per case;
         * {@link #LITERAL_SWITCH_TERNARY_STUB} a literal guard over an inline-function subject); THIS site fires for
         * every switch that passes both and still reaches the end of {@code ControlFlowHandler.handle(RSwitchExpr)} -
         * placed AFTER the case loop so the two guarded sites keep their fixtures and their declared rows. The chaos
         * cell's M2 rows ({@code target/v32-gen2-census.md} s3: a switch inside a mapItem lambda whose subject or arm kinds
         * the block renderers decline, an alias body with switch arms, a comparison / {@code +} / {@code or} / {@code and}
         * operand, a data-type condition's then arm - s26 / s27 / s31) are its witnesses, re-measured at this seat's D11
         * on both routes (the IR route serves the switch through this handler as an oracle root - LAW 77 by identity);
         * PREDICTED 0 on every vendored cell (no golden carries a chained ternary - upstream never wrote one), a claim
         * the 25-cell rings print. The HEAL is
         * chartered by hoist channel for the seats after this one (D52 decision 3); this site keeps M2 LOUD until then.
         * Witnessed by the seat suite's c1 (the site proven able to fire) and n1 (a blessed seat still renders).
         */
        SWITCH_TERNARY_STUB,
        /**
         * v3.2 seat 12 (PR #633, D52 - the fourteenth site): the deep-path {@code TODO} stub. A deep feature call
         * ({@code ->>}) whose RECEIVER type {@code NavigationHandler.resolveDeepReceiverJavaClass} cannot name used to
         * render the map hop {@code <recv>.map("chooseX", _x -> chooseX(_x))} with a {@code TODO(M7b-4): wire
         * DeepPathUtil} block comment inside the lambda body and NO {@code @Inject <Type>DeepPathUtil} field - a
         * comment wearing a compile error, the seat-4 banked register
         * question ("a TODO render is a silent emission today") answered here. The resolver has no arm for (a) a receiver
         * that is itself a deep call ({@code o ->> inners ->> deep}; a rule's {@code extract outer ->> inners ->> deep})
         * nor (b) the explicit parameter of a then-extract over a LIST OF LISTS ({@code then extract ins [ ins ->> deep
         * ... ]}) - the chaos M7b rows (s29 {@code C29LoL} / {@code C29InnerRule}), whose heal (the two arms, the
         * list-of-lists count wrap + coercion, the rule's cardinality) is seat 13's (D52 decision 3). PREDICTED 0 on every
         * vendored cell (no golden carries the stub; the deep-path utils are at 100 percent since #408 / #627). Witnessed
         * by the seat suite's c2 (the site proven able to fire) and n2 (a lambda-item deep receiver still resolves and
         * injects - the seat-6 heal).
         */
        DEEP_PATH_UTIL_UNRESOLVED,
        /**
         * v3.2 seat 12 (PR #633, D52 - the fifteenth site): the alias signature's RAW rune type name. When
         * {@code FunctionAliasHelper}'s signature walk declines, {@code computeReturnType} falls back to the function
         * OUTPUT's raw type name inside a Mapper wildcard ({@code MapperS<? extends number>}, {@code MapperC<? extends
         * string>}) - for a BUILTIN type (string / number / int / boolean / date ... / a typeAlias of one) never a Java
         * type, a non-compiling signature with no counter (seat 7 banked it: "a typed fallback or a refusal"). This site
         * refuses that leg - the output's type resolved through the generator model with aliases stripped is a
         * builtin; a MODEL-typed output keeps the fallback (its raw name IS the Java simple name and the bytes may be
         * upstream's - the rings decide). The chaos M6b rows ({@code C25Shadow}: a reduce over counts) and the seams of
         * M2's {@code C26AliasArms} / {@code C26Nested} are its witnesses; {@code MapperC<l2>} (M7c - the nested
         * closure parameter's NAME as the type, the walk's own arm) is NOT this leg - it is
         * {@link #ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE}'s since v3.2 seat 13 (a byte row until then).
         * PREDICTED 0 on every vendored cell (a golden carries no rune basic name as a Java type).
         * Witnessed by the seat suite's c3 (the site proven able to fire) and n3 (a model-typed output keeps the
         * fallback's bytes).
         */
        ALIAS_SIGNATURE_RAW_TYPE,

        /**
         * v3.2 seat 13 (D53, THE CLOSING SEAT - site R4, the chaos M7c rows {@code C29Piped}): the alias signature
         * walk's 2-name-root fallback ({@code FunctionAliasHelper.inferEnumValueRefType}, "assuming it IS an enum
         * just not resolved") returned a CLOSURE PARAMETER's name as the alias element type - the parser carries
         * {@code l2 -> v} inside {@code then extract l2 [ l2 -> v ]} as a 2-name ref, and a then-piped extract has no
         * bare-symbol LEFT for the walk's extract-item arm to root on, so every typing arm declined and the seam
         * rendered {@code MapperC<l2>}: a signature javac refuses, SILENT. Refused when the root name is bound as a
         * parameter by an enclosing inline function (the parent walk up to the owning function or rule -
         * {@code enclosingClosureParameterOwner}, the ONE predicate); every other unresolved 2-name root keeps the
         * fallback's bytes. The walk's typing of the then-piped extract's element is the v3.3 heal (D53 decision 2).
         * PREDICTED 0 on every vendored cell (a golden carries no lambda parameter as a Java type). Witnessed by the
         * closing seat suite's c4 (the site proven able to fire) and n4 (the same 2-name root under an extract
         * with a bare-symbol left still types through the extract-item arm).
         */
        ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE,

        /**
         * v3.2 seat 13 (D53 - site R6, the chaos M5e rows {@code C23Scale}'s {@code counted} alias): the then-chain
         * alias body ({@code FunctionExpressionRenderer.renderAliasThenHoistOrNull}) returned its then-consumer BARE
         * against the alias method's Mapper seam - the count render's primitive {@code int}
         * ({@code return thenArg.resultCount();} in a {@code MapperS<Integer>} method), a body javac refuses, SILENT.
         * Refused by TYPE on the reference route: a consumer whose compiled type is an item (not a wrapper - not a
         * List, a Mapper, a list-of-lists Mapper or a ComparisonResult) that the two proven-single collapse arms did
         * not re-wrap. A wrapper-typed consumer keeps its bytes; an UNTYPED consumer (the #433 standing class) is not
         * decided here and stays a byte row, disclosed. The {@code MapperS.of(...)} wrap the plugin renders is the
         * v3.3 heal (D53 decision 2). PREDICTED 0 on every vendored cell (a bare item return never compiled against
         * the seam, so no golden carries it). Witnessed by the closing seat suite's c5 (the site proven able to fire)
         * and n5 (a then-chain whose consumer is a Mapper keeps its bytes).
         */
        ALIAS_BODY_ITEM_UNWRAPPED,

        /**
         * v3.2 seat 13 (D53 - site R5, the chaos M3-PARKED rows {@code C24WithMetaExists} / {@code C24WithMetaPlain}):
         * the legacy M7b-1 with-meta STUB of {@code ConstructionHandler.handle(RWithMetaExpr)} -
         * {@code <arg>.toBuilder().setMeta(MetaFields.builder()…).build()} - reached whenever the three typed arms
         * (the annotated-output wrapper, the POJO's own MetaFields, the expression-derived wrapper) decline; documented
         * non-compiling since PR #202 ("zero goldens carry the stub form") and SILENT. On the chaos cell every carrier
         * is a with-meta construction over a {@code nothing} value ({@code null.toBuilder()…}; the released plugin
         * mints {@code ReferenceWithMetaVoid} beside it, itself non-compiling for the PLAIN twin - the cell's upstream
         * register). Refused at the stub itself; the typed arms keep their bytes. PREDICTED 0 on every vendored cell
         * (no golden carries the stub form). Witnessed by the closing seat suite's c6 (the site proven able to fire)
         * and n6 (a with-meta over a typed value keeps the typed wrapper render).
         */
        WITH_META_UNTYPED_STUB,

        /**
         * v3.2 seat 13 (D53 - site R13, the chaos M3-DECLARED rows {@code C24IntoMeta}, the hold-out byte pin
         * {@code SetMetaVoid}): the single-output SET seat's fallback ({@code FunctionExpressionRenderer
         * .renderOperationInner}, {@code <out> = toBuilder(<rhs>)}) with a META-ANNOTATED output and a value whose
         * item type is {@code Void} - {@code toBuilder(MapperS.of(c).<Void>map(…).get())}, which javac refuses
         * ({@code no suitable method found for toBuilder(Void)}), SILENT. The released plugin renders the EMPTY
         * wrapper ({@code toBuilder(FieldWithMetaVoid.builder().build())} - upstream's Void-into-meta coercion, the
         * v3.3 heal under D53 decision 2). Refused by TYPE: the value's item type read from its compiled stamp (the
         * #630 Void render law) or, untyped, from the front-end type. A non-Void value keeps its bytes. PREDICTED 0
         * on every vendored cell. Witnessed by the closing seat suite's c7 (the site proven able to fire) and n7 (a
         * plain value into the same meta output keeps the {@code toBuilder} wrap); the hold-out byte pin
         * {@code SetMetaVoid} is RE-CUT to the refusal at the byte bar and the compile gate (LAW 73).
         */
        VOID_INTO_META_OUTPUT,

        /**
         * v3.2 seat 13 (D53 - site R14, the chaos M9 rows {@code C30MultiThenAdd}): the LAST-RESORT inline ternary
         * of {@code ControlFlowHandler.handle(RConditionalExpr)} - {@code <cond>.getOrDefault(false) ? <then> :
         * <else>} - reached by a conditional every hoist seat declined (on the chaos cell: a conditional at a pathed
         * ADD whose arms are list literals of different element types; the ADD seat then appends {@code .getMulti()}
         * to the ELSE arm alone). The released plugin hoists every conditional to a statement
         * ({@code JavaIfThenElseBuilder} - the {@code final List<BigDecimal> ifThenElseResult0} block with a
         * per-arm coercion); ZERO vendored goldens carry the ternary shape (the seat-13 census over 206,685 golden
         * files: 0 hits of {@code .getOrDefault(false) ? }), so the arm is refused WHOLE on the FUNCTION path - a render
         * no golden ever carried and javac refuses wherever the arms' Mapper kinds differ. The RULE path keeps the
         * pre-seat ternary (commit 4): the OPTIMISED route's rule face, which has no statement-hoist machinery, reaches
         * this render on vendored drr 6.34.1 (regulation-hkma-rewrite-trade-rule 1079 / 1104) where the default route's
         * rule path hoists, and that route's suite forbids an undeclared vendored rule-face refusal; no chaos rule row
         * reaches the seat. PREDICTED 0 on every vendored cell on the default and IR routes.
         * Witnessed by the closing seat suite's c8 (the site proven able to fire) and n8 (a conditional at a hoist
         * seat still renders its {@code ifThenElseResult} block).
         */
        INLINE_CONDITIONAL_TERNARY,

        /**
         * v3.2 seat 13 (D53 - site R7, the chaos M5f rows {@code C27NatCalled} at DATA_RULE): the explicit-args
         * evaluate loop of {@code ReferenceHandler.handle(RSymbolReference)} reached a position with NO HOIST
         * CONSUMER for the coercion the callee's parameter needs - the STATEMENT_SINK route with no reachable
         * statement-hoist sink (a data-rule / type-condition path opens no session; a lambda interior stops the
         * walk) or route NONE - and DECLINED, "keeping the flat, still-waivered form": an {@code Integer} item into
         * a {@code number} parameter ({@code c27Check.evaluate(c27Nat)} where the released plugin renders
         * {@code c27Check.evaluate((c27Nat == null ? null : BigDecimal.valueOf(c27Nat)))}), or a meta wrapper into a
         * bare-value parameter - javac refuses both, SILENT (18 of 18 non-compiling by the seat-13 census). Refused
         * by the ONE plan {@code ReferenceHandler.planArgCoercion} (LAW 69: tryMetaDerefArg's own decision half - the
         * arg's item type against the parameter's Java type through the coercion service's probe); the heal is the
         * inline null-guarded coercion at the sink-less seat (v3.3, D53 decision 2). PREDICTED 0 on every vendored
         * cell. Witnessed by the closing seat suite's c9 (the site proven able to fire on the alias-condition shape)
         * and n9 (the same call at a function's SET seat keeps its hoisted coercion).
         */
        CALL_ARGUMENT_COERCION_DROPPED,

        /**
         * v3.2 seat 13 (D53 - site R10, the chaos M4 rows {@code C30TwoOp} and the M3-VOID-RENDER-LAW-NEW-SEATS
         * rows {@code C24Segments}): a segment-pathed conditional SET whose LEAF is meta-annotated hoists the leaf's
         * concrete wrapper ({@code final FieldWithMetaString ifThenElseResultN;} - {@code FunctionExpressionRenderer
         * .renderPathedConditionalSetOrNull}) and assigned each arm's BARE value to it ({@code ifThenElseResultN = a;}
         * / {@code = MapperS.of(c).<Void>map("getTok", …).get();}) - a String or a Void into a FieldWithMeta local,
         * which javac refuses; 25 of 25 non-compiling by the seat-13 census, SILENT. The released plugin coerces the
         * arm into the wrapper null-guarded ({@code a == null ? FieldWithMetaString.builder().build() :
         * FieldWithMetaString.builder().setValue(a).build()}, a literal hoisted to a type-named local first; a Void
         * arm the EMPTY wrapper) - the v3.3 heal under D53 decision 2. Refused by TYPE at the arm: the leaf is a
         * META leaf and the arm's compiled item type is not the wrapper (an untyped arm keeps its bytes; an arm that
         * IS the wrapper assigns directly, the golden form). PREDICTED 0 on every vendored cell (a bare value into a
         * wrapper local never compiled). The class's second mechanism - a MULTI meta leaf's conditional collapsing to
         * {@code out = <list>.get();} - sits behind this site in text order on every chaos carrier and stays byte-listed.
         * Witnessed by the closing seat suite's c10 (the site proven able to fire) and n10 (the same conditional at a
         * PLAIN leaf keeps its hoist block).
         */
        HOIST_ARM_COERCION_DROPPED
    }

    /**
     * v3.2 seat 3, round 5 (spec SF-2 / cq SF-3): the ORIGIN of a simple name a condition seat has
     * CLAIMED — recorded by the producer that registers the claim, read by the
     * {@link Site#BOILERPLATE_NAME_COLLISION} refusal to name the witness that exists. ONE declaration
     * for both seats (LAW 69): the validator's claim map and the data rule's clash carry it, and the
     * refusal message renders {@link #witnessClause} from it — never a test on the canonical name's
     * text (the round-4 java.lang PREFIX test on the claimant, retired here: a {@code java.lang.annotation}
     * import would have been labelled a java.lang claim by that prefix while coming from the import set).
     * Witnessed at both seats by a17 (IMPORT) and a18 (JAVA_LANG); lanes AF / AG cross the clauses.
     */
    public enum ClaimOrigin {
        /** The class's own name, claimed by the header — oracle group alias-conditions-filescope
         * ({@code OwnTypeFormatValidator}); a header claim is never the refused shape, so this clause is
         * UNRENDERABLE at both seats today (round-6 cq SF-2 / spec N-1): the validator stores the OWN_CLASS
         * claim under the one key {@code validatorClassName}, and its refusal is guarded by a
         * {@code headerClaims} set that always holds that name; the data-rule seat stamps only IMPORT /
         * JAVA_LANG. The constant exists so every producer stamps an origin (LAW 69); lane X witnesses the
         * CLAIM (a15), and a15 pins this wording beside its siblings' so that the day the guard changes the
         * clause is already gated. */
        OWN_CLASS(" (oracle group alias-conditions-filescope)"),
        /** A name the file IMPORTS (the boilerplate set, the data class, {@code Inject}, the cast,
         * wrapper and dependency types) — the banked oracle group alias-conditions-boilerplate carries
         * the shape (a17). */
        IMPORT(" (oracle group alias-conditions-boilerplate)"),
        /** A java.lang type the file WRITES without importing — the multi local's element type at the
         * validator seat, the alias subject at the data-rule seat; no oracle golden carries the shape,
         * the seat fixture a18 alone does. */
        JAVA_LANG(" (no oracle golden carries this shape; seat fixture a18)");

        /** The clause the refusal message appends after "writes X fully qualified"; OWN_CLASS's is never
         * reached — see that constant. */
        public final String witnessClause;

        ClaimOrigin(String witnessClause) {
            this.witnessClause = witnessClause;
        }
    }

    /**
     * A claim on a simple name: the CANONICAL name that holds it and the {@link ClaimOrigin} that recorded
     * it — ONE declaration of the pair for both condition seats (round-6 cq SF-3 / spec N-2: the validator
     * had carried it as a method-local record and the data rule as two parallel locals paired by
     * convention, where a third clash source assigning the name without the origin would have been an NPE
     * on the refusal path — the silent-degradation machinery defeated by a null). Both halves are required:
     * the compact constructor refuses a null (witnessed in a18).
     */
    public record Claim(String canonical, ClaimOrigin origin) {
        public Claim {
            Objects.requireNonNull(canonical, "a claim's canonical name");
            Objects.requireNonNull(origin, "a claim's origin");
        }
    }

    /**
     * A refusal: the emitter declining to produce output it cannot justify.
     *
     * <p>A distinct type because the generator is full of catch-alls — <b>23</b> clauses
     * in {@code src/main} broad enough to intercept a refusal (any clause naming
     * {@code Exception}, {@code RuntimeException} or {@code Throwable}, INCLUDING as one
     * alternative of a multi-catch: three read
     * {@code catch (ClassNotFoundException | LinkageError | RuntimeException e)}, and the
     * lint could not see any of them until PR #566 widened it) — and several do not
     * merely swallow: {@code FunctionGenerator} substitutes
     * {@code /* TODO: expression compilation error … *}{@code /} for the expression and
     * carries on, which is silent breakage wearing a comment. A refusal must survive all
     * of them, so every catch-all rethrows this type on sight
     * ({@code if (e instanceof SilentDegradation.Refusal r) throw r;}) and the committed
     * lint {@code scripts/ci/refusal-propagation-lint.py} fails the build if one stops.
     * Ordinary recovery — a speculative probe that legitimately declines — is unaffected,
     * because only a refusal carries this type.
     */
    public static final class Refusal extends GenerationException {
        private static final long serialVersionUID = 1L;

        private final Site site; // round-2 spec N-4 / cq N-1: an enum is serializable; the typed channel survives a round-trip

        Refusal(String message, RNode context, Site site) {
            super(message, null, context);
            this.site = site;
        }

        /**
         * The register site this refusal was raised at — the TYPED read a test or a gate makes
         * (v3.2 seat 3, round-1 cq SF-7: a refusal is identified by its class and site, never by
         * the {@code [SITE]} suffix of its message).
         */
        public Site site() {
            return site;
        }
    }

    /** How many distinct witnesses to retain per site — enough to triage, bounded for memory. */
    private static final int MAX_WITNESSES_PER_SITE = 12;

    private static final Map<Site, AtomicInteger> COUNTS = new EnumMap<>(Site.class);
    private static final Map<Site, Set<String>> WITNESSES = new EnumMap<>(Site.class);

    static {
        for (Site site : Site.values()) {
            COUNTS.put(site, new AtomicInteger());
            WITNESSES.put(site, new LinkedHashSet<>());
        }
    }

    private SilentDegradation() {
    }

    /**
     * Records one degradation.
     *
     * @param site    the seat
     * @param witness what degraded — a name, type or file, enough to find it again. Kept
     *                only up to {@link #MAX_WITNESSES_PER_SITE} distinct values per site.
     */
    public static void record(Site site, String witness) {
        COUNTS.get(site).incrementAndGet();
        if (witness == null) {
            return;
        }
        Set<String> seen = WITNESSES.get(site);
        synchronized (seen) {
            if (seen.size() < MAX_WITNESSES_PER_SITE) {
                seen.add(witness);
            }
        }
    }

    /** Current counts, every site present (zeros included — an absent site reads as "not measured"). */
    public static Map<Site, Integer> counts() {
        Map<Site, Integer> out = new EnumMap<>(Site.class);
        COUNTS.forEach((site, count) -> out.put(site, count.get()));
        return out;
    }

    /** The retained witnesses for one site, in first-seen order. */
    public static List<String> witnesses(Site site) {
        Set<String> seen = WITNESSES.get(site);
        synchronized (seen) {
            return new ArrayList<>(seen);
        }
    }

    /** Total across all sites — zero is the C4 target. */
    public static int total() {
        return COUNTS.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    /** Clears every count and witness. The harness calls this before each cell it measures. */
    public static void reset() {
        COUNTS.values().forEach(c -> c.set(0));
        WITNESSES.values().forEach(seen -> {
            synchronized (seen) {
                seen.clear();
            }
        });
    }

    /**
     * Counts the degradation AND produces the refusal to throw in its place — the C0 flip
     * from "emit something plausible" to "say what is not supported, and where".
     *
     * <p>The counter still increments, so the register keeps measuring after the flip: a
     * refusal is a counted event, and its count is the burn-down meter for the stage that
     * owns the site. The message follows the plan's refusal contract, {@code UNSUPPORTED:
     * <construct> at <file>:<line>}, and {@code JavaClassGenerator#generateClasses}
     * catches it PER ELEMENT — so the element that cannot be generated refuses, with its
     * target path attached, while every other element of the model still emits. D11
     * surfaces it through the generation-error gate it already had; no new harness
     * vocabulary was needed, which is exactly why the contract was specified this way.
     *
     * @return the exception to throw — callers write {@code throw SilentDegradation.refuse(…)}
     *         so the compiler sees the control flow
     */
    public static Refusal refuse(Site site, String construct, RNode context) {
        record(site, construct);
        return new Refusal(
                "UNSUPPORTED: " + construct + " at " + location(context)
                        + " [" + site + "] — refusing rather than emitting output that cannot"
                        + " be justified (v3.1 C0 refusal contract).",
                context, site);
    }

    /** {@code <file>:<line>} for a node, or {@code <unknown>} when the range is absent. */
    private static String location(RNode context) {
        SourceRange range = context == null ? null : context.sourceRange();
        if (range == null || range.file() == null) {
            return "<unknown>";
        }
        return range.file() + ":" + range.startLine();
    }

    /** One line, stable key order, for a receipt log: {@code SITE=n SITE=n …}. */
    public static String render() {
        StringBuilder sb = new StringBuilder();
        for (Site site : Site.values()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(site).append('=').append(COUNTS.get(site).get());
        }
        return sb.toString();
    }
}
