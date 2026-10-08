/**
 * The neutral emitter SPI — the authoring surface for new (non-byte-parity) targets that lower the
 * neutral IR to a concrete language.
 *
 * <p>{@link com.regnosys.rosetta.ir.emit.IRExprEmitter} is the expression-emitter contract;
 * {@link com.regnosys.rosetta.ir.emit.AbstractIRExprEmitter} provides the per-node dispatch + the
 * single {@link com.regnosys.rosetta.ir.emit.EmitterException} decline choke; backends (e.g.
 * {@code com.regnosys.rosetta.ir.emit.python.IRPythonEmitter}) override only the kinds they support.
 *
 * <p>Neutrality (L-029): this package carries no Xtext/EMF/{@code rune-java-generator} type — the IR
 * carries facts, each target derives its own forms. The byte-parity Java emitter stays outside this SPI.
 */
package com.regnosys.rosetta.ir.emit;
