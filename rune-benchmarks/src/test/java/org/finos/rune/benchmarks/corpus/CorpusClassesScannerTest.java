package org.finos.rune.benchmarks.corpus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The class-name scanner the corpus benchmarks discover XMeta/ReportFunction sets with. */
class CorpusClassesScannerTest {

    @TempDir
    Path tmp;

    @Test
    void scannerMapsPathsToDottedNamesAndFilters() throws Exception {
        Path meta = tmp.resolve("cdm/base/meta");
        Files.createDirectories(meta);
        Files.write(meta.resolve("PartyMeta.class"), new byte[0]);
        Files.write(meta.resolve("PartyMeta$Inner.class"), new byte[0]);
        Files.write(tmp.resolve("cdm/base/Party.class"), new byte[0]);

        List<String> metas = CorpusClasses.classNamesUnder(tmp,
                n -> n.endsWith("Meta") && n.contains(".meta.") && !n.contains("$"));
        assertEquals(List.of("cdm.base.meta.PartyMeta"), metas);

        List<String> all = CorpusClasses.classNamesUnder(tmp, n -> true);
        assertEquals(3, all.size());
    }
}
