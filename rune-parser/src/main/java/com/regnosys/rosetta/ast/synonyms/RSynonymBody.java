package com.regnosys.rosetta.ast.synonyms;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.SynonymBodyKind;
import com.regnosys.rosetta.ast.mapping.RMapping;
import com.regnosys.rosetta.ast.mapping.RMappingSetTo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Synonym body node, corresponding to the {@code rosettaSynonymBody} grammar rule.
 *
 * <p>Contains 5 alternative forms determined by {@link SynonymBodyKind}:
 * <ul>
 *   <li>{@code VALUE} — values with optional mapping and meta fields</li>
 *   <li>{@code HINT} — hint path strings</li>
 *   <li>{@code MERGE} — merge synonym value</li>
 *   <li>{@code SET_TO} — set-to mapping expression</li>
 *   <li>{@code META_ONLY} — meta-only with meta fields</li>
 * </ul>
 *
 * <p>Trailing modifiers (dateFormat, patternMatch, patternReplace, removeHtml, mapper)
 * are common across alternatives.
 */
public class RSynonymBody extends RNode {

    private SynonymBodyKind kind;

    // -- VALUE alternative fields
    private final List<RSynonymValue> values = new ArrayList<>();
    private RMapping mapping;
    private final List<String> metaFields = new ArrayList<>();

    // -- HINT alternative fields
    private final List<String> hints = new ArrayList<>();

    // -- MERGE alternative fields
    private RMergeSynonymValue merge;

    // -- SET_TO alternative fields
    private RMappingSetTo setTo;

    // -- Trailing modifiers (common)
    private String dateFormat;
    private String patternMatch;
    private String patternReplace;
    private boolean removeHtml;
    private String mapper;

    // -- kind -----------------------------------------------------------------

    public SynonymBodyKind kind() {
        return kind;
    }

    public void setKind(SynonymBodyKind kind) {
        checkMutable();
        this.kind = kind;
    }

    // -- values (VALUE alternative) -------------------------------------------

    public List<RSynonymValue> values() {
        return values;
    }

    // -- mapping (VALUE alternative) ------------------------------------------

    public Optional<RMapping> mapping() {
        return Optional.ofNullable(mapping);
    }

    public void setMapping(RMapping mapping) {
        checkMutable();
        this.mapping = mapping;
    }

    // -- metaFields (VALUE / META_ONLY alternative) ---------------------------

    public List<String> metaFields() {
        return metaFields;
    }

    // -- hints (HINT alternative) ---------------------------------------------

    public List<String> hints() {
        return hints;
    }

    // -- merge (MERGE alternative) --------------------------------------------

    public Optional<RMergeSynonymValue> merge() {
        return Optional.ofNullable(merge);
    }

    public void setMerge(RMergeSynonymValue merge) {
        checkMutable();
        this.merge = merge;
    }

    // -- setTo (SET_TO alternative) -------------------------------------------

    public Optional<RMappingSetTo> setTo() {
        return Optional.ofNullable(setTo);
    }

    public void setSetTo(RMappingSetTo setTo) {
        checkMutable();
        this.setTo = setTo;
    }

    // -- dateFormat (trailing modifier) ---------------------------------------

    public Optional<String> dateFormat() {
        return Optional.ofNullable(dateFormat);
    }

    public void setDateFormat(String dateFormat) {
        checkMutable();
        this.dateFormat = dateFormat;
    }

    // -- patternMatch (trailing modifier) -------------------------------------

    public Optional<String> patternMatch() {
        return Optional.ofNullable(patternMatch);
    }

    public void setPatternMatch(String patternMatch) {
        checkMutable();
        this.patternMatch = patternMatch;
    }

    // -- patternReplace (trailing modifier) -----------------------------------

    public Optional<String> patternReplace() {
        return Optional.ofNullable(patternReplace);
    }

    public void setPatternReplace(String patternReplace) {
        checkMutable();
        this.patternReplace = patternReplace;
    }

    // -- removeHtml (trailing modifier) ---------------------------------------

    public boolean isRemoveHtml() {
        return removeHtml;
    }

    public void setRemoveHtml(boolean removeHtml) {
        checkMutable();
        this.removeHtml = removeHtml;
    }

    // -- mapper (trailing modifier) -------------------------------------------

    public Optional<String> mapper() {
        return Optional.ofNullable(mapper);
    }

    public void setMapper(String mapper) {
        checkMutable();
        this.mapper = mapper;
    }

    // -- children (for traversal) ---------------------------------------------

    @Override
    public List<? extends RNode> children() {
        List<RNode> result = new ArrayList<>();
        result.addAll(values);
        if (mapping != null) {
            result.add(mapping);
        }
        if (merge != null) {
            result.add(merge);
        }
        if (setTo != null) {
            result.add(setTo);
        }
        return List.copyOf(result);
    }
}
