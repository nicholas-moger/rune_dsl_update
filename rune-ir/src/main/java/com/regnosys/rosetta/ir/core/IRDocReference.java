package com.regnosys.rosetta.ir.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One {@code [docReference …]} / {@code [regulatoryReference …]} written on a declaration, a field, a
 * choice option, an enum or an enum value — every part a backend renders into the member's
 * documentation, carried whole so a backend needs no second look at the parsed source.
 *
 * <p>A corpus is carried TWICE over: {@link Corpus#reference()} is the text the model wrote (it may be
 * qualified through an import alias) and {@link Corpus#resolved()} the declaration it names, resolved in
 * the REFERENCING file's scope — the rendered line uses the declaration's own name, display name and
 * definition, never the reference text.
 *
 * @param regulatory          true for {@code regulatoryReference}, false for {@code docReference}
 * @param path                the optional {@code for <path>} the reference is scoped to
 * @param body                the referenced body, when the reference names a document
 * @param corpora             the referenced corpora, in source order
 * @param segments            the {@code segmentName "value"} pairs, in source order
 * @param rationales          the {@code rationale} / {@code rationale_author} pairs, in source order
 * @param structuredProvision the {@code structured_provision} text
 * @param provision           the {@code provision} text
 * @param reportedField       true when the reference is marked {@code reportedField}
 * @param namedArgs           the remaining {@code name "value"} arguments, in source order
 */
public record IRDocReference(boolean regulatory, Optional<IRAnnotationPath> path, Optional<String> body, List<Corpus> corpora,
                             List<Segment> segments, List<Rationale> rationales,
                             Optional<String> structuredProvision, Optional<String> provision,
                             boolean reportedField, List<NamedArg> namedArgs) {

    public IRDocReference {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(body, "body");
        corpora = List.copyOf(corpora);
        segments = List.copyOf(segments);
        rationales = List.copyOf(rationales);
        Objects.requireNonNull(structuredProvision, "structuredProvision");
        Objects.requireNonNull(provision, "provision");
        namedArgs = List.copyOf(namedArgs);
    }

    /**
     * A referenced corpus.
     *
     * @param reference the text the model wrote
     * @param resolved  the corpus declaration it names, when the referencing file's scope resolves it
     */
    public record Corpus(String reference, Optional<Declaration> resolved) {
        public Corpus {
            Objects.requireNonNull(reference, "reference");
            Objects.requireNonNull(resolved, "resolved");
        }

        /**
         * The resolved corpus declaration's own facts.
         *
         * @param typeKeyword the corpus type keyword ({@code Regulation}, {@code TechnicalStandard}, …), when declared
         * @param name        the declaration's own simple name
         * @param displayName its display name
         * @param definition  its definition
         */
        public record Declaration(Optional<String> typeKeyword, String name, Optional<String> displayName,
                                  Optional<String> definition) {
            public Declaration {
                Objects.requireNonNull(typeKeyword, "typeKeyword");
                Objects.requireNonNull(name, "name");
                Objects.requireNonNull(displayName, "displayName");
                Objects.requireNonNull(definition, "definition");
            }
        }
    }

    /** A {@code segmentName "value"} pair. */
    public record Segment(String name, String value) {
        public Segment {
            Objects.requireNonNull(name, "name");
            value = value == null ? "" : value;
        }
    }

    /** A {@code rationale} with its optional {@code rationale_author}. */
    public record Rationale(Optional<String> text, Optional<String> author) {
        public Rationale {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(author, "author");
        }
    }

    /** A remaining {@code name "value"} argument. */
    public record NamedArg(String name, String value) {
        public NamedArg {
            Objects.requireNonNull(name, "name");
            value = value == null ? "" : value;
        }
    }
}
