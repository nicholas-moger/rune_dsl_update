package com.regnosys.rosetta.japicmp;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Validates the StAX-based japicmp report consumer against a committed
 * fixture. Runs on every CI build because it depends only on a classpath
 * resource, not on the prior-run output of `mvn verify` (which is in a
 * later Maven phase than `test` and therefore not available during this
 * test's execution on a fresh CI checkout).
 *
 * <p>The fixture at {@code src/test/resources/sample-japicmp-report.xml}
 * mirrors the schema emitted by japicmp-maven-plugin 0.23.1 (three
 * {@code <class>} elements: two binaryCompatible=false, one true). It
 * must stay in sync with the plugin's XML schema; revisit on plugin
 * version bumps (tracked via {@code rune-japicmp/pom.xml}).
 */
class JapicmpReportTest {

    @Test
    void reportParsesFixtureAndCountsClasses() throws Exception {
        URL fixtureUrl = getClass().getResource("/sample-japicmp-report.xml");
        assertNotNull(fixtureUrl, "sample-japicmp-report.xml fixture must be on classpath");
        Path fixture = Paths.get(fixtureUrl.toURI());

        JapicmpReport report = JapicmpReport.parse(fixture);
        assertNotNull(report, "japicmp XML fixture should parse via StAX");
        assertEquals(3, report.totalClassCount(),
            "fixture has three <class> elements");
        assertEquals(2, report.binaryIncompatibleCount(),
            "fixture has two classes with binaryCompatible=false");
    }
}
