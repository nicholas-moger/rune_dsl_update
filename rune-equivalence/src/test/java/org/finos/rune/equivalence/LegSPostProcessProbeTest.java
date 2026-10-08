package org.finos.rune.equivalence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.hashing.GlobalKeyProcessStep;
import com.regnosys.rosetta.common.hashing.NonNullHashCollector;
import com.regnosys.rosetta.common.hashing.ReKeyProcessStep;
import com.regnosys.rosetta.common.hashing.ReferenceConfig;
import com.regnosys.rosetta.common.hashing.ReferenceResolverProcessStep;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import org.finos.rune.benchmarks.corpus.CorpusClasses;
import org.junit.jupiter.api.Test;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The rosetta-common census's DYNAMIC leg (research/p3-rosetta-common-census.md
 * § 6) + the O7 pair smoke — corpus-gated, rosetta-common at TEST SCOPE only
 * (the plan § 3.4 letter).
 *
 * <p>Proves live, over the FORK runtime + the 2.18.9-managed Jackson: a real
 * cdm5 ingestion-result sample loads through {@code RosettaObjectMapper} into
 * corpus-compiled classes; the three concrete post-process steps (global-key
 * generation → re-key → reference resolution) run over the builder; the
 * post-processed instance rebuilds and re-serialises. Then the pair leg:
 * the SAME sample deserialised through TWO loaders, both sides post-processed
 * by the production chain, compared structurally (O7 post-processed identity)
 * AND via the serialize-both-and-byte-compare channel (O1's secondary).
 */
class LegSPostProcessProbeTest {

    /** The census-pinned deterministic sample (cdm5's first ingestion-result set). */
    private static final String SAMPLE =
            "cdm/cdm-5.38.0/rosetta-source/src/main/resources/result-json-files/"
                    + "cme-cleared-confirm-1-17/Basis-ex01-LIBOR-vs-SOFR.json";

    /**
     * The candidate root types the STRICT binding probe quantifies over — the
     * cell's ingestion-config roots plus the event envelope. The probe (not a
     * hand guess) decides which one the sample IS: the first lenient-mapper
     * run of this suite deserialised the sample "successfully" into an EMPTY
     * TradeState (53 serialised bytes — every property silently unknown),
     * which is exactly the failure mode strict binding exists to catch.
     */
    private static final List<String> ROOT_TYPE_CANDIDATES = List.of(
            "cdm.event.common.TradeState",
            "cdm.event.workflow.WorkflowStep",
            "cdm.event.common.BusinessEvent");

    @Test
    void rosettaCommonLoadsPostProcessesAndReserialisesOverTheForkRuntime() throws Exception {
        assumeTrue(CorpusClasses.corpusPresent(), "test-corpus/ absent — local-only probe");
        Path sample = CorpusClasses.repoRoot().resolve("test-corpus").resolve(SAMPLE);
        assumeTrue(Files.isRegularFile(sample), "census-pinned sample missing: " + sample);

        Path out = CorpusClasses.ensureCompiled("cdm/cdm-5.38.0", List.of());
        try (URLClassLoader cl = CorpusClasses.loaderOver(List.of(out))) {
            Object postProcessed = loadAndPostProcess(sample, cl);
            ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();
            String reserialised = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(postProcessed);
            assertTrue(reserialised.length() > 100, "re-serialisation produced no payload");
            assertTrue(reserialised.contains("globalKey"),
                    "the post-processed tree carries global keys");
            System.out.printf("[LegSProbe] sample=%s boundType=%s loaded+post-processed"
                            + " (globalKey+reKey+referenceResolver) reserialisedBytes=%d%n",
                    SAMPLE, postProcessed.getClass().getInterfaces()[0].getName(),
                    reserialised.length());
        }
    }

    @Test
    void o7PostProcessedIdentityHoldsAcrossTheLoaderPair() throws Exception {
        assumeTrue(CorpusClasses.corpusPresent(), "test-corpus/ absent — local-only probe");
        Path sample = CorpusClasses.repoRoot().resolve("test-corpus").resolve(SAMPLE);
        assumeTrue(Files.isRegularFile(sample), "census-pinned sample missing: " + sample);

        Path out = CorpusClasses.ensureCompiled("cdm/cdm-5.38.0", List.of());
        try (URLClassLoader refCl = CorpusClasses.loaderOver(List.of(out));
             URLClassLoader optCl = CorpusClasses.loaderOver(List.of(out))) {
            Object ref = loadAndPostProcess(sample, refCl);
            Object opt = loadAndPostProcess(sample, optCl);

            String divergence = ReflectiveDeepCompare.firstDivergence(ref, opt);
            assertNull(divergence, "O7 post-processed identity must hold (ref≡ref at PR-3): "
                    + divergence);

            ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();
            String refJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ref);
            String optJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(opt);
            assertEquals(refJson, optJson,
                    "the serialize-both-and-byte-compare channel (O1 secondary) must agree");
            System.out.printf("[LegSProbe O7] pair post-processed identity HOLDS; "
                    + "serialisedBytes=%d (both sides byte-equal)%n", refJson.length());
        }
    }

    /**
     * The STRICT binding probe: try every candidate with unknown-property
     * failure ENABLED; exactly one candidate must bind. Lenient loading is
     * never the loadability oracle — it "succeeds" into an empty instance.
     */
    private static Class<?> bindRootType(Path sample, URLClassLoader cl) throws Exception {
        ObjectMapper strict = RosettaObjectMapper.getNewRosettaObjectMapper().copy()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature
                        .FAIL_ON_UNKNOWN_PROPERTIES, true);
        List<Class<?>> bound = new java.util.ArrayList<>();
        for (String candidate : ROOT_TYPE_CANDIDATES) {
            Class<?> type = Class.forName(candidate, true, cl);
            try {
                strict.readValue(sample.toFile(), type);
                bound.add(type);
                System.out.printf("[LegSProbe binding] %s BINDS strict%n", candidate);
            } catch (Exception e) {
                System.out.printf("[LegSProbe binding] %s rejects: %.120s%n",
                        candidate, String.valueOf(e.getMessage()).replace('\n', ' '));
            }
        }
        assertEquals(1, bound.size(),
                "exactly one candidate root type must bind strictly — the probe IS the"
                        + " loadability oracle: " + bound);
        return bound.get(0);
    }

    /** Load the sample into {@code cl}'s probe-bound root type and run the production chain. */
    private static Object loadAndPostProcess(Path sample, URLClassLoader cl) throws Exception {
        Class<?> rootType = bindRootType(sample, cl);
        ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();
        Object instance = mapper.readValue(sample.toFile(), rootType);
        assertNotNull(instance, "sample failed to deserialise");

        RosettaModelObject model = (RosettaModelObject) instance;
        RosettaModelObjectBuilder builder = model.toBuilder();
        @SuppressWarnings("unchecked")
        Class<RosettaModelObject> topClass = (Class<RosettaModelObject>) rootType;

        GlobalKeyProcessStep globalKey = new GlobalKeyProcessStep(NonNullHashCollector::new);
        ReKeyProcessStep reKey = new ReKeyProcessStep(globalKey);
        ReferenceResolverProcessStep referenceResolver =
                new ReferenceResolverProcessStep(ReferenceConfig.noScopeOrExcludedPaths());

        assertNotNull(globalKey.runProcessStep(topClass, builder), "global-key step report");
        assertNotNull(reKey.runProcessStep(topClass, builder), "re-key step report");
        assertNotNull(referenceResolver.runProcessStep(topClass, builder),
                "reference-resolver step report");
        return builder.build();
    }
}
