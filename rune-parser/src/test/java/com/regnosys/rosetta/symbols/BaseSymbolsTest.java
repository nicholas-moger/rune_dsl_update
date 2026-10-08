package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.RRootElement;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Shared helpers for symbol table / linker tests. Mirrors the role of
 * {@code BaseAstTest} for M2 tests.
 */
public abstract class BaseSymbolsTest {

    /** Parses a single source string into an {@link RModel}. */
    protected RModel parseModel(String source) {
        return AstBuilder.buildFromString(source, getClass().getSimpleName() + ".rosetta");
    }

    /** Parses + links a single source. Convenience for one-file scenarios. */
    protected RLinkingResult parseAndLink(String source) {
        return RWorkspace.build(List.of(parseModel(source)));
    }

    /** Parses multiple sources and links them as one workspace.
     *  Each source gets a unique filename to prevent diagnostic mixing. */
    protected RLinkingResult parseAndLink(String... sources) {
        List<RModel> models = new java.util.ArrayList<>();
        for (int i = 0; i < sources.length; i++) {
            models.add(AstBuilder.buildFromString(sources[i],
                getClass().getSimpleName() + "-" + i + ".rosetta"));
        }
        return RWorkspace.build(models);
    }

    /** Finds a top-level data type by name in a model. */
    protected RDataType findType(RModel model, String name) {
        for (RRootElement el : model.rootElements()) {
            if (el instanceof RDataType dt && name.equals(dt.name())) {
                return dt;
            }
        }
        throw new NoSuchElementException(
            "type '" + name + "' not found in " + model.namespace());
    }

    /** Finds a top-level function by name in a model. */
    protected RFunction findFunction(RModel model, String name) {
        for (RRootElement el : model.rootElements()) {
            if (el instanceof RFunction fn && name.equals(fn.name())) {
                return fn;
            }
        }
        throw new NoSuchElementException(
            "function '" + name + "' not found in " + model.namespace());
    }
}
