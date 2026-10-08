package com.regnosys.rosetta.generator.java.ir;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.annotations.RAnnotationPathExpression;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.annotations.RLabelAnnotation;
import com.regnosys.rosetta.ast.annotations.RRuleReferenceAnnotation;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.regulatory.RCorpus;
import com.regnosys.rosetta.ast.regulatory.RDocReference;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.supporting.RTypeCallArgument;
import com.regnosys.rosetta.ast.supporting.RTypeCallArgumentExpression;
import com.regnosys.rosetta.ast.supporting.RTypeParameter;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.util.ModelGeneratorUtil;
import com.regnosys.rosetta.ir.adapter.AstToIRAdapter;
import com.regnosys.rosetta.ir.adapter.IREnumNode;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.IRAnnotationPath;
import com.regnosys.rosetta.ir.core.IRAnnotationUse;
import com.regnosys.rosetta.ir.core.IRBounds;
import com.regnosys.rosetta.ir.core.IRDocReference;
import com.regnosys.rosetta.ir.core.IREffectiveBase;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.IRTypeArgument;
import com.regnosys.rosetta.ir.core.IRTypeParameter;
import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RMissingType;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RRecordType;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.alias.TypeAliasSolver;
import com.regnosys.rosetta.types.builtin.BuiltinTypeRegistry;

/**
 * THE DECLARATION RECONCILE (v3.3 seat 5, decision D55): every fact the enriched declaration IR carries is
 * ASSERTED, per declaration, against the parsed source - and, for a type reference, against the OLD
 * GENERATOR'S OWN resolution ({@link GeneratorModel#resolveTypeCall}), the oracle the emitted bytes already
 * agree with. One reconciler serves the three IR-routed declaration generators; each holds its own
 * instance, so its counters read that generator's pass alone.
 *
 * <p>The reads here never go through {@link AstToIRAdapter}: the reconciler goes to the AST accessors (and to
 * the generator model) itself and never asks the adapter what it would have produced, so an adapter that
 * DROPS or mangles a fact is RED here (the mutation lanes prove it, a lane per fact family). HOW FAR that
 * independence reaches, said plainly: a type reference's KIND and its declaration are anchored to a SECOND
 * oracle (the old generator's own resolution); a reference's namespace and own name are read on both sides
 * from the same {@code referencedType()} link, and a corpus from the same {@code lookupCorpus} producer - a
 * dropped resolution is red, a resolution the shared producer itself gets wrong cannot be.
 *
 * <p>THE TYPE GATE (v3.3 seat 7, PR #643) added three fact families to every type reference, and a fourth
 * declaration kind. Per reference: {@code .resolution} (WHICH LEG answered - a declaration, the builtin registry, or
 * nothing; the old generator's three UNLINKED workspace fallbacks therefore read RED by name - {@code workspace-fallback}
 * against the IR's {@code unresolved} - because the IR route refuses to type a name the linker never resolved; its
 * post-resolution bypass answers a name the linker DID resolve, so {@code .resolution} reads {@code declaration} on both
 * halves there and the bypass is red on {@code .kind} / {@code .legacyDeclaration} and on {@code .javaType} instead),
 * {@code .javaType}
 * ({@link IRJavaTypeNames#of} from the IR facts ALONE equals {@code JavaTypeTranslator.toJavaReferenceType} of the old
 * generator's own resolution - a refusal on either side reads {@code REFUSED}), and, on a {@code typeAlias}
 * reference, the {@code .effectiveBase.*} of the collapsed chain. The collapse law is IMPLEMENTED HERE
 * ({@link #collapse}) rather than called on the adapter, so the two halves are two producers that must agree (LAW 69);
 * a number BELT ({@code .effectiveBase.number}) anchors the collapsed digits / fractionalDigits / min / max to a
 * THIRD producer, the parser's own {@code TypeAliasSolver.evaluateForward} over the old generator's answer. String
 * constraints have no such belt - the old generator's recovery arm DROPS them - so they stay anchored to the source
 * walk alone, which is stated rather than implied. {@link #reconcileTypeAlias} is the fourth kind: the
 * {@code typeAlias} DECLARATION itself, its parameters and its body.
 *
 * <p>The counters - declarations ATTEMPTED, facts asserted, mismatches - are what the D11 host prints per
 * cell BEFORE it asserts {@code mismatches == 0}. A declaration is counted by {@link #attempt()} BEFORE its
 * adapter runs and a throw on the way is booked by {@link #threw()} as a mismatch, so a declaration can never
 * leave the population silently - the host holds {@code declarations} equal to ITS OWN count of the cell's
 * emitted declarations (LAW 84: the gate asserts its own population).
 */
final class IRDeclarationReconciler {

    /** The shared builtin table - a pure by-NAME lookup, read by this reconciler's OWN alias-chain walk. */
    private static final BuiltinTypeRegistry BUILTINS = BuiltinTypeRegistry.createDefault();

    /** The parser's own alias solver - read ONLY by the number BELT, never by the collapse walk below. */
    private static final TypeAliasSolver SOLVER = new TypeAliasSolver();

    /** The alias-chain walk's depth bound - the parser's own ({@code TypeAliasSolver.MAX_DEPTH}). */
    private static final int MAX_ALIAS_DEPTH = 100;

    /**
     * The order a BASIC_TYPE / RECORD_TYPE leaf's known parameters are stated in. Both halves of the Java-type
     * reconcile walk the chain independently and must agree on the argument LIST, so the order is CANONICAL rather
     * than the body's; an argument no builtin knows follows, in body order.
     */
    private static final List<String> CANONICAL_PARAMETERS =
            List.of("digits", "fractionalDigits", "min", "max", "minLength", "maxLength", "pattern");

    private final GeneratorModel generatorModel;
    private final ModelGeneratorUtil corpusLookup;
    private final AstToIRAdapter adapter;
    private final JavaTypeTranslator typeTranslator;
    private final AtomicInteger declarations = new AtomicInteger();
    private final AtomicInteger facts = new AtomicInteger();
    private final AtomicInteger mismatches = new AtomicInteger();

    IRDeclarationReconciler(GeneratorModel generatorModel) {
        this(generatorModel, Optional.empty());
    }

    /**
     * A reconciler that SHARES another's adapter (PR #644, the banked cq NIT-1 of #643): the type-alias pass of
     * {@link IRModelObjectGenerator} reconciles a second population through a second set of counters, but one
     * {@code CorpusResolver} must serve both halves - so the second reconciler is built over the first one's adapter
     * instead of building an adapter it never reads.
     */
    IRDeclarationReconciler(GeneratorModel generatorModel, AstToIRAdapter sharedAdapter) {
        this(generatorModel, Optional.of(Objects.requireNonNull(sharedAdapter, "sharedAdapter")));
    }

    private IRDeclarationReconciler(GeneratorModel generatorModel, Optional<AstToIRAdapter> sharedAdapter) {
        this.generatorModel = Objects.requireNonNull(generatorModel, "generatorModel");
        this.corpusLookup = new ModelGeneratorUtil(generatorModel.workspace());
        // the OLD GENERATOR'S OWN Java-type answer, the oracle the emitted bytes already agree with (the D4 table)
        this.typeTranslator = new JavaTypeTranslator(new JavaTypeUtil());
        // ONE producer for the corpus scope: the javadoc renderer's own lookup (LAW 69 - the two halves agree); and
        // ONE producer for an EXPRESSION's type: the workspace's own fixed-point engine (v3.3 seat 8, PR #644 - a
        // `with-meta` use's wrapper is decided by its ARGUMENT's type, and an adapter built without that seam states a
        // `missing` refusal rather than guessing, so the seam is supplied HERE, where the host's GeneratorModel is in hand)
        this.adapter = sharedAdapter.orElseGet(
                () -> new AstToIRAdapter((ref, name) -> Optional.ofNullable(corpusLookup.lookupCorpus(ref, name)),
                        generatorModel.workspace()::getInferredType));
    }

    AstToIRAdapter adapter() {
        return adapter;
    }

    /** Books ONE declaration as attempted - called BEFORE the adapter runs, so a throwing declaration is still counted. */
    void attempt() {
        declarations.incrementAndGet();
    }

    /** An adapter (or reconcile) throw IS a mismatch - never a silent drop from the counters. */
    void threw() {
        mismatches.incrementAndGet();
    }

    /** {@code {declarations attempted, facts asserted, mismatches}} of this generator's passes so far. */
    int[] stats() {
        return new int[] {declarations.get(), facts.get(), mismatches.get()};
    }

    // ------------------------------------------------------------------------------------------------ data

    List<String> reconcileData(String namespace, RDataType ast, IRTypeNode ir) {
        Check c = new Check("STRUCT " + ir.name());
        c.same("kind", IRKind.STRUCT, ir.kind());
        c.same("name", qualified(namespace, ast.name()), ir.name());
        c.same("namespace", Optional.ofNullable(namespace), ir.namespace());
        c.same("definition", ast.definition(), ir.definition());
        c.same("conditionNames", ast.conditions().stream().map(x -> x.name()).toList(), ir.conditionNames());
        annotations(c, "annotations", ast.annotationRefs(), ir.annotations());
        docReferences(c, "docReferences", ast.docReferences(), ir.docReferences());
        c.same("baseType.present", ast.superTypeName().isPresent(), ir.baseType().isPresent());
        if (ast.superTypeName().isPresent() && ir.baseType().isPresent()) {
            IRType base = ir.baseType().get();
            c.same("baseType.name", ast.superTypeName().get(), base.name());
            Optional<? extends RNode> target = quiet(ast::choiceSuperType);
            IRKind expected = IRKind.CHOICE;
            if (target.isEmpty()) {
                target = quiet(ast::superType);
                expected = IRKind.STRUCT;
            }
            c.same("baseType.kind", expected, base.kind());
            c.same("baseType.namespace", target.flatMap(IRDeclarationReconciler::namespaceOf), base.namespace());
            c.same("baseType.resolvedName", target.map(IRDeclarationReconciler::simpleName), base.resolvedName());
        }
        c.same("fields.size", ast.attributes().size(), ir.fields().size());
        for (int i = 0; i < Math.min(ast.attributes().size(), ir.fields().size()); i++) {
            attribute(c, ast.attributes().get(i), ir.fields().get(i));
        }
        return c.close();
    }

    private void attribute(Check c, RAttribute ast, IRField ir) {
        String at = "field " + ast.name() + ".";
        c.same(at + "name", ast.name(), ir.name());
        typeReference(c, at + "type", ast.typeCall(), ir.type(), ir.typeArguments());
        c.same(at + "bounds", ast.cardinality().map(IRDeclarationReconciler::bounds), ir.bounds());
        c.same(at + "cardinality", bucket(ast.cardinality()), ir.cardinality().name());
        c.same(at + "override", ast.isOverride(), ir.isOverride());
        typeArguments(c, at + "typeArguments", ast.typeCall(), ir);
        c.same(at + "definition", ast.definition(), ir.definition());
        annotations(c, at + "annotations", ast.annotationRefs(), ir.annotations());
        docReferences(c, at + "docReferences", ast.docReferences(), ir.docReferences());
        labels(c, at + "labels", ast.labelAnnotations(), ir);
        ruleReferences(c, at + "ruleReferences", ast.ruleReferenceAnnotations(), ir);
    }

    // ---------------------------------------------------------------------------------------------- choice

    List<String> reconcileChoice(String namespace, RChoice ast, IRTypeNode ir) {
        Check c = new Check("CHOICE " + ir.name());
        c.same("kind", IRKind.CHOICE, ir.kind());
        c.same("name", qualified(namespace, ast.name()), ir.name());
        c.same("namespace", Optional.ofNullable(namespace), ir.namespace());
        c.same("definition", ast.definition(), ir.definition());
        annotations(c, "annotations", ast.annotationRefs(), ir.annotations());
        // a choice declares neither doc references nor conditions: the IR must say so too (constants, asserted)
        c.same("docReferences", List.of(), ir.docReferences());
        c.same("conditionNames", List.of(), ir.conditionNames());
        c.same("options.size", ast.options().size(), ir.fields().size());
        for (int i = 0; i < Math.min(ast.options().size(), ir.fields().size()); i++) {
            RChoiceOption option = ast.options().get(i);
            IRField field = ir.fields().get(i);
            String at = "option " + i + ".";
            c.same(at + "name", option.typeCall() == null ? "" : option.typeCall().typeName(), field.name());
            typeReference(c, at + "type", option.typeCall(), field.type(), field.typeArguments());
            c.same(at + "bounds", Optional.empty(), field.bounds());
            c.same(at + "cardinality", "ONE_TO_ONE", field.cardinality().name());   // an option has no cardinality: exactly one
            c.same(at + "override", false, field.isOverride());
            typeArguments(c, at + "typeArguments", option.typeCall(), field);
            c.same(at + "definition", option.definition(), field.definition());
            annotations(c, at + "annotations", option.annotationRefs(), field.annotations());
            docReferences(c, at + "docReferences", option.docReferences(), field.docReferences());
            labels(c, at + "labels", option.labelAnnotations(), field);
            ruleReferences(c, at + "ruleReferences", option.ruleReferenceAnnotations(), field);
        }
        return c.close();
    }

    // ------------------------------------------------------------------------------------------- type alias

    /**
     * THE {@code typeAlias} DECLARATION (v3.3 seat 7, PR #643 - the type gate): an alias declares no field and no
     * annotation; what it carries is its declared PARAMETERS, its BODY as a type reference, the arguments the body
     * wrote AS WRITTEN, its conditions and its documentation. The declaration node itself states NO effective base -
     * the collapse is a property of a USE SITE - and that absence is asserted here rather than assumed.
     *
     * <p>The body reference's own use-site arguments are the body's arguments: {@code typeAlias Scaled(scale int):
     * number(digits: 10, fractionalDigits: scale)} types its body as {@code number} with {@code digits: 10} in force
     * and {@code fractionalDigits} unbound - a name value is simply not a literal, and binds nothing.
     */
    List<String> reconcileTypeAlias(String namespace, RTypeAlias ast, IRTypeNode ir) {
        Check c = new Check("TYPE_ALIAS " + ir.name());
        c.same("kind", IRKind.TYPE_ALIAS, ir.kind());
        c.same("name", qualified(namespace, ast.name()), ir.name());
        c.same("namespace", Optional.ofNullable(namespace), ir.namespace());
        c.same("definition", ast.definition(), ir.definition());
        c.same("conditionNames", ast.conditions().stream().map(x -> x.name()).toList(), ir.conditionNames());
        // the grammar allows an alias neither an annotation nor a doc reference: the IR must say so too (constants, asserted)
        c.same("annotations", List.of(), ir.annotations());
        c.same("docReferences", List.of(), ir.docReferences());
        c.same("fields.size", 0, ir.fields().size());
        // the collapse is a use site's fact - a DECLARATION never carries one
        c.same("effectiveBase.present", false, ir.effectiveBase().isPresent());
        c.same("typeParameters.size", ast.typeParameters().size(), ir.typeParameters().size());
        for (int i = 0; i < Math.min(ast.typeParameters().size(), ir.typeParameters().size()); i++) {
            RTypeParameter parameter = ast.typeParameters().get(i);
            IRTypeParameter irParameter = ir.typeParameters().get(i);
            String at = "param " + parameter.name() + ".";
            c.same(at + "name", parameter.name(), irParameter.name());
            // the parameter's OWN type call's arguments are its use-site arguments (PR #644, the banked cq SF-1 of #643):
            // `n number(fractionalDigits: 0)` types Integer on the old generator's side, and the IR side must read the
            // same arguments to agree - before this fact they were reconciled against List.of() and lost silently
            typeReference(c, at + "type", parameter.typeCall(), irParameter.type(), irParameter.typeArguments());
            typeArguments(c, at + "typeArguments", parameter.typeCall(), irParameter.typeArguments());
            c.same(at + "definition", parameter.definition(), irParameter.definition());
        }
        c.same("baseType.present", ast.typeCall() != null, ir.baseType().isPresent());
        if (ast.typeCall() != null && ir.baseType().isPresent()) {
            typeReference(c, "baseType", ast.typeCall(), ir.baseType().get(), ir.baseTypeArguments());
        }
        typeArguments(c, "baseTypeArguments", ast.typeCall(), ir.baseTypeArguments());
        return c.close();
    }

    // ------------------------------------------------------------------------------------------------ enum

    List<String> reconcileEnum(String namespace, REnumeration ast, IREnumNode ir) {
        Check c = new Check("ENUM " + ir.name());
        c.same("kind", IRKind.ENUM, ir.kind());
        c.same("name", qualified(namespace, ast.name()), ir.name());
        c.same("namespace", Optional.ofNullable(namespace), ir.namespace());
        c.same("definition", ast.definition(), ir.definition());
        annotations(c, "annotations", ast.annotationRefs(), ir.annotations());
        docReferences(c, "docReferences", ast.docReferences(), ir.docReferences());
        c.same("parent.present", ast.superTypeName().isPresent(), ir.parent().isPresent());
        if (ast.superTypeName().isPresent() && ir.parent().isPresent()) {
            IRType parent = ir.parent().get();
            Optional<REnumeration> target = quiet(ast::superType);
            c.same("parent.name", ast.superTypeName().get(), parent.name());
            c.same("parent.kind", IRKind.ENUM, parent.kind());
            c.same("parent.namespace", target.flatMap(IRDeclarationReconciler::namespaceOf), parent.namespace());
            c.same("parent.resolvedName", target.map(REnumeration::name), parent.resolvedName());
        }
        c.same("values.size", ast.values().size(), ir.values().size());
        for (int i = 0; i < Math.min(ast.values().size(), ir.values().size()); i++) {
            REnumValue value = ast.values().get(i);
            IREnumValue irValue = ir.values().get(i);
            String at = "value " + value.name() + ".";
            c.same(at + "name", value.name(), irValue.name());
            c.same(at + "displayName", value.displayName(), irValue.displayName());
            c.same(at + "definition", value.definition(), irValue.definition());
            annotations(c, at + "annotations", value.annotationRefs(), irValue.annotations());
            docReferences(c, at + "docReferences", value.docReferences(), irValue.docReferences());
            c.same(at + "synonyms", value.synonyms().stream().map(s -> List.of(s.sources(), s.value() == null ? "" : s.value(), s.definitionText(),
                            s.patternMatch(), s.patternReplace(), s.isRemoveHtml())).toList(),
                    irValue.synonyms().stream().map(s -> List.of(s.sources(), s.value(), s.definition(),
                            s.patternMatch(), s.patternReplace(), s.removeHtml())).toList());
        }
        return c.close();
    }

    // --------------------------------------------------------------------------------------- the shared facts

    /**
     * A type reference against the OLD GENERATOR'S resolution: the class of {@link RType} it resolves the call
     * to is the IR reference's kind, and the declaration behind it gives the namespace and the own name.
     *
     * <p>THE TYPE GATE (v3.3 seat 7, PR #643) adds, after those: {@code .resolution} - the LEG that answered, so an
     * old-generator workspace fallback reads red by name; {@code .javaType} - the emitter-side derivation from the IR
     * facts alone against the old generator's D4 table; and, when the call resolves to a {@code typeAlias}, the
     * {@code .effectiveBase.*} of the chain COLLAPSED HERE, by this class's own walk, plus the number belt.
     *
     * @param useSiteArguments the arguments the IR carries at THIS use site - what an IR-routed emitter would pass
     *     to {@link IRJavaTypeNames#of} and therefore what the reconcile must judge
     */
    private void typeReference(Check c, String at, RTypeCall call, IRType ir, List<IRTypeArgument> useSiteArguments) {
        if (call == null) {
            c.same(at + ".name", "", ir.name());
            return;
        }
        c.same(at + ".name", call.typeName(), ir.name());
        RType legacy = generatorModel.resolveTypeCall(call);
        Optional<? extends RNode> declaration = quiet(call::referencedType);
        c.same(at + ".kind", legacyKind(legacy, declaration), ir.kind());
        c.same(at + ".namespace", declaration.flatMap(IRDeclarationReconciler::namespaceOf), ir.namespace());
        // a name only the old generator's BUILTIN fallback places (a workspace without the model library) is named as written
        Optional<String> expectedName = declaration.map(IRDeclarationReconciler::simpleName);
        if (declaration.isEmpty() && isBuiltin(legacy)) {
            expectedName = Optional.of(call.typeName());
        }
        c.same(at + ".resolvedName", expectedName, ir.resolvedName());
        if (legacy instanceof RDataTypeRef ref) {
            c.same(at + ".legacyDeclaration", Optional.of(ref.astNode()), declaration.map(n -> (Object) n));
        } else if (legacy instanceof REnumTypeRef ref) {
            c.same(at + ".legacyDeclaration", Optional.of(ref.astNode()), declaration.map(n -> (Object) n));
        } else if (legacy instanceof RChoiceTypeRef ref && ref.astNode() != null) {
            c.same(at + ".legacyDeclaration", Optional.of(ref.astNode()), declaration.map(n -> (Object) n));
        }
        c.same(at + ".resolution", legacyResolution(legacy, declaration), irResolution(ir));
        c.same(at + ".javaType", legacyJavaType(legacy), irJavaType(ir, useSiteArguments));
        if (declaration.isPresent() && declaration.get() instanceof RTypeAlias alias) {
            effectiveBase(c, at, alias, call, legacy, ir);
        }
    }

    // ------------------------------------------------------------------------------ the type gate's three families

    /**
     * WHICH LEG answered the reference. The old generator: its DECLARATION (the linker resolved it - the post-resolution
     * bypass fires INSIDE that branch, so it reads {@code declaration} here too and is caught on {@code .kind} /
     * {@code .legacyDeclaration} / {@code .javaType}, never on this fact), else the shared builtin REGISTRY by name, else
     * nothing at all - else one of its three UNLINKED workspace fallbacks (the three-entry alias table, the simple-name
     * first match, the alias search), which this names {@code workspace-fallback} and the IR route refuses to reproduce
     * (PR #643 round 2, SF-2). The IR: a namespace means a declaration, a resolved name WITHOUT one means the registry,
     * neither means unresolved.
     */
    private static String legacyResolution(RType legacy, Optional<? extends RNode> declaration) {
        if (declaration.isPresent()) {
            return "declaration";
        }
        if (isBuiltin(legacy)) {
            return "registry";
        }
        return legacy instanceof RMissingType ? "unresolved" : "workspace-fallback";
    }

    private static String irResolution(IRType ir) {
        if (ir.namespace().isPresent()) {
            return "declaration";
        }
        return ir.resolvedName().isPresent() ? "registry" : "unresolved";
    }

    /**
     * The OLD GENERATOR'S Java answer - or {@code REFUSED:<reason>}, the one thing a refusal and an answer must not
     * confuse, the reason classed from its own throw sites by {@link IRJavaTypeNames#legacyRefusalToken} (PR #644, the
     * banked cq NIT-3 of #643: two refusals for different reasons must not read EQUAL).
     */
    private String legacyJavaType(RType legacy) {
        try {
            return typeTranslator.toJavaReferenceType(legacy).getCanonicalName().withDots();
        } catch (RuntimeException refused) {
            return IRJavaTypeNames.legacyRefusalToken(legacy, refused);
        }
    }

    /** The EMITTER'S Java answer, from the IR facts alone - or {@code REFUSED:<reason>}, its own named refusal's reason. */
    private static String irJavaType(IRType ir, List<IRTypeArgument> useSiteArguments) {
        try {
            return IRJavaTypeNames.of(ir, useSiteArguments);
        } catch (IRJavaTypeNames.Refusal refused) {
            return refused.reason().token();
        }
    }

    /**
     * The collapsed chain of an alias reference, walked HERE from the source (never asked of the adapter), then the
     * BELT: when the old generator's own answer forwards to a number, its four constraints must be the four the IR's
     * effective arguments state. A present-but-unparseable constraint makes the old generator answer
     * {@code RMissingType}, so the belt does not fire there - the {@code .javaType} family carries that case.
     */
    private void effectiveBase(Check c, String at, RTypeAlias alias, RTypeCall call, RType legacy, IRType ir) {
        Optional<IREffectiveBase> expected = collapse(alias, bindingsOf(call), 0);
        c.same(at + ".effectiveBase.present", expected.isPresent(), ir.effectiveBase().isPresent());
        if (expected.isPresent() && ir.effectiveBase().isPresent()) {
            IREffectiveBase e = expected.get();
            IREffectiveBase a = ir.effectiveBase().get();
            c.same(at + ".effectiveBase.kind", e.kind(), a.kind());
            c.same(at + ".effectiveBase.name", e.name(), a.name());
            c.same(at + ".effectiveBase.namespace", e.namespace(), a.namespace());
            c.same(at + ".effectiveBase.arguments", argumentRows(e.arguments()), argumentRows(a.arguments()));
        }
        if (SOLVER.evaluateForward(legacy) instanceof RNumberType number) {
            Optional<Integer> digits = number.digits().isPresent()
                    ? Optional.of(number.digits().getAsInt()) : Optional.<Integer>empty();
            Optional<Integer> fractionalDigits = number.fractionalDigits().isPresent()
                    ? Optional.of(number.fractionalDigits().getAsInt()) : Optional.<Integer>empty();
            c.same(at + ".effectiveBase.number",
                    List.<Object>of(digits, fractionalDigits,
                            number.min().map(BigDecimal::toPlainString), number.max().map(BigDecimal::toPlainString)),
                    List.<Object>of(irInt(ir, "digits"), irInt(ir, "fractionalDigits"),
                            irDecimal(ir, "min"), irDecimal(ir, "max")));
        }
    }

    /** One effective argument per row: the parameter, the literal as written, the sign. */
    private static List<List<Object>> argumentRows(List<IRTypeArgument> arguments) {
        List<List<Object>> rows = new ArrayList<>();
        for (IRTypeArgument argument : arguments) {
            rows.add(List.of(argument.parameter(), argument.literalValue().orElse(""), argument.negated()));
        }
        return rows;
    }

    /** An effective-base integer constraint, parsed; absent (or unreadable - see {@link #effectiveBase}) stays empty. */
    private static Optional<Integer> irInt(IRType ir, String parameter) {
        Optional<String> literal = ir.effectiveBase().flatMap(base -> base.argument(parameter));
        if (literal.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.valueOf(literal.get()));
        } catch (NumberFormatException notAnInt) {
            return Optional.empty();
        }
    }

    /** An effective-base decimal constraint, parsed to its plain string; absent or unreadable stays empty. */
    private static Optional<String> irDecimal(IRType ir, String parameter) {
        Optional<String> literal = ir.effectiveBase().flatMap(base -> base.argument(parameter));
        if (literal.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(literal.get()).toPlainString());
        } catch (NumberFormatException notADecimal) {
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------ the collapse law, this reconciler's OWN walk

    /** A literal bound to a type parameter: the value as written, WITHOUT the sign, and the sign. */
    private record Literal(String value, boolean negated) {
        IRTypeArgument asArgument(String parameter) {
            return new IRTypeArgument(parameter, Optional.empty(), Optional.of(value), negated);
        }
    }

    /** The literal arguments a USE SITE binds; an argument passing a parameter through by name binds nothing. */
    private static Map<String, Literal> bindingsOf(RTypeCall call) {
        Map<String, Literal> bindings = new LinkedHashMap<>();
        if (call == null) {
            return bindings;
        }
        for (RTypeCallArgument argument : call.arguments()) {
            RTypeCallArgumentExpression value = argument.value();
            if (value != null && value.literalValue().isPresent()) {
                bindings.put(argument.parameterName(), new Literal(value.literalValue().get(), value.isNegated()));
            }
        }
        return bindings;
    }

    /**
     * Collapse {@code alias}'s chain under {@code bindings} - the declaration it ends on and the arguments in force
     * there. The law is the parser's own ({@code TypeAliasSolver.evaluateBodyWithBindings}): a literal body argument
     * wins, a parameter passed through by name reads its binding or stays ABSENT, a body naming another alias recurses
     * with the resolved arguments as the new bindings, a body naming a MODEL-declared basic / record type is
     * {@code nothing}, and a body no declaration answers is looked up by NAME in the shared registry. Written HERE
     * rather than called on the adapter: two producers that must agree is the point (LAW 69).
     */
    private static Optional<IREffectiveBase> collapse(RTypeAlias alias, Map<String, Literal> bindings, int depth) {
        if (depth >= MAX_ALIAS_DEPTH) {
            return Optional.empty();
        }
        RTypeCall body = alias.typeCall();
        if (body == null) {
            return Optional.empty();
        }
        Map<String, Literal> resolved = new LinkedHashMap<>();
        for (RTypeCallArgument argument : body.arguments()) {
            RTypeCallArgumentExpression value = argument.value();
            if (value == null) {
                continue;
            }
            if (value.literalValue().isPresent()) {
                resolved.put(argument.parameterName(), new Literal(value.literalValue().get(), value.isNegated()));
            } else if (value.nameValue().isPresent()) {
                Literal bound = bindings.get(value.nameValue().get());
                if (bound != null) {
                    resolved.put(argument.parameterName(), bound);
                }
            }
        }
        Optional<RNode> target = quiet(body::referencedType);
        if (target.isPresent()) {
            RNode node = target.get();
            if (node instanceof RTypeAlias inner) {
                return collapse(inner, resolved, depth + 1);
            }
            if (node instanceof RDataType dataType) {
                return Optional.of(new IREffectiveBase(IRKind.STRUCT, dataType.name(), namespaceOf(dataType),
                        declaredArguments(resolved)));
            }
            if (node instanceof REnumeration enumeration) {
                return Optional.of(new IREffectiveBase(IRKind.ENUM, enumeration.name(), namespaceOf(enumeration),
                        declaredArguments(resolved)));
            }
            if (node instanceof RChoice choice) {
                return Optional.of(new IREffectiveBase(IRKind.CHOICE, choice.name(), namespaceOf(choice),
                        declaredArguments(resolved)));
            }
            if (node instanceof com.regnosys.rosetta.ast.types.RBasicType
                    || node instanceof com.regnosys.rosetta.ast.types.RRecordType) {
                String written = simpleName(node);
                String declared = written == null ? "" : written;
                Optional<RType> registryType = BUILTINS.lookup(declared);
                if (registryType.isPresent()) {
                    return Optional.of(registryBase(declared, registryType.get(), resolved));
                }
                // the released 9.83.0 plugin types a MODEL-declared basic / record type as `nothing`
                return Optional.of(new IREffectiveBase(IRKind.BASIC_TYPE, "nothing", Optional.empty(), List.of()));
            }
            return Optional.empty();
        }
        String bodyName = body.typeName();
        if (bodyName == null) {
            return Optional.empty();
        }
        return BUILTINS.lookup(bodyName).map(registryType -> registryBase(bodyName, registryType, resolved));
    }

    /** A declared leaf (STRUCT / ENUM / CHOICE) states the resolved arguments in the body's own order. */
    private static List<IRTypeArgument> declaredArguments(Map<String, Literal> resolved) {
        List<IRTypeArgument> arguments = new ArrayList<>();
        for (Map.Entry<String, Literal> entry : resolved.entrySet()) {
            arguments.add(entry.getValue().asArgument(entry.getKey()));
        }
        return arguments;
    }

    /**
     * A builtin leaf: the registry type's OWN constraints seed the arguments ({@code int}'s
     * {@code fractionalDigits: 0} survives a body that never restates it), the resolved literals overlay them, and the
     * result is stated in {@link #CANONICAL_PARAMETERS} order, anything else after it in body order.
     */
    private static IREffectiveBase registryBase(String name, RType registryType, Map<String, Literal> resolved) {
        IRKind kind = registryType instanceof RRecordType ? IRKind.RECORD_TYPE : IRKind.BASIC_TYPE;
        Map<String, Literal> effective = new LinkedHashMap<>(seed(registryType));
        effective.putAll(resolved);
        List<IRTypeArgument> arguments = new ArrayList<>();
        for (String parameter : CANONICAL_PARAMETERS) {
            Literal literal = effective.get(parameter);
            if (literal != null) {
                arguments.add(literal.asArgument(parameter));
            }
        }
        for (Map.Entry<String, Literal> entry : resolved.entrySet()) {
            if (!CANONICAL_PARAMETERS.contains(entry.getKey())) {
                arguments.add(entry.getValue().asArgument(entry.getKey()));
            }
        }
        return new IREffectiveBase(kind, name, Optional.empty(), arguments);
    }

    /** The constraints the registry type carries itself; only a number type has any. */
    private static Map<String, Literal> seed(RType registryType) {
        Map<String, Literal> seeded = new LinkedHashMap<>();
        if (registryType instanceof RNumberType number) {
            number.digits().ifPresent(d -> seeded.put("digits", new Literal(Integer.toString(d), false)));
            number.fractionalDigits().ifPresent(f -> seeded.put("fractionalDigits", new Literal(Integer.toString(f), false)));
            number.min().ifPresent(m -> seeded.put("min", new Literal(m.abs().toPlainString(), m.signum() < 0)));
            number.max().ifPresent(m -> seeded.put("max", new Literal(m.abs().toPlainString(), m.signum() < 0)));
        }
        return seeded;
    }

    /** True for a builtin basic / record type of the shared registry - none of the declaration-backed or alias classes. */
    private static boolean isBuiltin(RType legacy) {
        return !(legacy instanceof RDataTypeRef || legacy instanceof RChoiceTypeRef || legacy instanceof REnumTypeRef
                || legacy instanceof RAliasType || legacy instanceof RMissingType);
    }

    /** The IR kind the old generator's resolved {@link RType} stands for. */
    private static IRKind legacyKind(RType legacy, Optional<? extends RNode> declaration) {
        if (legacy instanceof RDataTypeRef) {
            return IRKind.STRUCT;
        }
        if (legacy instanceof RChoiceTypeRef) {
            return IRKind.CHOICE;
        }
        if (legacy instanceof REnumTypeRef) {
            return IRKind.ENUM;
        }
        if (legacy instanceof RAliasType) {
            return IRKind.TYPE_ALIAS;
        }
        if (legacy instanceof RRecordType) {
            return IRKind.RECORD_TYPE;
        }
        if (legacy instanceof RMissingType) {
            return IRKind.STRUCT;   // the unresolved reference's declared default
        }
        // a builtin basic type (RBasicType / RNumberType / RStringType): the model library's basicType, or - when
        // the call carries arguments - the library's parameterised basicType the old generator applied them over
        return declaration.flatMap(IRDeclarationReconciler::declaredKind).orElse(IRKind.BASIC_TYPE);
    }

    private static Optional<IRKind> declaredKind(RNode declaration) {
        if (declaration instanceof com.regnosys.rosetta.ast.types.RBasicType) {
            return Optional.of(IRKind.BASIC_TYPE);
        }
        if (declaration instanceof com.regnosys.rosetta.ast.types.RTypeAlias) {
            return Optional.of(IRKind.TYPE_ALIAS);
        }
        if (declaration instanceof com.regnosys.rosetta.ast.types.RRecordType) {
            return Optional.of(IRKind.RECORD_TYPE);
        }
        if (declaration instanceof com.regnosys.rosetta.ast.types.RMetaType) {
            return Optional.of(IRKind.META_TYPE);
        }
        return Optional.empty();
    }

    private void typeArguments(Check c, String at, RTypeCall call, IRField ir) {
        typeArguments(c, at, call, ir.typeArguments());
    }

    /** The arguments a call wrote AS WRITTEN - a literal with its sign, or a parameter passed through by name. */
    private void typeArguments(Check c, String at, RTypeCall call, List<IRTypeArgument> ir) {
        List<List<Object>> expected = new ArrayList<>();
        if (call != null) {
            for (var argument : call.arguments()) {
                var value = argument.value();
                expected.add(List.of(argument.parameterName(),
                        value == null ? Optional.empty() : value.nameValue(),
                        value == null ? Optional.empty() : value.literalValue(),
                        value != null && value.isNegated()));
            }
        }
        c.same(at, expected, ir.stream()
                .map(a -> List.<Object>of(a.parameter(), a.nameValue(), a.literalValue(), a.negated())).toList());
    }

    private void annotations(Check c, String at, List<RAnnotationRef> ast, List<IRAnnotationUse> ir) {
        c.same(at, ast.stream().map(a -> List.<Object>of(a.annotationName(), a.qualifierName(),
                        a.qualifiers().stream().map(q -> List.<Object>of(q.key(), q.value() == null ? "" : q.value(), q.isAttributeRef())).toList())).toList(),
                ir.stream().map(a -> List.<Object>of(a.name(), a.qualifier(),
                        a.arguments().stream().map(q -> List.<Object>of(q.key(), q.value(), q.attributeRef())).toList())).toList());
    }

    private void docReferences(Check c, String at, List<RDocReference> ast, List<IRDocReference> ir) {
        c.same(at + ".size", ast.size(), ir.size());
        for (int i = 0; i < Math.min(ast.size(), ir.size()); i++) {
            RDocReference a = ast.get(i);
            IRDocReference r = ir.get(i);
            String p = at + "[" + i + "].";
            var doc = a.regulatoryDocRef();
            c.same(p + "regulatory", a.isRegulatoryReference(), r.regulatory());
            c.same(p + "path", a.forPath().map(IRDeclarationReconciler::pathText), r.path().map(IRAnnotationPath::display));
            c.same(p + "body", doc == null ? Optional.empty() : Optional.ofNullable(doc.bodyRef()), r.body());
            List<List<Object>> corpora = new ArrayList<>();
            if (doc != null) {
                for (String name : doc.corpusRefs()) {
                    RCorpus corpus = corpusLookup.lookupCorpus(a, name);
                    corpora.add(List.of(name, corpus == null ? Optional.empty() : Optional.of(List.of(
                            Optional.ofNullable(corpus.corpusTypeKeyword()), corpus.name(), corpus.displayName(), corpus.definition()))));
                }
            }
            c.same(p + "corpora", corpora, r.corpora().stream().map(x -> List.<Object>of(x.reference(),
                    x.resolved().map(d -> List.<Object>of(d.typeKeyword(), d.name(), d.displayName(), d.definition())))).toList());
            c.same(p + "segments", doc == null ? List.of() : doc.segmentRefs().stream()
                            .map(s -> List.of(s.segmentName(), s.value() == null ? "" : s.value())).toList(),
                    r.segments().stream().map(s -> List.of(s.name(), s.value())).toList());
            c.same(p + "rationales", a.rationales().stream().map(x -> List.of(x.rationale(), x.rationaleAuthor())).toList(),
                    r.rationales().stream().map(x -> List.of(x.text(), x.author())).toList());
            c.same(p + "structuredProvision", a.structuredProvision(), r.structuredProvision());
            c.same(p + "provision", a.provision(), r.provision());
            c.same(p + "reportedField", a.isReportedField(), r.reportedField());
            c.same(p + "namedArgs", a.namedArgs().stream().map(x -> List.of(x.name(), x.value() == null ? "" : x.value())).toList(),
                    r.namedArgs().stream().map(x -> List.of(x.name(), x.value())).toList());
        }
    }

    private void labels(Check c, String at, List<RLabelAnnotation> ast, IRField ir) {
        c.same(at, ast.stream().map(l -> List.<Object>of(l.label() == null ? "" : l.label(),
                        l.forPath().map(IRDeclarationReconciler::pathText), l.asPath().map(IRDeclarationReconciler::pathText))).toList(),
                ir.labels().stream().map(l -> List.<Object>of(l.label(), l.forPath().map(IRAnnotationPath::display),
                        l.asPath().map(IRAnnotationPath::display))).toList());
    }

    private void ruleReferences(Check c, String at, List<RRuleReferenceAnnotation> ast, IRField ir) {
        List<List<Object>> expected = new ArrayList<>();
        for (RRuleReferenceAnnotation annotation : ast) {
            Optional<RRule> rule = quiet(annotation::rule);
            expected.add(List.of(annotation.forPath().map(IRDeclarationReconciler::pathText), annotation.ruleName(),
                    rule.flatMap(IRDeclarationReconciler::namespaceOf), rule.map(RRule::name)));
        }
        c.same(at, expected, ir.ruleReferences().stream().map(r -> List.<Object>of(r.forPath().map(IRAnnotationPath::display),
                r.ruleName(), r.resolvedNamespace(), r.resolvedName())).toList());
    }

    // ------------------------------------------------------------------------------------------------ helpers

    /** The annotation path in the model's own spelling - the AST half of {@link IRAnnotationPath#display()}. */
    private static String pathText(RAnnotationPathExpression path) {
        StringBuilder sb = new StringBuilder(path.isRootItem() ? "item" : String.valueOf(path.root()));
        for (var segment : path.segments()) {
            sb.append(segment.isDeep() ? " ->> " : " -> ").append(segment.name());
        }
        return sb.toString();
    }

    private static IRBounds bounds(RCardinality cardinality) {
        BigInteger lower = cardinality.inf() == null ? BigInteger.ZERO : cardinality.inf();
        return new IRBounds(lower, cardinality.isUnbounded() ? Optional.empty() : Optional.ofNullable(cardinality.sup()));
    }

    /** The four-bucket name from the EXACT bounds - an independent derivation of the adapter's bucket. */
    private static String bucket(Optional<RCardinality> cardinality) {
        if (cardinality.isEmpty()) {
            return "ONE_TO_ONE";
        }
        IRBounds b = bounds(cardinality.get());
        boolean many = b.isUnbounded() || b.upper().get().compareTo(BigInteger.ONE) > 0;
        return (b.lower().signum() == 0 ? "ZERO" : "ONE") + "_TO_" + (many ? "MANY" : "ONE");
    }

    private static Optional<String> namespaceOf(RNode declaration) {
        return AstWalker.findAncestor(declaration, RModel.class).map(RModel::namespace);
    }

    private static String simpleName(RNode declaration) {
        if (declaration instanceof RDataType d) {
            return d.name();
        }
        if (declaration instanceof RChoice ch) {
            return ch.name();
        }
        if (declaration instanceof REnumeration e) {
            return e.name();
        }
        if (declaration instanceof com.regnosys.rosetta.ast.types.RBasicType b) {
            return b.name();
        }
        if (declaration instanceof com.regnosys.rosetta.ast.types.RTypeAlias a) {
            return a.name();
        }
        if (declaration instanceof com.regnosys.rosetta.ast.types.RRecordType r) {
            return r.name();
        }
        if (declaration instanceof com.regnosys.rosetta.ast.types.RMetaType m) {
            return m.name();
        }
        return null;
    }

    private static String qualified(String namespace, String simpleName) {
        return namespace == null || namespace.isEmpty() ? simpleName : namespace + "." + simpleName;
    }

    private static <T> Optional<T> quiet(java.util.function.Supplier<Optional<T>> read) {
        try {
            Optional<T> value = read.get();
            return value == null ? Optional.empty() : value;
        } catch (RuntimeException unresolved) {
            return Optional.empty();
        }
    }

    /**
     * One declaration's assertions: counts every fact, collects the mismatches, books both on {@link #close()}. The
     * declaration itself is booked by {@link #attempt()}, before the adapter - never here.
     */
    private final class Check {
        private final String subject;
        private final List<String> failed = new ArrayList<>();
        private int asserted;

        Check(String subject) {
            this.subject = subject;
        }

        void same(String fact, Object expected, Object actual) {
            asserted++;
            if (!Objects.equals(expected, actual)) {
                failed.add("IR/AST reconciliation failed for " + subject + ": " + fact + " - the source says " + expected
                        + ", the IR says " + actual);
            }
        }

        List<String> close() {
            facts.addAndGet(asserted);
            mismatches.addAndGet(failed.size());
            return failed;
        }
    }
}
