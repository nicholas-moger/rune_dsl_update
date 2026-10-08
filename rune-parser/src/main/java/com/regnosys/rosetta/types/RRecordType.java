package com.regnosys.rosetta.types;

import java.util.List;
import java.util.Objects;

/**
 * Record type: date, dateTime, zonedDateTime. Consolidates Xtext's 3
 * separate classes into one with a {@link RecordKind} discriminator.
 */
public final class RRecordType implements RType {

    public static final RRecordType DATE = new RRecordType(RecordKind.DATE, "date",
        List.of(new RecordFeature("day"), new RecordFeature("month"), new RecordFeature("year")));

    public static final RRecordType DATE_TIME = new RRecordType(RecordKind.DATE_TIME, "dateTime",
        List.of(new RecordFeature("date"), new RecordFeature("time")));

    public static final RRecordType ZONED_DATE_TIME = new RRecordType(RecordKind.ZONED_DATE_TIME, "zonedDateTime",
        List.of(new RecordFeature("date"), new RecordFeature("time"), new RecordFeature("timezone")));

    private final RecordKind kind;
    private final String name;
    private final List<RecordFeature> features;

    private RRecordType(RecordKind kind, String name, List<RecordFeature> features) {
        this.kind = Objects.requireNonNull(kind);
        this.name = Objects.requireNonNull(name);
        this.features = List.copyOf(features);
    }

    @Override public String name() { return name; }
    @Override public boolean hasNaturalOrder() { return true; }
    public RecordKind kind() { return kind; }
    public List<RecordFeature> features() { return features; }

    @Override public boolean equals(Object o) { return this == o; }
    @Override public int hashCode() { return System.identityHashCode(this); }
    @Override public String toString() { return name; }
}
