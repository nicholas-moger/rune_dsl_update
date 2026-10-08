package com.regnosys.rosetta.generator.java.template;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImportCollectorTest {

    @Test void adds_and_sorts_imports() {
        var collector = new ImportCollector("com.example");
        collector.addImport("java.util.List");
        collector.addImport("com.rosetta.model.lib.RosettaModelObject");
        collector.addImport("java.util.ArrayList");

        var imports = collector.getImports();
        assertEquals(3, imports.size());
        assertEquals("com.rosetta.model.lib.RosettaModelObject", imports.get(0));
        assertEquals("java.util.ArrayList", imports.get(1));
        assertEquals("java.util.List", imports.get(2));
    }

    @Test void skips_same_package() {
        var collector = new ImportCollector("com.example");
        collector.addImport("com.example.Foo");
        assertTrue(collector.getImports().isEmpty());
    }

    @Test void skips_java_lang() {
        var collector = new ImportCollector("com.example");
        collector.addImport("java.lang.String");
        assertTrue(collector.getImports().isEmpty());
    }

    @Test void static_imports_sorted_separately() {
        var collector = new ImportCollector("com.example");
        collector.addStaticImport("java.util.Collections.emptyList");
        collector.addStaticImport("com.google.common.base.Strings.isNullOrEmpty");

        var statics = collector.getStaticImports();
        assertEquals(2, statics.size());
        assertEquals("com.google.common.base.Strings.isNullOrEmpty", statics.get(0));
        assertEquals("java.util.Collections.emptyList", statics.get(1));
    }

    @Test void deduplicates() {
        var collector = new ImportCollector("com.example");
        collector.addImport("java.util.List");
        collector.addImport("java.util.List");
        assertEquals(1, collector.getImports().size());
    }

    @Test void importType_returns_simple_name_and_records_import() {
        var collector = new ImportCollector("com.example");
        String simple = collector.importType("java.util.List");
        assertEquals("List", simple);
        assertEquals(1, collector.getImports().size());
        assertEquals("java.util.List", collector.getImports().get(0));
    }

    @Test void importType_skips_same_package_returns_simple() {
        var collector = new ImportCollector("com.example");
        String simple = collector.importType("com.example.Foo");
        assertEquals("Foo", simple);
        assertTrue(collector.getImports().isEmpty());
    }

    @Test void importStatic_returns_simple_name_and_records() {
        var collector = new ImportCollector("com.example");
        String simple = collector.importStatic("java.util.Collections.emptyList");
        assertEquals("emptyList", simple);
        assertEquals(1, collector.getStaticImports().size());
        assertEquals("java.util.Collections.emptyList", collector.getStaticImports().get(0));
    }

    @Test void null_package_throws() {
        assertThrows(NullPointerException.class, () -> new ImportCollector(null));
    }

    @Test void has_imports_flags() {
        var collector = new ImportCollector("com.example");
        assertFalse(collector.hasImports());
        assertFalse(collector.hasStaticImports());

        collector.addImport("java.util.List");
        assertTrue(collector.hasImports());
        assertFalse(collector.hasStaticImports());

        collector.addStaticImport("java.util.Collections.emptyList");
        assertTrue(collector.hasStaticImports());
    }
}
