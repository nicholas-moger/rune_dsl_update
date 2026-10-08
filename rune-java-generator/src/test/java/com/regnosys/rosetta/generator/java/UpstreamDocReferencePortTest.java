package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.Test;

/**
 * Leg-C port (PR #414) of upstream
 * {@code rune-integration-tests/.../docrefs/DocReferenceTest.xtend} — 10/10
 * methods, all parse-acceptance ({@code parseRosettaWithNoErrors}): the harness's
 * {@link UpstreamPortHarness#generate} fails the test on any parse or generation
 * error — STRONGER than upstream on the generation axis (the snippets also flow
 * through the full generator battery), though upstream's parse helper additionally
 * asserts Xtext VALIDATION diagnostics the fork pipeline does not replay (inert
 * for these valid-by-construction snippets; the diagnostics harness is a slice-2
 * item — see the ledger). No compile step — these
 * lock grammar acceptance of the doc-annotation surface (body/corpus/segment/
 * docReference on every element kind). Ledger:
 * the development audit "2026-07-17-leg-c-integration-port-ledger".
 */
class UpstreamDocReferencePortTest {

    /** Upstream {@code declearCorpusWithBodyReference} (sic — upstream's name kept). */
    @Test
    void declearCorpusWithBodyReference() {
        UpstreamPortHarness.generate("""
                body Organisation Org1 <"some description 1">
                corpus Agreement Org1 "Agreement 1" Agr1 <"some description 2">

                """);
    }

    /** Upstream {@code declearCorpusWithoutBodyReference}. */
    @Test
    void declearCorpusWithoutBodyReference() {
        UpstreamPortHarness.generate("""
                corpus Agreement "Agreement 1" Agr1 <"some description 2">

                """);
    }

    /** Upstream {@code corpusDisplaytNameIsOptional} (sic). */
    @Test
    void corpusDisplaytNameIsOptional() {
        UpstreamPortHarness.generate("""
                corpus Agreement Agr1
                """);
    }

    /** Upstream {@code typeCanHaveDocRef}. */
    @Test
    void typeCanHaveDocRef() {
        UpstreamPortHarness.generate("""
                body Organisation Org1
                corpus Agreement Org1 "Agreement 1" Agr1

                segment name

                type Foo:
                	[docReference Org1 Agr1 name "something" provision "some provision"]
                	bar string (1..1)

                """);
    }

    /** Upstream {@code docRefProvisionIsOptional}. */
    @Test
    void docRefProvisionIsOptional() {
        UpstreamPortHarness.generate("""
                body Organisation Org1
                corpus Agreement Org1 "Agreement 1" Agr1

                segment name

                type Foo:
                	[docReference Org1 Agr1 name "something"]
                	bar string (1..1)

                """);
    }

    /** Upstream {@code attributeCanHaveDocRef}. */
    @Test
    void attributeCanHaveDocRef() {
        UpstreamPortHarness.generate("""
                body Organisation Org1
                corpus Agreement Org1 "Agreement 1" Agr1

                segment name

                type Foo:
                	bar string (1..1)
                		[docReference Org1 Agr1 name "something"]

                """);
    }

    /** Upstream {@code enumCanHaveDocRef}. */
    @Test
    void enumCanHaveDocRef() {
        UpstreamPortHarness.generate("""
                body Organisation Org1
                corpus Agreement Org1 "Agreement 1" Agr1

                segment name

                enum Foo:
                [docReference Org1 Agr1 name "something"]
                	bar


                """);
    }

    /** Upstream {@code enumValueCanHaveDocRef}. */
    @Test
    void enumValueCanHaveDocRef() {
        UpstreamPortHarness.generate("""
                body Organisation Org1
                corpus Agreement Org1 "Agreement 1" Agr1

                segment name

                enum Foo:
                	bar
                		[docReference Org1 Agr1 name "something"]
                """);
    }

    /** Upstream {@code functionsCanHaveDocRef}. */
    @Test
    void functionsCanHaveDocRef() {
        UpstreamPortHarness.generate("""
                body Organisation Org1
                corpus Agreement Org1 "Agreement 1" Agr1

                segment name

                func Sum:
                	[docReference Org1 Agr1 name "something"]
                	inputs: x number (0..*)
                	output: s number (1..1)

                """);
    }

    /** Upstream {@code conditionsCanHaveDocRef}. */
    @Test
    void conditionsCanHaveDocRef() {
        UpstreamPortHarness.generate("""
                body Organisation Org1
                corpus Agreement Org1 "Agreement 1" Agr1

                segment name

                type Foo:
                	a int (1..1)

                	condition:
                		[docReference Org1 Agr1 name "something"]
                		a > 0


                """);
    }
}
