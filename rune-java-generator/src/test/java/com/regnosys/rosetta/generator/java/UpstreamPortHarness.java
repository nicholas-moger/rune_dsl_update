package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.HoldOutByteCompareTest.GenerationRun;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;

import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The leg-C port harness (PR #414, W42 sweep leg C — the upstream
 * {@code rune-integration-tests} port; design doc
 * {@code docs/specs/2026-07-15-w42-robustness-sweep-design.md} §C).
 *
 * <p>Mirrors upstream's test plumbing on the fork pipeline, one seam at a time:
 * <ul>
 *   <li><b>{@code ModelHelper} semantics</b> — a snippet that does not start with
 *       {@code namespace}/{@code override} gets the default header
 *       ({@code namespace "com.rosetta.test.model"} + {@code version "test"}); this
 *       mirrors upstream's MULTI-model law (its single-model path checks
 *       {@code namespace} only — unobservable here, no ported snippet starts with
 *       {@code override}); every parse additionally loads the builtins AND the
 *       {@code commonTestTypes} companion model ({@code metaType scheme string} —
 *       the builtins declare NO metaTypes, so upstream's companion is load-bearing
 *       for {@code [metadata scheme]} snippets).</li>
 *   <li><b>{@code CodeGeneratorTestHelper.generateCode}</b> — the fork's full-battery
 *       pipeline ({@link HoldOutByteCompareTest#generateAllKindsFromModels}) over the
 *       snippet models; output re-keyed by Java class FQN. {@code package-info}
 *       entries are skipped: upstream's test maps never CONTAIN one (its
 *       {@code JavaPackageInfoGenerator} emits only for models with a definition,
 *       which test snippets lack — the fork generator has the same gate), and the
 *       skip is load-bearing here regardless — {@code compileToClasses}'s
 *       {@code Class.forName} loop cannot load a class-less {@code package-info}
 *       (Seat-1 #414 MF-1: the earlier "upstream null-className filter" wording was
 *       wrong — xtext's {@code getJavaClassName()} is non-null for package-info).</li>
 *   <li><b>{@code compileToClasses}</b> — in-memory {@code javac} against the SAME
 *       gate classpath as {@code HoldOutCompileGateTest} (the oracle law: the
 *       RELEASED {@code rune-runtime:9.83.0} surface, never the vendored snapshot),
 *       loaded in an ISOLATED loader (parent = platform + the gate classpath) so
 *       runtime types resolve from the released jar — invoke results cross the
 *       loader boundary only as JDK types or reflective {@code Object} handles.
 *       DISCLOSED DIVERGENCE (Seat-1 #414 MF-2): upstream's helper compiles with
 *       {@code --release 8 -Xlint:all -Xdiags:verbose}; this harness deliberately
 *       keeps the GATE's options ({@code -classpath …, -proc:none}) — one compile
 *       law across all fork bars. Inert for the current ports (all emission shapes
 *       are corpus/hold-out-locked Java-8 forms); revisit at the slice-3
 *       text+invoke ports if a post-8-platform-API regression matters.</li>
 *   <li><b>{@code FunctionGeneratorHelper.createFunc/invokeFunc}</b> — a reflective
 *       mini-injector replaying upstream's Guice bindings: {@code @ImplementedBy}
 *       just-in-time resolution, {@code ConditionValidator →
 *       DefaultConditionValidator}, {@code ModelObjectValidator →
 *       NoOpModelObjectValidator}, function-typed {@code @Inject} fields recursively
 *       instantiated. Per-class SINGLETONS — a deliberate cycle-breaking deviation
 *       from Guice's new-instance-per-injection-point JIT semantics, inert for
 *       stateless generated functions.</li>
 *   <li><b>{@code createInstanceUsingBuilder} / {@code createEnumInstance} /
 *       {@code createFieldWithMetaString}</b> — upstream's reflective builder
 *       helpers, kept fully reflective (see the loader-isolation note above).</li>
 * </ul>
 *
 * <p>Port tests that COMPILE or INVOKE generated code SKIP (assumption) when the
 * released runtime jar is absent — the same gate as {@code HoldOutCompileGateTest}
 * (each such class calls {@link #assumeReleasedRuntime} in {@code @BeforeAll}).
 * Generation-only ports (e.g. {@code UpstreamDocReferencePortTest}) need no jar
 * and run everywhere.
 */
final class UpstreamPortHarness {

    private UpstreamPortHarness() { }

    /** Upstream {@code ModelHelper.getVersionInfo()} — byte-for-byte. */
    static final String TEST_NS_HEADER = "namespace \"com.rosetta.test.model\"\nversion \"test\"\n";

    /** Upstream {@code ModelHelper.commonTestTypes} — byte-for-byte. */
    static final String COMMON_TEST_TYPES = TEST_NS_HEADER + "metaType scheme string\n";

    static final String ROOT_PACKAGE = "com.rosetta.test.model";

    // ---------------------------------------------------------------- generate

    /**
     * Parse + generate the snippet(s) through the fork's full battery, failing the
     * test on any parse/generation error (upstream's {@code parseRosettaWithNoErrors}
     * + {@code generateCode} contract). Output keyed by emission path.
     */
    static Map<String, String> generate(String... snippets) {
        GenerationRun run = generateWithErrors(snippets);
        if (!run.errorMessages().isEmpty()) {
            Assertions.fail("generation error(s) on ported snippet:\n  - "
                    + String.join("\n  - ", run.errorMessages()));
        }
        return run.output();
    }

    /** The error-returning form (for ports that assert ON generation errors). */
    static GenerationRun generateWithErrors(String... snippets) {
        List<RModel> models = parseSnippetModels(snippets);
        Set<RModel> groupModels = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        groupModels.addAll(models.subList(models.size() - snippets.length, models.size()));
        return HoldOutByteCompareTest.generateAllKindsFromModels(models, groupModels);
    }

    /**
     * Builtins + the {@code commonTestTypes} companion + the header-prepended,
     * version-stamped snippet models (the snippet models are the LAST
     * {@code snippets.length} entries — callers derive the emit set from the
     * tail). The single snippet-prep law for {@link #generateWithErrors} and
     * {@link #generateWithNamespaceFilter} (Seat-1 #423 OBS-5: one
     * implementation, no drift).
     */
    private static List<RModel> parseSnippetModels(String... snippets) {
        try {
            List<RModel> models = new ArrayList<>();
            for (Path p : HoldOutByteCompareTest.resolveBuiltinFiles()) {
                models.add(AstBuilder.buildFromFile(p));
            }
            models.add(AstBuilder.buildFromString(COMMON_TEST_TYPES, "common-test-types.rosetta"));
            int i = 0;
            for (String snippet : snippets) {
                String trimmed = snippet.trim();
                String src = (trimmed.startsWith("namespace") || trimmed.startsWith("override"))
                        ? snippet
                        : TEST_NS_HEADER + "\n" + snippet;
                RModel m = AstBuilder.buildFromString(src, "port-snippet-" + (i++) + ".rosetta");
                if (m.version().isEmpty()) {
                    // The version law (PR #410): upstream's unversioned models emit
                    // @version 0.0.0 (RosettaModelImpl.VERSION_EDEFAULT).
                    m.setVersion(HoldOutByteCompareTest.UPSTREAM_VERSION_DEFAULT);
                }
                models.add(m);
            }
            return models;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Re-key an emission-path map by Java class FQN (upstream's
     * {@code generateCode} map shape). {@code package-info} entries are skipped —
     * upstream test maps never contain one (the definition gate; see the class
     * javadoc), and {@link #compileToClasses}'s {@code Class.forName} loop cannot
     * load a class-less {@code package-info}.
     */
    static Map<String, String> byClassName(Map<String, String> byPath) {
        Map<String, String> out = new LinkedHashMap<>();
        byPath.forEach((path, content) -> {
            if (path.endsWith("package-info.java")) {
                return;
            }
            String cls = path.substring(0, path.length() - ".java".length()).replace('/', '.');
            out.put(cls, content);
        });
        return out;
    }

    /** Upstream one-liner: generate + re-key. */
    static Map<String, String> generateCode(String... snippets) {
        return byClassName(generate(snippets));
    }

    // ---------------------------------------------------------------- compile

    /** Skip (assumption) when the released 9.83.0 runtime jar is absent. */
    static void assumeReleasedRuntime() {
        Assumptions.assumeTrue(HoldOutCompileGateTest.released983RuntimeJar() != null,
                "released rune-runtime:9.83.0 absent from ~/.m2 — leg-C port test skipped");
    }

    /** An in-memory source keyed by class FQN. */
    private static final class InMemorySource extends SimpleJavaFileObject {
        private final String content;

        InMemorySource(String classFqn, String content) {
            super(URI.create("string:///" + classFqn.replace('.', '/') + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }

    /** An in-memory class-bytes sink keyed by binary name. */
    private static final class InMemoryClassFile extends SimpleJavaFileObject {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        InMemoryClassFile(String binaryName) {
            super(URI.create("mem:///" + binaryName.replace('.', '/') + Kind.CLASS.extension),
                    Kind.CLASS);
        }

        @Override
        public OutputStream openOutputStream() {
            return bytes;
        }
    }

    /**
     * Upstream {@code compileToClasses}: in-memory javac over the class-FQN-keyed
     * sources, gate classpath, isolated loader. Returns source FQN → loaded Class.
     */
    static Map<String, Class<?>> compileToClasses(Map<String, String> byClassName) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Assertions.assertNotNull(compiler, "system Java compiler unavailable (JRE-only JVM?)");

        Map<String, InMemoryClassFile> classFiles = new LinkedHashMap<>();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager standard = compiler.getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            JavaFileManager fm = new ForwardingJavaFileManager<StandardJavaFileManager>(standard) {
                @Override
                public JavaFileObject getJavaFileForOutput(Location location, String className,
                        JavaFileObject.Kind kind, FileObject sibling) {
                    return classFiles.computeIfAbsent(className, InMemoryClassFile::new);
                }
            };
            List<JavaFileObject> units = new ArrayList<>();
            byClassName.forEach((cls, src) -> units.add(new InMemorySource(cls, src)));
            List<String> options = List.of(
                    "-classpath", HoldOutCompileGateTest.gateClasspath(),
                    "-proc:none");
            Boolean ok = compiler.getTask(null, fm, diagnostics, options, null, units).call();
            if (!Boolean.TRUE.equals(ok)) {
                StringBuilder sb = new StringBuilder("ported snippet failed to compile:\n");
                diagnostics.getDiagnostics().forEach(d -> sb.append("  ").append(d).append('\n'));
                Assertions.fail(sb.toString());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        ClassLoader loader = newIsolatedLoader(classFiles);
        Map<String, Class<?>> classes = new LinkedHashMap<>();
        for (String cls : byClassName.keySet()) {
            try {
                classes.put(cls, Class.forName(cls, false, loader));
            } catch (ClassNotFoundException e) {
                throw new AssertionError("compiled class not loadable: " + cls, e);
            }
        }
        return classes;
    }

    /**
     * Parent = platform loader + the gate classpath as URLs (the released runtime
     * surface, never this JVM's vendored-snapshot classpath); child = the in-memory
     * compiled classes.
     */
    private static ClassLoader newIsolatedLoader(Map<String, InMemoryClassFile> classFiles) {
        List<URL> urls = new ArrayList<>();
        for (String entry : HoldOutCompileGateTest.gateClasspath().split(File.pathSeparator)) {
            try {
                urls.add(Path.of(entry).toUri().toURL());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        URLClassLoader parent = new URLClassLoader(
                urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader());
        return new ClassLoader(parent) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                InMemoryClassFile file = classFiles.get(name);
                if (file == null) {
                    throw new ClassNotFoundException(name);
                }
                byte[] bytes = file.bytes.toByteArray();
                return defineClass(name, bytes, 0, bytes.length);
            }
        };
    }

    // ---------------------------------------------------------------- invoke

    /**
     * Upstream {@code FunctionGeneratorHelper.createFunc}: instantiate the generated
     * function through the reflective mini-injector (default package
     * {@code com.rosetta.test.model.functions}).
     */
    static Object createFunc(Map<String, Class<?>> classes, String funcName) {
        return createFunc(classes, funcName, ROOT_PACKAGE + ".functions");
    }

    static Object createFunc(Map<String, Class<?>> classes, String funcName, String packageName) {
        Class<?> cls = classes.get(packageName + "." + funcName);
        Assertions.assertNotNull(cls, "generated function class missing: "
                + packageName + "." + funcName + " (generated: " + classes.keySet() + ")");
        return new MiniInjector(cls.getClassLoader()).instance(cls);
    }

    /**
     * Upstream's Guice bindings, replayed reflectively: {@code @ImplementedBy}
     * just-in-time resolution + the two validator bindings + recursive function-dep
     * injection with per-class singletons.
     */
    private static final class MiniInjector {
        private final ClassLoader loader;
        private final Map<Class<?>, Object> singletons = new IdentityHashMap<>();

        MiniInjector(ClassLoader loader) {
            this.loader = loader;
        }

        Object instance(Class<?> requested) {
            Class<?> target = resolve(requested);
            Object existing = singletons.get(target);
            if (existing != null) {
                return existing;
            }
            try {
                var ctor = target.getDeclaredConstructor();
                ctor.setAccessible(true);
                Object obj = ctor.newInstance();
                singletons.put(target, obj);
                inject(obj);
                return obj;
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("mini-injector could not instantiate " + target, e);
            }
        }

        private Class<?> resolve(Class<?> requested) {
            try {
                if (requested.getName().equals("com.rosetta.model.lib.functions.ConditionValidator")) {
                    return loader.loadClass("com.rosetta.model.lib.functions.DefaultConditionValidator");
                }
                if (requested.getName().equals("com.rosetta.model.lib.functions.ModelObjectValidator")) {
                    return loader.loadClass("com.rosetta.model.lib.functions.NoOpModelObjectValidator");
                }
            } catch (ClassNotFoundException e) {
                throw new AssertionError("released runtime lacks a validator binding target", e);
            }
            if (Modifier.isAbstract(requested.getModifiers()) || requested.isInterface()) {
                for (Annotation a : requested.getAnnotations()) {
                    if (a.annotationType().getName().equals("com.google.inject.ImplementedBy")) {
                        try {
                            return (Class<?>) a.annotationType().getMethod("value").invoke(a);
                        } catch (ReflectiveOperationException e) {
                            throw new AssertionError("@ImplementedBy unreadable on " + requested, e);
                        }
                    }
                }
                throw new AssertionError("mini-injector: abstract " + requested
                        + " carries no @ImplementedBy — no JIT binding to replay");
            }
            return requested;
        }

        private void inject(Object obj) {
            for (Class<?> c = obj.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (!hasInjectAnnotation(f)) {
                        continue;
                    }
                    f.setAccessible(true);
                    try {
                        if (f.get(obj) == null) {
                            f.set(obj, instance(f.getType()));
                        }
                    } catch (IllegalAccessException e) {
                        throw new AssertionError("mini-injector could not inject " + f, e);
                    }
                }
            }
        }

        private static boolean hasInjectAnnotation(Field f) {
            for (Annotation a : f.getAnnotations()) {
                String n = a.annotationType().getName();
                if (n.equals("javax.inject.Inject") || n.equals("jakarta.inject.Inject")
                        || n.equals("com.google.inject.Inject")) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Upstream {@code invokeFunc}: reflective {@code evaluate(...)} dispatch by
     * assignability (null-tolerant), unwrapping {@link InvocationTargetException}
     * to its cause exactly as upstream does.
     */
    @SuppressWarnings("unchecked")
    static <T> T invokeFunc(Object func, Class<T> resultClass, Object... inputs) {
        Class<?>[] argTypes = new Class<?>[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            argTypes[i] = inputs[i] == null ? null : inputs[i].getClass();
        }
        Method evaluate = getMatchingMethod(func.getClass(), "evaluate", argTypes);
        Assertions.assertNotNull(evaluate, "no matching evaluate(...) on " + func.getClass()
                + " for arg types " + java.util.Arrays.toString(argTypes));
        try {
            evaluate.setAccessible(true);
            return (T) evaluate.invoke(func, inputs);
        } catch (InvocationTargetException e) {
            throw asUnchecked(e.getCause());
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static RuntimeException asUnchecked(Throwable t) {
        if (t instanceof RuntimeException re) {
            return re;
        }
        if (t instanceof Error err) {
            throw err;
        }
        return new RuntimeException(t);
    }

    /** Upstream {@code CodeGeneratorTestHelper.getMatchingMethod} — same match law. */
    static Method getMatchingMethod(Class<?> clazz, String name, Class<?>[] argTypes) {
        for (Method m : clazz.getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != argTypes.length) {
                continue;
            }
            boolean match = true;
            Class<?>[] params = m.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                if (argTypes[i] != null && !params[i].isAssignableFrom(argTypes[i])) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return m;
            }
        }
        return null;
    }

    // ------------------------------------------------------------- instances

    /** Upstream {@code createInstanceUsingBuilder} (root-package overload). */
    static Object createInstanceUsingBuilder(Map<String, Class<?>> classes, String className,
            Map<String, Object> itemsToSet) {
        return createInstanceUsingBuilder(classes, ROOT_PACKAGE, className, itemsToSet, Map.of());
    }

    static Object createInstanceUsingBuilder(Map<String, Class<?>> classes, String className,
            Map<String, Object> itemsToSet, Map<String, List<?>> itemsToAddToList) {
        return createInstanceUsingBuilder(classes, ROOT_PACKAGE, className, itemsToSet, itemsToAddToList);
    }

    static Object createInstanceUsingBuilder(Map<String, Class<?>> classes, String namespace,
            String className, Map<String, Object> itemsToSet, Map<String, List<?>> itemsToAddToList) {
        Object builder = createBuilderInstance(classes, namespace, className);
        itemsToSet.forEach((name, value) -> setAttribute(builder, name, value));
        itemsToAddToList.forEach((name, values) -> {
            for (Object value : values) {
                Method adder = getMatchingMethod(builder.getClass(),
                        "add" + toFirstUpper(name), new Class<?>[] { value.getClass() });
                Assertions.assertNotNull(adder, "no add" + toFirstUpper(name) + "("
                        + value.getClass().getSimpleName() + ") on " + builder.getClass());
                invokeUnchecked(adder, builder, value);
            }
        });
        return build(builder);
    }

    /** Upstream {@code createBuilderInstance}. */
    static Object createBuilderInstance(Map<String, Class<?>> classes, String namespace, String className) {
        Class<?> cls = classes.get(namespace + "." + className);
        Assertions.assertNotNull(cls, "generated class missing: " + namespace + "." + className
                + " (generated: " + classes.keySet() + ")");
        try {
            return cls.getMethod("builder").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("builder() failed on " + cls, e);
        }
    }

    static Object createBuilderInstance(Map<String, Class<?>> classes, String className) {
        return createBuilderInstance(classes, ROOT_PACKAGE, className);
    }

    /** Upstream {@code setAttribute} — set via the assignability-matched setter. */
    static void setAttribute(Object builder, String name, Object value) {
        Method setter = getMatchingMethod(builder.getClass(), "set" + toFirstUpper(name),
                new Class<?>[] { value == null ? null : value.getClass() });
        Assertions.assertNotNull(setter, "no set" + toFirstUpper(name) + "("
                + (value == null ? "null" : value.getClass().getSimpleName()) + ") on " + builder.getClass());
        invokeUnchecked(setter, builder, value);
    }

    /** {@code build()} on a builder (reflective). */
    static Object build(Object builder) {
        try {
            return builder.getClass().getMethod("build").invoke(builder);
        } catch (InvocationTargetException e) {
            throw asUnchecked(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("build() failed on " + builder.getClass(), e);
        }
    }

    /** {@code toBuilder()} on a built instance (reflective). */
    static Object toBuilder(Object built) {
        try {
            return built.getClass().getMethod("toBuilder").invoke(built);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("toBuilder() failed on " + built.getClass(), e);
        }
    }

    /** Reflective getter: {@code getX()} / arbitrary no-arg method. */
    static Object call(Object target, String method) {
        try {
            Method m = target.getClass().getMethod(method);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (InvocationTargetException e) {
            throw asUnchecked(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(method + "() failed on " + target.getClass(), e);
        }
    }

    /** Upstream {@code createEnumInstance}. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object createEnumInstance(Map<String, Class<?>> classes, String className, String enumValue) {
        Class<?> cls = classes.get(ROOT_PACKAGE + "." + className);
        Assertions.assertNotNull(cls, "generated enum missing: " + ROOT_PACKAGE + "." + className);
        return Enum.valueOf((Class<? extends Enum>) cls.asSubclass(Enum.class), enumValue);
    }

    /** Upstream {@code createFieldWithMetaString} — fully reflective (loader isolation). */
    static Object createFieldWithMetaString(Map<String, Class<?>> classes, String value, String scheme) {
        Class<?> fwms = classes.get("com.rosetta.model.metafields.FieldWithMetaString");
        Assertions.assertNotNull(fwms, "FieldWithMetaString not generated (no [metadata scheme] in snippet?)");
        try {
            ClassLoader loader = fwms.getClassLoader();
            Class<?> metaFields = loader.loadClass("com.rosetta.model.metafields.MetaFields");
            Object metaBuilder = metaFields.getMethod("builder").invoke(null);
            setAttribute(metaBuilder, "scheme", scheme);

            Object fieldBuilder = fwms.getMethod("builder").invoke(null);
            setAttribute(fieldBuilder, "value", value);
            Method setMeta = getMatchingMethod(fieldBuilder.getClass(), "setMeta",
                    new Class<?>[] { metaBuilder.getClass() });
            Assertions.assertNotNull(setMeta, "no setMeta(MetaFields) on " + fieldBuilder.getClass());
            invokeUnchecked(setMeta, fieldBuilder, metaBuilder);
            return build(fieldBuilder);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("createFieldWithMetaString failed", e);
        }
    }

    private static Object invokeUnchecked(Method m, Object target, Object... args) {
        try {
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw asUnchecked(e.getCause());
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    static String toFirstUpper(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ------------------------------------------------------ filtered generate

    /**
     * The emission-filtered form of {@link #generate} (PR #423, leg-C slice 2 —
     * upstream {@code ModelMetaGeneratorFilteredNamespaceTest}): upstream's
     * {@code rosetta-config.yml} {@code generators.namespaces} channel maps to
     * the fork's {@code GeneratorModel} emission-filter predicate (the
     * caller-provided pattern since PR #331). The predicate sees each parsed
     * model's namespace; upstream's {@code "model1.*"} glob matches the exact
     * namespace {@code model1} AND dotted children, and the builtin
     * {@code com.rosetta.model} entry admits the metafield/basictype namespace
     * exactly as upstream's yml does.
     */
    static Map<String, String> generateWithNamespaceFilter(
            java.util.function.Predicate<String> namespaceFilter, String... snippets) {
        List<RModel> models = parseSnippetModels(snippets);
        GenerationRun run = HoldOutByteCompareTest.generateAllKindsFromModels(
                models, m -> namespaceFilter.test(m.namespace()));
        if (!run.errorMessages().isEmpty()) {
            Assertions.fail("generation error(s) on filtered port snippet:\n  - "
                    + String.join("\n  - ", run.errorMessages()));
        }
        return run.output();
    }

    // ------------------------------------------------- runtime Guice mirror

    /**
     * Upstream's model-test Guice environment, replayed with REAL Guice running
     * INSIDE the isolated loader (PR #423, leg-C slice 2). Upstream
     * ({@code QualifyTestHelper} / {@code ModelMetaGeneratorTest}) does:
     * <pre>
     *   Guice.createInjector(new AbstractModule() {
     *       configure() {
     *           bind(ConditionValidator).toInstance(new DefaultConditionValidator());
     *           bind(ModelObjectValidator).toInstance(new NoOpModelObjectValidator());
     *       }
     *   })
     * </pre>
     * and pulls {@code QualifyFunctionFactory.Default} / {@code ValidatorFactory.Default}
     * from it — both of which carry an {@code @Inject Injector} field (they resolve
     * generated qualify-function / validator classes per call), so the reflective
     * {@code MiniInjector} cannot stand in here: a REAL {@code Injector} instance is
     * required. Guice itself is ON the gate classpath (the {@code @ImplementedBy}-
     * carrying corpus goldens compile against it), so the injector is created
     * reflectively from the isolated loader's Guice, with the module supplied as a
     * {@link Proxy} implementing that loader's {@code Module} interface. Everything
     * — bindings, factory, generated classes — stays inside the isolated loader;
     * results cross back only as JDK types or reflective handles (the loader law).
     */
    static Object guiceInjector(ClassLoader loader) {
        try {
            Class<?> moduleCls = loader.loadClass("com.google.inject.Module");
            Object module = Proxy.newProxyInstance(loader, new Class<?>[] { moduleCls },
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "configure":
                                Object binder = args[0];
                                bindToNewInstance(loader, binder,
                                        "com.rosetta.model.lib.functions.ConditionValidator",
                                        "com.rosetta.model.lib.functions.DefaultConditionValidator");
                                bindToNewInstance(loader, binder,
                                        "com.rosetta.model.lib.functions.ModelObjectValidator",
                                        "com.rosetta.model.lib.functions.NoOpModelObjectValidator");
                                return null;
                            case "equals":
                                return proxy == args[0];
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            case "toString":
                                return "UpstreamPortHarness.validatorBindingsModule";
                            default:
                                throw new AssertionError(
                                        "unexpected Module call: " + method.getName());
                        }
                    });
            Object moduleArray = java.lang.reflect.Array.newInstance(moduleCls, 1);
            java.lang.reflect.Array.set(moduleArray, 0, module);
            Class<?> guice = loader.loadClass("com.google.inject.Guice");
            return guice.getMethod("createInjector", moduleArray.getClass())
                    .invoke(null, moduleArray);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Guice injector creation failed in the isolated loader", e);
        }
    }

    /**
     * {@code binder.bind(iface).toInstance(new impl())} — reflective, loader-local.
     * Methods are resolved on the PUBLIC Guice interfaces ({@code Binder},
     * {@code LinkedBindingBuilder}), never on the returned implementation classes.
     */
    private static void bindToNewInstance(ClassLoader loader, Object binder,
            String ifaceName, String implName) throws ReflectiveOperationException {
        Class<?> iface = loader.loadClass(ifaceName);
        Object instance = loader.loadClass(implName).getDeclaredConstructor().newInstance();
        Object bindingBuilder = loader.loadClass("com.google.inject.Binder")
                .getMethod("bind", Class.class).invoke(binder, iface);
        loader.loadClass("com.google.inject.binder.LinkedBindingBuilder")
                .getMethod("toInstance", Object.class).invoke(bindingBuilder, instance);
    }

    /** Reflective {@code injector.getInstance(cls)} (the injector lives in the isolated loader). */
    static Object getInstance(Object injector, Class<?> cls) {
        try {
            Method m = cls.getClassLoader().loadClass("com.google.inject.Injector")
                    .getMethod("getInstance", Class.class);
            return m.invoke(injector, cls);
        } catch (InvocationTargetException e) {
            throw asUnchecked(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("injector.getInstance failed for " + cls, e);
        }
    }

    /** Load a class from the same isolated loader as the generated {@code classes}. */
    static Class<?> loadRuntimeClass(Map<String, Class<?>> classes, String name) {
        ClassLoader loader = classes.values().iterator().next().getClassLoader();
        try {
            return loader.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("runtime class absent from the gate classpath: " + name, e);
        }
    }

    // ------------------------------------------------------- qualify mirror

    /**
     * Upstream {@code QualifyTestHelper.createUtilAndGetAllResults}:
     * {@code new QualifyResultsExtractor(RosettaMetaDataBuilder.getMetaData(model)
     * .getQualifyFunctions(funcFactory), model).getAllResults()} — fully
     * reflective, with the factory pulled from {@link #guiceInjector} exactly as
     * upstream pulls {@code QualifyFunctionFactory.Default} from its test injector.
     * Returns the {@code List<QualifyResult>} as an untyped {@code List<Object>}.
     */
    static List<Object> qualifyAllResults(Map<String, Class<?>> classes, Object modelInstance) {
        ClassLoader loader = modelInstance.getClass().getClassLoader();
        Object injector = guiceInjector(loader);
        Object funcFactory = getInstance(injector,
                loadRuntimeClass(classes, "com.rosetta.model.lib.qualify.QualifyFunctionFactory$Default"));
        try {
            Class<?> metaDataBuilder = loader.loadClass("com.rosetta.model.lib.meta.RosettaMetaDataBuilder");
            Class<?> rmo = loader.loadClass("com.rosetta.model.lib.RosettaModelObject");
            Object metaData = metaDataBuilder.getMethod("getMetaData", rmo)
                    .invoke(null, modelInstance);
            Class<?> funcFactoryIface = loader.loadClass("com.rosetta.model.lib.qualify.QualifyFunctionFactory");
            Object qualifyFunctions = metaData.getClass()
                    .getMethod("getQualifyFunctions", funcFactoryIface)
                    .invoke(metaData, funcFactory);
            Class<?> extractor = loader.loadClass("com.rosetta.model.lib.qualify.QualifyResultsExtractor");
            Object util = extractor.getConstructor(List.class, rmo)
                    .newInstance(qualifyFunctions, modelInstance);
            Object results = extractor.getMethod("getAllResults").invoke(util);
            List<Object> out = new ArrayList<>();
            for (Object r : (List<?>) results) {
                out.add(r);
            }
            return out;
        } catch (InvocationTargetException e) {
            throw asUnchecked(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("qualify runtime stack failed reflectively", e);
        }
    }

    /**
     * Upstream {@code QualifyTestHelper.getQualifyResult}: the single result whose
     * {@code getName()} matches, asserted unique.
     */
    static Object qualifyResult(List<Object> results, String name) {
        List<Object> matching = new ArrayList<>();
        for (Object r : results) {
            if (name.equals(call(r, "getName"))) {
                matching.add(r);
            }
        }
        Assertions.assertEquals(1, matching.size(),
                "Expected single isEvent function with name " + name + " (got " + matching + ")");
        return matching.get(0);
    }

    /**
     * Instantiate a generated {@code <Type>Meta} class (upstream's
     * {@code classes.get(rootPackage.child("meta") + '.FooMeta').declaredConstructor
     * .newInstance()}).
     */
    static Object metaInstance(Map<String, Class<?>> classes, String typeName) {
        Class<?> cls = classes.get(ROOT_PACKAGE + ".meta." + typeName + "Meta");
        Assertions.assertNotNull(cls, "generated meta class missing: "
                + ROOT_PACKAGE + ".meta." + typeName + "Meta (generated: " + classes.keySet() + ")");
        try {
            return cls.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("meta class not instantiable: " + cls, e);
        }
    }

    /**
     * {@code meta.validator(factory)} / {@code meta.typeFormatValidator(factory)}
     * with the factory = {@code ValidatorFactory.Default} from {@link #guiceInjector}
     * (upstream injects {@code ValidatorFactory} from its language injector; the
     * {@code Default} carries the same {@code @Inject Injector} pattern as the
     * qualify factory).
     */
    static Object metaValidator(Map<String, Class<?>> classes, Object metaInstance, String method) {
        ClassLoader loader = metaInstance.getClass().getClassLoader();
        Object injector = guiceInjector(loader);
        Object factory = getInstance(injector,
                loadRuntimeClass(classes, "com.rosetta.model.lib.validation.ValidatorFactory$Default"));
        try {
            Class<?> factoryIface = loader.loadClass("com.rosetta.model.lib.validation.ValidatorFactory");
            Method m = metaInstance.getClass().getMethod(method, factoryIface);
            m.setAccessible(true);
            return m.invoke(metaInstance, factory);
        } catch (InvocationTargetException e) {
            throw asUnchecked(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(method + "(factory) failed on " + metaInstance.getClass(), e);
        }
    }

    /**
     * {@code validator.getValidationResults(null, instance)} — the upstream call
     * shape (a null {@code RosettaPath}); returns the results as a {@code List}.
     */
    static List<Object> validationResults(Object validator, Object instance) {
        try {
            Class<?> pathCls = validator.getClass().getClassLoader()
                    .loadClass("com.rosetta.model.lib.path.RosettaPath");
            Method m = null;
            for (Method cand : validator.getClass().getMethods()) {
                if (cand.getName().equals("getValidationResults") && cand.getParameterCount() == 2
                        && cand.getParameterTypes()[0].equals(pathCls)) {
                    m = cand;
                    break;
                }
            }
            Assertions.assertNotNull(m, "no getValidationResults(RosettaPath, T) on " + validator.getClass());
            m.setAccessible(true);
            Object results = m.invoke(validator, null, instance);
            List<Object> out = new ArrayList<>();
            for (Object r : (List<?>) results) {
                out.add(r);
            }
            return out;
        } catch (InvocationTargetException e) {
            throw asUnchecked(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("getValidationResults failed on " + validator.getClass(), e);
        }
    }

    // ------------------------------------------------------ processor mirror

    /**
     * Upstream {@code RosettaAttributePathProcessor} mirrored as a {@link Proxy}
     * over the ISOLATED loader's {@code Processor} interface (the test-classpath
     * interface is a different {@code Class} object, so a directly-implemented
     * processor could never cross the loader boundary — the ledger's designed
     * {@code java.lang.reflect.Proxy} seat, PR #423). Every
     * {@code processRosetta}/{@code processBasic} call records
     * {@code path.toString()} (= upstream {@code RosettaPath.buildPath()}, the
     * form the upstream test joins) into {@code sink}; boolean methods return
     * {@code true} (descend), {@code report()} returns null — byte-matching the
     * upstream processor's behaviour.
     */
    static Object pathCollectingProcessor(ClassLoader loader, List<String> sink) {
        try {
            Class<?> processorCls = loader.loadClass("com.rosetta.model.lib.process.Processor");
            return Proxy.newProxyInstance(loader, new Class<?>[] { processorCls },
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "processRosetta":
                                sink.add(String.valueOf(args[0]));
                                return true;
                            case "processBasic":
                                sink.add(String.valueOf(args[0]));
                                return null;
                            case "report":
                                return null;
                            case "equals":
                                return proxy == args[0];
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            case "toString":
                                return "UpstreamPortHarness.pathCollectingProcessor";
                            default:
                                throw new AssertionError(
                                        "unexpected Processor call: " + method.getName());
                        }
                    });
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Processor absent from the gate classpath", e);
        }
    }

    /**
     * {@code instance.process(RosettaPath.valueOf(root), processor)} — reflective
     * (both argument types live in the isolated loader).
     */
    static void process(Object instance, String rootPath, Object processor) {
        try {
            ClassLoader loader = instance.getClass().getClassLoader();
            Class<?> pathCls = loader.loadClass("com.rosetta.model.lib.path.RosettaPath");
            Class<?> processorCls = loader.loadClass("com.rosetta.model.lib.process.Processor");
            Object path = pathCls.getMethod("valueOf", String.class).invoke(null, rootPath);
            Method m = instance.getClass().getMethod("process", pathCls, processorCls);
            m.setAccessible(true);
            m.invoke(instance, path, processor);
        } catch (InvocationTargetException e) {
            throw asUnchecked(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("process(path, processor) failed on " + instance.getClass(), e);
        }
    }
}
