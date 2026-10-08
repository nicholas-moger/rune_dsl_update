package com.regnosys.rosetta.generator.java.statement.builder;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.JavaAssignment;
import com.regnosys.rosetta.generator.java.statement.JavaExpressionStatement;
import com.regnosys.rosetta.generator.java.statement.JavaLambdaBody;
import com.regnosys.rosetta.generator.java.statement.JavaLocalVariableDeclarationStatement;
import com.regnosys.rosetta.generator.java.statement.JavaReturnStatement;
import com.regnosys.rosetta.generator.java.statement.JavaStatement;
import com.regnosys.rosetta.generator.java.statement.JavaStatementList;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

/**
 * An arbitrary Java expression. The simplest {@link JavaStatementBuilder}.
 *
 * <p>Replaces upstream's {@code StringConcatenationClient}-based rendering
 * with string-based rendering via {@link #renderToString()}.
 *
 * <p>PR-A v6.1/v6.2: carries {@code refs} (library/domain classes referenced
 * by the rendered source) and {@code staticWildcardImports} (static wildcard
 * import targets like {@code ExpressionOperatorsNullSafe}) alongside the
 * expression. These travel with the builder through unwrap / map / then /
 * collapse and feed {@code ImportCollector} directly without any string
 * scanning.
 *
 * <p>The optional {@link #unwrapToBuilder} field supports wrap factories
 * (e.g. {@link #wrappedInMapperSOf}) — when set, unwrap methods return the
 * inner builder directly (source strip + MAPPER_S drop atomic).
 */
public abstract class JavaExpression extends JavaStatementBuilder implements JavaLambdaBody {
    private final JavaType type;
    private final Set<JavaClass<?>> refs;
    private final Set<JavaClass<?>> staticWildcardImports;
    private final Optional<JavaStatementBuilder> unwrapToBuilder;

    /**
     * Legacy 1-arg constructor — predates the v6.1 {@code refs} /
     * {@code staticWildcardImports} fields. Retained for the three
     * zero-ref subclasses: {@link JavaLiteral}, {@link JavaThis},
     * {@link JavaVariable}. These emit only language-level tokens
     * (literals, {@code this}, local identifiers) — no library or
     * domain class references travel through them, so empty refs and
     * empty staticWildcardImports are semantically correct.
     *
     * <p>New subclasses that emit library/domain class references
     * MUST use one of the protected 2/3/4-arg constructors so the
     * refs propagate into {@code ImportCollector} via
     * {@link #getRefs()}; relying on this 1-arg constructor silently
     * drops the refs.
     */
    public JavaExpression(JavaType type) {
        this(type, Set.of(), Set.of(), Optional.empty());
    }

    /** 2-arg constructor — refs declared; staticWildcardImports default empty. */
    protected JavaExpression(JavaType type, Set<JavaClass<?>> refs) {
        this(type, refs, Set.of(), Optional.empty());
    }

    /** 3-arg constructor — adds staticWildcardImports. */
    protected JavaExpression(JavaType type, Set<JavaClass<?>> refs,
                             Set<JavaClass<?>> staticWildcardImports) {
        this(type, refs, staticWildcardImports, Optional.empty());
    }

    /** Full-control constructor — used by wrap factories. */
    protected JavaExpression(JavaType type, Set<JavaClass<?>> refs,
                             Set<JavaClass<?>> staticWildcardImports,
                             Optional<JavaStatementBuilder> unwrapToBuilder) {
        this.type = type;
        this.refs = Set.copyOf(Objects.requireNonNull(refs, "refs"));
        this.staticWildcardImports = Set.copyOf(
                Objects.requireNonNull(staticWildcardImports, "staticWildcardImports"));
        this.unwrapToBuilder = Objects.requireNonNull(unwrapToBuilder, "unwrapToBuilder");
    }

    /**
     * Legacy 2-arg factory — delegates with empty refs + staticWildcardImports.
     */
    public static JavaExpression from(String code, JavaType type) {
        return from(code, type, Set.of(), Set.of());
    }

    /**
     * 3-arg factory — refs declared; staticWildcardImports empty (common case).
     */
    public static JavaExpression from(String code, JavaType type, Set<JavaClass<?>> refs) {
        return from(code, type, refs, Set.of());
    }

    /**
     * 4-arg factory — refs + staticWildcardImports. The primary factory
     * for emissions that need both channels populated. Widely used across
     * the handler and coercer surface post-C3a.4:
     * <ul>
     *   <li>ComparisonHandler + ExistenceHandler + SetOperationHandler
     *       declare {@code ExpressionOperatorsNullSafe.*} as a static
     *       wildcard alongside their refs (areEqual / exists / choice /
     *       contains / disjoint emissions).</li>
     *   <li>Wrapper handlers (ConversionHandler, NavigationHandler) use
     *       it to propagate operand refs + staticWildcardImports through
     *       chained emissions — required after Copilot rounds 6-11
     *       established the cross-handler preservation invariant.</li>
     *   <li>Builder-level utilities ({@code JavaStatementBuilder.invokeMethod},
     *       {@code JavaConditionalExpression.collapseToSingleExpression})
     *       use it to union both channels across arguments / branches.</li>
     * </ul>
     * Every wrapper-handler emission that takes a compiled inner builder
     * and produces a new outer source MUST use this 4-arg form to carry
     * both channels through — this is the option F preservation invariant
     * documented in plan v6.3.
     */
    public static JavaExpression from(String code, JavaType type,
                                      Set<JavaClass<?>> refs,
                                      Set<JavaClass<?>> staticWildcardImports) {
        return new JavaExpression(type, refs, staticWildcardImports) {
            @Override
            public void render(StringBuilder sb) {
                sb.append(code);
            }
        };
    }

    /**
     * Wrap factory for {@code MapperS.of(inner)}. Sets {@link #unwrapToBuilder}
     * so <b>the structural-unwrap consumers</b> return the inner builder directly
     * (source = {@code inner.renderToString()}, refs = {@code inner.getRefs()} —
     * {@code MAPPER_S} dropped atomically with the wrap).
     *
     * <p>The reader set is NOT enumerated here, and the three methods this javadoc used to name
     * are stale as an enumeration (facet mapperWrapPrefix, v3.1 C2d retirement family 8,
     * PR #615): two of the string branches beside those readers were DELETED as dead code, and
     * the marker acquired three NEW structural readers in the same seat —
     * {@code CollectionHandler.compileEnumSwitchBlockLambda}'s case-arm splice gate,
     * {@code ControlFlowHandler}'s two mixed-arity ternary arm lifts, and
     * {@code CollectionHandler.wrapSingleArmMapperCOf}'s singleton-list lift. Treat every
     * {@code unwrapToBuilder()} read in the handler/renderer surface as a consumer; the marker is
     * the contract, the reader list is not.
     *
     * <p><b>Important:</b> callers MUST NOT further modify the returned builder
     * by appending {@code .map(...)} / {@code .evaluate(...)} via string
     * concatenation — chained forms must be constructed via
     * {@link #from(String, JavaType, Set)} so {@link #unwrapToBuilder} stays
     * {@link Optional#empty()}. That contract is now LOAD-BEARING beyond ref hygiene:
     * {@code wrapSingleArmMapperCOf} (S11) SPLICES the inner out of the render by substring on
     * the strength of the marker alone, so a chained form built by concatenation while keeping
     * the marker would be mis-cut. The seat carries a producer-contract check that throws rather
     * than mis-cutting, but the caller contract is what keeps it from ever being reached.
     */
    public static JavaExpression wrappedInMapperSOf(JavaExpression inner) {
        // Backward-compat overload — passes inner.getExpressionType() for the
        // wrapper's reported type. Callers that know the intended MapperS
        // wrapper JavaType (e.g. coercers with expectedType in scope) should
        // use {@link #wrappedInMapperSOf(JavaExpression, JavaType)} instead so
        // type-driven coercion in M7b-3+ sees the correct wrapper type rather
        // than the inner item type. (Copilot round-13 finding.)
        return wrappedInMapperSOf(inner, null);
    }

    /**
     * Wrap factory for {@code MapperS.of(...)} with an explicit wrapper
     * {@link JavaType}. Use this overload when the caller knows the intended
     * MapperS wrapper type — the returned expression's
     * {@link #getExpressionType()} reports {@code wrapperType} directly so
     * type-driven coercion sees MapperS&lt;T&gt; rather than the item type T.
     *
     * <p>When {@code wrapperType} is null, falls back to
     * {@code inner.getExpressionType()} for legacy compatibility; this
     * preserves behaviour for callers that don't yet have a wrapper type
     * available (e.g. LiteralHandler at M7b-1 where inner type is null).
     */
    public static JavaExpression wrappedInMapperSOf(JavaExpression inner, JavaType wrapperType) {
        Objects.requireNonNull(inner, "inner");
        Set<JavaClass<?>> refs = new HashSet<>(inner.getRefs());
        refs.add(com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper.MAPPER_S);
        Set<JavaClass<?>> staticWildcards = inner.getStaticWildcardImports();
        JavaType reportedType = wrapperType != null ? wrapperType : inner.getExpressionType();
        return new JavaExpression(reportedType, refs, staticWildcards,
                                  Optional.of(inner)) {
            @Override
            public void render(StringBuilder sb) {
                sb.append("MapperS.of(");
                inner.render(sb);
                sb.append(')');
            }
        };
    }

    /**
     * Wrap factory for {@code MapperC.of(...)}. Does NOT set
     * {@link #unwrapToBuilder} — verified by C3a.1 Step 3.1.1 grep that no
     * unwrap site strips MapperC. (The single-inner multi-cardinality wrap
     * {@link #wrappedInMapperCOfSingle} deliberately DOES mirror
     * {@link #wrappedInMapperSOf}'s structural-unwrap contract — see its javadoc;
     * the list-literal factories here keep the no-unwrap invariant.)
     */
    public static JavaExpression wrappedInMapperCOf(List<JavaExpression> inners) {
        // Backward-compat overload. Legacy callers (e.g. LiteralHandler list
        // literal at M7b-1) derive reported type from the first inner's
        // type — item type rather than MapperC<T> wrapper. Callers that
        // know the wrapper type should use the 2-arg overload below.
        // (Copilot round-13 finding.)
        return wrappedInMapperCOf(inners, null);
    }

    /**
     * Wrap factory for {@code MapperC.of(...)} with an explicit wrapper
     * {@link JavaType}. Use this overload when the caller knows the intended
     * MapperC wrapper type so the returned expression's
     * {@link #getExpressionType()} reports MapperC&lt;T&gt; rather than the
     * inner item type.
     *
     * <p>When {@code wrapperType} is null, falls back to deriving from the
     * first inner element's expression type (legacy behaviour).
     */
    public static JavaExpression wrappedInMapperCOf(List<JavaExpression> inners, JavaType wrapperType) {
        return wrappedInMapperCOf(inners, wrapperType, null);
    }

    /**
     * Wrap factory for {@code MapperC.of(...)} with an explicit wrapper type and
     * an optional generic type <em>witness</em>. When {@code witnessType} is
     * non-null the call is rendered {@code MapperC.<Item>of(...)} (the witness
     * simple name is {@code witnessType.getSimpleName()}) and the witness class is
     * added to {@code refs} so its import is collected ({@code ImportCollector}
     * filters {@code java.lang} / same-package). When {@code witnessType} is null
     * the bare {@code MapperC.of(...)} form is rendered (backward-compatible).
     *
     * <p>Mirrors upstream {@code ExpressionGenerator.caseListLiteral}
     * ({@code MapperC.<itemType>of(...)}): a Rune list literal always carries the
     * item-type witness. The bare {@code MapperC.of(...)} form (no witness) is
     * reserved for cardinality coercions ({@code ItemToWrapperCoercer} /
     * {@code WrapperToWrapperCoercer}), which keep their existing rendering.
     */
    public static JavaExpression wrappedInMapperCOf(List<JavaExpression> inners,
                                                    JavaType wrapperType,
                                                    JavaClass<?> witnessType) {
        Objects.requireNonNull(inners, "inners");
        Set<JavaClass<?>> refs = new HashSet<>();
        Set<JavaClass<?>> staticWildcards = new HashSet<>();
        for (JavaExpression e : inners) {
            refs.addAll(e.getRefs());
            staticWildcards.addAll(e.getStaticWildcardImports());
        }
        refs.add(com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper.MAPPER_C);
        if (witnessType != null) {
            refs.add(witnessType);
        }
        String prefix = witnessType != null
                ? "MapperC.<" + witnessType.getSimpleName() + ">of("
                : "MapperC.of(";
        JavaType reportedType = wrapperType != null
                ? wrapperType
                : (inners.isEmpty() ? null : inners.get(0).getExpressionType());
        return new JavaExpression(reportedType, refs, staticWildcards) {
            @Override
            public void render(StringBuilder sb) {
                sb.append(prefix);
                for (int i = 0; i < inners.size(); i++) {
                    if (i > 0) sb.append(", ");
                    inners.get(i).render(sb);
                }
                sb.append(')');
            }
        };
    }

    /**
     * Wrap factory for a SINGLE-inner witnessed {@code MapperC.<Item>of(inner)} — the
     * multi-cardinality counterpart of {@link #wrappedInMapperSOf(JavaExpression, JavaType)}
     * (facet multi_cardinality_value_wrap). A multi-cardinality value's Java form is a
     * {@code List<? extends Item>}, which upstream 9.83.0 coerces to the witnessed
     * {@code MapperC.<Item>of(...)} (item-to-MapperC cardinality coercion in
     * {@code ExpressionGenerator}/{@code TypeCoercionService}); the witness class is added
     * to {@code refs} so its import is collected, alongside {@code MapperC} itself.
     *
     * <p>Unlike the list-literal factory {@link #wrappedInMapperCOf(List, JavaType, JavaClass)}
     * (which intentionally does NOT set {@link #unwrapToBuilder}), this overload MIRRORS the
     * MapperS wrap's structural-unwrap contract — {@link #unwrapToBuilder()} returns the inner
     * expression — so the evaluate-arg / assignment / addAll strip sites unwrap a multi value
     * to its raw form exactly as they do for the MapperS wrap (e.g. a multi param passed into
     * another function's {@code evaluate(...)} renders raw, matching the golden).
     *
     * <p>Same caller contract as {@link #wrappedInMapperSOf(JavaExpression)}: do NOT append
     * chained forms via string concatenation on the returned builder — chained forms must go
     * through {@link #from(String, JavaType, Set)} so {@link #unwrapToBuilder} stays empty. See
     * that method's javadoc for why the contract is load-bearing at the S11 splice seat: this
     * factory carries the SAME marker over a {@code MapperC.<E>of(…)} render, so it is one of the
     * producers whose arrival at that seat the contract (and the seat's own producer-contract
     * check) has to keep well-formed.
     */
    public static JavaExpression wrappedInMapperCOfSingle(JavaExpression inner,
                                                          JavaType wrapperType,
                                                          JavaClass<?> witnessType) {
        return wrappedInMapperCOfSingle(inner, wrapperType, witnessType, null);
    }

    /**
     * FQN-witness overload (facet fqnWitnessMapperC — the {@code MapperC.<Item>of(...)}
     * render position of the {@code ImportingStringConcatenation} first-claim-wins
     * import-collision law, PR #194–#197's 4th/final position). When
     * {@code fqnWitnessRender} is non-null the witness renders FQN-inline
     * ({@code MapperC.<a.b.C>of(...)}) and the witness import is SUPPRESSED (the witness
     * class is NOT added to {@code refs}) — exactly the golden form when the witness
     * simple name collides with another import already claiming that simple name (e.g. the
     * fpml input item type {@code fpml.consolidated.shared.Party} colliding with the CDM
     * output {@code cdm.base.staticdata.party.Party}): the fork's bare-simple-name witness
     * adds a SECOND {@code import …Party;} → a duplicate-simple-name import that does NOT
     * compile, so every collision carrier is already waivered (green-safe by construction).
     * When {@code fqnWitnessRender} is null the bare {@code MapperC.<Party>of(...)} form
     * renders unchanged (witness import collected as before).
     *
     * <p>THE SENTINEL CASE (PR #607): the optimised route's alias-seam bridge passes an
     * UNRESOLVED {@code ImportCollisionResolver} sentinel here (the seam string's own
     * element spelling), which the file's first-claim-wins resolver later renders bare OR
     * FQN — so the render is not necessarily FQN, and the import suppression rests on the
     * invariant that the seam DECLARATION's own element reference registers the import when
     * the sentinel resolves bare. Both halves state it: {@code AliasValueSeamPolicy}'s
     * {@code witnessRender} and this parameter.
     */
    public static JavaExpression wrappedInMapperCOfSingle(JavaExpression inner,
                                                          JavaType wrapperType,
                                                          JavaClass<?> witnessType,
                                                          String fqnWitnessRender) {
        Objects.requireNonNull(inner, "inner");
        Objects.requireNonNull(witnessType, "witnessType");
        Set<JavaClass<?>> refs = new HashSet<>(inner.getRefs());
        refs.add(com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper.MAPPER_C);
        // FQN-inline collision render suppresses the witness import (the golden drops it,
        // keeping only the first-claim import of the colliding simple name).
        if (fqnWitnessRender == null) {
            refs.add(witnessType);
        }
        Set<JavaClass<?>> staticWildcards = inner.getStaticWildcardImports();
        JavaType reportedType = wrapperType != null ? wrapperType : inner.getExpressionType();
        return new MapperCOfSingleWrap(reportedType, refs, staticWildcards, inner, witnessType,
                fqnWitnessRender);
    }

    /**
     * The single-inner witnessed {@code MapperC.<Item>of(inner)} wrap returned by
     * {@link #wrappedInMapperCOfSingle(JavaExpression, JavaType, JavaClass)}. Named
     * (rather than anonymous) so a CONSUMPTION site can detect the wrap and select
     * upstream's witnessless re-render (facet tobuilder_output_assignment
     * mechanism 3): upstream decides the wrap shape from the consumer's EXPECTED
     * type — a multi value COLLAPSED to an item ({@code only-element}) takes the
     * witnessless List-to-item unwrap
     * ({@code TypeCoercionService.getListToItemConversionExpression}'s
     * {@code MapperC.of(«expression»).get()}), while only STAY-multi List-to-Mapper
     * coercions carry the {@code MapperC.<Item>of(...)} witness. The witnessed
     * render stays this class's default — every stay-multi producer/consumer is
     * byte-unchanged.
     */
    public static final class MapperCOfSingleWrap extends JavaExpression {
        private final JavaExpression inner;
        private final JavaClass<?> witnessType;
        /** Non-null when the witness renders FQN-inline (facet fqnWitnessMapperC). */
        private final String fqnWitnessRender;

        private MapperCOfSingleWrap(JavaType reportedType, Set<JavaClass<?>> refs,
                Set<JavaClass<?>> staticWildcards, JavaExpression inner,
                JavaClass<?> witnessType, String fqnWitnessRender) {
            super(reportedType, refs, staticWildcards, Optional.of(inner));
            this.inner = inner;
            this.witnessType = witnessType;
            this.fqnWitnessRender = fqnWitnessRender;
        }

        @Override
        public void render(StringBuilder sb) {
            String witness = fqnWitnessRender != null ? fqnWitnessRender : witnessType.getSimpleName();
            sb.append("MapperC.<").append(witness).append(">of(");
            inner.render(sb);
            sb.append(')');
        }

        /**
         * The witnessless re-render {@code MapperC.of(<inner>)} — upstream's
         * List-to-item collapse shape. Refs are the INNER refs plus
         * {@code MapperC} only: golden's witnessless form registers NO witness
         * import (the witness class ref is deliberately dropped), so a file whose
         * item type is otherwise unreferenced matches golden's import set. The
         * structural-unwrap contract ({@link #unwrapToBuilder()} returns the
         * inner) is preserved.
         */
        public JavaExpression witnesslessForm() {
            Set<JavaClass<?>> refs = new HashSet<>(inner.getRefs());
            refs.add(com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper.MAPPER_C);
            JavaExpression innerExpr = inner;
            return new JavaExpression(getExpressionType(), refs,
                    inner.getStaticWildcardImports(), Optional.of(inner)) {
                @Override
                public void render(StringBuilder sb) {
                    sb.append("MapperC.of(");
                    innerExpr.render(sb);
                    sb.append(')');
                }
            };
        }
    }

    /**
     * Identity-unwrap wrapper (facet ctor_set_rendering): renders exactly the inner
     * expression AND structurally unwraps to it ({@link #unwrapToBuilder()} returns
     * {@code inner}). For ITEM-typed emissions that are NOT Mappers — e.g. the
     * constructor typed-builder block {@code Type.builder()…\n….build()} — where the
     * strip sites' fall-through {@code .get()}/{@code .getMulti()} suffix would be
     * invalid Java (a builder has no Mapper unwrap). The assignment/evaluate-arg
     * unwrap sites structurally take {@code inner} and use the block verbatim,
     * matching upstream's POJO-typed {@code caseConstructorExpression} result.
     */
    public static JavaExpression selfUnwrapping(JavaExpression inner) {
        Objects.requireNonNull(inner, "inner");
        return new JavaExpression(inner.getExpressionType(), inner.getRefs(),
                inner.getStaticWildcardImports(), Optional.of(inner)) {
            @Override
            public void render(StringBuilder sb) {
                inner.render(sb);
            }
        };
    }

    /**
     * v3.2 seat 5 (PR #626, F6) round 1 (the code-quality review's SF-3): the KIND on the {@link #selfUnwrapping}
     * marker for the with-meta arm's HOISTED VARIABLE - the {@code withMetaArgument} sentinel the wrapped-argument
     * arm of {@code ConstructionHandler.buildTypedWithMeta} registers on the statement sink and returns as its
     * value. The SET seat's in-place unwrap ({@code FunctionExpressionRenderer.renderSetWithMetaValueUnwrapOrNull})
     * used to ask the RENDERED TEXT whether it was an identifier to learn this; it reads the class now. The
     * {@link #unwrapToBuilder()} contract is IDENTICAL to {@link #selfUnwrapping}'s (every structural-unwrap
     * consumer sees the same inner); a re-render through {@link #from} does NOT carry the witness - the seat reads
     * it from the compile result, exactly as {@link #bigIntegerLiteralValue}'s consumers do.
     */
    public static JavaExpression hoistedWithMetaVariable(JavaExpression inner, String variable) {
        Objects.requireNonNull(inner, "inner");
        return new HoistedWithMetaVariable(inner, variable);
    }

    /** The named subclass {@link #hoistedWithMetaVariable} returns - the witness IS the class. */
    public static final class HoistedWithMetaVariable extends JavaExpression {
        private final JavaExpression inner;
        private final String variable;

        private HoistedWithMetaVariable(JavaExpression inner, String variable) {
            super(inner.getExpressionType(), inner.getRefs(), inner.getStaticWildcardImports(), Optional.of(inner));
            this.inner = inner;
            this.variable = Objects.requireNonNull(variable, "variable");
        }

        /** The hoisted local's name - the session's {@code withMetaArgument} group token. */
        public String variable() {
            return variable;
        }

        @Override
        public void render(StringBuilder sb) {
            inner.render(sb);
        }
    }

    /**
     * facet collapseGetSuffix (v3.1 C2d retirement family 7 {@code collapse-get-suffix}, PR #614):
     * the KIND on the OVERLOADED {@link #selfUnwrapping} marker — an identity-unwrap wrapper whose
     * inner is the BARE ITEM-COLLAPSE render {@code <recv>.get()} (the {@code only-element} inline
     * collapse, {@code CollectionHandler.handle(RListOpExpr)}'s {@code ListOp.ONLY_ELEMENT} arm —
     * the sole producer of a transparent {@code .get()}-tailed expression in the generator).
     *
     * <p>Every other {@code selfUnwrapping} caller stays on the plain factory: the ten identity
     * sites are the constructor typed-builder blocks ({@code ConstructionHandler} ×3), the bare
     * hoist-sentinel splices ({@code ConstructionHandler}'s with-meta hoist, {@code
     * ControlFlowHandler}'s four {@code ifThenElseResult} locals) and the two
     * {@code getOrDefault(<ternary>)} joins ({@code SetOperationHandler}) — none of which renders a
     * {@code .get()} tail. Splitting the kind is what lets {@code HandlerHelper
     * .isSelfUnwrappingGetOperand} stop asking the RENDERED TEXT whether it ends {@code .get()}
     * (ledger family 7's {@code HandlerHelper} row): the PR #614 C2c census measured that
     * discriminator over 263,280 default-route / 186,279 IR-route / 125,929 optimised arrivals and
     * found exactly two transparent classes — 377 {@code .get()}-tailed (this kind) and SEVEN that
     * are not (the identity blocks the old javadoc called overloaded).
     *
     * <p>Mirrors the {@link #enumConstant} / {@link #multiExtracted} named-subclass contract: the
     * witness IS the class, and a re-render through {@link #from} does NOT carry it (nor the
     * structural unwrap it rides on). The {@link #unwrapToBuilder()} contract is IDENTICAL to
     * {@link #selfUnwrapping}'s — every existing structural-unwrap consumer sees the same inner.
     */
    public static JavaExpression selfUnwrappingBareCollapse(JavaExpression inner) {
        Objects.requireNonNull(inner, "inner");
        return new SelfUnwrappingBareCollapse(inner);
    }

    /**
     * The named subclass {@link #selfUnwrappingBareCollapse} returns — the witness IS the class, so
     * a consumer's {@code instanceof} (through {@code HandlerHelper.isSelfUnwrappingGetOperand})
     * plus the unchanged transparency compare is the whole read.
     */
    public static final class SelfUnwrappingBareCollapse extends JavaExpression {
        private final JavaExpression inner;

        private SelfUnwrappingBareCollapse(JavaExpression inner) {
            super(inner.getExpressionType(), inner.getRefs(), inner.getStaticWildcardImports(),
                    Optional.of(inner));
            this.inner = inner;
        }

        @Override
        public void render(StringBuilder sb) {
            inner.render(sb);
        }
    }

    /**
     * v3.2 seat 5 (PR #626, F3 - the beyond-long literal's conversion render form): the PRODUCER-stamped fact
     * that this expression is the VALUE consumption of a HOISTED beyond-long integer literal - the
     * {@code final BigInteger <sentinel> = new BigInteger("...");} declaration the literal seat
     * ({@code LiteralHandler.hoistBigIntegerLiteralOrNull}) registered on the statement sink (or the lambda
     * channel), rendered here as the lambda channel's Mapper ternary (seat 24's law F5). The STATEMENT seats
     * that assign or add the literal - upstream's {@code TypeCoercionService.convertNullSafe} completed as an
     * if/else statement ({@code FunctionExpressionRenderer.renderNumericConvertNullSafeOrNull}: the
     * conditional-arm, whole-output and add-singleton seats) - read the sentinel off this witness and render
     * the null-guarded CONVERSION over it, never the ternary text, which a statement seat could only complete
     * as the non-compiling {@code <ternary>.get()} (the chaos C4Gate rows, golden conv-bigint-statement).
     *
     * <p>Mirrors the {@link #enumConstant} / {@link #selfUnwrappingBareCollapse} contract: the witness IS
     * the class; a re-render through {@link #from} (the assignment unwrap's {@code .get()} append) does NOT
     * carry it - exactly right, the seats read it from the compile result before the unwrap. {@code refs}
     * are the ternary's (BigInteger, BigDecimal, MapperS); a statement seat that renders the conversion instead
     * of the ternary merges its OWN refs, not these.
     */
    public static JavaExpression bigIntegerLiteralValue(String code, Set<JavaClass<?>> refs, String sentinel) {
        return new BigIntegerLiteralValue(code, refs, sentinel);
    }

    /** The named subclass {@link #bigIntegerLiteralValue} returns - the witness IS the class. */
    public static final class BigIntegerLiteralValue extends JavaExpression {
        private final String code;
        private final String sentinel;

        private BigIntegerLiteralValue(String code, Set<JavaClass<?>> refs, String sentinel) {
            super(null, refs);
            this.code = Objects.requireNonNull(code, "code");
            this.sentinel = Objects.requireNonNull(sentinel, "sentinel");
        }

        /** The hoisted local's name - the session's {@code bigInteger} group token. */
        public String sentinel() {
            return sentinel;
        }

        @Override
        public void render(StringBuilder sb) {
            sb.append(code);
        }
    }

    /**
     * facet collapseGetSuffix (v3.1 C2d retirement family 7 {@code collapse-get-suffix}, PR #614):
     * the PRODUCER-stamped fact that this expression carries the EVALUATE-ARG pipeline's own
     * {@code .getMulti()} terminal — appended either by
     * {@code ReferenceHandler.unwrapForEvaluateArg}'s two terminal-accessor arms under
     * {@code asMulti} (the {@code .get()}-collapse re-wrap and the chained-Mapper fall-through) or
     * by one of the three sibling lift arms at the explicit-arg seat (the #345 collapsed-multi
     * re-wrap, the T2 multi-wrapper bridge lift, the T3 single-seam bridge lift) and the #191
     * closure-param collapse. The two elementwise-deref seats that must know the arg took the multi
     * collapse before they splice that suffix away then ask an {@code instanceof} instead of
     * reading the rendered text for {@code .getMulti()} (ledger family 7's two
     * {@code ReferenceHandler} render-shape rows).
     *
     * <p>DISTINCT from {@link #multiExtracted}, deliberately: that witness names the ADD unwrap's
     * own extraction ({@code FunctionExpressionRenderer.unwrapForAddAssignment}, PR #613) and its
     * consumers are the ADD rungs. Two pipelines, two facts — a shared class would make either
     * javadoc false and let one family's consumer read the other family's producer.
     *
     * <p>Mirrors the {@link #enumConstant} / {@link #multiExtracted} structural-witness contract:
     * the witness IS the class, and a re-render through {@link #from} does NOT carry it — which is
     * exactly right at the two seats that REPLACE the suffix (the meta-input null-guard form and
     * the elementwise splices rebuild through {@code from}, and their products no longer carry the
     * tail the fact names).
     *
     * <p>{@code refs} pass through VERBATIM from the source builder at every one of the six
     * producers — appending a terminal accessor references no new type — exactly as
     * {@link #enumConstant} requires its producers to carry the enum class through. A consumer that
     * merges this witness's refs therefore sees the source's set unchanged, and the ref bookkeeping
     * is identical to the {@link #from} form each producer used before the stamp.
     */
    public static JavaExpression evaluateArgMultiExtracted(String code, JavaType type,
            Set<JavaClass<?>> refs, Set<JavaClass<?>> staticWildcardImports) {
        return new EvaluateArgMultiExtracted(code, type, refs, staticWildcardImports);
    }

    /**
     * The named subclass {@link #evaluateArgMultiExtracted} returns — the witness IS the class, so
     * a consumer's {@code instanceof} is the whole read.
     */
    public static final class EvaluateArgMultiExtracted extends JavaExpression {
        private final String code;

        private EvaluateArgMultiExtracted(String code, JavaType type, Set<JavaClass<?>> refs,
                Set<JavaClass<?>> staticWildcardImports) {
            super(type, refs, staticWildcardImports);
            this.code = Objects.requireNonNull(code, "code");
        }

        @Override
        public void render(StringBuilder sb) {
            sb.append(code);
        }
    }

    /**
     * facet enumConstantWitness (v3.1 C2d retirement family 4 {@code dotted-enum-constant}, PR #611):
     * the PRODUCER-stamped fact that this expression renders a BARE Java enum constant
     * {@code EnumName.CONSTANT} — an item-typed constant, never a Mapper. Every emitter of that form
     * builds it through this factory: the resolved {@code Enum -> Value} reference and the
     * parser-bound bare value ({@code ReferenceHandler}), the fn-arg, ctor-setter and switch-arm
     * re-qualifications ({@code ReferenceHandler} / {@code ConstructionHandler} /
     * {@code FunctionExpressionRenderer}), the comparison seat's sibling-enum re-qualification
     * ({@code ComparisonHandler}) and the IR route's {@code IRJavaLeafEmitter.emitEnumValue}. Every
     * consumer that must know asks {@code HandlerHelper.isBareEnumConstant} — the {@code .get()}
     * suppression seats ({@code unwrapForEvaluateArg}, {@code unwrapForAssignment}, the ite-arm and
     * cond-list item ladders, the ctor-arg and with-meta item forms), the {@code MapperS.of} operand
     * wraps (comparison / contains / disjoint / to-string, the Mapper-typed ite-arm slot), the IR
     * route's operand wrap, and the two alias-retype hops that pass a witness through by identity
     * ({@code ComparisonHandler.retypeNullTypedAliasOperand}, {@code ConversionHandler}'s to-string
     * retype) — instead of reading the rendered text for a dotted, parenless shape (the
     * retired {@code HandlerHelper.isDottedEnumConstant}, four text conjuncts, plus the ite-arm seat's
     * inline twin: ledger family 4). The producer's decision and the consumer's read are then the SAME
     * object (LAW 69), on both routes (LAW 77).
     *
     * <p>Mirrors the {@link #wrappedInMapperSOf} / {@link #unwrapToBuilder()} structural-witness
     * contract. A re-render through {@link #from} does NOT carry the witness by design; the PR #611
     * C2c census (both routes, every cell of the 25-cell matrix; 391,168 default-route and 351,404
     * IR-route consumer arrivals) found no arrival whose witness had been lost that way and no
     * non-enum {@code X.Y} spelling at any seat — the text read and the witness, or at the comparison
     * seat that seat's own sibling-enum re-qualification (600 default-route / 399 IR-route arrivals),
     * agreed at every one.
     *
     * <p>{@code refs} must name the enum class the render references: the import travels with the
     * witness exactly as with any other emission, and a consumer that merges the witness's own refs
     * (the ctor-arg and assignment seats do) must not lose it.
     */
    public static JavaExpression enumConstant(String code, JavaType type, Set<JavaClass<?>> refs,
                                              Set<JavaClass<?>> staticWildcardImports) {
        return new EnumConstant(code, type, refs, staticWildcardImports);
    }

    /**
     * The named subclass {@link #enumConstant} returns — the witness IS the class, so a consumer's
     * {@code instanceof} (through {@code HandlerHelper.isBareEnumConstant}) is the whole read.
     */
    public static final class EnumConstant extends JavaExpression {
        private final String code;

        private EnumConstant(String code, JavaType type, Set<JavaClass<?>> refs,
                Set<JavaClass<?>> staticWildcardImports) {
            super(type, refs, staticWildcardImports);
            this.code = Objects.requireNonNull(code, "code");
        }

        @Override
        public void render(StringBuilder sb) {
            sb.append(code);
        }
    }

    /**
     * facet metaWrapperRecovery (v3.1 C2d retirement family 6, PR #613): the PRODUCER-stamped fact
     * that this expression is the ADD unwrap's own {@code .getMulti()} extraction — the
     * fall-through branch of {@code FunctionExpressionRenderer.unwrapForAddAssignment}, which
     * appends the suffix to a non-wrap Mapper. The ADD rungs that must know which producer put a
     * {@code .getMulti()} tail on their right-hand side then ask an {@code instanceof} instead of
     * reading the rendered text for the suffix.
     *
     * <p>Mirrors the {@link #enumConstant} / {@link #wrappedInMapperSOf} structural-witness
     * contract: the witness IS the class, and a re-render through {@link #from} does NOT carry it.
     */
    public static JavaExpression multiExtracted(String code, JavaType type, Set<JavaClass<?>> refs,
                                                Set<JavaClass<?>> staticWildcardImports) {
        return new MultiExtracted(code, type, refs, staticWildcardImports);
    }

    /**
     * The named subclass {@link #multiExtracted} returns — the witness IS the class, so a
     * consumer's {@code instanceof} is the whole read.
     */
    public static final class MultiExtracted extends JavaExpression {
        private final String code;

        private MultiExtracted(String code, JavaType type, Set<JavaClass<?>> refs,
                Set<JavaClass<?>> staticWildcardImports) {
            super(type, refs, staticWildcardImports);
            this.code = Objects.requireNonNull(code, "code");
        }

        @Override
        public void render(StringBuilder sb) {
            sb.append(code);
        }
    }

    /** Render this expression to a string. */
    public String renderToString() {
        var sb = new StringBuilder();
        render(sb);
        return sb.toString();
    }

    /** Render this expression to a StringBuilder. */
    public abstract void render(StringBuilder sb);

    @Override
    public JavaType getExpressionType() {
        return type;
    }

    @Override
    public Set<JavaClass<?>> getRefs() {
        return refs;
    }

    @Override
    public Set<JavaClass<?>> getStaticWildcardImports() {
        return staticWildcardImports;
    }

    /** Present iff this builder is an unwrap-compatible wrap; returns inner builder. */
    public Optional<JavaStatementBuilder> unwrapToBuilder() {
        return unwrapToBuilder;
    }

    @Override
    public JavaStatementBuilder mapExpression(
            Function<JavaExpression, ? extends JavaStatementBuilder> mapper) {
        return mapper.apply(this);
    }

    @Override
    public JavaStatementBuilder then(
            JavaStatementBuilder after,
            BiFunction<JavaExpression, JavaExpression, JavaStatementBuilder> combineExpressions,
            JavaStatementScope scope) {
        if (after instanceof JavaExpression afterExpr) {
            return combineExpressions.apply(this, afterExpr);
        }
        return after.then(this,
                (otherExpr, thisExpr) -> combineExpressions.apply(thisExpr, otherExpr),
                scope);
    }

    @Override
    public JavaStatement complete(Function<JavaExpression, JavaStatement> completer) {
        return completer.apply(this);
    }

    @Override
    public JavaReturnStatement completeAsReturn() {
        return new JavaReturnStatement(this);
    }

    @Override
    public JavaExpressionStatement completeAsExpressionStatement() {
        return new JavaExpressionStatement(this);
    }

    @Override
    public JavaAssignment completeAsAssignment(GeneratedIdentifier variableId) {
        return new JavaAssignment(variableId, this);
    }

    @Override
    public JavaStatementBuilder declareAsVariable(
            boolean isFinal, String variableId, JavaStatementScope scope) {
        GeneratedIdentifier id = scope.createIdentifier(this, variableId);
        return new JavaBlockBuilder(
                JavaStatementList.of(
                        new JavaLocalVariableDeclarationStatement(isFinal, this.type, id, this)),
                new JavaVariable(id, this.type)
        );
    }

    @Override
    public JavaStatementBuilder collapseToSingleExpression(JavaStatementScope scope) {
        return this;
    }

    @Override
    public JavaLambdaBody toLambdaBody() {
        return this;
    }

    @Override
    public String toString() {
        return renderToString();
    }
}
