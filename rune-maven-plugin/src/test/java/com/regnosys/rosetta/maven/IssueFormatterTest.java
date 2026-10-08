package com.regnosys.rosetta.maven;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.symbols.diagnostics.ValidationDiagnostic;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Locks the issue-line payload byte-for-byte against the banked V0 oracle
 * stream: the first line of
 * {@code target/439-v0-oracle/v0-oracle-cdm6-warnings.txt} is
 *
 * <pre>[WARNING] WARNING:Missing implementation for DayCountFractionEnum: ACT_ACT_AFB, ACT_ACT_ISMA, RBA_BOND_BASIS (file:/C:/work/swap-recon/common-domain-model/rosetta-source/target/classes/cdm/rosetta/base-datetime-daycount-func.rosetta line : 7 column : 6)</pre>
 *
 * where {@code [WARNING] } is the Maven logger prefix and the rest is the
 * payload this formatter must reproduce.
 */
class IssueFormatterTest {

    @Test
    void payloadMatchesTheBankedV0LineBytes() {
        ValidationDiagnostic d = new ValidationDiagnostic(
                Severity.WARNING,
                new SourceRange(
                        "C:/work/swap-recon/common-domain-model/rosetta-source/target/classes/cdm/rosetta/base-datetime-daycount-func.rosetta",
                        7, 6, 7, 6, SourceRange.OFFSETS_UNKNOWN, SourceRange.OFFSETS_UNKNOWN),
                "Missing implementation for DayCountFractionEnum: ACT_ACT_AFB, ACT_ACT_ISMA, RBA_BOND_BASIS",
                ValidationIssueCode.TYPE_ERROR);
        assertEquals(
                "WARNING:Missing implementation for DayCountFractionEnum: ACT_ACT_AFB, ACT_ACT_ISMA, "
                        + "RBA_BOND_BASIS (file:/C:/work/swap-recon/common-domain-model/rosetta-source"
                        + "/target/classes/cdm/rosetta/base-datetime-daycount-func.rosetta "
                        + "line : 7 column : 6)",
                IssueFormatter.format(d));
    }

    @Test
    void backslashPathsNormalizeToTheEmfFileUriForm() {
        assertEquals("file:/F:/x/y/z.rosetta", IssueFormatter.fileUri("F:\\x\\y\\z.rosetta"));
        assertEquals("file:/F:/x/y/z.rosetta", IssueFormatter.fileUri("F:/x/y/z.rosetta"));
        // A POSIX absolute path already starts with '/' — single-slash scheme join.
        assertEquals("file:/home/u/z.rosetta", IssueFormatter.fileUri("/home/u/z.rosetta"));
    }
}
