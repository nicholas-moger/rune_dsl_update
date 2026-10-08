package com.regnosys.rosetta.generator.java.spi;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.JavaClassGenerator;
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
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;

/**
 * Single, centralised access point for the opt-in IR generation routes (the
 * D43 train + the v3 optimised route). Reads the flags
 * {@code -Drosetta.generator.ir} / {@code -Drosetta.generator.ir.optimised}
 * and resolves the matching provider service ({@link IRGenerationProvider} /
 * {@link OptimisedIRGenerationProvider}) — the ServiceLoader inversion of the
 * lab's in-tree flag dispatch (L-002), keeping the shipping generator free of
 * IR imports.
 *
 * <p>Contract: with both flags off (the default), {@link #providerOrNull()}
 * returns {@code null} without touching {@link ServiceLoader}, and every
 * helper below executes the exact legacy Path-1 call — byte-identical by
 * construction. With a flag on but no matching provider on the classpath,
 * generation proceeds on Path-1 after a single {@code System.err} notice (an
 * explicit opt-in silently downgrading would misreport what ran). With BOTH
 * flags on, every call throws — the routes are exclusive by design.
 *
 * <p>The flags are re-read on every call (cheap; keeps the routes toggleable
 * per-generation-run within one JVM); each ServiceLoader lookup result is
 * cached after its first flag-on resolution (the classpath cannot change).
 */
public final class IRGeneration {

    /** JVM system property name: {@code rosetta.generator.ir}. */
    public static final String PROPERTY = "rosetta.generator.ir";

    /**
     * JVM system property name: {@code rosetta.generator.ir.optimised} — the
     * v3 optimised route's own opt-in flag (route 3 of the one-IR/three-backends
     * architecture; the phase plan § 2). Read with {@link Boolean#getBoolean},
     * so the documented spelling is {@code -Drosetta.generator.ir.optimised=true}
     * (the #535 flag-spelling law).
     */
    public static final String OPTIMISED_PROPERTY = "rosetta.generator.ir.optimised";

    private static volatile IRGenerationProvider resolved;
    private static volatile boolean lookupDone;

    private static volatile OptimisedIRGenerationProvider optimisedResolved;
    private static volatile boolean optimisedLookupDone;

    private IRGeneration() {
    }

    /** @return {@code true} iff {@code -Drosetta.generator.ir=true} is set. */
    public static boolean enabled() {
        return Boolean.getBoolean(PROPERTY);
    }

    /** @return {@code true} iff {@code -Drosetta.generator.ir.optimised=true} is set. */
    public static boolean optimisedEnabled() {
        return Boolean.getBoolean(OPTIMISED_PROPERTY);
    }

    /**
     * The resolved provider for the active route, or {@code null} when both
     * flags are off or the active route has no registration on the classpath.
     *
     * <p>Route selection: {@code -Drosetta.generator.ir=true} resolves the
     * reference {@link IRGenerationProvider}; {@code -Drosetta.generator.ir.optimised=true}
     * resolves the {@link OptimisedIRGenerationProvider} (registered under its
     * OWN service file, invisible to the reference lookup). The two routes are
     * exclusive: both flags set is a configuration error, loud on every call.
     *
     * @throws IllegalStateException when BOTH flags are set (the routes are
     *         exclusive — a run must state which backend it means), or when
     *         the active flag is on and MORE than one provider is registered —
     *         classpath-order-dependent selection would make routing
     *         nondeterministic (Copilot #466 R1); the surplus jar must be
     *         removed rather than silently out-ranked
     */
    public static IRGenerationProvider providerOrNull() {
        boolean ir = enabled();
        boolean optimised = optimisedEnabled();
        if (!ir && !optimised) {
            return null;
        }
        if (ir && optimised) {
            // Deliberately un-cached: the config error stays loud on every
            // call until one flag is dropped (the #466 exclusivity posture).
            throw new IllegalStateException("[rune-java-generator] both -D" + PROPERTY
                    + "=true and -D" + OPTIMISED_PROPERTY + "=true are set — the IR routes"
                    + " are exclusive; drop one flag.");
        }
        if (optimised) {
            return optimisedProviderOrNull();
        }
        if (!lookupDone) {
            synchronized (IRGeneration.class) {
                if (!lookupDone) {
                    List<IRGenerationProvider> found = ServiceLoader.load(IRGenerationProvider.class)
                            .stream().map(ServiceLoader.Provider::get).toList();
                    if (found.size() > 1) {
                        // Deliberately BEFORE lookupDone=true: the config error
                        // stays loud on every call until the classpath is fixed.
                        throw new IllegalStateException("[rune-java-generator] -D" + PROPERTY
                                + "=true but " + found.size() + " IRGenerationProvider"
                                + " registrations are on the classpath ("
                                + found.stream().map(p -> p.getClass().getName())
                                        .collect(Collectors.joining(", "))
                                + ") — the IR route is exclusive; remove the surplus jar(s).");
                    }
                    resolved = found.isEmpty() ? null : found.get(0);
                    if (resolved == null) {
                        System.err.println("[rune-java-generator] -D" + PROPERTY
                                + "=true is set but no IRGenerationProvider is on the classpath"
                                + " — generation proceeds on the standard (Path-1) route.");
                    }
                    lookupDone = true;
                }
            }
        }
        return resolved;
    }

    /**
     * The resolved optimised provider, or {@code null} when none is registered.
     * Looked up under {@link OptimisedIRGenerationProvider}'s own service file —
     * the reference route's registrations are never candidates here, and the
     * optimised jar's registration is never a candidate for the reference
     * lookup, so both jars may coexist on one classpath without tripping
     * either route's exclusivity check.
     */
    private static OptimisedIRGenerationProvider optimisedProviderOrNull() {
        if (!optimisedLookupDone) {
            synchronized (IRGeneration.class) {
                if (!optimisedLookupDone) {
                    List<OptimisedIRGenerationProvider> found =
                            ServiceLoader.load(OptimisedIRGenerationProvider.class)
                                    .stream().map(ServiceLoader.Provider::get).toList();
                    if (found.size() > 1) {
                        // Deliberately BEFORE optimisedLookupDone=true: loud on
                        // every call until the classpath is fixed.
                        throw new IllegalStateException("[rune-java-generator] -D" + OPTIMISED_PROPERTY
                                + "=true but " + found.size() + " OptimisedIRGenerationProvider"
                                + " registrations are on the classpath ("
                                + found.stream().map(p -> p.getClass().getName())
                                        .collect(Collectors.joining(", "))
                                + ") — the optimised route is exclusive; remove the surplus jar(s).");
                    }
                    optimisedResolved = found.isEmpty() ? null : found.get(0);
                    if (optimisedResolved == null) {
                        System.err.println("[rune-java-generator] -D" + OPTIMISED_PROPERTY
                                + "=true is set but no OptimisedIRGenerationProvider is on the"
                                + " classpath — generation proceeds on the standard (Path-1) route.");
                    }
                    optimisedLookupDone = true;
                }
            }
        }
        return optimisedResolved;
    }

    /** Construction seam for {@code new EnumGenerator(gm)}. */
    public static EnumGenerator enumGenerator(GeneratorModel gm) {
        IRGenerationProvider p = providerOrNull();
        return p != null ? p.enumGenerator(gm) : new EnumGenerator(gm);
    }

    /** Construction seam for {@code new ModelObjectGenerator(gm, tt, tu)}. */
    public static ModelObjectGenerator modelObjectGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        IRGenerationProvider p = providerOrNull();
        return p != null ? p.modelObjectGenerator(gm, tt, tu) : new ModelObjectGenerator(gm, tt, tu);
    }

    /** Construction seam for {@code new ChoiceObjectGenerator(gm, tt, tu, delegate)}. */
    public static ChoiceObjectGenerator choiceObjectGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu, ModelObjectGenerator delegate) {
        IRGenerationProvider p = providerOrNull();
        return p != null ? p.choiceObjectGenerator(gm, tt, tu, delegate)
                : new ChoiceObjectGenerator(gm, tt, tu, delegate);
    }

    /** Construction seam for {@code new MetaFieldGenerator(gm, tt)}. */
    public static MetaFieldGenerator metaFieldGenerator(GeneratorModel gm, JavaTypeTranslator tt) {
        IRGenerationProvider p = providerOrNull();
        return p != null ? p.metaFieldGenerator(gm, tt) : new MetaFieldGenerator(gm, tt);
    }

    /** Construction seam for {@code new FunctionGenerator(gm, tt, tu)}. */
    public static FunctionGenerator functionGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        IRGenerationProvider p = providerOrNull();
        return p != null ? p.functionGenerator(gm, tt, tu) : new FunctionGenerator(gm, tt, tu);
    }

    /**
     * Construction seam for {@code new DataRuleGenerator(gm, tt, tu)} — the DATA_RULE
     * seam (v3.3 seat 3, D54): with the reference route on, the provider's IR-routed
     * subclass renders every condition body through the IR compiler; flag-off (or the
     * optimised route, which declines the kind) constructs the exact legacy class.
     */
    public static DataRuleGenerator dataRuleGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        IRGenerationProvider p = providerOrNull();
        return p != null ? p.dataRuleGenerator(gm, tt, tu) : new DataRuleGenerator(gm, tt, tu);
    }

    /**
     * Construction seam for {@code new OnlyExistsValidatorGenerator(gm, tt, tu)} - one of the FIVE DERIVED-FILE
     * seams (v3.3 seat 9, PR #645 commit 12): a data type's six files are emitted as ONE unit and five of them are
     * written by five separate per-kind generators, so each needs a seam of its own before any of them can be
     * routed. Flag-off (and the optimised route, which declines the kind) constructs the exact legacy class.
     */
    public static OnlyExistsValidatorGenerator onlyExistsValidatorGenerator(GeneratorModel gm,
            JavaTypeTranslator tt, JavaTypeUtil tu) {
        IRGenerationProvider p = providerOrNull();
        return p != null ? p.onlyExistsValidatorGenerator(gm, tt, tu)
                : new OnlyExistsValidatorGenerator(gm, tt, tu);
    }

    /**
     * Construction seam for {@code new CardinalityValidatorGenerator(gm, tt, tu)} - see
     * {@link #onlyExistsValidatorGenerator}.
     */
    public static CardinalityValidatorGenerator cardinalityValidatorGenerator(GeneratorModel gm,
            JavaTypeTranslator tt, JavaTypeUtil tu) {
        IRGenerationProvider p = providerOrNull();
        return p != null ? p.cardinalityValidatorGenerator(gm, tt, tu)
                : new CardinalityValidatorGenerator(gm, tt, tu);
    }

    /**
     * Construction seam for {@code new TypeFormatValidatorGenerator(gm, tt, tu)} - see
     * {@link #onlyExistsValidatorGenerator}.
     */
    public static TypeFormatValidatorGenerator typeFormatValidatorGenerator(GeneratorModel gm,
            JavaTypeTranslator tt, JavaTypeUtil tu) {
        IRGenerationProvider p = providerOrNull();
        return p != null ? p.typeFormatValidatorGenerator(gm, tt, tu)
                : new TypeFormatValidatorGenerator(gm, tt, tu);
    }

    /** Construction seam for {@code new ModelMetaGenerator(gm, tt)} - see {@link #onlyExistsValidatorGenerator}. */
    public static ModelMetaGenerator modelMetaGenerator(GeneratorModel gm, JavaTypeTranslator tt) {
        IRGenerationProvider p = providerOrNull();
        return p != null ? p.modelMetaGenerator(gm, tt) : new ModelMetaGenerator(gm, tt);
    }

    /**
     * Construction seam for {@code new DeepPathUtilGenerator(gm, tt, tu)} - see
     * {@link #onlyExistsValidatorGenerator}.
     */
    public static DeepPathUtilGenerator deepPathUtilGenerator(GeneratorModel gm, JavaTypeTranslator tt,
            JavaTypeUtil tu) {
        IRGenerationProvider p = providerOrNull();
        return p != null ? p.deepPathUtilGenerator(gm, tt, tu) : new DeepPathUtilGenerator(gm, tt, tu);
    }

    /**
     * Dispatch seam for the per-model generation call: routes through the IR
     * path when the provider claims {@code generator}, else the legacy
     * {@code generator.generateClasses}.
     */
    public static List<GenerationException> generateClasses(JavaClassGenerator<?, ?> generator,
            RModel model, String version, Map<String, String> output) {
        IRGenerationProvider p = providerOrNull();
        if (p != null) {
            List<GenerationException> routed = p.generateClassesAsIR(generator, model, version, output);
            if (routed != null) {
                return routed;
            }
        }
        return generator.generateClasses(model, version, output);
    }

    /**
     * Dispatch seam for the workspace-wide metafield generation: routes
     * through the IR path when the provider claims {@code metaFieldGenerator},
     * else the legacy {@code metaFieldGenerator.generate}.
     */
    public static void generateMeta(MetaFieldGenerator metaFieldGenerator, Map<String, String> output) {
        IRGenerationProvider p = providerOrNull();
        if (p == null || !p.generateMetaAsIR(metaFieldGenerator, output)) {
            metaFieldGenerator.generate(output);
        }
    }
}
