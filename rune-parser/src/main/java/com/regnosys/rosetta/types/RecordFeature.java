package com.regnosys.rosetta.types;

import java.util.Objects;

/** A named feature of a record type (e.g., "day", "month", "year" for date). */
public record RecordFeature(String name) {
    public RecordFeature { Objects.requireNonNull(name); }
}
