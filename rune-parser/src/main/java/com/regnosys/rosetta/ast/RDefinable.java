package com.regnosys.rosetta.ast;

import java.util.Optional;

/**
 * Interface for AST nodes that may carry an optional human-readable definition.
 *
 * <p>In the Rosetta grammar, the {@code definable} rule produces the
 * {@code <"description text">} syntax. The following grammar rules include
 * an optional {@code definable?}:
 *
 * <ol>
 *   <li>{@code rosettaModel} — namespace definition</li>
 *   <li>{@code rosettaScope} — scope definition</li>
 *   <li>{@code annotationDecl} — annotation declaration</li>
 *   <li>{@code dataType} — data type declaration</li>
 *   <li>{@code choice} — choice declaration</li>
 *   <li>{@code choiceOption} — individual choice option</li>
 *   <li>{@code attribute} — attribute on a data type or function</li>
 *   <li>{@code enumeration} — enum declaration</li>
 *   <li>{@code rosettaEnumValue} — individual enum value</li>
 *   <li>{@code function} — function declaration</li>
 *   <li>{@code shortcutDeclaration} — alias declaration in a function</li>
 *   <li>{@code condition} — condition block</li>
 *   <li>{@code postCondition} — post-condition block</li>
 *   <li>{@code operation} — set/add operation in a function</li>
 *   <li>{@code rosettaBody} — regulatory body declaration</li>
 *   <li>{@code rosettaCorpus} — regulatory corpus declaration</li>
 *   <li>{@code rosettaBasicType} — built-in basic type declaration</li>
 *   <li>{@code rosettaRecordType} — built-in record type (on its definable? inside braces)</li>
 *   <li>{@code rosettaLibraryFunction} — library function declaration</li>
 *   <li>{@code rosettaTypeAlias} — type alias declaration</li>
 *   <li>{@code typeParameter} — type parameter declaration</li>
 *   <li>{@code rosettaRule} — reporting / eligibility rule</li>
 * </ol>
 */
public interface RDefinable {

    /**
     * Returns the optional human-readable definition text.
     *
     * <p>When present, corresponds to the {@code <"...">} syntax in Rune DSL
     * source. Returns {@link Optional#empty()} when no definition is provided.
     */
    Optional<String> definition();
}
