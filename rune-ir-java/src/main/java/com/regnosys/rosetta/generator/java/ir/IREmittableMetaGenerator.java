package com.regnosys.rosetta.generator.java.ir;

import java.util.Map;

/**
 * Marker for the lab-side, IR-routed variant of the <em>workspace-wide</em>
 * metafield generator. Distinct from {@link IREmittableGenerator} (which is the
 * per-model declaration seam) because {@code MetaFieldGenerator} runs once over
 * the whole workspace — deduplicating {@code FieldWithMeta}/{@code ReferenceWithMeta}
 * wrappers — rather than per {@code RModel}.
 *
 * <p>When the {@link IRFlag} is enabled, {@code JavaCodeGenerator} routes the
 * metafield generator through {@link #generateAsIR(Map)} instead of
 * {@code generate(Map)} (decision L-002), leaving Path-1 untouched when off.
 *
 * <p>Lab-authored Phase-1 (not present upstream).
 */
public interface IREmittableMetaGenerator {

    /**
     * IR-routed counterpart of {@code MetaFieldGenerator#generate(Map)}: writes
     * the generated metafield-wrapper files into {@code output}.
     */
    void generateAsIR(Map<String, String> output);
}
