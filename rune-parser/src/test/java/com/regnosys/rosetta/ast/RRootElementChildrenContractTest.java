package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.annotations.RRuneAnnotation;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract test that locks the {@code super.children()} invariant on every concrete
 * {@link RRootElement} subclass.
 *
 * <p>For each subclass we instantiate the class via its no-arg constructor (the
 * contract REQUIRES default construction so {@code AstBuilder} can populate the
 * node — a missing no-arg ctor fails the test, intentionally), inject a synthetic
 * {@link RRuneAnnotation} into the hoisted slot, and assert that the subclass's
 * {@link RNode#children()} surfaces it. This catches the silent-drop failure mode
 * where a subclass overrides {@code children()} without calling
 * {@code super.children()}.
 *
 * <p>P1.4.2 reviewer-driven hardening — landmine for any future H2/H11 expansion
 * that adds a new attach site without remembering to re-thread {@code super.children()}.
 */
class RRootElementChildrenContractTest {

    @TestFactory
    Stream<DynamicTest> everyConcreteRRootElementSurfacesHoistedRuneAnnotations() throws Exception {
        List<Class<? extends RRootElement>> subclasses = findAllConcreteSubclassesOf(RRootElement.class);
        assertTrue(subclasses.size() >= 14,
            "Expected ≥14 concrete RRootElement subclasses on the classpath; got " + subclasses.size()
            + ". Has the AST been refactored?");
        return subclasses.stream().map(cls -> DynamicTest.dynamicTest(
            cls.getSimpleName() + ".children() surfaces hoisted runeAnnotations",
            () -> {
                RRootElement instance;
                try {
                    instance = cls.getDeclaredConstructor().newInstance();
                } catch (NoSuchMethodException e) {
                    // The contract REQUIRES every RRootElement subclass to support default
                    // construction so AstBuilder can populate them. Failing here (rather
                    // than skipping) is intentional — a subclass missing a no-arg ctor is
                    // a real bug, not a test-environment artefact.
                    throw new AssertionError(cls.getName()
                        + " has no no-arg constructor — RRootElement subclasses must "
                        + "support default construction so AstBuilder can populate them.", e);
                }
                RRuneAnnotation marker = new RRuneAnnotation();
                marker.setAnnotationName("__contract_marker__");
                instance.addRuneAnnotation(marker);
                List<? extends RNode> children = instance.children();
                assertNotNull(children, cls.getSimpleName() + ".children() returned null");
                assertTrue(children.contains(marker),
                    cls.getName() + ".children() must surface hoisted runeAnnotations "
                    + "via super.children() — got " + children);
            }
        ));
    }

    /**
     * Discovers every concrete subclass of {@code parent} on the classpath under
     * {@code com.regnosys.rosetta.ast}. Walks the build's classes directory and
     * any JARs containing the package. Sufficient for our test environment;
     * the production classpath layout is stable.
     */
    private static List<Class<? extends RRootElement>> findAllConcreteSubclassesOf(
            Class<? extends RRootElement> parent) throws IOException, URISyntaxException {
        String pkg = "com/regnosys/rosetta/ast";
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        List<Class<? extends RRootElement>> out = new ArrayList<>();
        var roots = cl.getResources(pkg);
        while (roots.hasMoreElements()) {
            URL url = roots.nextElement();
            URI uri = url.toURI();
            if ("file".equals(uri.getScheme())) {
                Path root = Paths.get(uri);
                walkDir(root, root, "com.regnosys.rosetta.ast", parent, out, cl);
            } else if ("jar".equals(uri.getScheme())) {
                // Reuse an existing FileSystem if one is already open for this URI
                // (common when other tests scan the same JAR), otherwise create one
                // and close it ourselves. Avoids FileSystemAlreadyExistsException.
                FileSystem fs;
                boolean owned = false;
                try {
                    fs = FileSystems.getFileSystem(uri);
                } catch (java.nio.file.FileSystemNotFoundException notOpen) {
                    fs = FileSystems.newFileSystem(uri, Map.of());
                    owned = true;
                }
                try {
                    Path root = fs.getPath(pkg);
                    walkDir(root, root, "com.regnosys.rosetta.ast", parent, out, cl);
                } finally {
                    if (owned) {
                        fs.close();
                    }
                }
            }
        }
        return out;
    }

    private static void walkDir(Path root, Path current, String pkgPrefix,
                                Class<? extends RRootElement> parent,
                                List<Class<? extends RRootElement>> out,
                                ClassLoader cl) throws IOException {
        try (var stream = Files.list(current)) {
            for (Path p : stream.toList()) {
                if (Files.isDirectory(p)) {
                    walkDir(root, p, pkgPrefix + "." + p.getFileName().toString(), parent, out, cl);
                } else if (p.getFileName().toString().endsWith(".class")) {
                    String simple = p.getFileName().toString();
                    String fqn = pkgPrefix + "." + simple.substring(0, simple.length() - ".class".length());
                    Class<?> c;
                    try {
                        c = Class.forName(fqn, false, cl);
                    } catch (Throwable t) {
                        continue;
                    }
                    if (parent.isAssignableFrom(c)
                            && !c.equals(parent)
                            && !Modifier.isAbstract(c.getModifiers())) {
                        @SuppressWarnings("unchecked")
                        Class<? extends RRootElement> sub = (Class<? extends RRootElement>) c;
                        out.add(sub);
                    }
                }
            }
        }
    }
}
