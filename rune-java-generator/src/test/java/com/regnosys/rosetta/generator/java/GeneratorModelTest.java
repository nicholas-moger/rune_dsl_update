package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.symbolid.GeneratorTestSymbolResolver;
import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.RMissingType;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RStringType;
import com.regnosys.rosetta.types.RType;
import com.rosetta.util.DottedPath;
import java.lang.ref.Reference;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorModelTest {

    // === Namespace resolution =================================================

    @Test void namespace_from_root_element() {
        var model = makeModel("com.example.model");
        var dt = new RDataType(); dt.setName("Trade");
        model.rootElements().add(dt);
        dt.setParent(model);

        var gm = makeGeneratorModel(model);
        assertEquals(DottedPath.splitOnDots("com.example.model"), gm.namespace(dt));
    }

    @Test void namespace_from_model() {
        var model = makeModel("com.example.model");
        var gm = makeGeneratorModel(model);
        assertEquals(DottedPath.splitOnDots("com.example.model"), gm.namespace(model));
    }

    @Test void namespace_no_parent_throws() {
        var dt = new RDataType(); dt.setName("Orphan");
        var gm = makeGeneratorModel(makeModel("com.example"));
        assertThrows(IllegalStateException.class, () -> gm.namespace(dt));
    }

    // === Symbol IDs ===========================================================

    @Test void data_type_symbol_id() {
        var model = makeModel("com.example");
        var dt = new RDataType(); dt.setName("Trade");
        model.rootElements().add(dt); dt.setParent(model);

        var gm = makeGeneratorModel(model);
        var id = gm.symbolId(dt);
        assertEquals("Trade", id.getName());
        assertEquals(DottedPath.splitOnDots("com.example"), id.getNamespace());
    }

    @Test void enum_symbol_id() {
        var model = makeModel("com.example");
        var en = new REnumeration(); en.setName("Color");
        model.rootElements().add(en); en.setParent(model);

        var gm = makeGeneratorModel(model);
        var id = gm.symbolId(en);
        assertEquals("Color", id.getName());
    }

    @Test void function_symbol_id() {
        var model = makeModel("com.example");
        var fn = new RFunction(); fn.setName("Calculate");
        model.rootElements().add(fn); fn.setParent(model);

        var gm = makeGeneratorModel(model);
        var id = gm.symbolId(fn);
        assertEquals("Calculate", id.getName());
    }

    // === All attributes (inheritance chain) ====================================

    @Test void own_attributes_returned() {
        var dt = new RDataType(); dt.setName("Foo");
        addAttribute(dt, "price", "number");
        addAttribute(dt, "quantity", "int");

        var gm = makeGeneratorModel(makeModel("com.example"));
        var attrs = new ArrayList<>(gm.allAttributes(dt));
        assertEquals(2, attrs.size());
        assertEquals("price", attrs.get(0).name());
        assertEquals("quantity", attrs.get(1).name());
    }

    @Test void inherited_attributes_included() {
        var resolver = new GeneratorTestSymbolResolver();
        var parent = new RDataType(); parent.setName("Parent");
        addAttribute(parent, "id", "string");

        var child = new RDataType(); child.setName("Child");
        resolver.wireSuperType(child, "com.example", "Parent", parent, child::setSuperTypeId);
        addAttribute(child, "value", "number");

        var gm = makeGeneratorModel(makeModel("com.example"));
        var attrs = new ArrayList<>(gm.allAttributes(child));
        assertEquals(2, attrs.size());
        assertEquals("id", attrs.get(0).name());   // inherited first
        assertEquals("value", attrs.get(1).name()); // own second
        Reference.reachabilityFence(resolver);
    }

    @Test void child_overrides_parent_attribute() {
        var resolver = new GeneratorTestSymbolResolver();
        var parent = new RDataType(); parent.setName("Parent");
        addAttribute(parent, "value", "string");

        var child = new RDataType(); child.setName("Child");
        resolver.wireSuperType(child, "com.example", "Parent", parent, child::setSuperTypeId);
        addAttribute(child, "value", "number"); // override

        var gm = makeGeneratorModel(makeModel("com.example"));
        var attrs = new ArrayList<>(gm.allAttributes(child));
        assertEquals(1, attrs.size()); // deduped by name
        // child's version wins (last in LinkedHashMap)
        assertEquals("number", attrs.get(0).typeCall().typeName());
        Reference.reachabilityFence(resolver);
    }

    @Test void three_level_inheritance() {
        var resolver = new GeneratorTestSymbolResolver();
        var gp = new RDataType(); gp.setName("GrandParent");
        addAttribute(gp, "a", "string");

        var p = new RDataType(); p.setName("Parent");
        resolver.wireSuperType(p, "com.example", "GrandParent", gp, p::setSuperTypeId);
        addAttribute(p, "b", "number");

        var c = new RDataType(); c.setName("Child");
        resolver.wireSuperType(c, "com.example", "Parent", p, c::setSuperTypeId);
        addAttribute(c, "c", "int");

        var gm = makeGeneratorModel(makeModel("com.example"));
        var attrs = new ArrayList<>(gm.allAttributes(c));
        assertEquals(3, attrs.size());
        assertEquals("a", attrs.get(0).name());
        assertEquals("b", attrs.get(1).name());
        assertEquals("c", attrs.get(2).name());
        Reference.reachabilityFence(resolver);
    }

    // === All enum values (inheritance chain) ===================================

    @Test void own_enum_values() {
        var en = new REnumeration(); en.setName("Color");
        addEnumValue(en, "Red");
        addEnumValue(en, "Blue");

        var gm = makeGeneratorModel(makeModel("com.example"));
        var values = gm.allValues(en);
        assertEquals(2, values.size());
        assertEquals("Red", values.get(0).name());
    }

    @Test void inherited_enum_values() {
        var parent = new REnumeration(); parent.setName("Base");
        addEnumValue(parent, "Red");

        var child = new REnumeration(); child.setName("Extended");
        addEnumValue(child, "Blue");

        var resolver = new GeneratorTestSymbolResolver();
        resolver.wireSuperType(child, "com.example", "Base", parent, child::setSuperTypeId);

        var gm = makeGeneratorModel(makeModel("com.example"));
        var values = gm.allValues(child);
        assertEquals(2, values.size());
        assertEquals("Red", values.get(0).name());  // inherited first
        assertEquals("Blue", values.get(1).name()); // own second

        Reference.reachabilityFence(resolver);
    }

    // === Version resolution ===================================================

    @Test void version_present() {
        var model = makeModel("com.example");
        model.setVersion("1.2.3");
        var gm = makeGeneratorModel(model);
        assertEquals("1.2.3", gm.version(model));
    }

    /**
     * v3.1 phase C, C0 item 4 (audit N4) — this asserted {@code null}, which was the
     * fork's behaviour and NOT upstream's. Upstream's EMF model carries the default in
     * its declaration ({@code rune-dsl/rune-lang/model/Rosetta.xcore}:
     * {@code class RosettaModel … String version = "0.0.0"}) and the grammar's version
     * clause is optional, so an undeclared model reaches upstream's emitters as
     * {@code "0.0.0"} — never null. The corpus proves it: drr 5.61.0's two
     * {@code techsprint.g20.mas} models declare no version and all four of their
     * goldens carry {@code @version 0.0.0}. Returning null rendered a valueless
     * {@code @version} line, a shape upstream cannot emit. See
     * {@link com.regnosys.rosetta.generator.java.object.ModelVersionDefaultTest}.
     */
    @Test void version_absent_returns_upstream_default() {
        var model = makeModel("com.example");
        var gm = makeGeneratorModel(model);
        assertEquals("0.0.0", gm.version(model));
    }

    // === File filtering =======================================================

    @Test void normal_file_not_ignored() {
        var model = makeModel("com.example");
        model.setSourceRange(SourceRange.of5Arg("cdm-model.rosetta", 1, 1, 100, 1));
        var gm = makeGeneratorModel(model);
        assertFalse(gm.isIgnoredFile(model));
    }

    @Test void basictypes_file_ignored() {
        var model = makeModel("com.rosetta");
        model.setSourceRange(SourceRange.of5Arg("basictypes.rosetta", 1, 1, 100, 1));
        var gm = makeGeneratorModel(model);
        assertTrue(gm.isIgnoredFile(model));
    }

    @Test void annotations_file_ignored() {
        var model = makeModel("com.rosetta");
        model.setSourceRange(SourceRange.of5Arg("annotations.rosetta", 1, 1, 100, 1));
        var gm = makeGeneratorModel(model);
        assertTrue(gm.isIgnoredFile(model));
    }

    @Test void model_no_code_gen_file_ignored() {
        var model = makeModel("com.rosetta");
        model.setSourceRange(SourceRange.of5Arg("model-no-code-gen.rosetta", 1, 1, 100, 1));
        var gm = makeGeneratorModel(model);
        assertTrue(gm.isIgnoredFile(model));
    }

    @Test void file_with_path_prefix_ignored() {
        var model = makeModel("com.rosetta");
        model.setSourceRange(SourceRange.of5Arg("/some/path/to/basictypes.rosetta", 1, 1, 100, 1));
        var gm = makeGeneratorModel(model);
        assertTrue(gm.isIgnoredFile(model));
    }

    @Test void unknown_source_not_ignored() {
        var model = makeModel("com.example");
        // Default SourceRange.NONE has file "<unknown>"
        var gm = makeGeneratorModel(model);
        assertFalse(gm.isIgnoredFile(model));
    }

    // === Type-alias resolution integration =====================================

    @Test void resolveTypeCall_alias_attribute_yields_RAliasType_wrapping_RNumberType() {
        // Per Copilot PR #64 R12 F59 2026-05-14 — integration test for the
        // GeneratorModel.resolveTypeCall(RTypeCall) → TypeAliasSolver.evaluateAliasBody
        // wiring at the codegen entry point. The existing TypeAliasSolverTest tests
        // the helper at the type-system level; this locks the integration point
        // GeneratorModel uses for attribute-type resolution (the P2.1.3 fix locus).
        //
        // Setup: typeAlias MyInt: number(digits: 9, fractionalDigits: 0)
        //        type Holder: multiplier MyInt (1..1)
        // Verify: gm.resolveTypeCall(multiplier.typeCall) → RAliasType("MyInt", {},
        //         RNumberType(of(9), of(0), empty, empty)) with isInteger()==true.
        // Without the alias-branch wiring at GeneratorModel#resolveTypeCall's
        // {@code if (node instanceof RTypeAlias ta)} branch, the attribute would
        // resolve to the unconstrained underlying number rather than the
        // parametric int-typed RNumberType — the exact Cluster F regression.
        var source = """
                namespace test
                typeAlias MyInt:
                    number(digits: 9, fractionalDigits: 0)
                type Holder:
                    multiplier MyInt (1..1)
                """;
        var model = AstBuilder.buildFromString(source, "test.rosetta");
        var linkingResult = RWorkspace.build(List.of(model));
        var gm = new GeneratorModel(linkingResult.workspace());

        var holder = model.rootElements().stream()
                .filter(e -> e instanceof RDataType)
                .map(e -> (RDataType) e)
                .filter(dt -> "Holder".equals(dt.name()))
                .findFirst().orElseThrow();
        var multiplierAttr = holder.attributes().stream()
                .filter(a -> "multiplier".equals(a.name()))
                .findFirst().orElseThrow();

        RType result = gm.resolveTypeCall(multiplierAttr.typeCall());

        assertInstanceOf(RAliasType.class, result,
                "resolveTypeCall(RTypeAlias-typed attribute) must wrap result in RAliasType");
        var aliasType = (RAliasType) result;
        assertEquals("MyInt", aliasType.name());

        assertInstanceOf(RNumberType.class, aliasType.refersTo(),
                "RAliasType refersTo must be the parametric RNumberType built by evaluateAliasBody");
        var num = (RNumberType) aliasType.refersTo();
        assertEquals(9, num.digits().getAsInt(),
                "digits literal from alias body must propagate to underlying RNumberType");
        assertEquals(0, num.fractionalDigits().getAsInt(),
                "fractionalDigits literal from alias body must propagate (the Cluster F root-cause path)");
        assertTrue(num.isInteger(),
                "RAliasType wrapping int-typed RNumberType must satisfy isInteger() for codegen integer routing");

        Reference.reachabilityFence(linkingResult);
    }

    @Test void resolveTypeCall_nested_alias_chain_resolves_through_inner_alias_not_RMissingType() {
        // PR #124 — nested typeAlias chain (alias-of-alias). FpML declares:
        //   typeAlias FpMLVersion: Token
        //   typeAlias Token:       string(minLength: 1)
        // and an attribute `fpmlVersion FpMLVersion (1..1)`. The fork resolved this
        // to Object (109 rune-fpml POJO byte-divergences) because resolveTypeCall's
        // RTypeAlias recovery did BUILTINS.lookup(ta.typeCall().typeName()) — i.e.
        // lookup("Token") — but Token is itself a user typeAlias, NOT a builtin, so
        // the lookup missed and the alias body stayed RMissingType, which
        // JavaTypeTranslator#caseMissingType maps to OBJECT. Upstream resolves the
        // full chain to String. The fix recurses resolveTypeCall(ta.typeCall()) when
        // the builtin recovery misses, collapsing the nested alias to its underlying
        // builtin. Single-hop aliases (Token alone, body typeName "string") already
        // resolved; only the two-hop (alias-of-alias) case was broken.
        var source = """
                namespace test
                typeAlias Inner:
                    string(minLength: 1)
                typeAlias Outer:
                    Inner
                type Holder:
                    value Outer (1..1)
                """;
        var model = AstBuilder.buildFromString(source, "test.rosetta");
        var linkingResult = RWorkspace.build(List.of(model));
        var gm = new GeneratorModel(linkingResult.workspace());

        var holder = model.rootElements().stream()
                .filter(e -> e instanceof RDataType)
                .map(e -> (RDataType) e)
                .filter(dt -> "Holder".equals(dt.name()))
                .findFirst().orElseThrow();
        var valueAttr = holder.attributes().stream()
                .filter(a -> "value".equals(a.name()))
                .findFirst().orElseThrow();

        RType result = gm.resolveTypeCall(valueAttr.typeCall());

        assertInstanceOf(RAliasType.class, result,
                "resolveTypeCall(nested-alias attribute) must wrap result in RAliasType");
        var outer = (RAliasType) result;
        assertEquals("Outer", outer.name());
        // The crux: the nested-alias body must NOT collapse to RMissingType (which
        // JavaTypeTranslator maps to Object); it must resolve through the inner alias.
        assertFalse(outer.refersTo() instanceof RMissingType,
                "nested-alias body must resolve through the inner alias, not fall back to "
                + "RMissingType (which JavaTypeTranslator#caseMissingType maps to Object)");
        // The inner hop is itself an RAliasType wrapping the underlying string builtin,
        // so the whole chain collapses to String for codegen (the byte-parity target).
        assertInstanceOf(RAliasType.class, outer.refersTo(),
                "Outer's body resolves to the inner alias RAliasType(Inner, ...)");
        var inner = (RAliasType) outer.refersTo();
        assertEquals("Inner", inner.name());
        assertInstanceOf(RStringType.class, inner.refersTo(),
                "Inner alias must bottom out at the underlying string builtin (→ Java String, not Object)");

        Reference.reachabilityFence(linkingResult);
    }

    @Test void getType_functionSignatureAlias_underAliasedWildcardImport_resolvesConcreteNotObject() {
        // PR #145 (M7B FUNCTION unpause — return-type-alias-Object facet). A function
        // output/input typed as a workspace type-alias-over-builtin, referenced BARE
        // under an ALIASED wildcard import (`import iso.std.* as iso`), fails scope
        // resolution — the bare name needs the `iso.` prefix — so its
        // typeCall.referencedTypeId() stays empty. The pre-fix resolveTypeCall
        // empty-branch routed to resolveFromWorkspace, which matches only RDataType /
        // REnumeration / RChoice and SKIPS RTypeAlias → RMissingType →
        // JavaTypeTranslator#caseMissingType → Object. This is exactly why the 17 drr
        // projection Get* functions emitted `public Object evaluate(...)` vs golden
        // String/Date (e.g. GetTxId output `uti UTIIdentifier`, with
        // `typeAlias UTIIdentifier: string(pattern: ...)`). The new
        // resolveAliasNodeFromWorkspace branch recovers the concrete builtin via the
        // SAME full alias-body logic (resolveAliasNode) as the resolved-node branch.
        var aliasSrc = """
                namespace iso.std
                typeAlias MyTxId:
                    string(pattern: "[A-Z]{3}")
                typeAlias MyQty:
                    int(digits: 3)
                """;
        var fnSrc = """
                namespace proj.fn
                import iso.std.* as iso

                type Holder:
                    name string (0..1)

                func GetMyTxId:
                    inputs: h Holder (1..1)
                    output: out MyTxId (1..1)
                    set out: "ABC"

                func TakeMyQty:
                    inputs: q MyQty (1..1)
                    output: result string (1..1)
                    set result: "x"
                """;
        var aliasModel = AstBuilder.buildFromString(aliasSrc, "iso-std.rosetta");
        var fnModel = AstBuilder.buildFromString(fnSrc, "proj-fn.rosetta");
        var linkingResult = RWorkspace.build(List.of(aliasModel, fnModel));
        var gm = new GeneratorModel(linkingResult.workspace());

        // (1) The facet: a string-alias OUTPUT must resolve to RAliasType(string),
        //     NOT RMissingType (which JavaTypeTranslator maps to Object).
        var getMyTxId = fnModel.rootElements().stream()
                .filter(e -> e instanceof RFunction f && "GetMyTxId".equals(f.name()))
                .map(e -> (RFunction) e).findFirst().orElseThrow();
        // PRECONDITION (Copilot R1): the bare alias OUTPUT under an aliased wildcard import
        // must be UNLINKED — typeCall.referencedTypeId() empty — so getType routes through the
        // PR #145 empty-referencedTypeId branch (resolveAliasNodeFromWorkspace), NOT the
        // resolved-node RTypeAlias branch. Without this guard, a future linker change that
        // resolved bare-aliased names would silently route around the fix yet keep the test green.
        RAttribute outAttr = getMyTxId.output().orElseThrow();
        assertTrue(outAttr.typeCall().referencedTypeId().isEmpty(),
                "PRECONDITION: the bare alias output under an aliased wildcard import must have an "
                + "empty referencedTypeId — the empty-branch path the PR #145 fix targets");
        RType outType = gm.getType(outAttr);
        assertFalse(outType instanceof RMissingType,
                "alias-over-builtin OUTPUT under an aliased wildcard import must NOT fall back "
                + "to RMissingType (→ Object); it must resolve to the concrete builtin");
        assertInstanceOf(RAliasType.class, outType,
                "MyTxId output must resolve to RAliasType, not RMissingType");
        assertInstanceOf(RStringType.class, ((RAliasType) outType).refersTo(),
                "MyTxId (string-pattern alias) must recover to the string builtin (→ Java String)");

        // (2) Load-bearing constraint (refute lens — number-alias inputs): an
        //     int-constrained alias INPUT must resolve through the FULL alias-body logic
        //     to the parametric int-typed RNumberType (→ Integer), NOT the unconstrained
        //     string base a pattern-string-only recovery would have yielded.
        var takeMyQty = fnModel.rootElements().stream()
                .filter(e -> e instanceof RFunction f && "TakeMyQty".equals(f.name()))
                .map(e -> (RFunction) e).findFirst().orElseThrow();
        RAttribute qtyAttr = takeMyQty.inputs().get(0);
        assertTrue(qtyAttr.typeCall().referencedTypeId().isEmpty(),
                "PRECONDITION: the bare alias input must also be UNLINKED so this leg exercises "
                + "the same empty-referencedTypeId fallback (not the resolved-node branch)");
        RType qtyType = gm.getType(qtyAttr);
        assertInstanceOf(RAliasType.class, qtyType, "MyQty input alias must resolve to RAliasType");
        assertInstanceOf(RNumberType.class, ((RAliasType) qtyType).refersTo(),
                "MyQty (int alias) must recover to a parametric RNumberType, not the string base");
        assertTrue(((RNumberType) ((RAliasType) qtyType).refersTo()).isInteger(),
                "MyQty int(digits: 3) alias must be integer-typed so codegen routes to Integer, not Object/String");

        Reference.reachabilityFence(linkingResult);
    }

    @Test void resolveTypeCall_calculation_alias_attribute_yields_RAliasType_wrapping_RStringType() {
        // P2.1.3b T2 acceptance #14 — α regression FENCE test extending the
        // {@code resolveTypeCall_alias_attribute_yields_RAliasType_wrapping_RNumberType}
        // pattern above.
        //
        // SCOPE — what this test DOES lock:
        // The alias-name preservation invariant for calculation/productType/eventType
        // string aliases — that gm.resolveTypeCall(attr) returns RAliasType wrapping
        // RStringType with the alias name preserved (per D36 invariant #1).
        //
        // SCOPE — what this test DOES NOT lock:
        // The Case B bypass branch at GeneratorModel#resolveTypeCall (the
        // {@code if (primary == RMissingType.INSTANCE)} block) itself.
        // The synthetic AST path here resolves to RTypeAlias naturally (inline
        // typeAlias parses to that node), so this test exercises Case A (the
        // {@code if (node instanceof RTypeAlias ta)} branch) rather than the
        // Case B bypass. The 14-cell D11 run at T5 is the ground-truth
        // discriminator that confirms Case B behaviour against the real-parser
        // AST path (where the 25-entry α retirement was empirically verified).
        // Per the development audit "cluster-f-residual-T0-spike" § 4 +
        // the development audit "cluster-f-fix-evidence" § 7.1.
        //
        // Why a synthetic-AST fence is still valuable: it protects the
        // alias-name-preservation invariant against future refactors of
        // resolveTypeCall's RTypeAlias Case A branch, which is the path that
        // synthetic ASTs (e.g. from M4 ast.builder) reach.
        var source = """
                namespace test
                typeAlias calculation:
                    string
                type Holder:
                    formula calculation (1..1)
                """;
        var model = AstBuilder.buildFromString(source, "test.rosetta");
        var linkingResult = RWorkspace.build(List.of(model));
        var gm = new GeneratorModel(linkingResult.workspace());

        var holder = model.rootElements().stream()
                .filter(e -> e instanceof RDataType)
                .map(e -> (RDataType) e)
                .filter(dt -> "Holder".equals(dt.name()))
                .findFirst().orElseThrow();
        var formulaAttr = holder.attributes().stream()
                .filter(a -> "formula".equals(a.name()))
                .findFirst().orElseThrow();

        RType result = gm.resolveTypeCall(formulaAttr.typeCall());

        assertInstanceOf(RAliasType.class, result,
                "resolveTypeCall(calculation-typed attribute) must wrap result in RAliasType, not fall through to Object");
        var aliasType = (RAliasType) result;
        assertEquals("calculation", aliasType.name(),
                "alias name must be preserved as `calculation`, not the underlying `string` builtin");
        assertInstanceOf(RStringType.class, aliasType.refersTo(),
                "RAliasType refersTo must be RStringType (the calculation/productType/eventType target per TYPE_ALIAS_TO_BUILTIN map)");

        Reference.reachabilityFence(linkingResult);
    }

    @Test void resolveTypeCall_choice_attribute_yields_RChoice() {
        // P2.1.3c T2 acceptance — verify the type resolver layer correctly returns
        // RChoiceTypeRef for a choice option's typeCall. β1 was an emission bug
        // (ModelObjectGenerator's streamObjects() skipped RChoice root elements
        // entirely); resolveTypeCall itself has always worked for choices. This
        // fence locks that invariant against future refactors.
        var source = """
                namespace test
                type Cash:
                    amount string (1..1)
                type Commodity:
                    symbol string (1..1)
                choice Asset:
                    Cash
                    Commodity
                type Holder:
                    asset Asset (1..1)
                """;
        var model = AstBuilder.buildFromString(source, "test.rosetta");
        var linkingResult = RWorkspace.build(List.of(model));
        var gm = new GeneratorModel(linkingResult.workspace());

        var holder = model.rootElements().stream()
                .filter(e -> e instanceof RDataType)
                .map(e -> (RDataType) e)
                .filter(dt -> "Holder".equals(dt.name()))
                .findFirst().orElseThrow();
        var assetAttr = holder.attributes().stream()
                .filter(a -> "asset".equals(a.name()))
                .findFirst().orElseThrow();

        RType result = gm.resolveTypeCall(assetAttr.typeCall());

        assertInstanceOf(RChoiceTypeRef.class, result,
                "resolveTypeCall(choice-typed attribute) must return RChoiceTypeRef, not RMissingType");
        var choiceRef = (RChoiceTypeRef) result;
        assertEquals("Asset", choiceRef.name(),
                "RChoiceTypeRef name must be `Asset`");

        Reference.reachabilityFence(linkingResult);
    }

    @Test void resolveTypeCall_list_of_data_type_attribute_yields_RDataTypeRef_not_RMissingType() {
        // P2.1.3c T3 acceptance #17 — 4th sub-cause integration test.
        // The Cluster F 4th residual sub-cause is `<UserDataType> (0..*)` typed
        // attributes emitting `List<Object> getX()` instead of `List<? extends X>`.
        //
        // T3.0 diagnostic finding: list cardinality is metadata on RAttribute
        // (not the type), so `resolveTypeCall(typeCall)` returns the ELEMENT type
        // (not RList). The bug therefore surfaces as `RMissingType` being returned
        // for the element of a list-of-user-data-type attribute under
        // corpus-loaded conditions; JavaTypeTranslator.caseMissingType maps that
        // to OBJECT and the list-wrapper at RJavaPojoInterface.initializeProperties
        // produces `List<Object>` instead of `List<? extends UserDataType>`.
        //
        // SCOPE — what this test DOES lock:
        // The invariant that `resolveTypeCall` MUST return the resolved element
        // RType (RDataTypeRef wrapping the user-defined RDataType — `Item` here)
        // and MUST NOT return RMissingType for a list-of-data-type attribute. This
        // is the precondition for the downstream list wrapping to emit
        // `List<? extends Item>` rather than `List<Object>`.
        //
        // Per P2.1.3b α Case B precedent (cluster-f-residual-T0-spike.md § 4),
        // synthetic-AST + real-parser AST paths can diverge — the 14-cell D11
        // run at T6 is the ground-truth discriminator for the corpus emission
        // shape. This unit fence locks the resolution invariant.
        var source = """
                namespace test
                type Item:
                    value string (1..1)
                type Container:
                    items Item (0..*)
                """;
        var model = AstBuilder.buildFromString(source, "test.rosetta");
        var linkingResult = RWorkspace.build(List.of(model));
        var gm = new GeneratorModel(linkingResult.workspace());

        var container = model.rootElements().stream()
                .filter(e -> e instanceof RDataType)
                .map(e -> (RDataType) e)
                .filter(dt -> "Container".equals(dt.name()))
                .findFirst().orElseThrow();
        var itemsAttr = container.attributes().stream()
                .filter(a -> "items".equals(a.name()))
                .findFirst().orElseThrow();

        RType result = gm.resolveTypeCall(itemsAttr.typeCall());

        assertNotEquals(RMissingType.INSTANCE, result,
                "resolveTypeCall(list-of-data-type attribute) must NOT return RMissingType — "
                + "RMissingType maps to Object in JavaTypeTranslator and produces "
                + "List<Object> downstream (the 4th sub-cause root cause)");
        assertInstanceOf(RDataTypeRef.class, result,
                "resolveTypeCall(list-of-data-type attribute) must return RDataTypeRef "
                + "wrapping the user-defined RDataType `Item`, not Object/RMissingType");
        var dtRef = (RDataTypeRef) result;
        assertEquals("Item", dtRef.name(),
                "RDataTypeRef name must preserve the user-defined type name `Item` "
                + "so downstream list wrapping emits List<? extends Item>");

        Reference.reachabilityFence(linkingResult);
    }

    @Test void resolveTypeCall_list_of_type_alias_attribute_yields_RAliasType_element() {
        // P2.1.3c T5 acceptance #18 — latent list-of-type-alias-to-Object integration test.
        //
        // The latent Cluster F sub-cause manifests in drr corpus as `<TypeAlias> (0..*)`
        // typed attributes emitting `List<Object> getX()` instead of `List<String>`
        // (or whatever the alias's underlying primitive type is). Six entries in drr/6.29.0
        // POJO + drr/7.0.0-dev.113 POJO surfaced at P2.1.3b T5 with first-diff at
        // `productGrade Max50Text (0..*)` (where Max50Text is a string alias).
        //
        // T5 extends T3's workspace-search fallback at GeneratorModel.resolveTypeCall
        // symmetrically for RTypeAlias element types: when the standard resolution
        // path falls through to RMissingType for a list-of-alias attribute under
        // corpus loading, search the loaded workspace's models for a matching
        // RTypeAlias by name and return an RAliasType wrapping the evaluated
        // alias body (via TypeAliasSolver.evaluateAliasBody, same as Case A —
        // the {@code if (node instanceof RTypeAlias ta)} branch in resolveTypeCall).
        //
        // SCOPE — what this test DOES lock:
        // The invariant that `resolveTypeCall(list-of-type-alias)` MUST return the
        // resolved element RType (RAliasType wrapping the underlying parametric type)
        // and MUST NOT return RMissingType. This is the precondition for the downstream
        // list wrapping to emit `List<UnderlyingType>` rather than `List<Object>`.
        //
        // Per P2.1.3b α Case B + P2.1.3c T3 Locus 1 precedents
        // (cluster-f-residual-T0-spike.md § 4 + cluster-f-final-re-audit.md § 1.2),
        // synthetic-AST + real-parser AST paths can diverge — the 14-cell D11 drr run
        // at T6 is the ground-truth discriminator for the corpus emission shape. This
        // unit fence locks the resolution invariant under the synthetic Case A path
        // (which the workspace-search fallback also flows through after the T3
        // empty-Optional + Case B bypass extensions in resolveTypeCall).
        var source = """
                namespace test
                typeAlias Max50Text:
                    string(minLength: 1, maxLength: 50)
                type Container:
                    productGrade Max50Text (0..*)
                """;
        var model = AstBuilder.buildFromString(source, "test.rosetta");
        var linkingResult = RWorkspace.build(List.of(model));
        var gm = new GeneratorModel(linkingResult.workspace());

        var container = model.rootElements().stream()
                .filter(e -> e instanceof RDataType)
                .map(e -> (RDataType) e)
                .filter(dt -> "Container".equals(dt.name()))
                .findFirst().orElseThrow();
        var productGradeAttr = container.attributes().stream()
                .filter(a -> "productGrade".equals(a.name()))
                .findFirst().orElseThrow();

        RType result = gm.resolveTypeCall(productGradeAttr.typeCall());

        assertNotEquals(RMissingType.INSTANCE, result,
                "resolveTypeCall(list-of-type-alias attribute) must NOT return RMissingType — "
                + "RMissingType maps to Object in JavaTypeTranslator and produces "
                + "List<Object> downstream (the latent sub-cause root cause)");
        assertInstanceOf(RAliasType.class, result,
                "resolveTypeCall(list-of-type-alias attribute) must return RAliasType "
                + "wrapping the user-defined alias `Max50Text`, not Object/RMissingType");
        var aliasResult = (RAliasType) result;
        assertEquals("Max50Text", aliasResult.name(),
                "RAliasType name must preserve the user-defined alias name `Max50Text` "
                + "so downstream list wrapping emits List<String> (the alias underlying)");
        assertInstanceOf(RStringType.class, aliasResult.refersTo(),
                "RAliasType refersTo must be the parametric RStringType built by "
                + "evaluateAliasBody (alias-body argument propagation from minLength/maxLength)");

        Reference.reachabilityFence(linkingResult);
    }

    // === Helpers ==============================================================

    private RModel makeModel(String namespace) {
        var model = new RModel();
        model.setNamespace(namespace);
        return model;
    }

    private GeneratorModel makeGeneratorModel(RModel model) {
        var result = RWorkspace.build(List.of(model));
        return new GeneratorModel(result.workspace());
    }

    private void addAttribute(RDataType dt, String name, String typeName) {
        var attr = new RAttribute();
        attr.setName(name);
        var tc = new RTypeCall(); tc.setTypeName(typeName);
        attr.setTypeCall(tc);
        dt.attributes().add(attr);
    }

    private void addEnumValue(REnumeration en, String name) {
        var val = new REnumValue();
        val.setName(name);
        en.values().add(val);
    }
}
