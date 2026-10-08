package com.regnosys.rosetta.types;

import java.util.List;
import java.util.Objects;

/**
 * Universal type inference result: an {@link RType} plus optional metadata
 * attributes. Every inference result is wrapped in this type, even when
 * metadata is empty (D7).
 *
 * <p>Xtext equivalent: {@code RMetaAnnotatedType}.
 *
 * <p>Spec: D7 in {@code docs/specs/2026-04-08-m4-type-system-design.md}.
 */
public final class RMetaAnnotatedType {

    /** Sentinel for missing/unknown types. */
    public static final RMetaAnnotatedType MISSING =
        new RMetaAnnotatedType(RMissingType.INSTANCE, List.of());

    private final RType type;
    private final List<String> metaAttributes;

    private RMetaAnnotatedType(RType type, List<String> metaAttributes) {
        this.type = Objects.requireNonNull(type);
        this.metaAttributes = List.copyOf(metaAttributes);
    }

    public static RMetaAnnotatedType withNoMeta(RType type) {
        return new RMetaAnnotatedType(type, List.of());
    }

    public static RMetaAnnotatedType withMeta(RType type, List<String> meta) {
        return new RMetaAnnotatedType(type, meta);
    }

    public RType type() { return type; }
    public List<String> metaAttributes() { return metaAttributes; }
    public boolean hasMeta() { return !metaAttributes.isEmpty(); }
    public boolean isMissing() { return type instanceof RMissingType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RMetaAnnotatedType that)) return false;
        return type.equals(that.type) && metaAttributes.equals(that.metaAttributes);
    }

    @Override public int hashCode() { return Objects.hash(type, metaAttributes); }

    @Override
    public String toString() {
        return hasMeta() ? type + " meta " + metaAttributes : type.toString();
    }
}
