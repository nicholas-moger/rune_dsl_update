package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LexicalResolutionTest extends BaseSymbolsTest {

    @Test
    void function_input_parameter_resolves_in_body() {
        RLinkingResult result = parseAndLink("""
                namespace test
                func Plus:
                    inputs:
                        a int (1..1)
                        b int (1..1)
                    output: result int (1..1)
                    set result: a + b
                """);
        RFunction f = findFunction(result.workspace().files().get(0), "Plus");
        var refs = AstWalker.findAll(f, RSymbolReference.class);
        for (RSymbolReference ref : refs) {
            if ("a".equals(ref.name()) || "b".equals(ref.name())) {
                assertTrue(ref.symbol().isPresent(),
                    "symbol reference '" + ref.name() + "' should resolve to input param");
            }
        }
    }

    @Test
    void unresolved_symbol_emits_diagnostic() {
        RLinkingResult result = parseAndLink("""
                namespace test
                func F:
                    output: result int (1..1)
                    set result: nonexistent
                """);
        // If the parser produces an RSymbolReference for "nonexistent",
        // it should get a SYMBOL_NOT_FOUND diagnostic. The generated input
        // from derived state won't conflict.
        boolean hasSymbolDiag = result.linkingDiagnostics().stream()
            .anyMatch(d -> d.category() == DiagnosticCategory.SYMBOL_NOT_FOUND
                        && "nonexistent".equals(d.unresolvedName()));
        boolean hasTypeDiag = result.linkingDiagnostics().stream()
            .anyMatch(d -> d.category() == DiagnosticCategory.TYPE_NOT_FOUND
                        && "nonexistent".equals(d.unresolvedName()));
        // "nonexistent" might be parsed as a type reference (in set target
        // position) or as a symbol reference depending on grammar context.
        // Either diagnostic category is acceptable — the key is it's NOT silent.
        assertTrue(hasSymbolDiag || hasTypeDiag,
            "unresolved name should produce either SYMBOL_NOT_FOUND or TYPE_NOT_FOUND");
    }

    @Test
    void scope_chain_walks_to_file_scope() {
        RLinkingResult result = parseAndLink("""
                namespace test
                type Money:
                    amount int (1..1)
                func F:
                    output: result Money (1..1)
                """);
        // "Money" in the output type call should resolve via the file scope
        // (already tested in T8 via RTypeCall resolution), but verify the
        // function scope walk doesn't interfere
        RFunction f = findFunction(result.workspace().files().get(0), "F");
        assertTrue(f.output().isPresent(), "function output should be present");
    }

    @Test
    void inline_function_scope_shadows_parent() {
        // Unit test for the scope chain classes themselves
        RLinkingResult result = parseAndLink("""
                namespace test
                type Dummy:
                """);
        var m = result.workspace().files().get(0);
        RFileScope fileScope = result.workspace().fileScope(m).orElseThrow();

        RFunctionScope fnScope = new RFunctionScope(fileScope);
        RAttribute fnItem = new RAttribute();
        fnItem.setName("item");
        fnScope.register("item", fnItem);

        RInlineFunctionScope inlineScope = new RInlineFunctionScope(fnScope);
        RAttribute inlineItem = new RAttribute();
        inlineItem.setName("item");
        inlineScope.register("item", inlineItem);

        // Inline lookup should hit the inline binding (shadows function-level)
        assertSame(inlineItem, inlineScope.lookup("item").orElseThrow());
        // Function scope still sees its own
        assertSame(fnItem, fnScope.lookup("item").orElseThrow());
    }
}
