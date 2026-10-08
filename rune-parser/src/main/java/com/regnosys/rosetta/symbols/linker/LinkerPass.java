package com.regnosys.rosetta.symbols.linker;

/**
 * Tag interface for the five linker passes. Each pass is stateless except
 * for its output and consumes the previous pass's output. Passes are run
 * sequentially by {@link com.regnosys.rosetta.symbols.RWorkspace#build(java.util.List)}
 * in strict topological order:
 *
 * <ol>
 *   <li>{@link SymbolRegistrationPass}</li>
 *   <li>ImportResolutionPass (T6)</li>
 *   <li>DerivedStatePass (T7)</li>
 *   <li>GlobalResolutionPass (T8-T9)</li>
 *   <li>LexicalResolutionPass (T10)</li>
 * </ol>
 *
 * <p>Spec: D7 in {@code docs/specs/2026-04-07-m3-symbols-resolution-design.md}.
 */
public interface LinkerPass {
}
