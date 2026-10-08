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
 * Anchors for the LAST three drr 7.x residue mechanisms (v3.1 C1 part 2) —
 * after the elsethen cascade-breaker, the per-cell survivors were exactly
 * three named fork gaps (plus the two upstream-own IMPORT refusals), each a
 * distinct mechanism, each pinned here in miniature:
 *
 * <ul>
 *   <li><b>underliers</b> ({@code base-trade-basket-func.rosetta:38}) — the
 *       head of a disguised {@code <alias> -> <Option>} nav is an RShortcut,
 *       and {@code TypeDirectedResolver.typeOfHead} returns {@code null} for
 *       an alias BY DESIGN (its type is its body's, which only the engine's
 *       memo knows) — so the R9 arm declined and the phase-A ENUM_NOT_FOUND
 *       stood. The engine now supplies the alias body's inferred type at the
 *       R9 seat.</li>
 *   <li><b>Debt</b> ({@code base-qualification-product-func.rosetta:582}) —
 *       {@code Security -> instrumentType = Debt} inside an extract over a
 *       choice item: the R9 arm binds the ATTRIBUTE into the authority slot,
 *       but the REnumValueRef type ladder's authority read (5/n) handled only
 *       ChoiceOption — the attribute leg typed MISSING, the equality LHS had
 *       no type, and the bare RHS could not seed.</li>
 *   <li><b>ADHO</b> ({@code regulation-common-trade-quantity-func.rosetta:371})
 *       — {@code Conv(x) then default ADHO}: the default's LEFT is the elided
 *       pipe subject, so the expected-type walker's default arm read
 *       {@code enumOfBranch(null)} and seeded nothing; it now reads the
 *       enclosing item type for an elided subject, exactly as the
 *       equals/contains/disjoint arms already did (3/n).</li>
 * </ul>
 */
class DrrResidueResolutionTest {

    private List<String> unresolvedNames(String src, String file) {
        RModel model = AstBuilder.buildFromString(src, file);
        RLinkingResult result = RWorkspace.build(List.of(model));
        // The bare harness loads no builtin models, so TYPE_NOT_FOUND for
        // `string` etc. is an artifact; every resolution category must be clean.
        return result.linkingDiagnostics().stream()
                .filter(d -> d.severity() == Severity.ERROR)
                .filter(d -> !"TYPE_NOT_FOUND".equals(String.valueOf(d.category())))
                .map(d -> d.category() + " '" + d.unresolvedName() + "' line " + d.range().startLine())
                .collect(Collectors.toList());
    }

    /** underliers: the disguised nav whose head is an ALIAS typed as a choice. */
    @Test
    void aliasHeadOptionNav_resolves() {
        String src = String.join("\n",
                "namespace test",
                "type Basket:",
                "    identifier string (1..1)",
                "choice Underlier:",
                "    Basket",
                "type Holder:",
                "    underlier Underlier (0..*)",
                "func F:",
                "    inputs:",
                "        h Holder (1..1)",
                "    output:",
                "        result string (0..*)",
                "    alias underliers: h -> underlier",
                "    set result:",
                "        underliers -> Basket -> identifier");
        assertEquals(List.of(), unresolvedNames(src, "alias-head.rosetta"),
                "the alias-headed nav must resolve: the head lexically, the option on "
                        + "the alias body's type, the attribute on the option's type");
    }

    /** Debt: the equality RHS behind an option-headed LHS bound in the AUTHORITY slot. */
    @Test
    void equalityRhsBehindAuthorityAttributeLhs_resolves() {
        String src = String.join("\n",
                "namespace test",
                "enum InstrumentTypeEnum:",
                "    Debt",
                "    Equity",
                "type Security:",
                "    instrumentType InstrumentTypeEnum (1..1)",
                "type Loan:",
                "    id string (1..1)",
                "choice Instrument:",
                "    Security",
                "    Loan",
                "type Holder:",
                "    instrument Instrument (0..*)",
                "func F:",
                "    inputs:",
                "        h Holder (1..1)",
                "    output:",
                "        result boolean (0..*)",
                "    set result:",
                "        h -> instrument",
                "            extract (Loan exists or Security -> instrumentType = Debt)");
        assertEquals(List.of(), unresolvedNames(src, "authority-attr-lhs.rosetta"),
                "the option-headed LHS types through the authority attribute, so the "
                        + "bare RHS enum value seeds from it");
    }

    /** ADHO: `call then default BARE` — the default's LEFT is the elided pipe subject. */
    @Test
    void defaultRhsBehindElidedPipeSubject_resolves() {
        String src = String.join("\n",
                "namespace test",
                "enum FreqEnum:",
                "    ADHO",
                "    WEEK",
                "type Quantity:",
                "    period FreqEnum (0..1)",
                "func Conv:",
                "    inputs:",
                "        x string (0..1)",
                "    output:",
                "        out FreqEnum (0..1)",
                "    set out:",
                "        WEEK",
                "func F:",
                "    inputs:",
                "        s string (0..1)",
                "    output:",
                "        q Quantity (0..1)",
                "    set q:",
                "        Quantity {",
                "            period: Conv(s)",
                "                    then default ADHO",
                "        }");
        assertEquals(List.of(), unresolvedNames(src, "default-elided.rosetta"),
                "the default RHS seeds from the elided pipe subject's type (the call's "
                        + "output enum)");
    }
}
