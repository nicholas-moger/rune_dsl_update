package com.regnosys.rosetta.harness.matrix;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MatrixCoordinateTest {

    private static final Version V6160 = Version.parse("6.16.0");
    private static final Version V6100 = Version.parse("6.10.0");
    private static final Version ISO_1_36 = Version.parse("1.36.0");

    @Test
    void display_name_is_deterministic_and_slash_separated() {
        MatrixCoordinate mc = new MatrixCoordinate(
                Corpus.CDM, "base-model", V6160, ElementKind.TYPE,
                Path.of("test-corpus/cdm/cdm-6.16.0/base-model/Trade.rosetta"));
        assertEquals("cdm/base-model/6.16.0/TYPE:Trade.rosetta", mc.displayName());
    }

    @Test
    void display_name_with_no_project_uses_dash() {
        // ISO-20022 uses 1.x versions per test-corpus/CATALOGUE.md — mirror that.
        MatrixCoordinate mc = new MatrixCoordinate(
                Corpus.ISO20022, "", ISO_1_36, ElementKind.REPORT,
                Path.of("test-corpus/iso20022/iso20022-1.36.0/Report.rosetta"));
        assertEquals("iso20022/-/1.36.0/REPORT:Report.rosetta", mc.displayName());
    }

    @Test
    void equality_is_component_wise() {
        Path p = Path.of("a.rosetta");
        MatrixCoordinate a = new MatrixCoordinate(Corpus.CDM, "x", V6160, ElementKind.TYPE, p);
        MatrixCoordinate b = new MatrixCoordinate(Corpus.CDM, "x", V6160, ElementKind.TYPE, p);
        MatrixCoordinate c = new MatrixCoordinate(Corpus.DRR, "x", V6160, ElementKind.TYPE, p);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void compare_orders_by_corpus_then_project_then_version_then_kind() {
        // CDM has versions 6.10.0 and 6.16.0 per test-corpus/CATALOGUE.md;
        // use two real CDM versions for the ordering test.
        Path p = Path.of("a.rosetta");
        MatrixCoordinate cdmBaseOlder = new MatrixCoordinate(Corpus.CDM, "base", V6100, ElementKind.TYPE, p);
        MatrixCoordinate cdmBaseNewer = new MatrixCoordinate(Corpus.CDM, "base", V6160, ElementKind.TYPE, p);
        MatrixCoordinate cdmProduct = new MatrixCoordinate(Corpus.CDM, "product", V6100, ElementKind.TYPE, p);
        MatrixCoordinate drr = new MatrixCoordinate(Corpus.DRR, "base", V6100, ElementKind.TYPE, p);

        assertTrue(cdmBaseOlder.compareTo(cdmBaseNewer) < 0, "older version sorts first");
        assertTrue(cdmBaseOlder.compareTo(cdmProduct) < 0, "project orders alphabetically");
        assertTrue(cdmProduct.compareTo(drr) < 0, "corpus orders enum-declaration order");
    }

    @Test
    void compare_distinguishes_by_kind_when_other_axes_equal() {
        Path p = Path.of("a.rosetta");
        MatrixCoordinate type = new MatrixCoordinate(Corpus.CDM, "x", V6160, ElementKind.TYPE, p);
        MatrixCoordinate rule = new MatrixCoordinate(Corpus.CDM, "x", V6160, ElementKind.RULE, p);
        // TYPE is declared before RULE in ElementKind
        assertTrue(type.compareTo(rule) < 0);
    }

    @Test
    void compare_distinguishes_by_source_path_when_all_axes_equal() {
        MatrixCoordinate a = new MatrixCoordinate(Corpus.CDM, "x", V6160, ElementKind.TYPE, Path.of("a.rosetta"));
        MatrixCoordinate b = new MatrixCoordinate(Corpus.CDM, "x", V6160, ElementKind.TYPE, Path.of("b.rosetta"));
        assertTrue(a.compareTo(b) < 0);
    }

    @Test
    void constructor_rejects_null_components() {
        Path p = Path.of("a.rosetta");
        assertThrows(NullPointerException.class,
                () -> new MatrixCoordinate(null, "x", V6160, ElementKind.TYPE, p));
        assertThrows(NullPointerException.class,
                () -> new MatrixCoordinate(Corpus.CDM, null, V6160, ElementKind.TYPE, p));
        assertThrows(NullPointerException.class,
                () -> new MatrixCoordinate(Corpus.CDM, "x", null, ElementKind.TYPE, p));
        assertThrows(NullPointerException.class,
                () -> new MatrixCoordinate(Corpus.CDM, "x", V6160, null, p));
        assertThrows(NullPointerException.class,
                () -> new MatrixCoordinate(Corpus.CDM, "x", V6160, ElementKind.TYPE, null));
    }
}
