package com.regnosys.rosetta.symbols;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.expressions.supporting.RClosureParameter;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.supporting.RParameter;
import com.regnosys.rosetta.ast.supporting.RTypeParameter;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RLibraryFunction;

/**
 * R6 of {@code docs/specs/2026-08-15-upstream-resolution-spec.md} — WHAT
 * UPSTREAM'S SYMBOL SCOPE CAN CONTAIN, in one place.
 *
 * <p>Upstream filters the global scope by the cross-reference's declared type.
 * At a symbol seat that type is {@code RosettaSymbol}, whose implementors are:
 * {@code Attribute} (hence {@code ChoiceOption}), {@code ShortcutDeclaration},
 * {@code Function}, {@code RosettaRule}, {@code RosettaExternalFunction},
 * {@code RosettaEnumeration}, {@code RosettaEnumValue}, {@code ClosureParameter},
 * {@code TypeParameter}, {@code RosettaParameter} and {@code RosettaMetaType}.
 *
 * <p><b>A {@code Data} is NOT one</b> — and since {@code Choice extends Data},
 * neither is a choice. So a top-level TYPE never competes for a bare name, and
 * an implicit-item feature of the same name wins by DEFAULT rather than by
 * precedence. That single fact is why the spec's two pinned precedence cases
 * give opposite answers: a global {@code enum Colour} beats a same-named item
 * attribute (an enumeration IS a symbol), while a global {@code choice Asset}
 * loses to a same-named item option (a choice is not).
 *
 * <p><b>{@code RosettaMetaType} is deliberately excluded here.</b> It implements
 * {@code RosettaSymbol}, but upstream filters it out at the ROOT of the symbol
 * parent chain, so a meta name is reachable as a FEATURE (§ 4 of the spec) and
 * never as a bare symbol. Every caller of this class is asking the bare-symbol
 * question, so the filter is folded in rather than left to each of them.
 *
 * <p><b>Why a whitelist.</b> The first implementation of this rule was a
 * blacklist of type-like classes, which silently admitted every node kind
 * nobody had thought of. Upstream's rule is an {@code instanceof} test against
 * an interface — an allow-list by construction — so this mirrors it. A fork
 * node class that belongs in the scope must be ADDED here, which is a decision
 * someone makes rather than a default they inherit.
 */
public final class UpstreamSymbolKinds {

    private UpstreamSymbolKinds() {}

    /**
     * True when {@code node} is something upstream's SYMBOL scope could contain.
     *
     * <p>{@link RClosureParameter} IS upstream's {@code ClosureParameter} (v3.2
     * seat 8 — the parameter node landed; the resolution spec's § 7 gap closed).
     * {@link RInlineFunction} stays admitted for the name-only parameter shape
     * that still binds to its declaring lambda: the synthetic {@code item} of an
     * implicit lambda, and lambdas hand-built through {@code paramNames()}.
     */
    public static boolean isUpstreamSymbol(RNode node) {
        return node instanceof RAttribute
                || node instanceof RChoiceOption
                || node instanceof RShortcut
                || node instanceof RFunction
                || node instanceof RRule
                || node instanceof RLibraryFunction
                || node instanceof REnumeration
                || node instanceof REnumValue
                || node instanceof RClosureParameter
                || node instanceof RInlineFunction
                || node instanceof RTypeParameter
                || node instanceof RParameter;
    }
}
