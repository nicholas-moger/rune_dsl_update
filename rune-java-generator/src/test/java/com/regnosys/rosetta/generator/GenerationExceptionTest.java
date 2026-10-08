package com.regnosys.rosetta.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Locks the {@link GenerationException#getTargetPath() targetPath} carrier
 * contract. The D11 regression gate relies on these exact semantics to tell an
 * already-waivered (known-incomplete) generation failure from a new one: the
 * field defaults to {@code null} (treated as unwaivered → fails the cell), is
 * set once by {@code JavaClassGenerator#generateClasses}, and a later
 * {@code null} must not clobber a recorded path.
 *
 * <p>The attachment itself (JavaClassGenerator setting the path on failure) is
 * integration-locked by {@code D11CorpusRegressionTest}: a dropped attachment
 * yields a {@code null} path, which the rule-family gate treats as unwaivered
 * and throws — turning the regression red rather than silently masking it.
 */
class GenerationExceptionTest {

    @Test
    void targetPath_defaultsToNull() {
        GenerationException e = new GenerationException("boom", "src.rosetta", null);
        assertNull(e.getTargetPath(),
                "targetPath must default to null so an unattributed failure is treated as unwaivered");
    }

    @Test
    void setTargetPath_recordsTheOutputPath() {
        GenerationException e = new GenerationException("boom", "src.rosetta", null);
        e.setTargetPath("drr/foo/bar/reports/BazRule.java");
        assertEquals("drr/foo/bar/reports/BazRule.java", e.getTargetPath());
    }

    @Test
    void setTargetPath_nullDoesNotClobberARecordedPath() {
        GenerationException e = new GenerationException("boom", "src.rosetta", null);
        e.setTargetPath("drr/foo/bar/reports/BazRule.java");
        e.setTargetPath(null);
        assertEquals("drr/foo/bar/reports/BazRule.java", e.getTargetPath(),
                "a null set must not erase an already-recorded target path");
    }

    @Test
    void setTargetPath_nonNullOverwriteIsAllowed() {
        GenerationException e = new GenerationException("boom", "src.rosetta", null);
        e.setTargetPath("first/Path.java");
        e.setTargetPath("second/Path.java");
        assertEquals("second/Path.java", e.getTargetPath(),
                "the authoritative path computed at the capture site may replace an earlier value");
    }
}
