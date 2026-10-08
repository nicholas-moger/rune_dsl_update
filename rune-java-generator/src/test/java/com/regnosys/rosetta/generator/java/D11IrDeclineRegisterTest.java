package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The corpus-free proof that the v3.3 IR-share gate's register loader and its two verdicts are NOT vacuous
 * (v3.3 seat 1, PR #637; the house convention: a gate nobody has seen fail is a gate nobody should trust).
 * Every refusal names {@code name:line}; both verdict directions fire on synthetic breakdowns; the map
 * comparison is order-insensitive; an absent resource is an error, never an empty map; the committed resource itself
 * loads and its rows partition into vendored burn-down, KEPT-BY-DESIGN and chaos exactly as its own header counts them.
 */
class D11IrDeclineRegisterTest {

    private static Map<String, Map<String, Integer>> parse(String text) throws IOException {
        return IrDeclineRegister.parse(new BufferedReader(new StringReader(text)), "test");
    }

    private static AssertionError refused(String text) {
        AssertionError e = assertThrows(AssertionError.class, () -> parse(text));
        assertTrue(e.getMessage().startsWith("test:"), "the refusal names name:line - " + e.getMessage());
        return e;
    }

    private static Map<String, Integer> m(Object... kv) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            out.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return out;
    }

    @Test
    void acceptsCommentsBlanksWhitespaceAndEveryAxis() throws IOException {
        Map<String, Map<String, Integer>> r = parse("# a comment\n\n  drr/7.0.0   FUNCTION site   adapterGap   140  \n"
                + "drr/7.0.0 FUNCTION site ruleDelegation 3\n"
                + "drr/7.0.0 FUNCTION family RSymbolReference 43\n"
                + "drr/7.0.0 FUNCTION family SomeFutureFamily 1\n"
                + "chaos/1.1.0 FUNCTION untargeted RReduceExpr 13\n");
        assertEquals(m("adapterGap", 140, "ruleDelegation", 3), r.get("drr/7.0.0 FUNCTION site"));
        assertEquals(m("RSymbolReference", 43, "SomeFutureFamily", 1), r.get("drr/7.0.0 FUNCTION family"), "the family token set is OPEN");
        assertEquals(m("RReduceExpr", 13), r.get("chaos/1.1.0 FUNCTION untargeted"));
        assertEquals(3, r.size());
        assertThrows(UnsupportedOperationException.class, () -> r.get("drr/7.0.0 FUNCTION site").put("x", 1), "frozen");
    }

    @Test
    void acceptsAllFiveSiteTokens() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String site : IrDeclineRegister.SITES) {
            sb.append("cdm/6.20.6 RULE site ").append(site).append(" 1\n");
        }
        assertEquals(5, parse(sb.toString()).get("cdm/6.20.6 RULE site").size());
    }

    @Test
    void refusesFourAndSixFields() {
        assertTrue(refused("drr/7.0.0 FUNCTION site adapterGap\n").getMessage().contains("five fields"));
        assertTrue(refused("drr/7.0.0 FUNCTION site adapterGap 140 extra\n").getMessage().contains("got 6"));
    }

    @Test
    void refusesAMalformedCell() {
        assertTrue(refused("/1.1.0 FUNCTION site adapterGap 1\n").getMessage().contains("<corpus>/<version>"));
        assertTrue(refused("cdm/ FUNCTION site adapterGap 1\n").getMessage().contains("<corpus>/<version>"));
        assertTrue(refused("cdm FUNCTION site adapterGap 1\n").getMessage().contains("<corpus>/<version>"));
    }

    @Test
    void refusesAnElementKindAsTheSeam() throws java.io.IOException {
        // the lock against "fixing" the seam validation into an ElementKind one: POJO is a real ElementKind and NOT a seam.
        // DATA_RULE is BOTH since v3.3 seat 3 (D54) - the seam measured inside datarule_comparison AND the file kind of the
        // same name - so the loader ACCEPTS it as the seam; the ElementKind-vs-seam distinction stands, carried by POJO.
        AssertionError e = refused("cdm/6.20.6 POJO site adapterGap 1\n");
        assertTrue(e.getMessage().contains("NOT an ElementKind"), e.getMessage());
        assertTrue(e.getMessage().contains("seam"), e.getMessage());
        assertEquals(m("adapterGap", 1), parse("cdm/6.20.6 DATA_RULE site adapterGap 1\n").get("cdm/6.20.6 DATA_RULE site"),
                "DATA_RULE is the third seam (v3.3 seat 3)");
    }

    @Test
    void refusesAnUnknownAxisAndAnUnknownSiteToken() {
        assertTrue(refused("cdm/6.20.6 FUNCTION sites adapterGap 1\n").getMessage().contains("axis"));
        AssertionError e = refused("cdm/6.20.6 FUNCTION site adapterGapp 1\n");
        assertTrue(e.getMessage().contains("recordDecline"), e.getMessage());
    }

    @Test
    void refusesANonPositiveOrNonIntegerCount() {
        assertTrue(refused("cdm/6.20.6 FUNCTION site adapterGap -1\n").getMessage().contains("decimal digits"));
        assertTrue(refused("cdm/6.20.6 FUNCTION site adapterGap +1\n").getMessage().contains("decimal digits"));
        assertTrue(refused("cdm/6.20.6 FUNCTION site adapterGap 0\n").getMessage().contains("ABSENCE of a row"));
        assertTrue(refused("cdm/6.20.6 FUNCTION site adapterGap x\n").getMessage().contains("decimal digits"));
    }

    @Test
    void refusesADuplicateRow() {
        AssertionError e = refused("cdm/6.20.6 FUNCTION site adapterGap 1\ncdm/6.20.6 FUNCTION site adapterGap 2\n");
        assertTrue(e.getMessage().startsWith("test:2 - duplicate row"), e.getMessage());
    }

    @Test
    void theBreakdownParserAcceptsNoneAndAnyOrderAndRefusesAMalformedToken() {
        assertTrue(IrDeclineRegister.parseBreakdown("none").isEmpty());
        assertTrue(IrDeclineRegister.parseBreakdown("").isEmpty());
        assertEquals(m("adapterGap", 140, "ruleDelegation", 3), IrDeclineRegister.parseBreakdown("ruleDelegation=3 adapterGap=140"));
        assertThrows(AssertionError.class, () -> IrDeclineRegister.parseBreakdown("adapterGap"));
        assertThrows(AssertionError.class, () -> IrDeclineRegister.parseBreakdown("adapterGap=x"));
        assertThrows(AssertionError.class, () -> IrDeclineRegister.parseBreakdown("adapterGap=1 adapterGap=2"));
    }

    @Test
    void theVerdictsFireInBothDirectionsAndPassOnEqualityInAnyOrder() {
        String c = "drr/7.0.0", s = "FUNCTION", a = "site";
        // declared above measured -> HEALED; declared present, measured none -> HEALED
        String healed = IrDeclineRegister.verdict(c, s, a, m("adapterGap", 141), IrDeclineRegister.parseBreakdown("adapterGap=140"));
        assertNotNull(healed);
        assertTrue(healed.startsWith("D11 IR-share gate: HEALED"), healed);
        assertTrue(healed.contains("declared adapterGap=141, measured 140"), healed);
        String healedGone = IrDeclineRegister.verdict(c, s, a, m("adapterGap", 1), IrDeclineRegister.parseBreakdown("none"));
        assertTrue(healedGone.startsWith("D11 IR-share gate: HEALED") && healedGone.contains("measured absent"), healedGone);
        // measured above declared -> NEW DECLINE; measured present, declared empty -> NEW DECLINE
        String regression = IrDeclineRegister.verdict(c, s, a, m("adapterGap", 140), IrDeclineRegister.parseBreakdown("adapterGap=141"));
        assertTrue(regression.startsWith("D11 IR-share gate: NEW DECLINE") && regression.contains("measured adapterGap=141, declared 140"), regression);
        String undeclared = IrDeclineRegister.verdict(c, s, a, Map.of(), IrDeclineRegister.parseBreakdown("adapterGap=1"));
        assertTrue(undeclared.startsWith("D11 IR-share gate: NEW DECLINE") && undeclared.contains("declared absent"), undeclared);
        // a new decline wins the headline when a heal sits beside it on the same axis, and both are named
        String mixed = IrDeclineRegister.verdict(c, s, a, m("adapterGap", 140, "leafEmitter", 2), IrDeclineRegister.parseBreakdown("adapterGap=141"));
        assertTrue(mixed.startsWith("D11 IR-share gate: NEW DECLINE") && mixed.contains("Also healed") && mixed.contains("leafEmitter=2"), mixed);
        // equality passes, and passes with the tokens in the opposite order (rankedBreakdown reorders on a count change)
        assertNull(IrDeclineRegister.verdict(c, s, a, m("adapterGap", 140, "ruleDelegation", 3), IrDeclineRegister.parseBreakdown("adapterGap=140 ruleDelegation=3")));
        assertNull(IrDeclineRegister.verdict(c, s, a, m("adapterGap", 140, "ruleDelegation", 3), IrDeclineRegister.parseBreakdown("ruleDelegation=3 adapterGap=140")));
        assertNull(IrDeclineRegister.verdict(c, s, a, Map.of(), IrDeclineRegister.parseBreakdown("none")), "no row == the empty breakdown");
        assertNull(IrDeclineRegister.verdict(c, s, a, Map.of(), IrDeclineRegister.parseBreakdown("adapterGap=0")), "a zero-valued token is the absence of a token");
    }

    @Test
    void theCountParserRefusesALeadingZeroAnEmptyTokenAndAnOverflow() {
        // the row grammar says "the measured count, exact": 0140 is a second spelling of 140 (PR #637 round 1, cq NIT-3)
        assertTrue(refused("cdm/6.20.6 FUNCTION site adapterGap 0140\n").getMessage().contains("leading zero"));
        assertTrue(refused("cdm/6.20.6 FUNCTION site adapterGap 00\n").getMessage().contains("leading zero"));
        // a single zero PARSES (the leading-zero guard's `text.length() > 1` leg) and is then refused as a zero ROW,
        // not as a leading zero. The first cut asserted `"0".compareTo("0") == 0`, a control that cannot fail and
        // reads nothing from the register (PR #638 round 1, spec SF-5 = cq NIT-1); the zero-ROW half is already
        // pinned by refusesANonPositiveOrNonIntegerCount, so this case keeps only the half that is its own.
        assertEquals(0, IrDeclineRegister.digitsInt("0", "w"));
        assertTrue(IrDeclineRegister.parseBreakdown("adapterGap=140").containsKey("adapterGap"), "an exact count parses");
        // the overflow branch, reached the only way a caller can reach it (PR #637 round 2, cq SF-3)
        AssertionError over = assertThrows(AssertionError.class, () -> IrDeclineRegister.digitsInt("99999999999", "w"));
        assertTrue(over.getMessage().contains("overflows an int"), over.getMessage());
        // the empty branch is unreachable from parse (five non-empty fields) and parseBreakdown (a token ending in '='
        // is refused first); it is reached HERE by a direct call, and its message is the right cause - not an overflow
        AssertionError empty = assertThrows(AssertionError.class, () -> IrDeclineRegister.digitsInt("", "w"));
        assertTrue(empty.getMessage().contains("the count is empty"), empty.getMessage());
    }

    @Test
    void theConservationArbiterFiresOnEachRelationAndPassesWhenAllThreeHold() {
        // the recordDecline single-seat law: one decline is recorded at exactly one site and attributed to exactly one
        // family, so both breakdowns sum to irDeclined; the untargeted breakdown sums to its own printed total
        assertNull(IrDeclineRegister.conservation("drr/7.0.0", "FUNCTION", 143,
                m("adapterGap", 140, "ruleDelegation", 3), m("RSymbolReference", 100, "RFeatureCall", 43), 0, m()),
                "all three conservations hold");
        assertNull(IrDeclineRegister.conservation("cdm/5.38.0", "FUNCTION", 0, m(), m(), 13, m("RReduceExpr", 13)));
        String site = IrDeclineRegister.conservation("drr/7.0.0", "FUNCTION", 143,
                m("adapterGap", 140), m("RSymbolReference", 143), 0, m());
        assertNotNull(site);
        assertTrue(site.startsWith("D11 IR-share gate: the print does not CONSERVE"), site);
        assertTrue(site.contains("declined by site: the breakdown sums to 140 but the print declares 143"), site);
        assertTrue(!site.contains("declined by family:"), "only the relation that failed is named - " + site);
        String family = IrDeclineRegister.conservation("drr/7.0.0", "RULE", 402,
                m("adapterGap", 391, "ruleDelegation", 11), m("RFeatureCall", 401), 0, m());
        assertTrue(family.contains("declined by family: the breakdown sums to 401 but the print declares 402"), family);
        String untargeted = IrDeclineRegister.conservation("chaos/1.1.0", "FUNCTION", 0, m(), m(), 13, m("RReduceExpr", 12));
        assertTrue(untargeted.contains("untargeted visits: the breakdown sums to 12 but the print declares 13"), untargeted);
        // all three at once, in ONE message, each with both sides
        String all = IrDeclineRegister.conservation("drr/7.1.0", "FUNCTION", 5, m("adapterGap", 4), m("RFeatureCall", 3), 2, m());
        for (String relation : new String[] {"declined by site:", "declined by family:", "untargeted visits:"}) {
            assertEquals(1, all.split(java.util.regex.Pattern.quote(relation), -1).length - 1,
                    "each failed relation is named exactly once in the ONE message - " + relation + " in " + all);
        }
    }

    @Test
    void anAbsentResourceIsAnAssertionErrorNeverAnEmptyMap() {
        AssertionError e = assertThrows(AssertionError.class, () -> IrDeclineRegister.load("/no-such-register.txt"));
        assertTrue(e.getMessage().contains("not found") && e.getMessage().contains("never an empty one"), e.getMessage());
    }

    @Test
    void theCommittedRegisterLoadsAndItsRowsPartitionAsItsHeaderSays() throws IOException {
        Map<String, Map<String, Integer>> r = IrDeclineRegister.load();
        assertTrue(r.size() > 0, "the committed register carries rows at this head (v3.3's burn-down is not finished)");
        int rows = 0;
        for (Map.Entry<String, Map<String, Integer>> e : r.entrySet()) {
            String[] k = e.getKey().split(" ");
            assertEquals(3, k.length, e.getKey());
            assertTrue(IrDeclineRegister.SEAMS.contains(k[1]) && IrDeclineRegister.AXES.contains(k[2]), e.getKey());
            rows += e.getValue().size();
            for (int c : e.getValue().values()) {
                assertTrue(c > 0);
            }
        }
        assertTrue(rows > 0);
        // the declared keys are "<cell> <SEAM>" - the wholeness check's population
        for (String k : IrDeclineRegister.declaredKeys(r)) {
            assertEquals(2, k.split(" ").length, k);
        }
        // the partition the file's own header states (generated FROM THE PRINT, never typed here): every row is a chaos row
        // (the chaos/ cell), a KEPT-BY-DESIGN row (a vendored site row whose token is ruleDelegation) or a vendored burn-down row
        int chaos = 0, kept = 0, vendored = 0;
        for (Map.Entry<String, Map<String, Integer>> e : r.entrySet()) {
            String[] k = e.getKey().split(" ");
            for (String token : e.getValue().keySet()) {
                if (k[0].startsWith("chaos/")) {
                    chaos++;
                } else if ("site".equals(k[2]) && "ruleDelegation".equals(token)) {
                    kept++;
                } else {
                    vendored++;
                }
            }
        }
        String header = null;
        java.io.InputStream resource = IrDeclineRegister.class.getResourceAsStream(IrDeclineRegister.RESOURCE);
        // unreachable today - load() above throws on an absent resource - but the reader is built from it, so it is
        // guarded with the loader's own message rather than a NullPointerException (PR #637 round 2, spec NIT-4)
        assertNotNull(resource, "D11 IR-share decline register not found: " + IrDeclineRegister.RESOURCE
                + " (an absent register is never an empty one)");
        try (java.io.InputStream in = resource;
             BufferedReader br = new BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("# ROW COUNT: ")) {
                    header = line;
                }
            }
        }
        assertNotNull(header, "the register's header carries its ROW COUNT line");
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("# ROW COUNT: (\\d+) = (\\d+) vendored burn-down \\+ (\\d+) KEPT-BY-DESIGN \\+ (\\d+) chaos").matcher(header);
        assertTrue(m.find(), header);
        assertEquals(Integer.parseInt(m.group(2)), vendored, "vendored burn-down rows vs the header");
        assertEquals(Integer.parseInt(m.group(3)), kept, "KEPT-BY-DESIGN rows vs the header");
        assertEquals(Integer.parseInt(m.group(4)), chaos, "chaos rows vs the header");
        assertEquals(Integer.parseInt(m.group(1)), rows, "the row total vs the header");
        assertEquals(rows, vendored + kept + chaos, "the partition is exhaustive");
    }
}
