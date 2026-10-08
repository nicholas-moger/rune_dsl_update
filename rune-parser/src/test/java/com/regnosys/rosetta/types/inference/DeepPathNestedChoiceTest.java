package com.regnosys.rosetta.types.inference;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.diagnostics.Severity;

/**
 * Anchor for the SF-1 collateral caught by the 25-cell seed (v3.1 C1 part 2):
 * a deep path ({@code ->>}) through a NESTED choice must keep resolving after
 * the plain arrow's choice arm refuses common attributes.
 *
 * <p>The deep walk's common-attribute rule composes through nested choices —
 * cdm 6.21+ nests {@code choice Index} inside {@code choice Observable}, and
 * {@code observable ->> identifier} reaches the attribute through BOTH hops
 * (upstream's deep feature set of a choice is the intersection over options,
 * an option that is itself a choice contributing its own set recursively).
 * That recursion used to happen BY ACCIDENT through {@code findAttribute}'s
 * choice arm; when SF-1 made the plain arrow refuse (probe P1's rule), the
 * nested hop silently died with it — SYMBOL_NOT_FOUND +21 per drr 7.x cell
 * and +1 per cdm 6.20.2+ cell on the seed, in the
 * {@code observable ->> identifier filter identifierType = ...} family. The
 * recursion now lives explicitly in {@code findCommonAttribute}
 * (cycle-guarded), where the DEEP semantics live, and this test locks the
 * whole shape: deep-through-nested-choice, then the filter lambda's item
 * features, then the bare enum RHS.
 */
class DeepPathNestedChoiceTest {

    @Test
    void deepPathThroughNestedChoice_resolvesFilterLambdaFeatures() {
        String src = String.join("\n",
                "namespace test",
                "enum IdTypeEnum:",
                "    Name",
                "    ISIN",
                "type AssetIdentifier:",
                "    identifier string (1..1)",
                "    identifierType IdTypeEnum (1..1)",
                "type Cash:",
                "    identifier AssetIdentifier (1..*)",
                "type CreditIndex:",
                "    identifier AssetIdentifier (1..*)",
                "type EquityIndex:",
                "    identifier AssetIdentifier (1..*)",
                "choice Index:",
                "    CreditIndex",
                "    EquityIndex",
                "choice Observable:",
                "    Cash",
                "    Index",
                "type Holder:",
                "    observable Observable (0..*)",
                "func F:",
                "    inputs:",
                "        h Holder (1..1)",
                "    output:",
                "        result string (0..*)",
                "    set result:",
                "        h -> observable ->> identifier",
                "            filter identifierType = Name",
                "            then extract identifier");
        RModel model = AstBuilder.buildFromString(src, "deep-nested-choice.rosetta");
        RLinkingResult result = RWorkspace.build(List.of(model));
        // The bare harness loads no builtin models, so `string` legitimately
        // reports TYPE_NOT_FOUND — everything else must resolve. In particular
        // no SYMBOL_NOT_FOUND may survive: identifierType and identifier are
        // item features of the deep result's element type, and Name is the
        // equality RHS bound from the typed LHS.
        List<String> unexpected = result.linkingDiagnostics().stream()
                .filter(d -> d.severity() == Severity.ERROR)
                .filter(d -> !"TYPE_NOT_FOUND".equals(String.valueOf(d.category())))
                .map(d -> d.category() + " '" + d.unresolvedName() + "' line " + d.range().startLine())
                .collect(Collectors.toList());
        assertEquals(List.of(), unexpected,
                "the deep path through the nested choice must leave no unresolved names");
    }
}
