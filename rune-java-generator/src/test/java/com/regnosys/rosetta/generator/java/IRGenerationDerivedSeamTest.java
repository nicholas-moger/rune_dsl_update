package com.regnosys.rosetta.generator.java;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.generator.java.object.ModelMetaGenerator;
import com.regnosys.rosetta.generator.java.object.deeppath.DeepPathUtilGenerator;
import com.regnosys.rosetta.generator.java.object.validators.CardinalityValidatorGenerator;
import com.regnosys.rosetta.generator.java.object.validators.OnlyExistsValidatorGenerator;
import com.regnosys.rosetta.generator.java.object.validators.TypeFormatValidatorGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * THE FIVE DERIVED-FILE CONSTRUCTION SEAMS, FLAG-OFF (v3.3 seat 9, PR #645 commit 12).
 *
 * <p>A data type's six generated files are emitted as ONE unit, and five of them are written by five separate
 * per-kind generators. Until this commit the IR route had construction seams for six OTHER kinds and none for these,
 * so no derived file could ever be routed through the IR at all. The seams exist now - and this class states the
 * half of their contract the shipping generator owns: <b>with the flag off the seam constructs the EXACT legacy
 * class</b>, so every call site may be re-cut to {@code IRGeneration} without moving one byte of Path-1.
 *
 * <p>{@code assertEquals(X.class, …getClass())} rather than {@code assertInstanceOf}: an {@code instanceof} check
 * would pass for a subclass, which is precisely the failure this pins against - the IR-routed variants ARE
 * subclasses of these five.
 */
class IRGenerationDerivedSeamTest {

    private static GeneratorModel emptyGm() {
        return new GeneratorModel(RWorkspace.build(List.of()).workspace());
    }

    @Test
    void theFlagIsOffInThisSuiteAndNoProviderResolves() {
        assertFalse(Boolean.getBoolean(IRGeneration.PROPERTY),
                "precondition: the shipping generator's own suite runs flag-off (the OFF contract)");
        assertNull(IRGeneration.providerOrNull(),
                "flag off means no provider and no ServiceLoader lookup - Path-1 by construction");
    }

    @Test
    void theFiveDerivedSeamsConstructTheExactLegacyClassesWithTheFlagOff() {
        GeneratorModel gm = emptyGm();
        JavaTypeUtil tu = new JavaTypeUtil();
        JavaTypeTranslator tt = new JavaTypeTranslator(tu);

        assertEquals(OnlyExistsValidatorGenerator.class,
                IRGeneration.onlyExistsValidatorGenerator(gm, tt, tu).getClass(),
                "flag off: the ONLY_EXISTS seam constructs the legacy generator, not a subclass");
        assertEquals(CardinalityValidatorGenerator.class,
                IRGeneration.cardinalityValidatorGenerator(gm, tt, tu).getClass(),
                "flag off: the CARDINALITY seam constructs the legacy generator, not a subclass");
        assertEquals(TypeFormatValidatorGenerator.class,
                IRGeneration.typeFormatValidatorGenerator(gm, tt, tu).getClass(),
                "flag off: the TYPE_FORMAT seam constructs the legacy generator, not a subclass");
        assertEquals(ModelMetaGenerator.class,
                IRGeneration.modelMetaGenerator(gm, tt).getClass(),
                "flag off: the XMETA seam constructs the legacy generator, not a subclass");
        assertEquals(DeepPathUtilGenerator.class,
                IRGeneration.deepPathUtilGenerator(gm, tt, tu).getClass(),
                "flag off: the DEEP_PATH seam constructs the legacy generator, not a subclass");
    }
}
