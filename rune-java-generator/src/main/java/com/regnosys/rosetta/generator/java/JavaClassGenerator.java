package com.regnosys.rosetta.generator.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.rosetta.util.types.JavaTypeDeclaration;

/**
 * Abstract base class for per-element Java code generators. Each concrete
 * generator selects elements from a model, creates a type representation,
 * and generates Java source code for each element.
 *
 * <p>Ported from upstream — replaced EMF {@code Resource/EObject} with our
 * AST types, replaced {@code IFileSystemAccess2} with {@code Map<String,String>}
 * output collection, replaced {@code RosettaModel} with {@code RModel}.
 *
 * @param <T> the AST element type this generator processes
 * @param <C> the Java type representation for generated output
 */
public abstract class JavaClassGenerator<T, C extends JavaTypeDeclaration<?>> {

    /**
     * Stream elements of type T from the model.
     */
    protected abstract Stream<? extends T> streamObjects(RModel model);

    /**
     * Create the Java type representation for a given element.
     */
    protected abstract C createTypeRepresentation(T object);

    /**
     * Generate Java source code for a single element.
     *
     * @param object the AST element
     * @param typeRepresentation the Java type representation
     * @param version the model version string (may be null)
     * @return the generated Java source code
     */
    protected abstract String generate(T object, C typeRepresentation, String version);

    /**
     * Generate Java classes for all relevant elements in the model.
     * Returns a list of generation errors (empty if all succeeded).
     *
     * @param model the parsed model
     * @param version the model version string
     * @param output map to collect generated files: canonical path → source code
     * @return list of generation errors
     */
    public List<GenerationException> generateClasses(RModel model, String version,
                                                      Map<String, String> output) {
        List<GenerationException> errors = new ArrayList<>();
        streamObjects(model).forEach(object -> {
            // Resolve the target path BEFORE body emission: the type representation
            // (and thus the output path) is known up front, while generate() may
            // throw. Attaching the path to any failure lets the regression gate tell
            // an already-waivered (known-incomplete) element from a new one.
            String filePath = null;
            try {
                C typeRepresentation = createTypeRepresentation(object);
                filePath = typeRepresentation.getCanonicalName().withForwardSlashes() + ".java";
                String javaFileCode = generate(object, typeRepresentation, version);
                output.put(filePath, javaFileCode);
            } catch (GenerationException e) {
                e.setTargetPath(filePath);
                errors.add(e);
            } catch (Exception e) {
                GenerationException wrapped = new GenerationException(e.getMessage(), null, null, e);
                wrapped.setTargetPath(filePath);
                errors.add(wrapped);
            }
        });
        return errors;
    }
}
