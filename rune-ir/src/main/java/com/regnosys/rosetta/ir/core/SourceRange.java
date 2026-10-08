package com.regnosys.rosetta.ir.core;

import java.net.URI;

/**
 * Position-tracked IR-side source range, DSL-agnostic.
 *
 * <p>Distinct from {@code com.regnosys.rosetta.ast.SourceRange} (the
 * AST-side type that's tied to {@code RNode} and rune-specific byte-offset
 * infrastructure). This record can be constructed by a cross-DSL
 * importer (W22) without pulling rune AST dependencies.
 *
 * @param startOffset byte offset of the first character (inclusive)
 * @param endOffset byte offset just past the last character (exclusive)
 * @param source URI identifying the source artefact (file, in-memory string, etc.)
 */
public record SourceRange(int startOffset, int endOffset, URI source) {}
