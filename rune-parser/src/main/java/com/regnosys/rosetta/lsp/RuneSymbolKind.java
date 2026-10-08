package com.regnosys.rosetta.lsp;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.RBasicType;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RRecordType;
import com.regnosys.rosetta.ast.types.RTypeAlias;

import java.util.Objects;

/**
 * LSP wire-format SymbolKind constants + {@link #forNode(RNode)} dispatch.
 *
 * <p>Constant values match the LSP 3.17 SymbolKind enum
 * (<a href="https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_documentSymbol">spec</a>).
 * Raw {@code int} matches the wire encoding — consumers serialize directly.
 *
 * <p>{@link #forNode} maps a curated set of nine named-symbol RNode subclasses:
 * {@link RDataType} → CLASS, {@link REnumeration} → ENUM, {@link RChoice} →
 * INTERFACE, {@link RFunction} → FUNCTION, {@link REnumValue} → ENUM_MEMBER,
 * {@link RAttribute} → FIELD, {@link RBasicType} → CLASS, {@link RRecordType}
 * → STRUCT, {@link RTypeAlias} → CLASS. This is the D8 stable mapping table
 * for P1.4.1c.
 *
 * <p>Other named RNode subclasses (e.g. {@code RLibraryFunction}, {@code RMetaType},
 * {@code RRule}, {@code RTypeParameter}, {@code RShortcut}) are deliberately
 * unmapped at this stage — pending an LSP-scope extension PR that decides their
 * canonical SymbolKind. Calling {@link #forNode} on any of these throws
 * {@link IllegalArgumentException} (fail-loud catches premature LSP-surface
 * use). Structural-only subclasses (expressions, options, etc.) likewise throw.
 *
 * <p>Spec: §6.6 in {@code docs/superpowers/specs/2026-04-26-p1.4.1-internal-hardening-design.md}.
 *
 * <p>Manifest entry: U007.
 */
public final class RuneSymbolKind {

    public static final int FILE = 1;
    public static final int MODULE = 2;
    public static final int NAMESPACE = 3;
    public static final int PACKAGE = 4;
    public static final int CLASS = 5;
    public static final int METHOD = 6;
    public static final int PROPERTY = 7;
    public static final int FIELD = 8;
    public static final int CONSTRUCTOR = 9;
    public static final int ENUM = 10;
    public static final int INTERFACE = 11;
    public static final int FUNCTION = 12;
    public static final int VARIABLE = 13;
    public static final int CONSTANT = 14;
    public static final int STRING = 15;
    public static final int NUMBER = 16;
    public static final int BOOLEAN = 17;
    public static final int ARRAY = 18;
    public static final int OBJECT = 19;
    public static final int KEY = 20;
    public static final int NULL = 21;
    public static final int ENUM_MEMBER = 22;
    public static final int STRUCT = 23;
    public static final int EVENT = 24;
    public static final int OPERATOR = 25;
    public static final int TYPE_PARAMETER = 26;

    private RuneSymbolKind() {}

    public static int forNode(RNode node) {
        Objects.requireNonNull(node, "node");
        return switch (node) {
            case RDataType d -> CLASS;
            case REnumeration e -> ENUM;
            case RChoice c -> INTERFACE;
            case RFunction f -> FUNCTION;
            case REnumValue v -> ENUM_MEMBER;
            case RAttribute a -> FIELD;
            case RBasicType b -> CLASS;
            case RRecordType r -> STRUCT;
            case RTypeAlias t -> CLASS;
            default -> throw new IllegalArgumentException(
                "No SymbolKind mapping for " + node.getClass().getName()
                + "; structural-only RNode subclass or missing mapping");
        };
    }
}
