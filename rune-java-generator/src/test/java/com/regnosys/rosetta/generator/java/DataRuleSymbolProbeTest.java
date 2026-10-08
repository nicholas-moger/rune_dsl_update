package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

/**
 * Wave-D scratch probe (opt-in, diagnostic only): what does the linker bind a
 * bare attribute reference inside a DATA-TYPE condition to? Drives the
 * condition-instance branch design in ReferenceHandler (drift class DR-A of the
 * #409 audit-1).
 */
@EnabledIfSystemProperty(named = "wave.audit", matches = "true",
        disabledReason = "wave-D scratch diagnostic — opt-in via -Dwave.audit=true")
class DataRuleSymbolProbeTest {

    @Test
    void probeConditionSymbolResolution() {
        String source = """
                namespace test.probe
                version "1.0.0"

                type Inner:
                    leafValue string (0..1)

                type Thing:
                    adjustedDate date (0..1)
                    unadjustedDate date (0..1)
                    taxonomyValue Inner (0..1)
                    val number (0..1)

                    condition AdjustedDate:
                        if adjustedDate is absent
                        then unadjustedDate exists

                    condition Chained:
                        if taxonomyValue -> leafValue exists
                        then unadjustedDate exists

                    condition NegLit:
                        val > -1 and val < 1
                """;
        probeModel(source);
        String caretSource = """
                namespace test.probe2
                version "1.0.0"

                type Caret:
                    ^type string (0..1)
                    other string (0..1)

                    condition Choice:
                        optional choice ^type, other
                """;
        probeModel(caretSource);
    }

    private void probeModel(String source) {
        RModel model = AstBuilder.buildFromString(source, "probe.rosetta");
        RWorkspace.build(List.of(model));
        for (var root : model.rootElements()) {
            if (!(root instanceof RDataType dataType) || dataType.conditions().isEmpty()) {
                continue;
            }
            for (RCondition cond : dataType.conditions()) {
                System.out.println("=== PROBE condition " + cond.name().orElse("?")
                        + " expressionText: [" + cond.expressionText().orElse("<ABSENT>") + "]");
                walk(cond.expression(), 0);
            }
        }
    }

    private void walk(RNode node, int depth) {
        if (node == null) {
            return;
        }
        StringBuilder line = new StringBuilder("=== PROBE ").append("  ".repeat(depth))
                .append(node.getClass().getSimpleName());
        if (node instanceof RSymbolReference ref) {
            line.append(" name=").append(ref.name())
                .append(" symbol=").append(ref.symbol()
                        .map(s -> s.getClass().getSimpleName() + ":" + s).orElse("EMPTY"));
        }
        System.out.println(line);
        for (RNode child : node.children()) {
            walk(child, depth + 1);
        }
    }
}
