package com.regnosys.rosetta.ast.symbolid;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.model.RQualifiableConfig;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.SymbolId;
import com.regnosys.rosetta.symbols.symbolid.TestSymbolResolver;
import org.junit.jupiter.api.Test;

import java.lang.ref.Reference;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the migrated cross-ref accessor for {@code RQualifiableConfig.rootTypeId}
 * (P1.4.1b Task 4.6 — site 6/6 SymbolId migration, final site).
 *
 * <ul>
 *   <li>{@code rootType()} keeps its existing {@code Optional<RDataType>} signature.</li>
 *   <li>{@code rootTypeId()} is a new accessor returning {@code Optional<SymbolId>}.</li>
 *   <li>{@code setRootTypeId(SymbolId)} replaces the removed
 *       {@code setResolvedRootType(RDataType)} setter.</li>
 * </ul>
 *
 * <p>Uses the dedicated minimal-corpus fixture under
 * {@code rune-parser/src/test/resources/symbolid-fixtures/qualifiable/}
 * so the test does not depend on the full builtin corpus.
 *
 * <p>Fixture grammar: {@code isProduct root RootType;} — the grammar rule is
 * {@code rosettaQualifiableConfiguration: (IS_EVENT | IS_PRODUCT) ROOT qualifiedName SEMI}.
 * See {@code rune-parser/src/test/resources/snippets/builtins/qualifiable-config.rosetta}
 * for a canonical snippet example.
 *
 * <p>Per plan step 4.6 — mirrors {@link REnumerationSuperTypeIdTest} shape.
 */
class RQualifiableConfigRootTypeIdTest {

    /**
     * After RWorkspace.build(), the qualifiable config's rootTypeId holds the
     * correct SymbolId, and rootType() resolves lazily to the RootType node.
     */
    @Test
    void rootType_resolvesViaWorkspace_postBuild() throws Exception {
        List<RModel> files = SymbolIdTestFixtures.parse("qualifiable/");
        RWorkspace ws = RWorkspace.build(files).workspace();

        RDataType rootTypeNode = ws.namespace("com.test")
                .flatMap(n -> n.lookup("RootType"))
                .filter(RDataType.class::isInstance)
                .map(RDataType.class::cast)
                .orElseThrow(() -> new AssertionError("RDataType com.test.RootType not found in workspace"));

        // Find the qualifiable config node from the parsed model
        RQualifiableConfig cfg = ws.files().stream()
                .flatMap(f -> f.configurations().stream())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No RQualifiableConfig found in workspace"));

        // rootTypeId() must be present with correct coordinates
        Optional<SymbolId> rootTypeId = cfg.rootTypeId();
        assertTrue(rootTypeId.isPresent(),
                "rootTypeId() should be present after build");
        assertEquals("com.test", rootTypeId.get().namespace(),
                "SymbolId namespace should be 'com.test'");
        assertEquals("RootType", rootTypeId.get().localName(),
                "SymbolId localName should be 'RootType'");
        assertEquals(ws.generation(), rootTypeId.get().generation(),
                "SymbolId generation should match workspace generation");

        // rootType() lazy-resolves to the same node instance as rootTypeNode
        assertSame(rootTypeNode, cfg.rootType().orElseThrow(),
                "rootType() should resolve to the same RDataType instance as RootType");
    }

    /**
     * A qualifiable config without a root type name should have an empty
     * rootTypeId and an empty rootType().
     *
     * <p>Constructed directly — no fixture needed since the grammar always
     * requires a root type name. Instead we test the null-field guard directly.
     */
    @Test
    void noRootType_idIsEmpty() {
        RQualifiableConfig cfg = new RQualifiableConfig();
        // rootTypeName not set — rootTypeId should be empty
        assertTrue(cfg.rootTypeId().isEmpty(),
                "rootTypeId() should be empty when rootTypeName is not set");
        // rootType() must NOT throw even when no workspace is attached (null guard fires first)
        assertTrue(cfg.rootType().isEmpty(),
                "rootType() should be empty when rootTypeId is null");
    }

    /**
     * Direct-construction test: wire a qualifiable config to an RDataType target via
     * TestSymbolResolver and the new setRootTypeId setter.
     */
    @Test
    void directConstruction_wireRootType_dataTypeTarget() {
        TestSymbolResolver resolver = new TestSymbolResolver();

        RDataType rootType = new RDataType();
        rootType.setName("MyRootType");

        RQualifiableConfig cfg = new RQualifiableConfig();
        cfg.setRootTypeName("MyRootType");

        // Wire via the new setter (mirrors wireSuperType pattern)
        resolver.wireSuperType(cfg, "test.ns", "MyRootType", rootType, cfg::setRootTypeId);

        // rootTypeId() is present
        Optional<SymbolId> id = cfg.rootTypeId();
        assertTrue(id.isPresent(), "rootTypeId() should be present after wiring");
        assertEquals("test.ns", id.get().namespace());
        assertEquals("MyRootType", id.get().localName());

        // rootType() lazy-resolves to the same RDataType instance
        assertSame(rootType, cfg.rootType().orElseThrow(),
                "rootType() should resolve lazily to rootType");

        Reference.reachabilityFence(resolver);
    }
}
