package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The corpus-free proof that the IR fallback register's loader and its arbiter are NOT vacuous (v3.3 seat 5, PR #641 -
 * decision D55, ruling R4; the house convention: a gate nobody has seen fail is a gate nobody should trust). Every
 * refusal names {@code name:line}; every verdict direction fires on a synthetic measurement - a NEW FALLBACK, a HEALED
 * row, a file the new emitter writes that is still listed, a broken print - and agreement reads {@code null}; an absent
 * resource is an error, never an empty register; the committed resource loads, and what it declares today is stated.
 *
 * <p>v3.3 seat 6 (PR #642, round 1 cq NIT-3) adds the cross-pass half of "a file has ONE writer" -
 * {@link #refusesOneFileListedUnderTwoSubKindsOfOneCell}.
 */
class D11IrFallbackRegisterTest {

    private static Map<String, Set<String>> parse(String text) throws IOException {
        return IrFallbackRegister.parse(new BufferedReader(new StringReader(text)), "test");
    }

    private static AssertionError refused(String text) {
        AssertionError e = assertThrows(AssertionError.class, () -> parse(text));
        assertTrue(e.getMessage().startsWith("test:"), "the refusal names name:line - " + e.getMessage());
        return e;
    }

    @Test
    void acceptsCommentsBlanksWhitespaceAndEverySubKind() throws IOException {
        Map<String, Set<String>> r = parse("# a comment\n\n  cdm/6.20.6   ENUM   cdm/base/AEnum.java  \n"
                + "cdm/6.20.6 ENUM cdm/base/BEnum.java\n"
                + "cdm/6.20.6 CHOICE cdm/base/Pick.java\n"
                + "chaos/1.1.0 DATA_TYPE chaos/a/T.java\n");
        assertEquals(Set.of("cdm/base/AEnum.java", "cdm/base/BEnum.java"), r.get("cdm/6.20.6 ENUM"));
        assertEquals(Set.of("cdm/base/Pick.java"), r.get("cdm/6.20.6 CHOICE"));
        assertEquals(3, r.size());
        assertThrows(UnsupportedOperationException.class, () -> r.get("cdm/6.20.6 ENUM").add("x.java"), "frozen");
        assertEquals(2, IrFallbackRegister.rowsOf(r, "ENUM"));
        assertEquals(0, IrFallbackRegister.rowsOf(parse("# empty\n"), "ENUM"), "an EMPTY register is a legal register - the meter's goal");
    }

    @Test
    void refusesAMalformedRow() {
        assertTrue(refused("cdm/6.20.6 ENUM\n").getMessage().contains("three fields"));
        assertTrue(refused("cdm/6.20.6 ENUM a/A.java extra\n").getMessage().contains("got 4"));
        assertTrue(refused("/6.20.6 ENUM a/A.java\n").getMessage().contains("<corpus>/<version>"));
        assertTrue(refused("cdm/6.20.6 POJO a/A.java\n").getMessage().contains("sub-kind"));
        assertTrue(refused("cdm/6.20.6 ENUM a/A.txt\n").getMessage().contains(".java path"));
        assertTrue(refused("cdm/6.20.6 ENUM /a/A.java\n").getMessage().contains(".java path"));
    }

    @Test
    void refusesADuplicateAndAnUnsortedRow() {
        assertTrue(refused("cdm/6.20.6 ENUM a/A.java\ncdm/6.20.6 ENUM a/A.java\n").getMessage().contains("strictly ascending"));
        AssertionError e = refused("cdm/6.20.6 ENUM a/B.java\ncdm/6.20.6 ENUM a/A.java\n");
        assertTrue(e.getMessage().startsWith("test:2"), e.getMessage());
    }

    @Test
    void theOrderIsPerKeyNotGlobal() throws IOException {
        assertEquals(2, parse("cdm/6.20.6 ENUM z/Z.java\ncdm/6.20.6 CHOICE a/A.java\n").size());
    }

    /**
     * THE CROSS-PASS HALF OF "A FILE HAS ONE WRITER" (v3.3 seat 6, PR #642 - round 1 cq NIT-3). The ascending law is
     * per key and blind to it: one file of one cell listed under two sub-kinds would be demanded of two different
     * passes, so one of them must read NEW FALLBACK or HEALED for the register's own reason. Refused, naming BOTH keys.
     * Delete the {@code writerOf} check in {@link IrFallbackRegister#parse} and this test is RED - the first two
     * assertions load a register the law must reject, and the third proves the law does not over-reach: the SAME file
     * path under a DIFFERENT cell is two cells' own business and stays legal.
     */
    @Test
    void refusesOneFileListedUnderTwoSubKindsOfOneCell() throws IOException {
        AssertionError e = refused("cdm/6.20.6 CHOICE a/A.java\ncdm/6.20.6 DATA_TYPE a/A.java\n");
        assertTrue(e.getMessage().startsWith("test:2"), e.getMessage());
        assertTrue(e.getMessage().contains("TWO sub-kinds") && e.getMessage().contains("cdm/6.20.6 CHOICE")
                && e.getMessage().contains("cdm/6.20.6 DATA_TYPE") && e.getMessage().contains("a/A.java"), e.getMessage());
        assertTrue(refused("cdm/6.20.6 ENUM a/A.java\ncdm/6.20.6 CHOICE a/A.java\ncdm/6.20.6 DATA_TYPE a/A.java\n")
                .getMessage().startsWith("test:2"), "the SECOND row is where the clash becomes readable");
        assertEquals(2, parse("cdm/6.20.6 ENUM a/A.java\ncdm/6.20.7 CHOICE a/A.java\n").size(),
                "the law is per CELL: the same path under two cells is two cells' own file, and legal");
    }

    // ------------------------------------------------------------------------------------------------ the arbiter

    private static final Set<String> TWO = Set.of("a/A.java", "a/B.java");

    @Test
    void agreementReadsNull() {
        assertNull(IrFallbackRegister.verdict("c/1", "ENUM", TWO, TWO, Set.of()), "today's truth: every file on the old generator, all declared");
        assertNull(IrFallbackRegister.verdict("c/1", "ENUM", Set.of("a/A.java"), TWO, Set.of("a/B.java")), "one file healed AND its row deleted");
        assertNull(IrFallbackRegister.verdict("c/1", "ENUM", Set.of(), TWO, TWO), "the goal: the new emitter writes every file, the register holds none");
        assertNull(IrFallbackRegister.verdict("c/1", "CHOICE", Set.of(), Set.of(), Set.of()), "a cell with no file of the sub-kind");
    }

    @Test
    void anUndeclaredOldGeneratorFileIsANewFallback() {
        String v = IrFallbackRegister.verdict("c/1", "ENUM", Set.of("a/A.java"), TWO, Set.of());
        assertNotNull(v);
        assertTrue(v.contains("NEW FALLBACK - a regression") && v.contains("a/B.java") && v.contains("1 file(s)"), v);
        assertTrue(IrFallbackRegister.verdict("c/1", "DATA_TYPE", Set.of(), TWO, Set.of()).contains("NEW FALLBACK"),
                "an EMPTY register against a route that still falls back is RED - the skeleton cannot pass by declaring nothing");
    }

    @Test
    void aDeclaredFileTheOldGeneratorNoLongerWritesIsHealed() {
        String gone = IrFallbackRegister.verdict("c/1", "ENUM", TWO, Set.of("a/A.java"), Set.of());
        assertTrue(gone.contains("HEALED") && gone.contains("a/B.java") && gone.contains("SHRINK-ONLY"), gone);
        String nativeNow = IrFallbackRegister.verdict("c/1", "ENUM", TWO, TWO, Set.of("a/B.java"));
        assertTrue(nativeNow.contains("HEALED") && nativeNow.contains("written by the NEW IR emitter") && nativeNow.contains("a/B.java"), nativeNow);
    }

    @Test
    void aNewEmitterClaimOutsideTheEmittedSetIsABrokenPrint() {
        String v = IrFallbackRegister.verdict("c/1", "ENUM", Set.of(), TWO, Set.of("a/Z.java"));
        assertTrue(v.contains("BROKEN PRINT") && v.contains("a/Z.java"), v);
    }

    @Test
    void aLongListIsSampled() {
        Set<String> many = new java.util.TreeSet<>();
        for (int i = 0; i < 9; i++) {
            many.add("a/F" + i + ".java");
        }
        assertTrue(IrFallbackRegister.verdict("c/1", "ENUM", Set.of(), many, Set.of()).contains("(+4 more)"));
    }

    // ------------------------------------------------------------------------------------------------ the resource

    @Test
    void anAbsentResourceIsAnErrorNeverAnEmptyRegister() {
        AssertionError e = assertThrows(AssertionError.class, () -> IrFallbackRegister.load("/no-such-register.txt"));
        assertTrue(e.getMessage().contains("an absent register is never an empty one"), e.getMessage());
    }

    /**
     * The committed register loads, every key is a legal (cell, sub-kind), and TODAY'S TRUTH is stated: the ENUM
     * emitter landed at PR #642 (v3.3 seat 6), so the register holds NO ENUM row - the clause that read {@code > 0}
     * until then flipped to {@code assertEquals(0, ...)} in the commit that shrank the register from the ON-route
     * print, and only then does the file meter count the enum sub-kind; the CHOICE emitter landed at PR #646 (v3.3
     * seat 10, THE TYPE UNIT GENERALISED to the choice kind - its clause flipped in the commit AFTER the one that
     * shrank the register: the s10c5 runs of record read this test RED at commit 5, the one red of 5,870, because
     * commit 5 re-cut the rows from run A's dump and left this clause at {@code > 0}); the DATA_TYPE emitter landed
     * at PR #645 and its clause stays {@code > 0} because the adversarial cell's twelve refused holders still hold
     * rows - the register is the chaos residual alone (12 rows: ENUM 0 / CHOICE 0 / DATA_TYPE 12). A clause flips the
     * same way in the PR that empties its sub-kind, and in the SAME commit as the register from now on.
     */
    @Test
    void theCommittedRegisterLoadsAndStatesTodaysTruth() {
        Map<String, Set<String>> register = IrFallbackRegister.load();
        for (String key : register.keySet()) {
            String[] parts = key.split(" ");
            assertEquals(2, parts.length, key);
            assertTrue(IrFallbackRegister.SUB_KINDS.contains(parts[1]), key);
        }
        assertEquals(0, IrFallbackRegister.rowsOf(register, "ENUM"), "the ENUM emitter landed at PR #642: the register holds no ENUM row");
        assertEquals(0, IrFallbackRegister.rowsOf(register, "CHOICE"),
                "the CHOICE emitter landed at PR #646 (v3.3 seat 10): the register holds no CHOICE row - the 237 rows"
                        + " were re-cut to zero from run A's dump at commit 5, every choice of every cell written by"
                        + " the type unit");
        assertTrue(IrFallbackRegister.rowsOf(register, "DATA_TYPE") > 0,
                "the DATA_TYPE emitter landed at PR #645 and its clause stays > 0: the adversarial cell's twelve refused"
                        + " C27 / C99 holders are the register's only rows (vendored DATA_TYPE rows 0)");
    }

    // ------------------------------------------------- the UNIT SHADOW line's divergence sample, ONE PER ARM

    /**
     * v3.3 seat 10, PR #646 commit 3 (PR #645 round 2 NIT-2) - EACH ARM KEEPS ITS OWN FIVE. Three arms of
     * {@code D11CorpusRegressionTest.printUnitShadow} name a key as divergent and they are different defects: a
     * differing compare, a file the IR wrote where the old generator refused, and a NO-FILE the old generator
     * wrote. They shared ONE five-slot list, so five keys of the first arm hid the ONE key of another - from the
     * print and from the {@code differing == 0} message alike, which is the arm a reader would most need.
     *
     * <p>THE MUTANT (lane R1c): the three lists collapsed back into one - {@code renderedWhereLegacyRefused}
     * declared as {@code = differing}. Then a full differing arm swallows the rendered-where-refused sample and
     * this test is RED naming the arm whose sample was lost.
     */
    @Test
    void aFullDifferingArmCannotCrowdTheOtherTwoArmsOutOfTheSample() {
        D11CorpusRegressionTest.UnitShadowDivergenceSamples samples =
                new D11CorpusRegressionTest.UnitShadowDivergenceSamples();
        for (int i = 1; i <= D11CorpusRegressionTest.UnitShadowDivergenceSamples.PER_ARM + 3; i++) {
            samples.differing("differing/" + i + ".java -> line " + i);
        }
        samples.renderedWhereLegacyRefused("rendered/Only.java -> the IR wrote a file the old generator did not");
        samples.noFileWhereLegacyWrote("nofile/Only.java -> the IR answered NO FILE BY LAW");

        assertEquals(D11CorpusRegressionTest.UnitShadowDivergenceSamples.PER_ARM, samples.differing().size(),
                "the differing arm keeps its own five and no more");
        assertEquals(List.of("rendered/Only.java -> the IR wrote a file the old generator did not"),
                samples.renderedWhereLegacyRefused(),
                "the rendered-where-the-old-generator-refused arm lost its sample to the differing arm's five -"
                        + " each arm keeps its OWN five");
        assertEquals(List.of("nofile/Only.java -> the IR answered NO FILE BY LAW"),
                samples.noFileWhereLegacyWrote(),
                "the no-file-where-the-old-generator-wrote arm lost its sample to the differing arm's five -"
                        + " each arm keeps its OWN five");

        String message = samples.message();
        assertTrue(message.contains("5 differing"), message);
        assertTrue(message.contains("1 rendered-where-the-old-generator-refused"), message);
        assertTrue(message.contains("1 no-file-where-the-old-generator-wrote"), message);
    }

    /**
     * v3.3 seat 10, PR #646 commit 3 - PRINT BEFORE JUDGE, AND PRINT EVERY GROUP. A group that vanished when its
     * arm took nothing would read as an arm nobody measured; each of the three prints {@code none} instead. On a
     * green cell - which is every cell of the two scoped runs - that is the whole of what the line adds.
     */
    @Test
    void everyGroupOfTheSampleIsPrintedEvenWhenItsArmTookNothing() {
        D11CorpusRegressionTest.UnitShadowDivergenceSamples empty =
                new D11CorpusRegressionTest.UnitShadowDivergenceSamples();
        assertEquals(List.of(
                        "  UNIT SHADOW[META] divergence [differing]: none",
                        "  UNIT SHADOW[META] divergence [rendered-where-the-old-generator-refused]: none",
                        "  UNIT SHADOW[META] divergence [no-file-where-the-old-generator-wrote]: none"),
                empty.lines("  UNIT SHADOW[META]"),
                "all three groups print, and an empty arm prints `none`");

        D11CorpusRegressionTest.UnitShadowDivergenceSamples one =
                new D11CorpusRegressionTest.UnitShadowDivergenceSamples();
        one.noFileWhereLegacyWrote("nofile/Only.java -> the IR answered NO FILE BY LAW");
        assertEquals(List.of(
                        "  UNIT SHADOW[META] divergence [differing]: none",
                        "  UNIT SHADOW[META] divergence [rendered-where-the-old-generator-refused]: none",
                        "  UNIT SHADOW[META] divergence [no-file-where-the-old-generator-wrote]:",
                        "  UNIT SHADOW[META]   nofile/Only.java -> the IR answered NO FILE BY LAW"),
                one.lines("  UNIT SHADOW[META]"),
                "the group's label comes first and its keys stand under it");
    }
}
