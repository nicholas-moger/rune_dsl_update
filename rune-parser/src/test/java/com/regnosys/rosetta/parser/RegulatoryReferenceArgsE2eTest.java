package com.regnosys.rosetta.parser;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.regulatory.RDocReference;
import com.regnosys.rosetta.ast.regulatory.RRegulatoryReferenceArg;
import com.regnosys.rosetta.ast.types.RDataType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests for the H4 regulatoryReference named-arg list (P1.4.2 / T13).
 */
class RegulatoryReferenceArgsE2eTest {

    @Test
    void regulatoryReferenceWithSingleNamedArg() {
        String src = """
                namespace foo

                type Trade:
                  [regulatoryReference ESMA MiFIR_RTS article "1" (jurisdiction = "EU")]
                  tradeId string (1..1)
                """;
        RModel model = parse(src);
        List<RRegulatoryReferenceArg> args = firstTypeLevelRegulatoryArgs(model);
        assertEquals(1, args.size());
        assertEquals("jurisdiction", args.get(0).name());
        assertEquals("EU", args.get(0).value());
    }

    @Test
    void regulatoryReferenceWithMultipleNamedArgs() {
        String src = """
                namespace foo

                type Trade:
                  [regulatoryReference ESMA MiFIR_RTS article "1" (jurisdiction = "EU", effective = "2018-01-03")]
                  tradeId string (1..1)
                """;
        RModel model = parse(src);
        List<RRegulatoryReferenceArg> args = firstTypeLevelRegulatoryArgs(model);
        assertEquals(2, args.size());
        assertEquals("jurisdiction", args.get(0).name());
        assertEquals("EU", args.get(0).value());
        assertEquals("effective", args.get(1).name());
        assertEquals("2018-01-03", args.get(1).value());
    }

    @Test
    void legacyRegulatoryReferenceWithoutNamedArgsParsesUnchanged() {
        String src = """
                namespace foo

                type Trade:
                  [regulatoryReference ESMA MiFIR_RTS article "1"]
                  tradeId string (1..1)
                """;
        RModel model = parse(src);
        List<RRegulatoryReferenceArg> args = firstTypeLevelRegulatoryArgs(model);
        assertTrue(args.isEmpty(),
            "legacy regulatoryReference without named-args produces empty list");
    }

    @Test
    void regulatoryReferenceArgsCoexistsWithRationale() {
        String src = """
                namespace foo

                type Trade:
                  [regulatoryReference ESMA MiFIR_RTS article "1" (jurisdiction = "EU")
                   rationale "scope qualifier"]
                  tradeId string (1..1)
                """;
        RModel model = parse(src);
        RDocReference docRef = ((RDataType) model.rootElements().get(0)).docReferences().get(0);
        assertEquals(1, docRef.namedArgs().size());
        assertEquals("EU", docRef.namedArgs().get(0).value());
        assertEquals(1, docRef.rationales().size(),
            "rationale and namedArgs co-exist on the same docReference");
    }

    /**
     * Returns the named-args of the first type-level docReference on the Trade type.
     * Per grammar — {@code dataType: runeAnnotations? TYPE ... (docReference|annotationRef|classSynonym)* attribute*}
     * — a {@code [regulatoryReference ...]} block immediately after the COLON attaches
     * to the TYPE, not to the following attribute.
     */
    private static List<RRegulatoryReferenceArg> firstTypeLevelRegulatoryArgs(RModel model) {
        RDataType trade = (RDataType) model.rootElements().get(0);
        RDocReference docRef = trade.docReferences().get(0);
        return docRef.namedArgs();
    }

    private static RModel parse(String src) {
        return TestParseHelper.parseModel(src);
    }
}
