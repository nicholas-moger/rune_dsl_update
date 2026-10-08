package org.finos.rune.equivalence;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.rosetta.model.lib.functions.ConditionValidator;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import org.finos.rune.benchmarks.corpus.CorpusClasses;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The pair-execution driver (the plan § 4.2): compile trees for BOTH sides,
 * load each in its own classloader over the SAME runtime, execute leg-G seat
 * instances pairwise, and compare the observables — O1 structural results,
 * O2 validation outcomes (name + object + success + failure-message text +
 * path, requirement 2's byte-for-byte surface), O3 thrown behaviour
 * (exception class + message), O5 the API-delta report, plus the
 * reference-state arms (resolved / unresolved) per reference slot.
 *
 * <p>The driver binds pairs at the SEMANTIC surface — each side's instance is
 * built from the same deterministic recipe through its own loader's classes —
 * so an API-shape departure on the optimised side never breaks the driver; it
 * shows up in O5 while behaviour stays gated by the comparisons.
 *
 * <p>The divergence total covers the BLOCKING observables only (O1/O2/O3 +
 * build/arm asymmetries); O5 prints beside them as the report it is. At PR-3
 * both sides load the SAME compiled goldens (no emission family has landed),
 * so every counter's expected divergence is ZERO — the run proves the
 * machinery and prints the census (the honest-denominator law: skips are
 * counted, never silent).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class PairHarness {

    /** One side's classpath: the cell's own classes dir first, then upstream dirs. */
    public record Side(List<Path> classDirs) {}

    public record Config(String cell, Side reference, Side optimised,
                         int flipWidthCap, int populateDepth, int divergenceDetailCap) {}

    /** The census. Divergence-class counters are all EXPECTED-ZERO at PR-3. */
    public static final class Summary {
        public int metaClasses;
        public int usableTypes;
        public int notMeta;
        public int modelMissing;
        public int slotTotal;
        public int seatsEnumerated;
        public int pairsBuilt;
        public int bothSidesNull;
        public int oneSidedBuilds;
        public int o1Compared;
        public int o1Divergent;
        public int o2ResultsCompared;
        public int o2Mismatches;
        public int o3EnvelopesCompared;
        public int o3Mismatches;
        public int referenceSlots;
        public int referenceArmPairs;
        public int referenceArmDivergent;
        public int fnClasses;
        public int fnInstantiable;
        public int fnSkippedNoInstance;
        /** The function classes counted in {@link #fnSkippedNoInstance}, each with its reason —
         *  printed by the run and carried into the gate's failure message (v3.2 seat 10: the
         *  chaos-1.1.0 cell's two were unnamed by the summary line alone). */
        public final List<String> fnSkippedNoInstanceNames = new ArrayList<>();
        /** The classes under a functions / reports namespace with NO canonical {@code evaluate} on EITHER side —
         *  generated NON-FUNCTIONS (v3.2 seat 10, round 1: the A8 axis's rival-TYPE validators under
         *  {@code <ns>.functions.validation}), recorded by name and pinned as a SET by the gate (LAW 73); never a
         *  skip — {@link #fnSkippedNoInstance} is the ASYMMETRIC-regression signal. */
        public int fnNonFunctions;
        public final List<String> fnNonFunctionNames = new ArrayList<>();
        /** The {@code <function>@<arm>} ids counted in {@link #fnOneSidedOutcomes} — the SET a chaos
         *  declaration pins (LAW 73: the set, not the count; v3.2 seat 10). */
        public final List<String> fnOneSidedArms = new ArrayList<>();
        public int fnPairsExecuted;
        public int fnO1Divergent;
        public int fnO3Mismatches;
        public int fnOneSidedOutcomes;
        public ApiDeltaReport.Summary apiDelta;
        /**
         * The § 6.3 PROTECTED channel (the program plan § 7): declared-protected
         * method signatures over the SAME paired population, reported BESIDE the
         * standing public channel — its own {@code O5p} line + uncapped per-class
         * detail lines (a tranche's membership reconciliation needs every row; a
         * silent cap is the banned class). Like {@link #apiDelta}, never a term
         * in {@link #totalDivergences()} (revised requirement 1).
         */
        public ApiDeltaReport.Summary apiDeltaProtected;
        public final List<String> divergenceDetail = new ArrayList<>();

        /**
         * The BLOCKING divergence total: O1 + O2 + O3 + the build/arm
         * asymmetries + the FUNCTION-leg divergences (PR-4 — the leg that
         * actually EXECUTES the re-emitted function bodies pairwise). O5 (the
         * API-delta report) is deliberately NOT a term — the delta is measured
         * + reported, never gated (revised requirement 1); O1's union getter
         * walk detects one-sided shape differences in its own right, so the
         * gate stays sound without it.
         */
        public int totalDivergences() {
            return o1Divergent + o2Mismatches + o3Mismatches + oneSidedBuilds
                    + referenceArmDivergent
                    + fnO1Divergent + fnO3Mismatches + fnOneSidedOutcomes;
        }
    }

    /**
     * Property-gated per-type census channel ({@code -Dequiv.typeCensus=true}):
     * one line per usable type with its populate-call delta and slot width —
     * the decode instrument for any census-count drift (the instrument-first
     * discipline; the standing receipts are byte-identical with the flag off).
     * Scope note: the per-type delta covers the SEAT loop only — the
     * reference-arm builds run after the line prints, so their populates land
     * in the run totals, not in any per-type line (the per-type lines do not
     * sum to the printed total wherever reference slots exist).
     */
    private static final boolean TYPE_CENSUS = Boolean.getBoolean("equiv.typeCensus");

    /**
     * Property-gated failure-text capture channel ({@code -Dequiv.textCapture=<dir>}):
     * the census § 14f BEFORE/AFTER instrument for runtime-lane changes. Every O2
     * validation line of the run — both sides; each line embeds the validator class +
     * result identity + the failure-message text ({@code getFailureReason}) + path —
     * is collected, sorted, and written to
     * {@code <dir>/<cellLeaf>-<same|pair>-capN.txt} with its SHA-256 printed: run the
     * suite with the flag before and after a rune-runtime edit and compare the digests
     * byte-for-byte (O2 ref-vs-opt on ONE runtime proves tree-equivalence; this
     * captures the text ACROSS the runtime change). Byte-inert when unset.
     * File NAMING assumes surefire's single reused fork (this module's default):
     * the seq counter is per-JVM, so {@code forkCount>1} would interleave numbering
     * across JVMs and break before/after FILENAME pairing (content soundness — the
     * sorted digest per run — is unaffected).
     */
    private static final String TEXT_CAPTURE_DIR = System.getProperty("equiv.textCapture");
    private static final AtomicInteger TEXT_CAPTURE_SEQ = new AtomicInteger();

    private PairHarness() {}

    public static Summary run(Config cfg, PrintStream out) throws Exception {
        Summary s = new Summary();
        List<String> textCapture = TEXT_CAPTURE_DIR == null ? null : new ArrayList<>();
        try (URLClassLoader refCl = CorpusClasses.loaderOver(cfg.reference().classDirs());
             URLClassLoader optCl = CorpusClasses.loaderOver(cfg.optimised().classDirs())) {

            Path cellDir = cfg.reference().classDirs().get(0);
            List<String> metaNames = CorpusClasses.classNamesUnder(cellDir,
                    n -> n.endsWith("Meta") && n.contains(".meta.") && !n.contains("$"));
            s.metaClasses = metaNames.size();

            ValidatorFactory factory = com.google.inject.Guice.createInjector()
                    .getInstance(ValidatorFactory.Default.class);
            SeatLattice lattice = new SeatLattice(cfg.flipWidthCap());
            InstanceSynthesizer refSynth = new InstanceSynthesizer(cfg.populateDepth());
            InstanceSynthesizer optSynth = new InstanceSynthesizer(cfg.populateDepth());
            RosettaPath path = RosettaPath.valueOf("Equivalence");
            List<String> pairedModelNames = new ArrayList<>();

            for (String metaName : metaNames) {
                Class<?> refMetaClass = Class.forName(metaName, true, refCl);
                if (!RosettaMetaData.class.isAssignableFrom(refMetaClass)) { s.notMeta++; continue; }
                String modelName = metaName.replace(".meta.", ".");
                modelName = modelName.substring(0, modelName.length() - "Meta".length());
                Class<?> refModel;
                Class<?> optModel;
                try {
                    refModel = Class.forName(modelName, true, refCl);
                    optModel = Class.forName(modelName, true, optCl);
                } catch (ClassNotFoundException e) {
                    s.modelMissing++;
                    continue;
                }
                s.usableTypes++;
                pairedModelNames.add(modelName);

                RosettaMetaData refMeta = (RosettaMetaData) refMetaClass.getConstructor().newInstance();
                RosettaMetaData optMeta = (RosettaMetaData) Class.forName(metaName, true, optCl)
                        .getConstructor().newInstance();

                int refPopulateBefore = refSynth.populator().populatedCount();
                List<SeatLattice.Slot> slots = refSlotList(lattice, refModel);
                s.slotTotal += slots.size();
                for (SeatLattice.Seat seat : lattice.seats(slots)) {
                    s.seatsEnumerated++;
                    Set<String> absent = new HashSet<>(seat.absentAttributes());
                    SideOutcome ref = execute(refSynth, refModel, absent, refMeta, factory, path);
                    SideOutcome opt = execute(optSynth, optModel, absent, optMeta, factory, path);
                    comparePair(s, cfg, modelName + "@" + seat.label(), ref, opt);
                    if (textCapture != null) {
                        textCapture.addAll(ref.validation());
                        textCapture.addAll(opt.validation());
                    }
                }

                if (TYPE_CENSUS) {
                    out.printf("[PairHarness typeCensus %s] %s populates=%d slots=%d%n",
                            cfg.cell(), modelName,
                            refSynth.populator().populatedCount() - refPopulateBefore,
                            slots.size());
                }

                for (SeatLattice.Slot refSlot : lattice.referenceSlots(slots)) {
                    s.referenceSlots++;
                    String wrapperName = refSlot.paramType().getName();
                    Class<?> refWrapper;
                    Class<?> optWrapper;
                    try {
                        refWrapper = Class.forName(wrapperName, true, refCl);
                        optWrapper = Class.forName(wrapperName, true, optCl);
                    } catch (ClassNotFoundException e) {
                        continue; // wrapper outside the cell dirs: censused via slotTotal
                    }
                    for (InstanceSynthesizer.ReferenceArm arm
                            : InstanceSynthesizer.ReferenceArm.values()) {
                        Object refArm = refSynth.buildReferenceArm(refWrapper, arm);
                        Object optArm = optSynth.buildReferenceArm(optWrapper, arm);
                        if (refArm == null && optArm == null) continue;
                        s.referenceArmPairs++;
                        String d = ReflectiveDeepCompare.firstDivergence(refArm, optArm);
                        if (d != null) {
                            s.referenceArmDivergent++;
                            detail(s, cfg, "refArm " + wrapperName + "@" + arm + ": " + d);
                        }
                    }
                }
            }

            functionLeg(s, cfg, refCl, optCl, refSynth, optSynth, pairedModelNames);

            s.apiDelta = ApiDeltaReport.compare(pairedModelNames, refCl, optCl);
            s.apiDeltaProtected = ApiDeltaReport.compareDeclaredProtected(pairedModelNames,
                    refCl, optCl);
            print(cfg, s, refSynth, optSynth, out);
            if (textCapture != null) {
                writeTextCapture(cfg, textCapture, out);
            }
        }
        return s;
    }

    /**
     * THE FUNCTION PAIR-EXECUTION LEG (PR-4; the family-3 widening at PR-8): every
     * generated function class in the cell — the {@code *.functions.*} packages
     * (qualification, plain funcs, ingest, projections) AND, since the census § 12
     * rule-face emission, the {@code *.reports.*} classes (the {@code <Name>Rule}
     * classes = the SUBSTITUTED family-3 surface, plus the {@code
     * <Name>ReportFunction} classes = the byte-identical composition roots whose
     * {@code @Inject}ed rules differ per side — executing them is the family's
     * INTEGRATION arm: every reported field runs its converted rule through the
     * production dispatch) — is Guice-instantiated on BOTH sides and its canonical
     * {@code evaluate} invoked over deterministically synthesized inputs, three arms
     * per function:
     *
     * <ol>
     *   <li><b>strict + populated</b> — the production condition contract
     *       ({@code DefaultConditionValidator}, throwing): outputs deep-compared
     *       (O1), thrown envelopes byte-compared (O3 — incl. condition-failure
     *       message text, requirement 2's surface at the function altitude);</li>
     *   <li><b>strict + all-null args</b> — the null-propagation probe (the direct
     *       ladder's short-circuit vs the Mapper error-item chain);</li>
     *   <li><b>lenient + populated</b> — {@link LenientConditionValidator}
     *       evaluates-but-never-throws, so operation bodies execute even where the
     *       synthesized inputs violate preconditions (the coverage arm; outcomes
     *       still compared strictly — lenient is never the oracle).</li>
     * </ol>
     *
     * <p>Inputs are built per SIDE from that side's own classes (the two-synthesizer
     * pair discipline) via the deterministic populator; {@code List}-typed parameters
     * take a singleton of the generic element. Instantiation failures are RECORDED
     * ({@code fnSkippedNoInstance}), never silently skipped. The function classes
     * join the O5 report population, so any signature delta of a re-emitted class is
     * published per family PR.
     */
    private static void functionLeg(Summary s, Config cfg, URLClassLoader refCl,
            URLClassLoader optCl, InstanceSynthesizer refSynth, InstanceSynthesizer optSynth,
            List<String> pairedModelNames) {
        Path cellDir = cfg.reference().classDirs().get(0);
        // v3.2 seat 10 (chaos-1.1.0, D49): the A8 output-package axis declares a rival TYPE inside
        // `<ns>.functions`, so that type's validators land under `.functions.validation.` — generated
        // classes with no `evaluate`, not functions (the c2 / g2 pair runs counted C4AddTypeFormatValidator
        // and C32TwinTypeFormatValidator as two symmetric instantiation failures). Round 1 (cq SF-7): the
        // first cut excluded them by NAME; the STRUCTURAL rule decides now — a class with no canonical
        // evaluate on EITHER side is a NON-FUNCTION, recorded by name and pinned as a set by the gate,
        // while a canonical evaluate on ONE side alone stays the hard-zero skip (classifyEvaluate).
        List<String> fnNames = CorpusClasses.classNamesUnder(cellDir,
                n -> (n.contains(".functions.") || n.contains(".reports."))
                        && !n.contains("$"));
        s.fnClasses = fnNames.size();
        if (fnNames.isEmpty()) {
            return;
        }
        Injector strict = Guice.createInjector();
        Injector lenient = Guice.createInjector(
                binder -> binder.bind(ConditionValidator.class)
                        .toInstance(new LenientConditionValidator()));

        for (String fnName : fnNames) {
            Class<?> refFn;
            Class<?> optFn;
            try {
                refFn = Class.forName(fnName, true, refCl);
                optFn = Class.forName(fnName, true, optCl);
            } catch (ClassNotFoundException | LinkageError e) {
                s.fnSkippedNoInstance++;
                s.fnSkippedNoInstanceNames.add(fnName + " [class not loadable: " + e + "]");
                continue;
            }
            Method refEval = canonicalEvaluate(refFn);
            Method optEval = canonicalEvaluate(optFn);
            FunctionLegClass legClass = classifyEvaluate(refEval, optEval);
            if (legClass == FunctionLegClass.NON_FUNCTION_BOTH_SIDES) {
                s.fnNonFunctions++;
                s.fnNonFunctionNames.add(fnName);
                continue;
            }
            if (legClass == FunctionLegClass.ASYMMETRIC) {
                s.fnSkippedNoInstance++;
                s.fnSkippedNoInstanceNames.add(fnName + " [no canonical evaluate on ONE side: ref="
                        + (refEval != null) + " opt=" + (optEval != null) + "]");
                continue;
            }
            Object[] refStrictInstance = tryInstance(strict, refFn);
            Object[] optStrictInstance = tryInstance(strict, optFn);
            Object[] refLenientInstance = tryInstance(lenient, refFn);
            Object[] optLenientInstance = tryInstance(lenient, optFn);
            if (refStrictInstance == null || optStrictInstance == null
                    || refLenientInstance == null || optLenientInstance == null) {
                s.fnSkippedNoInstance++;
                s.fnSkippedNoInstanceNames.add(fnName + " [not instantiable: refStrict="
                        + (refStrictInstance != null) + " optStrict=" + (optStrictInstance != null)
                        + " refLenient=" + (refLenientInstance != null) + " optLenient="
                        + (optLenientInstance != null) + "]");
                continue;
            }
            s.fnInstantiable++;
            pairedModelNames.add(fnName);

            // arm 1: strict + populated
            executeFnPair(s, cfg, fnName + "@strictPopulated",
                    refStrictInstance[0], refEval, buildArgs(refSynth, refEval, cfg, false),
                    optStrictInstance[0], optEval, buildArgs(optSynth, optEval, cfg, false));
            // arm 2: strict + all-null (the null-propagation probe)
            executeFnPair(s, cfg, fnName + "@strictNullArgs",
                    refStrictInstance[0], refEval, buildArgs(refSynth, refEval, cfg, true),
                    optStrictInstance[0], optEval, buildArgs(optSynth, optEval, cfg, true));
            // arm 3: lenient + populated (the coverage arm)
            executeFnPair(s, cfg, fnName + "@lenientPopulated",
                    refLenientInstance[0], refEval, buildArgs(refSynth, refEval, cfg, false),
                    optLenientInstance[0], optEval, buildArgs(optSynth, optEval, cfg, false));
        }
    }

    /** Guice instance boxed in a length-1 array, or {@code null} on any provision failure. */
    private static Object[] tryInstance(Injector injector, Class<?> fnClass) {
        try {
            return new Object[] { injector.getInstance(fnClass) };
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * The function leg's verdict on one class pair (v3.2 seat 10, round 1): a class under a functions namespace
     * is a FUNCTION when both sides carry a canonical {@code evaluate}; a NON-FUNCTION when neither does (a rival
     * TYPE's validator or meta under {@code <ns>.functions} — the A8 axis — recorded by name, pinned as a set by the
     * gate); ASYMMETRIC when exactly one does — the regression the skip counter's hard zero exists for. The pure
     * half of the leg; its corpus-free witness is {@code PairHarnessFunctionLegClassTest}.
     */
    enum FunctionLegClass { FUNCTION, NON_FUNCTION_BOTH_SIDES, ASYMMETRIC }

    static FunctionLegClass classifyEvaluate(Method refEval, Method optEval) {
        if (refEval != null && optEval != null) {
            return FunctionLegClass.FUNCTION;
        }
        if (refEval == null && optEval == null) {
            return FunctionLegClass.NON_FUNCTION_BOTH_SIDES;
        }
        return FunctionLegClass.ASYMMETRIC;
    }

    /**
     * The canonical {@code evaluate}: declared methods named {@code evaluate}, widest
     * parameter list first, then a stable toString tie-break (the rune-benchmarks
     * deterministic-overload-pick discipline).
     */
    private static Method canonicalEvaluate(Class<?> fnClass) {
        return java.util.Arrays.stream(fnClass.getMethods())
                .filter(m -> "evaluate".equals(m.getName()))
                .max(java.util.Comparator
                        .comparingInt(Method::getParameterCount)
                        .thenComparing(Method::toString, java.util.Comparator.reverseOrder()))
                .orElse(null);
    }

    /** Deterministic args for one side: populated via the side's synthesizer, or all-null. */
    private static Object[] buildArgs(InstanceSynthesizer synth, Method evaluate, Config cfg,
            boolean allNull) {
        Class<?>[] params = evaluate.getParameterTypes();
        java.lang.reflect.Type[] generics = evaluate.getGenericParameterTypes();
        Object[] args = new Object[params.length];
        if (allNull) {
            return args;
        }
        for (int i = 0; i < params.length; i++) {
            if (List.class.isAssignableFrom(params[i])) {
                Class<?> element = listElementType(generics[i]);
                Object v = element == null ? null
                        : synth.populator().valueFor(element, cfg.populateDepth());
                args[i] = v == null ? List.of() : List.of(v);
            } else {
                args[i] = synth.populator().valueFor(params[i], cfg.populateDepth());
            }
        }
        return args;
    }

    private static Class<?> listElementType(java.lang.reflect.Type generic) {
        if (generic instanceof java.lang.reflect.ParameterizedType pt
                && pt.getActualTypeArguments().length == 1) {
            java.lang.reflect.Type arg = pt.getActualTypeArguments()[0];
            if (arg instanceof Class<?> c) {
                return c;
            }
            if (arg instanceof java.lang.reflect.WildcardType wt
                    && wt.getUpperBounds().length == 1
                    && wt.getUpperBounds()[0] instanceof Class<?> ub) {
                return ub;
            }
        }
        return null;
    }

    /** Invoke one function pair on one arm and compare outcomes (O1 value / O3 envelope). */
    private static void executeFnPair(Summary s, Config cfg, String armId,
            Object refInstance, Method refEval, Object[] refArgs,
            Object optInstance, Method optEval, Object[] optArgs) {
        Object refValue = null;
        Object optValue = null;
        String refThrown = null;
        String optThrown = null;
        try {
            refValue = refEval.invoke(refInstance, refArgs);
        } catch (Throwable t) {
            refThrown = envelope(t);
        }
        try {
            optValue = optEval.invoke(optInstance, optArgs);
        } catch (Throwable t) {
            optThrown = envelope(t);
        }
        s.fnPairsExecuted++;
        if ((refThrown == null) != (optThrown == null)) {
            s.fnOneSidedOutcomes++;
            s.fnOneSidedArms.add(armId);
            detail(s, cfg, "fn " + armId + ": one-sided outcome — ref="
                    + (refThrown != null ? refThrown : "value") + " opt="
                    + (optThrown != null ? optThrown : "value"));
            return;
        }
        if (refThrown != null) {
            if (!refThrown.equals(optThrown)) {
                s.fnO3Mismatches++;
                detail(s, cfg, "fn " + armId + ": thrown mismatch — ref=" + refThrown
                        + " opt=" + optThrown);
            }
            return;
        }
        String d = ReflectiveDeepCompare.firstDivergence(refValue, optValue);
        if (d != null) {
            s.fnO1Divergent++;
            detail(s, cfg, "fn " + armId + ": " + d);
        }
    }

    /** One side's seat execution: the built instance + validation summary + thrown envelope. */
    private record SideOutcome(Object instance, List<String> validation, String thrown) {}

    private static SideOutcome execute(InstanceSynthesizer synth, Class<?> model,
                                       Set<String> absent, RosettaMetaData meta,
                                       ValidatorFactory factory, RosettaPath path) {
        Object instance;
        try {
            instance = synth.buildAtSeat(model, absent);
        } catch (Throwable t) {
            return new SideOutcome(null, List.of(), envelope(t));
        }
        if (instance == null) {
            return new SideOutcome(null, List.of(), null);
        }
        List<String> lines = new ArrayList<>();
        String thrown = null;
        try {
            Validator cardinality = meta.validator(factory);
            Validator typeFormat = meta.typeFormatValidator(factory);
            List<Validator> dataRules = meta.dataRules(factory);
            summarize(lines, cardinality, path, instance);
            summarize(lines, typeFormat, path, instance);
            for (Validator dr : dataRules) {
                summarize(lines, dr, path, instance);
            }
        } catch (Throwable t) {
            thrown = envelope(t);
        }
        return new SideOutcome(instance, lines, thrown);
    }

    /** O2's comparable line: validator + result identity incl. the message text + path. */
    private static void summarize(List<String> lines, Validator v, RosettaPath path, Object instance) {
        List<ValidationResult<?>> results =
                v.getValidationResults(path, (com.rosetta.model.lib.RosettaModelObject) instance);
        for (ValidationResult<?> r : results) {
            lines.add(v.getClass().getSimpleName() + "|" + r.getName() + "|"
                    + r.getModelObjectName() + "|" + r.isSuccess() + "|"
                    + r.getFailureReason().orElse("") + "|" + r.getPath());
        }
    }

    private static String envelope(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && (cause instanceof java.lang.reflect.InvocationTargetException
                || cause instanceof RuntimeException && cause.getMessage() == null)) {
            cause = cause.getCause();
        }
        return cause.getClass().getName() + ": " + cause.getMessage();
    }

    private static void comparePair(Summary s, Config cfg, String seatId,
                                    SideOutcome ref, SideOutcome opt) {
        if (ref.instance() == null && opt.instance() == null && ref.thrown() == null
                && opt.thrown() == null) {
            s.bothSidesNull++;
            return;
        }
        if ((ref.instance() == null) != (opt.instance() == null)) {
            s.oneSidedBuilds++;
            detail(s, cfg, seatId + ": one-sided build (ref="
                    + (ref.instance() != null) + ", opt=" + (opt.instance() != null) + ")");
            return;
        }
        s.pairsBuilt++;
        if (ref.instance() != null) {
            s.o1Compared++;
            String d = ReflectiveDeepCompare.firstDivergence(ref.instance(), opt.instance());
            if (d != null) {
                s.o1Divergent++;
                detail(s, cfg, seatId + " O1: " + d);
            }
        }
        s.o2ResultsCompared += Math.max(ref.validation().size(), opt.validation().size());
        if (!ref.validation().equals(opt.validation())) {
            s.o2Mismatches++;
            detail(s, cfg, seatId + " O2: validation summaries differ (ref "
                    + ref.validation().size() + " lines, opt " + opt.validation().size() + ")");
        }
        s.o3EnvelopesCompared++;
        boolean thrownEqual = ref.thrown() == null ? opt.thrown() == null
                : ref.thrown().equals(opt.thrown());
        if (!thrownEqual) {
            s.o3Mismatches++;
            detail(s, cfg, seatId + " O3: thrown differs (ref=" + ref.thrown()
                    + ", opt=" + opt.thrown() + ")");
        }
    }

    private static void detail(Summary s, Config cfg, String line) {
        if (s.divergenceDetail.size() < cfg.divergenceDetailCap()) {
            s.divergenceDetail.add(line);
        }
    }

    /** The § 14f capture write: sorted O2 lines (both sides) → file + printed SHA-256. */
    private static void writeTextCapture(Config cfg, List<String> lines, PrintStream out)
            throws Exception {
        Path dir = Path.of(TEXT_CAPTURE_DIR);
        Files.createDirectories(dir);
        String leaf = cfg.cell().substring(cfg.cell().lastIndexOf('/') + 1);
        String mode = cfg.reference().classDirs().equals(cfg.optimised().classDirs())
                ? "same" : "pair";
        Path file = dir.resolve(leaf + "-" + mode + "-cap"
                + TEXT_CAPTURE_SEQ.incrementAndGet() + ".txt");
        List<String> sorted = new ArrayList<>(lines);
        Collections.sort(sorted);
        StringBuilder sb = new StringBuilder();
        for (String line : sorted) {
            sb.append(line).append('\n');
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        Files.write(file, bytes);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        out.printf("[PairHarness textCapture %s] file=%s lines=%d sha256=%s%n",
                cfg.cell(), file.getFileName(), sorted.size(), hex);
    }

    private static List<SeatLattice.Slot> refSlotList(SeatLattice lattice, Class<?> model)
            throws Exception {
        Object builder = model.getMethod("builder").invoke(null);
        return lattice.slots(builder.getClass());
    }

    private static void print(Config cfg, Summary s, InstanceSynthesizer refSynth,
                              InstanceSynthesizer optSynth, PrintStream out) {
        out.printf("[PairHarness %s] metaClasses=%d usableTypes=%d notMeta=%d modelMissing=%d "
                        + "slotTotal=%d flipWidthCap=%d seatsEnumerated=%d pairsBuilt=%d "
                        + "bothSidesNull=%d oneSidedBuilds=%d%n",
                cfg.cell(), s.metaClasses, s.usableTypes, s.notMeta, s.modelMissing,
                s.slotTotal, cfg.flipWidthCap(), s.seatsEnumerated, s.pairsBuilt,
                s.bothSidesNull, s.oneSidedBuilds);
        out.printf("[PairHarness %s] O1 compared=%d divergent=%d | O2 resultsCompared=%d "
                        + "mismatches=%d | O3 envelopes=%d mismatches=%d%n",
                cfg.cell(), s.o1Compared, s.o1Divergent, s.o2ResultsCompared, s.o2Mismatches,
                s.o3EnvelopesCompared, s.o3Mismatches);
        out.printf("[PairHarness %s] referenceSlots=%d refArmPairs=%d refArmDivergent=%d "
                        + "| O5 classes=%d withDelta=%d refOnlySigs=%d optOnlySigs=%d%n",
                cfg.cell(), s.referenceSlots, s.referenceArmPairs, s.referenceArmDivergent,
                s.apiDelta.classesCompared(), s.apiDelta.classesWithDelta(),
                s.apiDelta.referenceOnlySignatures(), s.apiDelta.optimisedOnlySignatures());
        // The § 6.3 protected channel (its OWN line — the standing O5 line above
        // stays byte-comparable with every banked receipt), then EVERY per-class
        // delta row: the tranche membership reconciliation reads them all.
        out.printf("[PairHarness %s] O5p protected: classes=%d withDelta=%d refOnlySigs=%d "
                        + "optOnlySigs=%d%n",
                cfg.cell(), s.apiDeltaProtected.classesCompared(),
                s.apiDeltaProtected.classesWithDelta(),
                s.apiDeltaProtected.referenceOnlySignatures(),
                s.apiDeltaProtected.optimisedOnlySignatures());
        for (ApiDeltaReport.ClassDelta d : s.apiDeltaProtected.deltas()) {
            out.printf("[PairHarness %s] O5p DELTA %s refOnly=%s optOnly=%s%n",
                    cfg.cell(), d.className(), d.referenceOnly(), d.optimisedOnly());
        }
        for (String skipped : s.fnSkippedNoInstanceNames) {
            out.printf("[PairHarness %s] fnSkippedNoInstance %s%n", cfg.cell(), skipped);
        }
        for (String nonFunction : s.fnNonFunctionNames) {
            out.printf("[PairHarness %s] fnNonFunction %s%n", cfg.cell(), nonFunction);
        }
        out.printf("[PairHarness %s] fnClasses=%d instantiable=%d skippedNoInstance=%d nonFunctions=%d "
                        + "fnPairs=%d fnO1Divergent=%d fnO3Mismatches=%d fnOneSided=%d%n",
                cfg.cell(), s.fnClasses, s.fnInstantiable, s.fnSkippedNoInstance, s.fnNonFunctions,
                s.fnPairsExecuted, s.fnO1Divergent, s.fnO3Mismatches, s.fnOneSidedOutcomes);
        out.printf("[PairHarness %s] populators: ref ok=%d failed=%d rejections=%d | "
                        + "opt ok=%d failed=%d rejections=%d | TOTAL divergences=%d%n",
                cfg.cell(), refSynth.populator().populatedCount(),
                refSynth.populator().failedCount(),
                refSynth.populator().setterRejectionCount(),
                optSynth.populator().populatedCount(), optSynth.populator().failedCount(),
                optSynth.populator().setterRejectionCount(), s.totalDivergences());
        for (String d : s.divergenceDetail) {
            out.println("[PairHarness " + cfg.cell() + "] DIVERGENCE " + d);
        }
    }
}
