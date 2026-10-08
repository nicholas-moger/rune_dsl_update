package demo.harness.exec.workload;

import com.fasterxml.jackson.databind.JsonNode;
import demo.harness.exec.corpus.ModelClosure;
import demo.harness.exec.corpus.ReflectivePopulator;
import demo.harness.exec.serde.RuneJson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Workload 2 - {@code serialise}: JSON round trip ({@code value -> String -> JsonNode}) of a
 * stratified sample of populated model objects, per op.
 *
 * <p>The instances are built ONCE in {@link #setUp()} - construction is workload 1's subject,
 * not this one's - so an op is pure write-plus-parse over a fixed object graph, and the two
 * legs serialise byte-for-byte the same inputs.
 *
 * <p>Which mapper did the work is a REPORTED FACT, not an assumption: {@link RuneJson}
 * detects rosetta-common's canonical {@code RosettaObjectMapper} reflectively and falls back
 * to the leg's own pinned jackson taught the runtime's annotations. The mode appears in a
 * ##DEMO## metric, in the census, and in the receipt notes. Comparing two legs across
 * DIFFERENT modes would be meaningless, so the mode is also what the integrator must check
 * first when reading a legacy-vs-plus serialise delta.
 *
 * <p>Instances that cannot be serialised at all are dropped in setUp and COUNTED - a
 * serialise number over a silently shrinking object set would flatter whichever leg dropped
 * more.
 */
public final class SerialiseWorkload implements Workload {

    /** Default sample size when {@code --limit} is not given. */
    public static final int DEFAULT_LIMIT = 200;

    private final ModelClosure closure;
    private final int limit;
    private final int depth;

    private RuneJson json;
    private List<Object> instances;
    /** CHARS, not bytes: String.length() of the compact JSON summed over the sample. */
    private long jsonCharsPerOp;

    public SerialiseWorkload(ModelClosure closure, int limit, int depth) {
        this.closure = closure;
        this.limit = limit <= 0 ? DEFAULT_LIMIT : limit;
        this.depth = depth;
    }

    @Override
    public String name() {
        return "serialise";
    }

    @Override
    public Map<String, Object> setUp() throws Exception {
        json = RuneJson.create();
        List<Class<?>> types = Workloads.selectModelTypes(closure, limit);
        ReflectivePopulator populator = new ReflectivePopulator();
        List<Object> built = Workloads.buildInstances(types, depth, populator);

        instances = new ArrayList<>(built.size());
        int serialiseFailed = 0;
        long chars = 0L;
        for (Object o : built) {
            try {
                String s = json.write(o);
                chars += s.length();
                instances.add(o);
            } catch (Throwable t) {
                serialiseFailed++;
            }
        }
        jsonCharsPerOp = chars;
        if (instances.isEmpty()) {
            throw new IllegalStateException("zero serialisable instances - census above");
        }

        Map<String, Object> census = new LinkedHashMap<>();
        census.put("serialiser", json.modeLabel());
        census.put("serialiserDetail", json.detail());
        census.put("roundTrip", json.roundTripLabel());
        census.put("requestedTypes", limit);
        census.put("selectedTypes", types.size());
        census.put("builtInstances", built.size());
        census.put("serialisableInstances", instances.size());
        census.put("serialiseFailed", serialiseFailed);
        census.put("populatorFailedAllLevels", populator.failedCount());
        census.put("jsonCharsPerOp", jsonCharsPerOp);
        System.out.printf("[serialise census] serialiser=%s selected=%d built=%d serialisable=%d "
                        + "serialiseFailed=%d jsonChars=%d roundTrip=%s%n",
                json.modeLabel(), types.size(), built.size(), instances.size(),
                serialiseFailed, jsonCharsPerOp, json.roundTripLabel());
        return census;
    }

    @Override
    public int unitsPerOp() {
        return instances == null ? 0 : instances.size();
    }

    @Override
    public String unitLabel() {
        return "objects written and re-parsed";
    }

    @Override
    public long runOnce() throws Exception {
        long checksum = 0L;
        for (Object o : instances) {
            String s = json.write(o);
            JsonNode tree = json.read(s);
            checksum += s.length() + tree.size();
        }
        return checksum;
    }

    /** The active serialiser mode label, for the run banner and receipt notes. */
    public String serialiserMode() {
        return json == null ? "(not set up)" : json.modeLabel();
    }

    public RuneJson json() {
        return json;
    }

    /** The populated instances, so {@code live} can show one real serialised object. */
    public List<Object> instances() {
        return instances;
    }
}
