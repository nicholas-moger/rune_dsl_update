package com.regnosys.rosetta.upgrades;

import com.regnosys.rosetta.testutil.CorpusWalker;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the mandated U-template structure (9 sections matching U006 depth)
 * for every U-NNN manifest entry.
 *
 * <p><b>Allow-list scope:</b> this test enforces the 9-section template
 * + YAML frontmatter on ALL 22 U-NNN manifest entries (U001-U022). The
 * pre-P1.7-PR-3 allow-list (U008/U011/U012 only — the three manifests
 * authored after the 9-section template was introduced) was broadened
 * in P1.7 PR-3 closeout per D23 — U001-U007 + U009-U010 retrofitted to
 * canonical 9-section shape (then 12 U-NNN U001-U012). U013 STUB added
 * at P2.0.1 T15 per D30 (Path-2 IR shadow-path Java codegen first
 * consumer) — manifest body is STUB-only at landing; full content
 * deferred to P2.1+ alongside Path-2 implementation. U015 added at
 * P2.1.3c per D37 (Cluster F final retirement — top-level choice POJO
 * codegen + resolver hardening); FULL manifest, not STUB. U016 added
 * at Phase X PR #72 per D38 (3-generator Java codegen port — scaffolding
 * only; body emission deferred to Phase X1); FULL manifest.
 * Original template adoption date: 2026-04-29; full-population retrofit +
 * allow-list broaden: P1.7 PR-3 (2026-05-06); U013 add: P2.0.1
 * (2026-05-08); U014 add: P2.0.2 (2026-05-09 per W36 first-class
 * document structure design lock; spec at docs/superpowers/specs/2026-05-08-w36-first-class-document-structure-design.md (local));
 * U015 add: P2.1.3c (2026-05-16 per D37 Cluster F final retirement;
 * spec at docs/superpowers/specs/2026-05-15-p2.1.3c-cluster-f-final-design.md (local));
 * U016 add: Phase X PR #72 (2026-05-20 per D38 3-generator scaffolding
 * port; scope memo at the development audit "phase-x-T8X-body-emission-scope").
 * U017 add: PR #458 (2026-07-22 — the drop-in Maven plugin, Leg P of the
 * drop-in parity program; FULL manifest, not STUB).
 * U018 add: PR #459 (2026-07-23 — the fork-owned runtime, Leg R / R2 of the
 * drop-in parity program; FULL manifest, not STUB).
 * U019 + U020 add: PR #547 (2026-08-11 — U020 the lazy failure-text runtime
 * channel lands; U019, the optimised-codegen route manifest from PR #538,
 * had been omitted from this allow-list when it shipped — the gap closed
 * here in the same sweep; both FULL manifests, not STUBs).
 * U021 add: PR #550 (2026-08-12 — the deferred MapperPath lineage runtime
 * channel, v3 PR-14 / PR-B = B2; FULL manifest, not STUB).
 * U022 add: PR #553 (2026-08-12 — the seed-deferred MapperPath construction
 * runtime channel, v3 PR-17 / the § 18 seed edit; FULL manifest, not STUB;
 * the omission-at-open caught by the Rule-6 pre-merge audit — the Rule-4
 * failure class this allow-list's own history warns about).
 *
 * <p>If a future U-NNN entry is added without the 9-section template,
 * this test fails fast pointing at the first missing section. Use
 * {@code U006-structured-diagnostics.md} or {@code U008-rune-annotations.md}
 * as the depth benchmark.
 *
 * <p><b>Path resolution:</b> {@code docs/upgrades/} is resolved via
 * {@link CorpusWalker#repoRoot()} (classfile-derived, JVM-cwd independent).
 * Using {@code Path.of("../docs/upgrades")} would break under
 * {@code mvn -f rune-parser/pom.xml test} invoked from the repo root.
 */
class UpgradeManifestStructureTest {

    private static final List<String> ALLOWED_FILES = List.of(
            "U001-bytewise-source-range.md",
            "U002-frozen-ast-after-linker.md",
            "U003-rnode-metadata-slot.md",
            "U004-ast-interface-registry.md",
            "U005-symbolid-cross-references.md",
            "U006-structured-diagnostics.md",
            "U007-lsp-symbol-kinds.md",
            "U008-rune-annotations.md",
            "U009-file-header.md",
            "U010-regulatory-reference-named-args.md",
            "U011-rule-annotations.md",
            "U012-ir-scaffolding.md",
            "U013-ir-mediated-java-codegen.md",
            "U014-first-class-document-structure.md",
            "U015-choice-pojo-codegen.md",
            "U016-rule-report-labelprovider-codegen.md",
            "U017-drop-in-maven-plugin.md",
            "U018-fork-owned-runtime.md",
            "U019-optimised-ir-java-codegen.md",
            "U020-lazy-failure-text-runtime.md",
            "U021-deferred-mapper-path-lineage.md",
            "U022-seed-deferred-mapper-path-construction.md",
            "U023-value-typed-alias-seams.md"
    );

    /**
     * The 9 sections every allow-listed manifest must contain. Header text is
     * matched literally via {@link String#contains(CharSequence)} — no regex.
     */
    private static final List<String> REQUIRED_SECTIONS = List.of(
            "## Concept",
            "## Why",
            "## Structural changes",
            "## BC Story",
            "## How to use",
            "## Migration",
            "## Test coverage",
            "## Coverage",
            "## Cross-references"
    );

    @Test
    void allowListedManifestEntriesHaveRequiredSections() throws IOException {
        Path upgradesDir = CorpusWalker.repoRoot().resolve("docs/upgrades");
        for (String name : ALLOWED_FILES) {
            Path file = upgradesDir.resolve(name);
            assertTrue(Files.exists(file),
                    "Allow-listed manifest missing: " + file +
                            " (resolved from CorpusWalker.repoRoot())");
            String content = Files.readString(file);
            for (String section : REQUIRED_SECTIONS) {
                assertTrue(content.contains(section),
                        name + " missing required section: " + section +
                                " — see docs/upgrades/U006-structured-diagnostics.md " +
                                "for depth benchmark.");
            }
        }
    }
}
