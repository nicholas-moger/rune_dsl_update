package com.regnosys.rosetta.generator.java.template.model;

import java.util.List;

/**
 * Template model for a single enum constant value.
 *
 * <p>Covers all corpus variations:
 * <ul>
 *   <li>CDM: display names, definitions, synonyms, doc references</li>
 *   <li>ISO-20022: no display names (453 bare enums)</li>
 *   <li>rune-fpml: some display names, no synonyms</li>
 *   <li>DRR: display names, no synonyms</li>
 * </ul>
 *
 * <p>The {@code isLast} flag controls comma/newline separator placement.
 * Note: this is a view-layer concern; M9 (Python) may use ST4's native
 * separator handling instead.
 *
 * <p>v3.2 seat 9 (D46, F1 — enum-unicode-display): the display name is carried TWICE because the released
 * 9.83.0 plugin renders it under TWO laws — {@link #getDisplayName()} is the RAW model text, spliced into the
 * {@code @RosettaEnumValue(displayName = "…")} annotation exactly as written (non-ASCII kept, and even a
 * quote / a backslash / a tab kept — upstream's own non-compiling emission for those, the oracle group
 * {@code enum-unicode-display-edge}'s {@code EscapeEnum}); {@link #getDisplayNameLiteral()} is the
 * Java-ESCAPED string the constructor argument reads ({@code µ} for {@code µ}). Before the seat the
 * generator escaped once and the template read that one string at both seats (the chaos a5uni family,
 * 22 declared ENUM rows: every non-ASCII display name escaped inside the annotation).
 */
public class EnumValueModel {

    private final String javaName;
    private final String rosettaName;
    private final String displayName;
    private final String displayNameLiteral;
    private final String definition;
    /** Full javadoc block (with doc references), or null. Rendered by ModelGeneratorUtil.javadoc(). */
    private final String javadoc;
    private final List<SynonymModel> synonyms;
    private final boolean isLast;

    public EnumValueModel(String javaName, String rosettaName, String displayName, String displayNameLiteral,
                           String definition, String javadoc,
                           List<SynonymModel> synonyms, boolean isLast) {
        this.javaName = javaName;
        this.rosettaName = rosettaName;
        // v3.2 seat 9 round 1 (cq NIT-5): the two display names differ by escaping alone - a positional swap would
        // compile and pass the getters; the constructor refuses one (the byte bar would catch it later, this earlier).
        // round 2 (cq NIT-5): the two names are a PAIR - one null without the other is the swap the check exists for
        if ((displayName == null) != (displayNameLiteral == null)) {
            throw new IllegalArgumentException("displayName and displayNameLiteral must both be present or both absent: "
                    + displayName + " / " + displayNameLiteral);
        }
        if (displayName != null
                && !com.regnosys.rosetta.generator.java.template.JavaStringUtil.escapeJava(displayName).equals(displayNameLiteral)) {
            throw new IllegalArgumentException("displayNameLiteral must be the Java escape of displayName: "
                    + displayName + " / " + displayNameLiteral);
        }
        this.displayName = displayName;
        this.displayNameLiteral = displayNameLiteral;
        this.definition = definition;
        this.javadoc = javadoc;
        this.synonyms = synonyms != null ? List.copyOf(synonyms) : List.of();
        this.isLast = isLast;
    }

    public String getJavaName() { return javaName; }
    public String getRosettaName() { return rosettaName; }
    /** The RAW display name — the annotation seat (D46). Null when the value declares none. */
    public String getDisplayName() { return displayName; }
    /** The Java-escaped display name — the constructor-argument seat (D46). Null when the value declares none. */
    public String getDisplayNameLiteral() { return displayNameLiteral; }
    public String getDefinition() { return definition; }
    /** Full javadoc block with doc references, or null if no definition/docRefs. */
    public String getJavadoc() { return javadoc; }
    public List<SynonymModel> getSynonyms() { return synonyms; }
    public boolean getIsLast() { return isLast; }
}
