/**
 * Deterministic, hand-rolled, zero-dependency JSON for the neutral IR (step-2) — a versioned
 * tagged-object wire form that is both a durable golden sentinel and the stable contract an
 * external (non-JVM) consumer reads, writes, and validates. Read-only over the IR (cannot affect
 * the Java byte-parity path); the test-scope Jackson dependency is used only by tests — nothing
 * here depends on it, or on Xtext/EMF/the java generator.
 *
 * <h2>The surface</h2>
 * <ul>
 *   <li><b>Document model.</b> {@link Json} is an immutable JSON value tree; {@link JsonWriter}
 *       renders it to deterministic 2-space text, and {@link JsonReader} parses text back to a
 *       {@link Json} tree.</li>
 *   <li><b>Write.</b> {@link IRJsonSerializer} builds the {@link Json} tree from IR nodes by
 *       switching on the closed discriminator enums (no GoF visitor) — the versioned
 *       {@code {"irFormatVersion":1,"nodes":[...]}} declaration document, plus an expression
 *       fragment form (a debug-parity aid, not goldened).</li>
 *   <li><b>Read + round-trip.</b> {@link IRJsonDeserializer} (over {@link JsonReader}) is the
 *       strict inverse for the five declaration kinds plus the generic fallback: it rejects an
 *       unknown/duplicate/missing member and holds the round-trip
 *       {@code serializer.toJson(deserializer.fromJson(json)).equals(json)} for any
 *       serializer-canonical declaration document.</li>
 *   <li><b>Schema.</b> {@link IRJsonSchema} emits the Draft 2020-12 JSON Schema for the v1
 *       declaration wire form (built as a {@link Json} tree, rendered by {@link JsonWriter}), so a
 *       non-JVM consumer can validate IR JSON without the JVM model. A drift sentinel test pins it
 *       to the serializer's emission.</li>
 *   <li>{@link IRJsonException} is the package's uniform parse/validation error.</li>
 * </ul>
 *
 * <p>Expressions and the generic fallback are intentionally out of the frozen declaration
 * deserializer/schema scope (their wire form is not yet pinned).
 */
package com.regnosys.rosetta.ir.json;
