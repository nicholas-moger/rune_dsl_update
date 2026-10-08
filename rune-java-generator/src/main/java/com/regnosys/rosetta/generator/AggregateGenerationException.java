package com.regnosys.rosetta.generator;

import java.util.List;

/**
 * Aggregated generation exception collecting multiple errors from a single
 * generation pass.
 */
public class AggregateGenerationException extends RuntimeException {
    private static final long serialVersionUID = 3129706933187670252L;
    private final String resourceUri;
    private final List<GenerationException> generationExceptions;

    public AggregateGenerationException(String message, String resourceUri,
                                        List<GenerationException> generationExceptions) {
        // Upstream contract: callers never pass an empty list (RosettaGenerator
        // checks size > 0 before constructing). Preserving exact upstream behaviour.
        super(message, generationExceptions.get(0));
        this.resourceUri = resourceUri;
        this.generationExceptions = generationExceptions;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public List<GenerationException> getGenerationExceptions() {
        return generationExceptions;
    }

    @Override
    public String toString() {
        return "AggregateGenerationException [resourceUri=" + resourceUri
                + ", generationExceptions=" + generationExceptions
                + ", getMessage()=" + getMessage() + "]";
    }
}
