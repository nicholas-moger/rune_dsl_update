package com.regnosys.rosetta.ir.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.expr.IRExprKind;
import com.regnosys.rosetta.testutil.IRSamples;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IRJsonCoverageTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final IRJsonSerializer SER = new IRJsonSerializer();

    @Test
    void everyIRKindSerializesToValidNonBlankJson() {
        for (IRKind kind : IRKind.values()) {
            String json = SER.toJson(IRSamples.node(kind));
            assertFalse(json.isBlank(), "blank for " + kind);
            JsonNode tree = assertDoesNotThrow(() -> MAPPER.readTree(json), "invalid JSON for " + kind);
            // re-parse guard: the discriminator round-trips to the source kind (value-equality, not mere presence)
            assertEquals(kind.name(), tree.get("kind").asText(), "kind mismatch for " + kind);
        }
    }

    @Test
    void everyIRExprKindSerializesToValidNonBlankJson() {
        for (IRExprKind kind : IRExprKind.values()) {
            String json = SER.toJson(IRSamples.expr(kind));
            assertFalse(json.isBlank(), "blank for " + kind);
            JsonNode tree = assertDoesNotThrow(() -> MAPPER.readTree(json), "invalid JSON for " + kind);
            assertEquals(kind.name(), tree.get("kind").asText(), "kind mismatch for " + kind);
            assertTrue(tree.has("type"), "missing type for " + kind);
        }
    }

    @Test
    void documentCarriesIrFormatVersionOne() throws Exception {
        String doc = SER.toJson(List.of(IRSamples.node(IRKind.ENUM)));
        JsonNode tree = MAPPER.readTree(doc);
        assertEquals(1, tree.get("irFormatVersion").asInt());
        assertTrue(tree.get("nodes").isArray());
    }

    /**
     * Proof-of-emission for ALL 47 IRExprKinds (spec: each of the original 14 + the #494
     * CONSTRUCT + the #496 LAMBDA_OP + the #499 META_ACCESS + the #500 CONVERSION / PIPE /
     * ONLY_EXISTS + the #501 SYMBOL_NAV + the #502 CLOSURE_PARAM / META_OUTPUT_APPLY + the
     * #503 ALL_ANY_COMPARE + the #504 SYN_ITEM_NAV + the #505 CHOICE_OPTION_NAV /
     * DISPATCH_INPUT_REF + the #507 DEEP_FEATURE_NAV / RECORD_FEATURE_NAV + the #508
     * META_ITEM_NAV + the #512 RECORD_RECEIVER_NAV / QUALIFIER_ITEM_NAV + the #513
     * SWITCH_OP / DEFAULT_OP / MEMBERSHIP_OP / COLLECT_OP + the #514 OUTPUT_REF /
     * META_PARAM_REF / RULE_INPUT_NAV + the #515 WITH_META_OP / JOIN_OP + the #518
     * OUTPUT_ALIAS_NAV + the #520 LIBRARY_APPLY + the #525 QUALIFIER_RECEIVER_NAV + the
     * #528 IMPLICIT_ATTR_NAV + the #529 CHOICE_RECEIVER_NAV + the #640 CONDITION_INSTANCE (the 47th) — this
     * count line recuts WITH each kind mint since the #513 Seat-1
     * MF-1 (it froze at "29" while the #508 30th kind rode, then at "32" while the #513
     * kinds rode, then at "44" while the #528 45th kind rode — the stale-comment class the
     * Seat-1 sweep catches; the #528 instance caught and recut at #529; it froze at "46" while
     * the #640 47th kind rode - caught at PR #640 round 1, rule6 SF-5). Parsed-tree
     * (the spec allows
     * exact-string OR parsed-tree); the exact VALUES are additionally pinned by the exact-string
     * tests in IRJsonSerializerExpressionTest for every kind except POINT_FREE_APPLY,
     * META_ACCESS, the #500–#505 shallow kinds and the #507–#529 kinds (shallow, except
     * DEEP_FEATURE_NAV / RECORD_RECEIVER_NAV / QUALIFIER_RECEIVER_NAV /
     * CHOICE_RECEIVER_NAV receiver-bearing — their JSON nests the receiver
     * subtree, so "shallow" deliberately does not cover them) (the #492 kind rides this
     * parsed-tree proof
     * only — the #494 Copilot R4 accuracy recut; CONSTRUCT and LAMBDA_OP each gained their exact
     * pin with their teach; META_ACCESS rides the parsed-tree proof at its #499 introduction,
     * the #500 kinds, SYMBOL_NAV and the #502–#529 kinds at theirs). Total switch — a 47th kind
     * breaks compile.
     */
    @Test
    void everyIRExprKindEmitsItsDiscriminatorAndChildren() throws Exception {
        for (IRExprKind kind : IRExprKind.values()) {
            JsonNode t = MAPPER.readTree(SER.toJson(IRSamples.expr(kind)));
            for (String key : requiredKeys(kind)) {
                assertTrue(t.has(key), kind + " JSON missing key: " + key);
            }
        }
    }

    private static List<String> requiredKeys(IRExprKind kind) {
        return switch (kind) {
            case LITERAL -> List.of("literalKind", "value");
            case LIST_CONSTRUCT -> List.of("elements");
            case EMPTY_LITERAL -> List.of("source");
            case VARIABLE -> List.of("name", "variableKind");
            case REFERENCE -> List.of("target", "referenceKind");
            case APPLY -> List.of("callee", "args");
            case BINARY_OP -> List.of("op", "left", "right");
            case EXISTENCE -> List.of("op", "modifier", "arg");
            case FIELD_ACCESS -> List.of("receiver", "feature", "featureCardinality");
            case META_ACCESS -> List.of("receiver", "feature", "metaQualifiers", "featureCardinality");
            case LIST_OP -> List.of("op", "child");
            case CONDITIONAL -> List.of("condition", "then", "else");
            case LET -> List.of("binder", "value", "in");
            case TO_STRING -> List.of("child");
            case POINT_FREE_APPLY -> List.of("callee");
            case CONSTRUCT -> List.of("typeName", "attributeNames", "spread");
            case LAMBDA_OP -> List.of("op", "binderName", "receiver", "body");
            case CONVERSION -> List.of("conversionKind", "targetTypeName", "child");
            case PIPE -> List.of("spineLength");
            case ONLY_EXISTS -> List.of("pathCount");
            case SYMBOL_NAV -> List.of("symbolKind");
            case CLOSURE_PARAM -> List.of("paramName");
            case META_OUTPUT_APPLY -> List.of("calleeName");
            case ALL_ANY_COMPARE -> List.of("op", "modifier");
            case SYN_ITEM_NAV -> List.of("featureName");
            case CHOICE_OPTION_NAV -> List.of("headName", "optionName");
            case DISPATCH_INPUT_REF -> List.of("inputName");
            case DEEP_FEATURE_NAV -> List.of("receiver", "feature");
            case RECORD_FEATURE_NAV -> List.of("headName", "feature", "recordTypeName");
            case META_ITEM_NAV -> List.of("featureName");
            case RECORD_RECEIVER_NAV -> List.of("receiver", "feature", "recordTypeName");
            case QUALIFIER_ITEM_NAV -> List.of("qualifierName");
            case SWITCH_OP -> List.of("caseCount", "hasDefault");
            case DEFAULT_OP -> List.of();
            case MEMBERSHIP_OP -> List.of("op");
            case COLLECT_OP -> List.of("op", "hasBody");
            case OUTPUT_REF -> List.of("outputName");
            case META_PARAM_REF -> List.of("paramName");
            case RULE_INPUT_NAV -> List.of("featureName");
            case IMPLICIT_ATTR_NAV -> List.of("attributeName");
            case CONDITION_INSTANCE -> List.of("typeName");
            case WITH_META_OP -> List.of("entryCount");
            case JOIN_OP -> List.of("hasSeparator");
            case OUTPUT_ALIAS_NAV -> List.of("headName", "feature");
            case LIBRARY_APPLY -> List.of("calleeName");
            case QUALIFIER_RECEIVER_NAV -> List.of("receiver", "qualifierName");
            case CHOICE_RECEIVER_NAV -> List.of("receiver", "optionName", "choiceName");
        };
    }

    // -------------------------------------------------------------------------
    // Model-drift sentinel
    // -------------------------------------------------------------------------

    /**
     * Model-drift sentinel for the serializer (NOT a proof of emission). Mirrors
     * {@code IRPrintabilityCoverageTest#modelDrift_everyRecordComponentIsMappedOrAllowlisted}
     * verbatim: reflects each record's components, removes the excluded set, and compares to
     * {@link IRSamples#RENDERED_COMPONENTS} — so a NEW model component (neither mapped nor
     * excluded) fails this test, forcing a conscious serializer + map update. It checks component
     * names, not output, so it cannot catch a serializer that STOPS emitting an already-mapped
     * component; actual serialization is proven by the per-kind content tests above and the golden
     * snapshots, and switch-totality by {@code everyIRExprKindEmitsItsDiscriminatorAndChildren}.
     */
    @Test
    void modelDrift_everyRecordComponentIsMappedOrAllowlisted() {
        IRSamples.RENDERED_COMPONENTS.forEach((cls, expectedRendered) -> {
            TreeSet<String> actual = new TreeSet<>();
            for (RecordComponent rc : cls.getRecordComponents()) {
                if (!IRSamples.EXCLUDED_COMPONENTS.contains(rc.getName())) {
                    actual.add(rc.getName());
                }
            }
            assertEquals(new TreeSet<>(expectedRendered), actual,
                cls.getSimpleName() + ": a component is neither rendered nor allowlisted "
                    + "(update the serializer AND this expected set, or add to the allowlist).");
        });
    }
}
