package com.regnosys.rosetta.ir.emit.python;

import com.regnosys.rosetta.ir.emit.AbstractIRExprEmitter;
import com.regnosys.rosetta.ir.emit.EmitterException;
import com.regnosys.rosetta.ir.expr.BinaryOp;
import com.regnosys.rosetta.ir.expr.Existence;
import com.regnosys.rosetta.ir.expr.FieldAccess;
import com.regnosys.rosetta.ir.expr.IRApply;
import com.regnosys.rosetta.ir.expr.IRConditional;
import com.regnosys.rosetta.ir.expr.IREmptyLiteral;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.IRListOp;
import com.regnosys.rosetta.ir.expr.IRReference;
import com.regnosys.rosetta.ir.expr.IRLiteral;
import com.regnosys.rosetta.ir.expr.IRVariable;
import com.regnosys.rosetta.ir.expr.Let;
import com.regnosys.rosetta.ir.expr.Optionality;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.REnumTypeRef;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;

/**
 * A <strong>second-target</strong> emitter (the multi-target mainline, decision-log L-081): it lowers
 * the language-neutral Phase-2 expression IR ({@code com.regnosys.rosetta.ir.expr}) to
 * <strong>Python</strong> source, demonstrating that the same neutral tree the Java emitter
 * ({@code IRJavaLeafEmitter}) consumes also drives a different target — the pay-off of the neutral
 * middle layer (see {@code notes/multitarget-poc-scoping.md} and {@code notes/python-navigation-slice.md}).
 *
 * <p>The Java byte gate does <em>not</em> gate this emitter — a second target has no byte golden. Its
 * correctness is established by three oracles ({@code IRPythonEmitterTest}): exact emission strings, a
 * parse-and-run pass under CPython, and a same-tree pass that feeds the <em>actual</em>
 * {@code ExpressionToIRAdapter} output (the identical record the Java path consumes) through this emitter.
 * It is purely additive: it reads {@code ir.expr.*} but changes nothing there, so the Java path cannot
 * regress. Emitted expressions assume {@link #MODULE_PREAMBLE} and the named navigation/helper constants
 * are already in scope; each constant's javadoc lists exact dependencies.
 *
 * <h2>Scope</h2>
 * <ul>
 *   <li>the Wave-0 scalar literals ({@link IRLiteral.LiteralKind#INT INT} / {@link IRLiteral.LiteralKind#NUMBER
 *       NUMBER} / {@link IRLiteral.LiteralKind#STRING STRING} / {@link IRLiteral.LiteralKind#BOOLEAN BOOLEAN}),
 *       the {@code empty} absent leaf, and a variable/parameter reference;</li>
 *   <li>the boolean condition tier — a comparison/equality {@link BinaryOp}, logical {@code and}/{@code or}
 *       ({@link BinaryOp} {@code AND}/{@code OR}), and an existence/absence check ({@link Existence} —
 *       {@code exists}/{@code is absent} plus the {@code single}/{@code multiple} cardinality qualifiers
 *       ({@code resultCount() == 1} / {@code > 1}), keyed on operand cardinality) — composing through
 *       {@link IRExpr#children()} so real Rune conditions ({@code foo -> bar exists and baz = 5}) lower;</li>
 *   <li>arithmetic ({@link BinaryOp} {@code ADD}/{@code SUB}/{@code MUL}/{@code DIV}) — {@code + - *} as exact
 *       Python operators (under the {@code MAX_PREC} context) and {@code /} as decimal division at DECIMAL128
 *       (the {@code _div} helper). This reproduces only the <em>value</em> of Rune's {@code MapperMaths}/
 *       {@code BigDecimal} runtime; {@code MapperMaths}' absent-absorption (an absent operand ⇒ absent, never an
 *       error) is NOT reproduced, so an arithmetic node with an OPTIONAL operand <strong>declines</strong>
 *       (a documented gap); ordered comparison ({@code LT}/{@code GT}/{@code LTE}/{@code GTE}) over an
 *       OPTIONAL operand likewise declines fail-closed (Python {@code None < 5} raises {@code TypeError});
 *       equality ({@code EQ}/{@code NEQ}) and logical ({@code AND}/{@code OR}) are NOT affected (see the
 *       {@link IRApply} MULTI-param and {@code FLATTEN} gaps below);</li>
 *   <li><strong>feature navigation</strong> ({@link FieldAccess} — {@code a -> b -> c}),
 *       the full navigation tier — a single-result chain (None-propagation), a MULTI feature off a single
 *       receiver ({@code trade -> legs}, list navigation), a SINGLE feature off a MULTI receiver
 *       ({@code legs -> rate}, flat-map dropping per-element absents), and a MULTI feature off a MULTI
 *       receiver ({@code legs -> counterparties}, flatten = concatenate + drop nulls); every adapter-produced
 *       {@code FieldAccess} lowers. Plus a runnable <strong>function shell</strong>
 *       ({@link #emitFunction}) wrapping a body expression.</li>
 *   <li>a <strong>multi-statement function body</strong> — the neutral {@link Let} binding
 *       ({@code let binder = value in in}, the desugared form of a {@code then}-pipe) lowers to a Python
 *       statement sequence ({@code binder = <value>} lines + a final {@code return}); a {@code then}-chain
 *       flattens via {@link #emitFunction}'s {@code flattenLet}. {@code Let} is lowered only in
 *       <em>statement</em> position (the function body); an operand-position {@code Let} declines via the
 *       base {@code unsupported} choke ({@link EmitterException}) (a walrus {@code :=} is a follow-up).</li>
 *   <li>the <strong>flat list-op tier</strong> ({@link IRListOp} — {@code count}/{@code first}/{@code last}/
 *       {@code reverse}/{@code distinct} over a present-only list: {@code len(xs)} / {@code _first(xs)} /
 *       {@code _last(xs)} / {@code list(reversed(xs))} / {@code _distinct(xs)}, grounded in {@code MapperC};
 *       {@code flatten} (a list-of-lists flatten) declines as a follow-up).</li>
 *   <li>a <strong>function call</strong> ({@link IRApply} — {@code Foo(a, b)}): {@code <callee>(<args>)} with the
 *       callee name verbatim and no {@code Mapper} wrap (the Java {@code MapperS.of(fn.evaluate)} is Java-form).
 *       Args that are scalar params / literals lower; an {@code ENUM_VALUE} reference arg lowers to
 *       {@code <Enum>.<member>} (step-10), an {@code ALIAS} arg declines. Faithful
 *       for SINGLE-cardinality callee params only (see {@link #emitApply} — the MULTI-param coercion is a
 *       documented neutral-model gap).</li>
 *   <li>a <strong>conditional</strong> ({@link IRConditional} — {@code if <cond> then <a> (else <b>)?}, step-12
 *       task-2): a parenthesized Python ternary {@code (<a> if <cond> else <b>)}. A MULTI-cardinality condition
 *       declines (defensive — validator-blocked today); a genuine-else conditional whose branches disagree in
 *       cardinality also declines fail-closed (a scalar→list coercion is a documented follow-up); an OPTIONAL
 *       condition lowers FAITHFULLY, no decline — Python's {@code None} is falsy, matching Rune's
 *       absent-condition-selects-else semantic exactly. See {@link #emitConditional} for the full guard-order
 *       rationale.</li>
 * </ul>
 * Everything else is declined via {@link AbstractIRExprEmitter}'s single {@code unsupported} choke point
 * ({@link EmitterException}); this emitter overrides only the kinds it lowers. It depends on nothing
 * Java-specific: every fact it reads — {@link IRLiteral#literalKind()}/{@link IRLiteral#value()}, {@link BinaryOp#op()},
 * {@link FieldAccess#feature()}/{@link FieldAccess#cardinality()} and the operand {@code children()} — lives
 * on the neutral node; there is no {@code JavaTypeTranslator}, no scope manager, no {@code Mapper} runtime,
 * no resolver layer, and no consumption of the {@code ir.expr.anf} tier (which is the Java-target lowering;
 * a Python target would write its own normalize).
 *
 * <h2>Target decisions this emitter makes for itself</h2>
 * The neutral IR carries <em>facts</em> (kind, type, cardinality, optionality, op kind, feature name); each
 * target re-derives its own <em>form</em> from those facts (the L-029 split). This emitter's choices:
 * <ul>
 *   <li><strong>{@code empty} → {@code None}.</strong> The IR carries no monad identity (the scoping
 *       audit's headline risk); a Python target commits to {@code None} for the absent scalar.</li>
 *   <li><strong>INT → a Python {@code int}, unconditionally.</strong> Python's {@code int} is
 *       arbitrary-precision, so the Java {@code bigInteger}-hoist family (a value beyond Java
 *       {@code long}) simply disappears — the neutral {@link IRLiteral} that the Java target must hoist,
 *       Python emits inline.</li>
 *   <li><strong>Optional-bearing navigation → None-propagation</strong> (the {@link #NAV_HELPER}
 *       {@code _get} helper). An absent hop anywhere in a chain short-circuits the whole navigation to
 *       {@code None}, mirroring Rune's absorbing-absent semantics (and the Java {@code Mapper} null-safe
 *       runtime) — the neutral fact is {@link FieldAccess#optionality()}, the absorbing monoid; a Rust
 *       target would re-derive {@code Option}/{@code ?}, a Morphir target {@code Maybe}, from the same
 *       fact. Only the {@code _get} rendering is Python-owned.</li>
 *   <li><strong>An absent list → {@code []}</strong> (the {@link #LIST_NAV_HELPER} {@code _get_all} helper)
 *       — the second monad fork. A MULTI navigation yields a list; the absent/empty list is {@code []},
 *       not {@code None} (Rune carries no monad identity, so absent-multi and empty-multi collapse; every
 *       list-consuming family needs an iterable; the Java {@code MapperC} no-element runtime is {@code []}).
 *       The neutral facts are {@link FieldAccess#cardinality()} + {@code optionality()}; a Rust target would
 *       re-derive {@code Vec}/{@code vec![]}, Morphir {@code List}. Only the {@code []} rendering is Python-owned.</li>
 *   <li><strong>A flat-map drops per-element absents</strong> (the {@link #MAP_NAV_HELPER} {@code _map}
 *       helper). Navigating a SINGLE feature over a list ({@code legs -> rate}) keeps only the present
 *       results. This is NOT a target choice — it is Rune's runtime-defined list semantic ({@code MapperC}
 *       flags a null per-element result an error item and {@code getMulti()} excludes it); Python
 *       reproduces it with a filtered comprehension, Rust with {@code filter_map}, Morphir with
 *       {@code List.filterMap}.</li>
 *   <li><strong>A flatten concatenates + drops nulls</strong> (the {@link #FLATTEN_NAV_HELPER} {@code _flat_map}
 *       helper). A MULTI feature off a MULTI receiver ({@code legs -> counterparties}) maps each element to a
 *       list and concatenates, dropping null elements and absent/empty per-element lists. Like the flat-map,
 *       this is Rune's runtime-defined {@code mapC} semantic (not a target choice); Rust reproduces it with
 *       {@code flat_map}, Morphir with {@code List.concatMap}.</li>
 *   <li><strong>The Python attribute name = the Rune feature name, Pythonic {@code snake_case}, keyword-sanitized</strong>
 *       (via {@link PythonIdentifiers#snakeName} — {@code dayCountFraction} navigates as {@code 'day_count_fraction'},
 *       matching the declaration emitter's field identifier, step-11; a hard keyword like {@code global} still
 *       navigates as {@code 'global_'}, since {@code snakeName} keyword-sanitizes after snake-casing). This parallels
 *       the Java target's getter-name logic — {@code "get" + toFirstUpper(feature())} in {@code IRJavaLeafEmitter} —
 *       which reads the same neutral {@link FieldAccess#feature()} string. The function {@code def} name and its
 *       APPLY callee are the same value-level-identifier family and, as of step-11, ALSO route through
 *       {@link PythonIdentifiers#snakeName} ({@link #emitFunction} and {@link #emitApply} move together, so a
 *       call always agrees with its definition's name). Parameter, {@link Let} binder, and variable-reference
 *       names, as of step-11, ALSO route through the same {@link PythonIdentifiers#snakeName} transform
 *       ({@link #emitFunction}'s parameters, {@link #flattenLet}'s binders, and {@link #emitVariable} move
 *       together, so a body reference always agrees with its binding's name). So every value-level identifier —
 *       struct field, navigation key, function/callee name, parameter, {@code Let} binder, variable reference —
 *       snake_cases via {@code snakeName}; only type / enum-class names stay PascalCase
 *       ({@link PythonIdentifiers#safeName}) and enum-member identifiers stay verbatim
 *       ({@link PythonIdentifiers#sanitize}).</li>
 * </ul>
 */
public final class IRPythonEmitter extends AbstractIRExprEmitter<String> {

    /**
     * The module-level preamble the emitted expressions assume. A {@link IRLiteral.LiteralKind#NUMBER}
     * lowers to {@code Decimal('...')}, which requires {@code from decimal import Decimal}. It also sets the
     * decimal context precision to {@code MAX_PREC} so that {@code + - *} arithmetic is <strong>exact</strong>,
     * matching Rune's {@code BigDecimal.add/subtract/multiply} (which are unbounded — no rounding). Python's
     * default context rounds to 28 significant digits, which would diverge from Rune on a high-precision sum
     * (e.g. adding two 34-digit division results). Division does NOT use this context — it routes through
     * {@link #DIV_HELPER}'s bounded prec-34 context (a plain {@code /} under {@code MAX_PREC} would attempt
     * infinite digits on a non-terminating quotient and fail; the emitter never emits a bare {@code /}).
     * Collecting imports and emitting a module shell is a module-emitter concern, not this expression
     * emitter's; the dependency is <em>declared</em> here so a consumer (or a test oracle) supplies exactly
     * what the emitted expressions need rather than the emitter silently assuming an ambient import.
     */
    public static final String MODULE_PREAMBLE =
            "from decimal import Decimal, getcontext, MAX_PREC\ngetcontext().prec = MAX_PREC";

    /**
     * The decimal-division helper a {@code DIV} {@link BinaryOp} assumes — Rune's {@code /} is decimal
     * division at {@code java.math.MathContext.DECIMAL128} (verified: {@code MapperMaths.divide} →
     * Xtend {@code BigDecimalExtensions.operator_divide} = {@code BigDecimal.divide(_, MathContext.DECIMAL128)};
     * both operands are coerced to {@code BigDecimal}, so {@code int / int} is a decimal division, result
     * {@code number}). {@code DECIMAL128} is precision 34, rounding HALF_EVEN. Python's default context is
     * only 28 digits, so {@code _div} uses an explicit {@code Context(prec=34, rounding=ROUND_HALF_EVEN)} —
     * reproducing {@code BigDecimal.divide(_, DECIMAL128)} (e.g. {@code 1/3} → 34 threes on both). This
     * bounded context also keeps division terminating under the module's {@code MAX_PREC} default. A Rust
     * target re-derives a 34-digit decimal context, Morphir its decimal — the DECIMAL128 semantic is
     * target-neutral (a Rune runtime fact), only the {@code _div} rendering is Python's. Depends on
     * {@code Decimal} from {@link #MODULE_PREAMBLE}.
     */
    public static final String DIV_HELPER =
            "from decimal import Context, ROUND_HALF_EVEN\n"
                    + "_DIV_CTX = Context(prec=34, rounding=ROUND_HALF_EVEN)  # java.math.MathContext.DECIMAL128\n"
                    + "def _div(a, b):\n    return _DIV_CTX.divide(Decimal(a), Decimal(b))";

    /**
     * The None-propagating navigation helper a {@link FieldAccess} emission assumes — the Python form of
     * Rune's absorbing-absent navigation. {@code _get(o, 'a')} yields {@code None} when {@code o} is
     * {@code None} (an absent hop short-circuits the rest of the chain) and otherwise the attribute value;
     * a chain {@code a -> b -> c} composes as {@code _get(_get(a, 'b'), 'c')}. The {@code getattr} default
     * is a deliberate defensive leniency — it is NOT part of the absent semantics: the adapter only
     * produces a {@link FieldAccess} for a <em>resolved</em> feature, which always materializes as a named
     * attribute on the runtime object, so absent means the attribute's value is {@code None}, never a
     * missing attribute. Declared here (like {@link #MODULE_PREAMBLE}) so the module-assembler / oracle
     * supplies it rather than the emitter assuming an ambient definition.
     */
    public static final String NAV_HELPER =
            "def _get(o, a):\n    return None if o is None else getattr(o, a, None)";

    /**
     * The list-valued navigation helper a MULTI-result {@link FieldAccess} emission assumes — the Python
     * form of Rune's absent-list collapse. {@code _get_all(o, 'a')} yields {@code []} when {@code o} is
     * {@code None} (an absent receiver) OR the list attribute is absent, and otherwise the list itself; a
     * MULTI feature off a SINGLE receiver chain composes as {@code _get_all(_get(a, 'b'), 'legs')}. The
     * absent list → {@code []} choice (NOT {@code None}) is this target's <strong>list-monad</strong>
     * decision: Rune carries no monad identity (absent-multi and empty-multi are indistinguishable), every
     * list-consuming family needs an iterable, and the Java {@code MapperC} no-element runtime behaves as
     * {@code []}. Like {@link #NAV_HELPER}, the {@code getattr} default (missing attribute → {@code None} →
     * {@code []}) is a deliberate defensive leniency, NOT part of the absent semantics: the adapter only
     * produces a {@link FieldAccess} for a <em>resolved</em> feature, which always materializes as a named
     * list attribute, so absent means the value is {@code None}/missing, never a structural error. The
     * guard is {@code is not None} (not truthiness), so a present <em>empty</em> list is preserved as
     * {@code []} rather than re-defaulted. Declared here so the module-assembler / oracle supplies it.
     */
    public static final String LIST_NAV_HELPER =
            "def _get_all(o, a):\n    xs = None if o is None else getattr(o, a, None)\n    return xs if xs is not None else []";

    /**
     * The flat-map navigation helper a navigation THROUGH a MULTI receiver assumes — a SINGLE feature
     * mapped over each element of a list. {@code _map(xs, 'a')} navigates {@code a} on each element (via
     * {@link #NAV_HELPER}'s {@code _get}, so an absent element or absent feature yields {@code None}) and
     * <strong>drops the absents</strong>. Dropping (not keeping {@code None}) is Rune's runtime-defined
     * list-navigation semantic, NOT a target choice: {@code MapperC.map} flags a null per-element result
     * an error item ({@code MapperItem.getMapperItem}: {@code error = child == null}) and {@code getMulti()}
     * returns {@code nonErrorItems()} only — so the materialized list excludes the absents. A Rust target
     * reproduces it with {@code filter_map}, a Morphir target with {@code List.filterMap}. The {@code xs or
     * []} receiver guard collapses an absent OR empty source to {@code []} (consistent with
     * {@link #LIST_NAV_HELPER}'s absent-list rule); truthiness is sound here — unlike {@code _get_all},
     * which uses {@code is not None} to preserve a present empty list, {@code _map} maps an absent and an
     * empty source to the SAME {@code []}, so the distinction does not matter. Reuses {@code _get}, so
     * {@link #NAV_HELPER} must be in scope; declared here so the module-assembler / oracle supplies it.
     */
    public static final String MAP_NAV_HELPER =
            "def _map(xs, a):\n    return [v for v in (_get(x, a) for x in (xs or [])) if v is not None]";

    /**
     * The flatten navigation helper a MULTI feature off a MULTI receiver assumes — each element of a list
     * maps to a list (the multi feature), and the lists concatenate. {@code _flat_map(xs, 'a')} navigates
     * the list-valued feature {@code a} on each element (via {@link #LIST_NAV_HELPER}'s {@code _get_all}, so
     * an absent/empty per-element list yields {@code []} and contributes nothing) and flattens, dropping any
     * {@code None} elements. This is Rune's runtime-defined {@code mapC} semantic, NOT a target choice:
     * {@code MapperC.mapC} {@code addAll}s the per-element results (concatenation) via
     * {@code MapperItem.getMapperItems}, which flags a {@code null} child an error item
     * ({@code error = child == null}) and turns a {@code null}/empty child list into a single error item,
     * all dropped by {@code getMulti()}'s {@code nonErrorItems()}. A Rust target reproduces it with
     * {@code flat_map}+{@code filter_map}, a Morphir target with {@code List.concatMap}. Distinct from
     * {@link #MAP_NAV_HELPER}'s {@code _map} (a SINGLE feature over a list, no flatten) — the two coexist.
     * Reuses {@code _get_all}, so {@link #LIST_NAV_HELPER} must be in scope; declared here so the
     * module-assembler / oracle supplies it.
     */
    public static final String FLATTEN_NAV_HELPER =
            "def _flat_map(xs, a):\n    return [v for x in (xs or []) for v in _get_all(x, a) if v is not None]";

    /**
     * The {@code first} list-op helper — {@code MapperC.first()} = {@code nonErrorItems().findFirst()
     * .orElse(MapperS.ofNull())}: the first present element, or {@code None} for an empty list (Rune's
     * absorbing absent, L-082). The {@code if xs} truthiness guard is intentional and sound BECAUSE the
     * receiver is always a list (the list-monad — a MULTI navigation yields a list, never a falsy non-empty
     * scalar): a present empty list {@code []} is falsy ⟶ {@code None}, exactly {@code orElse(ofNull())}.
     * Declared here (like {@link #NAV_HELPER}) so the module-assembler / oracle supplies it.
     */
    public static final String FIRST_HELPER =
            "def _first(xs):\n    return xs[0] if xs else None";

    /**
     * The {@code last} list-op helper — {@code MapperC.last()} = {@code nonErrorItems().reduce((a,b)->b)
     * .orElse(MapperS.ofNull())}: the last present element, or {@code None} for an empty list. Same intentional
     * truthiness guard as {@link #FIRST_HELPER}.
     */
    public static final String LAST_HELPER =
            "def _last(xs):\n    return xs[-1] if xs else None";

    /**
     * The {@code distinct} list-op helper — {@code ExpressionOperators.distinct(o)} =
     * {@code o.getMulti().stream().distinct()}: an <strong>order-preserving</strong> dedup over the present-only
     * list. Java {@code Stream.distinct()} dedups by {@code equals()}; the {@code x not in out} accumulator
     * dedups by Python {@code ==} ({@code list.__contains__}), order-preserving, and — unlike
     * {@code dict.fromkeys} — works for UNHASHABLE elements (no {@code __hash__} required). For a dedup over
     * model objects the result matches the Java target when the runtime object's {@code __eq__} mirrors the
     * model's value equality — the same equality the Java target relies on via the model's generated
     * {@code equals}/{@code hashCode}. That is a property of the generated Python data layer, not something
     * {@code _distinct} itself supplies.
     *
     * <p><strong>Known gap — {@code Decimal} scale.</strong> For {@code number} elements the two targets can
     * diverge: Python's {@code Decimal} equality is value-based ({@code Decimal('1.0') == Decimal('1.00')} is
     * {@code True}), whereas Java's {@code BigDecimal.equals} is scale-sensitive ({@code 1.0} ≠ {@code 1.00}),
     * so a list differing only in trailing-zero scale dedups under Python but not under the Java target. This
     * is arguably MORE faithful to Rune, whose {@code number} is a value (scale is not semantically
     * significant); it is recorded here as a known cross-target difference, not silently assumed equivalent.
     *
     * <p>O(n²), faithful; a hashable fast-path is a later micro-optimisation.
     */
    public static final String DISTINCT_HELPER =
            "def _distinct(xs):\n    out = []\n    for x in (xs or []):\n        if x not in out:\n            out.append(x)\n    return out";

    /** The {@code empty} absent leaf lowers to Python {@code None} (this target's monad-identity choice). */
    @Override
    protected String emitEmptyLiteral(IREmptyLiteral node) {
        return "None";
    }

    /**
     * Wraps a body expression in a runnable Python function shell. A non-{@link Let} body is the single-return
     * form {@code def <name>(<params>): return <emit(body)>}; a {@link Let} body (the desugared {@code then}-pipe,
     * or a chain of them) flattens into a <strong>multi-statement</strong> body — one {@code <binder> = <value>}
     * statement per binding, then a final {@code return <result>} (see {@link #flattenLet}). The name and
     * parameter names come from the enclosing function <em>declaration</em> (an {@code RFunction}, NOT the
     * expression IR), so this shell is a thin declaration-layer wrapper; the {@code body} is what the neutral IR
     * drives. A production Python <em>module</em> emitter would walk the function declaration for the signature
     * and lower each body expression through {@link #emit}. Navigation bodies assume {@link #NAV_HELPER} is in
     * scope.
     *
     * @param name   the Rune function name — Pythonic {@code snake_case} then keyword-sanitized via
     *               {@link PythonIdentifiers#snakeName} (the value-level-identifier convention, step-11,
     *               shared with struct-field navigation) so a Rune function named {@code DayCount} emits as
     *               {@code def day_count(...)} and one named {@code global} emits as {@code def global_(...)};
     *               {@code snakeName} is <strong>idempotent</strong> ({@code snakeName("global_") == "global_"}),
     *               so a caller that pre-normalizes the signature won't double-mangle
     * @param params the parameter names, in order — each Pythonic {@code snake_case} then keyword-sanitized via
     *               {@link PythonIdentifiers#snakeName} (the value-level-identifier convention, step-11); the
     *               transform is <strong>forced</strong>: a body-position {@link IRVariable} for the same
     *               param is snake_cased identically through {@link #emitVariable}, so if the {@code def} param
     *               were left raw, the function body and signature would desync and produce a {@code SyntaxError}.
     *               If two raw names collapse onto the same identifier under {@code snakeName} (e.g. {@code global}
     *               and {@code global_} both become {@code global_}; or, a case-folding collision now possible
     *               post-step-11, {@code orderId} and {@code orderID} both become {@code order_id}), the emission
     *               declines with an {@link EmitterException} — the verb-side mirror of the declaration emitter's
     *               {@code requireFresh}, closing a duplicate-param {@code SyntaxError} (corpus-absent);
     *               param-vs-let-binder and cross-function uniquification stay deferred
     * @param body   the neutral body expression to lower (a {@link Let} chain flattens to statements)
     */
    public String emitFunction(String name, List<String> params, IRExpr body) {
        StringBuilder statements = new StringBuilder();
        String result = flattenLet(body, statements, "    ");
        String defName = PythonIdentifiers.snakeName(name);
        List<String> defParams = params.stream().map(PythonIdentifiers::snakeName).toList();
        if (Set.copyOf(defParams).size() != defParams.size()) {
            throw new EmitterException("IRPythonEmitter cannot emit `def " + defName
                    + "`: snake_case normalization collapsed two parameter names onto one identifier");
        }
        return "def " + defName + "(" + String.join(", ", defParams) + "):\n" + statements + "    return " + result;
    }

    /**
     * Flattens a (possibly nested) {@link Let} into Python statements — the multi-statement function body. For a
     * {@code Let}, it appends one {@code <indent><binder> = <value>} line and returns the continuation's result
     * expression; for any other node it returns {@link #emit}'s expression with no statement appended. The
     * <strong>value-is-{@code Let} recursion is the chain flattener</strong>: a {@code then}-chain desugars to
     * nested {@code Let}s whose OUTER {@code value} is the inner {@code Let}, so {@code flattenLet(let.value())}
     * emits the inner bindings first and yields the inner result expression, which becomes the outer binder's
     * right-hand side. A non-{@code Let} body appends nothing, so the output is exactly the single-{@code return}
     * shell — byte-stable for every non-{@code Let} body. The binder is the neutral {@link Let#binder()} name,
     * Pythonic {@code snake_case} then keyword-sanitized (via {@link PythonIdentifiers#snakeName}, step-11,
     * matching its {@link #emitVariable} references); the desugar stamps a fresh identifier (see
     * {@code ExpressionToIRAdapter.adaptThenChainToLet}), so the transform is normally inert on the live adapter
     * path (a fresh {@code _then0}-shaped name has no case boundary and is not a keyword).
     */
    private String flattenLet(IRExpr node, StringBuilder statements, String indent) {
        if (node instanceof Let let) {
            String valueExpr = flattenLet(let.value(), statements, indent); // value may itself be a Let (the chain)
            statements.append(indent).append(PythonIdentifiers.snakeName(let.binder())).append(" = ").append(valueExpr).append('\n');
            return flattenLet(let.in(), statements, indent);                // the continuation may also be a Let
        }
        return emit(node);
    }

    /**
     * A bound-variable / parameter reference lowers to its bare name, Pythonic {@code snake_case} then
     * keyword-sanitized via {@link PythonIdentifiers#snakeName} (step-11) so it matches its identically-transformed
     * definition (a {@code def} param, see {@link #emitFunction}, or a {@code Let} binder, see {@link #flattenLet}).
     * Per-scope <em>uniquification</em> of two distinct locals that snake_case-and-sanitize alike stays deferred (a
     * scope-manager concern); unlike the declaration emitter's {@code requireFresh}, the verb side has no per-scope
     * set, so such a (corpus-absent) collision would fail silent.
     */
    @Override
    protected String emitVariable(IRVariable variable) {
        return PythonIdentifiers.snakeName(variable.name());
    }

    /**
     * Lowers a by-name {@link IRReference}. Only {@code ENUM_VALUE} lowers (to Python enum-member access); the
     * other kinds decline with a self-describing message. {@code FUNCTION} never reaches here as a standalone
     * reference — it is {@link IRApply}'s callee, read directly by {@link #emitApply}; {@code SUPER}/{@code ALIAS}/
     * {@code RULE} are deferred. (Because {@link #emitApply} lowers each argument through {@link #emit}, an
     * {@code ENUM_VALUE} call argument now lowers to {@code <Enum>.<member>} as well; an {@code ALIAS} arg still
     * declines.)
     */
    @Override
    protected String emitReference(IRReference ref) {
        return switch (ref.referenceKind()) {
            case ENUM_VALUE -> emitEnumValueRef(ref);
            // Self-describing decline (the base's unsupported(kind()) would just say "REFERENCE"):
            default -> throw new EmitterException(
                    "IRPythonEmitter does not lower a reference of kind " + ref.referenceKind());
        };
    }

    /**
     * An enum-value reference → {@code <EnumClass>.<member>} (e.g. {@code DayCountFraction.ACT_360}). The enum's
     * simple class name is the NEUTRAL {@link REnumTypeRef#name()} off the resolved {@code ref.type().type()}
     * (read exactly as {@code RTypeFormatter}), {@code safeName}-normalised to match the noun emitter's
     * {@code class <safeName(en.name())>}; the member is {@code sanitize(ref.target())}, matching the noun
     * emitter's member identifier ({@code <member>} for {@code <member> = "<value>"}) — so a keyword member emits
     * {@code Foo.None_} here and {@code None_ = "None"} there. An enum constant is SINGLE+PRESENT, so it lowers to a
     * bare value (no {@code Mapper}/None-propagation wrap). Fail-closed: a null or non-enum {@code type()} declines
     * via {@link EmitterException} (unreachable for an adapter tree — the {@code isGenuineEnumValueRef} gate; the
     * guard catches a malformed/hand-built node, never an NPE).
     *
     * <p><strong>Module-assembler boundary.</strong> This renders a reference to {@code <EnumClass>} assuming the
     * enum was emitted; the declaration emitter may decline a whole enum (a reserved-shaped or sibling-colliding
     * member), so ensuring the referenced enum is actually present is the module assembler's job — the verb
     * emitter sees only the single reference, not the enum's full member set (the same cross-emitter module-level
     * concern as step-8).
     */
    private String emitEnumValueRef(IRReference ref) {
        var t = ref.type();
        if (t == null || !(t.type() instanceof REnumTypeRef en)) {
            throw new EmitterException(
                    "IRPythonEmitter: ENUM_VALUE reference type is not an enum: " + ref.target());
        }
        return PythonIdentifiers.safeName(en.name()) + "." + PythonIdentifiers.sanitize(ref.target());
    }

    /** A scalar literal, keyed off the neutral {@link IRLiteral.LiteralKind}. */
    @Override
    protected String emitLiteral(IRLiteral literal) {
        return switch (literal.literalKind()) {
            // Python int is arbitrary-precision: no long/BigInteger boundary, no bigInteger hoist.
            case INT -> ((BigInteger) literal.value()).toString();
            // Exact decimal, mirroring the Java target's BigDecimal (Rune `number` is exact).
            case NUMBER -> "Decimal('" + ((BigDecimal) literal.value()).toPlainString() + "')";
            case STRING -> "'" + escapePythonString((String) literal.value()) + "'";
            case BOOLEAN -> ((Boolean) literal.value()) ? "True" : "False";
        };
    }

    /**
     * A binary operator over two operand subtrees, recursing through {@link IRExpr#children()}. Most operators
     * (comparison / equality / logical {@code and}/{@code or}, and the {@code + - *} arithmetic) render as a
     * fully-parenthesized infix expression {@code (<left> <op> <right>)} — the parens make a nested operand's
     * Python operator precedence unambiguous (e.g. {@code ((n1 >= n2) and (foo is not None))}). Division is the
     * one exception: it lowers to the {@code _div(<left>, <right>)} helper call ({@link #DIV_HELPER}), because
     * Rune's {@code /} is decimal division at a bounded DECIMAL128 context — a bare Python {@code /} would be
     * float division (wrong) for {@code int} operands and would not terminate under the module's exact
     * {@code MAX_PREC} context. The {@code + - *} operators run under {@link #MODULE_PREAMBLE}'s
     * {@code MAX_PREC} context, so they reproduce the exact VALUE of Rune's unbounded {@code BigDecimal}
     * add/subtract/multiply.
     *
     * <p><strong>Arithmetic and ordered comparison over an OPTIONAL operand DECLINE.</strong>
     * <ul>
     *   <li><em>Arithmetic ({@code + - * /})</em>: Rune's {@code MapperMaths} is absent-absorbing — an absent
     *       operand makes the whole arithmetic absent (never errors). The emitted bare {@code + - *} /
     *       {@code _div}, by contrast, would raise a {@code TypeError} on a {@code None} operand. Rather than
     *       reproduce the absent-absorption (which would need None-propagating helpers), this emitter fails
     *       CLOSED: an {@code ADD}/{@code SUB}/{@code MUL}/{@code DIV} with an {@link Optionality#OPTIONAL}
     *       left or right operand throws {@link EmitterException} (the {@code FLATTEN} stance).</li>
     *   <li><em>Ordered comparison ({@code < > <= >=})</em>: Rune's comparison folds an absent ordered operand
     *       into a total {@code ComparisonResult} (no crash). The emitted bare Python {@code < > <= >=}, however,
     *       raises {@code TypeError} on a {@code None} operand ({@code None < 5} is a runtime error). So
     *       {@code LT}/{@code GT}/{@code LTE}/{@code GTE} with an OPTIONAL operand also decline fail-closed.</li>
     * </ul>
     * Both are documented faithfulness gaps, alongside the {@link #emitApply IRApply} MULTI-param and
     * {@code FLATTEN} gaps; present-operand arithmetic/ordering lowers unchanged. The equality operators
     * ({@code EQ}/{@code NEQ}) are NOT affected — {@code None == x} is {@code False} and {@code None != x}
     * is {@code True} in Python, faithful to Rune's not-equal-on-absent semantics. The logical operators
     * ({@code AND}/{@code OR}) are also NOT affected — the adapter admits logical only over total-boolean
     * {@code ComparisonResult} operands (never an OPTIONAL operand).
     *
     * <p><strong>MULTI-cardinality operand declines (all operators).</strong> A MULTI-cardinality left
     * or right operand on ANY binary op ({@link #emitBinaryOp}) declines fail-closed via {@link EmitterException}
     * — comparison, equality, ordering, arithmetic, and logical all assume SINGLE operands. The adapter
     * currently admits only scalar operands (the all/any cardinality modifier is deferred), so this guard
     * is purely defensive: a future all/any admission cannot silently emit {@code (list > scalar)}.
     */
    @Override
    protected String emitBinaryOp(BinaryOp node) {
        BinaryOp.BinOp op = node.op();
        // A MULTI-cardinality operand on ANY binary op declines fail-closed: all binary operators
        // (comparison, equality, ordering, arithmetic, logical) assume SINGLE-cardinality operands.
        // The adapter currently admits only scalar operands (the all/any modifier is deferred), so this
        // guard is purely defensive — a future all/any admission cannot silently emit `(list > scalar)`.
        if (node.left().cardinality() == ExpressionCardinality.MULTI
                || node.right().cardinality() == ExpressionCardinality.MULTI) {
            throw new EmitterException("IRPythonEmitter does not lower " + op
                    + " over a MULTI-cardinality operand: all binary operators assume SINGLE operands"
                    + " (the all/any cardinality modifier is deferred — a documented gap)");
        }
        // Arithmetic and ordered comparison over an OPTIONAL operand decline (fail-closed): both would raise
        // TypeError on a None operand. Rune absorbs/folds an absent operand without error in each case
        // (MapperMaths absent-absorption for arithmetic; ComparisonResult fold for ordered comparison).
        // EQ/NEQ are None-safe in Python (None == x is False; None != x is True). AND/OR are not affected
        // (the adapter guarantees total-boolean ComparisonResult operands). See the javadoc.
        if ((isArithmetic(op) || isOrdering(op))
                && (node.left().optionality() == Optionality.OPTIONAL
                        || node.right().optionality() == Optionality.OPTIONAL)) {
            throw new EmitterException("IRPythonEmitter does not lower " + op
                    + " over an OPTIONAL operand: the emitted Python operator raises TypeError on a None"
                    + " operand — Rune absorbs/folds an absent operand without error (arithmetic:"
                    + " MapperMaths absent-absorption; ordered comparison: ComparisonResult fold)"
                    + " — a documented faithfulness gap; EQ/NEQ are None-safe and unaffected");
        }
        // Division is a helper call, not an infix operator (decimal division at DECIMAL128 — see DIV_HELPER).
        if (op == BinaryOp.BinOp.DIV) {
            return "_div(" + emit(node.left()) + ", " + emit(node.right()) + ")";
        }
        String operator = pythonBinaryOperator(op);
        return "(" + emit(node.left()) + " " + operator + " " + emit(node.right()) + ")";
    }

    /** Whether {@code op} is one of the arithmetic operators ({@code + - * /}). */
    private static boolean isArithmetic(BinaryOp.BinOp op) {
        return op == BinaryOp.BinOp.ADD || op == BinaryOp.BinOp.SUB
                || op == BinaryOp.BinOp.MUL || op == BinaryOp.BinOp.DIV;
    }

    /**
     * Whether {@code op} is one of the ordered comparison operators ({@code < > <= >=}). These
     * operators raise {@code TypeError} on a {@code None} operand in Python (unlike equality,
     * where {@code None == x} is {@code False}), so they carry the same OPTIONAL-operand decline
     * as arithmetic (see {@link #emitBinaryOp}).
     */
    private static boolean isOrdering(BinaryOp.BinOp op) {
        return op == BinaryOp.BinOp.LT || op == BinaryOp.BinOp.GT
                || op == BinaryOp.BinOp.LTE || op == BinaryOp.BinOp.GTE;
    }

    /**
     * A feature navigation lowers via a four-way branch on the neutral cardinality facts — the
     * <strong>accumulated</strong> {@link FieldAccess#cardinality()}, the receiver's cardinality, and (for a
     * MULTI receiver) the per-hop {@link FieldAccess#featureCardinality()}:
     * <ul>
     *   <li><strong>SINGLE</strong> result → None-propagating attribute access
     *       {@code _get(<receiver>, '<feature>')} (see {@link #NAV_HELPER}), recursing through the receiver
     *       so {@code a -> b -> c} composes as {@code _get(_get(a, 'b'), 'c')};</li>
     *   <li>MULTI result off a <strong>SINGLE</strong> receiver (a multi feature off a scalar receiver
     *       chain, e.g. {@code trade -> legs}) → list navigation {@code _get_all(<receiver>, '<feature>')}
     *       (see {@link #LIST_NAV_HELPER}), absent list → {@code []};</li>
     *   <li>a SINGLE feature off a <strong>MULTI</strong> receiver (a flat-map over a list, e.g.
     *       {@code trade -> legs -> rate} or {@code legs -> rate}) → {@code _map(<receiver>, '<feature>')}
     *       (see {@link #MAP_NAV_HELPER}), which maps each element and drops the per-element absents;</li>
     *   <li>a MULTI feature off a MULTI receiver (a flatten, e.g. {@code legs -> counterparties}) →
     *       {@code _flat_map(<receiver>, '<feature>')} (see {@link #FLATTEN_NAV_HELPER}), which maps each
     *       element to a list and concatenates, dropping nulls.</li>
     * </ul>
     *
     * <p><strong>Why the branch reads three facts.</strong> The adapter computes {@code cardinality =
     * featureCardinality MULTI || receiver MULTI}, so: a SINGLE result is the scalar case; a MULTI result
     * with a SINGLE receiver necessarily means THIS hop is the multi feature (the list-nav case — no
     * {@code featureCardinality} read needed there); but under a MULTI receiver the accumulated
     * {@code cardinality()} is always MULTI, so {@code featureCardinality()} is the <em>sole</em>
     * discriminator between a map (SINGLE feature → {@code _map}) and a flatten (MULTI feature →
     * {@code _flat_map}). This mirrors the Java target, which reads the same {@code featureCardinality()} to
     * pick {@code map} vs {@code mapC} ({@code IRJavaLeafEmitter}).
     *
     * <p><strong>The gate is total — navigation no longer declines.</strong> The four branches cover every
     * reachable {@code (cardinality, receiver.cardinality, featureCardinality)} triple (a MULTI receiver
     * forces a MULTI result, so a SINGLE-result/MULTI-receiver combination is unrepresentable), and
     * {@code featureCardinality()} is a binary enum — so the final branch is unconditionally the
     * {@code (MULTI, MULTI, MULTI)} flatten case by elimination. The recursion is total over every MULTI
     * receiver too (a {@code _get_all}/{@code _map}/{@code _flat_map} list or a bare multi param), so
     * arbitrarily deep navigation chains all lower. The feature name is the Rune source name, Pythonic
     * {@code snake_case} then keyword-sanitized (via {@link PythonIdentifiers#snakeName} — {@code dayCountFraction}
     * navigates as {@code 'day_count_fraction'}, a keyword field like {@code global} still as {@code 'global_'})
     * to match the declaration emitter's field identifier; see the class doc.
     */
    @Override
    protected String emitFieldAccess(FieldAccess fieldAccess) {
        // A Rune feature name is an identifier, so it embeds in a Python string literal without escaping.
        String feature = "'" + PythonIdentifiers.snakeName(fieldAccess.feature()) + "'";
        if (fieldAccess.cardinality() == ExpressionCardinality.SINGLE) {
            return "_get(" + emit(fieldAccess.receiver()) + ", " + feature + ")";
        }
        // MULTI result off a SINGLE receiver → this hop is the multi feature: the list nav.
        if (fieldAccess.receiver().cardinality() == ExpressionCardinality.SINGLE) {
            return "_get_all(" + emit(fieldAccess.receiver()) + ", " + feature + ")";
        }
        // MULTI receiver → a navigation over a list. featureCardinality is the sole discriminator left:
        // a SINGLE feature is a map (drop per-element absents).
        if (fieldAccess.featureCardinality() == ExpressionCardinality.SINGLE) {
            return "_map(" + emit(fieldAccess.receiver()) + ", " + feature + ")";
        }
        // A MULTI feature off a MULTI receiver: a flatten (concatenate per-element lists, drop nulls). This
        // is the final case by elimination — featureCardinality() is binary, so under a MULTI receiver the
        // only alternative to SINGLE is MULTI; the gate is total (no navigation decline).
        return "_flat_map(" + emit(fieldAccess.receiver()) + ", " + feature + ")";
    }

    /**
     * Maps a neutral infix {@link BinaryOp.BinOp} (comparison / equality / logical / {@code + - *}) to its
     * Python token. Rune's {@code and}/{@code or} are null-safe, but the adapter admits a logical only over
     * total-boolean {@code ComparisonResult} operands (never an absent/bare-boolean operand), so plain Python
     * {@code and}/{@code or} over bools is faithful. The {@code + - *} arithmetic operators run under
     * {@link #MODULE_PREAMBLE}'s {@code MAX_PREC} context, so they are exact (matching Rune's unbounded
     * {@code BigDecimal} add/subtract/multiply). {@code DIV} is NOT an infix token — it lowers to the
     * {@code _div} helper (intercepted in {@link #emitBinaryOp}); it throws here so the exhaustive
     * {@code switch} (no {@code default}) keeps every operator compile-checked.
     */
    private static String pythonBinaryOperator(BinaryOp.BinOp op) {
        return switch (op) {
            case EQ -> "==";
            case NEQ -> "!=";
            case LT -> "<";
            case GT -> ">";
            case LTE -> "<=";
            case GTE -> ">=";
            case AND -> "and";
            case OR -> "or";
            case ADD -> "+";
            case SUB -> "-";
            case MUL -> "*";
            // Division is a helper call, not an infix operator — handled in emitBinaryOp before this point.
            case DIV -> throw new UnsupportedOperationException(
                    "DIV is lowered via the _div helper in emitBinaryOp, not as an infix operator");
        };
    }

    /**
     * An existence / absence check ({@code <arg> exists} / {@code is absent} / {@code single}/{@code multiple}
     * exists) over one operand subtree. The check is total (a present, single boolean), keyed on
     * {@link Existence#op()}, the optional {@link Existence#modifier()}, and the operand's cardinality
     * ({@code arg().cardinality()}): a SINGLE operand is {@code None} for absent (L-082), so a check is a
     * {@code None} comparison; a MULTI operand emits a Python list whose elements are all present (non-null),
     * so a check is a length test. Two of the list sources actively drop per-element absents
     * ({@code _map}/{@code _flat_map}, mirroring Rune's {@code MapperC} error-item exclusion); the others
     * ({@code _get_all} and a bare multi param) carry no nulls because the Rune object model forbids a null
     * element inside a list attribute — so {@code len(...)} is a faithful {@code resultCount()} in every case.
     * This is faithful to the Rune runtime ({@code ExpressionOperators}): {@code exists} is
     * {@code resultCount() > 0}, {@code single exists} is {@code resultCount() == 1}, {@code multiple exists} is
     * {@code resultCount() > 1}, where {@code MapperS.resultCount()} is a pure null check (a scalar's count is
     * {@code 0}/{@code 1}) and {@code MapperC.resultCount()} counts the non-error (present) items — exactly
     * {@code len(<the lowered Python list>)}. A Rust target re-derives {@code Option}/{@code Vec} length checks.
     *
     * <p><strong>{@code is absent} ignores any modifier</strong> — there is no {@code single}/{@code multiple}
     * "is absent" (legacy {@code ExistenceHandler} maps every {@code ABSENT} to {@code notExists} regardless of
     * the modifier; the modifier grammar attaches only to {@code exists}). So this branches on {@code op()}
     * FIRST and returns the plain absent check; a (non-producible) {@code ABSENT}+modifier is a dead-but-safe
     * branch.
     *
     * <p><strong>The SCALAR modifier cells are a cardinality-fact-driven constant fold.</strong> A SINGLE
     * operand has {@code resultCount() ∈ {0,1}}, so {@code single exists} ({@code == 1}) ⟺ present ⟺
     * {@code is not None} (identical to plain {@code exists} for a scalar — {@code single} is redundant there),
     * and {@code multiple exists} ({@code > 1}) is structurally impossible ⟶ the constant {@code False} (the
     * pure operand folds away). These cells are degenerate (the modifiers interrogate a list's count; a scalar
     * field's modifier is likely parser-restricted), but handling them keeps the family TOTAL over anything the
     * adapter — which admits an existence operand of either cardinality — can produce, and the fold is the
     * faithful, target-neutral re-derivation from the neutral {@link ExpressionCardinality} fact (the L-029
     * split — every target folds the same).
     */
    @Override
    protected String emitExistence(Existence existence) {
        String arg = emit(existence.arg());
        boolean multi = existence.arg().cardinality() == ExpressionCardinality.MULTI;
        // `is absent` ignores any single/multiple modifier (legacy notExists) — no single/multiple "is absent".
        if (existence.op() == Existence.ExistOp.ABSENT) {
            return multi ? "(len(" + arg + ") == 0)" : "(" + arg + " is None)";
        }
        // exists, optionally with a single/multiple cardinality qualifier (resultCount() == 1 / > 1).
        Existence.ExistMod modifier = existence.modifier();
        if (modifier == null) {
            // exists: at least one present value — a non-None scalar, or a non-empty present-element list.
            return multi ? "(len(" + arg + ") > 0)" : "(" + arg + " is not None)";
        }
        return switch (modifier) {
            // single exists: exactly one present value (resultCount() == 1). A scalar's count is ≤ 1, so == 1
            // ⟺ present (the same form as plain exists for a scalar).
            case SINGLE -> multi ? "(len(" + arg + ") == 1)" : "(" + arg + " is not None)";
            // multiple exists: more than one present value (resultCount() > 1). A scalar's count is ≤ 1, so this
            // is structurally False (the pure operand folds away — a cardinality-fact-driven constant fold).
            case MULTIPLE -> multi ? "(len(" + arg + ") > 1)" : "False";
        };
    }

    /**
     * A flat postfix list operation over one receiver subtree, dispatching on {@link IRListOp.Kind}. The
     * list-valued ops ({@code FIRST}/{@code LAST}/{@code REVERSE}/{@code DISTINCT}) read a receiver that lowers
     * to a Python list carrying no nulls (the list-monad — {@code _get_all}/{@code _map}/{@code _flat_map} or a
     * bare multi param): {@code _map}/{@code _flat_map} actively drop per-element absents (Rune's
     * {@code MapperC} error-item exclusion), while {@code _get_all} and a bare multi param carry none because
     * the Rune object model forbids a null element inside a list attribute. Faithful to the Rune runtime (each
     * {@code MapperC} op operates on {@code nonErrorItems()}). {@code COUNT} is the exception: it applies to a
     * child of <em>either</em> cardinality, so it keys on the child's neutral {@link IRExpr#cardinality()}
     * rather than assuming a list (see its bullet).
     * <ul>
     *   <li>{@code COUNT} → {@code len(<child>)} for a MULTI child ({@code MapperC.resultCount()}; the same
     *       {@code len} the existence tier uses); for a SINGLE/optional child it folds to
     *       {@code (0 if <child> is None else 1)} — {@code MapperS.resultCount()} is a pure null check yielding
     *       {@code 0}/{@code 1}, and {@code len(<scalar>)} would be a {@code TypeError}. Keyed on the child's
     *       neutral cardinality, mirroring the existence tier;</li>
     *   <li>{@code REVERSE} → {@code list(reversed(xs))} — a FRESH list (never mutates the receiver, matching
     *       {@code MapperC.reverse} collecting into a new list);</li>
     *   <li>{@code FIRST}/{@code LAST} → {@link #FIRST_HELPER}/{@link #LAST_HELPER} (empty ⟶ {@code None}); a helper
     *       avoids evaluating the receiver twice in {@code xs[0] if xs else None};</li>
     *   <li>{@code DISTINCT} → {@link #DISTINCT_HELPER} (order-preserving value-equality dedup).</li>
     * </ul>
     * {@code COUNT}/{@code REVERSE} are inline (the receiver is evaluated once). The flavour is the neutral
     * {@link IRListOp.Kind}; a Rust target re-derives {@code .len()}/{@code .iter().rev()}/{@code .first()}/
     * {@code .dedup()}, a Morphir target {@code List.length}/{@code reverse}/{@code head}/{@code unique}.
     *
     * @throws EmitterException for {@code FLATTEN} — a list-of-lists flatten
     *         ({@code MapperListOfLists.flattenList}, distinct from the nav-tier {@code _flat_map}); its
     *         element/empty-sublist null grounding is a later slice. Likewise for {@code ONLY_ELEMENT}
     *         (the #498 Java teach): its exactly-one collapse semantics ({@code MapperC.get()} — the
     *         item when the list holds exactly one, {@code None} otherwise) and the single-child
     *         self-unwrapping nuance are the rune-ir-python wave's own slice.
     */
    @Override
    protected String emitListOp(IRListOp node) {
        String child = emit(node.child());
        return switch (node.op()) {
            // COUNT is resultCount(): len() over a MULTI list, but a 0/1 null-fold over a SINGLE/optional child
            // (len(scalar) would be a TypeError). Keyed on the child's neutral cardinality, like the existence tier.
            case COUNT -> node.child().cardinality() == ExpressionCardinality.MULTI
                    ? "len(" + child + ")"
                    : "(0 if " + child + " is None else 1)";
            case REVERSE -> "list(reversed(" + child + "))";
            case FIRST -> "_first(" + child + ")";
            case LAST -> "_last(" + child + ")";
            case DISTINCT -> "_distinct(" + child + ")";
            case FLATTEN -> throw new EmitterException(
                    "IRPythonEmitter does not lower a FLATTEN list-op (list-of-lists) — outside the current slice");
            case ONLY_ELEMENT -> throw new EmitterException(
                    "IRPythonEmitter does not lower an ONLY_ELEMENT list-op (the exactly-one collapse) — the"
                            + " rune-ir-python wave's own slice (#498 taught the Java route only)");
            case SUM -> throw new EmitterException(
                    "IRPythonEmitter does not lower a SUM list-op (the numeric aggregate) — the"
                            + " rune-ir-python wave's own slice (#519 taught the Java oracle route only)");
        };
    }

    /**
     * A function application ({@link IRApply}) — {@code <callee>(<args>)}. The callee this arm renders is an
     * {@link IRReference} of kind {@code FUNCTION}; a {@code RULE} callee (the bare delegation, or the #498
     * args-present rule invocation) throws — its Python lowering is the rune-ir-python wave's own slice — and
     * a non-reference callee is malformed. The rendered callee's {@link IRReference#target()} is the Rune function name, Pythonic
     * {@code snake_case} then keyword-sanitized via {@link PythonIdentifiers#snakeName} (step-11) — a callee is a
     * value-level identifier, the same convention as the {@code def} name it must agree with (see
     * {@link #emitFunction}): a callee named {@code DayCount} lowers to {@code day_count(...)}, matching the
     * {@code def day_count(...)} shell emitted for that same function, so the call and its definition stay
     * name-consistent. The callee is a simple {@code RFunction.name()}, so a dotted name cannot reach here
     * — the neutral node carries only the simple name, the L-029 split. The callee name is read directly, NOT via {@link #emit}, because a
     * standalone FUNCTION {@link IRReference} has no {@code emit} arm (the call form is APPLY's own concern). Each
     * argument lowers through {@link #emit}: a scalar parameter (→ its bare name), a literal, or a genuine
     * {@code ENUM_VALUE} {@code IRReference} (→ {@code <Enum>.<member>}, step-10) lower; an {@code ALIAS} argument
     * still declines (no ALIAS arm) ⟶ the whole call declines.
     *
     * <p><strong>No result wrapping.</strong> The Java target wraps {@code MapperS.of(fn.evaluate(args))} and
     * unwraps each arg; a Python target has no {@code Mapper} runtime — a function returns its value directly and
     * args are plain values — so the lowering is a bare call (the node's {@code MapperS.of} wrap is, by the
     * node's own javadoc, an emitter concern).
     *
     * <p><strong>Limitation — SINGLE-cardinality callee params only.</strong> {@code adaptApply} admits a MULTI
     * callee parameter, and the arg-shape gate does not see the param's cardinality; Java coerces a scalar arg
     * into a {@code 0..*} param to a 0/1-element list ({@code CallParamMultiResolver} + {@code MapperS.getMulti()}).
     * That per-arg target cardinality is NOT carried on the neutral {@code IRApply} (it is derivable from the
     * resolved callee, a Java-style emission fact), so this emitter cannot detect or reproduce the coercion: a
     * bare {@code <name>(scalar)} into a MULTI param would be wrong. The emission is therefore faithful only for
     * all-SINGLE-param callees (the oracle uses only those). Enriching {@code IRApply} with the per-arg target
     * cardinality, or a Python-side resolver, is a documented follow-up (a genuine neutral-model gap).
     */
    @Override
    protected String emitApply(IRApply node) {
        // The adaptApply invariant guarantees an IRReference callee; guard the cast so a malformed tree fails
        // through the emitter's own choke point with a clear message rather than a raw ClassCastException.
        if (!(node.callee() instanceof IRReference ref)) {
            throw new EmitterException(
                    "IRPythonEmitter: APPLY callee is not an IRReference: " + node.callee().kind());
        }
        // #498: a RULE callee is now reachable (the bare delegation always was; the args-present
        // rule invocation joined at the Java teach). Its Python lowering (the rule-class call
        // convention) is the rune-ir-python wave's own slice — defer honestly rather than render
        // a rule as a bare snake_case function call.
        if (ref.referenceKind() == IRReference.ReferenceKind.RULE) {
            throw new EmitterException(
                    "IRPythonEmitter does not lower a RULE-callee apply (bare delegation / the #498"
                            + " args-present rule invocation) — the rune-ir-python wave's own slice");
        }
        String name = PythonIdentifiers.snakeName(ref.target());
        StringBuilder out = new StringBuilder(name).append('(');
        List<IRExpr> args = node.args();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(emit(args.get(i)));
        }
        return out.append(')').toString();
    }

    /**
     * A conditional {@code if <condition> then <thenBranch> (else <elseBranch>)?} lowers to a parenthesized
     * Python conditional expression {@code (<then> if <cond> else <else>)} (design {@code L-132} §3.2),
     * recursing through {@link IRExpr#children()} exactly like {@link #emitBinaryOp}'s operands. Python's
     * conditional expression is <strong>lazy</strong> — it evaluates {@code cond}, then only the selected
     * branch — matching Rune's lazy branch selection; the IR branches are pure value expressions (no side
     * effects), so the laziness introduces no divergence. The parens keep a nested operand/ternary
     * unambiguous, mirroring {@link #emitBinaryOp}'s fully-parenthesized infix convention.
     *
     * <p><strong>Guard order — the D8 condition check precedes the D10 branch check; both run before any
     * emission.</strong> The two guards test independent facts (the condition's cardinality vs. the two
     * branches' cardinalities), so one node could trip both (a MULTI condition with mismatched branches);
     * D8 running first only decides which decline <em>message</em> such a node gets — the decline outcome
     * itself does not depend on the order. (The order does, however, shape the decline tests: the D10 test
     * uses a SINGLE condition so its node reaches D10 rather than tripping D8 first.)
     * <ol>
     *   <li><strong>D8 — a MULTI-cardinality condition declines</strong> (defensive). {@code
     *       ExpressionValidator.checkConditionIsSingle} already enforces a scalar condition on the adapter
     *       path, so this is unreachable today; the guard mirrors {@link #emitBinaryOp}'s MULTI-operand
     *       guard, closing the same fail-closed posture against a future admission. An OPTIONAL condition
     *       is a <em>different</em> fact from a MULTI one and is NOT declined — that is D8's faithful half,
     *       below.</li>
     *   <li><strong>D10 — a mixed-cardinality genuine-else conditional declines</strong>, fail-closed. A
     *       genuine else whose branches disagree in cardinality (e.g. a SINGLE then vs. a MULTI else —
     *       valid Rune; {@code CardinalityProvider} joins such a pair to MULTI) would emit a scalar on one
     *       path and a list on the other, uniformly labeled MULTI, so a wrapping {@code count}/{@code len}
     *       would crash on the scalar branch. Java coerces the scalar branch via {@code
     *       MapperS.getMulti()}; reproducing that ({@code [x] if x is not None else []}) is a documented
     *       follow-up, so this emitter declines rather than emit a silently mistyped tree. (A no-else
     *       conditional never reaches this guard — {@code elseBranch() == null} short-circuits it.)</li>
     * </ol>
     *
     * <p><strong>D7 — the no-else absent value keys on {@code node.cardinality()}</strong>, the adapter's
     * accurate branch-join fact (not the degenerate {@code CardinalityComputer} SINGLE stamp every
     * {@code RConditionalExpr} carries): {@code []} for a MULTI conditional, {@code None} for SINGLE — the
     * emitter's established list-monad convention (see {@link #LIST_NAV_HELPER}: an absent list is
     * {@code []}, never {@code None}). This value is reachable only for a no-else conditional (a genuine
     * else always emits both branches, never the placeholder); for a no-else conditional {@code
     * node.cardinality() == thenBranch().cardinality()} exactly, so e.g. {@code if foo exists then
     * trade->legs} emits {@code (_get_all(trade, 'legs') if (foo is not None) else [])} and a downstream
     * {@code len(...)} stays well-typed — storing {@code None} there would raise a {@code TypeError}.
     *
     * <p><strong>An OPTIONAL condition lowers FAITHFULLY (D8's faithful half — its other half is the MULTI
     * decline above), not a decline.</strong> Rune pins absent-condition semantics at {@code
     * ControlFlowHandler}: {@code condition.getOrDefault(false) ? then : else} — an absent condition selects
     * the ELSE branch. Python's {@code None} is falsy, so {@code <then> if None else <else>} already selects
     * {@code <else>} — an exact match, with no propagating helper needed. The condition is <em>statically</em>
     * boolean-typed (the type system's expected-type mechanism pins it to {@code BOOLEAN}), so over
     * <em>validated</em> data it carries only {@code True}/{@code False}/{@code None} and Python truthiness
     * agrees with Rune exactly. Like every hook here, this assumes validated inputs — the emitter reproduces
     * Rune's <em>value</em> semantics, and field-type enforcement is the separate validator layer's job (the
     * construction-vs-validation split), not re-checked at emit time; a runtime-mistyped boolean field is a
     * data-validity concern, not an emitter one.
     *
     * @param node the conditional to lower
     * @return the parenthesized Python ternary
     * @throws EmitterException if the condition is MULTI-cardinality (D8), or the conditional has a
     *         genuine else whose branches disagree in cardinality (D10)
     */
    @Override
    protected String emitConditional(IRConditional node) {
        if (node.condition().cardinality() == ExpressionCardinality.MULTI) {
            throw new EmitterException("IRPythonEmitter does not lower a CONDITIONAL over a"
                    + " MULTI-cardinality condition: the condition must be scalar boolean"
                    + " (ExpressionValidator.checkConditionIsSingle enforces this on the adapter path today"
                    + " — a documented defensive guard)");
        }
        if (node.elseBranch() != null && node.thenBranch().cardinality() != node.elseBranch().cardinality()) {
            throw new EmitterException("IRPythonEmitter does not lower a CONDITIONAL whose then/else"
                    + " branches disagree in cardinality (then=" + node.thenBranch().cardinality()
                    + ", else=" + node.elseBranch().cardinality() + "): a scalar-vs-list ternary would be"
                    + " mislabeled uniformly, breaking a downstream count/len (the Java MapperS.getMulti()"
                    + " scalar coercion is a documented follow-up)");
        }
        String absent = node.cardinality() == ExpressionCardinality.MULTI ? "[]" : "None";
        String elseExpr = node.elseBranch() != null ? emit(node.elseBranch()) : absent;
        return "(" + emit(node.thenBranch()) + " if " + emit(node.condition()) + " else " + elseExpr + ")";
    }

    /** Escapes a raw (un-escaped) Rune string lexeme into a Python single-quoted literal body. */
    private static String escapePythonString(String raw) {
        StringBuilder out = new StringBuilder(raw.length() + 2);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '\'' -> out.append("\\'");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\x%02x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
