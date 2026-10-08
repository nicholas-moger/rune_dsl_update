package com.regnosys.rosetta.validation;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.annotations.RAnnotationPathSegment;
import com.regnosys.rosetta.ast.external.*;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.validation.validators.ImportValidator;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.inference.TypeDirectedResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pass 7 orchestrator: resolves deferred type-directed categories (3, 6, 7),
 * then runs all semantic validators. Category 5 (with-meta) stays in Pass 6.
 *
 * <p>Spec: D7 + D11 in {@code docs/specs/2026-04-09-m6-validation-design.md}.
 */
public final class ValidationPass {

    private final List<Validator> validators;
    private final TypeDirectedResolver resolver;

    public ValidationPass(List<Validator> validators, TypeDirectedResolver resolver) {
        this.validators = List.copyOf(validators);
        this.resolver = resolver;
    }

    /**
     * Runs type-directed resolution for deferred categories, then
     * runs all validators on all files.
     */
    public void run(List<RModel> files, ValidationCollector collector) {
        Objects.requireNonNull(files);
        Objects.requireNonNull(collector);

        // Phase A: Resolve deferred type-directed categories 3, 6, 7
        resolveDeferred(files);

        // v3.2 seat 4 (F10): the models in load order, for the cross-model checks
        for (Validator v : validators) {
            v.bindModels(files);
        }

        // Phase B: Model-level validators (e.g., import checks)
        for (RModel file : files) {
            for (Validator v : validators) {
                if (v instanceof ImportValidator iv) {
                    iv.validateModel(file, collector);
                }
            }
        }

        // Phase C: Per-element validators
        for (RModel file : files) {
            for (RRootElement elem : file.rootElements()) {
                for (Validator v : validators) {
                    v.validate(elem, collector);
                }
            }
        }
    }

    /**
     * Resolves type-directed categories 3 (operation paths), 6 (annotation
     * paths), 7 (externals). Category 5 (with-meta) depends on the receiver
     * expression's inferred type and is handled in Pass 6's inference loop.
     * These categories depend on the enclosing declaration's type context
     * from M3, not on inferred expression types.
     */
    private void resolveDeferred(List<RModel> files) {
        if (resolver == null) return;

        for (RModel file : files) {
            // Category 3: Operation paths in functions
            for (var fn : AstWalker.findAll(file, RFunction.class)) {
                resolveOperationPaths(fn);
            }
            // Category 6: Annotation paths on data types
            for (var dt : AstWalker.findAll(file, RDataType.class)) {
                resolveAnnotationPaths(dt);
            }
            // Category 7: External class attributes + enum values
            for (var ec : AstWalker.findAll(file, RExternalClass.class)) {
                resolveExternalClassAttributes(ec);
            }
            for (var ee : AstWalker.findAll(file, RExternalEnum.class)) {
                resolveExternalEnumValues(ee);
            }
        }
    }

    private void resolveOperationPaths(RFunction fn) {
        var output = fn.output();
        if (output.isEmpty()) return;
        var tc = output.get().typeCall();
        if (tc == null) return;
        var resolved = tc.referencedType();
        if (resolved.isEmpty() || !(resolved.get() instanceof RDataType dt)) return;

        var contextType = RMetaAnnotatedType.withNoMeta(new RDataTypeRef(dt));
        String outputName = output.get().name();
        for (ROperation op : AstWalker.findAll(fn, ROperation.class)) {
            var aliasContext = aliasTargetContextOrNull(fn, op, outputName, contextType);
            var effectiveContext = aliasContext != null ? aliasContext : contextType;
            op.segment().ifPresent(seg -> resolver.resolveOperationPath(seg, effectiveContext));
        }
    }

    /**
     * facet aliasRootedSetSegmentResolution (PR #435, finding #25): the type context
     * for an operation whose TARGET names an alias ({@link RShortcut}) of the
     * enclosing function. Upstream Xtext scopes an operation's path segments against
     * the linked assign-root SYMBOL's type (RosettaScopeProvider — for a shortcut
     * root that is the shortcut expression's type), so {@code set barAlias -> id:}
     * links {@code id} against the alias's TERMINAL type. The fork resolved every
     * operation segment against the function OUTPUT's type, leaving alias-rooted
     * segments unresolved — and an {@code id}/{@code key}/{@code scheme}/{@code
     * reference}-named leaf then fell into the renderer's C3 meta-pseudo arm
     * ({@code .getOrCreateMeta().setExternalKey(…)} — the #434-banked 084 face).
     *
     * <p>Admission: the alias body is a PURE output-rooted nav — a name chain in
     * either AST shape (linked {@link RFeatureCall}/{@link RSymbolReference} in the
     * port pipeline, disguised fully-unresolved {@link REnumValueRef} in the
     * workspace pipeline — the #433 both-shapes law; the #381
     * {@code isOutputBuilderNavAlias} discriminator) whose root name IS the output
     * name, and every hop resolves as a declared attribute (supertype chain walked
     * by {@link TypeDirectedResolver#findAttributeOnType}). Returns {@code null} —
     * the existing output-context resolution, byte-identical behavior — for a
     * non-alias target, a non-nav alias body, a non-output root, or any
     * unresolvable hop (resolution stays silently absent exactly as today).
     */
    private RMetaAnnotatedType aliasTargetContextOrNull(RFunction fn, ROperation op,
            String outputName, RMetaAnnotatedType outputContext) {
        String target = op.targetName();
        if (target == null || outputName == null || target.equals(outputName)) return null;
        RShortcut alias = null;
        for (RShortcut s : fn.shortcuts()) {
            if (target.equals(s.name())) {
                alias = s;
                break;
            }
        }
        if (alias == null) return null;
        List<String> names = flattenNavNames(alias.expression());
        if (names == null || names.size() < 2 || !outputName.equals(names.get(0))) return null;
        RMetaAnnotatedType current = outputContext;
        for (int i = 1; i < names.size(); i++) {
            if (current.isMissing()) return null;
            var attr = resolver.findAttributeOnType(current.type(), names.get(i));
            if (attr.isEmpty()) return null;
            current = resolver.inferAttributeRefType(attr.get());
        }
        return current.isMissing() ? null : current;
    }

    /**
     * Flattens a pure nav expression to its name chain ({@code topOut -> foo -> bar}
     * → {@code [topOut, foo, bar]}), covering both AST shapes: a linked
     * {@link RFeatureCall}/{@link RSymbolReference} chain (the port-env parse) and
     * the disguised 2-name {@link REnumValueRef} root (the workspace parse — the
     * grammar's {EnumName -> ValueName} precedence; admitted only FULLY unresolved,
     * mirroring the #381 discriminator, so a genuine enum-value ref never reads as
     * a nav). Returns {@code null} for any other expression shape.
     */
    private static List<String> flattenNavNames(RExpression expr) {
        RExpression cur = expr;
        List<String> tail = new ArrayList<>();
        while (cur instanceof RFeatureCall fc) {
            if (fc.featureName() == null) return null;
            tail.add(fc.featureName());
            cur = fc.receiver();
        }
        List<String> names = new ArrayList<>();
        if (cur instanceof REnumValueRef evr) {
            if (evr.enumeration().isPresent() || evr.resolvedSymbol().isPresent()
                    || evr.enumName() == null || evr.valueName() == null) {
                return null;
            }
            names.add(evr.enumName());
            names.add(evr.valueName());
        } else if (cur instanceof RSymbolReference ref) {
            if (ref.name() == null) return null;
            names.add(ref.name());
        } else {
            return null;
        }
        for (int i = tail.size() - 1; i >= 0; i--) {
            names.add(tail.get(i));
        }
        return names;
    }

    private void resolveAnnotationPaths(RDataType dt) {
        var contextType = RMetaAnnotatedType.withNoMeta(new RDataTypeRef(dt));
        for (var seg : AstWalker.findAll(dt, RAnnotationPathSegment.class)) {
            resolver.resolveAnnotationPathSegment(seg, contextType);
        }
    }

    private void resolveExternalClassAttributes(RExternalClass ec) {
        var resolved = ec.referencedType();
        if (resolved.isEmpty()) return;
        var contextType = RMetaAnnotatedType.withNoMeta(new RDataTypeRef(resolved.get()));
        for (var attr : ec.attributes()) {
            resolver.resolveExternalAttribute(attr, contextType);
        }
    }

    private void resolveExternalEnumValues(RExternalEnum ee) {
        var resolved = ee.referencedType();
        if (resolved.isEmpty()) return;
        var contextType = RMetaAnnotatedType.withNoMeta(new REnumTypeRef(resolved.get()));
        for (var val : ee.values()) {
            resolver.resolveExternalEnumValue(val, contextType);
        }
    }
}
