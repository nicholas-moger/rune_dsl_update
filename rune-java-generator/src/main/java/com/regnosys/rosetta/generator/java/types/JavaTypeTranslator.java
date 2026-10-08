package com.regnosys.rosetta.generator.java.types;

import com.regnosys.rosetta.generator.java.SilentDegradation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.regulatory.RReport;
import com.regnosys.rosetta.ast.regulatory.RRegulatoryDocumentReference;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.types.RAliasType;
import com.regnosys.rosetta.types.RBasicType;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RMissingType;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RRecordType;
import com.regnosys.rosetta.types.RStringType;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.types.RecordKind;
import com.fasterxml.jackson.core.type.TypeReference;
import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.reports.ReportFunction;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.util.DottedPath;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaGenericTypeDeclaration;
import com.rosetta.util.types.JavaParameterizedType;
import com.rosetta.util.types.JavaPrimitiveType;
import com.rosetta.util.types.JavaType;
import java.util.Set;

/**
 * Maps our sealed {@link RType} interface to Java types. Uses an exhaustive
 * {@code switch} on the 9 RType variants (Java 21 sealed interface guarantee).
 *
 * <p>This is the D4 table from the spec — every mapping must match upstream
 * exactly for D11 byte-identical output.
 *
 * <p>Ported from upstream's {@code JavaTypeTranslator extends RosettaTypeSwitch}.
 * Our version uses a sealed switch instead of the visitor pattern.
 *
 * <p>Note: some upstream methods ({@code toFunctionJavaClass}, {@code toConditionJavaClass},
 * {@code toValidatorClass}, etc.) depend on {@code GeneratorModel} and will be added
 * in T4 when the bridge layer is available. For now this class handles the core
 * RType → JavaType mapping.
 */
public class JavaTypeTranslator {

    private final JavaTypeUtil typeUtil;

    public JavaTypeTranslator(JavaTypeUtil typeUtil) {
        this.typeUtil = typeUtil;
    }

    /**
     * Map an {@link RType} to its Java type representation.
     * Exhaustive over all 9 sealed variants.
     */
    public JavaType toJavaType(RType type) {
        return switch (type) {
            case RDataTypeRef ref -> caseDataType(ref);
            case REnumTypeRef ref -> caseEnumType(ref);
            case RChoiceTypeRef ref -> caseChoiceType(ref);
            case RBasicType basic -> caseBasicType(basic);
            case RNumberType num -> caseNumberType(num);
            case RStringType str -> caseStringType(str);
            case RRecordType rec -> caseRecordType(rec);
            case RAliasType alias -> caseAliasType(alias);
            case RMissingType missing -> caseMissingType(missing);
        };
    }

    /**
     * Map an RType to a Java reference type (boxes primitives).
     */
    public JavaClass<?> toJavaReferenceType(RType type) {
        JavaType jt = toJavaType(type);
        if (jt instanceof JavaPrimitiveType pt) {
            return pt.toReferenceType();
        } else if (jt instanceof JavaClass<?> jc) {
            return jc;
        }
        throw new UnsupportedOperationException(
                "Cannot convert type " + type + " (" + type.getClass().getSimpleName()
                + ") to a Java reference type.");
    }

    // -- Case methods (one per sealed variant) --------------------------------

    private JavaType caseDataType(RDataTypeRef ref) {
        var dt = ref.astNode();
        if (dt.parent() instanceof com.regnosys.rosetta.ast.model.RModel model) {
            DottedPath ns = DottedPath.splitOnDots(model.namespace());
            // Use RosettaModelObject.class so isRosettaModelObject() returns true
            return RGeneratedJavaClass.create(
                    JavaPackageName.escape(ns), dt.name(),
                    com.rosetta.model.lib.RosettaModelObject.class);
        }
        // v3.1 C0 item 1 — REFUSAL. The declaring model is unreachable, so this model
        // type would collapse to java.lang.Object and every consumer downstream would
        // type against Object. The 25x2 matrix measured this site at ZERO hits on every
        // cell of the band, clean or not — no output depends on the collapse, so it
        // refuses by name rather than typing a whole file against Object. (Note the
        // audit's report-output collapse, `ReportFunction<…, Object>`, is NOT this seat:
        // that Object comes from the parser's arm-type join, upstream of the translator —
        // C1/C2 territory, and still uninstrumented. Recorded so the catalogue does not
        // read as if this site covered it.)
        throw SilentDegradation.refuse(SilentDegradation.Site.TYPE_COLLAPSED_TO_OBJECT,
                "unreachable declaring model for type '" + dt.name() + "'", dt);
    }

    private JavaType caseEnumType(REnumTypeRef ref) {
        var en = ref.astNode();
        if (en.parent() instanceof com.regnosys.rosetta.ast.model.RModel model) {
            DottedPath ns = DottedPath.splitOnDots(model.namespace());
            return new RJavaEnum(en, ns);
        }
        // v3.1 C0 item 1 — REFUSAL. The declaring model is unreachable, so this model
        // type would collapse to java.lang.Object and every consumer downstream would
        // type against Object. The 25x2 matrix measured this site at ZERO hits on every
        // cell of the band, clean or not — no output depends on the collapse, so it
        // refuses by name rather than typing a whole file against Object. (Note the
        // audit's report-output collapse, `ReportFunction<…, Object>`, is NOT this seat:
        // that Object comes from the parser's arm-type join, upstream of the translator —
        // C1/C2 territory, and still uninstrumented. Recorded so the catalogue does not
        // read as if this site covered it.)
        throw SilentDegradation.refuse(SilentDegradation.Site.TYPE_COLLAPSED_TO_OBJECT,
                "unreachable declaring model for type '" + en.name() + "'", en);
    }

    private JavaType caseChoiceType(RChoiceTypeRef ref) {
        // Upstream: caseChoiceType delegates to caseDataType(type.asRDataType())
        // Choice types generate as data type interfaces with same package/class structure
        var ch = ref.astNode();
        if (ch != null && ch.parent() instanceof com.regnosys.rosetta.ast.model.RModel model) {
            DottedPath ns = DottedPath.splitOnDots(model.namespace());
            return RGeneratedJavaClass.create(
                    JavaPackageName.escape(ns), ch.name(),
                    com.rosetta.model.lib.RosettaModelObject.class);
        }
        // v3.1 C0 item 1 — REFUSAL. The declaring model is unreachable, so this model
        // type would collapse to java.lang.Object and every consumer downstream would
        // type against Object. The 25x2 matrix measured this site at ZERO hits on every
        // cell of the band, clean or not — no output depends on the collapse, so it
        // refuses by name rather than typing a whole file against Object. (Note the
        // audit's report-output collapse, `ReportFunction<…, Object>`, is NOT this seat:
        // that Object comes from the parser's arm-type join, upstream of the translator —
        // C1/C2 territory, and still uninstrumented. Recorded so the catalogue does not
        // read as if this site covered it.)
        // REFUSE BY NAME, and the name survives a detached AST node: RChoiceTypeRef
        // carries its own name(), so a null astNode costs us the model but never the
        // identity. Naming the ref "<detached ref>" threw away triage information the
        // refusal had in hand, which is the one thing a refusal exists to carry
        // (Copilot R8, PR #566).
        String choiceName = ch != null ? ch.name() : ref.name();
        throw SilentDegradation.refuse(SilentDegradation.Site.TYPE_COLLAPSED_TO_OBJECT,
                "unreachable declaring model for choice type '"
                        + (choiceName == null ? "<unnamed choice ref>" : choiceName)
                        + "'" + (ch == null ? " (detached ref: no AST node)" : ""), ch);
    }

    private JavaType caseBasicType(RBasicType type) {
        if (type == RBasicType.BOOLEAN) {
            return JavaPrimitiveType.BOOLEAN;
        } else if (type == RBasicType.TIME) {
            return typeUtil.LOCAL_TIME;
        } else if (type == RBasicType.NOTHING) {
            return typeUtil.VOID;
        } else if (type == RBasicType.ANY) {
            return typeUtil.OBJECT;
        } else if (type == RBasicType.PATTERN) {
            // D4: PATTERN has no Java mapping. Upstream RosettaTypeSwitch throws.
            throw new IllegalStateException(
                    "RBasicType.PATTERN cannot be translated to a Java type. "
                    + "It is grammar-level only and should not appear in codegen.");
        }
        throw new IllegalStateException("Unknown RBasicType: " + type);
    }

    /**
     * Number type mapping (must match upstream exactly for D11):
     * <ul>
     *   <li>Non-integer → {@code BigDecimal}</li>
     *   <li>Integer, digits ≤ 9 → {@code int}</li>
     *   <li>Integer, digits ≤ 18 → {@code long}</li>
     *   <li>Integer, digits > 18 → {@code BigInteger}</li>
     *   <li>Integer, digits unconstrained → defaults to 9 → {@code int}</li>
     * </ul>
     */
    private JavaType caseNumberType(RNumberType type) {
        if (!type.isInteger()) {
            return JavaClass.from(BigDecimal.class);
        }
        int digits = type.digits().orElse(9);
        if (digits <= 9) {
            return JavaPrimitiveType.INT;
        } else if (digits <= 18) {
            return JavaPrimitiveType.LONG;
        } else {
            return JavaClass.from(BigInteger.class);
        }
    }

    private JavaType caseStringType(RStringType type) {
        return typeUtil.STRING;
    }

    private JavaType caseRecordType(RRecordType type) {
        return switch (type.kind()) {
            case DATE -> typeUtil.DATE;
            case DATE_TIME -> typeUtil.LOCAL_DATE_TIME;
            case ZONED_DATE_TIME -> typeUtil.ZONED_DATE_TIME;
        };
    }

    private JavaType caseAliasType(RAliasType type) {
        return toJavaType(type.refersTo());
    }

    private JavaType caseMissingType(RMissingType type) {
        return typeUtil.OBJECT;
    }

    // -- Class naming methods (for meta, validators, functions) ----------------

    /**
     * Compute the meta data class for a POJO type.
     * E.g., {@code AdjustableDate} → {@code AdjustableDateMeta} in {@code .meta} sub-package.
     */
    public RGeneratedJavaClass<? extends RosettaMetaData<?>> toJavaMetaDataClass(
            ModelSymbolId typeId) {
        DottedPath metaPackage = typeId.getNamespace().child("meta");
        String simpleName = typeId.getName() + "Meta";
        JavaParameterizedType<RosettaMetaData<?>> superType =
                JavaParameterizedType.from(typeUtil.ROSETTA_META_DATA, typeUtil.OBJECT);
        return RGeneratedJavaClass.createImplementingInterface(
                JavaPackageName.escape(metaPackage), simpleName, superType);
    }

    /**
     * Compute the cardinality validator class for a POJO type.
     * E.g., {@code AdjustableDate} → {@code AdjustableDateValidator} in {@code .validation} sub-package.
     */
    public RGeneratedJavaClass<? extends Validator<?>> toValidatorClass(ModelSymbolId typeId) {
        DottedPath validationPackage = typeId.getNamespace().child("validation");
        String simpleName = typeId.getName() + "Validator";
        return RGeneratedJavaClass.createImplementingInterface(
                JavaPackageName.escape(validationPackage), simpleName,
                JavaParameterizedType.from(typeUtil.VALIDATOR, typeUtil.OBJECT));
    }

    /**
     * Compute the type format validator class for a POJO type.
     * E.g., {@code AdjustableDate} → {@code AdjustableDateTypeFormatValidator}.
     */
    public RGeneratedJavaClass<? extends Validator<?>> toTypeFormatValidatorClass(
            ModelSymbolId typeId) {
        DottedPath validationPackage = typeId.getNamespace().child("validation");
        String simpleName = typeId.getName() + "TypeFormatValidator";
        return RGeneratedJavaClass.createImplementingInterface(
                JavaPackageName.escape(validationPackage), simpleName,
                JavaParameterizedType.from(typeUtil.VALIDATOR, typeUtil.OBJECT));
    }

    /**
     * Compute the only-exists validator class for a POJO type.
     * E.g., {@code AdjustableDate} → {@code AdjustableDateOnlyExistsValidator} in {@code .validation.exists}.
     */
    public RGeneratedJavaClass<? extends ValidatorWithArg<?, ?>> toOnlyExistsValidatorClass(
            ModelSymbolId typeId) {
        DottedPath existsPackage = typeId.getNamespace().child("validation").child("exists");
        String simpleName = typeId.getName() + "OnlyExistsValidator";
        var argType = JavaParameterizedType.from(
                JavaGenericTypeDeclaration.from(new TypeReference<Set<?>>() {}), typeUtil.STRING);
        return RGeneratedJavaClass.createImplementingInterface(
                JavaPackageName.escape(existsPackage), simpleName,
                JavaParameterizedType.from(typeUtil.VALIDATOR_WITH_ARG, typeUtil.OBJECT, argType));
    }

    /**
     * Compute the condition/data rule class for a condition.
     * E.g., condition "AdjustableDateChoice" on type "AdjustableDate"
     * → {@code AdjustableDateAdjustableDateChoice} in {@code .validation.datarule}.
     */
    public RGeneratedJavaClass<?> toConditionJavaClass(
            ModelSymbolId typeId, String conditionName) {
        DottedPath rulePackage = typeId.getNamespace().child("validation").child("datarule");
        String simpleName = typeId.getName() + conditionName;
        return RGeneratedJavaClass.create(
                JavaPackageName.escape(rulePackage), simpleName, Object.class);
    }

    /**
     * Compute the function class for a function. BC overload — used by every
     * existing call site that already has a {@link ModelSymbolId} in hand.
     * Internally delegates to the private {@link #toJavaFunctionClass(ModelSymbolId)}
     * router so the FUNCTION-origin path is shared between this BC entry point
     * and the {@link #toFunctionJavaClass(RFunction, ModelSymbolId)} principled
     * dispatcher added at T6.0.5.
     *
     * <p>E.g., function "Calculate" in namespace "com.example"
     * → {@code Calculate} in {@code com.example.functions}.
     */
    public RGeneratedJavaClass<? extends RosettaFunction> toFunctionJavaClass(
            ModelSymbolId functionId) {
        return toJavaFunctionClass(functionId);
    }

    /**
     * Phase X T6.0.5 — principled origin-dispatch entry point. Mirrors
     * upstream's {@code toFunctionJavaClass(RFunction)} (rune-dsl 9.83.0-line upstream lines
     * 96-106) verbatim: switches on {@link RFunction#origin()} and routes to
     * one of three private routers
     * ({@link #toJavaFunctionClass(ModelSymbolId)} /
     * {@link #toJavaReportClass(com.regnosys.rosetta.ast.regulatory.RReport, com.rosetta.util.DottedPath)} /
     * {@link #toJavaRuleClass(ModelSymbolId)}). Replaces the ad-hoc
     * {@code toReportFunctionJavaClass(DottedPath, RReport)} method that
     * shipped at T6.
     *
     * <p><b>Fork divergence vs upstream:</b> upstream's {@code RFunction}
     * carries its own {@code getSymbolId()} / {@code getReportId()}
     * accessors, so the dispatcher takes only an {@link RFunction}. The fork's
     * {@link RFunction} has no such accessors — symbol identity is resolved
     * through {@code GeneratorModel.symbolId(RFunction)} which understands
     * the synthetic-bridge recovery for {@link RFunction#fromRule(com.regnosys.rosetta.ast.functions.RRule)} /
     * {@link RFunction#fromReport(RReport)} factory output. To preserve
     * {@link JavaTypeTranslator}'s stateless contract (no GeneratorModel
     * dependency — used by 20+ call sites that don't have one), the caller
     * passes the pre-resolved {@link ModelSymbolId} as the second argument.
     * For REPORT-origin functions only {@code functionId.getNamespace()} is
     * load-bearing; the simple-name is derived from
     * {@link RFunction#originReport()} body+corpus references via the private
     * {@link #toJavaReportClass(com.regnosys.rosetta.ast.regulatory.RReport, com.rosetta.util.DottedPath)}
     * router.
     *
     * @param func the function to translate (carries the origin discriminator
     *        plus, for REPORT-origin, the source RReport via
     *        {@link RFunction#originReport()})
     * @param functionId the pre-resolved {@link ModelSymbolId} (caller composes
     *        as {@code generatorModel.symbolId(func)})
     * @return the generated class shape per {@link RFunction.Origin}:
     *         FUNCTION → {@code <ns>.functions/<Name>};
     *         REPORT   → {@code <ns>.reports/<body+corpus>ReportFunction};
     *         RULE     → {@code <ns>.reports/<Name>Rule}
     */
    public RGeneratedJavaClass<? extends RosettaFunction> toFunctionJavaClass(
            RFunction func, ModelSymbolId functionId) {
        return switch (func.origin()) {
            case FUNCTION -> toJavaFunctionClass(functionId);
            case REPORT -> {
                RReport report = func.originReport().orElseThrow(() -> new IllegalStateException(
                        "RFunction.origin() == REPORT but originReport() is empty — "
                        + "RFunction.fromReport(RReport) must always set both. function name="
                        + func.name()));
                @SuppressWarnings({"unchecked", "rawtypes"})
                RGeneratedJavaClass<? extends RosettaFunction> reportFn =
                        (RGeneratedJavaClass) toJavaReportClass(report, functionId.getNamespace());
                yield reportFn;
            }
            case RULE -> toJavaRuleClass(functionId);
        };
    }

    /**
     * Phase X T6.0.5 — FUNCTION-origin router. Mirrors upstream lines 117-121:
     * {@code <namespace>.functions/<Name>} with {@link RosettaFunction} as the
     * declared subtype.
     *
     * <p>Used by both the BC entry point {@link #toFunctionJavaClass(ModelSymbolId)}
     * and the principled dispatcher
     * {@link #toFunctionJavaClass(RFunction, ModelSymbolId)} for the FUNCTION
     * case.
     */
    private RGeneratedJavaClass<? extends RosettaFunction> toJavaFunctionClass(
            ModelSymbolId functionId) {
        DottedPath funcPackage = functionId.getNamespace().child("functions");
        return RGeneratedJavaClass.create(
                JavaPackageName.escape(funcPackage), functionId.getName(), RosettaFunction.class);
    }

    /**
     * Phase X T6.0.5 — REPORT-origin router. Mirrors upstream lines 122-126:
     * {@code <namespace>.reports/<body+corpus>ReportFunction} with
     * {@link ReportFunction} as the declared subtype.
     *
     * <p>The simple name is composed from the report's regulatory body
     * reference plus every corpus reference (no separator) — equivalent to
     * upstream's {@code ModelReportId.joinRegulatoryReference()}. Fails fast
     * if {@code report.regulatoryDocRef().bodyRef()} is absent or empty —
     * returning a literal fallback would collide across reports.
     */
    private RGeneratedJavaClass<? extends ReportFunction<?, ?>> toJavaReportClass(
            RReport report, DottedPath namespace) {
        RRegulatoryDocumentReference ref = report.regulatoryDocRef();
        if (ref == null || ref.bodyRef() == null || ref.bodyRef().isEmpty()) {
            throw new IllegalStateException(
                    "toJavaReportClass: RReport must have "
                    + "regulatoryDocRef.bodyRef set; got: " + report);
        }
        StringBuilder alphanumeric = new StringBuilder(ref.bodyRef());
        for (String corpus : ref.corpusRefs()) {
            if (corpus != null) {
                alphanumeric.append(corpus);
            }
        }
        DottedPath reportPackage = namespace.child("reports");
        String simpleName = alphanumeric + "ReportFunction";
        return RGeneratedJavaClass.create(
                JavaPackageName.escape(reportPackage), simpleName,
                new TypeReference<ReportFunction<?, ?>>() {});
    }

    /**
     * Phase X T6.0.5 — RULE-origin router. Mirrors upstream lines 127-131:
     * {@code <namespace>.reports/<Name>Rule} with {@link ReportFunction} as
     * the declared subtype (rules emit as {@code ReportFunction} subclasses
     * per upstream).
     *
     * <p><b>Critical correction vs T5:</b> the T5 RuleGenerator routed rules
     * to {@code <namespace>.functions/<Name>} via the FUNCTION-origin
     * {@link #toFunctionJavaClass(ModelSymbolId)} call. That was wrong —
     * upstream emits rules under {@code <namespace>.reports/<Name>Rule}.
     * T6.0.5 corrects this via the principled origin dispatch.
     */
    private RGeneratedJavaClass<? extends RosettaFunction> toJavaRuleClass(
            ModelSymbolId ruleId) {
        DottedPath rulePackage = ruleId.getNamespace().child("reports");
        String simpleName = ruleId.getName() + "Rule";
        @SuppressWarnings({"unchecked", "rawtypes"})
        RGeneratedJavaClass<? extends RosettaFunction> ruleClass =
                (RGeneratedJavaClass) RGeneratedJavaClass.create(
                        JavaPackageName.escape(rulePackage), simpleName,
                        new TypeReference<ReportFunction<?, ?>>() {});
        return ruleClass;
    }

    /**
     * Compute the deep path utility class for a data/choice type.
     * E.g., type "Asset" in namespace "cdm.base.staticdata.asset.common"
     * → {@code AssetDeepPathUtil} in {@code cdm.base.staticdata.asset.common.util}.
     *
     * <p>Deep path utility classes are generated for types that are eligible for
     * deep feature calls ({@code ->>} operator). They contain {@code chooseXxx}
     * methods that navigate through the one-of type hierarchy to find the
     * requested attribute.
     */
    public RGeneratedJavaClass<?> toDeepPathUtilJavaClass(ModelSymbolId typeId) {
        DottedPath utilPackage = typeId.getNamespace().child("util");
        String simpleName = typeId.getName() + "DeepPathUtil";
        return RGeneratedJavaClass.create(
                JavaPackageName.escape(utilPackage), simpleName, Object.class);
    }

    // -- Phase X T2.5: label-provider + meta-attribute resolution -------------

    /**
     * Phase X T2.5 — derive the generated label-provider class for an
     * {@link RFunction}-derived {@link ModelSymbolId}. Upstream
     * {@code LabelProviderGenerator.xtend} (rune-dsl 9.83.0-line upstream line 138-142)
     * computes the package as {@code function.getNamespace().child("labels")}
     * and the class simple name as
     * {@code function.getAlphanumericName() + "LabelProvider"}.
     *
     * <p><b>Fork divergence:</b> upstream's signature is
     * {@code toLabelProviderJavaClass(RFunction)} reading
     * {@code function.getNamespace() / getAlphanumericName()}. Neither accessor
     * exists on the fork's {@code RFunction}. The fork resolves a function's
     * fully-qualified identity through {@code GeneratorModel.symbolId(RFunction)},
     * so this method takes the resolved {@link ModelSymbolId} directly —
     * matching the existing {@code toFunctionJavaClass(ModelSymbolId)} +
     * {@code toConditionJavaClass(ModelSymbolId, String)} +
     * {@code toJavaMetaDataClass(ModelSymbolId)} convention on this class.
     * Call sites compose as
     * {@code typeTranslator.toLabelProviderJavaClass(generatorModel.symbolId(function))}.
     *
     * @param functionId the resolved function identifier (namespace + name)
     * @return the generated label-provider class — package
     *         {@code <funcNamespace>.labels}, simple name
     *         {@code <funcName>LabelProvider}, declared subtype of
     *         {@link LabelProvider}
     */
    public RGeneratedJavaClass<? extends LabelProvider> toLabelProviderJavaClass(
            ModelSymbolId functionId) {
        DottedPath labelPackage = functionId.getNamespace().child("labels");
        String simpleName = functionId.getName() + "LabelProvider";
        return RGeneratedJavaClass.create(
                JavaPackageName.escape(labelPackage), simpleName, LabelProvider.class);
    }

    // -- Phase X T2.5: meta-typed attribute resolution ------------------------

    /**
     * Phase X T2.5 — resolve the metafield-aware Java type for an
     * {@link RAttribute}. Upstream
     * {@code JavaTypeTranslator.toMetaJavaType(RAttribute)} (rune-dsl 9.83.0-line upstream
     * line 171-181) returns the meta-wrapped item type
     * ({@code FieldWithMeta<T>}-family) when the attribute carries meta
     * annotations, else the bare reference type. Cardinality-driven
     * {@code List} wrapping is layered on top.
     *
     * <p><b>Fork divergence (T2.5 minimal-scope contract):</b>
     * <ul>
     *   <li>Upstream reads
     *       {@code attr.getRMetaAnnotatedType().getRType()} to recover the
     *       underlying {@link RType}. The fork's {@link RAttribute} has no
     *       {@code getRMetaAnnotatedType()} surface (M4 type-resolution lives
     *       on {@code GeneratorModel}), so this method takes the
     *       caller-resolved {@link RType} as a second argument.</li>
     *   <li>Meta-presence detection mirrors {@code FunctionGenerator.hasMeta}
     *       (private static helper at FunctionGenerator.java:710) — a simple
     *       scan of {@link RAttribute#annotationRefs()} for any ref whose
     *       {@code annotationName()} equals {@code "metadata"}.</li>
     *   <li>When meta is absent, returns {@link #toJavaReferenceType(RType)}
     *       (boxed primitive when applicable).</li>
     *   <li>When meta is present, returns
     *       {@code JavaParameterizedType.from(typeUtil.FIELD_WITH_META, bareRefType)} —
     *       the runtime {@code com.rosetta.model.lib.meta.FieldWithMeta<T>}
     *       generic declaration (already exposed at {@code JavaTypeUtil.FIELD_WITH_META})
     *       parameterised with the bare reference type. The concrete per-namespace
     *       wrapper now exists in the fork ({@link RJavaWithMetaValue} +
     *       {@code RJavaFieldWithMeta}/{@code RJavaReferenceWithMeta}, ported at engine
     *       PR #8 / PR-A; constructed via {@link RJavaWithMetaValue#create}), but this
     *       method is intentionally left returning the generic
     *       {@code FieldWithMeta<T>}.
     *       <p><b>Not the meta-coercion lever (corrected at engine PR #9 / PR-B).</b>
     *       An earlier revision of this Javadoc claimed that switching this method to
     *       the per-namespace wrapper was "PR-B's activation step ... the lever that
     *       flips the meta-coercion waivers". That is FALSE and was verified so: this
     *       method has exactly two live call sites ({@code RuleGenerator} and
     *       {@code ReportGenerator}), both feeding only the
     *       {@code implements ReportFunction<I,O>} base-interface type-arguments, where
     *       {@link #hasMetaAnnotation} is false for every emitted rule/report I/O and no
     *       golden carries a meta type-argument — so switching it changes ZERO bytes
     *       (a no-op against the goldens). The actual meta-coercion lever is the
     *       navigation-receiver path:
     *       {@code ExpressionCompiler.coerceNavigationReceiver} +
     *       {@code NavigationHandler.metaNavResultType} (mirroring upstream
     *       {@code attributeCall}), which surfaces a concrete {@link RJavaWithMetaValue}
     *       into a navigation receiver's expression type so the PR-A coercer emits the
     *       golden {@code .getValue()} unwrap. Switching this method to the concrete
     *       wrapper is therefore deferred as unnecessary, not load-bearing.
     *       <p>Using the runtime {@code FieldWithMeta<T>} parameterised type lets
     *       callers detect the meta path via the simple-name shape
     *       ({@code "FieldWithMeta<bareType>"}) and is the same generic the fork's POJO
     *       codegen already references. Constructor BC of {@link JavaTypeTranslator} is
     *       preserved (existing 1-arg ctor still in use across 15+ call sites).</li>
     *   <li>Cardinality {@code List<...>} wrapping (upstream lines 173-180)
     *       is also deferred to the T5/T6 caller — the {@code ReportFunction<I,O>}
     *       base-interface use case at spec § 3.3 reads the singular item
     *       type for the type parameters, not a list-wrapped form.</li>
     * </ul>
     *
     * @param attribute the attribute being resolved (read for meta annotations)
     * @param resolvedType the {@link RType} the caller already resolved via
     *        {@code GeneratorModel.getType(attribute)} or equivalent
     * @return the bare reference java type, or
     *         {@code FieldWithMeta<bareRefType>} when the attribute carries a
     *         {@code [metadata ...]} annotation
     */
    public JavaType toMetaJavaType(RAttribute attribute, RType resolvedType) {
        JavaClass<?> bareRefType = toJavaReferenceType(resolvedType);
        if (!hasMetaAnnotation(attribute)) {
            return bareRefType;
        }
        return JavaParameterizedType.from(typeUtil.FIELD_WITH_META, bareRefType);
    }

    /**
     * Wrap a resolved java type in {@code List<...>} for the
     * {@code ReportFunction<I, List<O>>} base interface of a multi-valued
     * reporting rule (facet reportOutputCardinality, PR #272). The cardinality
     * {@code List<...>} wrapping that {@link #toMetaJavaType(RAttribute, RType)}
     * defers to its caller (see that method's javadoc) lives here. Uses the
     * polymorphic upper-bounded form {@code List<? extends O>} for a MODEL-typed
     * element (a {@code JavaPojoInterface}, e.g. {@code PricePeriod}) and the
     * invariant {@code List<O>} for a primitive/enum element ({@code Boolean},
     * {@code String}) — matching golden's base-interface shape exactly (the
     * emitted method signatures stay invariant {@code List<O>} via
     * {@code FunctionGenerator}'s {@code isMulti(output)} standard-model path).
     *
     * <p>{@code modelTyped} selects the polymorphic {@code ? extends} form. It is
     * the caller's responsibility because {@code wrapExtendsIfNotFinal}'s own
     * {@code instanceof JavaPojoInterface} check does NOT recognise a model
     * type emitted as an {@code RGeneratedJavaClass} (the #169 lesson: pojo-ness
     * is tested at the RTYPE level — {@code RDataTypeRef}/{@code RChoiceTypeRef} —
     * not on the rendered Java class).
     */
    public JavaType listWrap(JavaType inner, boolean modelTyped) {
        return modelTyped
                ? typeUtil.wrapExtends(typeUtil.LIST, inner)
                : typeUtil.wrap(typeUtil.LIST, inner);
    }

    /**
     * Phase X T2.5 — mirror of the private static
     * {@code FunctionGenerator.hasMeta(RAttribute)} helper. Kept private here
     * so {@link JavaTypeTranslator} does not take a cross-class dependency
     * on {@code FunctionGenerator}; the literal {@code "metadata"} annotation
     * name is the contract.
     *
     * <p><b>Legacy-plugin parity scope:</b> the fork's existing
     * {@code FunctionGenerator.hasMeta} matches the annotation name
     * {@code "metadata"} ONLY. Upstream's {@code RAttribute.getRMetaAnnotatedType()}
     * recognises a wider surface ({@code metadata} + {@code reference} +
     * {@code location} + {@code scheme}). Mirroring the existing fork helper
     * preserves byte-parity with today's legacy plugin output at rune-dsl
     * 9.83.0-line upstream — extending the set here would risk introducing a byte-diff
     * the legacy plugin does not produce. T3 LabelProviderGenerator +
     * T5 RuleGenerator + T6 ReportGenerator implementers should rely on
     * the D11 byte-diff at T7/T8 to surface any real {@code reference} /
     * {@code location} / {@code scheme} divergence: if the legacy goldens
     * include a meta-wrapped emission for such attributes, this method must
     * be extended (and {@code FunctionGenerator.hasMeta} alongside it for
     * consistency).
     */
    private static boolean hasMetaAnnotation(RAttribute attribute) {
        for (RAnnotationRef ref : attribute.annotationRefs()) {
            if ("metadata".equals(ref.annotationName())) {
                return true;
            }
        }
        return false;
    }

    // -- Package path helpers -------------------------------------------------

    private DottedPath validation(DottedPath p) {
        return p.child("validation");
    }

    private DottedPath existsValidation(DottedPath p) {
        return validation(p).child("exists");
    }
}
