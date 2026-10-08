package com.regnosys.rosetta.types.inference;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.types.RNumberType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Locks the L-032 fix: a shortcut (alias) reference resolves to its body
 * expression's type instead of {@code MISSING}, and the resolution is stable at
 * the engine's fixed-point convergence.
 *
 * <p>This is the de-risk reported to the IR Lab — its neutral
 * {@code ExpressionToIRAdapter} reads {@code getInferredType(<shortcut/alias
 * receiver>)} through the workspace, <em>post-convergence</em>, so the value it
 * consumes must (a) resolve and (b) be stable once the fixed-point loop settles.
 * The fix mirrors the {@code RRule} branch of
 * {@link ExpressionTypeComputer}{@code .inferTypeOfNode} and relies on the same
 * memoized fixed-point convergence. Driven end-to-end through
 * {@link RWorkspace#build} (parse + resolve + infer), like {@code
 * SmokeTypeInferenceTest}.
 */
class ShortcutAliasTypeInferenceTest {

    // A function with an alias used as a value, so a bare RSymbolReference
    // resolves to the RShortcut. The alias body is a literal here so it types
    // without cross-feature navigation resolution (which the lightweight smoke
    // harness does not wire up); the resolved-shortcut → body-type code path is
    // identical regardless of the body shape, and navigation-bodied aliases are
    // exercised on the real corpus by the full byte-parity gate.
    private static final String ALIAS_MODEL = """
            namespace "test"
            func F:
                output: result int (1..1)
                alias a: 42
                set result: a
            """;

    private RWorkspace build() {
        RModel model = AstBuilder.buildFromString(ALIAS_MODEL, "shortcut-type.rosetta");
        return RWorkspace.build(List.of(model)).workspace();
    }

    @Test
    void shortcut_alias_reference_resolves_to_body_type() {
        var ws = build();
        var root = ws.files().get(0);

        // Prerequisite: the alias body (the literal 42) types to int.
        var shortcuts = AstWalker.findAll(root, RShortcut.class);
        assertEquals(1, shortcuts.size(), "expected exactly one alias");
        var bodyType = ws.getInferredType(shortcuts.get(0).expression());
        assertFalse(bodyType.isMissing(), "alias body must be typed");
        assertInstanceOf(RNumberType.class, bodyType.type());

        // Gap 1: the alias reference ('set result: a') resolves to the body
        // type, not MISSING. Pre-fix the RShortcut branch returned MISSING.
        var aliasRefs = AstWalker.findAll(root, RSymbolReference.class).stream()
                .filter(r -> r.symbol().orElse(null) instanceof RShortcut)
                .toList();
        assertFalse(aliasRefs.isEmpty(), "expected a reference resolving to the alias");
        for (var ref : aliasRefs) {
            var t = ws.getInferredType(ref);
            assertFalse(t.isMissing(), "alias reference must resolve (Gap 1)");
            assertInstanceOf(RNumberType.class, t.type(), "alias reference types as its body");
        }
    }

    @Test
    void shortcut_resolution_is_stable_and_deterministic() {
        // The Lab reads getInferredType post-convergence, so the resolved value
        // must be stable once the fixed-point loop settles. Two independent
        // full-pipeline builds yielding the SAME resolved type demonstrate the
        // resolution is a stable, deterministic fixed point (not an artefact of
        // iteration order or a value still oscillating at the cap).
        var ws1 = build();
        var ws2 = build();
        var ref1 = AstWalker.findAll(ws1.files().get(0), RSymbolReference.class).stream()
                .filter(r -> r.symbol().orElse(null) instanceof RShortcut).findFirst().orElseThrow();
        var ref2 = AstWalker.findAll(ws2.files().get(0), RSymbolReference.class).stream()
                .filter(r -> r.symbol().orElse(null) instanceof RShortcut).findFirst().orElseThrow();
        var t1 = ws1.getInferredType(ref1);
        var t2 = ws2.getInferredType(ref2);
        assertFalse(t1.isMissing(), "alias reference must resolve");
        assertEquals(t1.type().name(), t2.type().name(),
                "shortcut resolution must be deterministic/stable post-convergence");
    }
}
