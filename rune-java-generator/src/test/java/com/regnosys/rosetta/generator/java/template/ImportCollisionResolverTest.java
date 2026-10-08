package com.regnosys.rosetta.generator.java.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * facet fqnWitness (PR #227) — unit pins for {@link ImportCollisionResolver}, the
 * render-order-aware first-claim-wins import collision resolver. Locks the law that
 * the FIRST reference to a Java simple name keeps the bare name (+ import) and any
 * LATER different-canonical reference renders fully-qualified inline (+ its import is
 * suppressed), with a non-colliding class resolving byte-identically to the bare form.
 */
class ImportCollisionResolverTest {

    private static String ref(String canonical) {
        return ImportCollisionResolver.typeRef(canonical);
    }

    @Test
    void noCollisionResolvesToBareNoSuppress() {
        // a single sentinel, no competing canonical → bare name, nothing suppressed
        String body = ref("cdm.base.datetime.Offset") + ".builder()";
        var r = ImportCollisionResolver.resolve(List.of(body), Map.of());
        assertEquals("Offset.builder()", r.bodies().get(0));
        assertTrue(r.suppressedCanonicals().isEmpty());
    }

    @Test
    void witnessLosesWhenConstructionClaimsFirst() {
        // cdm ctor sentinel (first) wins the bare `Offset`; the later fpml witness FQN-ed
        String body = ref("cdm.base.datetime.Offset") + ".builder().setX("
                + "MapperS.of(in).<" + ref("fpml.consolidated.shared.Offset") + ">map(...))";
        var r = ImportCollisionResolver.resolve(List.of(body), Map.of());
        assertEquals(
                "Offset.builder().setX(MapperS.of(in).<fpml.consolidated.shared.Offset>map(...))",
                r.bodies().get(0));
        assertEquals(java.util.Set.of("fpml.consolidated.shared.Offset"), r.suppressedCanonicals());
    }

    @Test
    void ctorLosesWhenWitnessClaimsFirst() {
        // fpml witness sentinel (first, in a hoisted condition) wins; the later cdm ctor FQN-ed
        String body = "if (exists(MapperS.of(in).<" + ref("fpml.consolidated.shared.ManualExercise")
                + ">map(...))) {} "
                + ref("cdm.product.template.ManualExercise") + ".builder()";
        var r = ImportCollisionResolver.resolve(List.of(body), Map.of());
        assertEquals(
                "if (exists(MapperS.of(in).<ManualExercise>map(...))) {} "
                        + "cdm.product.template.ManualExercise.builder()",
                r.bodies().get(0));
        assertEquals(java.util.Set.of("cdm.product.template.ManualExercise"),
                r.suppressedCanonicals());
    }

    @Test
    void claimsCarryAcrossBodiesInFileOrder() {
        // facet witnessDupImportCrossBody (PR #345): the claim map carries body-to-body —
        // the caller's ordered bodies (operations → aliases) match the generated file's
        // textual method order (assignOutput precedes the alias impls; CONDITION expressions
        // render inside evaluate() textually first but resolve last — an approximation with
        // ZERO corpus carriers, see the resolve javadoc), so a collision SPLIT across these
        // bodies resolves like upstream's whole-file render: the earlier body claims, the later
        // different-canonical sentinel FQNs + its duplicate import is suppressed
        // (MapEquityOptionPayout / MapCommodityAmericanExerciseTerms cdm6 — the pre-#345
        // both-bare form was a duplicate same-simple-name import that never compiled).
        String b0 = ref("a.b.X") + ".builder()";
        String b1 = ".<" + ref("c.d.X") + ">map(...)";
        var r = ImportCollisionResolver.resolve(List.of(b0, b1), Map.of());
        assertEquals("X.builder()", r.bodies().get(0));
        assertEquals(".<c.d.X>map(...)", r.bodies().get(1));
        assertEquals(java.util.Set.of("c.d.X"), r.suppressedCanonicals());
    }

    @Test
    void intraBodyCollisionResolvesByLeftToRightOrder() {
        // both references in ONE body: left-to-right first-claim is the true render order
        String body = ref("a.b.X") + ".builder().setX(MapperS.of(in).<" + ref("c.d.X") + ">map())";
        var r = ImportCollisionResolver.resolve(List.of(body), Map.of());
        assertEquals("X.builder().setX(MapperS.of(in).<c.d.X>map())", r.bodies().get(0));
        assertEquals(java.util.Set.of("c.d.X"), r.suppressedCanonicals());
    }

    @Test
    void canonicalThatWinsViaSeedKeepsItsImportAcrossBodies() {
        // facet witnessDupImportCrossBody (PR #345): under the carried claim map a
        // different-canonical loser stays FQN in EVERY later body (the pre-#345 per-body
        // reset let it win a later body bare — the split-collision dup-import bug). The
        // winner-keeps-import law survives through the SEED claims: a seeded canonical's
        // own sentinel renders bare (same canonical) → everBare → NOT suppressed, while
        // the different-canonical sibling is suppressed.
        String b0 = ".<" + ref("c.d.X") + ">map()";           // seeded winner → bare
        String b1 = ref("a.b.X") + ".builder()";              // different canonical → FQN
        var r = ImportCollisionResolver.resolve(List.of(b0, b1), Map.of("X", "c.d.X"));
        assertEquals(".<X>map()", r.bodies().get(0));
        assertEquals("a.b.X.builder()", r.bodies().get(1));
        assertEquals(java.util.Set.of("a.b.X"), r.suppressedCanonicals());
    }

    @Test
    void sameCanonicalTwiceStaysBareNoSuppress() {
        String body = ref("a.b.X") + ".builder().setX(" + ref("a.b.X") + ".builder())";
        var r = ImportCollisionResolver.resolve(List.of(body), Map.of());
        assertEquals("X.builder().setX(X.builder())", r.bodies().get(0));
        assertTrue(r.suppressedCanonicals().isEmpty());
    }

    @Test
    void seedClaimWinsOverBodySentinel() {
        // the signature/output type `Foo` (seeded) keeps the bare name; a body sentinel of a
        // DIFFERENT-canonical `Foo` is FQN-ed (the #195/#197 anchor case, generalised)
        Map<String, String> seed = ImportCollisionResolver.seedFromCanonicals(
                List.of("cdm.out.Foo"));
        String body = ".<" + ref("fpml.in.Foo") + ">map(...)";
        var r = ImportCollisionResolver.resolve(List.of(body), seed);
        assertEquals(".<fpml.in.Foo>map(...)", r.bodies().get(0));
        assertEquals(java.util.Set.of("fpml.in.Foo"), r.suppressedCanonicals());
    }

    @Test
    void seedSameCanonicalStaysBare() {
        Map<String, String> seed = ImportCollisionResolver.seedFromCanonicals(List.of("a.b.X"));
        String body = ref("a.b.X") + ".builder()";
        var r = ImportCollisionResolver.resolve(List.of(body), seed);
        assertEquals("X.builder()", r.bodies().get(0));
        assertTrue(r.suppressedCanonicals().isEmpty());
    }

    @Test
    void stripToBareNeutralisesSentinels() {
        String body = ref("a.b.X") + ".builder().<" + ref("c.d.Y") + ">map()";
        assertEquals("X.builder().<Y>map()", ImportCollisionResolver.stripToBare(body));
        assertFalse(ImportCollisionResolver.hasSentinel(ImportCollisionResolver.stripToBare(body)));
    }

    @Test
    void sentinelFreeBodyUnchanged() {
        String body = "PaymentDates.builder().setX(1)";
        var r = ImportCollisionResolver.resolve(List.of(body), Map.of());
        assertEquals(body, r.bodies().get(0));
        assertTrue(r.suppressedCanonicals().isEmpty());
        assertFalse(ImportCollisionResolver.hasSentinel(body));
    }
    // ---- v3.2 seat 11, round 1 (cq MF-5): the three seat-11 entry points and the round's helper, pinned at the producer ----

    @Test
    void typeRefOrBare_javaLangTopLevelIsBare_everythingElseASentinel_nestedAndSubPackageJavaLangIncluded() {
        assertEquals("String", ImportCollisionResolver.typeRefOrBare("java.lang.String"));
        // round 2 (spec MF-1 / cq MF-1): a NESTED java.lang type is a sentinel whose bare form is its simple name (upstream
        // writes State when free, the canonical when taken - never Thread.State); a java.lang SUB-PACKAGE type is a sentinel
        // whose bare form upstream imports (Method) - round 1's branch had written reflect.Method, invalid Java
        String nested = ImportCollisionResolver.typeRefOrBare("java.lang.Thread.State");
        assertTrue(ImportCollisionResolver.hasSentinel(nested));
        assertEquals("State", ImportCollisionResolver.stripToBare(nested));
        assertEquals("State", ImportCollisionResolver.resolve(List.of(nested), Map.of()).bodies().get(0));
        String sub = ImportCollisionResolver.typeRefOrBare("java.lang.reflect.Method");
        assertTrue(ImportCollisionResolver.hasSentinel(sub));
        assertEquals("Method", ImportCollisionResolver.stripToBare(sub));
        // the SENTINEL carries the canonical (OPEN + canonical + CLOSE) - the claim is on the RESOLVED form: bare Method,
        // never reflect.Method (the targeted run's catch on this test's first cut: it had asserted on the sentinel string)
        assertEquals("Method", ImportCollisionResolver.resolve(List.of(sub), Map.of()).bodies().get(0));
        String ref = ImportCollisionResolver.typeRefOrBare("java.util.List");
        assertTrue(ImportCollisionResolver.hasSentinel(ref));
        assertEquals("List", ImportCollisionResolver.stripToBare(ref));
        assertEquals(ImportCollisionResolver.typeRef("java.util.List"), ref);
    }

    @Test
    void typeRefOrBare_nestedJavaLangTakenLeg_writtenCanonicalWhenAModelTypeClaimedTheName() {
        // round 3 (cq NIT-1 / spec NIT-2): the javadoc's SECOND leg pinned - a nested java.lang type whose simple name the
        // file's own class already claims is written canonical (upstream: JavaFileScope.getIdentifier finds State taken and
        // internalDoImportIfPossible returns the canonical); the free leg is the test above.
        // round 4 (cq SF-2 / spec NIT-2): the IMPORT leg made able to fail - round 3 passed List.of() and asserted isEmpty(),
        // which holds for every implementation; the loser's own canonical goes in beside an unrelated import: the loser's
        // is DROPPED (suppressed with its canonical write), the unrelated one KEPT
        String nested = ImportCollisionResolver.typeRefOrBare("java.lang.Thread.State");
        var r = ImportCollisionResolver.resolveClass("public interface State { " + nested + " getState(); }",
                "census.State", List.of("java.lang.Thread.State", "java.util.List"));
        assertEquals("public interface State { java.lang.Thread.State getState(); }", r.classText());
        assertEquals(List.of("java.util.List"), r.imports());
    }

    @Test
    void typeRefs_mapsSimpleNameToSentinel_andRefusesTwoTokensWithOneSimpleName() {
        Map<String, String> refs = ImportCollisionResolver.typeRefs(List.of("java.util.List", "java.lang.String"));
        assertEquals(java.util.Set.of("List", "String"), refs.keySet());
        assertEquals(ImportCollisionResolver.typeRef("java.util.List"), refs.get("List"));
        assertEquals("String", refs.get("String"));
        IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ImportCollisionResolver.typeRefs(List.of("java.util.List", "census.seat11.List")));
        assertTrue(ex.getMessage().contains("java.util.List") && ex.getMessage().contains("census.seat11.List"));
    }

    @Test
    void resolveClass_sentinelFreeTextIsTheFastPath_bytesAndImportsUntouched() {
        String text = "public interface Plain { List<String> getBs(); }";
        var r = ImportCollisionResolver.resolveClass(text, "census.Plain", List.of("java.util.List"));
        assertEquals(text, r.classText());
        assertEquals(List.of("java.util.List"), r.imports());
    }

    @Test
    void resolveClass_ownClassIsTheSeed_loserWrittenCanonicalAndItsImportDropped() {
        String text = "public interface List { " + ref("java.util.List") + "<String> getBs(); }";
        var r = ImportCollisionResolver.resolveClass(text, "census.List", List.of("java.util.List", "java.util.ArrayList"));
        assertEquals("public interface List { java.util.List<String> getBs(); }", r.classText());
        assertEquals(List.of("java.util.ArrayList"), r.imports());
    }

    @Test
    void resolveClass_noCollisionResolvesBareWithAnEmptyImportList() {
        String text = "public interface Plain { " + ref("java.util.List") + "<String> getBs(); }";
        var r = ImportCollisionResolver.resolveClass(text, "census.Plain", List.of());
        assertEquals("public interface Plain { List<String> getBs(); }", r.classText());
        assertTrue(r.imports().isEmpty());
    }

    @Test
    void resolveClass_eachClassTextResolvesFromItsOwnSeed_oneCanonicalWinsInOneFileAndLosesInAnother() {
        String body = ref("java.util.List") + "<String> getBs();";
        var wins = ImportCollisionResolver.resolveClass("interface Plain { " + body + " }", "census.Plain", List.of("java.util.List"));
        var loses = ImportCollisionResolver.resolveClass("interface List { " + body + " }", "census.List", List.of("java.util.List"));
        assertEquals("interface Plain { List<String> getBs(); }", wins.classText());
        assertEquals(List.of("java.util.List"), wins.imports());
        assertEquals("interface List { java.util.List<String> getBs(); }", loses.classText());
        assertTrue(loses.imports().isEmpty());
    }

    @Test
    void simpleIsLastSegment_theOneValueSiteTest() {
        assertTrue(ImportCollisionResolver.simpleIsLastSegment("java.math.BigDecimal", "BigDecimal"));
        assertFalse(ImportCollisionResolver.simpleIsLastSegment("java.util.List", "List<String>"));
        assertFalse(ImportCollisionResolver.simpleIsLastSegment("java.util.Map", "Map.Entry"));
    }
}
