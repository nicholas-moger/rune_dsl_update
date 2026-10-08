package com.regnosys.rosetta.validation.validators;

import com.regnosys.rosetta.ast.RBinaryExpression;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.enums.ArithOp;
import com.regnosys.rosetta.ast.enums.CardMod;
import com.regnosys.rosetta.ast.enums.CompOp;
import com.regnosys.rosetta.ast.enums.EqOp;
import com.regnosys.rosetta.ast.enums.LogOp;
import com.regnosys.rosetta.ast.expressions.binary.*;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.supporting.ROnlyExistsElement;
import com.regnosys.rosetta.ast.expressions.unary.*;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RParameter;
import com.regnosys.rosetta.ast.types.RLibraryFunction;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.types.*;
import com.regnosys.rosetta.types.inference.CardinalityComputer;
import com.regnosys.rosetta.types.inference.TypeDirectedResolver;
import com.regnosys.rosetta.types.inference.TypeInferenceEngine;
import com.regnosys.rosetta.types.relation.SubtypeRelation;
import com.regnosys.rosetta.types.relation.TypeJoin;
import com.regnosys.rosetta.validation.ValidationCollector;
import com.regnosys.rosetta.validation.ValidationIssueCode;
import com.regnosys.rosetta.validation.Validator;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Validates expression semantics:
 * - Arithmetic operands follow upstream's type algebra ({@code +}: number/string/date+time;
 *   {@code -}: number/date−date; {@code *} {@code /}: numeric only)
 * - Equality/contains/disjoint operands must be comparable; comparison operands follow
 *   upstream's ordered-type ladder (number/date/zonedDateTime)
 * - Modified ({@code all}/{@code any}) binary operations carry upstream's
 *   cardinality-shape warnings; unmodified comparisons warn per-operand
 * - Logical operands must be boolean (fork-native bytes) and single (upstream warning)
 * - List literals and conditional branches must share a common supertype
 * - Call sites check arity (error), per-argument cardinality (warning) and
 *   per-argument assignability (error)
 * - {@code only exists} carries upstream's five structural error classes + the
 *   parent-cardinality warning + the choice-option deprecation warning
 * - The path operator on a choice-typed receiver carries upstream's
 *   deprecation warning (all three fork parse shapes — feature calls,
 *   disguised-nav EVRs, condition-context chains); list operations over
 *   single receivers warn across the full released {@code ListOperation}
 *   set; constructor pairs and set/add operations warn on list-to-single
 *   assignment (facet warningFamilyWaves, PR #455)
 *
 * <p>Upstream: {@code validation/expression/ExpressionValidator} +
 * {@code AbstractExpressionValidator} (message assembly) +
 * {@code ConstructorValidator} (the pair seat) +
 * {@code RosettaSimpleValidator.checkUnaryOperation} (the list-op seat).
 * See the {@code annexAReleasedSeverities} facet note below for the
 * severity oracle.
 */
public final class ExpressionValidator implements Validator {

    private final TypeInferenceEngine typeEngine;
    private final SubtypeRelation subtypeRelation;
    private final CardinalityComputer cardinalityComputer;
    private final TypeJoin typeJoin;
    private final TypeDirectedResolver typeResolver;

    public ExpressionValidator(TypeInferenceEngine typeEngine, SubtypeRelation subtypeRelation,
                                CardinalityComputer cardinalityComputer, TypeJoin typeJoin,
                                TypeDirectedResolver typeResolver) {
        this.typeEngine = typeEngine;
        this.subtypeRelation = subtypeRelation;
        this.cardinalityComputer = cardinalityComputer;
        this.typeJoin = typeJoin;
        this.typeResolver = typeResolver;
    }

    @Override
    public void validate(RRootElement element, ValidationCollector collector) {
        for (var expr : AstWalker.findAll(element, RExpression.class)) {
            if (expr instanceof RArithmeticExpr arith) {
                checkArithmeticOperands(arith, collector);
            }
            if (expr instanceof RComparisonExpr comp) {
                checkComparisonOperation(comp, collector);
            }
            if (expr instanceof RLogicalExpr log) {
                checkLogicalOperands(log, collector);
                checkLogicalCardinality(log, collector);
            }
            if (expr instanceof RContainsExpr cont) {
                checkContainsOperands(cont, collector);
            }
            if (expr instanceof RDisjointExpr dis) {
                checkDisjointOperands(dis, collector);
            }
            // T14: Cardinality checks
            if (expr instanceof RArithmeticExpr arith2) {
                checkOperandsSingle(arith2, collector);
            }
            if (expr instanceof RConditionalExpr cond) {
                checkConditionIsSingle(cond, collector);
                checkConditionalBranchTypes(cond, collector);
            }
            // T15: List operation checks
            if (expr instanceof RFilterExpr filter) {
                checkFilterBodyIsBoolean(filter, collector);
            }
            // PR #437 — the recorded validation item (the two false-NEGATIVE pins)
            if (expr instanceof REqualityExpr eq) {
                checkEqualityComparability(eq, collector);
                if (eq.mod().isPresent()) {
                    checkModifiedBinaryOperation(eq, eq.mod().get(),
                        eq.op() == EqOp.EQ ? "=" : "<>", collector);
                } else {
                    checkEqualityCardinality(eq, collector);
                }
            }
            // PR #455: the #437 only-element arm widened to the full released
            // ListOperation set (see checkListOperationSingleReceiver).
            if (expr instanceof RListOpExpr listOp) {
                checkListOperationSingleReceiver(listOp.argument(),
                    listOpOperatorName(listOp.op()), collector);
                if (listOp.op() == com.regnosys.rosetta.ast.enums.ListOp.FLATTEN) {
                    checkFlattenArgument(listOp, collector);
                }
            }
            if (expr instanceof RSortExpr sort) {
                checkListOperationSingleReceiver(sort.argument(), "sort", collector);
            }
            if (expr instanceof RMinExpr min) {
                checkListOperationSingleReceiver(min.argument(), "min", collector);
            }
            if (expr instanceof RMaxExpr max) {
                checkListOperationSingleReceiver(max.argument(), "max", collector);
            }
            if (expr instanceof RReduceExpr reduce) {
                checkListOperationSingleReceiver(reduce.argument(), "reduce", collector);
            }
            // PR #454 — the Annex-A family waves (see the annexAReleasedSeverities note)
            if (expr instanceof RListLiteral list) {
                checkListLiteralElements(list, collector);
            }
            if (expr instanceof RSymbolReference ref) {
                checkCallableReference(ref, collector);
                checkBareEnumTypeReference(ref, collector);
            }
            if (expr instanceof ROnlyExistsExpr oe) {
                checkOnlyExists(oe, collector);
            }
            if (expr instanceof RWithMetaExpr wm) {
                checkWithMetaSingle(wm, collector);
            }
            // PR #455 — the warning-family waves (facet warningFamilyWaves)
            if (expr instanceof RFeatureCall fc) {
                checkPathOperatorOnChoice(fc, collector);
            }
            if (expr instanceof com.regnosys.rosetta.ast.expressions.references.REnumValueRef evr) {
                checkPathOperatorOnChoiceEvr(evr, collector);
            }
            if (expr instanceof com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr ctor) {
                checkConstructorPairCardinality(ctor, collector);
            }
        }
        for (var op : AstWalker.findAll(element,
                com.regnosys.rosetta.ast.functions.ROperation.class)) {
            checkOperationAssignment(op, collector);
        }
    }

    // === T13: Operator type checks ===========================================

    /** The algebra's expected-type constants (upstream {@code RBuiltinTypeService} pairings). */
    private static final RMetaAnnotatedType TIME_NO_META =
            RMetaAnnotatedType.withNoMeta(RBasicType.TIME);
    private static final RMetaAnnotatedType DATE_NO_META =
            RMetaAnnotatedType.withNoMeta(RRecordType.DATE);
    private static final RMetaAnnotatedType STRING_NO_META =
            RMetaAnnotatedType.withNoMeta(RStringType.unconstrained());
    private static final RMetaAnnotatedType NUMBER_NO_META =
            RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained());

    /**
     * facet arithTypeAlgebra (PR #441 — the finding-#5 wave): upstream
     * {@code ExpressionValidator.checkArithmeticOperation}'s type algebra
     * (vendored rune-dsl {@code validation/expression/ExpressionValidator.java:144-187}),
     * messages byte-identical — {@code +} admits number+number, string+string
     * (concatenation) and date+time (the dateTime algebra); {@code -} admits
     * number−number and date−date (the day-count algebra); {@code *} and
     * {@code /} are numeric-only; a NOTHING left short-circuits every arm
     * (upstream tests nothing before the left-driven dispatch, and NOTHING
     * passes the {@code *}/{@code /} subtype checks as the bottom type).
     * Replaces the fork's pre-#441 numeric-only check, whose 64 false
     * positives on legal DRR string concatenations were the finding's corpus
     * footprint.
     *
     * <p>Cardinality stays at the fork's own T14 seat
     * ({@link #checkOperandsSingle}) — upstream's in-method isSingleChecks
     * are not part of the finding-#5 false-positive class.
     *
     * <p>Gated on BOTH operand types non-MISSING: a fork-unresolved side
     * declines the whole check (the PR #437 law — upstream never validates a
     * shape it cannot resolve, and a right-side arm driven by a MISSING left
     * would fabricate errors upstream cannot emit).
     */
    private void checkArithmeticOperands(RArithmeticExpr arith, ValidationCollector collector) {
        RExpression left = arith.left().orElse(null);
        RExpression right = arith.right().orElse(null);
        if (left == null || right == null) {
            return;
        }
        RMetaAnnotatedType leftType = typeEngine.getInferredType(left);
        RMetaAnnotatedType rightType = typeEngine.getInferredType(right);
        if (leftType.isMissing() || rightType.isMissing()) {
            return;
        }
        switch (arith.op()) {
            case PLUS -> {
                if (subtypeRelation.isSubtypeOf(leftType.type(), RBasicType.NOTHING)) {
                    // Do not check right type
                } else if (subtypeRelation.isSubtypeOf(leftType.type(), RRecordType.DATE)) {
                    subtypeCheck(TIME_NO_META, rightType, right,
                        actual -> "Cannot add `" + actual + "` to a `date`", collector);
                } else if (subtypeRelation.isSubtypeOf(leftType.type(), STRING_NO_META.type())) {
                    subtypeCheck(STRING_NO_META, rightType, right,
                        actual -> "Cannot add `" + actual + "` to a `string`", collector);
                } else if (subtypeRelation.isSubtypeOf(leftType.type(), NUMBER_NO_META.type())) {
                    subtypeCheck(NUMBER_NO_META, rightType, right,
                        actual -> "Cannot add `" + actual + "` to a `number`", collector);
                } else {
                    unsupportedTypeError(leftType, "+", left,
                        "Supported types are `number`, `string` and `date`", collector);
                    if (!subtypeRelation.isSubtypeOf(rightType.type(), RBasicType.TIME)
                            && !subtypeRelation.isSubtypeOf(rightType.type(), STRING_NO_META.type())
                            && !subtypeRelation.isSubtypeOf(rightType.type(), NUMBER_NO_META.type())) {
                        unsupportedTypeError(rightType, "+", right,
                            "Supported types are `number`, `string` and `time`", collector);
                    }
                }
            }
            case MINUS -> {
                if (subtypeRelation.isSubtypeOf(leftType.type(), RBasicType.NOTHING)) {
                    // Do not check right type
                } else if (subtypeRelation.isSubtypeOf(leftType.type(), RRecordType.DATE)) {
                    subtypeCheck(DATE_NO_META, rightType, right,
                        actual -> "Cannot subtract `" + actual + "` from a `date`", collector);
                } else if (subtypeRelation.isSubtypeOf(leftType.type(), NUMBER_NO_META.type())) {
                    subtypeCheck(NUMBER_NO_META, rightType, right,
                        actual -> "Cannot subtract `" + actual + "` from a `number`", collector);
                } else {
                    unsupportedTypeError(leftType, "-", left,
                        "Supported types are `number` and `date`", collector);
                    if (!subtypeRelation.isSubtypeOf(rightType.type(), RRecordType.DATE)
                            && !subtypeRelation.isSubtypeOf(rightType.type(), NUMBER_NO_META.type())) {
                        unsupportedTypeError(rightType, "-", right,
                            "Supported types are `number` and `date`", collector);
                    }
                }
            }
            case MULTIPLY, DIVIDE -> {
                String operator = arith.op() == ArithOp.MULTIPLY ? "*" : "/";
                Function<String, String> suggestion =
                    actual -> "Cannot use `" + actual + "` with operator `" + operator + "`";
                subtypeCheck(NUMBER_NO_META, leftType, left, suggestion, collector);
                subtypeCheck(NUMBER_NO_META, rightType, right, suggestion, collector);
            }
        }
    }

    /**
     * facet annexAReleasedSeverities (PR #454 — the Annex-A family waves): the
     * severity ORACLE for every check in this facet is the RELEASED 9.83.0
     * artifact ({@code org.finos.rune:rune-lang:9.83.0}, Maven Central — the
     * jar the corpus projects' own builds pin), whose bytecode DIVERGES from
     * the vendored source tree on exactly one axis: the released
     * {@code AbstractExpressionValidator.isSingleCheck}/{@code isMultiCheck}
     * emit {@code warning(...)} (a separate {@code isSingleCheckError} variant
     * — absent from the vendored tree — serves the with-meta seats), while the
     * vendored snapshot has them as errors (a post-release severity flip).
     * The V0 oracle streams prove the released behavior on the corpus: ZERO
     * validation errors + the in-family cardinality warnings (13 bare
     * "Expecting single cardinality" + 1 "remove the `any` modifier" + 1
     * disjoint-multi line across both banks). Every message template here was
     * verified byte-identical against the released constant pool
     * (javap: string constants + makeConcatWithConstants recipes). Type-shaped
     * checks ({@code subtypeCheck} / {@code comparableTypeCheck} /
     * {@code commonTypeCheck} / {@code unsupportedTypeError} / the direct
     * {@code error(...)} sites) are ERRORS in the released jar too — upstream's
     * corpus builds carry ZERO of them, so the V1 gate's empty validation
     * tables hold and any corpus firing is a measured regression.
     *
     * <p>Upstream {@code checkComparisonOperation} (vendored
     * {@code ExpressionValidator.java:241-268}): the unmodified form warns
     * per-operand on multi cardinality ("Did you mean to use `all` or `any`
     * in front of the `&lt;` operator?"); a modified form takes
     * {@code checkModifiedBinaryOperation}; the ordered-type LADDER errors —
     * zonedDateTime/date/number left-driven dispatch with "Cannot compare a
     * `X` to a `zonedDateTime|date|number`" and the two-sided
     * unsupported-type fallback. Replaces the fork-native natural-order check
     * (fork bytes "Expected comparable type for ... operand of comparison",
     * no lock carried them), whose boolean-is-ordered laxity was the pin's
     * recorded gap. NOTHING left short-circuits the ladder exactly like the
     * arithmetic seat (the bottom type passes every subtype test).
     */
    private void checkComparisonOperation(RComparisonExpr comp, ValidationCollector collector) {
        RExpression left = comp.left().orElse(null);
        RExpression right = comp.right().orElse(null);
        if (left == null || right == null) {
            return;
        }
        String operator = compOpLiteral(comp.op());
        if (comp.mod().isPresent()) {
            checkModifiedBinaryOperation(comp, comp.mod().get(), operator, collector);
        } else {
            String suggestion = "Did you mean to use `all` or `any` in front of the `"
                    + operator + "` operator?";
            singleCardinalityWarning(left, suggestion, collector);
            singleCardinalityWarning(right, suggestion, collector);
        }
        RMetaAnnotatedType leftType = typeEngine.getInferredType(left);
        RMetaAnnotatedType rightType = typeEngine.getInferredType(right);
        if (leftType.isMissing() || rightType.isMissing()) {
            return;
        }
        if (subtypeRelation.isSubtypeOf(leftType.type(), RBasicType.NOTHING)) {
            // Do not check right type
        } else if (subtypeRelation.isSubtypeOf(leftType.type(), RRecordType.ZONED_DATE_TIME)) {
            subtypeCheck(ZONED_DATE_TIME_NO_META, rightType, right,
                actual -> "Cannot compare a `" + actual + "` to a `zonedDateTime`", collector);
        } else if (subtypeRelation.isSubtypeOf(leftType.type(), RRecordType.DATE)) {
            subtypeCheck(DATE_NO_META, rightType, right,
                actual -> "Cannot compare a `" + actual + "` to a `date`", collector);
        } else if (subtypeRelation.isSubtypeOf(leftType.type(), NUMBER_NO_META.type())) {
            subtypeCheck(NUMBER_NO_META, rightType, right,
                actual -> "Cannot compare a `" + actual + "` to a `number`", collector);
        } else {
            unsupportedTypeError(leftType, operator, left,
                "Supported types are `number`, `date` and `zonedDateTime`", collector);
            if (!subtypeRelation.isSubtypeOf(rightType.type(), RRecordType.ZONED_DATE_TIME)
                    && !subtypeRelation.isSubtypeOf(rightType.type(), RRecordType.DATE)
                    && !subtypeRelation.isSubtypeOf(rightType.type(), NUMBER_NO_META.type())) {
                unsupportedTypeError(rightType, operator, right,
                    "Supported types are `number`, `date` and `zonedDateTime`", collector);
            }
        }
    }

    private static String compOpLiteral(CompOp op) {
        return switch (op) {
            case LT -> "<";
            case GT -> ">";
            case LTE -> "<=";
            case GTE -> ">=";
        };
    }

    private void checkLogicalOperands(RLogicalExpr log, ValidationCollector collector) {
        checkOperandIsBoolean(log.left().orElse(null), "left", "logical", collector);
        checkOperandIsBoolean(log.right().orElse(null), "right", "logical", collector);
    }

    /**
     * Upstream {@code checkContainsExpression} (vendored
     * {@code ExpressionValidator.java:271-274}): the left operand must be
     * MULTI — released-9.83.0 WARNING with the {@code Did you mean to use the
     * `=` operator instead?} suggestion — and the operands comparable (the
     * facet-verified {@code are not comparable} ERROR bytes, replacing the
     * fork-native {@code Incompatible types in 'contains'} form; the
     * either-subtype predicate is IDENTICAL — upstream
     * {@code TypeSystem.isComparable}).
     */
    private void checkContainsOperands(RContainsExpr cont, ValidationCollector collector) {
        cont.left().ifPresent(l -> multiCardinalityWarning(l,
            "Did you mean to use the `=` operator instead?", collector));
        comparableTypeCheck(cont, cont.left().orElse(null), cont.right().orElse(null), collector);
    }

    /**
     * Upstream {@code checkDisjointExpression} (vendored
     * {@code ExpressionValidator.java:277-281}): BOTH operands must be MULTI
     * (released WARNING — the drr V0 bank carries the witness line {@code
     * Expecting multi cardinality. The `disjoint` operator requires a multi
     * cardinality input}) and comparable. Upstream anchors both cardinality
     * checks at the LEFT feature (a vendored-source anchoring quirk) — the
     * fork mirrors that anchor since the PR #458 anchor wave (the mojo-seam
     * stream measured the fork's own-operand anchor against the drr bank's
     * left-sited witness line); message bytes identical.
     */
    private void checkDisjointOperands(RDisjointExpr dis, ValidationCollector collector) {
        String suggestion = "The `disjoint` operator requires a multi cardinality input";
        SourceRange leftAnchor = dis.left().map(RExpression::sourceRange)
                .orElse(dis.sourceRange());
        dis.left().ifPresent(l -> multiCardinalityWarning(l, leftAnchor, suggestion, collector));
        dis.right().ifPresent(r -> multiCardinalityWarning(r, leftAnchor, suggestion, collector));
        comparableTypeCheck(dis, dis.left().orElse(null), dis.right().orElse(null), collector);
    }

    // === T14: Cardinality checks ==============================================

    /**
     * facet warningFamilyWaves (PR #455): upstream
     * {@code checkArithmeticOperation}'s leading per-operand
     * {@code isSingleCheck(left/right, op, LEFT/RIGHT, op)} — the
     * released-9.83.0 bytecode emits the cardinality pair as WARNINGS
     * (the #454 severity oracle; the fork's pre-#455 ERROR here was the
     * recorded #454 OBS severity divergence, corpus-silent only through
     * the single-biased global {@code compute()}), message
     * {@code Expecting single cardinality. The `<op>` operator requires a
     * single cardinality input} anchored at the operand (upstream anchors
     * the operation's LEFT/RIGHT feature — the operand's own span). The
     * drr bank witnesses the {@code +} pair (2 lines).
     */
    private void checkOperandsSingle(RArithmeticExpr arith, ValidationCollector collector) {
        String suggestion = "The `" + arithOperatorName(arith.op())
                + "` operator requires a single cardinality input";
        arith.left().ifPresent(l -> singleCardinalityWarning(l, suggestion, collector));
        arith.right().ifPresent(r -> singleCardinalityWarning(r, suggestion, collector));
    }

    private static String arithOperatorName(ArithOp op) {
        return switch (op) {
            case PLUS -> "+";
            case MINUS -> "-";
            case MULTIPLY -> "*";
            case DIVIDE -> "/";
        };
    }

    /**
     * Upstream {@code checkConditionalExpression}'s
     * {@code isSingleCheck(expr.getIf(), ...)} — released severity WARNING
     * with the released suggestion bytes (constant-pool-verified); the
     * fork's pre-#455 ERROR form was the #454 OBS condition-single
     * severity divergence (PR #455).
     */
    private void checkConditionIsSingle(RConditionalExpr cond, ValidationCollector collector) {
        if (cond.condition() != null) {
            singleCardinalityWarning(cond.condition(),
                "The condition of an if-then-else expression should be single cardinality",
                collector);
        }
    }

    /**
     * facet listEqualsCardinalityModifier (PR #437 — the recorded validation item,
     * false-NEGATIVE half 1): upstream {@code ExpressionValidator.checkEqualityOperation}
     * — an equality with NO {@code all}/{@code any} modifier whose operands DIFFER in
     * cardinality errors {@code Operator `=` should specify `all` or `any` when
     * comparing a list to a single value} (message byte-identical; upstream emits it
     * for {@code =} and {@code <>} alike via {@code op.getOperator()}). A MODIFIED
     * equality takes upstream's separate modified-operation checks — not this error.
     * Gated on BOTH operands' inferred types being non-MISSING: a fork-unresolved
     * side declines (upstream never validates a shape it cannot resolve; a false
     * NEGATIVE there is the pre-#437 behavior, never a false error on a fork-blind
     * shape). Corpus-safe by upstream's own gate: this is an upstream ERROR, so no
     * corpus model can carry it (their builds would have failed).
     */
    private void checkEqualityCardinality(REqualityExpr eq, ValidationCollector collector) {
        if (eq.mod().isPresent()) {
            return;
        }
        RExpression left = eq.left().orElse(null);
        RExpression right = eq.right().orElse(null);
        if (left == null || right == null) {
            return;
        }
        if (typeEngine.getInferredType(left).isMissing()
                || typeEngine.getInferredType(right).isMissing()) {
            return;
        }
        // The FAITHFUL cardinality entry (computeRuleBody = the thenAware path — the
        // fork's mirror of upstream CardinalityProvider): the global compute() is
        // deliberately SINGLE-biased for generator seats (disguised REnumValueRef
        // navs, receiver-blind feature calls — the #291/#397 conservative arms), and
        // a validator consuming those reads would MISS a disguised `t2->nums` multi
        // side entirely. Validator-only consumption — zero generator seats move.
        boolean leftMulti = cardinalityComputer.computeRuleBody(left) == ExpressionCardinality.MULTI;
        boolean rightMulti = cardinalityComputer.computeRuleBody(right) == ExpressionCardinality.MULTI;
        if (leftMulti != rightMulti) {
            collector.error(
                eq.sourceRange(),
                "Operator `" + (eq.op() == com.regnosys.rosetta.ast.enums.EqOp.EQ ? "=" : "<>")
                        + "` should specify `all` or `any` when comparing a list to a single value",
                ValidationIssueCode.CARDINALITY_ERROR);
        }
    }

    /**
     * facet onlyElementSingleReceiver (PR #437, widened PR #455): upstream
     * {@code RosettaSimpleValidator.checkUnaryOperation} — a {@code ListOperation}
     * over a RESOLVED single-cardinality receiver warns
     * {@code List <operator> operation cannot be used for single cardinality
     * expressions.} (message byte-identical; the released bytecode interpolates
     * {@code getOperator()}). PR #437 scoped this to ONLY-ELEMENT (the pin's
     * witness); PR #455 widens it to the full released {@code ListOperation}
     * set — distinct/flatten/reverse/first/last/sum (RListOpExpr) +
     * sort/min/max/reduce (their own AST classes) — the released class
     * hierarchy verified by javap ({@code FilterOperation}/{@code MapOperation}/
     * {@code ThenOperation} implement only {@code CanHandleListOfLists}, NOT
     * {@code ListOperation} — excluded). The drr bank witnesses the {@code last}
     * pair (2 lines). The resolved gate = the receiver's inferred
     * type non-MISSING (upstream {@code isResolved}'s intent: never warn on a
     * broken reference). A WARNING never blocks generation — corpus carriers are
     * legitimate upstream behavior (their builds warn and pass).
     */
    private void checkListOperationSingleReceiver(RExpression receiver, String opName,
            ValidationCollector collector) {
        if (receiver == null) {
            return;
        }
        if (typeEngine.getInferredType(receiver).isMissing()) {
            return;
        }
        // PR #454: the resolution-completeness decline joined this #437 arm —
        // a choice-option-only disguised receiver (`payout -> PerformancePayout
        // only-element`, the cdm event-common carriers) reads a blind SINGLE
        // where upstream's resolved walk reads the (1..*) head MULTI and stays
        // silent (the V0 banks carry ZERO only-element warnings).
        if (!cardinalityReadable(receiver)) {
            return;
        }
        // PR #454: the VALIDATOR-LOCAL implicit-receiver supplement — a
        // single-declared receiver directly inside a `then` body over a MULTI
        // pipe is a list upstream (the receiver OR reaches the then-item:
        // `filter […] then cftcTransactionInformation -> internalTradeIdentifier
        // only-exists`-shaped drr dtcc/valuation carriers, V0-bank-silent), so
        // the warning declines. Local deliberately: the same OR at the core
        // Cat-10 read moved 6 BasketConstituent POJO goldens at the live cp
        // probe (the rule output back-fill consumes it — see the
        // disguisedChainCardinality NB); zero generator seats read THIS gate.
        if (thenItemContextMulti(receiver)) {
            return;
        }
        // The faithful cardinality entry — see checkEqualityCardinality's note.
        if (cardinalityComputer.computeRuleBody(receiver) != ExpressionCardinality.MULTI) {
            // Anchored at the OPERATOR keyword token (upstream's operator
            // feature — the drr bank's `last` pair sites at the keyword
            // column, per the mojo-seam stream measure; PR #458 anchor wave).
            RNode op = receiver.parent();
            SourceRange anchor = op != null
                    ? op.tokenRanges().getOrDefault("keyword", op.sourceRange())
                    : receiver.sourceRange();
            collector.warning(
                anchor,
                "List " + opName + " operation cannot be used for single cardinality expressions.",
                ValidationIssueCode.CARDINALITY_ERROR);
        }
    }

    /**
     * v3.1 CLOSE-OUT (the parser's standing conformance red, list (a)): upstream
     * {@code RosettaSimpleValidator.checkFlattenOperation} (vendored {@code :1088-1094})
     * — {@code flatten} over an argument that is NOT a list of lists is the ERROR
     * {@code List flatten only allowed for list of lists.} anchored at the operator
     * keyword ({@code ROSETTA_OPERATION__OPERATOR}); the fork's list-of-lists reading
     * is {@link CardinalityComputer#outputIsListOfLists(RExpression)}, the PR #457
     * mirror of upstream's {@code isOutputListOfLists}. A MISSING-typed argument
     * declines (the #437 law — upstream never validates a shape it cannot resolve).
     * b2-enum-chain-in-nested-extract:64 (`then flatten` over an extract whose body is
     * single) is the witness.
     */
    private void checkFlattenArgument(RListOpExpr flatten, ValidationCollector collector) {
        RExpression arg = flatten.argument();
        if (arg == null || typeEngine.getInferredType(arg).isMissing()) {
            return;
        }
        // The #454 resolution-decline law at THIS seat: the list-of-lists reading
        // walks the whole pipe the flatten sits in (a `then` item is the previous
        // step's output, an extract's item its argument's element), so a disguised
        // choice-option hop ANYWHERE in that pipe (`underliers -> Observable ->
        // Basket then extract GetBasket then flatten`, `payout -> OptionPayout ->
        // … -> adjustableDates extract AdjustableDatesResolution then flatten` —
        // three drr 7.x carriers, V0-bank-silent) reads a blind SINGLE where
        // upstream, resolving the head, reads MULTI. The pipe root is the
        // outermost then-chain / list-op / lambda the flatten hangs from; its
        // subtree must be cardinality-readable or the check declines.
        if (!cardinalityReadable(pipeRootOf(flatten))) {
            return;
        }
        if (!cardinalityComputer.outputIsListOfLists(arg)) {
            SourceRange anchor = flatten.tokenRanges().getOrDefault("keyword", flatten.sourceRange());
            collector.error(
                anchor,
                "List flatten only allowed for list of lists.",
                ValidationIssueCode.CARDINALITY_ERROR);
        }
    }

    /**
     * Whether upstream's type provider would type this arithmetic operation as
     * NOTHING (the bottom type — assignable to every output, so upstream's
     * assignment check is silent): {@code +} and {@code -} over a left operand
     * that is not one of the operator's admissible types, or over a right operand
     * of the wrong family for that left ({@code string + number}, {@code number -
     * string}); {@code *} over any non-number operand; {@code /} never (upstream
     * types a division as number unconditionally). A {@code nothing}-typed operand
     * on either side is NOTHING upstream too (the lambdas test {@code instanceof
     * RNumberType} / {@code RStringType}, which {@code nothing} is not). Either
     * operand MISSING declines (the #437 law). Mirrors {@code caseAddOperation} /
     * {@code caseSubtractOperation} / {@code caseMultiplyOperation} /
     * {@code caseDivideOperation} as far as the NOTHING verdict goes; the non-NOTHING
     * result stays the fork's inferred type.
     */
    private boolean arithmeticTypesAsNothingUpstream(RArithmeticExpr arith) {
        RExpression left = arith.left().orElse(null);
        RExpression right = arith.right().orElse(null);
        if (left == null || right == null) {
            return false;
        }
        RMetaAnnotatedType l = typeEngine.getInferredType(left);
        RMetaAnnotatedType r = typeEngine.getInferredType(right);
        if (l.isMissing() || r.isMissing()) {
            return true;
        }
        boolean lNumber = subtypeRelation.isSubtypeOf(l.type(), NUMBER_NO_META.type());
        boolean rNumber = subtypeRelation.isSubtypeOf(r.type(), NUMBER_NO_META.type());
        boolean lString = subtypeRelation.isSubtypeOf(l.type(), STRING_NO_META.type());
        boolean rString = subtypeRelation.isSubtypeOf(r.type(), STRING_NO_META.type());
        boolean lDate = subtypeRelation.isSubtypeOf(l.type(), RRecordType.DATE);
        boolean lNothing = subtypeRelation.isSubtypeOf(l.type(), RBasicType.NOTHING);
        boolean rNothing = subtypeRelation.isSubtypeOf(r.type(), RBasicType.NOTHING);
        return switch (arith.op()) {
            case PLUS -> lNothing || rNothing
                    || !(lDate || (lString && rString) || (lNumber && rNumber));
            case MINUS -> lNothing || rNothing || !(lDate || (lNumber && rNumber));
            case MULTIPLY -> lNothing || rNothing || !(lNumber && rNumber);
            case DIVIDE -> false;
        };
    }

    /**
     * The outermost expression of the pipe {@code expr} sits in: walks up through
     * {@code then} steps, the lambdas that carry them and the list operations that
     * consume them, stopping at the first parent of any other kind (an operation
     * root, a conditional arm, a call argument). The subtree below that root is
     * everything the list-of-lists reading can consult.
     */
    private static RExpression pipeRootOf(RExpression expr) {
        RExpression root = expr;
        RNode p = expr.parent();
        while (p instanceof RThenExpr || p instanceof RInlineFunction
                || p instanceof RListOpExpr || p instanceof RExtractExpr
                || p instanceof RFilterExpr || p instanceof RSortExpr
                || p instanceof RMinExpr || p instanceof RMaxExpr
                || p instanceof RReduceExpr) {
            if (p instanceof RExpression e) {
                root = e;
            }
            p = p.parent();
        }
        return root;
    }

    /**
     * v3.1 CLOSE-OUT (the parser's standing conformance red, list (a)): upstream
     * {@code checkSymbolReference}'s enumeration arm (vendored
     * {@code ExpressionValidator.java:319-326}) — a symbol reference that RESOLVES to an
     * enumeration and is not the receiver of a feature call names a TYPE where a value
     * is expected: ERROR {@code Enum type `E` must be followed by ` -> <enum value>`.
     * Possible values are: V1, V2}, the values in declaration order. The fork parses
     * {@code E -> V} as one {@link com.regnosys.rosetta.ast.expressions.references.REnumValueRef}
     * node, so the receiver exemption is reached only by a genuine feature call over a
     * bare symbol reference; the corpus carries no bare enumeration in value position
     * (the V0 streams are error-free). c1-precedence:53 (`extract Colour`, the global
     * enum outranking the implicit item's same-named feature — the precedence the
     * oracle pins) is the witness.
     */
    private void checkBareEnumTypeReference(RSymbolReference ref, ValidationCollector collector) {
        RNode symbol = ref.symbol().orElse(null);
        if (!(symbol instanceof com.regnosys.rosetta.ast.types.REnumeration en)) {
            return;
        }
        if (ref.parent() instanceof RFeatureCall) {
            return;
        }
        String values = en.values().stream()
                .map(com.regnosys.rosetta.ast.supporting.REnumValue::name)
                .collect(Collectors.joining(", "));
        collector.error(
            ref.sourceRange(),
            "Enum type `" + en.name() + "` must be followed by ` -> <enum value>`. Possible values are: "
                    + values,
            ValidationIssueCode.TYPE_ERROR);
    }

    /** Upstream {@code getOperator()} spellings for the RListOpExpr kinds. */
    private static String listOpOperatorName(com.regnosys.rosetta.ast.enums.ListOp op) {
        return switch (op) {
            case ONLY_ELEMENT -> "only-element";
            case FLATTEN -> "flatten";
            case DISTINCT -> "distinct";
            case REVERSE -> "reverse";
            case FIRST -> "first";
            case LAST -> "last";
            case SUM -> "sum";
        };
    }

    // === PR #455: the warning-family waves (facet warningFamilyWaves) ========

    /**
     * Upstream {@code ExpressionValidator.checkPathOperatorOnChoice} (vendored
     * {@code :45-53}, released-jar-verified {@code warning}): a plain {@code ->}
     * feature call whose RECEIVER types as a CHOICE fires the deprecation
     * WARNING {@code Using the path operator on a choice type is deprecated.
     * Use the switch operator instead}, anchored at the receiver (upstream
     * {@code ROSETTA_FEATURE_CALL__RECEIVER} — a chain over nested choices
     * fires once per step, the outer anchor sharing the inner's start
     * position, which the cdm bank's duplicate-position rows witness). The
     * released {@code RosettaDeepFeatureCall} ({@code ->>}) is NOT a
     * {@code RosettaFeatureCall} subtype (javap-verified) — deep feature
     * calls never fire. The cdm bank carries 784 lines of this class.
     */
    private void checkPathOperatorOnChoice(RFeatureCall fc, ValidationCollector collector) {
        RExpression receiver = fc.receiver();
        if (receiver == null) {
            return;
        }
        RMetaAnnotatedType receiverType = typeEngine.getInferredType(receiver);
        if (receiverType.type() instanceof RChoiceTypeRef) {
            pathOnChoiceWarning(receiver.sourceRange(), collector);
        }
    }

    /**
     * The disguised-nav half of {@link #checkPathOperatorOnChoice}: the fork
     * parses an UPPERCASE-second-segment nav ({@code asset -> Instrument}) as
     * one {@code REnumValueRef} where upstream parses a
     * {@code RosettaFeatureCall} whose receiver is the head — so an EVR whose
     * resolved binding is a NAV (never a true enum constant) carries exactly
     * one upstream feature-call step, and the deprecation fires when that
     * step's receiver types as a CHOICE. Per resolved channel: a choice-option
     * bind consults the bind-time head bit ({@code
     * choiceOptionHeadIsDirectChoice} — a {@code type X extends <choice>}
     * view-admitted head stays silent, exactly upstream's non-choice
     * receiver); a type-restriction bind fires on a choice-typed LHS
     * attribute; a Cat-10 attribute chain fires on a choice-typed head
     * attribute (the common-attribute admission on choices); an
     * input-feature bind fires on a choice-typed input parameter. Anchored at
     * the EVR (its start position IS the head's — upstream's receiver
     * anchor).
     */
    private void checkPathOperatorOnChoiceEvr(
            com.regnosys.rosetta.ast.expressions.references.REnumValueRef evr,
            ValidationCollector collector) {
        if (evr.resolvedChoiceOption().isPresent()) {
            if (evr.choiceOptionHeadIsDirectChoice()) {
                pathOnChoiceWarning(evr.sourceRange(), collector);
            }
            return;
        }
        if (evr.resolvedTypeRestriction().isPresent()) {
            RAttribute lhs = evr.resolvedTypeRestriction().get().lhsAttribute();
            if (lhs != null && typeResolver.inferAttributeRefType(lhs).type()
                    instanceof RChoiceTypeRef) {
                pathOnChoiceWarning(evr.sourceRange(), collector);
            }
            return;
        }
        if (evr.resolvedAttributeChain().isPresent()) {
            RAttribute head = evr.resolvedAttributeChain().get().attribute();
            if (head != null && typeResolver.inferAttributeRefType(head).type()
                    instanceof RChoiceTypeRef) {
                pathOnChoiceWarning(evr.sourceRange(), collector);
            }
            return;
        }
        if (evr.resolvedInputFeature().isPresent() && evr.enumName() != null) {
            RFunction fn = AstWalker.findAncestor(evr, RFunction.class).orElse(null);
            if (fn != null) {
                for (RAttribute input : fn.inputs()) {
                    if (evr.enumName().equals(input.name())) {
                        if (typeResolver.inferAttributeRefType(input).type()
                                instanceof RChoiceTypeRef) {
                            pathOnChoiceWarning(evr.sourceRange(), collector);
                        }
                        return;
                    }
                }
            }
            // No enclosing function input carries the head — a data-type
            // condition's ITEM-feature bind lands here too; fall through to
            // the condition-context read.
        }
        // The CONDITION-CONTEXT fallback: a data-type condition's disguised
        // nav rides the #443 TYPING-ONLY condition channel (cleared, no bind,
        // no inferred type), so none of the resolved classes above carry it —
        // but upstream types the head as the enclosing type's attribute and
        // fires on a choice (`payout -> InterestRatePayout -> ...` in the
        // product-template FpML conditions, the cdm bank's dominant
        // type-file block). The head lookup mirrors the engine's own T10
        // item read: the enclosing data type's attribute (supertype-walked)
        // named by the head. A true enum constant (resolvedEnum present)
        // never reaches here; upstream fires on the RECEIVER type alone, so
        // the option side needs no membership check.
        if (evr.enumeration().isEmpty() && evr.resolvedSymbol().isEmpty()
                && evr.enumName() != null
                && AstWalker.findAncestor(evr, RCondition.class).isPresent()) {
            var enclosingType = AstWalker.findAncestor(evr,
                    com.regnosys.rosetta.ast.types.RDataType.class).orElse(null);
            if (enclosingType != null) {
                var attr = typeResolver.findAttributeOnType(
                        new RDataTypeRef(enclosingType), evr.enumName());
                if (attr.isPresent() && typeResolver.inferAttributeRefType(attr.get()).type()
                        instanceof RChoiceTypeRef) {
                    pathOnChoiceWarning(evr.sourceRange(), collector);
                }
            }
        }
    }

    private void pathOnChoiceWarning(com.regnosys.rosetta.ast.SourceRange range,
            ValidationCollector collector) {
        collector.warning(
            range,
            "Using the path operator on a choice type is deprecated. Use the switch operator instead",
            ValidationIssueCode.DEPRECATION);
    }

    /**
     * Upstream {@code ConstructorValidator.checkConstructorExpression}'s
     * per-pair cardinality arm (vendored {@code :56-58}): a key-value pair
     * whose RESOLVED member attribute is single-cardinality takes
     * {@code isSingleCheck(value, ..., "Cannot assign a list to a single
     * value")} — released severity WARNING with the {@code Expecting single
     * cardinality. }-prefixed message, anchored at the pair's VALUE
     * expression. The cdm bank witnesses exactly one line (the
     * ingest-fpml-confirmation-settlement {@code adjustedDate:} chain). The
     * member lookup rides {@link TypeDirectedResolver#findAttributeOnType}
     * (the Cat-13d seat's own resolution — supertype- and choice-aware); an
     * unresolved constructor type or member declines per the #437 law.
     */
    private void checkConstructorPairCardinality(
            com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr ctor,
            ValidationCollector collector) {
        RMetaAnnotatedType ctorType = typeEngine.getInferredType(ctor);
        if (ctorType.isMissing()) {
            return;
        }
        for (var pair : ctor.pairs()) {
            if (pair.key() == null || pair.value() == null) {
                continue;
            }
            var attr = typeResolver.findAttributeOnType(ctorType.type(), pair.key());
            if (attr.isEmpty() || isAttributeMulti(attr.get())) {
                continue;
            }
            singleCardinalityWarning(pair.value(), "Cannot assign a list to a single value",
                collector);
        }
    }

    /**
     * Upstream {@code ExpressionValidator.checkOperation}'s cardinality tail
     * (vendored {@code :131-141}): the assign target is the path's LAST
     * segment when a segment chain is present, else the operation root; a
     * single-cardinality target takes the {@code `add` must be used with a
     * list} ERROR on an {@code add} operation and the
     * {@code isSingleCheck(expr, ..., "Cannot assign a list to a single
     * value")} released WARNING on the assigned expression. Corpus-silent
     * (the V0 banks carry ZERO lines at this seat — the constructor-pair
     * sibling carries the one bank witness); an alias-rooted or unresolved
     * target declines (upstream reads alias cardinality through
     * {@code isSymbolMulti} — a corpus-empty corner the fork declines
     * rather than models).
     */
    private void checkOperationAssignment(com.regnosys.rosetta.ast.functions.ROperation op,
            ValidationCollector collector) {
        if (op.expression() == null) {
            return;
        }
        RAttribute target = null;
        if (op.segment().isPresent()) {
            var seg = op.segment().get();
            while (seg.next().isPresent()) {
                seg = seg.next().get();
            }
            target = seg.resolvedAttribute().orElse(null);
        } else {
            RFunction fn = AstWalker.findAncestor(op, RFunction.class).orElse(null);
            if (fn != null && fn.output().isPresent()
                    && fn.output().get().name() != null
                    && fn.output().get().name().equals(op.targetName())) {
                target = fn.output().get();
            }
        }
        if (target == null) {
            return;
        }
        // v3.1 CLOSE-OUT (the parser's standing conformance red, list (a)):
        // upstream checkOperation's TYPE half (vendored :133) — the assigned
        // expression must be a subtype of the target's declared type, ERROR
        // `Expected type `E`, but got `A` instead. Cannot assign `A` to output
        // `<target>`` at the expression (OPERATION__EXPRESSION), the target
        // being the path's LAST segment or the assign root exactly as the
        // cardinality tail below reads it. Declines on a MISSING side (the #437
        // law) inside argSubtypeCheck. c1-precedence:52 (`extract Colour`
        // binding the GLOBAL enum over the item feature, so the value types as
        // the enum against a `string` output) and :78 (`extract Salt` binding
        // the item's string feature against a `Flavour` output) are the
        // witnesses; the twenty-cell diagnostic gate measures the corpus at
        // ZERO new rows (upstream's V0 streams are error-free there).
        //
        // The ONE decline beyond argSubtypeCheck's own: an assigned value typed
        // `any`. The fork's join of two arms with no common supertype is ANY
        // (checkConditionalBranchTypes reports that failure at the conditional),
        // where upstream's RosettaTypeProvider.caseConditionalExpression
        // (vendored :315-323) collapses an ANY join to NOTHING — the bottom type,
        // a subtype of every output — so upstream's assignment check stays silent
        // at such a seat (b3-super-enum-chain:65 against its :67 join error).
        // Declining on ANY here is the validator-local mirror of that collapse;
        // re-typing the fork's conditional to NOTHING would re-price generator
        // reads of the join and is the recorded typed follow-on. The SAME
        // collapse for an arithmetic value: upstream's case{Add,Subtract,
        // Multiply}Operation (vendored :265-292, :479-490, :550-569) type an
        // operation over inadmissible operands as NOTHING, where the fork's
        // left-driven read keeps the left operand's type — so `flag - flag2`
        // types `boolean` here and NOTHING there; the operand errors fire on
        // both sides, the assignment error only on ours. Declined by
        // arithmeticTypesAsNothingUpstream (SmokeTypeInferenceTest's arith8
        // "EXACTLY TWO diagnostics" pin is the witness).
        final RAttribute out = target;
        RMetaAnnotatedType assigned = typeEngine.getInferredType(op.expression());
        if (assigned.isMissing() || isAnyType(assigned.type())
                || (op.expression() instanceof RArithmeticExpr arith
                        && arithmeticTypesAsNothingUpstream(arith))) {
            return;
        }
        argSubtypeCheck(typeEngine.getInferredAttributeType(out), op.expression(),
            actual -> "Cannot assign `" + actual + "` to output `" + out.name() + "`",
            collector);
        if (target.cardinality().isEmpty()) {
            return;
        }
        if (isAttributeMulti(target)) {
            return;
        }
        if (op.operator() == com.regnosys.rosetta.ast.enums.OperationOp.ADD) {
            collector.error(
                op.sourceRange(),
                "`add` must be used with a list",
                ValidationIssueCode.CARDINALITY_ERROR);
        }
        singleCardinalityWarning(op.expression(), "Cannot assign a list to a single value",
            collector);
    }

    // === PR #454: the Annex-A family arms (facet annexAReleasedSeverities) ===

    /**
     * Upstream {@code checkEqualityOperation}'s {@code comparableTypeCheck}
     * (vendored {@code ExpressionValidator.java:216} +
     * {@code AbstractExpressionValidator.java:99-107}): equality operands must
     * be comparable — {@code isComparable(l, r) = isSubtypeOf(l, r) ||
     * isSubtypeOf(r, l)} ({@code TypeSystem.java:188-193}) — else the ERROR
     * {@code Types `L` and `R` are not comparable} (released-jar-verified
     * bytes, anchored at the whole operation upstream; the fork anchors at the
     * operation's own range). Both-side MISSING gate per the #437 law.
     */
    private void checkEqualityComparability(REqualityExpr eq, ValidationCollector collector) {
        comparableTypeCheck(eq, eq.left().orElse(null), eq.right().orElse(null), collector);
    }

    /**
     * Upstream {@code checkModifiedBinaryOperation} (vendored
     * {@code ExpressionValidator.java:189-212}), shared by the modified
     * ({@code all}/{@code any}) equality AND comparison forms: the left
     * operand must be MULTI and the right SINGLE, both emitted as WARNINGS in
     * the released 9.83.0 (the facet note's severity oracle; the V0 banks
     * carry the drr witnesses — the fca-ukemir {@code any =} left-single
     * "remove the modifier" line + bare right-multi lines). Suggestions
     * mirror upstream exactly: left failing suggests FLIP when the right is
     * multi (operands swapped) else REMOVE the modifier; right failing
     * suggests FLIP when the left is single, else carries NO suggestion
     * (both-multi — the bare "Expecting single cardinality" form).
     */
    private void checkModifiedBinaryOperation(RBinaryExpression op, CardMod mod,
            String operator, ValidationCollector collector) {
        RExpression left = op.left().orElse(null);
        RExpression right = op.right().orElse(null);
        if (left == null || right == null) {
            return;
        }
        String removeModifierSuggestion = "Did you mean to remove the `"
                + (mod == CardMod.ALL ? "all" : "any") + "` modifier on the `" + operator
                + "` operator?";
        String flipOperandsSuggestion = "Did you mean to flip around the operands of the `"
                + operator + "` operator?";
        // The resolution-completeness decline (see cardinalityReadable) gates
        // BOTH emissions AND the suggestion derivation: an unreadable side's
        // blind-SINGLE read would otherwise both mis-fire its own warning and
        // mis-pick the sibling's suggestion.
        if (!cardinalityReadable(left) || !cardinalityReadable(right)) {
            return;
        }
        boolean leftMulti = cardinalityComputer.computeRuleBody(left) == ExpressionCardinality.MULTI;
        boolean rightMulti = cardinalityComputer.computeRuleBody(right) == ExpressionCardinality.MULTI;
        String leftSuggestion = rightMulti ? flipOperandsSuggestion : removeModifierSuggestion;
        String rightSuggestion = leftMulti ? null : flipOperandsSuggestion;
        if (!leftMulti) {
            cardinalityWarning(left, "Expecting multi cardinality", leftSuggestion, collector);
        }
        if (rightMulti) {
            cardinalityWarning(right, "Expecting single cardinality", rightSuggestion, collector);
        }
    }

    /**
     * Upstream {@code checkLogicalOperation}'s cardinality half (vendored
     * {@code ExpressionValidator.java:234-235}): both operands single — the
     * released 9.83.0 emits WARNINGS ({@code Expecting single cardinality. The
     * `or` operator requires a single cardinality input}); the operand TYPE
     * check stays at the fork-native seat above ({@link #checkLogicalOperands}
     * — out of the Annex-A block). Zero corpus carriers in either V0 bank.
     */
    private void checkLogicalCardinality(RLogicalExpr log, ValidationCollector collector) {
        String operator = log.op() == LogOp.AND ? "and" : "or";
        String suggestion = "The `" + operator + "` operator requires a single cardinality input";
        log.left().ifPresent(l -> singleCardinalityWarning(l, suggestion, collector));
        log.right().ifPresent(r -> singleCardinalityWarning(r, suggestion, collector));
    }

    /**
     * Upstream {@code checkListLiteral} (vendored
     * {@code ExpressionValidator.java:291-293}): all elements must share a
     * common supertype — {@code commonTypeCheck} ERROR (the pin's
     * {@code [1, True]} witness).
     */
    private void checkListLiteralElements(RListLiteral list, ValidationCollector collector) {
        commonTypeCheck(list.elements(), collector);
    }

    /**
     * Upstream {@code checkConditionalExpression}'s branch half (vendored
     * {@code ExpressionValidator.java:287}): the then/else branches must share
     * a common supertype — {@code commonTypeCheck} ERROR anchored at the ELSE
     * branch upstream (the fork anchors at the failing element, which IS the
     * else branch in the two-element form). The condition-cardinality check
     * stays at the fork-native T14 seat ({@link #checkConditionIsSingle});
     * the condition-boolean check is a recorded out-of-block gap.
     */
    private void checkConditionalBranchTypes(RConditionalExpr cond, ValidationCollector collector) {
        RExpression thenBranch = cond.thenBranch();
        RExpression elseBranch = cond.elseBranch().orElse(null);
        if (thenBranch == null || elseBranch == null) {
            return;
        }
        commonTypeCheck(List.of(thenBranch, elseBranch), collector);
    }

    /**
     * Upstream {@code checkSymbolReference} → {@code checkCallableReference}
     * (vendored {@code ExpressionValidator.java:301-384}): a reference whose
     * resolved symbol is callable-with-args checks (1) ARITY — {@code Expected
     * N argument(s), but got M instead}, ERROR, released-jar-verified recipe
     * including the singular/plural split; (2) per-argument CARDINALITY — a
     * single-cardinality parameter's argument must be single, the released
     * 9.83.0 WARNING with NO suggestion (the bare {@code Expecting single
     * cardinality} form — the V0 banks' 13-line class; library-function
     * parameters are always single upstream, function inputs consult the
     * declared cardinality); (3) per-argument ASSIGNABILITY — {@code Cannot
     * assign `X` to input `b`} / {@code …to parameter `p`} (library) /
     * {@code Rule `R` cannot be called with type `X`}, ERROR, each argument's
     * inferred type against the declared parameter type, MISSING declining
     * per-argument (the #437 law).
     *
     * <p>Fork admission gate: the resolved symbol must BE a callable
     * declaration ({@link RFunction} — a dispatch VARIANT reads its inputs
     * through {@link RFunction#dispatchBase()} exactly like the #444
     * base-aware scope — {@link RLibraryFunction}, or {@link RRule}); every
     * other symbol kind (attributes, aliases, params, enums) falls outside
     * upstream's {@code RosettaCallableWithArgs} instanceof and is untouched.
     * A bare (zero-arg) reference to a callable fires the arity error exactly
     * like upstream's implicit zero-argument form.
     */
    private void checkCallableReference(RSymbolReference ref, ValidationCollector collector) {
        RNode symbol = ref.symbol().orElse(null);
        if (symbol == null) {
            return;
        }
        List<RExpression> args = ref.args();
        if (symbol instanceof RFunction fn) {
            List<RAttribute> declared = fn.inputs().stream()
                    .filter(a -> !com.regnosys.rosetta.symbols.derived.GeneratedInputRule
                            .isSynthesized(a))
                    .toList();
            List<RAttribute> params = declared.isEmpty()
                    ? fn.dispatchBase().map(RFunction::inputs).orElse(declared).stream()
                            .filter(a -> !com.regnosys.rosetta.symbols.derived.GeneratedInputRule
                                    .isSynthesized(a))
                            .toList()
                    : declared;
            if (!implicitArgumentSatisfies(ref, args, params.size())) {
                checkArgCount(ref, params.size(), args.size(), collector);
            }
            int minCount = Math.min(params.size(), args.size());
            for (int i = 0; i < minCount; i++) {
                RAttribute param = params.get(i);
                RExpression arg = args.get(i);
                if (!isAttributeMulti(param)) {
                    singleCardinalityWarning(arg, null, collector);
                }
                RMetaAnnotatedType paramType = typeEngine.getInferredAttributeType(param);
                argSubtypeCheck(paramType, arg,
                    actual -> "Cannot assign `" + actual + "` to input `" + param.name() + "`",
                    collector);
            }
        } else if (symbol instanceof RLibraryFunction lib) {
            List<RParameter> params = lib.parameters();
            if (!implicitArgumentSatisfies(ref, args, params.size())) {
                checkArgCount(ref, params.size(), args.size(), collector);
            }
            int minCount = Math.min(params.size(), args.size());
            for (int i = 0; i < minCount; i++) {
                RParameter param = params.get(i);
                RExpression arg = args.get(i);
                singleCardinalityWarning(arg, null, collector);
                RMetaAnnotatedType paramType = typeEngine.getInferredParameterType(param);
                argSubtypeCheck(paramType, arg,
                    actual -> "Cannot assign `" + actual + "` to parameter `" + param.name() + "`",
                    collector);
            }
        } else if (symbol instanceof RRule rule) {
            // Upstream: a rule reference checks its (single) input against the
            // rule's declared input type; arity is NOT checked (numberOfParameters
            // is 1 and the grammar admits exactly one — upstream's minCount guard).
            if (!args.isEmpty()) {
                RExpression arg = args.get(0);
                singleCardinalityWarning(arg, null, collector);
                RMetaAnnotatedType ruleInput = typeEngine.getInferredRuleFromType(rule);
                argSubtypeCheck(ruleInput, arg,
                    actual -> "Rule `" + rule.name() + "` cannot be called with type `"
                            + actual + "`",
                    collector);
            }
        }
    }

    /**
     * Upstream's LAZY IMPLICIT ARGUMENT (released
     * {@code RosettaSymbolReferenceImpl.getArgs()} bytecode, decoded by the
     * #454 probe): a reference with NO explicit arguments whose symbol is a
     * ONE-parameter callable, sitting where an implicit variable is in
     * context, receives the implicit item as its single argument — so
     * {@code then DifferentOrdinalsCondition} / {@code filter
     * IsAllowableAction} pipe stages arity-check as 1-of-1 upstream (the
     * probe's 3,181-carrier corpus class). The fork's AST keeps the args
     * list empty (the generator supplies the implicit at render), so the
     * arity seat mirrors the admission instead. The implicit argument's own
     * cardinality/type checks (upstream runs them against the generated
     * implicit variable) are a recorded follow-on; the fork's AST also does
     * not distinguish {@code Func()} (explicit empty parens) from a bare
     * {@code Func} — upstream admits only the latter; the conflation cannot
     * over-admit on a corpus upstream builds clean (a recorded AST-fidelity
     * narrow).
     */
    private boolean implicitArgumentSatisfies(RSymbolReference ref, List<RExpression> args,
            int paramCount) {
        return args.isEmpty() && paramCount == 1 && implicitVariableExistsInContext(ref);
    }

    /**
     * Upstream {@code implicitVariableExistsInContext}: the structural definer
     * walk (lambda / data-type condition / rule) UNIONED with the engine's
     * implicit-item walk, which additionally covers the #451 narrowing
     * SWITCH-CASE contexts (upstream's walk stops at a
     * {@code SwitchCaseOrDefault} that defines the item — the census's
     * 79-carrier {@code fpml.X then MapX...} residual class fires there).
     */
    private boolean implicitVariableExistsInContext(RNode node) {
        if (implicitContextDefiner(node) != null) {
            return true;
        }
        return !typeEngine.getImplicitItemType(node).isMissing();
    }

    private void checkArgCount(RSymbolReference ref, int paramCount, int argCount,
            ValidationCollector collector) {
        if (paramCount != argCount) {
            collector.error(
                ref.sourceRange(),
                "Expected " + paramCount + " argument" + (paramCount == 1 ? "" : "s")
                        + ", but got " + argCount + " instead",
                ValidationIssueCode.TYPE_ERROR);
        }
    }

    /** Upstream {@code isSymbolMulti} on a declared attribute (sup > 1 or unbounded). */
    private static boolean isAttributeMulti(RAttribute attr) {
        RCardinality card = attr.cardinality().orElse(null);
        if (card == null) {
            return false;
        }
        return card.isUnbounded()
                || (card.sup() != null && card.sup().compareTo(BigInteger.ONE) > 0);
    }

    /**
     * v3.2 seat 5 (PR #626): upstream {@code ExpressionValidator.checkWithMetaOperation} →
     * {@code AbstractExpressionValidator.isSingleCheckError} — the ERROR variant: {@code javap -c -p} on the released
     * jar at {@code ~/.m2/repository/org/finos/rune/rune-lang/9.83.0/rune-lang-9.83.0.jar} (the classes live under
     * {@code com.regnosys.rosetta.validation.expression}; the transcript is
     * {@code target/v32-seat5-instruments/scratch/javap-isSingleCheckError-9.83.0.txt}, local — round 2, the
     * code-quality review's MF-3 / NIT-4) shows {@code checkWithMetaOperation} loading the message constant and
     * invoking {@code isSingleCheckError}, whose body calls {@code error(...)}, where {@code isSingleCheck} calls
     * {@code warning(...)}; and the released plugin's own run on the shape — a throwaway probe group, its log
     * {@code scratch/oracle-s5c-withmeta-multi-refusal-probe-run1.log} — prints {@code [ERROR] ERROR:Expecting single
     * cardinality. The with-meta operator can only be used with single cardinality arguments (... line : 15 column :
     * 13)} and fails the build ("a severe validation error"): the message and the severity are prints of that run;
     * the ARGUMENT anchor is the bytecode's - {@code checkWithMetaOperation} passes
     * {@code ROSETTA_UNARY_OPERATION__ARGUMENT} as the feature (the transcript's line 11), where the run prints only
     * a START position the argument and its enclosing expression share (the re-verification's spec SF-3) (the #454
     * note at the head of
     * this facet: it records the same split; round 1, the spec review's MF-5, corrected the first cut's citation of the
     * warning variant) — a {@code with-meta} whose ARGUMENT is
     * multi-cardinality is an ERROR with the released message bytes, anchored at the ARGUMENT's own source
     * range — not the whole {@code with-meta} expression's, which shares its line; the lock reads the columns
     * back through the fixture text and asserts they cover {@code h -> codes} alone (round 1, cq NIT-2). The
     * seat's oracle run found the rule: the released 9.83.0 plugin REFUSED the fixture
     * {@code h -> codes with-meta { scheme: … }} ("Expecting single cardinality. The with-meta operator
     * can only be used with single cardinality arguments") where the fork would have EMITTED — the
     * rule is ported so the fork refuses where upstream refuses; the lock is
     * {@code CloseoutUnderReportValidatorTest}'s with-meta case. The cardinality read is the same
     * {@code cardinalityReadable} + {@code computeRuleBody} pair the other single-cardinality seats use.
     */
    private void checkWithMetaSingle(RWithMetaExpr wm, ValidationCollector collector) {
        RExpression argument = wm.argument();
        if (argument == null || !cardinalityReadable(argument)) {
            return;
        }
        if (cardinalityComputer.computeRuleBody(argument) == ExpressionCardinality.MULTI) {
            collector.error(argument.sourceRange(),
                "Expecting single cardinality. The with-meta operator can only be used with single cardinality arguments",
                ValidationIssueCode.CARDINALITY_ERROR);
        }
    }

    /**
     * Upstream {@code checkOnlyExistsExpression} (vendored
     * {@code ExpressionValidator.java:397-458}) on the fork's path-shaped
     * element AST ({@link ROnlyExistsElement}: a root — {@code item} or a
     * symbol name — plus a feature chain, with the parent navigation
     * synthesized as {@link ROnlyExistsElement#receiverExpression()} for
     * named-root chained elements). The five structural ERROR classes + the
     * parent-cardinality WARNING + the choice deprecation WARNING:
     *
     * <ol>
     *   <li>META-FEATURE USE (error): an element whose LEAF names a metadata
     *       face of its parent attribute ({@code x -> reference} on
     *       {@code [metadata reference]} — upstream: the arg's feature is a
     *       {@code RosettaMetaType}; the fork consults the #451
     *       {@code isMetaFaceOnAttribute} admission on the receiver's
     *       resolved leaf attribute);</li>
     *   <li>DUPLICATE ATTRIBUTE (error): two elements with identical
     *       root+chain paths (upstream {@code EcoreUtil2.equals} on the arg
     *       expressions — value equality of the written path);</li>
     *   <li>PARENT-PATH EQUALITY (error): every element's parent path
     *       (root + chain minus the leaf; ABSENT for a chainless element)
     *       must equal the first's — presence mismatch or value mismatch
     *       errors on the offending element;</li>
     *   <li>PARENT-OBJECT REQUIREMENT (error): a chainless first element
     *       needs an implicit variable in context (a lambda binding the item,
     *       a data-type condition, or a rule body — upstream
     *       {@code implicitVariableExistsInContext});</li>
     *   <li>PARENT CARDINALITY: an explicit parent must be single — the
     *       released 9.83.0 WARNING ({@code The `only exists` operator
     *       requires a single cardinality input}); an implicit-parent context
     *       that is MULTI (a {@code then} lambda over a multi pipe) is the
     *       ERROR {@code Expecting single cardinality input} exactly like
     *       upstream's {@code isImplicitVariableMulti} arm;</li>
     *   <li>UNSUPPORTED TYPE (error): the parent type must admit emptiness —
     *       every attribute optional (the full supertype walk) or a choice —
     *       else {@code Operator `only exists` is not supported for type `T`.
     *       All attributes of input type should be optional};</li>
     *   <li>CHOICE DEPRECATION (warning): a choice-typed parent warns
     *       {@code Using only exist on a choice option is deprecated}.</li>
     * </ol>
     *
     * <p>Fork declines (all corpus-empty, structurally safe): an
     * {@code item}-rooted CHAINED element synthesizes no receiver, so its
     * parent TYPE is unreadable — the type-shaped checks (5b/6/7) decline
     * while every structural check still runs; a condition inside a CHOICE
     * declaration (no {@code RDataType} ancestor) yields no implicit type —
     * same decline. MISSING parent types decline the type-shaped checks per
     * the #437 law.
     */
    private void checkOnlyExists(ROnlyExistsExpr oe, ValidationCollector collector) {
        List<ROnlyExistsElement> elements = oe.elements();
        if (elements.isEmpty()) {
            return;
        }
        // (1) meta-feature leaves
        for (ROnlyExistsElement el : elements) {
            if (el.featureChain().isEmpty()) {
                continue;
            }
            String leaf = el.featureChain().get(el.featureChain().size() - 1);
            RAttribute parentLeaf = receiverLeafAttribute(el.receiverExpression());
            if (parentLeaf != null && typeResolver.isMetaFaceOnAttribute(parentLeaf, leaf)) {
                collector.error(
                    rangeOf(el, oe),
                    "Invalid use of `only exists` on meta feature " + leaf,
                    ValidationIssueCode.TYPE_ERROR);
            }
        }
        // (2) duplicates + (3) parent-path equality — upstream's exact loop shape
        ROnlyExistsElement first = elements.get(0);
        boolean firstHasParent = !first.featureChain().isEmpty();
        List<String> firstParentPath = parentPathOf(first);
        for (int i = 1; i < elements.size(); i++) {
            ROnlyExistsElement other = elements.get(i);
            for (int j = 0; j < i; j++) {
                if (samePath(elements.get(j), other)) {
                    collector.error(rangeOf(other, oe), "Duplicate attribute",
                        ValidationIssueCode.DUPLICATE_ATTRIBUTE);
                }
            }
            boolean otherHasParent = !other.featureChain().isEmpty();
            if (firstHasParent != otherHasParent
                    || (firstHasParent && !firstParentPath.equals(parentPathOf(other)))) {
                // anchored on the offending element's PARENT expression (measured: `t` of `t -> q`,
                // `t -> r` of `t -> r -> d` — the round-2 oracle dump)
                collector.error(parentRangeOf(other, oe), "All parent paths must be equal",
                    ValidationIssueCode.TYPE_ERROR);
            }
        }
        // (4) parent-object requirement + (5) cardinality + (6)/(7) parent-type shape
        RMetaAnnotatedType parentType;
        if (firstHasParent) {
            RExpression receiver = first.receiverExpression();
            if (receiver != null) {
                // the cardinality is the RECEIVER's and the anchor is the PARENT family's ONE
                // declaration (parentRangeOf — the receiver's own range: `ts -> a`, or the bare root
                // `ts`; measured against the released validator at v3.2 seat 8 round 2, consulted
                // rather than restated at round 3 — LAW 69, and lane K2's reach)
                singleCardinalityWarning(receiver, parentRangeOf(first, oe),
                    "The `only exists` operator requires a single cardinality input", collector);
                parentType = typeEngine.getInferredType(receiver);
            } else {
                parentType = RMetaAnnotatedType.MISSING; // item-rooted chain — declined
            }
        } else {
            RNode implicitDefiner = implicitContextDefiner(oe);
            if (implicitDefiner == null && !implicitVariableExistsInContext(oe)) {
                collector.error(rangeOf(first, oe), "Object must have a parent object",
                    ValidationIssueCode.TYPE_ERROR);
                return;
            }
            if (implicitDefiner != null && implicitContextMulti(implicitDefiner)) {
                collector.error(rangeOf(first, oe), "Expecting single cardinality input",
                    ValidationIssueCode.CARDINALITY_ERROR);
            }
            parentType = implicitContextType(implicitDefiner, oe);
        }
        if (parentType.isMissing()) {
            return;
        }
        RType parentData = unwrapAlias(parentType.type());
        if (parentData == RBasicType.NOTHING) { // singleton identity — the Copilot R1 class
            return;
        }
        if (!mayBeEmpty(parentData)) {
            // the type is the RECEIVER's; the anchor is the whole element when the element
            // carries a parent path (measured: `t -> r -> d`, `u -> f` — the round-2 oracle
            // dump), the whole expression otherwise
            unsupportedTypeError(parentType, "only exists", firstHasParent
                    && first.receiverExpression() != null ? rangeOf(first, oe) : oe.sourceRange(),
                "All attributes of input type should be optional", collector);
        }
        if (parentData instanceof RChoiceTypeRef) {
            collector.warning(oe.sourceRange(),
                "Using only exist on a choice option is deprecated",
                ValidationIssueCode.TYPE_ERROR);
        }
    }

    /** The element's parent path — root + chain minus the leaf ({@code item} spelled literally). */
    private static List<String> parentPathOf(ROnlyExistsElement el) {
        List<String> path = new ArrayList<>();
        path.add(el.isRootItem() ? "item" : el.root());
        List<String> chain = el.featureChain();
        for (int i = 0; i < chain.size() - 1; i++) {
            path.add(chain.get(i));
        }
        return path;
    }

    /** Value equality of two elements' WHOLE written paths (upstream {@code EcoreUtil2.equals}). */
    private static boolean samePath(ROnlyExistsElement a, ROnlyExistsElement b) {
        return a.isRootItem() == b.isRootItem()
                && Objects.equals(a.isRootItem() ? null : a.root(), b.isRootItem() ? null : b.root())
                && a.featureChain().equals(b.featureChain());
    }

    /**
     * The anchor of a diagnostic about the ELEMENT: its whole written range ({@code root ->
     * ... -> leaf}) when carried, else the whole expression's — a fallback that is dead in
     * practice, kept as the belt: the builder ranges every element from its own context
     * before the path synthesis, the declined shapes included.
     *
     * <p>MEASURED against the released 9.83.0 validator (v3.2 seat 8 round 2 — the upstream
     * resolution oracle's {@code --issues} dump over an invalid fixture, every issue with its
     * column and length): {@code Duplicate attribute}, the unsupported-type error and
     * {@code Object must have a parent object} anchor on the whole element, one-hop and two-hop
     * alike; the single-cardinality WARNING and {@code All parent paths must be equal} anchor on
     * the element's PARENT expression ({@link #parentRangeOf}). Until round 2 no head of the fork
     * matched both families: the pre-seat read was the synthesized receiver's range for all five
     * (the whole element on a multi-hop path, the bare root on a one-hop path); the seat's per-hop
     * ranging moved that receiver to the parent path (right for the parent family, one feature
     * short for the element family on multi-hop paths — round 1's catch); round 1 read the whole
     * element for all five (right for the element family, wrong for the parent family — round
     * 2's catch, the same class inverted). {@code OnlyExistsDiagnosticAnchorTest} pins the nine
     * measured columns; lane K1 is the element family's witness.
     */
    private static com.regnosys.rosetta.ast.SourceRange rangeOf(ROnlyExistsElement el,
            ROnlyExistsExpr oe) {
        return !com.regnosys.rosetta.ast.SourceRange.NONE.equals(el.sourceRange())
                ? el.sourceRange() : oe.sourceRange();
    }

    /**
     * The anchor of a diagnostic about the element's PARENT PATH: the synthesized receiver's own
     * range ({@code root -> ... -> chain[n-2]} — since v3.2 seat 8 every hop is ranged to its own
     * feature, so this IS the parent path; the bare root for a one-feature element), else the
     * element's. The measured family: the single-cardinality warning and {@code All parent paths
     * must be equal} (see {@link #rangeOf}).
     */
    private static com.regnosys.rosetta.ast.SourceRange parentRangeOf(ROnlyExistsElement el,
            ROnlyExistsExpr oe) {
        RExpression receiver = el.receiverExpression();
        return receiver != null && !com.regnosys.rosetta.ast.SourceRange.NONE.equals(receiver.sourceRange())
                ? receiver.sourceRange() : rangeOf(el, oe);
    }

    /** The receiver's resolved LEAF attribute (a bare root's symbol / a chain's last feature). */
    private static RAttribute receiverLeafAttribute(RExpression receiver) {
        if (receiver instanceof RSymbolReference sr
                && sr.symbol().orElse(null) instanceof RAttribute attr) {
            return attr;
        }
        if (receiver instanceof RFeatureCall fc) {
            return fc.resolvedFeature().orElse(null);
        }
        return null;
    }

    /**
     * The nearest ancestor DEFINING an implicit variable (upstream
     * {@code ImplicitVariableUtil.findContainerDefiningImplicitVariable}): a
     * lambda that binds the item (implicit form or parameterless — explicit
     * parameters shadow it, exactly the engine's own filter), a data-type
     * condition, or a rule body. {@code null} when none (a function body).
     */
    private static RNode implicitContextDefiner(RNode node) {
        for (RNode p = node.parent(); p != null; p = p.parent()) {
            if (p instanceof RInlineFunction fn
                    && (fn.isImplicit() || fn.paramNames().isEmpty())) {
                return fn;
            }
            if (p instanceof RCondition cond) {
                return AstWalker.findAncestor(cond,
                        com.regnosys.rosetta.ast.types.RDataType.class).isPresent() ? cond : null;
            }
            if (p instanceof RRule rule) {
                return rule;
            }
            if (p instanceof RRootElement) {
                return null;
            }
        }
        return null;
    }

    /** A {@code then} lambda's item is the WHOLE piped value — multi iff the pipe is. */
    private boolean implicitContextMulti(RNode definer) {
        if (definer instanceof RInlineFunction fn
                && fn.parent() instanceof RThenExpr then) {
            RExpression piped = then.left().orElse(null);
            return piped != null
                    && cardinalityComputer.computeRuleBody(piped) == ExpressionCardinality.MULTI;
        }
        return false;
    }

    /**
     * Whether {@code expr} sits DIRECTLY in a {@code then} body whose pipe is
     * MULTI — the implicit receiver the core cardinality read deliberately
     * does not OR in (see the only-element seat's note). The nearest
     * enclosing inline function decides: a then-bound function's item is the
     * whole piped value; any other lambda (a real extract/filter body) binds
     * a per-element view and stops the walk.
     */
    private boolean thenItemContextMulti(RExpression expr) {
        RNode fn = AstWalker.findAncestor(expr, RInlineFunction.class).orElse(null);
        return fn != null && implicitContextMulti(fn);
    }

    /** The implicit context's item TYPE per definer kind (MISSING declines downstream). */
    private RMetaAnnotatedType implicitContextType(RNode definer, ROnlyExistsExpr oe) {
        if (definer instanceof RCondition cond) {
            var dataType = AstWalker.findAncestor(cond,
                    com.regnosys.rosetta.ast.types.RDataType.class).orElse(null);
            return dataType != null
                    ? RMetaAnnotatedType.withNoMeta(new RDataTypeRef(dataType))
                    : RMetaAnnotatedType.MISSING;
        }
        // lambda / rule / narrowing-switch-case contexts — the engine's walk
        return typeEngine.getImplicitItemType(oe);
    }

    /**
     * Upstream {@code mayBeEmpty} (vendored {@code ExpressionValidator.java:393-395}):
     * a data type whose FULL attribute set (own + the supertype chain,
     * choice-parent options included as optional) is all-optional, or a choice.
     */
    private boolean mayBeEmpty(RType t) {
        if (t instanceof RChoiceTypeRef) {
            return true;
        }
        if (!(t instanceof RDataTypeRef dtRef)) {
            return false;
        }
        Set<com.regnosys.rosetta.ast.types.RDataType> visited = new LinkedHashSet<>();
        com.regnosys.rosetta.ast.types.RDataType current = dtRef.astNode();
        while (current != null && visited.add(current)) {
            for (RAttribute attr : current.attributes()) {
                RCardinality card = attr.cardinality().orElse(null);
                boolean optional = card != null && BigInteger.ZERO.equals(card.inf());
                if (!optional) {
                    return false;
                }
            }
            // a choice super-type contributes its options as (0..1) attributes —
            // always optional; the data super-type chain continues the walk
            current = current.superType().orElse(null);
        }
        return true;
    }

    // === T15: List operation checks ==========================================

    private void checkFilterBodyIsBoolean(RFilterExpr filter, ValidationCollector collector) {
        if (filter.body() == null || filter.body().body() == null) return;
        RMetaAnnotatedType bodyType = typeEngine.getInferredType(filter.body().body());
        if (bodyType.isMissing()) return;
        RType t = unwrapAlias(bodyType.type());
        if (!(t instanceof RBasicType b && "boolean".equals(b.name()))
            && !(t instanceof RBasicType b2 && "nothing".equals(b2.name()))) {
            collector.error(
                filter.body().body().sourceRange(),
                "Filter body must return boolean but got '" + bodyType.type().name() + "'",
                ValidationIssueCode.TYPE_ERROR);
        }
    }

    // === Helpers ==============================================================

    /**
     * Upstream {@code AbstractExpressionValidator.subtypeCheck} (the arithmetic
     * wing): errors when {@code actual} is not a subtype of {@code expected},
     * with upstream's {@code notASubtypeMessage} bytes — {@code Expected type
     * `E`, but got `A` instead. <suggestion(A)>} — as TYPE_ERROR on the
     * operand's own range (upstream anchors via the operation's LEFT/RIGHT
     * feature, which resolves to the same operand region). Upstream's
     * NOTHING-expected guard is not ported: every expected here is one of the
     * four builtin constants above, never NOTHING.
     */
    private void subtypeCheck(RMetaAnnotatedType expected, RMetaAnnotatedType actual,
                               RExpression target, Function<String, String> suggestion,
                               ValidationCollector collector) {
        if (!subtypeRelation.isSubtypeOf(actual, expected)) {
            String actualDescr = relevantTypeDescription(actual, expected);
            collector.error(
                target.sourceRange(),
                "Expected type `" + relevantTypeDescription(expected, actual)
                    + "`, but got `" + actualDescr + "` instead. " + suggestion.apply(actualDescr),
                ValidationIssueCode.TYPE_ERROR);
        }
    }

    /**
     * Upstream {@code AbstractExpressionValidator.unsupportedTypeError}:
     * {@code Operator `<op>` is not supported for type `<T>`. <supported-types>}.
     * The supported-types sentences are passed pre-built — upstream assembles
     * them from varargs, and the three call shapes here produce exactly
     * {@code Supported types are `number`, `string` and `date`} /
     * {@code … `number`, `string` and `time`} / {@code … `number` and `date`}.
     */
    private void unsupportedTypeError(RMetaAnnotatedType type, String operator, RExpression target,
                                       String supportedTypesMessage, ValidationCollector collector) {
        unsupportedTypeError(type, operator, target.sourceRange(), supportedTypesMessage, collector);
    }

    /** The same error at an explicit anchor (the only-exists seat anchors on the element, not the receiver). */
    private void unsupportedTypeError(RMetaAnnotatedType type, String operator,
                                       com.regnosys.rosetta.ast.SourceRange anchor,
                                       String supportedTypesMessage, ValidationCollector collector) {
        collector.error(
            anchor,
            "Operator `" + operator + "` is not supported for type `" + type.type().name()
                + "`. " + supportedTypesMessage,
            ValidationIssueCode.TYPE_ERROR);
    }

    /**
     * Upstream {@code AbstractExpressionValidator.relevantTypeDescription},
     * scoped to this seat: equal types include meta info, equal names include
     * type parameters, otherwise the bare name. Upstream's namespace-prepend
     * arm (equal names resolved cross-namespace) is unreachable here — the
     * expected side is always a builtin whose name is a grammar keyword, so no
     * user-defined type can share it — and is not ported.
     */
    private String relevantTypeDescription(RMetaAnnotatedType type, RMetaAnnotatedType context) {
        RType valueType = type.type();
        RType valueContext = context.type();
        if (valueType.equals(valueContext)) {
            return type.toString(); // include meta info
        }
        if (valueType.name().equals(valueContext.name())) {
            return valueType.toString(); // include type parameters
        }
        return valueType.name();
    }

    private void checkOperandIsBoolean(RExpression operand, String side, String op,
                                        ValidationCollector collector) {
        if (operand == null) return;
        RMetaAnnotatedType type = typeEngine.getInferredType(operand);
        if (type.isMissing()) return;
        RType t = unwrapAlias(type.type());
        if (!(t instanceof RBasicType b && "boolean".equals(b.name()))
            && !(t instanceof RBasicType b2 && "nothing".equals(b2.name()))) {
            collector.error(
                operand.sourceRange(),
                "Expected boolean for " + side + " operand of " + op +
                    " but got '" + type.type().name() + "'",
                ValidationIssueCode.TYPE_ERROR);
        }
    }

    // === Annex-A shared helpers (facet annexAReleasedSeverities) =============

    private static final RMetaAnnotatedType ZONED_DATE_TIME_NO_META =
            RMetaAnnotatedType.withNoMeta(RRecordType.ZONED_DATE_TIME);

    /**
     * Upstream {@code AbstractExpressionValidator.comparableTypeCheck}
     * (vendored {@code :99-107}): the operands must be comparable —
     * {@code isSubtypeOf(l, r) || isSubtypeOf(r, l)} ({@code TypeSystem
     * .isComparable}) — else the ERROR {@code Types `L` and `R` are not
     * comparable} (released-verified bytes), anchored at the operation.
     * Both-side MISSING gate (the #437 law).
     */
    private void comparableTypeCheck(RExpression op, RExpression left, RExpression right,
            ValidationCollector collector) {
        if (left == null || right == null) {
            return;
        }
        RMetaAnnotatedType tl = typeEngine.getInferredType(left);
        RMetaAnnotatedType tr = typeEngine.getInferredType(right);
        if (tl.isMissing() || tr.isMissing()) {
            return;
        }
        if (!validatorSubtype(tl, tr) && !validatorSubtype(tr, tl)) {
            collector.error(
                op.sourceRange(),
                "Types `" + relevantTypeDescription(tl, tr) + "` and `"
                        + relevantTypeDescription(tr, tl) + "` are not comparable",
                ValidationIssueCode.TYPE_ERROR);
        }
    }

    /**
     * The VALIDATOR-LOCAL subtype read (facet annexAReleasedSeverities):
     * {@link SubtypeRelation#isSubtypeOf} widened by the two declared-lineage
     * links the fork's core relation does not yet walk — an enum extending an
     * enum ({@code PartyIdentifierFormat2Enum extends LeiIdentifierFormatEnum}
     * — upstream's enum subtype walks the parent chain; the #454 census's
     * 16-carrier comparability class) and a data type extending a CHOICE
     * ({@code BasketConstituent extends Observable} — upstream's
     * choice-as-data admission; the census's 1-carrier assignability class).
     * Kept LOCAL to the validator seats deliberately: widening the core
     * relation re-prices every downstream admission (joins, generator gates
     * — the #453 law), so the core widening is the recorded typed follow-on
     * with its own probe.
     */
    private boolean validatorSubtype(RMetaAnnotatedType sub, RMetaAnnotatedType sup) {
        if (subtypeRelation.isSubtypeOf(sub, sup)) {
            return true;
        }
        RType subT = unwrapAlias(sub.type());
        RType supT = unwrapAlias(sup.type());
        if (subT instanceof REnumTypeRef subEnum && supT instanceof REnumTypeRef supEnum) {
            Set<com.regnosys.rosetta.ast.types.REnumeration> visited = new LinkedHashSet<>();
            com.regnosys.rosetta.ast.types.REnumeration current = subEnum.astNode();
            while (current != null && visited.add(current)) {
                if (current == supEnum.astNode()) {
                    return true;
                }
                current = current.superType().orElse(null);
            }
            return false;
        }
        if (subT instanceof RDataTypeRef subData) {
            Set<com.regnosys.rosetta.ast.types.RDataType> visited = new LinkedHashSet<>();
            com.regnosys.rosetta.ast.types.RDataType current = subData.astNode();
            while (current != null && visited.add(current)) {
                if (supT instanceof RDataTypeRef supData && current == supData.astNode()) {
                    return true;
                }
                var choiceSuper = current.choiceSuperType();
                if (choiceSuper.isPresent() && supT instanceof RChoiceTypeRef supChoice
                        && choiceSuper.get() == supChoice.astNode()) {
                    return true;
                }
                current = current.superType().orElse(null);
            }
        }
        return false;
    }

    /**
     * Upstream {@code AbstractExpressionValidator.commonTypeCheck} (vendored
     * {@code :153-177}): folds the expressions' types through the JOIN,
     * erroring when an element degenerates the accumulated join to ANY (the
     * top type = no common supertype): {@code Types `A`, `B` and `C` do not
     * have a common supertype} — the accumulated compatible set backtick-listed
     * against the failing element, released-verified assembly. A failing
     * element is NOT added to the set (upstream's exact accumulation).
     * Anchored at the failing element (upstream's per-index anchor resolves
     * to the same region). ANY element MISSING declines the whole check.
     */
    private void commonTypeCheck(List<RExpression> expressions, ValidationCollector collector) {
        if (expressions.isEmpty()) {
            return;
        }
        List<RMetaAnnotatedType> elementTypes = new ArrayList<>(expressions.size());
        for (RExpression e : expressions) {
            RMetaAnnotatedType t = typeEngine.getInferredType(e);
            if (t.isMissing()) {
                return;
            }
            elementTypes.add(t);
        }
        Set<RMetaAnnotatedType> types = new LinkedHashSet<>();
        RMetaAnnotatedType commonType = elementTypes.get(0);
        types.add(commonType);
        for (int i = 1; i < elementTypes.size(); i++) {
            RMetaAnnotatedType elemType = elementTypes.get(i);
            RMetaAnnotatedType newCommonType = typeJoin.join(commonType, elemType);
            if (isAnyType(newCommonType.type())) {
                final RMetaAnnotatedType elem = elemType;
                collector.error(
                    expressions.get(i).sourceRange(),
                    "Types " + types.stream()
                            .map(t -> "`" + relevantTypeDescription(t, elem) + "`")
                            .collect(Collectors.joining(", "))
                            + " and `" + relevantTypeDescription(elemType, newCommonType)
                            + "` do not have a common supertype",
                    ValidationIssueCode.TYPE_ERROR);
            } else {
                types.add(elemType);
                commonType = newCommonType;
            }
        }
    }

    private static boolean isAnyType(RType t) {
        return t == RBasicType.ANY; // genuine singleton — RBasicType.equals IS identity
    }

    /**
     * The released-9.83.0 cardinality WARNING pair (the facet note's severity
     * oracle — the vendored source's {@code error} here is a post-release
     * flip): {@code Expecting single|multi cardinality} + an optional
     * {@code ". " + suggestion}, on the FAITHFUL cardinality path
     * ({@code computeRuleBody} — see {@link #checkEqualityCardinality}'s
     * note; the global {@code compute()} is deliberately single-biased for
     * generator seats and would both over- and under-fire here).
     */
    private void singleCardinalityWarning(RExpression expr, String suggestion,
            ValidationCollector collector) {
        singleCardinalityWarning(expr, expr.sourceRange(), suggestion, collector);
    }

    /** The same warning read off {@code expr}'s cardinality, anchored at {@code anchor} (the only-exists seat). */
    private void singleCardinalityWarning(RExpression expr, com.regnosys.rosetta.ast.SourceRange anchor,
            String suggestion, ValidationCollector collector) {
        if (cardinalityReadable(expr)
                && cardinalityComputer.computeRuleBody(expr) == ExpressionCardinality.MULTI) {
            cardinalityWarning(anchor, "Expecting single cardinality", suggestion, collector);
        }
    }

    private void multiCardinalityWarning(RExpression expr, String suggestion,
            ValidationCollector collector) {
        multiCardinalityWarning(expr, expr.sourceRange(), suggestion, collector);
    }

    /**
     * Anchor-explicit form: the CHECKED expression and the SITED range may
     * differ (the disjoint seat mirrors upstream's left-feature anchor for
     * both operand checks; PR #458 anchor wave).
     */
    private void multiCardinalityWarning(RExpression expr, SourceRange anchor, String suggestion,
            ValidationCollector collector) {
        if (cardinalityReadable(expr)
                && cardinalityComputer.computeRuleBody(expr) != ExpressionCardinality.MULTI) {
            collector.warning(
                anchor,
                suggestion != null ? "Expecting multi cardinality. " + suggestion
                        : "Expecting multi cardinality",
                ValidationIssueCode.CARDINALITY_ERROR);
        }
    }

    /**
     * The #437 resolution-decline law generalized to the SYMBOL surface: a
     * cardinality warning declines when the checked operand's subtree carries
     * a name the fork did not resolve — an unresolved symbol reference or a
     * disguised-nav {@code REnumValueRef} with neither an enumeration nor a
     * resolved symbol (the product-template-type condition chains, whose
     * heads ride the TYPING-ONLY condition-context channel: the fork's
     * cardinality walk reads such shapes as a blind SINGLE, which mis-fires
     * the modifier warnings where upstream — resolving the chain — reads
     * MULTI and stays silent; the V0 banks prove the upstream reads).
     * Upstream never validates a shape it cannot resolve; the fork declines
     * shapes IT cannot fully resolve.
     */
    private boolean cardinalityReadable(RExpression expr) {
        for (RSymbolReference sr : AstWalker.findAll(expr, RSymbolReference.class)) {
            if (sr.symbol().isEmpty()) {
                return false;
            }
        }
        for (com.regnosys.rosetta.ast.expressions.references.REnumValueRef evr
                : AstWalker.findAll(expr,
                        com.regnosys.rosetta.ast.expressions.references.REnumValueRef.class)) {
            // A disguised nav is readable through exactly the bindings the
            // cardinality walk consumes (disguisedChainCardinality's resolution
            // order): a real enum constant, the callable/global symbol arm, the
            // Cat-10 attribute chain, or the lexical-head input feature. A
            // choice-option-only or restriction-only binding (the
            // product-template condition chains) carries no consumable
            // cardinality — decline.
            boolean readable = evr.enumeration().isPresent()
                    || evr.resolvedSymbol().isPresent()
                    || evr.resolvedAttributeChain().isPresent()
                    || evr.resolvedInputFeature().isPresent();
            if (!readable) {
                return false;
            }
        }
        return true;
    }

    private void cardinalityWarning(RExpression expr, String message, String suggestion,
            ValidationCollector collector) {
        cardinalityWarning(expr.sourceRange(), message, suggestion, collector);
    }

    /**
     * The ONE producer of a cardinality warning's text, at an explicit anchor (LAW 69 —
     * v3.2 seat 8 round 3: the only-exists seat anchors the single-cardinality warning on
     * {@code parentRangeOf}, the parent family's declaration, and consults it through this
     * anchor-taking form rather than restating the receiver read at the site).
     */
    private void cardinalityWarning(com.regnosys.rosetta.ast.SourceRange anchor, String message,
            String suggestion, ValidationCollector collector) {
        collector.warning(
            anchor,
            suggestion != null ? message + ". " + suggestion : message,
            ValidationIssueCode.CARDINALITY_ERROR);
    }

    /**
     * The call-argument assignability ERROR (upstream {@code subtypeCheck}
     * with the per-callable suggestion): declines on a MISSING side (the
     * #437 law) and on a NOTHING expected (upstream's own guard — a rule
     * with no declared input).
     */
    private void argSubtypeCheck(RMetaAnnotatedType expected, RExpression arg,
            Function<String, String> message, ValidationCollector collector) {
        if (expected.isMissing()) {
            return;
        }
        if (expected.type() == RBasicType.NOTHING) { // singleton identity — the Copilot R1 class
            return;
        }
        RMetaAnnotatedType actual = typeEngine.getInferredType(arg);
        if (actual.isMissing()) {
            return;
        }
        if (!validatorSubtype(actual, expected)) {
            String actualDescr = relevantTypeDescription(actual, expected);
            collector.error(
                arg.sourceRange(),
                "Expected type `" + relevantTypeDescription(expected, actual)
                        + "`, but got `" + actualDescr + "` instead. " + message.apply(actualDescr),
                ValidationIssueCode.TYPE_ERROR);
        }
    }

    private RType unwrapAlias(RType type) {
        if (type instanceof RAliasType alias) return alias.refersTo();
        return type;
    }
}
