package com.regnosys.rosetta.symbols;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AST schema lock test (D5/E12). Generates the current M2 AST shape and
 * compares against a checked-in snapshot. Any drift fails loudly with a
 * diff so the reviewer must consciously approve schema changes.
 *
 * <p>To update the snapshot deliberately (e.g. when M3 adds resolved
 * field methods to M2 classes per D2), run:
 * <pre>mvn -Dtest=SchemaLockTest#update_snapshot -Dupdate.snapshots=true test</pre>
 */
class SchemaLockTest {

    /**
     * Resolves {@code src/test/resources/symbols/schema-lock.txt} relative to
     * the module's source root, derived from the location of this class's
     * own classfile (under {@code target/test-classes/}). Independent of JVM
     * working directory; correct under Surefire {@code forkCount=0}
     * (in-process tests) and when {@code mvn -f rune-parser/pom.xml test} is
     * invoked from the repo root, where {@code Paths.get(...)} would
     * otherwise resolve to a non-existent path.
     */
    private static final Path SNAPSHOT_PATH = resolveSnapshotPath();

    private static Path resolveSnapshotPath() {
        try {
            Path testClassesRoot = Paths.get(
                    SchemaLockTest.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI());
            // testClassesRoot = .../rune-parser/target/test-classes
            // module root     = .../rune-parser
            Path moduleRoot = testClassesRoot.getParent().getParent();
            return moduleRoot.resolve("src/test/resources/symbols/schema-lock.txt");
        } catch (java.net.URISyntaxException e) {
            throw new IllegalStateException("failed to resolve test-classes root", e);
        }
    }

    @Test
    void m2_ast_schema_matches_snapshot() throws Exception {
        if (!Files.exists(SNAPSHOT_PATH)) {
            fail("Snapshot missing — run: mvn -Dtest=SchemaLockTest#update_snapshot -Dupdate.snapshots=true test");
        }
        String current = SchemaLockGenerator.generate();
        String expected = Files.readString(SNAPSHOT_PATH);

        if (!current.equals(expected)) {
            Path actualPath = SNAPSHOT_PATH.resolveSibling("schema-lock.actual.txt");
            Files.writeString(actualPath, current);
            fail("M2 AST schema has drifted from the lock snapshot.\n"
                + "Compare:\n"
                + "  expected: " + SNAPSHOT_PATH + "\n"
                + "  actual:   " + actualPath + "\n"
                + "If the change is deliberate (e.g. M3 added a resolved-field method),\n"
                + "regenerate with: mvn -Dtest=SchemaLockTest#update_snapshot -Dupdate.snapshots=true test");
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "update.snapshots", matches = "true")
    void update_snapshot() throws Exception {
        String generated = SchemaLockGenerator.generate();
        Files.createDirectories(SNAPSHOT_PATH.getParent());
        Files.writeString(SNAPSHOT_PATH, generated);
        System.out.println("Snapshot updated: " + SNAPSHOT_PATH + " (" + generated.length() + " chars)");
    }
}
