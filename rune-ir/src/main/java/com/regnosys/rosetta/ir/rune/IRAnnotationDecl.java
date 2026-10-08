package com.regnosys.rosetta.ir.rune;

import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRParameter;

import java.util.List;

/**
 * Annotation declaration — a user-defined annotation type that other source
 * elements can reference. Rune-specific. {@link IRNode#kind()} returns
 * {@link IRKind#ANNOTATION_DECL}.
 *
 * <p>Distinct from annotation <em>instances</em> (those attach to RNode but
 * are not RRootElement subclasses; out of P1.4.3 IR scope).
 */
public interface IRAnnotationDecl extends IRNode {
    /**
     * Declared annotation parameters, in source-declaration order.
     * Non-null; unmodifiable. Empty for parameterless annotations.
     */
    List<IRParameter> parameters();
}
