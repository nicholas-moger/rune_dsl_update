package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.expressions.unary.RCardinalityCheckExpr;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RPostCondition;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.validation.*;

/**
 * Naming-convention validation aligned to the upstream RELEASED-9.83.0
 * surface (the #454/#455 severity-oracle law: the released
 * {@code org.finos.rune:rune-lang:9.83.0} jar's bytecode and constant pool;
 * on this family the vendored tree and the released artifact agree —
 * six arms across five upstream validators, every one a {@code warning}).
 *
 * <p>The upstream populations, reproduced exactly:
 * <ul>
 *   <li><b>Type names</b> — upstream {@code TypeValidator.checkTypeNameIsCapitalized(Data)}
 *       fires on {@code Data} ONLY. {@code Choice extends Data} in the upstream
 *       Ecore (RosettaSimple.xcore:112), so choices fire the same message.
 *       {@code RosettaTypeAlias} is NOT a {@code Data} — type-alias names are
 *       NEVER checked upstream (the #456 alignment: the fork's former
 *       type-alias arm fired 48 corpus rows the V0 banks never carry — the
 *       builtins' {@code typeAlias int/productType/eventType/calculation} and
 *       iso20022's 40 lowercase dtcc-rds aliases). Basic types, record types,
 *       library functions, rules and reports are equally outside the upstream
 *       population.</li>
 *   <li><b>Enumeration names</b> — {@code EnumValidator.checkEnumNameIsCapitalized}.
 *       Enum VALUES carry no upstream naming check.</li>
 *   <li><b>Function names</b> — {@code FunctionValidator.checkFunctionNameStartsWithCapital}
 *       on {@code simple.Function}; library functions are
 *       {@code RosettaExternalFunction} upstream — unchecked.</li>
 *   <li><b>Condition names</b> — {@code ConditionValidator.checkConditionName}
 *       fires per {@code Condition} EObject wherever contained (data types,
 *       type aliases via {@code RosettaTypeWithConditions}, functions — and
 *       post-conditions, which upstream folds into {@code Condition} with a
 *       flag): an UNNAMED condition warns "Condition name should be specified"
 *       (INVALID_NAME, NO suppression gate) unless it is a CONSTRAINT
 *       condition ({@code isOneOf || isChoice} — the fork's
 *       {@link RCardinalityCheckExpr} covers exactly those two ops; the
 *       corpus's 54 unnamed conditions are all constraints, which is why the
 *       V0 banks are silent); a NAMED lowercase condition warns
 *       "Condition name should start with a capital" (INVALID_CASE, gated on
 *       the condition's OWN suppression — upstream {@code Condition} is
 *       {@code Annotated}). {@code RPostCondition} carries no annotation
 *       refs, so its suppression read is constant-false — and upstream's
 *       {@code postCondition} GRAMMAR rule carries no annotation segment
 *       either (only the model class is Annotated), so constant-false is
 *       exactly upstream's parsed behavior, not a divergence.</li>
 *   <li><b>Attribute names</b> — {@code AttributeValidator.checkAttributeNameStartsWithLowerCase}
 *       fires on every {@code Attribute} EXCEPT choice options
 *       ({@code ChoiceOption extends Attribute}, explicitly excluded — options
 *       are type-named by design) and annotation-declaration attributes
 *       (excluded via the {@code eContainer() instanceof Annotation} gate —
 *       the cdm {@code annotation creation:} block's uppercase
 *       {@code BusinessEvent}/{@code WorkflowStep} rows and the builtins'
 *       serialization-format attributes are silent upstream). Function INPUTS
 *       and OUTPUT are {@code Attribute}s upstream (RosettaSimple.xcore:162-163)
 *       and ARE checked. Suppression is the attribute's own OR its
 *       container's (upstream ORs {@code eContainer()} — the drr iosco
 *       {@code AnnaDsb*} types carry {@code [suppressWarnings capitalisation]}
 *       at type level over their uppercase attributes, the reason the drr
 *       bank is silent there).</li>
 * </ul>
 *
 * <p>Message bytes are the released constant-pool strings verbatim.
 * All arms are warnings ({@code warning(...)} in the released bytecode,
 * disassembly-verified).
 *
 * <p>Suppression-semantics note: upstream's {@code WarningSuppressionHelper}
 * matches the warning category against the {@code suppressWarnings}
 * annotation DECLARATION's declared attribute names (any
 * {@code [suppressWarnings ...]} ref suppresses every category the
 * declaration declares); the fork's {@link WarningSuppressionHelper} matches
 * the REF's qualifier. On the corpus the two agree everywhere: the only
 * carriers are the iosco {@code [suppressWarnings capitalisation]} refs and
 * the builtin declaration declares exactly {@code capitalisation boolean (0..1)}.
 * The divergence is confined to exotic off-corpus refs (a bare or
 * differently-qualified ref against a multi-category declaration).
 */
public final class NamingValidator implements Validator {

    private static final String SUPPRESSION_CODE = "capitalisation";

    // The released 9.83.0 message bytes (constant-pool verbatim).
    private static final String TYPE_MSG = "Type name should start with a capital";
    private static final String ENUM_MSG = "Enumeration name should start with a capital";
    private static final String FUNC_MSG = "Function name should start with a capital";
    private static final String COND_UNNAMED_MSG = "Condition name should be specified";
    private static final String COND_CASE_MSG = "Condition name should start with a capital";
    private static final String ATTR_MSG = "Attribute name should start with a lower case";

    @Override
    public void validate(RRootElement element, ValidationCollector collector) {
        if (element instanceof RDataType dt) {
            checkCapital(dt, dt.name(), TYPE_MSG, collector);
            for (RAttribute attr : dt.attributes()) {
                checkAttributeCase(dt, attr, collector);
            }
            for (RCondition cond : dt.conditions()) {
                checkConditionName(dt, cond, collector);
            }
        }
        if (element instanceof RChoice ct) {
            // Choice extends Data upstream — the Data arm's message applies.
            // Choice OPTIONS are ChoiceOption upstream, excluded from the
            // attribute check (not iterated here).
            checkCapital(ct, ct.name(), TYPE_MSG, collector);
        }
        if (element instanceof REnumeration en) {
            checkCapital(en, en.name(), ENUM_MSG, collector);
        }
        if (element instanceof RTypeAlias ta) {
            // NO name check — upstream RosettaTypeAlias is not a Data.
            // Its conditions are still Condition EObjects upstream.
            for (RCondition cond : ta.conditions()) {
                checkConditionName(ta, cond, collector);
            }
        }
        if (element instanceof RFunction fn) {
            checkCapital(fn, fn.name(), FUNC_MSG, collector);
            for (RAttribute input : fn.inputs()) {
                checkAttributeCase(fn, input, collector);
            }
            fn.output().ifPresent(out -> checkAttributeCase(fn, out, collector));
            for (RCondition cond : fn.conditions()) {
                checkConditionName(fn, cond, collector);
            }
            for (RPostCondition post : fn.postConditions()) {
                checkPostConditionName(fn, post, collector);
            }
        }
    }

    private void checkCapital(RRootElement element, String name, String message,
            ValidationCollector collector) {
        if (name == null || name.isEmpty()) return;
        if (Character.isLowerCase(name.charAt(0))
                && !WarningSuppressionHelper.isSuppressed(element, SUPPRESSION_CODE)) {
            collector.warning(element.sourceRange(), message, ValidationIssueCode.INVALID_CASE);
        }
    }

    private void checkConditionName(RRootElement enclosing, RCondition cond,
            ValidationCollector collector) {
        SourceRange range = cond.sourceRange() != null ? cond.sourceRange() : enclosing.sourceRange();
        String name = cond.name().orElse(null);
        if (name == null) {
            // Upstream: unnamed conditions warn UNLESS constraint conditions
            // (isOneOf || isChoice — the fork parses one-of and
            // required/optional choice as RCardinalityCheckExpr, exactly the
            // upstream pair). NO suppression gate on this arm.
            if (!(cond.expression() instanceof RCardinalityCheckExpr)) {
                collector.warning(range, COND_UNNAMED_MSG, ValidationIssueCode.INVALID_NAME);
            }
        } else if (!name.isEmpty() && Character.isLowerCase(name.charAt(0))
                && !WarningSuppressionHelper.isSuppressedRefs(cond.annotationRefs(), SUPPRESSION_CODE)) {
            collector.warning(range, COND_CASE_MSG, ValidationIssueCode.INVALID_CASE);
        }
    }

    private void checkPostConditionName(RRootElement enclosing, RPostCondition post,
            ValidationCollector collector) {
        // Upstream post-conditions ARE Conditions (postCondition flag) — the
        // same name check applies. The fork grammar gives post-conditions no
        // annotation refs, so the INVALID_CASE arm has no suppression read.
        SourceRange range = post.sourceRange() != null ? post.sourceRange() : enclosing.sourceRange();
        String name = post.name().orElse(null);
        if (name == null) {
            if (!(post.expression() instanceof RCardinalityCheckExpr)) {
                collector.warning(range, COND_UNNAMED_MSG, ValidationIssueCode.INVALID_NAME);
            }
        } else if (!name.isEmpty() && Character.isLowerCase(name.charAt(0))) {
            collector.warning(range, COND_CASE_MSG, ValidationIssueCode.INVALID_CASE);
        }
    }

    private void checkAttributeCase(RRootElement enclosing, RAttribute attr,
            ValidationCollector collector) {
        String name = attr.name();
        if (name == null || name.isEmpty()) return;
        if (Character.isUpperCase(name.charAt(0))
                && !WarningSuppressionHelper.isSuppressedRefs(attr.annotationRefs(), SUPPRESSION_CODE)
                && !WarningSuppressionHelper.isSuppressed(enclosing, SUPPRESSION_CODE)) {
            collector.warning(
                attr.sourceRange() != null ? attr.sourceRange() : enclosing.sourceRange(),
                ATTR_MSG,
                ValidationIssueCode.INVALID_CASE);
        }
    }
}
