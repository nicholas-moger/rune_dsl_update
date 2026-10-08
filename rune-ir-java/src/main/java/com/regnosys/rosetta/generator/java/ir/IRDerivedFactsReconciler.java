package com.regnosys.rosetta.generator.java.ir;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.supporting.RTypeParameter;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.SilentDegradation;
import com.regnosys.rosetta.generator.java.object.ConditionCases;
import com.regnosys.rosetta.generator.java.object.ModelMetaGenerator;
import com.regnosys.rosetta.generator.java.object.deeppath.DeepPathScan;
import com.regnosys.rosetta.generator.java.object.deeppath.DeepPathUtilGenerator;
import com.regnosys.rosetta.generator.java.object.validators.TypeFormatConstraintScan;
import com.regnosys.rosetta.generator.java.object.validators.TypeFormatValidatorGenerator;
import com.regnosys.rosetta.generator.java.object.validators.ValidatorScan;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RGeneratedJavaClass;
import com.regnosys.rosetta.generator.java.types.RJavaPojoInterface;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RStringType;
import com.regnosys.rosetta.types.RType;

/**
 * THE DERIVED-FILE FACT RECONCILE (v3.3 seat 8, PR #644 - THE PROPERTY GATE, {@code PLAN.md} § D families D01-D13).
 *
 * <p>Every per-type DERIVED file the data-type emitter will write as ONE unit with the POJO - the
 * {@code *TypeFormatValidator}, the {@code *CardinalityValidator}, the {@code *OnlyExistsValidator}, the {@code *Meta}
 * and the deep-path util - stands on facts that are NOT the POJO's. This reconciler asserts each of them, per
 * validated element (a {@code Data} or a {@code choice}), TWO WAYS (LAW 69):
 *
 * <ul>
 *   <li>the SOURCE half, WHICH IS THIS CLASS'S OWN, reads the OLD GENERATOR'S OWN decision -
 *       {@code ValidatorScan.scan} (the member list, the cardinality bounds, the {@code isFullyUnbounded} skip and the
 *       synthetic-{@code meta} exclusion), {@code TypeFormatConstraintScan.constrainedBasic} (the envelope after alias
 *       stripping), {@code TypeFormatValidatorGenerator.declaredTypesByPropertyName} (the property-to-declared-type-call
 *       join), {@code ConditionCases} + {@code ModelMetaGenerator.unnamedConditionKind} and
 *       {@code ModelMetaGenerator.collectConditionRefs} (the condition-class naming),
 *       {@code DeepPathScan} (eligibility, the feature map AND its iteration order) and
 *       {@code ValidatorScan.collidesWithJavaLang} - reached where needed through the reconcile seams this PR opened
 *       in {@code object/validators} and in {@code object/ModelMetaGenerator} (visibility only, named in each seam's
 *       javadoc);</li>
 *   <li>the IR half, WHICH IS {@link IRDerivedFacts}, computes the same fact from {@link IRTypeNode}, the property
 *       model and {@link IRTypeIndex} ALONE, never from an AST node. Where the fact is a WALK rather than a read - the
 *       alias chain, the condition chain, the meta-annotation union over an override, the deep-feature map - this
 *       class walks the SOURCE side ITSELF too, so the adapter is one producer and this class is the other and a
 *       dropped rung is red.</li>
 * </ul>
 *
 * <p><b>THE IR HALF MOVED OUT (v3.3 seat 9, PR #645 commit 12).</b> The derivations were private methods HERE while
 * nothing but this class read them; the three validator emitters read them now, and an emitter that called a method of
 * this class would compile against a {@code GeneratorModel} and three generator seams it must never touch. They are
 * RELOCATED to {@link IRDerivedFacts}, unchanged in law - not copied - so there is still exactly ONE declaration of
 * each and this reconcile still compares the emitter's own answer with the generator's own answer. The test seam moved
 * with them: {@link IRDerivedLie} is now a top-level enum and {@link #lie} hands its value to the facts.
 *
 * <p><b>The alias chain.</b> {@code ir.aliasChain()} is the adapter's walk; the ORACLE beside it is this class's own
 * walk of the source chain under {@code TypeFormatConstraintScan.aliasHierarchy}'s law (a seen-set, a depth bound of
 * {@value #MAX_ALIAS_DEPTH}, the referenced type resolved through the workspace) - and that walk is ALSO asserted
 * equal to the scan's own answer, so three reads agree before the emitter writes a byte. From the chain comes the
 * refusal that withholds a WHOLE validator file ({@code TypeFormatValidatorGenerator.java:240-260}): any rung that is
 * parameterised AND carries a condition refuses the type-format validator of every attribute of the element, not just
 * its own. That predicate is a fact here ({@code typeFormat.<p>.refusesValidator} and the element's
 * {@code typeFormat.refusesValidator}), because an emitter that reproduced the checks but not the refusal would write
 * a file where the old generator wrote none.
 *
 * <p><b>The deep-path ORDER ({@code deepPath.order}, lane D13, risk R6).</b> {@code DeepPathScan.findDeepFeatureMap}
 * returns a FRESH {@code java.util.HashMap} whose iteration order IS the golden {@code choose*} method order
 * ({@code DeepPathUtilGenerator.java:36-77}). On 151 of the 836 eligible vendored elements that order is neither
 * sorted nor a declaration subsequence - it is a pure JDK hash artefact. The IR half therefore does NOT sort or
 * re-derive the order: it performs the SAME sequence of {@code put} / {@code removeIf} operations on its own
 * {@code HashMap}, with the same keys in the same insertion sequence, and asserts the joined iteration order equal.
 * <b>This is the ONE non-model law the data-type emitter carries</b> - {@code String.hashCode} is JLS-specified
 * and {@code HashMap}'s table walk is the same OpenJDK code the old generator runs, so the reproduction is exact on
 * the JDK the build pins, and nowhere else.
 *
 * <p>The counters - declarations ATTEMPTED, facts asserted, mismatches - are what the D11 host prints per cell before
 * it asserts {@code mismatches == 0}; {@link #attempt()} books a declaration BEFORE the producing half runs and
 * {@link #threw()} books a throw AS a mismatch, so no element leaves the population silently (LAW 84).
 */
final class IRDerivedFactsReconciler {

    /** The alias walk's depth bound - {@code TypeFormatConstraintScan.MAX_DEPTH}'s own. */
    static final int MAX_ALIAS_DEPTH = 100;

    /** The supertype-chain walk's depth bound - {@link IRDerivedFacts}'s own, so both halves bound alike. */
    private static final int MAX_CHAIN_DEPTH = IRDerivedFacts.MAX_CHAIN_DEPTH;

    private final GeneratorModel generatorModel;
    /**
     * The workspace-wide MODEL index - the IR half of the qualify wing (v3.3 seat 9, PR #645 commit 13). A
     * CONSTRUCTOR argument and never a nullable setter, for {@link IRModelReconciler}'s own reason (round 1 of
     * PR #644, MF-2): an absent index would silently make every type a non-root and reconcile two empty lists.
     */
    private final IRModelIndex modelIndex;
    private final JavaTypeTranslator typeTranslator;
    private final JavaTypeUtil typeUtil;
    /** Held ONLY for its {@code declaredTypesByPropertyName} reconcile seam - no emission path runs through it. */
    private final TypeFormatValidatorGenerator declaredTypeSeam;
    /** Held ONLY for its {@code collectConditionRefs} reconcile seam - no emission path runs through it either. */
    private final ModelMetaGenerator conditionRefSeam;
    private final DeepPathScan deepPathScan;
    /**
     * Held ONLY for its {@code dependenciesOf} / {@code armsOf} reconcile seams (v3.3 seat 9, PR #645 commit 14)
     * - no emission path runs through it; it renders nothing here.
     */
    private final DeepPathUtilGenerator deepPathSeam;

    private final AtomicInteger declarations = new AtomicInteger();
    private final AtomicInteger facts = new AtomicInteger();
    private final AtomicInteger mismatches = new AtomicInteger();

    /** The emitter's OWN check counts, for the host's TWO-READS third read: {typeFormat, cardinality, onlyExists}. */
    private final AtomicInteger typeFormatChecks = new AtomicInteger();
    /** The type-format checks the IR half walked, PER ELEMENT (keyed by the node's qualified name) - see {@link #typeFormatChecksByElement()}. */
    private final Map<String, Integer> typeFormatChecksByElement = new java.util.concurrent.ConcurrentHashMap<>();
    private final AtomicInteger cardinalityChecks = new AtomicInteger();
    private final AtomicInteger onlyExistsMembers = new AtomicInteger();

    /** The TWO FACT FAMILIES commit 12 adds, counted apart so the DERIVED line can print its carried sub-count. */
    private final AtomicInteger aliasConditionClassFacts = new AtomicInteger();
    private final AtomicInteger castTypeFacts = new AtomicInteger();
    /** The THIRD family, commit 13's: one fact per validated element - the {@code *Meta}'s qualify wing. */
    private final AtomicInteger qualifyFunctionFacts = new AtomicInteger();
    /**
     * The FOURTH and FIFTH families, commit 14's: one fact per validated element each - the deep-path util's
     * INJECTED DEPENDENCIES in their {@code HashSet} iteration order, and its ARM per (deep feature x
     * alternative) in render order.
     */
    private final AtomicInteger deepPathDependencyFacts = new AtomicInteger();
    private final AtomicInteger deepPathArmFacts = new AtomicInteger();

    private IRDerivedLie lie = IRDerivedLie.NONE;

    /**
     * @param modelIndex the workspace-wide model index over the PASS'S OWN adapter - the qualify wing's IR half
     *     reads it, and it is required non-null for {@link IRModelReconciler}'s reason (no silent fallback)
     */
    IRDerivedFactsReconciler(GeneratorModel generatorModel, IRModelIndex modelIndex) {
        this.generatorModel = Objects.requireNonNull(generatorModel, "generatorModel");
        this.modelIndex = Objects.requireNonNull(modelIndex, "modelIndex");
        this.typeUtil = new JavaTypeUtil();
        this.typeTranslator = new JavaTypeTranslator(typeUtil);
        this.declaredTypeSeam = new TypeFormatValidatorGenerator(generatorModel, typeTranslator, typeUtil);
        this.conditionRefSeam = new ModelMetaGenerator(generatorModel, typeTranslator);
        this.deepPathScan = new DeepPathScan(generatorModel);
        this.deepPathSeam = new DeepPathUtilGenerator(generatorModel, typeTranslator, typeUtil);
    }

    /** TEST SEAM (see {@link IRDerivedLie}) - disables one named law of one half. No production caller. */
    void lie(IRDerivedLie lie) {
        this.lie = Objects.requireNonNull(lie, "lie");
    }

    /** Books ONE validated element as attempted - BEFORE either half runs, so a throwing element is still counted. */
    void attempt() {
        declarations.incrementAndGet();
    }

    /** A throw on either half IS a mismatch - never a silent drop from the counters. */
    void threw() {
        mismatches.incrementAndGet();
    }

    /** {@code {elements attempted, facts asserted, mismatches}}. */
    int[] stats() {
        return new int[] {declarations.get(), facts.get(), mismatches.get()};
    }

    /**
     * The type-format checks the IR half walked for each reconciled element, keyed by the node's qualified name -
     * the host's {@code DERIVED TWO-READS} line subtracts the entries of the elements whose validator file the
     * generator REFUSED (by the refusals' own target paths) and holds the written text against the remainder.
     */
    Map<String, Integer> typeFormatChecksByElement() {
        return java.util.Collections.unmodifiableMap(typeFormatChecksByElement);
    }

    /**
     * The IR half's OWN count of the checks the derived validators would emit, for the host's TWO-READS line:
     * {@code {type-format checks over the elements the alias-condition refusal does NOT withhold, cardinality checks,
     * only-exists members}}. The host holds these against the literal {@code checkString(} / {@code checkNumber(} /
     * {@code checkCardinality(} / {@code .isSet((} tokens in the text the cell actually WROTE.
     */
    int[] checkCounts() {
        return new int[] {typeFormatChecks.get(), cardinalityChecks.get(), onlyExistsMembers.get()};
    }

    /**
     * {@code {aliasConditionClasses, castType, qualifyFunctions, deepPathDependencies, deepPathArms}} - the fact
     * families v3.3 seat 9, PR #645 commits 12, 13 and 14 ADD. The D11 DERIVED line prints
     * {@code factsAsserted=<total> (carried=<old> + aliasConditionClasses=<n1> + castType=<n2> +
     * qualifyFunctions=<n3> + deepPathDependencies=<n4> + deepPathArms=<n5>)}, and {@code carried}, {@code n1},
     * {@code n2} and {@code n3} MUST read the commit-13 figures of the cell: the two new families are the ONLY
     * thing that may move the total at this commit, so a sub-count that drifts is a fact this commit moved
     * without saying so.
     */
    int[] newFactCounts() {
        return new int[] {aliasConditionClassFacts.get(), castTypeFacts.get(), qualifyFunctionFacts.get(),
                deepPathDependencyFacts.get(), deepPathArmFacts.get()};
    }

    // ============================================================================================ the reconcile

    /**
     * Every derived-file fact of ONE validated element, asserted both ways.
     *
     * @param namespace  the declaring model's namespace
     * @param ast        the element - an {@code RDataType} or an {@code RChoice}
     * @param ir         its node, the SAME instance the emitter will read
     * @param properties its property model (the POJO surface, PR #644 commit 6)
     * @param index      the workspace-wide type index - the IR half's only route to another declaration
     * @return the mismatch messages, each naming its fact family
     */
    List<String> reconcile(String namespace, RRootElement ast, IRTypeNode ir, IRPropertyModel properties,
            IRTypeIndex index) {
        String kindToken = ast instanceof RChoice ? "CHOICE" : "STRUCT";
        Check c = new Check("DERIVED " + kindToken + " " + ir.name());
        // THE IR HALF, one instance for this element - the SAME class the three validator emitters read, built with
        // this reconciler's lie so every lane (D01-D14) still reads RED on its family (v3.3 seat 9, PR #645 commit 12)
        IRDerivedFacts irFacts = new IRDerivedFacts(index, lie);

        // ---- the two member lists -------------------------------------------------------------------------
        RJavaPojoInterface pojo = ValidatorScan.toPojo(ast, generatorModel, typeTranslator, typeUtil);
        List<ValidatorScan.ScannedAttribute> scanned =
                ValidatorScan.scan(ast, pojo, generatorModel, typeUtil);
        List<IRPropertyModel.IRProperty> members = irFacts.members(properties);

        c.same("onlyExists.size", scanned.size(), members.size());
        c.same("onlyExists.members",
                IRDerivedFacts.join(scanned.stream().map(ValidatorScan.ScannedAttribute::name).toList()),
                IRDerivedFacts.join(members.stream().map(IRPropertyModel.IRProperty::name).toList()));
        c.same("onlyExists.syntheticMetaSkipped", pojo.getAllProperties().size() - scanned.size(),
                properties.allProperties().size() - members.size());
        onlyExistsMembers.addAndGet(members.size());

        List<IRTypeNode> chain = irFacts.chain(ir);
        Map<String, IRDerivedFacts.Owned> effective = IRDerivedFacts.effectiveAttributes(chain);
        Map<String, IRField> declaredByProperty = IRDerivedFacts.declaredFieldsByProperty(ir, chain);

        // ---- D03: the cardinality bounds and the fully-unbounded skip --------------------------------------
        int sourceChecked = 0;
        int irChecked = 0;
        for (int i = 0; i < Math.min(scanned.size(), members.size()); i++) {
            ValidatorScan.ScannedAttribute source = scanned.get(i);
            IRPropertyModel.IRProperty member = members.get(i);
            String at = "cardinality." + source.name() + ".";
            int[] bounds = IRDerivedFacts.bounds(member, effective);
            c.same(at + "bounds", source.min() + ".." + source.max(), bounds[0] + ".." + bounds[1]);
            boolean irSkipped = bounds[2] == 1;
            if (lie == IRDerivedLie.CARDINALITY_SKIP_INVERTED) {
                irSkipped = !irSkipped;
            }
            c.same(at + "skipped", source.isFullyUnbounded(), irSkipped);
            // v3.3 seat 9 (PR #645 commit 12) - THE CAST TEXT, the emitters' own. The old scan's answer through
            // the ValidatorScan.castType seam against IRDataTypeEmitter.interfaceGetterType, which is what the
            // three validator emitters render at every cast site AND what the POJO member writes at its getter.
            // ONE assertion per member serves both validator families: the type-format wing's local type is the
            // same text by construction (its plan reads the very same scan field).
            c.same(at + "castType", ValidatorScan.castType(source.prop(), typeUtil),
                    IRDataTypeEmitter.interfaceGetterType(member));
            castTypeFacts.incrementAndGet();
            if (!source.isFullyUnbounded()) {
                sourceChecked++;
            }
            if (!irSkipped) {
                irChecked++;
            }
        }
        c.same("cardinality.size", sourceChecked, irChecked);
        cardinalityChecks.addAndGet(irChecked);

        // ---- D01 / D02: the type-format envelope, the alias chain and the whole-validator refusal ----------
        Map<String, TypeFormatValidatorGenerator.DeclaredType> declaredTypes =
                declaredTypeSeam.declaredTypesByPropertyName(ast);
        // v3.3 seat 9 (PR #645 commit 12) - THE WIRED CONDITION CLASSES, the wing's own. The generator's OWN
        // decision through the wiredConditionClasses seam (the very list buildModel renders) against the IR's
        // alias-link law. The seam REFUSES the whole element for a parameterised alias carrying a condition, and
        // so does the IR - so the fact compares "REFUSED" against "REFUSED" there rather than dropping the
        // element out of the family: a refusal is an answer, not an absence.
        Map<String, List<String>> sourceWired = null;
        boolean sourceWiringRefused = false;
        try {
            Map<String, List<RGeneratedJavaClass<?>>> wired = declaredTypeSeam.wiredConditionClasses(ast);
            sourceWired = new LinkedHashMap<>();
            for (Map.Entry<String, List<RGeneratedJavaClass<?>>> entry : wired.entrySet()) {
                List<String> canonical = new ArrayList<>();
                for (RGeneratedJavaClass<?> conditionClass : entry.getValue()) {
                    canonical.add(conditionClass.getCanonicalName().withDots());
                }
                sourceWired.put(entry.getKey(), canonical);
            }
        } catch (SilentDegradation.Refusal refused) {
            sourceWiringRefused = true;
        }
        boolean irWiringRefused = false;
        for (ValidatorScan.ScannedAttribute source : scanned) {
            IRField field = declaredByProperty.get(source.name());
            if (field != null && IRDerivedFacts.refusesWholeValidator(field)) {
                irWiringRefused = true;
                break;
            }
        }
        for (ValidatorScan.ScannedAttribute source : scanned) {
            IRField field = declaredByProperty.get(source.name());
            String sourceClasses = sourceWiringRefused ? "REFUSED"
                    : IRDerivedFacts.join(sourceWired.getOrDefault(source.name(), List.of()));
            String irClasses = irWiringRefused ? "REFUSED"
                    : field == null ? "" : IRDerivedFacts.join(IRDerivedFacts.aliasConditionClassNames(field));
            c.same("typeFormat." + source.name() + ".aliasConditionClasses", sourceClasses, irClasses);
            aliasConditionClassFacts.incrementAndGet();
        }
        boolean sourceRefuses = false;
        boolean irRefuses = false;
        int sourceEnvelopes = 0;
        int irEnvelopes = 0;
        for (ValidatorScan.ScannedAttribute source : scanned) {
            TypeFormatValidatorGenerator.DeclaredType declared = declaredTypes.get(source.name());
            IRField field = declaredByProperty.get(source.name());
            if (declared == null) {
                c.same("typeFormat." + source.name() + ".declared", false, field != null);
                continue;
            }
            String at = "typeFormat." + source.name() + ".";
            c.same(at + "declared", true, field != null);
            if (field == null) {
                continue;
            }
            // the chain: THREE reads - the scan's, this reconciler's own walk, and the adapter's
            List<RTypeAlias> ownWalk = walkAliasChain(declared.typeCall());
            List<RTypeAlias> scanWalk =
                    TypeFormatConstraintScan.aliasHierarchy(declared.typeCall(), generatorModel.workspace());
            String sourceChain =
                    IRDerivedFacts.join(ownWalk.stream().map(IRDerivedFactsReconciler::renderSourceRung).toList());
            c.same(at + "aliasChain.scanAgrees",
                    IRDerivedFacts.join(scanWalk.stream().map(IRDerivedFactsReconciler::renderSourceRung).toList()),
                    sourceChain);
            c.same(at + "aliasChain",
                    sourceChain, IRDerivedFacts.join(IRDerivedFacts.aliasChain(field).stream()
                            .map(IRDerivedFactsReconciler::renderIrRung).toList()));

            boolean sourceRefusal = refusesWholeValidator(ownWalk);
            boolean irRefusal = IRDerivedFacts.refusesWholeValidator(field);
            c.same(at + "refusesValidator", sourceRefusal, irRefusal);
            sourceRefuses |= sourceRefusal;
            irRefuses |= irRefusal;

            c.same(at + "metaWrapped", declared.metaWrapped(), irFacts.metaWrapped(field, effective, chain));

            Optional<String> sourceEnvelope = renderSourceEnvelope(TypeFormatConstraintScan.constrainedBasic(
                    declared.typeCall(), generatorModel.workspace()));
            Optional<String> irEnvelope = irFacts.envelope(field).map(IRDerivedFacts::renderEnvelope);
            c.same(at + "constrained", sourceEnvelope.isPresent(), irEnvelope.isPresent());
            c.same(at + "string", slot(sourceEnvelope, "string"), slot(irEnvelope, "string"));
            c.same(at + "number", slot(sourceEnvelope, "number"), slot(irEnvelope, "number"));
            c.same(at + "viaAlias", !ownWalk.isEmpty(), !IRDerivedFacts.aliasChain(field).isEmpty());
            if (sourceEnvelope.isPresent()) {
                sourceEnvelopes++;
            }
            if (irEnvelope.isPresent()) {
                irEnvelopes++;
            }
        }
        c.same("typeFormat.refusesValidator", sourceRefuses, irRefuses);
        c.same("typeFormat.size", sourceRefuses ? 0 : sourceEnvelopes, irRefuses ? 0 : irEnvelopes);
        // THE WALK COUNTS EVERY ELEMENT (the decode probe's walkAll): a refusal - the alias-condition wing above OR the
        // generator's own java.lang collision refusal, which the IR half does not model until the emitter PR - withholds a
        // WHOLE file, and the D11 host closes the arithmetic the probe's way, text + refusedChecks == irWalk, subtracting
        // each REFUSED element's own count from this per-element record (never a silent exclusion here)
        typeFormatChecks.addAndGet(irEnvelopes);
        typeFormatChecksByElement.put(ir.name(), irEnvelopes);

        // ---- D05 / D06: the *Meta condition refs, ROOT-FIRST, and every condition's kind -------------------
        // THREE reads, as the alias chain takes three (round 1, SF-3): this class's own mirror walk, the GENERATOR'S
        // OWN answer through the collectConditionRefs seam, and the IR chain's. The seam is what stops the first two
        // being one law written twice by one author - the class NAMES the *Meta imports, so a mirror that drifted
        // from the generator (the unnamed condition's namedCount suffix is the fragile part, risk R7) would name a
        // class the data-rule generator never wrote and both mirrors would still agree.
        List<String> ownConditionRefs = sourceConditionRefs(ast);
        c.same("meta.conditionRefs.scanAgrees",
                IRDerivedFacts.join(generatorConditionRefs(ast)), IRDerivedFacts.join(ownConditionRefs));
        c.same("meta.conditionRefs",
                IRDerivedFacts.join(ownConditionRefs), IRDerivedFacts.join(IRDerivedFacts.conditionRefs(chain)));
        List<String> sourceKinds = new ArrayList<>();
        List<String> sourceUnnamedKinds = new ArrayList<>();
        for (RCondition condition : conditionsOf(ast)) {
            String kind = ModelMetaGenerator.unnamedConditionKind(condition);
            sourceKinds.add(kind);
            if (condition.name().isEmpty()) {
                sourceUnnamedKinds.add(kind);
            }
        }
        c.same("meta.conditionKinds", IRDerivedFacts.join(sourceKinds), IRDerivedFacts.join(ir.conditionKinds()));
        List<String> irUnnamedKinds = new ArrayList<>();
        for (int i = 0; i < Math.min(ir.conditionNames().size(), ir.conditionKinds().size()); i++) {
            if (ir.conditionNames().get(i).isEmpty()) {
                irUnnamedKinds.add(ir.conditionKinds().get(i));
            }
        }
        c.same("meta.unnamedKind", IRDerivedFacts.join(sourceUnnamedKinds), IRDerivedFacts.join(irUnnamedKinds));

        // ---- the QUALIFY WING, as the *Meta writes it (v3.3 seat 9, PR #645 commit 13) --------------------
        // TWO PRODUCERS, both answering the ORDERED CLASS LIST the file renders: the GENERATOR'S OWN
        // collectQualifyFunctions through the seam opened at this commit (the banked cq SF-3 of PR #644), and the
        // EMITTER'S OWN law over the workspace-wide model index - ONE declaration, TWO callers (LAW 69:
        // IRModelMetaEmitter.buildModel renders from the same method this line asserts). It is a stronger read
        // than model.qualify.matched, which compares function NAMES per model and never the class canonicals, the
        // ROOT decision or the load order the list is built in; and it is EMPTY for every non-root, which is a
        // witnessed negative rather than an absence - the four vendored roots and the chaos cell's s22 namespaces
        // are where it reads non-empty.
        List<String> sourceQualify = new ArrayList<>();
        for (ModelMetaGenerator.QualifyRef ref : conditionRefSeam.collectQualifyFunctions(ast)) {
            sourceQualify.add(ref.fqn());
        }
        c.same("meta.qualifyFunctions", IRDerivedFacts.join(sourceQualify),
                IRDerivedFacts.join(IRModelMetaEmitter.qualifyFunctionClasses(ir, modelIndex)));
        qualifyFunctionFacts.incrementAndGet();

        // ---- D10 / D13: deep-path eligibility, the feature set and the HashMap ORDER -----------------------
        boolean sourceEligible = deepPathScan.isEligible(ast);
        boolean irIsEligible = irFacts.eligible(ir, chain);
        c.same("deepPath.eligible", sourceEligible, irIsEligible);
        Map<String, DeepPathScan.ScanAttr> sourceFeatures = deepPathScan.findDeepFeatureMap(ast);
        Map<String, IRDerivedFacts.Feature> irFeatures = irFacts.featureMap(ir);
        c.same("deepPath.features", IRDerivedFacts.join(sorted(sourceFeatures.keySet())),
                IRDerivedFacts.join(sorted(irFeatures.keySet())));
        c.same("deepPath.order", IRDerivedFacts.join(new ArrayList<>(sourceFeatures.keySet())),
                IRDerivedFacts.join(new ArrayList<>(irFeatures.keySet())));
        // the VALUE the metadata collapse left on each key - what DeepPathUtilGenerator's "Type coercion" unwrap
        // reads, and the direct gate on the two swaps (DeepPathScan :250-256, :265-268)
        // SORTED, so this fact gates the collapse's VALUES and never the iteration order - `deepPath.order` owns that
        List<String> sourceFeatureMeta = new ArrayList<>();
        sourceFeatures.forEach((name, attr) -> sourceFeatureMeta.add(name + "=" + attr.hasMeta()));
        List<String> irFeatureMeta = new ArrayList<>();
        irFeatures.forEach((name, feature) -> irFeatureMeta.add(name + "=" + feature.hasMeta()));
        Collections.sort(sourceFeatureMeta);
        Collections.sort(irFeatureMeta);
        c.same("deepPath.featureMeta", IRDerivedFacts.join(sourceFeatureMeta), IRDerivedFacts.join(irFeatureMeta));

        // ---- the DEEP-PATH UTIL's own two rendering facts (v3.3 seat 9, PR #645 commit 14) -----------------
        // TWO PRODUCERS again, both answering what the FILE renders: the GENERATOR'S OWN decisions through the
        // two seams opened at this commit (dependenciesOf / armsOf - visibility and a projection of its own
        // HashSet and its own armDecision, no law duplicated), and the EMITTER'S OWN law (ONE declaration, TWO
        // callers, LAW 69: IRDeepPathUtilEmitter.buildModel renders from the very methods these lines assert).
        //
        // `deepPath.dependencies` is the ORDER fact the class javadoc's HashSet law stands on - neither
        // alphabetical nor declaration order, a JDK table walk over two value-hashed strings - and it is EMPTY
        // for every element that writes no util and every dependency-free one, a witnessed negative.
        // `deepPath.<feature>.arms` is the three decisions renderAlternativeExpr takes per (feature x
        // alternative): self / deeper / direct, the meta unwrap and the feature-side divergence unwrap.
        IRDeepPathUtilEmitter deepPathEmitter = new IRDeepPathUtilEmitter(index, irFacts);
        c.same("deepPath.dependencies", IRDerivedFacts.join(deepPathSeam.dependenciesOf(ast)),
                IRDerivedFacts.join(deepPathEmitter.dependencyCanonicals(ir)));
        deepPathDependencyFacts.incrementAndGet();
        c.same("deepPath.arms", IRDerivedFacts.join(deepPathSeam.armsOf(ast)),
                IRDerivedFacts.join(deepPathEmitter.armDecisions(ir)));
        deepPathArmFacts.incrementAndGet();

        // ---- D12: the java.lang simple-name collision law --------------------------------------------------
        String elementSimpleName = pojo.getSimpleName();
        Set<String> asserted = new LinkedHashSet<>();
        collision(c, irFacts, asserted, elementSimpleName, IRDerivedFacts.simpleName(ir));
        for (ValidatorScan.ScannedAttribute source : scanned) {
            String sourceItem = typeUtil.getItemType(source.prop().getType()).getSimpleName();
            String irItem = irFacts.itemSimpleName(declaredByProperty.get(source.name()), effective, chain);
            if (irItem == null) {
                continue;
            }
            collision(c, irFacts, asserted, sourceItem, irItem);
        }

        return c.close();
    }

    // ---------------------------------------------------------------------------------------- the alias chain

    /**
     * THIS RECONCILER'S OWN walk of the source alias chain, under {@code TypeFormatConstraintScan.aliasHierarchy}'s
     * law ({@code :90-105}): outermost first, the referenced type resolved through the generator's workspace, a
     * cycle ended by the seen-list and the walk bounded by {@link #MAX_ALIAS_DEPTH}. It is the ORACLE for
     * {@code IRType.aliasChain()} - the adapter is the other producer (LAW 69).
     */
    private List<RTypeAlias> walkAliasChain(RTypeCall typeCall) {
        List<RTypeAlias> chain = new ArrayList<>();
        RTypeCall call = typeCall;
        int depth = 0;
        while (call != null && depth++ < MAX_ALIAS_DEPTH) {
            RNode node = call.referencedTypeId().map(generatorModel.workspace()::resolveTypeLike).orElse(null);
            if (!(node instanceof RTypeAlias alias) || chain.contains(alias)) {
                break;
            }
            chain.add(alias);
            if (lie == IRDerivedLie.ALIAS_CHAIN_NO_RECURSION) {
                break;
            }
            call = alias.typeCall();
        }
        return chain;
    }

    /** One source rung: its qualified name, its parameter names, its condition names and their kinds. */
    private static String renderSourceRung(RTypeAlias alias) {
        List<String> parameters = new ArrayList<>();
        for (RTypeParameter parameter : alias.typeParameters()) {
            parameters.add(parameter.name() == null ? "?" : parameter.name());
        }
        List<String> conditionNames = new ArrayList<>();
        List<String> conditionKinds = new ArrayList<>();
        for (RCondition condition : alias.conditions()) {
            conditionNames.add(condition.name().orElse("-"));
            conditionKinds.add(ModelMetaGenerator.unnamedConditionKind(condition));
        }
        Optional<String> namespace = AstWalker.findAncestor(alias, RModel.class).map(RModel::namespace);
        String qualified = namespace.filter(ns -> !ns.isEmpty()).map(ns -> ns + "." + alias.name())
                .orElse(alias.name());
        return qualified + "(" + IRDerivedFacts.join(parameters) + ")[" + IRDerivedFacts.join(conditionNames) + "]{"
                + IRDerivedFacts.join(conditionKinds) + "}";
    }

    /** One IR rung, in the SAME spelling the source rung takes. */
    private static String renderIrRung(com.regnosys.rosetta.ir.core.IRAliasLink link) {
        List<String> conditionNames = new ArrayList<>();
        for (Optional<String> name : link.conditionNames()) {
            conditionNames.add(name.orElse("-"));
        }
        return link.qualifiedName() + "(" + IRDerivedFacts.join(link.parameterNames()) + ")["
                + IRDerivedFacts.join(conditionNames) + "]{" + IRDerivedFacts.join(link.conditionKinds()) + "}";
    }

    /**
     * THE WHOLE-VALIDATOR REFUSAL, source side ({@code TypeFormatValidatorGenerator.java:248-264}): a rung that is
     * PARAMETERISED and carries at least one condition case refuses the type-format validator of the WHOLE element -
     * the wing is one method over every attribute, so there is no per-attribute file to drop.
     */
    private static boolean refusesWholeValidator(List<RTypeAlias> chain) {
        for (RTypeAlias alias : chain) {
            if (ConditionCases.parameterisedOwner(alias) != null && !ConditionCases.casesOf(alias).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------- the type-format envelope

    /**
     * The source envelope, rendered canonically: {@code string[...]} or {@code number[...]}. The RENDERING is
     * {@link IRDerivedFacts}'s, so both halves are spelled by one declaration (v3.3 seat 9, PR #645 commit 12).
     */
    private static Optional<String> renderSourceEnvelope(Optional<RType> envelope) {
        if (envelope.isEmpty()) {
            return Optional.empty();
        }
        RType type = envelope.get();
        if (type instanceof RStringType string) {
            return Optional.of(IRDerivedFacts.renderString(string.minLength(), string.maxLength(),
                    string.pattern().map(Pattern::toString)));
        }
        RNumberType number = (RNumberType) type;
        return Optional.of(IRDerivedFacts.renderNumber(number.digits(), number.fractionalDigits(),
                number.min().map(IRDerivedFactsReconciler::decimal),
                number.max().map(IRDerivedFactsReconciler::decimal)));
    }

    /** The {@code string} or the {@code number} half of an envelope rendering, or {@code -} when it is the other. */
    private static String slot(Optional<String> envelope, String kind) {
        return envelope.filter(e -> e.startsWith(kind + "[")).orElse("-");
    }

    /** The ONE decimal spelling both halves take - {@link IRDerivedFacts#decimal}'s. */
    private static String decimal(BigDecimal value) {
        return IRDerivedFacts.decimal(value);
    }

    // ------------------------------------------------------------------------------------ the condition refs

    private static List<RCondition> conditionsOf(RRootElement element) {
        return element instanceof RDataType dataType ? dataType.conditions() : List.of();
    }

    /**
     * THE GENERATOR'S OWN condition-class refs, read through the {@code collectConditionRefs} reconcile seam
     * ({@code ModelMetaGenerator.java:261-309}, opened {@code public} by this PR, visibility only). These are the
     * simple names the {@code *Meta} file IMPORTS and the data-rule generator WRITES; {@link #sourceConditionRefs}
     * is this class's independent walk of the same law, and {@code meta.conditionRefs.scanAgrees} holds the two
     * together before either is held against the IR.
     */
    private List<String> generatorConditionRefs(RRootElement element) {
        List<String> refs = new ArrayList<>();
        for (ModelMetaGenerator.ConditionRef ref : conditionRefSeam.collectConditionRefs(element)) {
            refs.add(ref.simpleName());
        }
        return refs;
    }

    /**
     * The condition-class refs of an element, ROOT-FIRST - this class's OWN walk of {@code ModelMetaGenerator}'s
     * law ({@code :261-309}), held against the generator's own answer by {@code meta.conditionRefs.scanAgrees}
     * ({@link #generatorConditionRefs}) before it is held against the IR. A {@code choice} contributes its implicit
     * {@code <Name>Choice}; a data type contributes one ref per OWN
     * condition in declaration order, an unnamed one named by the CORPUS-FITTED {@code namedCount} law
     * ({@code :297-299}) - the count of ALL named conditions of the declaring type, which gives every unnamed
     * condition of a type the SAME suffix. Risk R7: that is a latent limitation for two unnamed conditions on one
     * type (zero corpus witnesses), and it is reproduced AS WRITTEN because the emitter must write the same class
     * name the data-rule generator writes - two unnamed conditions therefore produce the SAME ref TWICE, on all
     * three reads, and the fixture's {@code TwoUnnamed} witnesses exactly that (round 1, SF-4). The lane that proves
     * the law is gated rather than merely copied is {@link IRDerivedLie#CONDITION_UNNAMED_BY_ORDINAL}.
     */
    private List<String> sourceConditionRefs(RRootElement element) {
        List<RRootElement> chain = new ArrayList<>();
        RRootElement current = element;
        int depth = 0;
        while (current != null && depth++ < MAX_CHAIN_DEPTH) {
            chain.add(current);
            if (current instanceof RDataType dataType) {
                Optional<RDataType> dataSuper = dataType.superType();
                if (dataSuper.isPresent()) {
                    current = dataSuper.get();
                    continue;
                }
                current = dataType.choiceSuperType().orElse(null);
                continue;
            }
            current = null;
        }
        List<String> refs = new ArrayList<>();
        boolean elementFirst = lie == IRDerivedLie.CONDITION_CHAIN_ELEMENT_FIRST;
        for (int step = 0; step < chain.size(); step++) {
            RRootElement link = chain.get(elementFirst ? step : chain.size() - 1 - step);
            if (link instanceof RChoice choice) {
                refs.add(choice.name() + "Choice");
                continue;
            }
            RDataType dataType = (RDataType) link;
            long namedCount = dataType.conditions().stream().filter(c -> c.name().isPresent()).count();
            int unnamedOrdinal = 0;
            for (RCondition condition : dataType.conditions()) {
                if (condition.name().isPresent()) {
                    refs.add(dataType.name() + condition.name().get());
                    continue;
                }
                // the CORPUS-FITTED law AS WRITTEN: the suffix is the count of ALL NAMED conditions of the declaring
                // type, so every unnamed condition of a type takes the SAME suffix (risk R7, reproduced because the
                // emitter must write the class name the data-rule generator writes). The ordinal is the LANE's lie.
                long suffix = lie == IRDerivedLie.CONDITION_UNNAMED_BY_ORDINAL ? unnamedOrdinal : namedCount;
                unnamedOrdinal++;
                refs.add(dataType.name() + ModelMetaGenerator.unnamedConditionKind(condition) + suffix);
            }
        }
        return refs;
    }

    // ----------------------------------------------------------------------------------------- the collision

    /**
     * The two collision facts of one cast-site simple name, each asserted ONCE per distinct subject (round 1,
     * NIT-7): a fact NAME that repeats inside one element's {@link Check} is counted as many times as the element
     * has properties of that type, which inflates {@code factsAsserted} and makes the same name mean two things.
     *
     * <p>The two dedup keys are deliberately different. {@code collision.<name>} is the java.lang PREDICATE and is a
     * function of the source name alone, so it is asserted once per NAME. {@code collision.simpleName.<name>} is the
     * two halves' agreement on the name itself, so it is asserted once per (source, IR) PAIR - deduping THAT one by
     * the source name would let a second property whose IR name disagrees hide behind a first whose agrees.
     */
    private void collision(Check c, IRDerivedFacts irFacts, Set<String> asserted, String sourceSimpleName,
            String irSimpleName) {
        if (sourceSimpleName == null || irSimpleName == null) {
            return;
        }
        if (asserted.add("name|" + sourceSimpleName + "|" + irSimpleName)) {
            c.same("collision.simpleName." + sourceSimpleName, sourceSimpleName, irSimpleName);
        }
        if (!asserted.add("collides|" + sourceSimpleName)) {
            return;
        }
        c.same("collision." + sourceSimpleName, ValidatorScan.collidesWithJavaLang(sourceSimpleName),
                irFacts.collides(irSimpleName));
    }

    // --------------------------------------------------------------------------------------------- the helpers

    private static List<String> sorted(Set<String> values) {
        List<String> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted;
    }

    /**
     * One element's assertions: counts every fact, collects the mismatches, books both on {@link #close()} - the
     * {@code IRDeclarationReconciler.Check} idiom, copied rather than shared because each reconciler books its own
     * counters and the D11 host prints them on its own line.
     */
    private final class Check {
        private final String subject;
        private final List<String> failed = new ArrayList<>();
        private int asserted;

        Check(String subject) {
            this.subject = subject;
        }

        void same(String fact, Object expected, Object actual) {
            asserted++;
            if (!Objects.equals(expected, actual)) {
                failed.add("IR/AST reconciliation failed for " + subject + ": " + fact + " - the source says "
                        + expected + ", the IR says " + actual);
            }
        }

        List<String> close() {
            facts.addAndGet(asserted);
            mismatches.addAndGet(failed.size());
            return failed;
        }
    }
}
