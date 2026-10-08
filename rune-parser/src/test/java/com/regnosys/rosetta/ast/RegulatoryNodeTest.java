package com.regnosys.rosetta.ast;

import com.regnosys.rosetta.ast.annotations.RAnnotationPathExpression;
import com.regnosys.rosetta.ast.annotations.RAnnotationPathSegment;
import com.regnosys.rosetta.ast.enums.ReportTiming;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.regulatory.RBody;
import com.regnosys.rosetta.ast.regulatory.RCorpus;
import com.regnosys.rosetta.ast.regulatory.RDocReference;
import com.regnosys.rosetta.ast.regulatory.RDocumentRationale;
import com.regnosys.rosetta.ast.regulatory.RRegulatoryDocumentReference;
import com.regnosys.rosetta.ast.regulatory.RReport;
import com.regnosys.rosetta.ast.regulatory.RSegmentDef;
import com.regnosys.rosetta.ast.regulatory.RSegmentRef;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.functions.RFunction;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for regulatory and document reference AST nodes (Task 6).
 *
 * <p>These tests construct nodes directly (no parsing) to validate getters,
 * setters, optional fields, list fields, boolean getters, the
 * {@code children()} traversal, and the updated docReferences placeholder types.
 */
class RegulatoryNodeTest extends BaseAstTest {

    // =========================================================================
    // RBody
    // =========================================================================

    @Test
    void rBody_defaultState() {
        RBody body = new RBody();
        assertNull(body.bodyTypeKeyword());
        assertNull(body.name());
        assertEquals(Optional.empty(), body.definition());
    }

    @Test
    void rBody_setBodyTypeKeyword() {
        RBody body = new RBody();
        body.setBodyTypeKeyword("Authority");

        assertEquals("Authority", body.bodyTypeKeyword());
    }

    @Test
    void rBody_setName() {
        RBody body = new RBody();
        body.setName("CFTC");

        assertEquals("CFTC", body.name());
    }

    @Test
    void rBody_setDefinition() {
        RBody body = new RBody();
        body.setBodyTypeKeyword("Authority");
        body.setName("CFTC");
        body.setDefinition("Commodity Futures Trading Commission");

        assertEquals(Optional.of("Commodity Futures Trading Commission"), body.definition());
    }

    @Test
    void rBody_noDefinition() {
        RBody body = new RBody();
        body.setBodyTypeKeyword("Standard");
        body.setName("ISDA");

        assertEquals(Optional.empty(), body.definition());
    }

    @Test
    void rBody_implementsRDefinable() {
        RBody body = new RBody();
        assertInstanceOf(RDefinable.class, body);
    }

    @Test
    void rBody_extendsRRootElement() {
        RBody body = new RBody();
        assertInstanceOf(RRootElement.class, body);
        assertInstanceOf(RNode.class, body);
    }

    @Test
    void rBody_isLeafNode() {
        RBody body = new RBody();
        body.setBodyTypeKeyword("Authority");
        body.setName("ESMA");
        assertTrue(body.children().isEmpty());
    }

    // =========================================================================
    // RCorpus
    // =========================================================================

    @Test
    void rCorpus_defaultState() {
        RCorpus corpus = new RCorpus();
        assertNull(corpus.corpusTypeKeyword());
        assertEquals(Optional.empty(), corpus.bodyRef());
        assertEquals(Optional.empty(), corpus.displayName());
        assertNull(corpus.name());
        assertEquals(Optional.empty(), corpus.definition());
    }

    @Test
    void rCorpus_setCorpusTypeKeyword() {
        RCorpus corpus = new RCorpus();
        corpus.setCorpusTypeKeyword("Regulation");

        assertEquals("Regulation", corpus.corpusTypeKeyword());
    }

    @Test
    void rCorpus_setBodyRef() {
        RCorpus corpus = new RCorpus();
        corpus.setCorpusTypeKeyword("Regulation");
        corpus.setBodyRef("ESMA");

        assertEquals(Optional.of("ESMA"), corpus.bodyRef());
    }

    @Test
    void rCorpus_noBodyRef() {
        RCorpus corpus = new RCorpus();
        corpus.setCorpusTypeKeyword("Directive");
        corpus.setName("MiFID");

        assertEquals(Optional.empty(), corpus.bodyRef());
    }

    @Test
    void rCorpus_setDisplayName() {
        RCorpus corpus = new RCorpus();
        corpus.setCorpusTypeKeyword("Regulation");
        corpus.setDisplayName("MiFIR RTS");

        assertEquals(Optional.of("MiFIR RTS"), corpus.displayName());
    }

    @Test
    void rCorpus_noDisplayName() {
        RCorpus corpus = new RCorpus();
        corpus.setCorpusTypeKeyword("Regulation");
        corpus.setName("MiFIR_RTS");

        assertEquals(Optional.empty(), corpus.displayName());
    }

    @Test
    void rCorpus_setName() {
        RCorpus corpus = new RCorpus();
        corpus.setName("MiFIR_RTS");

        assertEquals("MiFIR_RTS", corpus.name());
    }

    @Test
    void rCorpus_setDefinition() {
        RCorpus corpus = new RCorpus();
        corpus.setCorpusTypeKeyword("Regulation");
        corpus.setName("MiFIR_RTS");
        corpus.setDefinition("Markets in Financial Instruments Regulation");

        assertEquals(Optional.of("Markets in Financial Instruments Regulation"), corpus.definition());
    }

    @Test
    void rCorpus_implementsRDefinable() {
        RCorpus corpus = new RCorpus();
        assertInstanceOf(RDefinable.class, corpus);
    }

    @Test
    void rCorpus_extendsRRootElement() {
        RCorpus corpus = new RCorpus();
        assertInstanceOf(RRootElement.class, corpus);
        assertInstanceOf(RNode.class, corpus);
    }

    @Test
    void rCorpus_fullExample() {
        RCorpus corpus = new RCorpus();
        corpus.setCorpusTypeKeyword("Regulation");
        corpus.setBodyRef("ESMA");
        corpus.setDisplayName("MiFIR RTS");
        corpus.setName("MiFIR_RTS");
        corpus.setDefinition("MiFIR Regulatory Technical Standards");

        assertEquals("Regulation", corpus.corpusTypeKeyword());
        assertEquals(Optional.of("ESMA"), corpus.bodyRef());
        assertEquals(Optional.of("MiFIR RTS"), corpus.displayName());
        assertEquals("MiFIR_RTS", corpus.name());
        assertEquals(Optional.of("MiFIR Regulatory Technical Standards"), corpus.definition());
    }

    // =========================================================================
    // RSegmentDef
    // =========================================================================

    @Test
    void rSegmentDef_defaultState() {
        RSegmentDef seg = new RSegmentDef();
        assertNull(seg.name());
    }

    @Test
    void rSegmentDef_setName() {
        RSegmentDef seg = new RSegmentDef();
        seg.setName("article");

        assertEquals("article", seg.name());
    }

    @Test
    void rSegmentDef_keywordName() {
        // Segment can also use keyword names like RATIONALE
        RSegmentDef seg = new RSegmentDef();
        seg.setName("rationale");

        assertEquals("rationale", seg.name());
    }

    @Test
    void rSegmentDef_notRDefinable() {
        RSegmentDef seg = new RSegmentDef();
        assertFalse(RDefinable.class.isAssignableFrom(RSegmentDef.class));
    }

    @Test
    void rSegmentDef_extendsRRootElement() {
        RSegmentDef seg = new RSegmentDef();
        assertInstanceOf(RRootElement.class, seg);
        assertInstanceOf(RNode.class, seg);
    }

    @Test
    void rSegmentDef_isLeafNode() {
        RSegmentDef seg = new RSegmentDef();
        seg.setName("section");
        assertTrue(seg.children().isEmpty());
    }

    // =========================================================================
    // RSegmentRef
    // =========================================================================

    @Test
    void rSegmentRef_defaultState() {
        RSegmentRef ref = new RSegmentRef();
        assertNull(ref.segmentName());
        assertNull(ref.value());
    }

    @Test
    void rSegmentRef_setSegmentName() {
        RSegmentRef ref = new RSegmentRef();
        ref.setSegmentName("article");

        assertEquals("article", ref.segmentName());
    }

    @Test
    void rSegmentRef_setValue() {
        RSegmentRef ref = new RSegmentRef();
        ref.setSegmentName("article");
        ref.setValue("13");

        assertEquals("13", ref.value());
    }

    @Test
    void rSegmentRef_isLeafNode() {
        RSegmentRef ref = new RSegmentRef();
        ref.setSegmentName("section");
        ref.setValue("2");
        assertTrue(ref.children().isEmpty());
    }

    @Test
    void rSegmentRef_extendsRNode() {
        RSegmentRef ref = new RSegmentRef();
        assertInstanceOf(RNode.class, ref);
        assertFalse(RRootElement.class.isAssignableFrom(RSegmentRef.class));
    }

    // =========================================================================
    // RDocumentRationale
    // =========================================================================

    @Test
    void rDocumentRationale_defaultState() {
        RDocumentRationale dr = new RDocumentRationale();
        assertEquals(Optional.empty(), dr.rationale());
        assertEquals(Optional.empty(), dr.rationaleAuthor());
    }

    @Test
    void rDocumentRationale_setRationale() {
        RDocumentRationale dr = new RDocumentRationale();
        dr.setRationale("Required for reporting");

        assertEquals(Optional.of("Required for reporting"), dr.rationale());
    }

    @Test
    void rDocumentRationale_setRationaleAuthor() {
        RDocumentRationale dr = new RDocumentRationale();
        dr.setRationaleAuthor("ISDA");

        assertEquals(Optional.of("ISDA"), dr.rationaleAuthor());
    }

    @Test
    void rDocumentRationale_bothFields() {
        RDocumentRationale dr = new RDocumentRationale();
        dr.setRationale("Required for compliance");
        dr.setRationaleAuthor("CFTC");

        assertEquals(Optional.of("Required for compliance"), dr.rationale());
        assertEquals(Optional.of("CFTC"), dr.rationaleAuthor());
    }

    @Test
    void rDocumentRationale_isLeafNode() {
        RDocumentRationale dr = new RDocumentRationale();
        dr.setRationale("Required");
        assertTrue(dr.children().isEmpty());
    }

    @Test
    void rDocumentRationale_extendsRNode() {
        RDocumentRationale dr = new RDocumentRationale();
        assertInstanceOf(RNode.class, dr);
        assertFalse(RRootElement.class.isAssignableFrom(RDocumentRationale.class));
    }

    // =========================================================================
    // RRegulatoryDocumentReference
    // =========================================================================

    @Test
    void rRegulatoryDocumentReference_defaultState() {
        RRegulatoryDocumentReference rdr = new RRegulatoryDocumentReference();
        assertNull(rdr.bodyRef());
        assertTrue(rdr.corpusRefs().isEmpty());
        assertTrue(rdr.segmentRefs().isEmpty());
        assertTrue(rdr.children().isEmpty());
    }

    @Test
    void rRegulatoryDocumentReference_setBodyRef() {
        RRegulatoryDocumentReference rdr = new RRegulatoryDocumentReference();
        rdr.setBodyRef("CFTC");

        assertEquals("CFTC", rdr.bodyRef());
    }

    @Test
    void rRegulatoryDocumentReference_addCorpusRefs() {
        RRegulatoryDocumentReference rdr = new RRegulatoryDocumentReference();
        rdr.setBodyRef("ESMA");
        rdr.corpusRefs().add("MiFIR_RTS");
        rdr.corpusRefs().add("MiFID_II");

        assertEquals(2, rdr.corpusRefs().size());
        assertEquals("MiFIR_RTS", rdr.corpusRefs().get(0));
        assertEquals("MiFID_II", rdr.corpusRefs().get(1));
    }

    @Test
    void rRegulatoryDocumentReference_addSegmentRefs() {
        RRegulatoryDocumentReference rdr = new RRegulatoryDocumentReference();
        rdr.setBodyRef("CFTC");
        rdr.corpusRefs().add("DoddFrank");

        RSegmentRef seg1 = new RSegmentRef();
        seg1.setSegmentName("article");
        seg1.setValue("13");

        RSegmentRef seg2 = new RSegmentRef();
        seg2.setSegmentName("section");
        seg2.setValue("2");

        rdr.segmentRefs().add(seg1);
        rdr.segmentRefs().add(seg2);

        assertEquals(2, rdr.segmentRefs().size());
        assertEquals("article", rdr.segmentRefs().get(0).segmentName());
        assertEquals("13", rdr.segmentRefs().get(0).value());
        assertEquals("section", rdr.segmentRefs().get(1).segmentName());
        assertEquals("2", rdr.segmentRefs().get(1).value());
    }

    @Test
    void rRegulatoryDocumentReference_childrenIncludesSegmentRefs() {
        RRegulatoryDocumentReference rdr = new RRegulatoryDocumentReference();
        rdr.setBodyRef("CFTC");
        rdr.corpusRefs().add("DoddFrank");

        RSegmentRef seg = new RSegmentRef();
        seg.setSegmentName("article");
        seg.setValue("13");
        rdr.segmentRefs().add(seg);

        List<? extends RNode> children = rdr.children();
        assertEquals(1, children.size());
        assertSame(seg, children.get(0));
    }

    @Test
    void rRegulatoryDocumentReference_childrenEmptyWithoutSegments() {
        RRegulatoryDocumentReference rdr = new RRegulatoryDocumentReference();
        rdr.setBodyRef("CFTC");
        rdr.corpusRefs().add("DoddFrank");

        assertTrue(rdr.children().isEmpty());
    }

    @Test
    void rRegulatoryDocumentReference_extendsRNode() {
        RRegulatoryDocumentReference rdr = new RRegulatoryDocumentReference();
        assertInstanceOf(RNode.class, rdr);
        assertFalse(RRootElement.class.isAssignableFrom(RRegulatoryDocumentReference.class));
    }

    // =========================================================================
    // RDocReference
    // =========================================================================

    @Test
    void rDocReference_defaultState() {
        RDocReference ref = new RDocReference();
        assertFalse(ref.isRegulatoryReference());
        assertEquals(Optional.empty(), ref.forPath());
        assertNull(ref.regulatoryDocRef());
        assertTrue(ref.rationales().isEmpty());
        assertEquals(Optional.empty(), ref.structuredProvision());
        assertEquals(Optional.empty(), ref.provision());
        assertFalse(ref.isReportedField());
        assertTrue(ref.children().isEmpty());
    }

    @Test
    void rDocReference_setRegulatoryReference() {
        RDocReference ref = new RDocReference();
        ref.setRegulatoryReference(true);

        assertTrue(ref.isRegulatoryReference());
    }

    @Test
    void rDocReference_docReferenceNotRegulatory() {
        RDocReference ref = new RDocReference();
        ref.setRegulatoryReference(false);

        assertFalse(ref.isRegulatoryReference());
    }

    @Test
    void rDocReference_setForPath() {
        RDocReference ref = new RDocReference();
        ref.setRegulatoryReference(true);

        RAnnotationPathExpression path = new RAnnotationPathExpression();
        path.setRoot("trade");
        path.setRootIsItem(false);
        ref.setForPath(path);

        assertTrue(ref.forPath().isPresent());
        assertEquals("trade", ref.forPath().get().root());
    }

    @Test
    void rDocReference_noForPath() {
        RDocReference ref = new RDocReference();
        assertEquals(Optional.empty(), ref.forPath());
    }

    @Test
    void rDocReference_setRegulatoryDocRef() {
        RDocReference ref = new RDocReference();
        ref.setRegulatoryReference(true);

        RRegulatoryDocumentReference regDocRef = new RRegulatoryDocumentReference();
        regDocRef.setBodyRef("CFTC");
        regDocRef.corpusRefs().add("DoddFrank");
        ref.setRegulatoryDocRef(regDocRef);

        assertNotNull(ref.regulatoryDocRef());
        assertEquals("CFTC", ref.regulatoryDocRef().bodyRef());
    }

    @Test
    void rDocReference_addRationales() {
        RDocReference ref = new RDocReference();
        ref.setRegulatoryReference(true);

        RDocumentRationale dr1 = new RDocumentRationale();
        dr1.setRationale("Required for reporting");

        RDocumentRationale dr2 = new RDocumentRationale();
        dr2.setRationaleAuthor("ISDA");

        ref.rationales().add(dr1);
        ref.rationales().add(dr2);

        assertEquals(2, ref.rationales().size());
        assertEquals(Optional.of("Required for reporting"), ref.rationales().get(0).rationale());
        assertEquals(Optional.of("ISDA"), ref.rationales().get(1).rationaleAuthor());
    }

    @Test
    void rDocReference_setStructuredProvision() {
        RDocReference ref = new RDocReference();
        ref.setStructuredProvision("Article 13, Section 2");

        assertEquals(Optional.of("Article 13, Section 2"), ref.structuredProvision());
    }

    @Test
    void rDocReference_noStructuredProvision() {
        RDocReference ref = new RDocReference();
        assertEquals(Optional.empty(), ref.structuredProvision());
    }

    @Test
    void rDocReference_setProvision() {
        RDocReference ref = new RDocReference();
        ref.setProvision("The counterparty shall report...");

        assertEquals(Optional.of("The counterparty shall report..."), ref.provision());
    }

    @Test
    void rDocReference_noProvision() {
        RDocReference ref = new RDocReference();
        assertEquals(Optional.empty(), ref.provision());
    }

    @Test
    void rDocReference_setReportedField() {
        RDocReference ref = new RDocReference();
        ref.setReportedField(true);

        assertTrue(ref.isReportedField());
    }

    @Test
    void rDocReference_notReportedField() {
        RDocReference ref = new RDocReference();
        assertFalse(ref.isReportedField());
    }

    @Test
    void rDocReference_childrenIncludesForPathAndRegDocRefAndRationales() {
        RDocReference ref = new RDocReference();
        ref.setRegulatoryReference(true);

        RAnnotationPathExpression forPath = new RAnnotationPathExpression();
        forPath.setRoot("trade");
        forPath.setRootIsItem(false);
        ref.setForPath(forPath);

        RRegulatoryDocumentReference regDocRef = new RRegulatoryDocumentReference();
        regDocRef.setBodyRef("CFTC");
        regDocRef.corpusRefs().add("DoddFrank");
        ref.setRegulatoryDocRef(regDocRef);

        RDocumentRationale dr = new RDocumentRationale();
        dr.setRationale("Required");
        ref.rationales().add(dr);

        List<? extends RNode> children = ref.children();
        assertEquals(3, children.size());
        assertSame(forPath, children.get(0));
        assertSame(regDocRef, children.get(1));
        assertSame(dr, children.get(2));
    }

    @Test
    void rDocReference_childrenWithoutForPath() {
        RDocReference ref = new RDocReference();
        ref.setRegulatoryReference(true);

        RRegulatoryDocumentReference regDocRef = new RRegulatoryDocumentReference();
        regDocRef.setBodyRef("CFTC");
        regDocRef.corpusRefs().add("DoddFrank");
        ref.setRegulatoryDocRef(regDocRef);

        List<? extends RNode> children = ref.children();
        assertEquals(1, children.size());
        assertSame(regDocRef, children.get(0));
    }

    @Test
    void rDocReference_extendsRNode() {
        RDocReference ref = new RDocReference();
        assertInstanceOf(RNode.class, ref);
        assertFalse(RRootElement.class.isAssignableFrom(RDocReference.class));
    }

    // =========================================================================
    // RReport
    // =========================================================================

    @Test
    void rReport_defaultState() {
        RReport report = new RReport();
        assertNull(report.regulatoryDocRef());
        assertNull(report.timing());
        assertNull(report.fromType());
        assertTrue(report.whenConditions().isEmpty());
        assertEquals(Optional.empty(), report.usingStandard());
        assertNull(report.withType());
        assertEquals(Optional.empty(), report.withSource());
        assertTrue(report.children().isEmpty());
    }

    @Test
    void rReport_hasNoNameField() {
        // RReport does NOT have a name() method — verify it doesn't exist
        // by checking the class doesn't declare one
        try {
            RReport.class.getDeclaredMethod("name");
            fail("RReport should not have a name() method");
        } catch (NoSuchMethodException e) {
            // Expected — RReport has no name
        }
    }

    @Test
    void rReport_notRDefinable() {
        assertFalse(RDefinable.class.isAssignableFrom(RReport.class));
    }

    @Test
    void rReport_extendsRRootElement() {
        RReport report = new RReport();
        assertInstanceOf(RRootElement.class, report);
        assertInstanceOf(RNode.class, report);
    }

    @Test
    void rReport_setRegulatoryDocRef() {
        RReport report = new RReport();

        RRegulatoryDocumentReference regDocRef = new RRegulatoryDocumentReference();
        regDocRef.setBodyRef("CFTC");
        regDocRef.corpusRefs().add("DoddFrank");
        report.setRegulatoryDocRef(regDocRef);

        assertNotNull(report.regulatoryDocRef());
        assertEquals("CFTC", report.regulatoryDocRef().bodyRef());
    }

    @Test
    void rReport_setTiming() {
        RReport report = new RReport();
        report.setTiming(ReportTiming.T_PLUS_1);

        assertEquals(ReportTiming.T_PLUS_1, report.timing());
    }

    @Test
    void rReport_setFromType() {
        RReport report = new RReport();

        RTypeCall tc = new RTypeCall();
        tc.setTypeName("TradeInstruction");
        report.setFromType(tc);

        assertNotNull(report.fromType());
        assertEquals("TradeInstruction", report.fromType().typeName());
    }

    @Test
    void rReport_addWhenConditions() {
        RReport report = new RReport();
        report.whenConditions().add("IsReportable");
        report.whenConditions().add("IsCleared");

        assertEquals(2, report.whenConditions().size());
        assertEquals("IsReportable", report.whenConditions().get(0));
        assertEquals("IsCleared", report.whenConditions().get(1));
    }

    @Test
    void rReport_setUsingStandard() {
        RReport report = new RReport();
        report.setUsingStandard("ISDA_Create");

        assertEquals(Optional.of("ISDA_Create"), report.usingStandard());
    }

    @Test
    void rReport_noUsingStandard() {
        RReport report = new RReport();
        assertEquals(Optional.empty(), report.usingStandard());
    }

    @Test
    void rReport_setWithType() {
        RReport report = new RReport();
        report.setWithType("MiFIRReport");

        assertEquals("MiFIRReport", report.withType());
    }

    @Test
    void rReport_setWithSource() {
        RReport report = new RReport();
        report.setWithSource("MiFIRSource");

        assertEquals(Optional.of("MiFIRSource"), report.withSource());
    }

    @Test
    void rReport_noWithSource() {
        RReport report = new RReport();
        assertEquals(Optional.empty(), report.withSource());
    }

    @Test
    void rReport_childrenIncludesRegDocRefAndFromType() {
        RReport report = new RReport();

        RRegulatoryDocumentReference regDocRef = new RRegulatoryDocumentReference();
        regDocRef.setBodyRef("CFTC");
        regDocRef.corpusRefs().add("DoddFrank");
        report.setRegulatoryDocRef(regDocRef);

        RTypeCall tc = new RTypeCall();
        tc.setTypeName("TradeInstruction");
        report.setFromType(tc);

        List<? extends RNode> children = report.children();
        assertEquals(2, children.size());
        assertSame(regDocRef, children.get(0));
        assertSame(tc, children.get(1));
    }

    @Test
    void rReport_childrenEmptyWhenNoNodeFields() {
        RReport report = new RReport();
        report.setTiming(ReportTiming.REAL_TIME);
        report.setWithType("SomeReport");
        // Only string/enum fields set — no child nodes
        assertTrue(report.children().isEmpty());
    }

    @Test
    void rReport_fullExample() {
        // report CFTC DoddFrank in T+1
        //     from TradeInstruction
        //     when IsReportable and IsCleared
        //     using standard ISDA_Create
        //     with type MiFIRReport
        //     with source MiFIRSource
        RReport report = new RReport();

        RRegulatoryDocumentReference regDocRef = new RRegulatoryDocumentReference();
        regDocRef.setBodyRef("CFTC");
        regDocRef.corpusRefs().add("DoddFrank");
        report.setRegulatoryDocRef(regDocRef);

        report.setTiming(ReportTiming.T_PLUS_1);

        RTypeCall tc = new RTypeCall();
        tc.setTypeName("TradeInstruction");
        report.setFromType(tc);

        report.whenConditions().add("IsReportable");
        report.whenConditions().add("IsCleared");

        report.setUsingStandard("ISDA_Create");
        report.setWithType("MiFIRReport");
        report.setWithSource("MiFIRSource");

        assertEquals("CFTC", report.regulatoryDocRef().bodyRef());
        assertEquals(ReportTiming.T_PLUS_1, report.timing());
        assertEquals("TradeInstruction", report.fromType().typeName());
        assertEquals(List.of("IsReportable", "IsCleared"), report.whenConditions());
        assertEquals(Optional.of("ISDA_Create"), report.usingStandard());
        assertEquals("MiFIRReport", report.withType());
        assertEquals(Optional.of("MiFIRSource"), report.withSource());

        List<? extends RNode> children = report.children();
        assertEquals(2, children.size());
    }

    // =========================================================================
    // Integration: docReferences placeholder updated to List<RDocReference>
    // =========================================================================

    @Test
    void integration_dataTypeDocReferencesIsTyped() {
        RDataType dt = new RDataType();
        dt.setName("Trade");

        RDocReference docRef = new RDocReference();
        docRef.setRegulatoryReference(true);
        RRegulatoryDocumentReference regDocRef = new RRegulatoryDocumentReference();
        regDocRef.setBodyRef("CFTC");
        regDocRef.corpusRefs().add("DoddFrank");
        docRef.setRegulatoryDocRef(regDocRef);

        dt.docReferences().add(docRef);

        assertEquals(1, dt.docReferences().size());
        assertInstanceOf(RDocReference.class, dt.docReferences().get(0));
        assertTrue(dt.docReferences().get(0).isRegulatoryReference());
        assertEquals("CFTC", dt.docReferences().get(0).regulatoryDocRef().bodyRef());

        // Verify it appears in children
        assertTrue(dt.children().contains(docRef));
    }

    @Test
    void integration_functionDocReferencesIsTyped() {
        RFunction fn = new RFunction();
        fn.setName("Create_Trade");

        RDocReference docRef = new RDocReference();
        docRef.setRegulatoryReference(false);
        RRegulatoryDocumentReference regDocRef = new RRegulatoryDocumentReference();
        regDocRef.setBodyRef("ISDA");
        regDocRef.corpusRefs().add("CDM");
        docRef.setRegulatoryDocRef(regDocRef);

        fn.docReferences().add(docRef);

        assertEquals(1, fn.docReferences().size());
        assertInstanceOf(RDocReference.class, fn.docReferences().get(0));
        assertFalse(fn.docReferences().get(0).isRegulatoryReference());

        // Verify it appears in children
        assertTrue(fn.children().contains(docRef));
    }

    @Test
    void integration_fullDocReferenceOnDataType() {
        // Build a complete doc reference with all optional fields
        RDocReference docRef = new RDocReference();
        docRef.setRegulatoryReference(true);

        // FOR path
        RAnnotationPathExpression forPath = new RAnnotationPathExpression();
        forPath.setRoot("trade");
        forPath.setRootIsItem(false);
        RAnnotationPathSegment seg = new RAnnotationPathSegment();
        seg.setName("partyRole");
        seg.setDeep(false);
        forPath.segments().add(seg);
        docRef.setForPath(forPath);

        // Regulatory doc ref
        RRegulatoryDocumentReference regDocRef = new RRegulatoryDocumentReference();
        regDocRef.setBodyRef("CFTC");
        regDocRef.corpusRefs().add("DoddFrank");
        RSegmentRef segRef = new RSegmentRef();
        segRef.setSegmentName("article");
        segRef.setValue("13");
        regDocRef.segmentRefs().add(segRef);
        docRef.setRegulatoryDocRef(regDocRef);

        // Rationales
        RDocumentRationale dr = new RDocumentRationale();
        dr.setRationale("Required for reporting");
        dr.setRationaleAuthor("CFTC");
        docRef.rationales().add(dr);

        // Structured provision + provision
        docRef.setStructuredProvision("Article 13, Section 2");
        docRef.setProvision("The counterparty shall report...");
        docRef.setReportedField(true);

        // Attach to data type
        RDataType dt = new RDataType();
        dt.setName("Trade");
        dt.docReferences().add(docRef);

        // Verify deep tree
        RDocReference attached = dt.docReferences().get(0);
        assertTrue(attached.isRegulatoryReference());
        assertTrue(attached.forPath().isPresent());
        assertEquals("trade", attached.forPath().get().root());
        assertEquals("CFTC", attached.regulatoryDocRef().bodyRef());
        assertEquals(1, attached.regulatoryDocRef().segmentRefs().size());
        assertEquals("article", attached.regulatoryDocRef().segmentRefs().get(0).segmentName());
        assertEquals(1, attached.rationales().size());
        assertEquals(Optional.of("Required for reporting"), attached.rationales().get(0).rationale());
        assertEquals(Optional.of("Article 13, Section 2"), attached.structuredProvision());
        assertEquals(Optional.of("The counterparty shall report..."), attached.provision());
        assertTrue(attached.isReportedField());

        // children: forPath + regDocRef + rationale
        List<? extends RNode> docRefChildren = attached.children();
        assertEquals(3, docRefChildren.size());
    }

    // =========================================================================
    // Integration: parse-and-build tests
    // =========================================================================

    @Nested
    class IntegrationParseAndBuild {

        @Test
        void parseBody() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "body Authority CFTC <\"Commodity Futures Trading Commission\">");
            assertEquals(1, model.rootElements().size());
            assertInstanceOf(RBody.class, model.rootElements().get(0));

            RBody body = (RBody) model.rootElements().get(0);
            assertEquals("Authority", body.bodyTypeKeyword());
            assertEquals("CFTC", body.name());
            assertEquals(Optional.of("Commodity Futures Trading Commission"), body.definition());
            assertSourceRangeSet(body);
        }

        @Test
        void parseBodyNoDefinition() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "body Standard ISDA");
            RBody body = (RBody) model.rootElements().get(0);
            assertEquals("Standard", body.bodyTypeKeyword());
            assertEquals("ISDA", body.name());
            assertEquals(Optional.empty(), body.definition());
        }

        @Test
        void parseCorpus() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "body Authority ESMA\n"
                    + "corpus Regulation ESMA \"MiFIR RTS\" MiFIR_RTS <\"MiFIR Regulatory Technical Standards\">");
            // body + corpus = 2 root elements
            assertEquals(2, model.rootElements().size());
            assertInstanceOf(RCorpus.class, model.rootElements().get(1));

            RCorpus corpus = (RCorpus) model.rootElements().get(1);
            assertEquals("Regulation", corpus.corpusTypeKeyword());
            assertEquals(Optional.of("ESMA"), corpus.bodyRef());
            assertEquals(Optional.of("MiFIR RTS"), corpus.displayName());
            assertEquals("MiFIR_RTS", corpus.name());
            assertEquals(Optional.of("MiFIR Regulatory Technical Standards"), corpus.definition());
            assertSourceRangeSet(corpus);
        }

        @Test
        void parseCorpusMinimal() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "corpus Regulation MyCorp");
            RCorpus corpus = (RCorpus) model.rootElements().get(0);
            assertEquals("Regulation", corpus.corpusTypeKeyword());
            assertEquals(Optional.empty(), corpus.bodyRef());
            assertEquals(Optional.empty(), corpus.displayName());
            assertEquals("MyCorp", corpus.name());
            assertEquals(Optional.empty(), corpus.definition());
        }

        @Test
        void parseSegment() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "segment article");
            assertEquals(1, model.rootElements().size());
            assertInstanceOf(RSegmentDef.class, model.rootElements().get(0));

            RSegmentDef seg = (RSegmentDef) model.rootElements().get(0);
            assertEquals("article", seg.name());
            assertSourceRangeSet(seg);
        }

        @Test
        void parseDocReferenceOnDataType() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "body Authority CFTC\n"
                    + "corpus Regulation CFTC DoddFrank\n"
                    + "segment article\n"
                    + "type Trade:\n"
                    + "    [docReference CFTC DoddFrank article \"13\" provision \"The counterparty shall report.\"]\n"
                    + "    id string (1..1)");
            // body + corpus + segment + data type
            RDataType dt = (RDataType) model.rootElements().get(3);
            assertEquals(1, dt.docReferences().size());

            RDocReference docRef = dt.docReferences().get(0);
            assertFalse(docRef.isRegulatoryReference());
            assertNotNull(docRef.regulatoryDocRef());
            assertEquals("CFTC", docRef.regulatoryDocRef().bodyRef());
            assertEquals(1, docRef.regulatoryDocRef().corpusRefs().size());
            assertEquals("DoddFrank", docRef.regulatoryDocRef().corpusRefs().get(0));
            assertEquals(1, docRef.regulatoryDocRef().segmentRefs().size());
            assertEquals("article", docRef.regulatoryDocRef().segmentRefs().get(0).segmentName());
            assertEquals("13", docRef.regulatoryDocRef().segmentRefs().get(0).value());
            assertEquals(Optional.of("The counterparty shall report."), docRef.provision());
            assertSourceRangeSet(docRef);
        }

        @Test
        void parseDocReferenceRegulatory() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "body Authority CFTC\n"
                    + "corpus Regulation CFTC DoddFrank\n"
                    + "type Trade:\n"
                    + "    [regulatoryReference CFTC DoddFrank]\n"
                    + "    id string (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(2);
            assertEquals(1, dt.docReferences().size());

            RDocReference docRef = dt.docReferences().get(0);
            assertTrue(docRef.isRegulatoryReference());
            assertEquals("CFTC", docRef.regulatoryDocRef().bodyRef());
        }

        @Test
        void parseDocReferenceWithRationale() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "body Authority CFTC\n"
                    + "corpus Regulation CFTC DoddFrank\n"
                    + "type Trade:\n"
                    + "    [docReference CFTC DoddFrank rationale \"Required for compliance\"]\n"
                    + "    id string (1..1)");
            RDataType dt = (RDataType) model.rootElements().get(2);
            RDocReference docRef = dt.docReferences().get(0);
            assertEquals(1, docRef.rationales().size());

            RDocumentRationale dr = docRef.rationales().get(0);
            assertEquals(Optional.of("Required for compliance"), dr.rationale());
            assertEquals(Optional.empty(), dr.rationaleAuthor());
        }

        @Test
        void parseReport() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "body Authority CFTC\n"
                    + "corpus Regulation CFTC DoddFrank\n"
                    + "report CFTC DoddFrank in T+1\n"
                    + "    from TradeInstruction\n"
                    + "    when IsReportable\n"
                    + "    with type MiFIRReport");
            // body + corpus + report = 3 elements
            assertInstanceOf(RReport.class, model.rootElements().get(2));

            RReport report = (RReport) model.rootElements().get(2);
            assertNotNull(report.regulatoryDocRef());
            assertEquals("CFTC", report.regulatoryDocRef().bodyRef());
            assertEquals(1, report.regulatoryDocRef().corpusRefs().size());
            assertEquals("DoddFrank", report.regulatoryDocRef().corpusRefs().get(0));
            assertEquals(ReportTiming.T_PLUS_1, report.timing());
            assertNotNull(report.fromType());
            assertEquals("TradeInstruction", report.fromType().typeName());
            assertEquals(1, report.whenConditions().size());
            assertEquals("IsReportable", report.whenConditions().get(0));
            assertEquals("MiFIRReport", report.withType());
            assertEquals(Optional.empty(), report.usingStandard());
            assertEquals(Optional.empty(), report.withSource());
            assertSourceRangeSet(report);
        }

        @Test
        void parseReportWithAllOptions() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "body Authority CFTC\n"
                    + "corpus Regulation CFTC DoddFrank\n"
                    + "report CFTC DoddFrank in real-time\n"
                    + "    from TradeInstruction\n"
                    + "    when IsReportable and IsCleared\n"
                    + "    using standard ISDA_Create\n"
                    + "    with type MiFIRReport\n"
                    + "    with source MiFIRSource");
            RReport report = (RReport) model.rootElements().get(2);
            assertEquals(ReportTiming.REAL_TIME, report.timing());
            assertEquals(2, report.whenConditions().size());
            assertEquals("IsReportable", report.whenConditions().get(0));
            assertEquals("IsCleared", report.whenConditions().get(1));
            assertEquals(Optional.of("ISDA_Create"), report.usingStandard());
            assertEquals("MiFIRReport", report.withType());
            assertEquals(Optional.of("MiFIRSource"), report.withSource());
        }

        @Test
        void parseMultipleRegulatoryElements() {
            var model = parseAndBuild(
                    "namespace test.example\n"
                    + "body Authority CFTC <\"Commission\">\n"
                    + "body Standard ISDA\n"
                    + "corpus Regulation CFTC DoddFrank\n"
                    + "segment article\n"
                    + "segment section");
            assertEquals(5, model.rootElements().size());
            assertInstanceOf(RBody.class, model.rootElements().get(0));
            assertInstanceOf(RBody.class, model.rootElements().get(1));
            assertInstanceOf(RCorpus.class, model.rootElements().get(2));
            assertInstanceOf(RSegmentDef.class, model.rootElements().get(3));
            assertInstanceOf(RSegmentDef.class, model.rootElements().get(4));

            assertEquals("CFTC", ((RBody) model.rootElements().get(0)).name());
            assertEquals("ISDA", ((RBody) model.rootElements().get(1)).name());
            assertEquals("DoddFrank", ((RCorpus) model.rootElements().get(2)).name());
            assertEquals("article", ((RSegmentDef) model.rootElements().get(3)).name());
            assertEquals("section", ((RSegmentDef) model.rootElements().get(4)).name());
        }
    }
}
