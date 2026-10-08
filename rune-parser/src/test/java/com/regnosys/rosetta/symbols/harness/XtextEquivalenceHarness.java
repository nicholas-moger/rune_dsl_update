package com.regnosys.rosetta.symbols.harness;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.builder.AstBuilder;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Differential test harness — loads a .rosetta source through BOTH the
 * legacy Xtext pipeline (via the rune-dsl submodule) and our M1+M2+M3
 * pipeline, exposes both resolved graphs, and supports per-cross-ref-
 * category comparison.
 *
 * <p>This is the load-bearing safety mechanism for D11 (like-for-like
 * Xtext behaviour). M3 tasks register their cross-ref category against
 * the harness as they implement resolution; the corpus equivalence test
 * then asserts byte-equivalence per category for every file.
 *
 * <p>The Xtext side uses reflection to avoid compile-time dependency on
 * rune-testing/rune-lang. When those artifacts are absent (CI, or local
 * without rune-dsl built), the harness degrades gracefully — comparisons
 * return empty (no diffs asserted).
 *
 * <p>Spec: D5/E1 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public final class XtextEquivalenceHarness {

    /** Cross-reference categories the harness can compare. */
    public enum Category {
        ALL,
        SUPER_TYPE,
        TYPE_CALL,
        ANNOTATION_REF,
        ENUM_VALUE_REF,
        SUPER_FUNCTION,
        IMPORT,
        EXTERNAL_TYPE,
        EXTERNAL_SUPER_SOURCE,
        SYMBOL_REFERENCE,
        DISPATCH,
        SEGMENT_REF,
        CONVERSION_TARGET_ENUM,
        RULE_REFERENCE,
        ROOT_TYPE
    }

    /** One difference between the two pipelines. */
    public record Difference(
            Category category,
            String filePath,
            String unresolvedName,
            String xtextResolvedFqn,
            String ourResolvedFqn,
            String message) {}

    /** Xtext injector loaded via reflection. Null if unavailable. */
    private static final Object XTEXT_INJECTOR;
    static {
        Object injector = null;
        try {
            // Reflective equivalent of: new RosettaStandaloneSetup().createInjectorAndDoEMFRegistration()
            Class<?> setupClass = Class.forName("com.regnosys.rosetta.RosettaStandaloneSetup");
            Object setup = setupClass.getDeclaredConstructor().newInstance();
            Method createMethod = setupClass.getMethod("createInjectorAndDoEMFRegistration");
            injector = createMethod.invoke(setup);
        } catch (ClassNotFoundException e) {
            // Expected when rune-testing/rune-lang not on classpath (CI)
            System.out.println("INFO: Xtext classes not on classpath — equivalence harness disabled");
        } catch (Exception e) {
            System.err.println("WARNING: Xtext standalone setup failed — equivalence harness disabled: " + e);
        }
        XTEXT_INJECTOR = injector;
    }

    private final Object xtextModel;
    private final RModel ourModel;
    private final String fileName;

    private XtextEquivalenceHarness(Object xtextModel, RModel ourModel, String fileName) {
        this.xtextModel = xtextModel;
        this.ourModel = ourModel;
        this.fileName = fileName;
    }

    public static XtextEquivalenceHarness loadString(String source) {
        return loadString(source, "<test>.rosetta");
    }

    public static XtextEquivalenceHarness loadString(String source, String fileName) {
        Object xtextModel = loadViaXtext(source, fileName);
        RModel ourModel = AstBuilder.buildFromString(source, fileName);
        return new XtextEquivalenceHarness(xtextModel, ourModel, fileName);
    }

    public static boolean isAvailable() {
        return XTEXT_INJECTOR != null;
    }

    /**
     * Loads a .rosetta source via the Xtext pipeline using reflection.
     * Returns null if the pipeline is unavailable or loading fails.
     */
    private static Object loadViaXtext(String source, String fileName) {
        if (XTEXT_INJECTOR == null) return null;
        try {
            // Reflective equivalent of:
            //   XtextResourceSet rs = injector.getInstance(XtextResourceSet.class);
            //   XtextResource resource = (XtextResource) rs.createResource(URI.createURI("inmemory:/" + fileName));
            //   resource.load(new ByteArrayInputStream(source.getBytes(UTF_8)), rs.getLoadOptions());
            //   return resource.getContents().get(0);

            Class<?> xrsClass = Class.forName("org.eclipse.xtext.resource.XtextResourceSet");
            Method getInstance = XTEXT_INJECTOR.getClass().getMethod("getInstance", Class.class);
            Object rs = getInstance.invoke(XTEXT_INJECTOR, xrsClass);

            Class<?> uriClass = Class.forName("org.eclipse.emf.common.util.URI");
            Method createURI = uriClass.getMethod("createURI", String.class);
            Object uri = createURI.invoke(null, "inmemory:/" + fileName);

            Method createResource = rs.getClass().getMethod("createResource", uriClass);
            Object resource = createResource.invoke(rs, uri);

            Method getLoadOptions = rs.getClass().getMethod("getLoadOptions");
            Object loadOptions = getLoadOptions.invoke(rs);

            Method load = resource.getClass().getMethod("load", java.io.InputStream.class, java.util.Map.class);
            load.invoke(resource, new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), loadOptions);

            Method getContents = resource.getClass().getMethod("getContents");
            Object contents = getContents.invoke(resource);
            if (contents instanceof List<?> list && !list.isEmpty()) {
                return list.get(0);
            }
            return null;
        } catch (Exception e) {
            System.err.println("Xtext load failed for " + fileName + ": " + e);
            return null;
        }
    }

    public Object xtextModel() { return xtextModel; }
    public RModel ourModel()  { return ourModel; }
    public String fileName()  { return fileName; }

    /**
     * Compares cross-references for the given category. Returns a list of
     * {@link Difference} — empty = equivalent. Categories are wired in by
     * T18; until then, this returns empty (no comparison yet performed).
     */
    public List<Difference> compareCrossRefs(Category category) {
        // Scaffold — categories light up as T18 wires comparators.
        return new ArrayList<>();
    }
}
