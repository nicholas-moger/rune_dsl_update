package com.regnosys.rosetta.generator.java.spi;

/**
 * Service-provider interface for the v3 OPTIMISED IR-routed generation path
 * (route 3 of the one-IR/three-backends architecture — the phase plan
 * the development plan "2026-08-10-p3-optimised-codegen-phase-plan" § 2). The
 * {@code rune-ir-java-optimised} module implements this interface and
 * registers it via {@code META-INF/services} under THIS interface's name —
 * never under {@link IRGenerationProvider}'s — so the reference route's
 * lookup cannot see the optimised registration and both jars may coexist on
 * one classpath without tripping either exclusivity check.
 *
 * <p>{@link IRGeneration} resolves it only when the opt-in flag
 * {@code -Drosetta.generator.ir.optimised=true} is set; with the flag off the
 * provider is never looked up and every call site executes the exact legacy
 * Path-1 code, byte-identical by construction (the same contract the
 * reference route's seam carries). Setting BOTH route flags is a loud
 * configuration error — the routes are exclusive.
 *
 * <p>The seam surface is inherited unchanged from {@link IRGenerationProvider}
 * (six construction seams + two dispatch seams): the optimised backend claims
 * exactly the generator kinds whose emission families have landed and DECLINES
 * everything else (construction seams return the plain legacy generators;
 * dispatch seams return {@code null}/{@code false}), so un-claimed kinds fall
 * back to Path-1 — whose output the byte-parity rings prove identical to the
 * reference route's. Frozen-kind service is therefore automatic: only
 * re-emitted families ever produce different bytes, and only behind this flag.
 */
public interface OptimisedIRGenerationProvider extends IRGenerationProvider {
}
