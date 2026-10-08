package com.regnosys.rosetta.generator.java.expression.handlers;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.ConversionKind;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair;
import com.regnosys.rosetta.ast.expressions.unary.RConversionExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.unary.RToStringExpr;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.generator.GeneratedIdentifier;
import com.regnosys.rosetta.generator.java.JavaNamingUtil;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.statement.JavaLocalVariableDeclarationStatement;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.rosetta.util.types.JavaClass;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles code generation for conversion expressions:
 * {@link RConversionExpr} (to-number, to-int, to-time, to-enum, to-date,
 * to-date-time, to-zoned-date-time) and {@link RToStringExpr} (to-string).
 *
 * <p>Golden output patterns (verified against CDM golden files):
 * <pre>
 *   to-number         →  arg.checkedMap("to-number", BigDecimal::new, NumberFormatException.class)
 *   to-int            →  arg.checkedMap("to-int", Integer::parseInt, NumberFormatException.class)
 *   to-time           →  arg.checkedMap("to-time", s -> LocalTime.parse(s, DateTimeFormatter.ISO_LOCAL_TIME), DateTimeParseException.class)
 *   to-enum (string)  →  arg.checkedMap("to-enum", EnumType::fromDisplayName, IllegalArgumentException.class)
 *   to-enum (enum)    →  arg.checkedMap("to-enum", e -> EnumType.valueOf(e.name()), IllegalArgumentException.class)
 *   to-enum (FieldWithMeta string leaf)
 *                     →  arg.&lt;String&gt;map("Type coercion", fieldWithMetaString -> ...getValue()).checkedMap("to-enum", EnumType::fromDisplayName, ...)
 *                        (facet ingest_setter_value_form arm C5 — see enumArgMetaUnwrapStep)
 *   to-date           →  arg.checkedMap("to-date", Date::parse, DateTimeParseException.class)
 *   to-date-time      →  arg.checkedMap("to-date-time", LocalDateTime::parse, DateTimeParseException.class)
 *   to-zoned-date-time→  arg.checkedMap("to-zoned-date-time", ZonedDateTime::parse, DateTimeParseException.class)
 *   to-string (string)→  arg.map("to-string", Object::toString)
 *   to-string (enum)  →  arg.map("to-string", EnumType::toDisplayString)
 * </pre>
 *
 * <p>The generated code chains a {@code .checkedMap()} or {@code .map()} call
 * onto the compiled argument expression.
 */
public class ConversionHandler {

    // =========================================================================
    // Conversion expressions (to-number, to-int, to-time, to-enum, etc.)
    // =========================================================================

    /**
     * Compiles a conversion expression by chaining a {@code .checkedMap()} call
     * onto the compiled argument.
     *
     * @param expr     the conversion expression node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of the argument
     * @return a {@link JavaExpression} rendering the chained conversion call
     */
    public JavaStatementBuilder handle(RConversionExpr expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // PR-A §9.1 C3a.4.c (D2 α + D7 η): per-arm structured refs for every
        // non-java.lang class textually present in the emitted checkedMap
        // chain. ConversionHandler is the sole feeder for these imports
        // post-C3c.2 (the regex path that used to resolve them was deleted
        // atomically). D7 η-confirmed (2026-04-21): DateTimeFormatter +
        // DateTimeParseException were never in the deleted BUILTIN_TYPE_FQN
        // table — without structured refs they would be silently dropped on
        // every to-time/to-date/to-date-time/to-zoned-date-time emission.
        JavaStatementBuilder argBuilder =
                compiler.compile(expr.argument(), ctx.expectedType(), ctx.scope());
        String arg = HandlerHelper.render(argBuilder);

        // facet checkedMapFqn (PR #196): when a to-enum TARGET enum's Java simple
        // name collides with the conversion SOURCE enum (a DIFFERENT canonical
        // name — e.g. fpml.consolidated.fpmlenum.PeriodEnum -> cdm.base.datetime.
        // PeriodEnum), the target is rendered FULLY-QUALIFIED inline and its import
        // is SUPPRESSED — upstream ImportingStringConcatenation first-claim-wins
        // (the SOURCE enum, imported as the witness/input-param type, claims the
        // bare simple name first; the target loses). Non-null = the target FQN to
        // render; null = no collision (bare simple name + import, the prior path).
        // The local sibling of PR #195's fpmlInputFqn input-vs-output collision.
        String enumCollisionFqn = enumTargetCollisionFqn(expr, compiler);

        // The checkedMap description is the rune operator keyword itself
        // (upstream ExpressionGenerator emits "«expr.operator»"), which is
        // hyphenated for every conversion: to-number / to-int / to-time /
        // to-enum / to-date / to-date-time / to-zoned-date-time. The fork
        // previously emitted camelCase labels ("toNumber", …) for all kinds;
        // only to-enum (961) + to-int (2) appear in the 9.83.0 goldens (both
        // hyphenated, none camelCase), so the hyphenated form cannot regress a
        // passing cell. (The to-string operator lives in handle(RToStringExpr).)
        String chainedCall = switch (expr.kind()) {
            case NUMBER ->
                    ".checkedMap(\"to-number\", BigDecimal::new, NumberFormatException.class)";
            case INT ->
                    ".checkedMap(\"to-int\", Integer::parseInt, NumberFormatException.class)";
            case TIME -> {
                // facet toTimeLambdaEscape (PR #419): upstream names the parse lambda's
                // param via lambdaScope.createUniqueIdentifier("s") (vendored
                // ExpressionGenerator.xtend L1176-1182), so a taken `s` (e.g. a function
                // input named s) escapes to `_s` — oracle golden expr-conversions-valid
                // `_s -> LocalTime.parse(_s, …)`; the fork's hardcoded `s` SHADOWED the
                // method param (javac error). The deferred lambda-param channel (facet
                // lambdaNaming #329 — underscore-escape resolves at finalization against
                // the completed parent chain; the #382 alias-body sort-key precedent
                // proves the channel runs for alias bodies) replaces the literal.
                // Corpus-neutral by ABSENCE: zero to-time emissions in the 9.83.0
                // goldens (the #196 census: only to-enum 961 + to-int 2). A null scope
                // declines to the literal `s` (today's bytes).
                String timeParam = ctx.scope() != null
                        ? ctx.scope().registerDeferredLambdaParam("s")
                        : "s";
                yield ".checkedMap(\"to-time\", " + timeParam + " -> LocalTime.parse(" + timeParam
                        + ", DateTimeFormatter.ISO_LOCAL_TIME), DateTimeParseException.class)";
            }
            case ENUM -> {
                // The target enum is rendered by its SIMPLE name (+ import), unless
                // it collides with the source enum (facet checkedMapFqn) — then by
                // its FQN inline (no import). Golden uses the hyphenated operator
                // keyword "to-enum" as the checkedMap description (verified across
                // the 9.83.0 corpus: all 5 cells emit "to-enum", none "toEnum").
                // The conversion function depends on the SOURCE type — an enum
                // source re-maps by Java constant name (e -> T.valueOf(e.name())); a
                // string source parses the display name (T::fromDisplayName). See
                // isEnumSource for how the source type is resolved.
                // facet checkedMapTargetSimpleName (PR #215): off-collision, the target
                // enum renders by its Java SIMPLE name (+ import, registered below) —
                // NOT expr.targetEnumName(), which echoes the rosetta-written reference
                // verbatim and so emits a FULLY-QUALIFIED inline name when the source
                // wrote `to-enum some.pkg.TargetEnum` (e.g. `schemeName to-enum
                // iso20022.auth030.hkma.dtcc.HKTRPartyScheme`). Upstream always emits the
                // simple name and lets the import (already collected here) resolve it;
                // the collision arm above keeps the FQN where golden genuinely needs it.
                // facet toEnumClaimsSentinel (PR #348): ON-collision, the target renders
                // as a #227 first-claim-wins SENTINEL instead of the #196 eager
                // source-wins FQN — the CLAIMS TABLE decides. Golden
                // MapReturnSwapLegToSettlementTerms claims the cdm SettlementTypeEnum
                // FIRST (the ite-hoist decl's #327 sentinel), so the fpml witness FQNs
                // and the to-enum target renders BARE — the eager rule had the winner
                // backwards at exactly this seat while rendering the same enum bare two
                // lines above (a claims inconsistency inside one file). In every
                // green-FQN file the SOURCE claims first (an input-param seed or an
                // earlier witness sentinel — Agent A: 126/126 FQN goldens carry a real
                // different-canonical collision), so the sentinel resolves FQN + the
                // resolver suppresses the import: byte-identical. Off-collision keeps
                // the bare-simple-name path untouched.
                String enumType = enumCollisionFqn != null
                        ? com.regnosys.rosetta.generator.java.template.ImportCollisionResolver
                                .typeRef(enumCollisionFqn)
                        : targetEnumSimpleName(expr, compiler);
                String conversionFn =
                        enumConversionFunction(enumType, isEnumSource(expr.argument(), compiler));
                yield ".checkedMap(\"to-enum\", " + conversionFn + ", IllegalArgumentException.class)";
            }
            case DATE ->
                    ".checkedMap(\"to-date\", Date::parse, DateTimeParseException.class)";
            case DATE_TIME ->
                    ".checkedMap(\"to-date-time\", LocalDateTime::parse, DateTimeParseException.class)";
            case ZONED_DATE_TIME ->
                    ".checkedMap(\"to-zoned-date-time\", ZonedDateTime::parse, DateTimeParseException.class)";
        };

        // Per-arm ref set. Java-lang types (Integer, NumberFormatException,
        // IllegalArgumentException, Object) need no import.
        Set<JavaClass<?>> refs = new HashSet<>();
        switch (expr.kind()) {
            case NUMBER -> refs.add(HandlerHelper.BIG_DECIMAL);
            case TIME -> {
                refs.add(HandlerHelper.LOCAL_TIME);
                refs.add(HandlerHelper.DATE_TIME_FORMATTER);
                refs.add(HandlerHelper.DATE_TIME_PARSE_EXCEPTION);
            }
            case DATE -> {
                refs.add(HandlerHelper.ROSETTA_DATE);
                refs.add(HandlerHelper.DATE_TIME_PARSE_EXCEPTION);
            }
            case DATE_TIME -> {
                refs.add(HandlerHelper.LOCAL_DATE_TIME);
                refs.add(HandlerHelper.DATE_TIME_PARSE_EXCEPTION);
            }
            case ZONED_DATE_TIME -> {
                refs.add(HandlerHelper.ZONED_DATE_TIME);
                refs.add(HandlerHelper.DATE_TIME_PARSE_EXCEPTION);
            }
            case ENUM -> {
                // The TARGET enum appears by SIMPLE name in BOTH conversion
                // forms (T::fromDisplayName and e -> T.valueOf(e.name())), so
                // its type must be importable from this emission — collecting
                // it here covers the file whose target enum is referenced ONLY
                // inside the conversion (no ambient output/attribute-type
                // import exists; the pre-fix emission did not even compile
                // there). Refs are de-duplicated downstream and the
                // ImportCollector skips same-package imports, so adding it is
                // a no-op when an ambient import is already present (the same
                // REnumTypeRef → JavaClass route as ReferenceHandler's
                // enumImportRefs, PR #130). Skipped when the resolver or
                // translator is absent (unresolved target / stateless unit
                // compiler) — the rendering string is unaffected either way.
                // facet checkedMapFqn (PR #196): suppress the target enum's import
                // when it is rendered FQN-inline on a source-name collision (the
                // source enum already imports the simple name; a second import of
                // the differently-packaged target would be a duplicate = a Java
                // compile error — exactly the pre-fix bug). Off-collision, register
                // it as before (it appears by simple name in both conversion forms).
                // facet toEnumClaimsSentinel (PR #348): the ref registers ON-collision
                // too — the resolver's suppressedCanonicals removes it when the
                // sentinel LOSES (today's no-import), and keeps it when the target is
                // the claim winner (golden MapReturnSwapLegToSettlementTerms keeps
                // cdm.product.common.settlement.SettlementTypeEnum imported).
                var translator = compiler.getTypeTranslator();
                if (translator != null) {
                    expr.targetEnum().ifPresent(en ->
                            refs.add(translator.toJavaReferenceType(new REnumTypeRef(en))));
                }
            }
            case INT -> { /* java.lang only — no library refs */ }
        }
        refs.addAll(argBuilder.getRefs());

        // Preserve staticWildcardImports from the argument builder — a
        // nested comparison / existence inside a conversion argument
        // carries EXPRESSION_OPERATORS_NULL_SAFE that must round-trip
        // through this wrapper. Part of the refs + staticWildcardImports
        // preservation invariant documented in plan v6.3 architecture
        // (Option F: wrapper handlers must propagate BOTH channels from
        // their operand builders).
        // facet getCollapsedMetaDeref (PR #314): a to-enum whose FIELD_WITH_META-string source is a
        // `.get()`-COLLAPSED MULTI chain (an `…mapC(…).<FieldWithMetaString>map(…).get()` bare
        // wrapper) cannot take the inline C5 `.<String>map("Type coercion", …)` step — `.map` is a
        // Mapper method, invalid on the bare item. golden block-converts the enclosing map lambda:
        // hoist `final FieldWithMetaString <name> = <chain>.get();` + null-guard-reconstruct
        // `(<name> == null ? MapperS.<String>ofNull() : MapperS.of(<name>.getValue()))` before the
        // `.checkedMap`. The fork spliced the bare-wrapper `.map` BARE (non-compiling → already a
        // waivered mismatch → green-safe by construction).
        JavaExpression collapsedDeref =
                getCollapsedMetaDerefOrNull(expr, arg, argBuilder, ctx, compiler);
        if (collapsedDeref != null) {
            Set<JavaClass<?>> outRefs = new HashSet<>(refs);
            outRefs.addAll(collapsedDeref.getRefs());
            return JavaExpression.from(
                    collapsedDeref.renderToString() + chainedCall,
                    null,
                    outRefs,
                    argBuilder.getStaticWildcardImports());
        }
        // facet fnCallMetaSourceHoist (PR #367): the FUNCTION-path STATEMENT-SINK
        // sibling of the #314 lambda arm above — a to-enum whose source is a DIRECT
        // function call returning a FIELD_WITH_META string hoists the evaluate result
        // to a `final FieldWithMetaString <name> = <fn>.evaluate(…);` statement (the
        // #331 string-registered sink hoist) and null-guard-reconstructs the
        // MapperS<String> before the checkedMap (the #317 guarded rewrap): golden
        // cdm6 MapSettlementProvisionToSettlementTerms `.setSettlementRateOptionValue(
        // (fieldWithMetaString == null ? MapperS.<String>ofNull() : MapperS.of(
        // fieldWithMetaString.getValue())).checkedMap("to-enum", …).get())`. The
        // fork's `MapperS.of(<evaluate>).checkedMap(String-fn…)` checkedMaps the
        // WRAPPER with a String function — non-compiling, so every carrier is a
        // waivered mismatch (green-safe by construction). Declines to the inline
        // render below when the source is not a single meta-string fn call or no
        // statement sink is reachable (a lambda-interior seat stops the walk — the
        // #314 lambda channel owns those).
        JavaExpression fnCallHoisted =
                fnCallMetaSourceHoistOrNull(expr, argBuilder, ctx, compiler);
        if (fnCallHoisted != null) {
            Set<JavaClass<?>> outRefs = new HashSet<>(refs);
            outRefs.addAll(fnCallHoisted.getRefs());
            return JavaExpression.from(
                    fnCallHoisted.renderToString() + chainedCall,
                    null,
                    outRefs,
                    argBuilder.getStaticWildcardImports());
        }
        // facet collapsedOnlyElementConversionRewrap (PR #371, F-A2): a conversion whose
        // argument is the bare-item ONLY_ELEMENT collapse (`then distinct only-element
        // to-enum X` — ONLY_ELEMENT over the implicit item, optionally through DISTINCT)
        // renders `distinct(<prev>).get()` — a BARE item that `.checkedMap` (a Mapper
        // method) cannot chain on. Golden re-wraps the collapse before the chain:
        // `MapperS.of(distinct(thenArg2).get()).checkedMap("to-enum", …)` (fca
        // LoadTypeRule) — the #260 collapse re-wrap law at the conversion-receiver seat.
        // Same guards as the F-A1 decl-seat arm; a bare-item receiver never compiled, so
        // green files cannot carry the unwrapped form.
        // facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614): the `.get()` suffix test
        // is DELETED outright here — the row's by-construction claim ("isBareCollapseConversionArg
        // already names the two AST collapse shapes whose render IS `<prev>.get()`") is now
        // MEASURED, not asserted. The c7b census at this seat: 9,587 / 9,587 / 9,139 arrivals
        // (default-route, IR-route, optimised), text=true at 10, and text <=> isBareCollapseConversionArg <=> `ONLY_ELEMENT AND typeNull`, EXACT
        // BOTH WAYS on all three walks. The sibling AST predicate is therefore the whole read
        // (LAW 69 — the seat consults the predicate that already stood beside it rather than a
        // second copy of the family arbiter).
        // facet mapperWrapPrefix (v3.1 C2d retirement family 8, PR #615) — VERDICT-MOVED
        // RETIRE-AFTER-CENSUS -> JUSTIFIED-KEPT: the surviving conjunct is a LAW-74 idempotence
        // belt with no typed channel and a measured zero, the #340-belt disposition of PR #614
        // applied here. c8 census, 28,313 arrivals (9,587 / 9,587 / 9,139 — default-route D11,
        // IR-route D11, optimised): inside this seat's OWN AST gate the belt is TRUE at 30/30 and
        // NEVER declines, and the seat's own 305 emission never re-arrives (`prod=none` at
        // 28,313/28,313), so the second pass it guards does not occur at this corpus. Both
        // candidate channels are refuted: the bare marker agrees at only 17.23% (4,878/28,313),
        // because 23,405 conversion arguments already render `MapperS.of(…)` from RAW-STRING
        // producers carrying no marker — 23,269 of them single-line and untyped, plus 124
        // multi-line untyped and 12 single-line TYPED; 23,269 is the largest slice, NOT the class,
        // and the class is what the agreement percentage is computed against. And — decisively —
        // at ALL 30 arrivals where this seat actually FIRES the marker
        // is PRESENT and transparent (kind `JavaExpression$SelfUnwrappingBareCollapse`, the render
        // the bare `<prev>.get()`), so the channel is INVERTED at 100% of the firing population.
        if (isBareCollapseConversionArg(expr.argument())
                && !arg.startsWith("MapperS.of(")) {
            arg = "MapperS.of(" + arg + ")";
            refs.add(HandlerHelper.MAPPER_S);
        }
        return JavaExpression.from(
                arg + enumArgMetaUnwrapStep(expr, ctx, compiler) + chainedCall,
                null,
                refs,
                argBuilder.getStaticWildcardImports());
    }

    /**
     * facet collapsedOnlyElementConversionRewrap (PR #371, F-A2): ONLY_ELEMENT over the
     * implicit item — bare ({@code item only-element}) or through DISTINCT
     * ({@code distinct only-element}) — the two collapse shapes whose render is the
     * type-less {@code <prev>.get()} / {@code distinct(<prev>).get()} bare item
     * (mirrors {@code CollectionHandler.isBareItemOnlyElementLocal} + its DISTINCT
     * sibling). Nav-rooted collapses stay declined at this seat (no conversion-seat
     * carrier — missed-flip-at-worst).
     */
    private static boolean isBareCollapseConversionArg(RExpression argument) {
        if (!(argument instanceof RListOpExpr op) || op.op() != ListOp.ONLY_ELEMENT) {
            return false;
        }
        RExpression inner = op.argument();
        if (inner instanceof RListOpExpr distinctOp && distinctOp.op() == ListOp.DISTINCT) {
            inner = distinctOp.argument();
        }
        return inner instanceof RImplicitVariable;
    }

    /**
     * facet fnCallMetaSourceHoist (PR #367): the statement-sink hoist + guarded
     * rewrap for a {@code to-enum} whose source is a DIRECT function call with a
     * SINGLE {@code FIELD_WITH_META string} output — see the call-site comment
     * for the law. Returns {@code null} (the caller keeps the inline render)
     * unless EVERY gate passes: ENUM kind, an args-carrying {@link RSymbolReference}
     * source resolving to an {@link com.regnosys.rosetta.ast.functions.RFunction}
     * whose output is a single FIELD_WITH_META {@code string}, a reachable
     * statement-hoist sink (lambda-boundary-stopped — the #314 lambda channel is
     * disjoint by construction), a resolvable wrapper, and a structurally
     * unwrappable compiled call (the bare {@code evaluate} for the decl value).
     */
    private static JavaExpression fnCallMetaSourceHoistOrNull(RConversionExpr expr,
            JavaStatementBuilder argBuilder, ExpressionContext ctx, ExpressionCompiler compiler) {
        if (expr.kind() != ConversionKind.ENUM || ctx == null || ctx.scope() == null) {
            return null;
        }
        if (!(expr.argument() instanceof RSymbolReference call) || call.args().isEmpty()) {
            return null;
        }
        var callee = call.symbol()
                .filter(com.regnosys.rosetta.ast.functions.RFunction.class::isInstance)
                .map(com.regnosys.rosetta.ast.functions.RFunction.class::cast)
                .orElse(null);
        RAttribute out = callee == null ? null : callee.output().orElse(null);
        if (out == null
                || MetaFieldGenerator.detectMetaKind(out) != MetaFieldGenerator.MetaKind.FIELD_WITH_META
                || out.typeCall() == null
                || !"string".equals(out.typeCall().typeName())) {
            return null;
        }
        var gm = compiler.getGeneratorModel();
        if (gm == null || gm.isMulti(out)) {
            return null;
        }
        var sink = ctx.scope().findStatementHoistSink();
        if (sink == null) {
            return null;
        }
        RJavaWithMetaValue wrapper = NavigationHandler.metaWrapperOf(out, compiler);
        if (wrapper == null) {
            return null;
        }
        String baseName = JavaNamingUtil.toFirstLower(wrapper.getSimpleName());
        if (!javax.lang.model.SourceVersion.isIdentifier(baseName)) {
            return null;
        }
        // The decl value is the BARE evaluate — strip the fn-call compile's
        // MapperS.of contract wrap structurally (a non-contract shape declines).
        if (!(argBuilder instanceof JavaExpression argExpr)
                || argExpr.unwrapToBuilder().isEmpty()) {
            return null;
        }
        String bareCall = HandlerHelper.render(argExpr.unwrapToBuilder().get());
        GeneratedIdentifier id = ctx.scope().createUniqueIdentifier(baseName);
        String nameToken = ctx.scope().registerDeferredCoercionName(id);
        sink.registerStatementHoist(
                "final " + wrapper.getSimpleName() + " " + nameToken + " = " + bareCall + ";");
        Set<JavaClass<?>> outRefs = new HashSet<>(argBuilder.getRefs());
        outRefs.add(wrapper);
        outRefs.add(HandlerHelper.MAPPER_S);
        return JavaExpression.from(
                "(" + nameToken + " == null ? MapperS.<String>ofNull() : MapperS.of("
                        + nameToken + ".getValue()))",
                null,
                outRefs,
                argBuilder.getStaticWildcardImports());
    }

    /**
     * facet getCollapsedMetaDeref (PR #314): the BLOCK-CONVERSION variant of {@link #enumArgMetaUnwrapStep}
     * (arm C5) for a {@code to-enum} whose FIELD_WITH_META-{@code string} source is a {@code .get()}
     * -COLLAPSED bare wrapper. The inline C5 step {@code .<String>map("Type coercion", …)} is a
     * {@code Mapper} method and does not compile on the bare {@code FieldWithMetaString} the
     * {@code .get()} yields (the source is a MULTI chain — an {@code …mapC(…)} — collapsed to a
     * single item). Upstream (TypeCoercionService convertNullSafe) hoists the bare wrapper to a local
     * and null-guard-RECONSTRUCTS a {@code MapperS<String>}:
     *
     * <pre>
     * final FieldWithMetaString &lt;name&gt; = &lt;chain&gt;.get();
     * … return (&lt;name&gt; == null ? MapperS.&lt;String&gt;ofNull()
     *                           : MapperS.of(&lt;name&gt;.getValue())).checkedMap("to-enum", …);
     * </pre>
     *
     * <p>The hoist registers on the enclosing map/extract lambda's scope
     * ({@code registerPendingLambdaHoist}) so {@code CollectionHandler.compileLambda} block-converts
     * the {@code mapSingleToItem} expression lambda (the LAMBDA_CHANNEL, sharing the #170/#237/#309
     * deferred-coercion naming so the decl name and the null-guard references agree). Carrier: drr
     * {@code BookingLocationRule} ({@code getCountry} on a MULTI {@code getAddress} chain
     * {@code to-enum ISOCountryCodeEnum}).
     *
     * <p>Declines (→ the inline C5 form, today's still-waivered non-compiling bytes → green-safe by
     * construction) when: the kind is not ENUM; the leaf is not a FIELD_WITH_META {@code string}
     * attribute (the C5 gate); the source is NOT an only-element collapse (a plain {@code MapperS}
     * source takes the compiling inline C5 step); the enclosing rule is absent (RULE-scoped →
     * FUNCTION-byte-neutral, #232); the source is not directly inside a drainable map/extract lambda
     * (the hoist would orphan); a statement-hoist sink IS reachable (a top-level rule ctor, not a
     * lambda); or the wrapper / derived name is unresolved.
     */
    private static JavaExpression getCollapsedMetaDerefOrNull(RConversionExpr expr, String arg,
            JavaStatementBuilder argBuilder, ExpressionContext ctx, ExpressionCompiler compiler) {
        if (expr.kind() != ConversionKind.ENUM || ctx == null || ctx.scope() == null) {
            return null;
        }
        // The C5 gate: a FIELD_WITH_META string leaf (the value type is String → the <String> witness
        // and the MapperS<String> reconstruction below are exact).
        RAttribute leaf = conversionLeafAttribute(expr.argument(), compiler);
        if (leaf == null
                || MetaFieldGenerator.detectMetaKind(leaf) != MetaFieldGenerator.MetaKind.FIELD_WITH_META
                || leaf.typeCall() == null
                || !"string".equals(leaf.typeCall().typeName())) {
            return null;
        }
        // The source must be a `.get()`-COLLAPSED bare wrapper — a plain MapperS source takes the
        // inline C5 step (which compiles), only a collapsed MULTI chain needs the hoist.
        // facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614): read through the family's
        // shared arbiter. The c7b census at this seat: 50 / 50 / 42 arrivals (default-route,
        // IR-route, optimised), text=true at 18
        // (BookingLocationRule x9 + TraderLocationRule x9 — the row's own carrier), and
        // `ONLY_ELEMENT AND typeNull` selects exactly those 18 on all three walks. The row's named
        // channel is REFUTED: `CardinalityComputer.compute(expr.argument()) == MULTI` — the doc's
        // own "a MULTI chain collapsed to a single item" — answers SINGLE at EVERY fire (the global
        // path's conservative then-pipe verdict), and the AST bare-item/list-literal shapes are
        // false at every one.
        if (!HandlerHelper.bareOnlyElementCollapse(expr.argument(), argBuilder)) {
            return null;
        }
        // RULE-scoped → the FUNCTION tail is byte-frozen (#232); the shared ConversionHandler seat is
        // reachable from both paths.
        if (HandlerHelper.findEnclosingRule(expr.argument()) == null) {
            return null;
        }
        // The hoist must land in a DRAINABLE map/extract lambda (compileLambda drains it) or it
        // orphans; a top-level rule ctor value HAS a statement-hoist sink and is not this shape.
        if (!HandlerHelper.isInsideDrainableMapLambda(expr.argument())
                || ctx.scope().findStatementHoistSink() != null) {
            return null;
        }
        RJavaWithMetaValue wrapper = NavigationHandler.metaWrapperOf(leaf, compiler);
        if (wrapper == null) {
            return null;
        }
        String baseName = JavaNamingUtil.toFirstLower(wrapper.getSimpleName());
        if (!javax.lang.model.SourceVersion.isIdentifier(baseName)) {
            return null;
        }
        GeneratedIdentifier id = ctx.scope().createUniqueIdentifier(baseName);
        String nameToken = ctx.scope().registerDeferredCoercionName(id);
        Set<JavaClass<?>> refs = new HashSet<>(argBuilder.getRefs());
        refs.add(wrapper);
        refs.add(HandlerHelper.MAPPER_S);
        // The hoisted decl value is the whole `.get()`-collapsed source (the same bare-item collapse
        // the inline form would consume). Its declared type (wrapper) import must be contributed by
        // the initializer's refs — JavaLocalVariableDeclarationStatement#getRefs ignores the declared
        // type — so pass a wrapper-enriched ref set (the #237/#301 pattern; #314 Copilot R1). Byte-
        // neutral: the returned expression's refs already carry the wrapper, but the hoist decl now
        // contributes it self-sufficiently even when the collapsed source omits the <Wrapper> witness.
        Set<JavaClass<?>> declRefs = new HashSet<>(argBuilder.getRefs());
        declRefs.add(wrapper);
        JavaExpression declValue = JavaExpression.from(arg, wrapper,
                declRefs, argBuilder.getStaticWildcardImports());
        ctx.scope().registerPendingLambdaHoist(
                new JavaLocalVariableDeclarationStatement(true, wrapper, id, declValue));
        // The value type is String (the string-leaf gate), so the <String> witness + MapperS<String>
        // reconstruction are exact (matching the inline C5 step's own <String> witness).
        return JavaExpression.from(
                "(" + nameToken + " == null ? MapperS.<String>ofNull() : MapperS.of("
                        + nameToken + ".getValue()))",
                null, refs, argBuilder.getStaticWildcardImports());
    }

    /**
     * facet {@code ingest_setter_value_form} (arm C5): a {@code to-enum}
     * conversion whose source navigation LEAF is a FIELD_WITH_META-annotated
     * basic-{@code string} attribute is compiled by upstream against the
     * meta-STRIPPED expected type, materialising the guarded unwrap step
     * {@code .<String>map("Type coercion", fieldWithMetaString ->
     * fieldWithMetaString == null ? null : fieldWithMetaString.getValue())}
     * between the source and {@code .checkedMap("to-enum", ...)}
     * (TypeCoercionService getMapperSItemConversionExpression + convertNullSafe;
     * golden cdm6 GetCashCurrency). Pre-fix the raw {@code FieldWithMetaString}
     * fed {@code CurrencyCodeEnum::fromDisplayName} (a {@code String} param) —
     * non-compiling, so no green file carries the firing shape. Returns the
     * empty string — today's render — for every other shape: non-ENUM kinds,
     * REFERENCE_WITH_META and non-string value types have no corpus carrier;
     * the lambda param rides the scope disambiguation exactly as the
     * record-feature nav var does (single occurrence in the sole carrier —
     * unnumbered, matching golden). No new refs: the {@code String} witness is
     * java.lang and the wrapper class appears only lowercased in the param name.
     */
    private static String enumArgMetaUnwrapStep(RConversionExpr expr, ExpressionContext ctx,
            ExpressionCompiler compiler) {
        if (expr.kind() != ConversionKind.ENUM) {
            return "";
        }
        RAttribute leaf = conversionLeafAttribute(expr.argument(), compiler);
        if (leaf == null
                || MetaFieldGenerator.detectMetaKind(leaf)
                        != MetaFieldGenerator.MetaKind.FIELD_WITH_META
                || leaf.typeCall() == null
                || !"string".equals(leaf.typeCall().typeName())) {
            // facet toEnumBoundItemDeref (PR #367): the IMPLICIT-item source — a bare
            // `then extract to-enum <E>` pipes the previous level's element, so the
            // meta-string proof rides the enclosing BOUND thenArg ref (render truth —
            // the #362 compiled-type stamp law): walk the enclosing inline fns
            // outward and read the FIRST bound ref; a Mapper of FieldWithMetaString
            // (value String) fires the SAME C5 step (golden asic
            // CountryOfCounterparty2Rule `item.<String>map("Type coercion",
            // fieldWithMetaString -> … .getValue()).checkedMap("to-enum", …)`). An
            // unbound walk, an unstamped ref, or a non-meta/non-string element keeps
            // today's bytes (empty step).
            if (expr.argument() instanceof RImplicitVariable
                    && ctx.scope() != null && compiler.getTypeUtil() != null) {
                var tu = compiler.getTypeUtil();
                RNode cur = expr.argument().parent();
                for (int i = 0; cur != null && i < 24; i++, cur = cur.parent()) {
                    if (!(cur instanceof com.regnosys.rosetta.ast.expressions.supporting
                            .RInlineFunction fn)) {
                        continue;
                    }
                    // facet thenBindingOverlay (v3.1 C2d retirement family 9, PR #616) —
                    // VERDICT-MOVED RETIRE-AFTER-CENSUS -> JUSTIFIED-KEPT (the seat-30 bar; S28):
                    // BOTH proposed channels are POPULATED AND WRONG at 100% of the firing
                    // population — a stronger refutation than an unpopulated channel. c9 census,
                    // 28,494 arrivals (9,642 / 9,642 / 9,210) = 28,161 entry + 333 walk, the seat
                    // reached at 333 and FIRING at 27. CHANNEL 1, the workspace: missing=false at
                    // 28,494/28,494 so it always has an answer, but hasMeta=false and metas=[] at
                    // 28,494/28,494 — including at ALL 27 arrivals whose bound item IS a
                    // FieldWithMetaString. CHANNEL 2, detectMetaKind(leaf): NONE at 25,355 and
                    // noLeaf at 3,139, never a meta kind. Locator drift corrected: the triage's
                    // L514-519 is live at 545-550 and L520-529 at 551-560.
                    JavaExpression bound = ctx.scope().thenArgRefFor(fn);
                    if (bound == null) {
                        continue;
                    }
                    if (bound.getExpressionType() != null
                            && tu.getItemType(bound.getExpressionType())
                                    instanceof RJavaWithMetaValue itemMeta
                            && tu.STRING.equals(itemMeta.getValueType())) {
                        String boundParam = ctx.scope().disambiguate("fieldWithMetaString");
                        return ".<String>map(\"Type coercion\", " + boundParam + " -> "
                                + boundParam + " == null ? null : " + boundParam
                                + ".getValue())";
                    }
                    break;
                }
            }
            return "";
        }
        String param = ctx.scope().disambiguate("fieldWithMetaString");
        return ".<String>map(\"Type coercion\", " + param + " -> "
                + param + " == null ? null : " + param + ".getValue())";
    }

    /**
     * The resolved attribute at the leaf of a conversion argument's navigation,
     * looking through the parameterless list-op collapses ({@code first}/
     * {@code last}/{@code only-element}/...) that keep the leaf's item type.
     * Alias-rooted chains carry an EMPTY {@code resolvedFeature} (the known IR
     * gap), so the leaf falls back to the gm-aware
     * {@link NavigationHandler#fallbackResolveFeature} — the SAME resolution
     * the rendered {@code <FieldWithMetaString>} witness on the very same line
     * already uses, so the gate and the witness cannot disagree.
     * {@code null} for every other argument shape (→ arm C5 declines).
     */
    private static RAttribute conversionLeafAttribute(RExpression argument,
            ExpressionCompiler compiler) {
        return conversionLeafAttribute(argument, compiler, 0);
    }

    private static RAttribute conversionLeafAttribute(RExpression argument,
            ExpressionCompiler compiler, int depth) {
        RExpression cur = argument;
        while (cur instanceof RListOpExpr listOp) {
            cur = listOp.argument();
        }
        if (cur instanceof RFeatureCall fc) {
            return fc.resolvedFeature()
                    .orElseGet(() -> NavigationHandler.fallbackResolveFeature(fc, compiler));
        }
        // A single-step nav off an alias (`cashId -> identifier`) parses as the
        // DISGUISED REnumValueRef (the grammar's `EnumName -> valueName`
        // ambiguity, facet numeric_literal_typing lesson) — resolve through the
        // same shared disguise resolution the sibling handlers use.
        if (cur instanceof REnumValueRef evr) {
            RAttribute attr = NavigationHandler.resolveDisguisedFeature(evr, compiler, new HashSet<>());
            if (attr != null) {
                return attr;
            }
            // facet implicitItemEnumSource (PR #225): a 2-name disguised nav
            // (`periodicPayment -> fixedRateDayCountConvention`) whose ROOT is a feature
            // on the enclosing lambda's implicit ITEM element type — the shape the shared
            // disguise resolver (function inputs/outputs/aliases) does not reach.
            return NavigationHandler.implicitItemDisguisedLeaf(evr, compiler);
        }
        // facet implicitItemEnumSource (PR #225): a BARE symbol source
        // (`spreadCurrency to-string`) — a single-name item feature parses as an
        // unresolved RSymbolReference (not in function scope) — resolve on the
        // enclosing lambda's implicit ITEM element type. A genuine function-input enum
        // source resolves earlier (inferred-type map / siblingComparand), so this arm
        // only ever recovers an item feature; enum-ness is filtered downstream.
        if (cur instanceof RSymbolReference sym) {
            return NavigationHandler.implicitItemSymbolLeaf(sym, compiler);
        }
        // facet toEnumImplicitItemLeaf (PR #359, F-2): a BARE implicit-item source
        // (`then extract to-enum ISOCountryCodeEnum` — the conversion argument is the
        // extract's elided item) resolves its leaf through the SAME owner walk the
        // sibling item consumers use (implicitItemArgument): the item ranges over the
        // owning extract/then ARGUMENT, whose own leaf IS the source attribute
        // (CountryOfCounterparty2Rule drr: the previous step's `country`
        // FieldWithMetaString — golden derefs before the checkedMap exactly like the
        // nav-sourced C5 form; fromDisplayName takes String, so the un-deref'd form
        // never compiled). Three hops for the then-of-extract shape (extract argument =
        // the item again → the then argument = the previous chain → its last step
        // body); depth-bounded belt-and-suspenders per the #253 cycle law.
        if (cur instanceof RImplicitVariable && depth < 8) {
            RExpression ownerArg = NavigationHandler.implicitItemArgument(cur);
            if (ownerArg != null) {
                return conversionLeafAttribute(ownerArg, compiler, depth + 1);
            }
        }
        // The leaf of a then-CHAIN is the leaf of its LAST step's body (the item the
        // downstream conversion consumes ranges over that step's result — the same
        // last-step read the deep-then consumer applies).
        if (cur instanceof com.regnosys.rosetta.ast.expressions.binary.RThenExpr thenChain
                && depth < 8) {
            RExpression lastBody = thenChain.body()
                    .map(com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction::body)
                    .orElse(null);
            if (lastBody != null) {
                return conversionLeafAttribute(lastBody, compiler, depth + 1);
            }
        }
        // The leaf of an EXTRACT step (`then extract <chain>` — the step body is an
        // RExtractExpr wrapping its own inline) is the leaf of the extract's body.
        if (cur instanceof com.regnosys.rosetta.ast.expressions.unary.RExtractExpr extractStep
                && depth < 8) {
            RExpression exBody = extractStep.body() == null ? null : extractStep.body().body();
            if (exBody != null) {
                return conversionLeafAttribute(exBody, compiler, depth + 1);
            }
        }
        return null;
    }

    /**
     * Determines whether a {@code to-enum} conversion's source expression is an
     * enumeration (vs. a string), delegating the direct type test to
     * {@link #isEnumSourceType(RMetaAnnotatedType)}.
     *
     * <p>Two-step resolution. First the workspace's inferred-type map (the same
     * map {@code RuleGenerator} reads to back-fill rule output types) — it types
     * a directly-declared symbol reference ({@code enumInput to-enum T}). But it
     * is MISSING for (almost) every function-body NAVIGATION source: a
     * single-step nav ({@code drrReport -> eventType}) parses as a disguised
     * {@code REnumValueRef} (the grammar's {@code EnumName -> valueName}
     * ambiguity) and a chained nav as an {@code RFeatureCall}, neither of which
     * the inference engine types on this path — so every navigation source fell
     * to the string form where the upstream 9.83.0 golden, whose type system
     * types the same source as its enum, emits the {@code valueOf(e.name())}
     * re-map. The second step therefore resolves the navigation's LEAF enum via
     * {@link NavigationHandler#leafEnumeration} — the SAME best-effort
     * receiver-chain resolution the {@code <Type>} witness emitted on the very
     * same line already uses (PR #154), so the source-type decision and the
     * rendered witness derive from one resolved attribute and cannot disagree.
     *
     * <p>Returns {@code false} — the string-source form — when neither step
     * resolves an enum: a string-typed source, a non-navigation expression, an
     * unresolvable chain, or the stateless zero-arg compiler.
     */
    private static boolean isEnumSource(RExpression argument, ExpressionCompiler compiler) {
        if (argument == null) {
            return false;
        }
        if (compiler.getGeneratorModel() != null && isEnumSourceType(
                compiler.getGeneratorModel().workspace().getInferredType(argument))) {
            return true;
        }
        if (NavigationHandler.leafEnumeration(argument, compiler) != null) {
            return true;
        }
        // facet checkedMapEnumSource (PR #224): a BARE alias / function-call source
        // (`lvl(drrReport) to-enum ModificationLevel1Code` — `lvl` a function/alias
        // whose result is the source enum) parses as a bare {@link RSymbolReference}
        // that no navigation route resolves; {@link NavigationHandler#siblingComparandEnumeration}
        // recovers the enum from the alias body / the callee OUTPUT type (the same
        // resolver the bare-enum comparison operand uses). Green-safe: a function/alias
        // returning a string/non-enum yields null → the string form is preserved.
        if (NavigationHandler.siblingComparandEnumeration(argument, compiler) != null) {
            return true;
        }
        // facet checkedMapEnumSource (PR #224): a navigation source whose root is an
        // ALIAS / FUNCTION call (`optionSettlementModel -> settlementType` where the
        // alias body wraps `fpmlOptionSettlementModel(...)`) parses as a DISGUISED
        // REnumValueRef whose enum {@link NavigationHandler#leafEnumeration} cannot
        // recover — its REnumValueRef arm uses the bare 1-arg disguise resolver that
        // does not walk an alias/function root. The conversion-scoped
        // {@link #conversionLeafAttribute} resolves the SAME leaf the meta-unwrap arm
        // C5 gate + the rendered `<Type>` witness use (gm-aware
        // {@code resolveDisguisedFeature} + the {@code first}/`only-element` collapse
        // strip), so the source-type decision and the witness cannot disagree. A
        // genuine string source resolves to a basic/alias leaf (→ no enum → the
        // string `::fromDisplayName` form is preserved; green-safe).
        return conversionLeafEnumeration(argument, compiler) != null;
    }

    /**
     * facet checkedMapEnumSource (PR #224): the source {@link REnumeration} of a
     * conversion ({@code to-enum} / {@code to-string}) recovered through the
     * conversion-scoped {@link #conversionLeafAttribute} — the gm-aware leaf
     * resolution (alias/function-rooted disguised {@link REnumValueRef} +
     * parameterless list-op collapse) that {@link NavigationHandler#leafEnumeration}
     * does not reach for an alias/function root. Returns {@code null} when the
     * argument is not a recognised navigation, the leaf does not resolve, or the
     * leaf type is not a (direct) enumeration — a string/basic/alias leaf keeps the
     * generic {@code ::fromDisplayName} / {@code Object::toString} form, so the
     * recovery only ever turns an already-waivered enum-source mismatch golden-exact.
     * Deliberately NOT folded into {@link NavigationHandler#leafEnumeration}: that
     * method is shared by the bare-enum comparison path
     * ({@code siblingComparandEnumeration} / {@code tryBareEnumComparand}), and
     * widening it there would change that green population; keeping the gm-aware
     * disguised resolution conversion-scoped holds the blast radius to the
     * conversion seats.
     */
    private static REnumeration conversionLeafEnumeration(RExpression argument,
            ExpressionCompiler compiler) {
        RAttribute leaf = conversionLeafAttribute(argument, compiler);
        if (leaf == null || leaf.typeCall() == null) {
            return null;
        }
        return leaf.typeCall().referencedType()
                .filter(REnumeration.class::isInstance)
                .map(REnumeration.class::cast)
                .orElse(null);
    }

    /**
     * True when a {@code to-enum} source type is an enumeration (→ re-map by
     * Java constant name) rather than a string (→ parse the display name).
     * Returns {@code false} for a {@code null} or {@code MISSING} type (the
     * safe string-source default).
     *
     * <p>Package-private so the decision is unit-tested directly; the
     * workspace-read plumbing in {@link #isEnumSource} is locked end-to-end by
     * the D11 corpus byte-oracle (a fully-wired {@code ExpressionCompiler} unit
     * test is impractical — {@code RWorkspace} is {@code final} and the
     * inference engine needs a parsed model).
     */
    static boolean isEnumSourceType(RMetaAnnotatedType inferred) {
        return inferred != null && !inferred.isMissing()
                && inferred.type() instanceof REnumTypeRef;
    }

    /**
     * The {@code to-enum} conversion function passed to {@code checkedMap}: an
     * enum source re-maps by Java constant name
     * ({@code e -> Target.valueOf(e.name())}); a string source parses the
     * display name ({@code Target::fromDisplayName}). Package-private for unit
     * tests.
     */
    static String enumConversionFunction(String enumType, boolean enumSource) {
        return enumSource
                ? "e -> " + enumType + ".valueOf(e.name())"
                : enumType + "::fromDisplayName";
    }

    /**
     * facet checkedMapFqn (PR #196): the canonical (dotted) name to render a
     * {@code to-enum} TARGET enum inline when its Java simple name collides with
     * the conversion SOURCE enum (a DIFFERENT canonical name —
     * {@code fpml.consolidated.fpmlenum.PeriodEnum} as the source/witness vs
     * {@code cdm.base.datetime.PeriodEnum} as the target); else {@code null}
     * (render the bare simple name + register its import — the prior path).
     *
     * <p>Upstream {@code ImportingStringConcatenation} first-claim-wins: the
     * SOURCE enum is imported as the navigation {@code <Type>} witness / the input
     * parameter type, so it claims the simple name; the target loses and renders
     * fully-qualified with no import. The fork otherwise imported BOTH (a duplicate
     * same-simple-name import = a Java compile error, so every such file was
     * already non-compiling/waivered — green-safe by construction). The source enum
     * resolves through the SAME routes the to-string source uses
     * ({@link #sourceEnumeration}). Returns {@code null} for a non-enum conversion
     * (no target enum), a string source (no source enum), the stateless unit
     * compiler (no translator), differing simple names, or a same-canonical-name
     * self-conversion (not a collision — the MANDATORY FQN-difference gate, the
     * sibling of PR #195's {@code resolveInputParamForOutputCollision}).
     */
    static String enumTargetCollisionFqn(RConversionExpr expr, ExpressionCompiler compiler) {
        var translator = compiler.getTypeTranslator();
        if (translator == null) {
            return null;
        }
        var targetOpt = expr.targetEnum();
        if (targetOpt.isEmpty()) {
            return null;
        }
        REnumeration source = sourceEnumeration(expr.argument(), compiler);
        if (source == null) {
            return null;
        }
        JavaClass<?> targetJC = translator.toJavaReferenceType(new REnumTypeRef(targetOpt.get()));
        JavaClass<?> sourceJC = translator.toJavaReferenceType(new REnumTypeRef(source));
        String targetFqn = targetJC.getCanonicalName().withDots();
        if (targetJC.getSimpleName().equals(sourceJC.getSimpleName())
                && !targetFqn.equals(sourceJC.getCanonicalName().withDots())) {
            return targetFqn;
        }
        return null;
    }

    /**
     * facet checkedMapTargetSimpleName (PR #215): the to-enum TARGET enum's Java SIMPLE name (the
     * import is registered alongside), resolved through the translator from the resolved target
     * {@link REnumeration}. Replaces {@link RConversionExpr#targetEnumName()} in the off-collision
     * render arm — {@code targetEnumName()} echoes the rosetta-written reference verbatim, so a source
     * that wrote a QUALIFIED name (e.g. {@code to-enum iso20022.auth030.hkma.dtcc.HKTRPartyScheme})
     * leaked a fully-qualified inline reference where upstream emits the simple name + import.
     * Falls back to {@code targetEnumName()} when the translator or resolved target is absent (the
     * stateless unit compiler / an unresolved target), so the rendering is unchanged there.
     */
    static String targetEnumSimpleName(RConversionExpr expr, ExpressionCompiler compiler) {
        var translator = compiler.getTypeTranslator();
        if (translator != null && expr.targetEnum().isPresent()) {
            JavaClass<?> jc = translator.toJavaReferenceType(new REnumTypeRef(expr.targetEnum().get()));
            if (jc != null) {
                return jc.getSimpleName();
            }
        }
        return expr.targetEnumName().orElse("Object");
    }

    // =========================================================================
    // To-string expression
    // =========================================================================

    /**
     * Compiles a to-string expression by chaining a {@code .map()} call
     * onto the compiled argument.
     *
     * <p>Output pattern (facet tostring_enum_source — upstream
     * {@code ExpressionGenerator.caseToStringOperation} labels the map with the
     * OPERATOR KEYWORD and selects the mapping function from the SOURCE type):
     * {@code arg.map("to-string", SourceEnum::toDisplayString)} for an enum source,
     * {@code arg.map("to-string", Object::toString)} otherwise. The 9.83.0 goldens
     * carry ZERO camelCase {@code "toString"} labels across all 5 cells, so no green
     * file can carry the pre-facet emission — the label + function fix is
     * green-file-safe by corpus law.
     *
     * <p>The source enum resolves through the SAME routes the {@code to-enum}
     * source-type test uses ({@link #isEnumSource}: the workspace inferred-type map,
     * then {@link NavigationHandler#leafEnumeration} for navigation sources), PLUS
     * the implicit-item route ({@link NavigationHandler#implicitItemEnumeration})
     * for a bare {@code item} source inside a filter/extract lambda
     * ({@code financialSector extract [item to-string]}). The resolved enum's import
     * ref registers exactly as the {@code to-enum} arm's target ref does
     * ({@code REnumTypeRef} → JavaClass, dedup'd downstream; rendering is unaffected
     * when the translator is absent — the stateless unit compiler).
     *
     * @param expr     the to-string expression node
     * @param ctx      the current expression compilation context
     * @param compiler the parent compiler for recursive compilation of the argument
     * @return a {@link JavaExpression} rendering the chained to-string call
     */
    public JavaStatementBuilder handle(RToStringExpr expr, ExpressionContext ctx, ExpressionCompiler compiler) {
        // Both refs AND staticWildcardImports from the argument builder must
        // round-trip through this wrapper so nested expressions' static
        // wildcards survive (refs + staticWildcardImports preservation
        // invariant — see plan v6.3 Option F architecture).
        JavaStatementBuilder argBuilder =
                compiler.compile(expr.argument(), ctx.expectedType(), ctx.scope());
        // facet tostring_operand_meta_unwrap: upstream caseToStringOperation
        // (ExpressionGenerator L1164-74) compiles its argument against
        // MAPPER_S.wrapExtendsWithoutMeta(expr.argument) — a meta-typed operand
        // gains the Type-coercion unwrap BEFORE the to-string map (guarded iff the
        // chain rides MapperS, bare iff MapperC — the #170 law, decided by the
        // wrapper kind the coercer sees; all corpus to-string carriers are MapperS
        // item chains, so the emitted form is the guarded ternary). The shared
        // coerceNavigationReceiver lever no-ops on every non-meta / untyped
        // operand — the byte-flat guarantee for the green to-string population
        // (facet tostring_enum_source, 36 flips at #165).
        // facet arithOperandWrapperCoerce sibling (PR #334): an ALIAS-call argument
        // compiles null-typed, so the meta unwrap below never saw it — re-type from
        // the FunctionAliasHelper signature walk (the arm-A1 channel; the retype
        // alone changes no bytes, and the coercion still no-ops on non-meta items),
        // activating the guarded unwrap for a meta-typed alias source (drr upi
        // FloatingRateIndex's `floatingRateIndex(interestRatePayout)`).
        // facet enumConstantWitness (PR #611, the code review's MF-1): the retype re-creates the
        // expression from its text and would drop the producer's enum-constant witness the wrap
        // below reads — a witnessed constant is null-typed like an alias call, so guard it here.
        if (argBuilder.getExpressionType() == null && !HandlerHelper.isBareEnumConstant(argBuilder)) {
            var aliasType = NavigationHandler.tryAliasReceiverMapperType(expr.argument(), compiler);
            if (aliasType != null) {
                argBuilder = NavigationHandler.retypeBuilder(argBuilder, aliasType);
            }
        }
        // facet toStringCollapsedMetaRetype (PR #387): a to-string whose argument is a
        // SINGLE-collapsing list-op (`… -> floatingRateIndex first to-string`) compiles
        // type-less — the collapse erases the terminal's meta wrapper (the #333 fused-
        // collapse class), so the L883 unwrap below never saw it and the wrapper reached
        // `.map("to-string", …)` un-deref'd (non-compiling: toDisplayString/toString on
        // the wrapper — already waivered, green-safe by construction). Recover the
        // MapperS<FieldWithMetaX> via the #237 terminal walker (the SAME recovery the
        // evaluate-arg + #263 then-output seats use) and retype; the shared
        // coerceNavigationReceiver then emits the guarded deref BEFORE the to-string map
        // (MapperS kind — golden esma/fca IndicatorOfTheUnderlyingIndex
        // `.first().<FloatingRateIndexEnum>map("Type coercion", …)`). Scoped to the
        // collapse-op argument + a NON-collapsed Mapper-form value (a collapsed bare
        // item is the #361 metaValueDerefHoist family's seat below — disjoint by the
        // shape guard); a non-meta terminal recovers null, bytes unchanged.
        // facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614): that disjointness guard
        // is the family's shared arbiter now, not the render's suffix. The c7b census at this seat:
        // 17,493 / 17,493 / 14,308 arrivals (default-route, IR-route, optimised),
        // `.get()`-tailed at 149, and `ONLY_ELEMENT AND typeNull` selects
        // exactly those 149 on all three walks — while the row's OWN named channel is REFUTED:
        // isBareItemOnlyElementLocal / listLiteralCollapseArgument answer true at only 26 of the
        // 149 (the other 123 — InterestRateLeg1/2ReturnSwap x45 each, Create_ContractType15__1 x18,
        // SortIdentifiers x15 — collapse a DEEPER receiver, not the bare-item shape). Typing the
        // gate also removes a HandlerHelper.render call that THROWS on a block builder.
        if (argBuilder.getExpressionType() == null
                && expr.argument() instanceof RListOpExpr toStrCollapse
                && (toStrCollapse.op() == ListOp.FIRST || toStrCollapse.op() == ListOp.LAST
                        || toStrCollapse.op() == ListOp.ONLY_ELEMENT)
                && !HandlerHelper.bareOnlyElementCollapse(expr.argument(), argBuilder)) {
            com.rosetta.util.types.JavaType termMeta = NavigationHandler.tryTerminalMetaMapperType(
                    expr.argument(), compiler);
            if (termMeta != null) {
                argBuilder = NavigationHandler.retypeBuilder(argBuilder, termMeta);
            }
        }
        argBuilder = compiler.coerceNavigationReceiver(argBuilder, ctx.scope());
        // v3.2 seat 2 (Law 2, the chaos C5Deep rows): a primitive-typed argument (`… count
        // to-string` — the count's `int`) lifts to the MapperS upstream's caseToStringOperation
        // expects, so the `.map("to-string", …)` lands on a Mapper, not on a bare int (the F16
        // javac census: `int cannot be dereferenced` ×13). HandlerHelper.liftPrimitiveOperand —
        // the ONE lift the comparison seat also reads (LAW 69).
        argBuilder = HandlerHelper.liftPrimitiveOperand(argBuilder, compiler, ctx.scope());
        String arg = HandlerHelper.render(argBuilder);
        Set<JavaClass<?>> refs = new HashSet<>(argBuilder.getRefs());
        // facet enumConstantToStringWrap (PR #244): a to-string whose SOURCE renders
        // as a bare enum constant (EnumType.VALUE, by the producer's witness — a literal enum value, e.g.
        // the rosetta `iso.ActionTypeEnum -> VALU to-string`) has no `.map()` method,
        // so the fork's `EnumType.VALUE.map("to-string", ...)` does not compile and the
        // carrier is already waivered. Upstream caseToStringOperation compiles the
        // argument against MAPPER_S.wrapExtendsWithoutMeta, so the item-typed constant
        // coerces to MapperS.of(EnumType.VALUE). The SHARED HandlerHelper.wrapEnumOperand
        // applies the same MapperS.of(...) wrap a comparison / set-operation bare-enum
        // operand uses (gated on the producer's enum-constant witness,
        // HandlerHelper.isBareEnumConstant — PR #611; registers MAPPER_S). A navigation /
        // chained-Mapper source and the bound implicit item carry no witness, so both
        // decline — no double-wrap. Green-safe by corpus law: golden never
        // leaves a bare enum constant before .map("to-string") (the bare form does not
        // compile), so the rewrite only ever turns an already-waivered mismatch golden-exact.
        arg = HandlerHelper.wrapEnumOperand(argBuilder, refs);
        // facet ruleMetaDerefConsumerWrap (PR #267, A2 — to-string bare-invocation operand): a
        // to-string whose ARGUMENT is a bare no-args FUNCTION invocation (e.g. the rosetta
        // `Extract_BondConnect to-string` — a bare func ref renders UNWRAPPED via
        // ReferenceHandler.renderImplicitFunctionInvocation, `extract_BondConnect.evaluate(<in>)`)
        // has no `.map` method, so the fork's `<fn>.evaluate(<in>).map("to-string", …)` does not
        // compile and the carrier is already waivered. Upstream caseToStringOperation compiles the
        // argument against MAPPER_S.wrapExtendsWithoutMeta, so the item-typed invocation coerces to
        // MapperS.of(<fn>.evaluate(<in>)). The SHARED HandlerHelper.wrapBareInvocationOperand (the
        // #256 single-source-of-truth, keyed on a no-args RSymbolReference whose symbol is an
        // RFunction) applies the same MapperS.of wrap a comparison / exists operand uses. A
        // navigation / chained-Mapper / bare-RULE source (a rule already wraps) declines, so no
        // double-wrap. Green-safe by construction: golden never leaves a bare function invocation
        // before .map("to-string") (the bare form does not compile), so the rewrite only ever turns
        // an already-waivered (NON_COMPILING) mismatch golden-exact (raising functional parity too).
        arg = HandlerHelper.wrapBareInvocationOperand(arg, expr.argument(), refs);
        // facet toStringOverCollapse (PR #356): a to-string whose ARGUMENT renders as a
        // COLLAPSED chain (a `.get()`-terminal bare item — the enclosing single-consumption
        // collapse of a mapC navigation) has no `.map` method, so the fork's
        // `<chain>.get().map("to-string", …)` does not compile and the carrier is already
        // waivered. Upstream compiles the argument against MAPPER_S.wrapExtendsWithoutMeta,
        // so the item-typed collapse coerces to `MapperS.of(<chain>.get())` (golden
        // hkma-dtcc/tr Create_ContractType15__1 `.setCd(…)`). The earlier enum-constant /
        // bare-invocation wraps produce `MapperS.of(…)` forms that never end `.get()` — no
        // double-wrap. Green-safe by construction: a bare item before `.map("to-string", …)`
        // never compiles, so no green file carries the unwrapped form. Evidence-scoped
        // THREE ways to the verified carriers (the P356C vector): (1) SINGLE-LINE collapses
        // only — a MULTI-LINE collapsed chain golden structures differently (the cp8
        // NameOfTheUnderlyingIndex sideways-FLAT catch); (2) the CTOR-SETTER value seat
        // only (parent RKeyValuePair — both carriers' `.setCd(…)`) — list-literal-element
        // and equality-operand to-string collapses golden shapes differently (the cp9/cp10
        // SortIdentifiers / InterestRateLeg1ReturnSwap sideways-FLAT catch); (3) a NON-META
        // terminal — a `<FieldWithMetaString>map(…).get()` collapse golden HOISTS +
        // null-guard-derefs to the value instead (the meta-deref family's seat, not this
        // wrap's). Every decline keeps today's bytes.
        // facet metaValueDerefHoist (PR #361): a to-string whose argument COLLAPSES
        // to a META-WRAPPER item (`<chain|alias-collapse>.get()` where the element
        // is FieldWithMetaX — exactly the family the #356 gate-3 decline below
        // reserved for "the meta-deref family's seat") hoists the collapsed wrapper
        // to a type-named local and re-presents the VALUE guarded (golden csa
        // InterestRateLeg1/2ReturnSwap: `final FieldWithMetaFloatingRateIndexEnum
        // fieldWithMetaFloatingRateIndexEnum0 = floatingRateIndexContractualProduct(
        // product).get();` consumed as `(x == null ? MapperS.<FloatingRateIndexEnum>
        // ofNull() : MapperS.of(x.getValue())).map("to-string",
        // FloatingRateIndexEnum::toDisplayString)` — at the renderConditionalAssignment
        // condition seats the block relocates into the owning branch via the #173
        // caller-collected channel, and the alias-literal seats drain at the
        // alias-sink method top, both for free). The wrapper resolves through the
        // #264 recovery walker PLUS a RECOVERY-LOCAL element-preserving alias-body
        // descent (collapseMetaWrapper — the #360 (a3) discipline: the shared walker
        // stays untouched). The hoist name-group is the lowercased wrapper simple
        // name (the #237 fieldWithMetaString convention, method-spanning numbering).
        // Green-safe by construction: `.get().map("to-string", …)` on a bare item
        // never compiles, so every converted seat lived in a waivered file; declines
        // without a reachable sink (rule/POJO emission) — bytes unchanged, and the
        // re-presented arg no longer ends `.get()`, so the #356 ctor-setter wrap
        // below stays naturally disjoint.
        // facet toStringCollapsedMetaRetype (PR #387): the join-bare REPLAY window
        // admits MULTI-LINE collapses too — golden hoists the two-chain list-literal
        // collapse VERBATIM into the in-rung decl (drr common NameOfTheUnderlyingIndex
        // `final FieldWithMetaString fieldWithMetaString3 = MapperC.<…>of(<a>\n.first(),
        // <b>\n.first()).get();`). The #356 single-line gate stands everywhere else —
        // it was scoped BECAUSE this very file moved sideways when the multi-line admit
        // fired WITHOUT the in-rung hoist structure (the cp8-356 catch).
        boolean toStrReplayWindow = ctx.scope() != null
                && ctx.scope().isLadderJoinBareArmReplay();
        // facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614): the "is the argument a
        // `.get()`-collapsed bare item?" half of this gate reads the family's shared arbiter; the
        // multi-line half stays (it is a fact of the text this method is about to splice, and the
        // #387 replay window is the exception it was scoped for). The c7b census at this seat:
        // 17,493 / 17,493 / 14,308 arrivals (default-route, IR-route, optimised), text=true at
        // 149, and `ONLY_ELEMENT AND typeNull` selects
        // exactly those 149 on all three walks. Neither of the row's named channels would have
        // done: the AST bare-item/list-literal pair covers only 26 of the 149 (the S08 refutation),
        // and the recovered WRAPPER is non-null on BOTH text polarities, so the wrapper alone
        // over-fires by 36 same-shape arrivals.
        boolean collapsedArg = HandlerHelper.bareOnlyElementCollapse(expr.argument(), argBuilder);
        RJavaWithMetaValue collapseMeta = collapsedArg
                && (!arg.contains("\n") || toStrReplayWindow)
                ? collapseMetaWrapper(expr.argument(), compiler)
                : null;
        // facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614): the in-method SPLICE
        // fact the #356 ctor-setter/list-literal gate below used to read out of the text. Set
        // exactly where the meta-deref hoist REPLACES `arg` with its guarded re-presentation — both
        // helpers return a `(<name> == null ? … : …)` form, so the `.get()` tail is gone whenever
        // this flag is set, which is precisely what the old suffix test observed (see below).
        boolean collapseDerefSpliced = false;
        if (collapseMeta != null && ctx.scope() != null) {
            String deref = HandlerHelper.metaCollapseDerefHoistOrNull(
                    arg, collapseMeta, ctx.scope(), refs);
            if (deref != null) {
                arg = deref;
                collapseDerefSpliced = true;
            } else if (ctx.scope().isLadderJoinBareArmReplay()) {
                // facet toStringCollapsedMetaRetype (PR #387): the sink-less in-lambda
                // seat — the join-bare ladder REPLAY's collapse arm hoists IN-RUNG via
                // the pending-lambda channel (see metaCollapseDerefLambdaHoist; the
                // arm drain admits the class under the same replay flag). Outside the
                // replay the #361 decline stands byte-frozen.
                arg = HandlerHelper.metaCollapseDerefLambdaHoist(
                        arg, collapseMeta, ctx.scope(), refs);
                collapseDerefSpliced = true;
            }
        }
        // facet toStringListLiteralCollapseRewrap (v3.1 flip seat 32, law D.3): golden
        // `MapperC.of(MapperS.of(MapperC.<String>of(<a>, <b>).get()).map("to-string",
        // Object::toString))` <- fork `MapperC.<String>of(<a>, <b>).get().map("to-string",
        // Object::toString)` (drr common NameOfTheUnderlyingIndexRule x drr 7.0/7.1/7.2/7.3,
        // sigs B059 + B019; 4 WHOLE files). The #356 gate below admits exactly ONE seat - a
        // SINGLE-LINE collapse under an RKeyValuePair ctor-setter value - and BOTH of those
        // conjuncts decline this carrier: its `[<a> first to-string, <b> first to-string]
        // only-element` renders MULTI-LINE (each element's `first` hop breaks the line), and
        // its parent is a conditional ARM, not a key-value pair. Admit the LIST-LITERAL
        // collapse as its own disjunct, keyed on the AST SHAPE rather than on the rendered
        // text: the argument is a single-collapsing list-op over a NON-EMPTY RListLiteral -
        // the same shape predicate the #337 value-returning ladder rung consults, hoisted to
        // HandlerHelper.listLiteralCollapseArgument (LAW 69 - one walk, two consumers, each
        // passing its own operator set so neither seat widens into the other).
        //
        // WHAT STAYS PUT, deliberately. The newline test and the RKeyValuePair parent test
        // stay INSIDE the ctor-setter disjunct they were scoped for, and the new disjunct
        // adds no rendered-text test at all: rune-ir-java re-implements the collapse render
        // including its line breaks (IRJavaLeafEmitter's ONLY_ELEMENT link/method arm), so a
        // newline-keyed admission is a standing route-divergence risk - this law REMOVES the
        // newline test from the admitted path rather than adding another (LAW 77). The meta
        // guard is KEPT and UNWIDENED: it is what reserves a META-nav list-literal collapse
        // for the #361/#387 hoist family - the drr 6.34.1-6.38.0 NameOfTheUnderlyingIndexRule
        // greens, the SAME basename as the carrier, whose golden is `MapperC.of((
        // fieldWithMetaStringN == null ? MapperS.<String>ofNull() : MapperS.of(
        // fieldWithMetaStringN.getValue())).map("to-string", Object::toString))`. Stated
        // rather than assumed: at THIS corpus that guard is defence-in-depth, because every
        // meta collapse it would catch has already been rewritten by the #361/#387 hoist
        // above and no longer ends `.get()` when control reaches here.
        //
        // GREEN-SAFE BY MEASUREMENT, not by construction. This gate's ONLY effect is to wrap
        // an `arg` that ENDS `.get()`, so the only byte it can move is an emitted
        // `<x>.get().map("to-string", `. That token occurs in ZERO of the 174,141 goldens (a
        // read-only walk over every cell of every corpus), and outside the 88-file band the
        // fork IS golden - so it occurs in the fork exactly FOUR times, one per carrier cell.
        // No green file can reach a byte this disjunct moves. The WRAPPED form
        // `<x>.get()).map("to-string", ` is a 22-file golden domain: the 18 GREEN
        // Create_ContractType15__1 (9 drr cells x the hkma dtcc/tr projections), which keep
        // riding the untouched ctor-setter disjunct, plus these 4 carriers.
        //
        // The outer `MapperC.of(...)` half needs NO code change. Once the arm text begins
        // `MapperS.of(`, the render-prefix short-circuit at the MapperC arm-lift seat (the
        // facet mapperCFactoryArmNoRewrap arm in CollectionHandler) no longer matches and the
        // lift emits golden's wrap. That is the same missing wrap PROBE29-F16w measured on
        // this very file a seat ago (`golden=2 gen=1 decidedBy=mapperCPrefix`, four cells,
        // both routes); the short-circuit is CORRECT and stays untouched - it only fired
        // because this gate declined and left the arm beginning `MapperC.`.
        //
        // facet collapseGetSuffix (v3.1 C2d retirement family 7, PR #614): the leading "does `arg`
        // still end `.get()`?" test is the family arbiter AND the in-method splice fact, because
        // the census showed the text read here was answering TWO questions at once. Over the
        // 17,493 / 17,493 / 14,308 arrivals (default-route, IR-route, optimised),
        // `ONLY_ELEMENT AND typeNull` holds at 149 while the text held at only
        // 35: the other 114 are arrivals where THIS METHOD's own meta-deref hoist above already
        // replaced the collapse with its guarded re-presentation, so the tail the test looked for
        // had been spliced away one step earlier (the paragraph at the #361 seat says exactly that:
        // "the re-presented arg no longer ends `.get()`, so the #356 ctor-setter wrap below stays
        // naturally disjoint"). text=true <=> `ONLY_ELEMENT AND typeNull AND the hoist did not
        // splice` — EXACT on all three walks; the fires are Create_ContractType15__1 x18 at the
        // ctor-setter seat and NameOfTheUnderlyingIndex x17 at the list-literal seat, and the 9
        // list-literal text=false arrivals are exactly the spliced class. The splice fact is an
        // in-method local, so no text read is needed to carry it.
        boolean listLitCollapse = HandlerHelper.listLiteralCollapseArgument(
                expr.argument(), ListOp.ONLY_ELEMENT, ListOp.FIRST, ListOp.LAST) != null;
        boolean ctorSetterValueSeat = !arg.contains("\n")
                && expr.parent() instanceof RKeyValuePair;
        if (collapsedArg && !collapseDerefSpliced && (ctorSetterValueSeat || listLitCollapse)
                && NavigationHandler.recoverExprMetaWrapper(expr.argument(), compiler) == null) {
            arg = "MapperS.of(" + arg + ")";
            refs.add(HandlerHelper.MAPPER_S);
        }
        String toStringMethod = "Object::toString";
        REnumeration sourceEnum = sourceEnumeration(expr.argument(), compiler);
        if (sourceEnum != null) {
            toStringMethod = sourceEnum.name() + "::toDisplayString";
            var translator = compiler.getTypeTranslator();
            if (translator != null) {
                refs.add(translator.toJavaReferenceType(new REnumTypeRef(sourceEnum)));
            }
        }
        return JavaExpression.from(
                arg + ".map(\"to-string\", " + toStringMethod + ")",
                null,
                refs,
                argBuilder.getStaticWildcardImports());
    }

    /**
     * Best-effort resolution of a {@code to-string} source expression's
     * {@link REnumeration}: the workspace inferred-type map (a directly-typed
     * source), then {@link NavigationHandler#leafEnumeration} (a navigation
     * source — the same receiver-chain resolution the {@code <Type>} witness on
     * the same line uses), then {@link NavigationHandler#implicitItemEnumeration}
     * for a bare implicit-{@code item} source whose enum is pinned by the
     * enclosing filter/extract's argument. Returns {@code null} — the
     * {@code Object::toString} form — when no route resolves an enum (a
     * string-typed source, an unresolvable chain, or the stateless compiler);
     * upstream's test is {@code rType.stripFromTypeAliases instanceof REnumType}
     * on the same argument. The alias-strip half is deliberately NOT mirrored
     * (same shape as the to-enum {@link #isEnumSourceType}): an alias-of-enum
     * source falls to the generic form — a missed flip on an already-waivered
     * file, never a wrong byte on a green one (zero camelCase to-string labels
     * exist in any golden), and a decline-gate for the W42 inventory.
     */
    private static REnumeration sourceEnumeration(RExpression argument, ExpressionCompiler compiler) {
        if (argument == null) {
            return null;
        }
        // facet tostringAliasEnumSource (PR #334): a CONDITIONAL source joins its arms
        // to ONE type (rune's conditional join), so the enum resolves from whichever
        // arm yields one — the alias-of-enum bodies are elseless-chained conditionals
        // over meta-enum navigations (drr upi FloatingRateIndex). A non-enum
        // conditional resolves null on every arm — the generic form unchanged.
        if (argument instanceof RConditionalExpr condSrc) {
            REnumeration fromThen = sourceEnumeration(condSrc.thenBranch(), compiler);
            if (fromThen != null) {
                return fromThen;
            }
            return condSrc.elseBranch()
                    .map(e -> sourceEnumeration(e, compiler)).orElse(null);
        }
        REnumeration en = NavigationHandler.inferredEnumeration(argument, compiler);
        if (en != null) {
            return en;
        }
        en = NavigationHandler.leafEnumeration(argument, compiler);
        if (en != null) {
            return en;
        }
        if (argument instanceof RImplicitVariable) {
            return NavigationHandler.implicitItemEnumeration(argument, compiler);
        }
        // facet checkedMapEnumSource (PR #224): the to-string siblings of the to-enum
        // fallbacks — a bare alias/function source (→ siblingComparandEnumeration) and
        // an alias/function-rooted disguised navigation source whose enum
        // leafEnumeration cannot recover (→ the conversion-scoped gm-aware leaf walk).
        // Both yield SourceEnum::toDisplayString, not Object::toString; a non-enum
        // source resolves null and preserves the generic form (green-safe).
        en = NavigationHandler.siblingComparandEnumeration(argument, compiler);
        if (en != null) {
            return en;
        }
        en = conversionLeafEnumeration(argument, compiler);
        if (en != null) {
            return en;
        }
        // facet tostringAliasEnumSource (PR #334): a function-ALIAS source resolves
        // through its BODY — upstream types the shortcut call from the shortcut
        // expression, so an alias-of-(meta-)enum is an enum source there and golden
        // renders `<Enum>::toDisplayString` (drr upi FloatingRateIndex's
        // `floatingRateIndex(interestRatePayout) to-string`). The former decline
        // ("alias-of-enum falls to the generic form") was the documented W42
        // reach-only gate; opening it only ever turns an already-waivered generic
        // `Object::toString` label golden-exact (zero goldens carry the generic
        // label over an enum-typed alias source).
        if (argument instanceof com.regnosys.rosetta.ast.expressions.references.RSymbolReference aliasRef) {
            com.regnosys.rosetta.ast.functions.RShortcut sc = aliasRef.symbol()
                    .filter(com.regnosys.rosetta.ast.functions.RShortcut.class::isInstance)
                    .map(com.regnosys.rosetta.ast.functions.RShortcut.class::cast).orElse(null);
            if (sc != null && sc.expression() != null) {
                return sourceEnumeration(sc.expression(), compiler);
            }
            // facet caseNarrowedToStringArm (PR #396): a bare unresolved SYMBOL source
            // inside a TYPE-guard switch CASE resolves as the NARROWED case type's
            // attribute — the #368-F-B re-root's ENUM read, so the case-narrowed bare
            // nav's to-string renders the enum label form (golden cdm6
            // ExtractNotionalAdjustmentByLeg `NotionalAdjustmentEnum::toDisplayString`).
            // Monotone: a green Object::toString over a case-narrowed ENUM attr cannot
            // exist (upstream always renders toDisplayString for enum sources).
            if (aliasRef.args().isEmpty() && aliasRef.name() != null
                    && aliasRef.symbol().isEmpty()) {
                com.regnosys.rosetta.ast.types.RDataType narrowed =
                        NavigationHandler.caseNarrowedImplicitType(argument, compiler);
                if (narrowed != null) {
                    com.regnosys.rosetta.ast.supporting.RAttribute caseAttr =
                            HandlerHelper.findAttributeOnDataType(narrowed, aliasRef.name());
                    if (caseAttr != null && caseAttr.typeCall() != null) {
                        REnumeration caseEnum = caseAttr.typeCall().referencedType()
                                .filter(REnumeration.class::isInstance)
                                .map(REnumeration.class::cast)
                                .orElse(null);
                        if (caseEnum != null) {
                            return caseEnum;
                        }
                    }
                }
            }
        }
        // facet metaValueDerefHoist (PR #361): an element-preserving list-op source
        // (only-element / first / last / distinct) resolves through its ARGUMENT —
        // the collapse preserves the element type, so the enum is the argument
        // chain's (golden csa InterestRateLeg1/2ReturnSwap `<alias-of-meta-enum>
        // only-element to-string` renders FloatingRateIndexEnum::toDisplayString
        // through this descent + the #334 alias-body arm above).
        if (argument instanceof RListOpExpr lop && isElementPreservingListOp(lop.op())) {
            return sourceEnumeration(lop.argument(), compiler);
        }
        return null;
    }

    /** The element-preserving cardinality collapses (the terminalNavAttr set). */
    private static boolean isElementPreservingListOp(ListOp op) {
        return op == ListOp.ONLY_ELEMENT || op == ListOp.FIRST
                || op == ListOp.LAST || op == ListOp.DISTINCT;
    }

    /**
     * facet metaValueDerefHoist (PR #361): the to-string collapse-argument meta
     * recovery — the shared #264 walker first, then a RECOVERY-LOCAL
     * element-preserving alias-body descent (strip only-element/first/last/
     * distinct shells to a bare alias ref and recover from the shortcut BODY —
     * the walker's own RSymbolReference arm resolves only RAttribute symbols,
     * and widening the SHARED walker to aliases would move its other consumers:
     * the #360 (a3) / #224/#225 conversion-scope discipline). Null when neither
     * route proves a meta-wrapper element — the caller keeps today's bytes.
     */
    static RJavaWithMetaValue collapseMetaWrapper(RExpression argument,
            ExpressionCompiler compiler) {
        RJavaWithMetaValue direct = NavigationHandler.recoverExprMetaWrapper(argument, compiler);
        if (direct != null) {
            return direct;
        }
        RExpression cur = argument;
        int depth = 0;
        while (cur instanceof RListOpExpr lop && depth++ < 8
                && isElementPreservingListOp(lop.op())) {
            cur = lop.argument();
        }
        if (cur instanceof RSymbolReference sr) {
            com.regnosys.rosetta.ast.functions.RShortcut sc = sr.symbol()
                    .filter(com.regnosys.rosetta.ast.functions.RShortcut.class::isInstance)
                    .map(com.regnosys.rosetta.ast.functions.RShortcut.class::cast).orElse(null);
            if (sc != null && sc.expression() != null) {
                return NavigationHandler.recoverExprMetaWrapper(sc.expression(), compiler);
            }
        }
        // facet toStringCollapsedMetaRetype (PR #387): a LIST-LITERAL under the
        // collapse shell (`[<navMeta> first, <navMeta> first] only-element to-string` —
        // drr common NameOfTheUnderlyingIndexRule) joins its ELEMENTS' wrappers via the
        // #331 all-present-arms-agree law: every element must recover the SAME wrapper
        // (the shared walker descends each element's own fused collapse); any null or
        // disagreeing element declines — the caller keeps today's bytes. RECOVERY-LOCAL
        // like the alias arm above (the #360 (a3) conversion-scope discipline: the
        // shared walker stays untouched).
        if (cur instanceof com.regnosys.rosetta.ast.expressions.literals.RListLiteral ll
                && !ll.elements().isEmpty()) {
            RJavaWithMetaValue joined = null;
            for (RExpression el : ll.elements()) {
                RJavaWithMetaValue m = NavigationHandler.recoverExprMetaWrapper(el, compiler);
                if (m == null || (joined != null && !m.equals(joined))) {
                    return null;
                }
                joined = m;
            }
            return joined;
        }
        return null;
    }

}
