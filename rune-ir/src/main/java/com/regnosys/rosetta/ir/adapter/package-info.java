/**
 * RNode → IRNode adapter package.
 *
 * <p>Empty in P1.4.3 (type surface only — see decision D21).
 * Concrete adapter classes land in P2 when the first IR consumer is wired
 * (Java codegen rewire, Python target, or a cross-DSL importer — whichever first).
 *
 * <p>This package exists so that:
 * <ul>
 *   <li>P2 PRs have a stable target namespace.
 *   <li>japicmp can track surface-area additions to this package once
 *       consumers exist.
 *   <li>Future cross-DSL imports (W22) bypass this package — they construct IR
 *       directly. This package is for the rune-dsl-plus → IR direction only.
 *       The IR JSON deserializer ({@code com.regnosys.rosetta.ir.json.IRJsonDeserializer}) is a sanctioned
 *       non-AST IR constructor that reuses these records directly (the step-2b read-vs-write asymmetry);
 *       it is the one importer that does NOT bypass this package.
 * </ul>
 */
package com.regnosys.rosetta.ir.adapter;
