package com.regnosys.rosetta.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the {@code rosetta-config.yml} reader against the measured consumer
 * shapes (CDM 6.20.6 / DRR 6.34.1 both carry exactly {@code model.name} +
 * empty {@code dependencies} + {@code generators.namespaces}; the iso20022
 * cell adds {@code generators.doNotPrune} entries).
 */
class RosettaConfigFileTest {

    @TempDir
    Path tmp;

    private RosettaConfigFile write(String yaml) throws IOException {
        Path yml = tmp.resolve("rosetta-config.yml");
        Files.writeString(yml, yaml);
        return RosettaConfigFile.load(yml);
    }

    @Test
    void cdmShapedConfig() throws IOException {
        RosettaConfigFile config = write("""
                model:
                  name: Common Domain Model
                dependencies:
                generators:
                  namespaces:
                  - cdm.*
                  - com.rosetta.model
                """);
        assertTrue(config.namespaceFilter().test("cdm.base.staticdata.party"));
        assertTrue(config.namespaceFilter().test("com.rosetta.model"));
        assertFalse(config.namespaceFilter().test("fpml.consolidated.recordkeeping"));
        assertEquals(Set.of(), config.doNotPrune());
    }

    @Test
    void doNotPruneEntriesNormalizeToTypeHashAttribute() throws IOException {
        RosettaConfigFile config = write("""
                generators:
                  namespaces:
                  - iso20022.*
                  doNotPrune:
                  - type: ClearingPartyAndTime1Choice__1
                    attribute: dtls
                  - type: ClearingPartyAndTime2Choice__1
                    attribute: dtls
                """);
        assertEquals(
                Set.of("ClearingPartyAndTime1Choice__1#dtls", "ClearingPartyAndTime2Choice__1#dtls"),
                config.doNotPrune());
    }

    @Test
    void absentGeneratorsSectionMeansAllowAll() throws IOException {
        RosettaConfigFile config = write("""
                model:
                  name: Anything
                """);
        assertTrue(config.namespaceFilter().test("any.namespace.at.all"));
        assertEquals(Set.of(), config.doNotPrune());
    }

    @Test
    void defaultsAllowAll() {
        RosettaConfigFile config = RosettaConfigFile.defaults();
        assertTrue(config.namespaceFilter().test("whatever"));
        assertEquals(Set.of(), config.doNotPrune());
    }

    @Test
    void yamlGlobalTagsAreRejected() {
        // Contract pin (not a differential witness — SnakeYAML 2.x's default
        // constructor also rejects global tags): the config loader must never
        // instantiate types from YAML tags. SafeConstructor guarantees
        // plain scalars/maps/lists regardless of library defaults.
        assertThrows(org.yaml.snakeyaml.error.YAMLException.class, () -> write("""
                generators:
                  namespaces: !!java.util.Date 2020-01-01
                """));
    }
}
