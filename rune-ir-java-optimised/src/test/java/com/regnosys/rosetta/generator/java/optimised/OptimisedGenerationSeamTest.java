package com.regnosys.rosetta.generator.java.optimised;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelMetaGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.object.deeppath.DeepPathUtilGenerator;
import com.regnosys.rosetta.generator.java.object.validators.CardinalityValidatorGenerator;
import com.regnosys.rosetta.generator.java.object.validators.OnlyExistsValidatorGenerator;
import com.regnosys.rosetta.generator.java.object.validators.TypeFormatValidatorGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.spi.IRGenerationProvider;
import com.regnosys.rosetta.generator.java.spi.OptimisedIRGenerationProvider;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the ServiceLoader seam between the shipping generator and this module
 * (the v3 optimised route — the phase plan § 2; the #466 seam pattern
 * mirrored). Fork-authored.
 *
 * <p>What is locked and why:
 * <ul>
 *   <li><b>Property-name agreement</b> — the flag literal lives in TWO modules
 *   (this module's {@link OptimisedIRFlag}; the generator's
 *   {@code IRGeneration.OPTIMISED_PROPERTY} owns the seam's read). A drift
 *   between them would split the opt-in surface silently (the Rule-3 lock —
 *   the same lock the reference route's seam test carries).</li>
 *   <li><b>Registration</b> — exactly one optimised provider on this module's
 *   classpath, registered under the OPTIMISED interface's own service file,
 *   and INVISIBLE to the reference route's lookup (the coexistence contract:
 *   neither route's exclusivity check can trip on the other's jar).</li>
 *   <li><b>The decline contract, seam by seam</b> — every UN-FLIPPED
 *   construction seam returns the PLAIN legacy generator (exact class, not a
 *   subclass) and both dispatch seams decline, so a flag-on run executes
 *   Path-1 for every un-flipped kind. These are deliberate pins: a family PR
 *   that flips a seam MUST edit the matching assertion in the same PR (the
 *   un-pin-deliberately contract). PR-4 flipped the FUNCTION seam (navigation
 *   chains, tranche 1) — its pin now asserts the optimised generator class.</li>
 *   <li><b>Route exclusivity</b> — both route flags set throws loudly on every
 *   call; clearing re-closes (the flags are re-read per call).</li>
 * </ul>
 */
class OptimisedGenerationSeamTest {

    private static GeneratorModel emptyGm() {
        return new GeneratorModel(RWorkspace.build(List.of()).workspace());
    }

    @Test
    void flagPropertyNameAgreesAcrossModules() {
        assertEquals(IRGeneration.OPTIMISED_PROPERTY, OptimisedIRFlag.PROPERTY,
                "the generator's seam and this module's OptimisedIRFlag must read the SAME"
                        + " system property");
    }

    @Test
    void serviceRegistrationResolvesExactlyThisProvider() {
        List<OptimisedIRGenerationProvider> found =
                ServiceLoader.load(OptimisedIRGenerationProvider.class)
                        .stream().map(ServiceLoader.Provider::get).toList();
        assertEquals(1, found.size(),
                "exactly one OptimisedIRGenerationProvider registration expected");
        assertInstanceOf(OptimisedIRGenerationProviderImpl.class, found.get(0));
    }

    @Test
    void registrationIsInvisibleToTheReferenceRouteLookup() {
        List<IRGenerationProvider> found = ServiceLoader.load(IRGenerationProvider.class)
                .stream().map(ServiceLoader.Provider::get).toList();
        assertEquals(0, found.size(),
                "the optimised registration lives under the OPTIMISED interface's service file"
                        + " ONLY — the reference route's lookup must find nothing on this"
                        + " classpath, so both jars can coexist without tripping either"
                        + " exclusivity check");
    }

    @Test
    void flagOffLoaderReturnsNoProvider() {
        assertFalse(Boolean.getBoolean(IRGeneration.PROPERTY),
                "precondition: this suite runs with the reference flag off");
        assertFalse(Boolean.getBoolean(IRGeneration.OPTIMISED_PROPERTY),
                "precondition: this suite runs with the optimised flag off");
        assertNull(IRGeneration.providerOrNull(),
                "both flags off ⇒ no provider, no ServiceLoader lookup — Path-1 by construction");
    }

    @Test
    void flagOnLoaderResolvesTheRegisteredProvider() {
        assertFalse(Boolean.getBoolean(IRGeneration.OPTIMISED_PROPERTY),
                "precondition: the suite starts flag-off");
        System.setProperty(IRGeneration.OPTIMISED_PROPERTY, "true");
        try {
            IRGenerationProvider p = IRGeneration.providerOrNull();
            assertTrue(p instanceof OptimisedIRGenerationProviderImpl,
                    "optimised flag on + this jar on the classpath ⇒ the optimised provider"
                            + " resolves through the SAME access point the seam call sites use");
        } finally {
            System.clearProperty(IRGeneration.OPTIMISED_PROPERTY);
        }
        assertNull(IRGeneration.providerOrNull(),
                "the flag is re-read per call — clearing it re-closes the route even though"
                        + " the ServiceLoader lookup result stays cached");
    }

    @Test
    void bothRouteFlagsThrowLoudly() {
        System.setProperty(IRGeneration.PROPERTY, "true");
        System.setProperty(IRGeneration.OPTIMISED_PROPERTY, "true");
        try {
            assertThrows(IllegalStateException.class, IRGeneration::providerOrNull,
                    "both route flags set is a configuration error — the routes are exclusive"
                            + " and the error stays loud on every call");
        } finally {
            System.clearProperty(IRGeneration.PROPERTY);
            System.clearProperty(IRGeneration.OPTIMISED_PROPERTY);
        }
        assertNull(IRGeneration.providerOrNull(),
                "dropping the flags re-closes both routes");
    }

    @Test
    void constructionSeamsPinExactClassesPerFamilyState() {
        GeneratorModel gm = emptyGm();
        JavaTypeUtil tu = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(tu);
        OptimisedIRGenerationProvider provider = new OptimisedIRGenerationProviderImpl();

        // Exact-class pins, NOT instanceOf: every un-flipped seam declines with the
        // PLAIN legacy class, and a family PR flipping a seam must re-pin the
        // matching line deliberately (PR-4 flipped the FUNCTION seam below).
        assertEquals(EnumGenerator.class, provider.enumGenerator(gm).getClass(),
                "no enum family has landed — the seam returns the plain legacy generator");
        assertEquals(ModelObjectGenerator.class,
                provider.modelObjectGenerator(gm, tt, tu).getClass(),
                "no pojo family has landed — the seam returns the plain legacy generator");
        assertEquals(ChoiceObjectGenerator.class,
                provider.choiceObjectGenerator(gm, tt, tu,
                        provider.modelObjectGenerator(gm, tt, tu)).getClass(),
                "no choice family has landed — the seam returns the plain legacy generator");
        assertEquals(MetaFieldGenerator.class, provider.metaFieldGenerator(gm, tt).getClass(),
                "no metafield family has landed — the seam returns the plain legacy generator");
        // v3.3 seat 3 (D54): the DATA_RULE construction seam exists since the reference route's
        // condition seam landed; this route has no data-rule twin, so the seam declines with the
        // PLAIN legacy class (an explicit override, pinned exactly like every other un-flipped seam).
        assertEquals(DataRuleGenerator.class, provider.dataRuleGenerator(gm, tt, tu).getClass(),
                "no data-rule family has landed — the seam returns the plain legacy generator");
        // THE FIVE DERIVED-FILE SEAMS (v3.3 seat 9, PR #645 commit 12) DECLINE on this route: the optimised backend
        // has no per-type derived-file family, so each constructs the EXACT legacy class - Path-1, byte-identical.
        assertEquals(OnlyExistsValidatorGenerator.class,
                provider.onlyExistsValidatorGenerator(gm, tt, tu).getClass(),
                "the optimised route declines the ONLY_EXISTS kind - the plain legacy generator, never a subclass");
        assertEquals(CardinalityValidatorGenerator.class,
                provider.cardinalityValidatorGenerator(gm, tt, tu).getClass(),
                "the optimised route declines the CARDINALITY kind - the plain legacy generator");
        assertEquals(TypeFormatValidatorGenerator.class,
                provider.typeFormatValidatorGenerator(gm, tt, tu).getClass(),
                "the optimised route declines the TYPE_FORMAT kind - the plain legacy generator");
        assertEquals(ModelMetaGenerator.class, provider.modelMetaGenerator(gm, tt).getClass(),
                "the optimised route declines the XMETA kind - the plain legacy generator");
        assertEquals(DeepPathUtilGenerator.class, provider.deepPathUtilGenerator(gm, tt, tu).getClass(),
                "the optimised route declines the DEEP_PATH kind - the plain legacy generator");
        // THE PR-4 DELIBERATE RE-PIN (the un-pin-deliberately contract): the FUNCTION
        // seam is the first flipped family — navigation chains, tranche 1.
        assertEquals(OptimisedFunctionGenerator.class,
                provider.functionGenerator(gm, tt, tu).getClass(),
                "the navigation-chain family (PR-4) flips the FUNCTION seam to the"
                        + " optimised generator");
    }

    @Test
    void dispatchSeamsDeclineEverything() {
        GeneratorModel gm = emptyGm();
        OptimisedIRGenerationProvider provider = new OptimisedIRGenerationProviderImpl();

        assertNull(provider.generateClassesAsIR(new EnumGenerator(gm), null, null,
                new LinkedHashMap<>()),
                "no optimised DISPATCH route has landed (the PR-4 family rides the FUNCTION"
                        + " construction seam) — the caller falls back to Path-1");
        assertFalse(provider.generateMetaAsIR(
                new MetaFieldGenerator(gm, new JavaTypeTranslator(new JavaTypeUtil())),
                new LinkedHashMap<>()),
                "no optimised metafield route has landed — the caller falls back to"
                        + " Path-1");
    }

    @Test
    void seamHelpersExecutePathOneUnderTheSkeleton() {
        GeneratorModel gm = emptyGm();
        assertFalse(Boolean.getBoolean(IRGeneration.OPTIMISED_PROPERTY),
                "precondition: the suite starts flag-off");
        System.setProperty(IRGeneration.OPTIMISED_PROPERTY, "true");
        try {
            // End-to-end through the generator's own seam helper: flag read →
            // ServiceLoader → the skeleton provider → the PLAIN legacy generator.
            assertEquals(EnumGenerator.class, IRGeneration.enumGenerator(gm).getClass(),
                    "flag-on with the skeleton provider ⇒ the seam helper still constructs"
                            + " the plain legacy generator — Path-1 behaviour end-to-end");
            // v3.3 seat 3 (D54): the DATA_RULE seam helper, end-to-end on this route, is the
            // plain legacy generator too (the provider's explicit decline).
            JavaTypeUtil tu = new JavaTypeUtil();
            assertEquals(DataRuleGenerator.class,
                    IRGeneration.dataRuleGenerator(gm, new JavaTypeTranslator(tu), tu).getClass(),
                    "flag-on with the skeleton provider ⇒ the data-rule seam helper constructs"
                            + " the plain legacy generator — the route has no data-rule twin");
        } finally {
            System.clearProperty(IRGeneration.OPTIMISED_PROPERTY);
        }
    }
}
