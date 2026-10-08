package demo.harness.exec.workload;

import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import demo.harness.exec.corpus.ModelClosure;
import demo.harness.exec.corpus.ReflectivePopulator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Workload 3 - {@code validate}: the validator sweep, adapted from
 * {@code rune-benchmarks/.../CdmValidationSweepBenchmark.java} to the demo closure. Every
 * discoverable XMeta registry contributes its cardinality validator, its type-format
 * validator and its data rules; one op runs all of them over deterministically populated
 * instances.
 *
 * <p>Four deliberate hardenings over the reference:
 * <ul>
 *   <li>{@code typeFormatValidator(factory)} is allowed to return null - the interface's
 *       default does exactly that for a model generated before 9.37.0 - so it is null-checked
 *       and counted rather than assumed present. {@code validator(factory)} is null-checked
 *       for the same reason.</li>
 *   <li>The model class name is derived by replacing the LAST {@code ".meta."} segment, not
 *       every occurrence, so a package that legitimately contains that token cannot be
 *       silently mis-resolved into a {@code modelMissing} skip.</li>
 *   <li>A registry that fails to LOAD is counted separately from one that loads and turns
 *       out not to be a registry. Folding both into one bucket would let a leg lose hundreds
 *       of metas behind a census that looked identical to the healthy leg's.</li>
 *   <li>A registry supplying no validator at all contributes nothing, so it is counted and
 *       skipped rather than padding {@code unitsPerOp} with an empty loop body.</li>
 * </ul>
 *
 * <p>The census reports metas discovered, usable pairs and every skip reason - and the skip
 * reasons RECONCILE: considered = usablePairs + the six drop-out counters. It also reports
 * the validator invocations one op performs, which is the real denominator behind
 * {@code meanMs}.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class ValidateWorkload implements Workload {

    private static final String META_SEGMENT = ".meta.";
    private static final String META_SUFFIX = "Meta";

    private final ModelClosure closure;
    private final int limit;
    private final int depth;

    private List<Pair> pairs;
    private RosettaPath path;
    private int invocationsPerOp;

    record Pair(Validator cardinality, Validator typeFormat, List<Validator> dataRules,
                Object instance) {}

    public ValidateWorkload(ModelClosure closure, int limit, int depth) {
        this.closure = closure;
        this.limit = limit;
        this.depth = depth;
    }

    @Override
    public String name() {
        return "validate";
    }

    @Override
    public Map<String, Object> setUp() throws Exception {
        List<String> allMetaNames = closure.classNames(
                n -> n.endsWith(META_SUFFIX) && n.contains(META_SEGMENT));
        List<String> metaNames = Workloads.stratify(allMetaNames, limit);

        // The Default factory carries an @Inject Injector (validators are guice-instantiated),
        // exactly as the reference benchmark does.
        ValidatorFactory factory = closure.injector().getInstance(ValidatorFactory.Default.class);
        ReflectivePopulator populator = new ReflectivePopulator();
        pairs = new ArrayList<>();
        path = RosettaPath.valueOf("Demo");
        int metaLoadFailed = 0;
        int notMeta = 0;
        int metaConstructFailed = 0;
        int modelMissing = 0;
        int populateFailed = 0;
        int noCardinality = 0;
        int noTypeFormat = 0;
        int noValidatorsAtAll = 0;
        int invocations = 0;

        for (String metaName : metaNames) {
            Class<?> metaClass;
            try {
                // shape-only load for the screen: a class that FAILS TO LOAD is a different
                // fact from a class that loads and is not a registry, and conflating them
                // would let a leg lose hundreds of metas without the census showing it
                metaClass = Workloads.loadShape(closure, metaName);
            } catch (Throwable t) {
                metaLoadFailed++;
                continue;
            }
            if (!RosettaMetaData.class.isAssignableFrom(metaClass)) {
                notMeta++;
                continue;
            }
            RosettaMetaData meta;
            try {
                meta = (RosettaMetaData) metaClass.getConstructor().newInstance();
            } catch (Throwable t) {
                metaConstructFailed++;
                continue;
            }
            String modelName = modelNameFor(metaName);
            Class<?> modelClass;
            try {
                modelClass = closure.load(modelName);
            } catch (Throwable t) {
                modelMissing++;
                continue;
            }
            Object instance = populator.populate(modelClass, depth);
            if (instance == null) {
                populateFailed++;
                continue;
            }
            Validator cardinality = meta.validator(factory);
            Validator typeFormat = meta.typeFormatValidator(factory);
            if (cardinality == null) {
                noCardinality++;
            }
            if (typeFormat == null) {
                noTypeFormat++;
            }
            List<Validator> dataRules = meta.dataRules(factory);
            if (dataRules == null) {
                dataRules = List.of();
            }
            int contribution = (cardinality == null ? 0 : 1) + (typeFormat == null ? 0 : 1)
                    + dataRules.size();
            if (contribution == 0) {
                // a registry that supplies NO validator contributes no work; counting it as
                // a usable pair would inflate unitsPerOp with an empty loop body
                noValidatorsAtAll++;
                continue;
            }
            invocations += contribution;
            pairs.add(new Pair(cardinality, typeFormat, dataRules, instance));
        }
        invocationsPerOp = invocations;
        if (pairs.isEmpty()) {
            throw new IllegalStateException("zero usable (meta, instance) pairs - census above");
        }

        // The drop-out account reconciles exactly:
        //   considered = usablePairs + metaLoadFailed + notMeta + metaConstructFailed
        //                + modelMissing + populateFailed + noValidatorsAtAll
        Map<String, Object> census = new LinkedHashMap<>();
        census.put("metaClassesFound", allMetaNames.size());
        census.put("metaClassesConsidered", metaNames.size());
        census.put("usablePairs", pairs.size());
        census.put("metaLoadFailed", metaLoadFailed);
        census.put("notMeta", notMeta);
        census.put("metaConstructFailed", metaConstructFailed);
        census.put("modelMissing", modelMissing);
        census.put("populateFailed", populateFailed);
        census.put("noValidatorsAtAll", noValidatorsAtAll);
        census.put("noCardinalityValidator", noCardinality);
        census.put("noTypeFormatValidator", noTypeFormat);
        census.put("validatorInvocationsPerOp", invocationsPerOp);
        // The populator counters span EVERY recursion level (a nested child object is
        // populated by the same instance), so they do not reconcile against the per-meta
        // columns above and must not be read as a top-level split.
        census.put("populatorBuiltAllLevels", populator.populatedCount());
        census.put("populatorFailedAllLevels", populator.failedCount());
        census.put("populatorSetterRejectionsAllLevels", populator.setterRejectionCount());
        System.out.printf("[validate census] metaClasses=%d considered=%d usablePairs=%d "
                        + "metaLoadFailed=%d notMeta=%d metaConstructFailed=%d modelMissing=%d "
                        + "populateFailed=%d noValidatorsAtAll=%d noCardinality=%d "
                        + "noTypeFormat=%d invocationsPerOp=%d (populator all levels: built=%d "
                        + "failed=%d setterRejections=%d)%n",
                allMetaNames.size(), metaNames.size(), pairs.size(), metaLoadFailed, notMeta,
                metaConstructFailed, modelMissing, populateFailed, noValidatorsAtAll,
                noCardinality, noTypeFormat, invocationsPerOp,
                populator.populatedCount(), populator.failedCount(),
                populator.setterRejectionCount());
        return census;
    }

    /** {@code a.b.meta.XMeta -> a.b.X}, keyed on the LAST ".meta." segment. */
    static String modelNameFor(String metaName) {
        int seg = metaName.lastIndexOf(META_SEGMENT);
        String withoutSegment = seg < 0 ? metaName
                : metaName.substring(0, seg) + "." + metaName.substring(seg + META_SEGMENT.length());
        return withoutSegment.endsWith(META_SUFFIX)
                ? withoutSegment.substring(0, withoutSegment.length() - META_SUFFIX.length())
                : withoutSegment;
    }

    @Override
    public int unitsPerOp() {
        return pairs == null ? 0 : pairs.size();
    }

    @Override
    public String unitLabel() {
        return "model objects swept (" + invocationsPerOp + " validator invocations)";
    }

    @Override
    public long runOnce() {
        long checksum = 0L;
        for (Pair p : pairs) {
            if (p.cardinality() != null) {
                List<?> r = p.cardinality().getValidationResults(path, p.instance());
                checksum += r == null ? 0 : r.size();
            }
            if (p.typeFormat() != null) {
                List<?> r = p.typeFormat().getValidationResults(path, p.instance());
                checksum += r == null ? 0 : r.size();
            }
            for (Validator dr : p.dataRules()) {
                List<?> r = dr.getValidationResults(path, p.instance());
                checksum += r == null ? 0 : r.size();
            }
        }
        return checksum;
    }
}
