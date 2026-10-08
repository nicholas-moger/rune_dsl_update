package com.regnosys.rosetta.generator.java.ir;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.spi.IRGenerationProvider;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the ServiceLoader seam between the shipping generator and this module
 * (the D43 PR-2 inversion of the lab's in-tree flag dispatch, L-002).
 * Fork-authored (not part of the lab port).
 *
 * <p>What is locked and why:
 * <ul>
 *   <li><b>Property-name agreement</b> — the flag literal lives in TWO modules
 *   (the ported {@link IRFlag} keeps the lab's byte-identical constant; the
 *   generator's {@code IRGeneration} owns the seam's read). A drift between
 *   them would split the opt-in surface silently (the Rule-3 lock).</li>
 *   <li><b>Registration</b> — exactly one provider on this module's classpath,
 *   and it is {@link IRGenerationProviderImpl}. A missing/duplicated
 *   {@code META-INF/services} entry breaks the ON ring while every OFF-ring
 *   suite stays green (flag-off never looks it up) — exactly the silent class
 *   this test exists for.</li>
 *   <li><b>The construction seams</b> — each returns the IR-routed subclass,
 *   and the per-model variants implement {@link IREmittableGenerator} (the
 *   dispatch seam's route key).</li>
 *   <li><b>The dispatch decline contract</b> — a PLAIN (non-IR) generator
 *   returns {@code null}/{@code false} so the caller falls back to Path-1;
 *   the flag-off loader returns no provider at all.</li>
 * </ul>
 */
class IRGenerationSeamTest {

    private static GeneratorModel emptyGm() {
        return new GeneratorModel(RWorkspace.build(List.of()).workspace());
    }

    @Test
    void flagPropertyNameAgreesAcrossModules() {
        assertEquals(IRGeneration.PROPERTY, IRFlag.PROPERTY,
                "the generator's seam and the ported IRFlag must read the SAME system property");
    }

    @Test
    void serviceRegistrationResolvesExactlyThisProvider() {
        List<IRGenerationProvider> found = ServiceLoader.load(IRGenerationProvider.class)
                .stream().map(ServiceLoader.Provider::get).toList();
        assertEquals(1, found.size(), "exactly one IRGenerationProvider registration expected");
        assertInstanceOf(IRGenerationProviderImpl.class, found.get(0));
    }

    @Test
    void flagOffLoaderReturnsNoProvider() {
        assertFalse(Boolean.getBoolean(IRGeneration.PROPERTY),
                "precondition: this suite runs flag-off (the OFF contract)");
        assertNull(IRGeneration.providerOrNull(),
                "flag off ⇒ no provider, no ServiceLoader lookup — Path-1 by construction");
    }

    @Test
    void constructionSeamsReturnIRRoutedVariants() {
        GeneratorModel gm = emptyGm();
        JavaTypeUtil tu = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(tu);
        IRGenerationProvider provider = new IRGenerationProviderImpl();

        assertInstanceOf(IREnumGenerator.class, provider.enumGenerator(gm));
        assertInstanceOf(IREmittableGenerator.class, provider.enumGenerator(gm));
        assertInstanceOf(IRModelObjectGenerator.class, provider.modelObjectGenerator(gm, tt, tu));
        assertInstanceOf(IREmittableGenerator.class, provider.modelObjectGenerator(gm, tt, tu));
        assertInstanceOf(IRChoiceObjectGenerator.class,
                provider.choiceObjectGenerator(gm, tt, tu, provider.modelObjectGenerator(gm, tt, tu)));
        assertInstanceOf(IRMetaFieldGenerator.class, provider.metaFieldGenerator(gm, tt));
        assertInstanceOf(IREmittableMetaGenerator.class, provider.metaFieldGenerator(gm, tt));
        assertInstanceOf(IRFunctionGenerator.class, provider.functionGenerator(gm, tt, tu));
    }

    /**
     * THE FIVE DERIVED-FILE SEAMS (v3.3 seat 9, PR #645 commit 12): a data type's six files are emitted as ONE unit
     * and five of them are written by five separate per-kind generators, so each needs an IR-routed variant of its
     * own before any derived file can be routed at all. Each is a SUBCLASS of its legacy generator and an
     * {@link IREmittableGenerator} - the dispatch seam's route key.
     */
    @Test
    void theFiveDerivedConstructionSeamsReturnIRRoutedVariants() {
        GeneratorModel gm = emptyGm();
        JavaTypeUtil tu = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(tu);
        IRGenerationProvider provider = new IRGenerationProviderImpl();

        assertInstanceOf(IROnlyExistsValidatorGenerator.class,
                provider.onlyExistsValidatorGenerator(gm, tt, tu));
        assertInstanceOf(IREmittableGenerator.class, provider.onlyExistsValidatorGenerator(gm, tt, tu));
        assertInstanceOf(IRCardinalityValidatorGenerator.class,
                provider.cardinalityValidatorGenerator(gm, tt, tu));
        assertInstanceOf(IREmittableGenerator.class, provider.cardinalityValidatorGenerator(gm, tt, tu));
        assertInstanceOf(IRTypeFormatValidatorGenerator.class,
                provider.typeFormatValidatorGenerator(gm, tt, tu));
        assertInstanceOf(IREmittableGenerator.class, provider.typeFormatValidatorGenerator(gm, tt, tu));
        assertInstanceOf(IRModelMetaGenerator.class, provider.modelMetaGenerator(gm, tt));
        assertInstanceOf(IREmittableGenerator.class, provider.modelMetaGenerator(gm, tt));
        assertInstanceOf(IRDeepPathUtilGenerator.class, provider.deepPathUtilGenerator(gm, tt, tu));
        assertInstanceOf(IREmittableGenerator.class, provider.deepPathUtilGenerator(gm, tt, tu));
    }

    @Test
    void dispatchSeamsDeclinePlainGenerators() {
        GeneratorModel gm = emptyGm();
        IRGenerationProvider provider = new IRGenerationProviderImpl();

        assertNull(provider.generateClassesAsIR(new EnumGenerator(gm), null, null,
                new LinkedHashMap<>()),
                "a plain (non-IR) generator has no IR route — the caller falls back to Path-1");
        assertFalse(provider.generateMetaAsIR(
                new MetaFieldGenerator(gm, new JavaTypeTranslator(new JavaTypeUtil())),
                new LinkedHashMap<>()),
                "a plain MetaFieldGenerator has no IR route — the caller falls back to Path-1");
    }

    @Test
    void flagOnLoaderResolvesTheRegisteredProvider() {
        assertFalse(Boolean.getBoolean(IRGeneration.PROPERTY),
                "precondition: the suite starts flag-off");
        System.setProperty(IRGeneration.PROPERTY, "true");
        try {
            IRGenerationProvider p = IRGeneration.providerOrNull();
            assertTrue(p instanceof IRGenerationProviderImpl,
                    "flag on + this jar on the classpath ⇒ the registered provider resolves");
        } finally {
            System.clearProperty(IRGeneration.PROPERTY);
        }
        assertNull(IRGeneration.providerOrNull(),
                "the flag is re-read per call — clearing it re-closes the route even though the"
                        + " ServiceLoader lookup result stays cached");
    }
}
