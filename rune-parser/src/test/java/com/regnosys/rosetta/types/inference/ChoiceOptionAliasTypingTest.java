package com.regnosys.rosetta.types.inference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.types.RMetaAnnotatedType;

/**
 * Anchor for SF-8 (v3.1 C1 part 2): a TYPE ALIAS in a choice-option slot must
 * type as the alias's terminal type, not MISSING.
 *
 * <p>The grammar makes {@code choiceOption : typeCall} — the same production
 * {@code attribute} uses — so an alias is admissible in an option slot, and the
 * RELEASED 9.83.0 oracle confirms upstream ACCEPTS it (probe P10, adopted at
 * {@code resolution-conformance/p10-alias-as-choice-option.rosetta}: zero
 * errors; the option binds {@code RosettaTypeAlias} and the navigation binds
 * the {@code ChoiceOption}). The fork's
 * {@code ExpressionTypeComputer.typeOfChoiceOption} mapped only
 * {@code RDataType} and {@code RChoice}, returning MISSING for an alias — a
 * silent type loss NO conformance gate can see: the binding differential is
 * green (the option walk is name-based) and no error count moves (the fork's
 * assignment check skips MISSING). Only a direct inferred-type read pins it,
 * which is this test.
 *
 * <p>ZERO corpus carriers (80 choices, 173 aliases, no overlap across the 25
 * cells) — latent, not live, so the fix cannot move the seed, the ring or the
 * band, and those gates were re-measured anyway when it landed.
 */
class ChoiceOptionAliasTypingTest {

    private RWorkspace parseAndLink(String source) {
        RModel model = AstBuilder.buildFromString(source, "test.rosetta");
        RLinkingResult result = RWorkspace.build(List.of(model));
        return result.workspace();
    }

    /** The disguised {@code head -> valueName} nav matching the given names. */
    private REnumValueRef disguisedNav(RWorkspace ws, String head, String feature) {
        RModel model = ws.files().get(0);
        return AstWalker.findAll(model, REnumValueRef.class).stream()
                .filter(e -> head.equals(e.enumName()) && feature.equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "expected a disguised REnumValueRef " + head + " -> " + feature));
    }

    /** The P10 shape in miniature: the alias option's nav types as the alias's terminal type. */
    @Test
    void aliasOptionNav_typesAsAliasTerminalType() {
        String src = String.join("\n",
                "namespace test",
                "typeAlias AliasText:",
                "    string(pattern: \"[ -~]*\")",
                "choice AliasOnly:",
                "    AliasText",
                "func F:",
                "    inputs:",
                "        c AliasOnly (1..1)",
                "    output:",
                "        result string (0..1)",
                "    set result:",
                "        c -> AliasText");
        RWorkspace ws = parseAndLink(src);
        REnumValueRef nav = disguisedNav(ws, "c", "AliasText");
        RMetaAnnotatedType t = ws.getInferredType(nav);
        assertFalse(t.isMissing(),
                "c -> AliasText (AliasText a typeAlias option) must type as the alias's "
                        + "terminal type, not MISSING — upstream accepts the model (probe P10)");
        assertEquals("string", t.type().name(),
                "the alias option's nav types as the alias's terminal base type");
    }

    /** P10's Mixed control: an alias option must not poison a data-type sibling. */
    @Test
    void aliasSibling_doesNotPoisonDataOption() {
        String src = String.join("\n",
                "namespace test",
                "typeAlias AliasText:",
                "    string(pattern: \"[ -~]*\")",
                "type Plain:",
                "    shared string (1..1)",
                "choice Mixed:",
                "    Plain",
                "    AliasText",
                "func F:",
                "    inputs:",
                "        c Mixed (1..1)",
                "    output:",
                "        result string (0..1)",
                "    set result:",
                "        c -> Plain -> shared");
        RWorkspace ws = parseAndLink(src);
        REnumValueRef nav = disguisedNav(ws, "c", "Plain");
        RMetaAnnotatedType t = ws.getInferredType(nav);
        assertFalse(t.isMissing(), "the data-type sibling option still types");
        assertEquals("Plain", t.type().name(), "the sibling option types as its data type");
    }
}
