package com.regnosys.rosetta.ast.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.Reference;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.enums.OperationOp;
import com.regnosys.rosetta.ast.enums.ReportTiming;
import com.regnosys.rosetta.ast.enums.RuleKind;
import com.regnosys.rosetta.ast.expressions.literals.RStringLiteral;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.regulatory.RRegulatoryDocumentReference;
import com.regnosys.rosetta.ast.regulatory.RReport;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.symbols.SymbolId;
import com.regnosys.rosetta.symbols.symbolid.TestSymbolResolver;

/**
 * Phase X T2 — unit tests for the {@link RFunction#fromRule(RRule)} and
 * {@link RFunction#fromReport(RReport)} static factories, fork analogues
 * to upstream {@code RObjectFactory.buildRFunction(RosettaRule)} and
 * {@code .buildRFunction(RosettaReport)}.
 *
 * <p>Tests verify the 4 acceptance cases per controller T2 brief +
 * plan the development plan "2026-05-19-phase-x-port-3-generators" § T2 step 8:
 * inputs/output preservation from rule, inputs/output preservation from
 * report, originReport accessor returns Optional.of(report) for fromReport
 * + Optional.empty() for fromRule.
 */
class RFunctionFactoryTest {

    // === Fixture helpers =====================================================

    private static RRule buildSyntheticRule(String name, String fromTypeName) {
        RRule rule = new RRule();
        rule.setKind(RuleKind.REPORTING);
        rule.setName(name);
        if (fromTypeName != null) {
            RTypeCall tc = new RTypeCall();
            tc.setTypeName(fromTypeName);
            rule.setFromType(tc);
        }
        return rule;
    }

    private static RReport buildSyntheticReport(String regulatoryBodyRef,
                                                 String fromTypeName,
                                                 String withTypeName) {
        RReport report = new RReport();
        report.setTiming(ReportTiming.T_PLUS_1);
        RRegulatoryDocumentReference docRef = new RRegulatoryDocumentReference();
        docRef.setBodyRef(regulatoryBodyRef);
        report.setRegulatoryDocRef(docRef);
        RTypeCall inputTc = new RTypeCall();
        inputTc.setTypeName(fromTypeName);
        report.setFromType(inputTc);
        report.setWithType(withTypeName);
        return report;
    }

    // === fromRule tests ======================================================

    @Test
    void fromRule_preservesInputsAndOutput() {
        RRule rule = buildSyntheticRule("MyRule", "Trade");
        RFunction func = RFunction.fromRule(rule);

        assertNotNull(func, "fromRule should not return null");
        assertEquals("MyRule", func.name(), "name should mirror the rule's name");
        assertEquals(1, func.inputs().size(),
                "synthetic function should have exactly 1 input");
        RAttribute input = func.inputs().get(0);
        assertEquals("input", input.name());
        assertNotNull(input.typeCall(),
                "fromRule should copy rule.fromType into the input's typeCall");
        assertEquals("Trade", input.typeCall().typeName());

        Optional<RAttribute> output = func.output();
        assertTrue(output.isPresent(), "synthetic function should have an output");
        assertEquals("output", output.get().name());
    }

    @Test
    void fromRule_setsNameFromRule() {
        RRule rule = buildSyntheticRule("AnotherRule", null);
        RFunction func = RFunction.fromRule(rule);
        assertEquals("AnotherRule", func.name(),
                "RFunction.name() should derive from rule.name()");
    }

    @Test
    void fromRule_originReportReturnsEmpty() {
        RRule rule = buildSyntheticRule("MyRule", "Trade");
        RFunction func = RFunction.fromRule(rule);
        assertTrue(func.originReport().isEmpty(),
                "RFunction built via fromRule must NOT carry an originReport");
    }

    @Test
    void fromRule_handlesAbsentFromType() {
        RRule rule = buildSyntheticRule("NoFromType", null);
        RFunction func = RFunction.fromRule(rule);

        assertEquals(1, func.inputs().size());
        RAttribute input = func.inputs().get(0);
        assertEquals("input", input.name());
        assertNull(input.typeCall(),
                "input typeCall should be null when rule.fromType is absent");
    }

    @Test
    void fromRule_failsOnNull() {
        assertThrows(NullPointerException.class, () -> RFunction.fromRule(null),
                "null rule must be rejected fail-fast");
    }

    // Phase X1 — operations population. fromRule must synthesise an
    // ROperation(SET, "output", rule.expression()) when the rule carries an
    // expression, mirroring upstream RObjectFactory.buildRFunction(RosettaRule)
    // line 121. This is the body-emission hook that lets FunctionGenerator
    // compile a non-empty body for the synthetic RFunction.
    @Test
    void fromRule_populatesOperationsFromExpression() {
        RRule rule = buildSyntheticRule("EnrichmentData", "Trade");
        RStringLiteral lit = new RStringLiteral();
        lit.setValue("test");
        rule.setExpression(lit);

        RFunction func = RFunction.fromRule(rule);

        assertEquals(1, func.operations().size(),
                "fromRule must synthesise exactly 1 ROperation when rule has expression");
        ROperation op = func.operations().get(0);
        assertEquals(OperationOp.SET, op.operator(),
                "synthetic operation must use SET (mirrors upstream line 121)");
        assertEquals("output", op.targetName(),
                "synthetic operation must target the output attribute by name");
        assertSame(lit, op.expression(),
                "synthetic operation must reference the same RExpression instance "
                + "from rule.expression() — no defensive copy");
    }

    // Phase X1 — operations stay empty when the rule has no expression.
    // Mirrors upstream's implicit behaviour (no ROperation synthesised when
    // RosettaRule.getExpression() is null).
    @Test
    void fromRule_emptyOperationsWhenNoExpression() {
        RRule rule = buildSyntheticRule("NoExprRule", "Trade");
        // rule.expression unset

        RFunction func = RFunction.fromRule(rule);

        assertTrue(func.operations().isEmpty(),
                "fromRule must produce empty operations when rule has no expression");
    }

    // Phase X1 — operator discriminator. The synthetic operation must be
    // OperationOp.SET (not ADD) so FunctionGenerator emits a setter assignment
    // rather than a list mutation. Mirrors upstream RObjectFactory line 121.
    @Test
    void fromRule_syntheticOperationUsesSetOperator() {
        RRule rule = buildSyntheticRule("R", "Trade");
        RStringLiteral lit = new RStringLiteral();
        lit.setValue("v");
        rule.setExpression(lit);

        RFunction func = RFunction.fromRule(rule);

        assertEquals(OperationOp.SET, func.operations().get(0).operator(),
                "synthetic ROperation must use SET operator");
    }

    // Phase X1 — segment absence. The synthetic operation targets the output
    // attribute directly with no nested-attribute path; segment must be empty
    // so FunctionGenerator emits a top-level setter (not a nested-path walk).
    @Test
    void fromRule_syntheticOperationSegmentIsEmpty() {
        RRule rule = buildSyntheticRule("R", "Trade");
        RStringLiteral lit = new RStringLiteral();
        lit.setValue("v");
        rule.setExpression(lit);

        RFunction func = RFunction.fromRule(rule);

        assertTrue(func.operations().get(0).segment().isEmpty(),
                "synthetic ROperation must have no segment (output is the direct target)");
    }

    // Phase X1 — originRule reachability. RuleGenerator needs the source RRule
    // (the synthetic RFunction has no parent attachment of its own) to recover
    // the RModel parent for namespace + ruleReferenceAnnotations resolution.
    @Test
    void fromRule_originRuleAccessor_returnsSourceRule() {
        RRule rule = buildSyntheticRule("MyRule", "Trade");
        RFunction func = RFunction.fromRule(rule);

        Optional<RRule> origin = func.originRule();
        assertTrue(origin.isPresent(),
                "RFunction built via fromRule MUST carry an originRule");
        assertSame(rule, origin.get(),
                "originRule identity must match the source RRule");
    }

    // T6.0.5 — Origin discriminator. fromRule must set origin=RULE so the
    // principled JavaTypeTranslator.toFunctionJavaClass(RFunction, ModelSymbolId)
    // dispatcher routes the synthetic RFunction to <ns>.reports/<Name>Rule via
    // the toJavaRuleClass private router (mirrors upstream lines 127-131).
    @Test
    void fromRule_setsOriginToRule() {
        RRule rule = buildSyntheticRule("MyRule", "Trade");
        RFunction func = RFunction.fromRule(rule);
        assertEquals(RFunction.Origin.RULE, func.origin(),
                "RFunction built via fromRule must carry origin=RULE so the "
                + "principled toFunctionJavaClass(RFunction, ModelSymbolId) "
                + "dispatcher routes to <ns>.reports/<Name>Rule");
    }

    // === fromReport tests ====================================================

    /**
     * v3.2 seat 4 (PR #625, round 2 — the code-quality review's SF-2): the fail-CLOSED contract of
     * {@code RNode.shareResolverWith} witnessed directly. A report that was never attached shares no
     * resolver, so {@code fromReport} must leave its synthetic output ID-LESS (both report seats refuse an
     * id-less output loudly); an id on a DETACHED node is the state whose first {@code referencedType()}
     * read threw "never attached" and was rendered as a TODO comment — the seat's one surprise. Lane W
     * (the share answering {@code true} for a detached node) is red on this test alone.
     */
    @Test
    void fromReport_detachedReport_leavesTheOutputIdLess_failClosed() {
        RReport report = buildSyntheticReport("CFTC", "TradeInstruction", "MiFIRReport");
        TestSymbolResolver resolver = new TestSymbolResolver();
        report.setWithTypeId(resolver.idFor("test.ns", "MiFIRReport"));   // an id, on a report never attached

        RFunction func = RFunction.fromReport(report);

        RTypeCall out = func.output().orElseThrow().typeCall();
        assertTrue(out.referencedTypeId().isEmpty(),
                "a never-attached report shares no resolver: the synthetic output must stay id-LESS (refused loudly downstream), never an id on a detached node");
        assertFalse(report.shareResolverWith(new RTypeCall()), "shareResolverWith answers false for a detached node");
    }

    /**
     * The other half of the contract: an ATTACHED report's id rides the synthetic output AND resolves through the
     * shared resolver — the target is bound in the resolver and read back from the synthetic output (round 3, cq
     * NIT-3: the name claimed the resolver, the assertions had only checked the id). The resolver's only strong
     * reference is a local, so it is fenced past the last read (round 3, cq NIT-1: under liveness-based
     * reachability a collection between the attach and {@code fromReport} would clear the weak reference and fail
     * this test for a reason that is not the contract — the window the round-2 single read closed in production).
     */
    @Test
    void fromReport_attachedReport_carriesTheWithTypeId_throughTheSharedResolver() {
        RReport report = buildSyntheticReport("CFTC", "TradeInstruction", "MiFIRReport");
        TestSymbolResolver resolver = new TestSymbolResolver();
        RDataType target = new RDataType();
        target.setName("MiFIRReport");
        resolver.bind("test.ns", "MiFIRReport", target);
        SymbolId id = resolver.idFor("test.ns", "MiFIRReport");
        report.attachToWorkspace(resolver);
        report.setWithTypeId(id);

        RFunction func = RFunction.fromReport(report);

        RTypeCall out = func.output().orElseThrow().typeCall();
        assertEquals(Optional.of(id), out.referencedTypeId(), "the attached report's id rides the synthetic output");
        assertSame(target, out.referencedType().orElseThrow(),
                "the synthetic output resolves THROUGH the shared resolver: the bound target, read back from the synthetic");
        assertTrue(report.shareResolverWith(new RTypeCall()), "shareResolverWith answers true for an attached node");
        Reference.reachabilityFence(resolver);
    }

    /**
     * Round 3 (cq NIT-2): the up-front null contract of {@code shareResolverWith} — a null synthetic is refused with a
     * {@link NullPointerException} BEFORE the resolver is read, on a detached node (which would otherwise answer
     * {@code false}) and on an attached one (which would otherwise throw from the attach) alike. The re-verification's
     * catch (the code-quality seat at commit 11): the attached half was GREEN on the guard-less code — a null synthetic
     * handed to {@code attachToWorkspace} throws the same CLASS from the null receiver — so both halves assert the guard's
     * own MESSAGE, which no helpful NPE (JEP 358) spells; lane X (the {@code requireNonNull} deleted) is red on this test
     * alone — decided by the DETACHED half, which fails first (a detached node answers {@code false} without throwing); the
     * attached half's message assertion is the reasoned half of the contract, unreachable under that mutation (a split into
     * two methods, one lane-reached half each, is BANKED). No fence: the guard throws before the resolver is read, so its
     * collection cannot reach either half.
     */
    @Test
    void shareResolverWith_nullSynthetic_isRefusedUpFront_detachedAndAttached() {
        RReport detached = buildSyntheticReport("CFTC", "TradeInstruction", "MiFIRReport");
        NullPointerException onDetached = assertThrows(NullPointerException.class, () -> detached.shareResolverWith(null),
                "a null synthetic is refused up front on a detached node, never answered false");
        assertEquals("synthetic must not be null", onDetached.getMessage(),
                "the guard's own message: the refusal is the up-front requireNonNull, not a later dereference");
        RReport attached = buildSyntheticReport("CFTC", "TradeInstruction", "MiFIRReport");
        TestSymbolResolver resolver = new TestSymbolResolver();
        attached.attachToWorkspace(resolver);
        NullPointerException onAttached = assertThrows(NullPointerException.class, () -> attached.shareResolverWith(null),
                "a null synthetic is refused up front on an attached node, never handed to attachToWorkspace");
        assertEquals("synthetic must not be null", onAttached.getMessage(),
                "the guard's own message: without the guard the null receiver's NPE from attachToWorkspace shares the class, not the message");
    }

    @Test
    void fromReport_preservesInputsAndOutput() {
        RReport report = buildSyntheticReport("CFTC", "TradeInstruction", "MiFIRReport");
        RFunction func = RFunction.fromReport(report);

        assertNotNull(func);
        assertEquals(1, func.inputs().size(),
                "synthetic function should have exactly 1 input");
        RAttribute input = func.inputs().get(0);
        assertEquals("input", input.name());
        assertEquals("TradeInstruction", input.typeCall().typeName(),
                "input typeCall should mirror report.fromType");

        Optional<RAttribute> output = func.output();
        assertTrue(output.isPresent());
        assertEquals("output", output.get().name());
        assertEquals("MiFIRReport", output.get().typeCall().typeName(),
                "output typeCall should mirror report.withType");
    }

    @Test
    void fromReport_originReportAccessor_returnsSourceReport() {
        RReport report = buildSyntheticReport("CFTC", "TradeInstruction", "MiFIRReport");
        RFunction func = RFunction.fromReport(report);

        Optional<RReport> origin = func.originReport();
        assertTrue(origin.isPresent(),
                "RFunction built via fromReport MUST carry an originReport");
        assertSame(report, origin.get(),
                "originReport identity must match the source RReport");
    }

    @Test
    void fromReport_setsNameFromBodyPlusCorpusList() {
        // Single-body-no-corpus report — name is just the bodyRef.
        RReport report = buildSyntheticReport("CFTC", "TradeInstruction", "MiFIRReport");
        RFunction func = RFunction.fromReport(report);
        // Name derivation mirrors upstream's
        // {@code ModelReportId.joinRegulatoryReference()} — concatenates body
        // with every corpus (no separator). T6 (2026-05-19) replaced the
        // earlier {@code withType}-based derivation; see RFunction.deriveReportName
        // for the byte-parity rationale.
        assertEquals("CFTC", func.name(),
                "RFunction.name() with no corpus list should equal the bodyRef alone");
    }

    @Test
    void fromReport_setsNameFromBodyPlusCorpusList_withCorpus() {
        // Body + 2-corpus report — name is body+corpus1+corpus2 (no separator).
        RReport report = new RReport();
        report.setTiming(ReportTiming.T_PLUS_1);
        RRegulatoryDocumentReference docRef = new RRegulatoryDocumentReference();
        docRef.setBodyRef("ASIC");
        docRef.corpusRefs().add("Dissemination");
        docRef.corpusRefs().add("Margin");
        report.setRegulatoryDocRef(docRef);
        RTypeCall inputTc = new RTypeCall();
        inputTc.setTypeName("Trade");
        report.setFromType(inputTc);
        report.setWithType("ASICMarginReport");

        RFunction func = RFunction.fromReport(report);
        assertEquals("ASICDisseminationMargin", func.name(),
                "RFunction.name() should be body+corpus1+corpus2 verbatim");
    }

    @Test
    void fromReport_failsOnNull() {
        assertThrows(NullPointerException.class, () -> RFunction.fromReport(null),
                "null report must be rejected fail-fast");
    }

    @Test
    void fromReport_failsWhenBodyRefMissing() {
        // Reports without regulatoryDocRef.bodyRef cannot yield a unique
        // RFunction.name — fail-fast rather than collide downstream at
        // emission time. (T6 dropped the earlier withType fallback because
        // the body+corpus alphanumeric name is now the load-bearing input to
        // toLabelProviderJavaClass + toReportFunctionJavaClass.)
        RReport report = new RReport();
        report.setTiming(ReportTiming.T_PLUS_1);
        RTypeCall inputTc = new RTypeCall();
        inputTc.setTypeName("TradeInstruction");
        report.setFromType(inputTc);
        report.setWithType("SomeReport");
        // regulatoryDocRef unset
        assertThrows(IllegalArgumentException.class,
                () -> RFunction.fromReport(report),
                "report missing regulatoryDocRef.bodyRef must fail-fast");
    }

    @Test
    void fromReport_failsWhenFromTypeMissing() {
        // PR #72 R8 F21 — Rosetta grammar (RosettaReport production)
        // mandates 'from' inputType=TypeCall with no '?' optionality marker.
        // A null fromType means the input AST is grammar-violating;
        // fail-fast at the factory boundary prevents the synthetic input
        // attribute from being silently emitted with no typeCall (which
        // would resolve to RMissingType downstream).
        RReport report = buildSyntheticReport("CFTC", "TradeInstruction", "MiFIRReport");
        report.setFromType(null);
        assertThrows(IllegalArgumentException.class,
                () -> RFunction.fromReport(report),
                "report missing fromType must fail-fast");
    }

    @Test
    void fromReport_failsWhenWithTypeMissing() {
        // PR #72 R8 F21 — Rosetta grammar (RosettaReport production)
        // mandates 'with' 'type' reportType=[Data|QualifiedName] with no
        // '?' optionality marker. A null/empty withType means the input AST
        // is grammar-violating; fail-fast at the factory boundary prevents
        // the synthetic output attribute from being silently emitted with
        // no typeCall (which would resolve to RMissingType downstream and
        // produce wrong-typed ReportFunction<I,O> generics — silent
        // byte-parity regression).
        RReport report = buildSyntheticReport("CFTC", "TradeInstruction", "MiFIRReport");
        report.setWithType(null);
        assertThrows(IllegalArgumentException.class,
                () -> RFunction.fromReport(report),
                "report missing withType must fail-fast");

        report.setWithType("");
        assertThrows(IllegalArgumentException.class,
                () -> RFunction.fromReport(report),
                "report with empty withType must fail-fast");
    }

    // T6.0.5 — Origin discriminator. fromReport must set origin=REPORT so the
    // principled JavaTypeTranslator.toFunctionJavaClass(RFunction, ModelSymbolId)
    // dispatcher routes the synthetic RFunction to
    // <ns>.reports/<body+corpus>ReportFunction via the toJavaReportClass
    // private router (mirrors upstream lines 122-126).
    @Test
    void fromReport_setsOriginToReport() {
        RReport report = buildSyntheticReport("CFTC", "TradeInstruction", "MiFIRReport");
        RFunction func = RFunction.fromReport(report);
        assertEquals(RFunction.Origin.REPORT, func.origin(),
                "RFunction built via fromReport must carry origin=REPORT so the "
                + "principled toFunctionJavaClass(RFunction, ModelSymbolId) "
                + "dispatcher routes to <ns>.reports/<body+corpus>ReportFunction");
    }

    // T6.0.5 — Default-construction invariant. A directly-constructed
    // RFunction (AstBuilder path for plain `func` declarations) must carry
    // origin=FUNCTION so the principled dispatcher routes to
    // <ns>.functions/<Name> via toJavaFunctionClass (mirrors upstream
    // lines 117-121).
    @Test
    void defaultConstruction_setsOriginToFunction() {
        RFunction func = new RFunction();
        assertEquals(RFunction.Origin.FUNCTION, func.origin(),
                "directly-constructed RFunction (AstBuilder path) must default "
                + "to origin=FUNCTION so the principled toFunctionJavaClass "
                + "dispatcher routes to <ns>.functions/<Name>");
    }

    // === Cross-factory invariant tests =======================================

    @Test
    void factoriesProduceMutableFunctions_butWithCleanSlate() {
        RFunction ruleFunc = RFunction.fromRule(buildSyntheticRule("R", "T"));
        RFunction reportFunc = RFunction.fromReport(
                buildSyntheticReport("CFTC", "T", "Out"));

        // Both factory-produced functions have fresh, empty list fields for
        // shortcuts/conditions/operations/postConditions/docReferences/annotationRefs.
        assertTrue(ruleFunc.shortcuts().isEmpty());
        assertTrue(ruleFunc.conditions().isEmpty());
        assertTrue(ruleFunc.operations().isEmpty());
        assertTrue(ruleFunc.postConditions().isEmpty());
        assertTrue(ruleFunc.docReferences().isEmpty());
        assertTrue(ruleFunc.annotationRefs().isEmpty());

        assertTrue(reportFunc.shortcuts().isEmpty());
        assertTrue(reportFunc.conditions().isEmpty());
        assertTrue(reportFunc.operations().isEmpty());
        assertTrue(reportFunc.postConditions().isEmpty());

        // dispatch is unset; superFunctionId is unset.
        assertFalse(ruleFunc.dispatch().isPresent());
        assertFalse(ruleFunc.superFunctionId().isPresent());
        assertFalse(reportFunc.dispatch().isPresent());
        assertFalse(reportFunc.superFunctionId().isPresent());
    }

    // === Phase X1 T2-AstBuilder diagnostic ===================================

    // Diagnostic: confirms AstBuilder populates RRule.expression for parsed
    // reporting rules. Unit-built rules with rule.setExpression(lit) work
    // (proved by fromRule_populatesOperationsFromExpression). The integration
    // path through AstBuilder.buildFromString fails — operations stays empty
    // at RuleGenerator.generate. See
    // the development audit "phase-x1-T2-astbuilder-finding".
    @Test
    void astBuilder_parsesReportingRuleExpression() {
        String source = "namespace x\n"
                + "type T:\n"
                + "  id string (1..1)\n"
                + "reporting rule R from T:\n"
                + "  extract id\n";
        RModel model = AstBuilder.buildFromString(source, "test.rosetta");
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Parsed model contains no RRule — grammar shape may be wrong"));

        assertTrue(rule.expression().isPresent(),
                "AstBuilder must populate RRule.expression for parsed "
                + "'reporting rule R from T: extract id' — got empty");
    }

    // Confirms the full bridge path: parse → model.rootElements() →
    // RFunction.fromRule. This is the EXACT path RuleGenerator.streamObjects
    // executes. If this passes, the synthetic operations DO populate from a
    // parsed-AST RRule and RuleGenerator.generate can rely on operations
    // being non-empty when the rule has an expression.
    @Test
    void astBuilder_parsedRule_fromRule_populatesOperations() {
        String source = String.join("\n",
                "namespace com.example.test",
                "",
                "type Trade:",
                "    id string (1..1)",
                "",
                "reporting rule TradeIdRule from Trade:",
                "    extract id"
        );
        RModel model = AstBuilder.buildFromString(source, "test.rosetta");
        RRule rule = (RRule) model.rootElements().stream()
                .filter(e -> e instanceof RRule)
                .findFirst()
                .orElseThrow();

        // Sanity: AstBuilder populated rule.expression.
        assertTrue(rule.expression().isPresent(),
                "parsed RRule must carry a non-empty expression");

        // Now bridge via fromRule — same path as RuleGenerator.streamObjects.
        RFunction func = RFunction.fromRule(rule);
        assertEquals(1, func.operations().size(),
                "fromRule on a parsed RRule with extract id must synthesise "
                + "exactly 1 ROperation — got " + func.operations().size());
        ROperation op = func.operations().get(0);
        assertEquals(OperationOp.SET, op.operator());
        assertEquals("output", op.targetName());
    }
}
