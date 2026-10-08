package com.regnosys.rosetta.ir.json;

import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.testutil.IRSamples;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IRJsonRoundTripTest {

    private static final Path GOLDEN_DIR = com.regnosys.rosetta.testutil.CorpusWalker.moduleRoot()
            .resolve("src/test/resources/ir-json-golden");

    private final IRJsonSerializer serializer = new IRJsonSerializer();
    private final IRJsonDeserializer deserializer = new IRJsonDeserializer();

    // The six committed declaration documents (the 5 step-2 goldens + the abstract-type fixture).
    private static final List<String> GOLDENS = List.of(
            "simple-type", "simple-choice", "simple-enum",
            "type-with-extends", "enum-with-extends", "abstract-type");

    @ParameterizedTest
    @EnumSource(value = IRKind.class,
            names = {"STRUCT", "CHOICE", "TYPE_ALIAS", "ENUM", "FIELD", "ENUM_VALUE", "MODEL",
                    "FUNCTION"})
    void fragmentRoundTripsForEveryDeclarationKindAndTheGenericFallback(IRKind kind) {
        IRNode node = IRSamples.node(kind);
        String json = serializer.toJson(node);
        assertEquals(json, serializer.toJson(deserializer.fromJsonNode(json)),
                "fragment round-trip drifted for " + kind);
    }

    @Test
    void documentRoundTripsForEveryGolden() throws IOException {
        for (String name : GOLDENS) {
            String text = Files.readString(GOLDEN_DIR.resolve(name + ".ir.json"));
            assertEquals(text, serializer.toJson(deserializer.fromJson(text)),
                    "document round-trip drifted for " + name);
        }
    }

    /**
     * The property gate (PR #644): the MINIMAL model node rides the {@code EnumSource} above with
     * every other declaration kind; this pins the FULLY-POPULATED one ({@link IRSamples#model()},
     * every model-level fact at once) on both routes - the bare fragment and the versioned document.
     */
    @Test
    void theModelNodeRoundTripsAsAFragmentAndInADocument() {
        String fragment = serializer.toJson(IRSamples.model());
        assertEquals(fragment, serializer.toJson(deserializer.fromJsonNode(fragment)),
                "fragment round-trip drifted for MODEL");
        String document = serializer.toJson(List.of(IRSamples.model()));
        assertEquals(document, serializer.toJson(deserializer.fromJson(document)),
                "document round-trip drifted for MODEL");
    }

    @Test
    void documentRoundTripsForEmptyModel() {
        String text = serializer.toJson(List.of());
        assertEquals(text, serializer.toJson(deserializer.fromJson(text)));
    }
}
