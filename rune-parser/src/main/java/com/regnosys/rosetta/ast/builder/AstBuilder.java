package com.regnosys.rosetta.ast.builder;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.annotations.RAnnotation;
import com.regnosys.rosetta.ast.annotations.RAnnotationPathExpression;
import com.regnosys.rosetta.ast.annotations.RAnnotationPathSegment;
import com.regnosys.rosetta.ast.annotations.RAnnotationQualifier;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.annotations.RLabelAnnotation;
import com.regnosys.rosetta.ast.annotations.RRuleReferenceAnnotation;
import com.regnosys.rosetta.ast.annotations.RRuneAnnotation;
import com.regnosys.rosetta.ast.annotations.RRuneAnnotationArg;
import com.regnosys.rosetta.ast.enums.ArithOp;
import com.regnosys.rosetta.ast.enums.CardCheckOp;
import com.regnosys.rosetta.ast.enums.CardMod;
import com.regnosys.rosetta.ast.enums.CompOp;
import com.regnosys.rosetta.ast.enums.ConversionKind;
import com.regnosys.rosetta.ast.enums.EqOp;
import com.regnosys.rosetta.ast.enums.ExistenceOp;
import com.regnosys.rosetta.ast.enums.ExistsModifier;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.enums.LogOp;
import com.regnosys.rosetta.ast.enums.MapPrimaryKind;
import com.regnosys.rosetta.ast.enums.MappingInstanceKind;
import com.regnosys.rosetta.ast.enums.Necessity;
import com.regnosys.rosetta.ast.enums.OperationOp;
import com.regnosys.rosetta.ast.enums.QualifiableKind;
import com.regnosys.rosetta.ast.enums.RSynonymRef;
import com.regnosys.rosetta.ast.enums.ReportTiming;
import com.regnosys.rosetta.ast.enums.RuleKind;
import com.regnosys.rosetta.ast.enums.SwitchGuardKind;
import com.regnosys.rosetta.ast.enums.SwitchGuardLiteralKind;
import com.regnosys.rosetta.ast.enums.SynonymBodyKind;
import com.regnosys.rosetta.ast.external.RExternalClass;
import com.regnosys.rosetta.ast.external.RExternalClassSynonym;
import com.regnosys.rosetta.ast.external.RExternalEnum;
import com.regnosys.rosetta.ast.external.RExternalEnumSynonym;
import com.regnosys.rosetta.ast.external.RExternalEnumValue;
import com.regnosys.rosetta.ast.external.RExternalRegularAttribute;
import com.regnosys.rosetta.ast.external.RExternalRuleSource;
import com.regnosys.rosetta.ast.external.RExternalSynonym;
import com.regnosys.rosetta.ast.external.RExternalSynonymSource;
import com.regnosys.rosetta.ast.expressions.binary.RArithmeticExpr;
import com.regnosys.rosetta.ast.expressions.binary.RComparisonExpr;
import com.regnosys.rosetta.ast.expressions.binary.RContainsExpr;
import com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr;
import com.regnosys.rosetta.ast.expressions.binary.RDisjointExpr;
import com.regnosys.rosetta.ast.expressions.binary.REqualityExpr;
import com.regnosys.rosetta.ast.expressions.binary.RJoinExpr;
import com.regnosys.rosetta.ast.expressions.binary.RLogicalExpr;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RNumberLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RStringLiteral;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.REmptyLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSuperCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair;
import com.regnosys.rosetta.ast.expressions.supporting.ROnlyExistsElement;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard;
import com.regnosys.rosetta.ast.expressions.supporting.RWithMetaEntry;
import com.regnosys.rosetta.ast.expressions.unary.RCardinalityCheckExpr;
import com.regnosys.rosetta.ast.expressions.unary.RConversionExpr;
import com.regnosys.rosetta.ast.expressions.unary.RCountExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExistenceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.unary.RFilterExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMaxExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMinExpr;
import com.regnosys.rosetta.ast.expressions.unary.ROnlyExistsExpr;
import com.regnosys.rosetta.ast.expressions.unary.RReduceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSortExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr;
import com.regnosys.rosetta.ast.expressions.unary.RToStringExpr;
import com.regnosys.rosetta.ast.expressions.unary.RWithMetaExpr;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RDispatch;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.ROperation;
import com.regnosys.rosetta.ast.functions.RPostCondition;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RSegment;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.mapping.RMapPath;
import com.regnosys.rosetta.ast.mapping.RMapPathValue;
import com.regnosys.rosetta.ast.mapping.RMapPrimaryExpression;
import com.regnosys.rosetta.ast.mapping.RMapRosettaPath;
import com.regnosys.rosetta.ast.mapping.RMapTest;
import com.regnosys.rosetta.ast.mapping.RMapTestAbsent;
import com.regnosys.rosetta.ast.mapping.RMapTestEquality;
import com.regnosys.rosetta.ast.mapping.RMapTestExists;
import com.regnosys.rosetta.ast.mapping.RMapTestFunc;
import com.regnosys.rosetta.ast.mapping.RMapping;
import com.regnosys.rosetta.ast.mapping.RMappingInstance;
import com.regnosys.rosetta.ast.mapping.RMappingPathTests;
import com.regnosys.rosetta.ast.mapping.RMappingSetTo;
import com.regnosys.rosetta.ast.mapping.RMappingSetToInstance;
import com.regnosys.rosetta.ast.model.RImport;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.model.RQualifiableConfig;
import com.regnosys.rosetta.ast.model.RScope;
import com.regnosys.rosetta.ast.regulatory.RBody;
import com.regnosys.rosetta.ast.regulatory.RCorpus;
import com.regnosys.rosetta.ast.regulatory.RDocReference;
import com.regnosys.rosetta.ast.regulatory.RDocumentRationale;
import com.regnosys.rosetta.ast.regulatory.RRegulatoryDocumentReference;
import com.regnosys.rosetta.ast.regulatory.RReport;
import com.regnosys.rosetta.ast.regulatory.RSegmentDef;
import com.regnosys.rosetta.ast.regulatory.RSegmentRef;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RChoiceOption;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.supporting.RParameter;
import com.regnosys.rosetta.ast.supporting.RRecordFeature;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.supporting.RTypeCallArgument;
import com.regnosys.rosetta.ast.supporting.RTypeCallArgumentExpression;
import com.regnosys.rosetta.ast.supporting.RTypeParameter;
import com.regnosys.rosetta.ast.synonyms.RClassSynonym;
import com.regnosys.rosetta.ast.synonyms.RClassSynonymValue;
import com.regnosys.rosetta.ast.synonyms.REnumSynonym;
import com.regnosys.rosetta.ast.synonyms.RMergeSynonymValue;
import com.regnosys.rosetta.ast.synonyms.RMetaSynonymValue;
import com.regnosys.rosetta.ast.synonyms.RSynonym;
import com.regnosys.rosetta.ast.synonyms.RSynonymBody;
import com.regnosys.rosetta.ast.synonyms.RSynonymSource;
import com.regnosys.rosetta.ast.synonyms.RSynonymValue;
import com.regnosys.rosetta.ast.types.RBasicType;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RLibraryFunction;
import com.regnosys.rosetta.ast.types.RMetaType;
import com.regnosys.rosetta.ast.types.RRecordType;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.parser.RosettaParseResult;
import com.regnosys.rosetta.parser.RosettaParser;
import com.regnosys.rosetta.parser.RosettaParser.RosettaModelContext;
import com.regnosys.rosetta.parser.RosettaParserBaseVisitor;
import com.regnosys.rosetta.parser.RosettaParserFacade;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ANTLR4 visitor that converts a Rosetta parse tree into a typed AST rooted
 * at {@link RModel}.
 *
 * <p>The visitor implements grammar-rule visit methods across these categories:
 * <ul>
 *   <li><strong>Model structure</strong> — namespace, imports, scope, version,
 *       qualifiable configurations.</li>
 *   <li><strong>Type declarations</strong> — data types, enumerations, choices,
 *       type aliases, basic types, record types, meta types, library functions,
 *       and supporting types (attributes, enum values, choice options, type calls,
 *       type call arguments, cardinalities, type parameters, record features,
 *       library function parameters).</li>
 *   <li><strong>Functions and rules</strong> — functions (with dispatch, extends,
 *       inputs/output, conditions, operations, shortcuts, post-conditions),
 *       reporting and eligibility rules, segments.</li>
 *   <li><strong>Annotations</strong> — annotation declarations, annotation
 *       references with qualifiers, label annotations, rule reference annotations,
 *       annotation path expressions.</li>
 *   <li><strong>Regulatory</strong> — bodies, corpora, segments, reports,
 *       document references with rationales and provisions.</li>
 *   <li><strong>Expressions</strong> — all ~91 labelled grammar alternatives
 *       under the {@code expression} rule, including binary operators, postfix
 *       operations, "without left" partial forms, primary expressions (literals,
 *       references, super, item, empty, list literals), and parenthesised forms
 *       (which are transparent per design decision D9).</li>
 * </ul>
 *
 * <p><strong>Synonyms, mappings, and external sources</strong> (Task 14) — class
 * and attribute synonyms, enum synonyms, synonym bodies (all five alternatives),
 * synonym source declarations, mapping instances and set-to forms, mapping path
 * tests and all six map-test alternatives, and external synonym/rule sources
 * with their nested classes, attributes, enums, and synonyms.
 *
 * <p>Usage:
 * <pre>
 *   RModel model = AstBuilder.buildFromString(source, "test.rosetta");
 *   RModel model = AstBuilder.buildFromFile(Paths.get("model.rosetta"));
 * </pre>
 */
public class AstBuilder extends RosettaParserBaseVisitor<Object> {

    private final String fileName;
    private final int[] charToByte;

    /**
     * Creates a new builder for the given source file name (byte offsets disabled).
     *
     * <p>Source ranges produced by this builder use the {@link SourceRange#OFFSETS_UNKNOWN}
     * sentinel for byte offsets. New code should call
     * {@link #AstBuilder(String, String)} to populate byte-precise positions.
     *
     * @param fileName the source file name used in {@link com.regnosys.rosetta.ast.SourceRange}
     * @deprecated since 0.1.0 — use {@link #AstBuilder(String, String)} for byte
     *     offsets. See {@code docs/upgrades/U001-bytewise-source-range.md}.
     *     Removal: not scheduled.
     */
    @Deprecated(since = "0.1.0", forRemoval = false)
    public AstBuilder(String fileName) {
        this.fileName = fileName;
        this.charToByte = null;
    }

    /**
     * Creates a new builder for the given source file name and content.
     *
     * <p>Source ranges produced by this builder include UTF-8 byte offsets
     * via the {@link com.regnosys.rosetta.ast.CharToByteOffsets} projection
     * table built once from the supplied content.
     *
     * @param fileName the source file name used in {@link com.regnosys.rosetta.ast.SourceRange}
     * @param content  the source code; used to build the char-to-byte projection table
     */
    public AstBuilder(String fileName, String content) {
        this.fileName = fileName;
        this.charToByte = com.regnosys.rosetta.ast.CharToByteOffsets.table(content);
    }

    /**
     * Builds a {@link SourceRange} spanning two arbitrary tokens. Falls back to
     * sentinel offsets when no char-to-byte table is available, when either
     * token has a {@code -1} index, or when {@code stop.getStopIndex() + 1}
     * would index past the table.
     */
    private SourceRange rangeOfTokenSpan(org.antlr.v4.runtime.Token start, org.antlr.v4.runtime.Token stop) {
        if (charToByte == null
                || start.getStartIndex() < 0
                || stop.getStopIndex() < 0
                || stop.getStopIndex() + 1 >= charToByte.length) {
            return SourceRange.of5Arg(
                    fileName,
                    start.getLine(),
                    start.getCharPositionInLine() + 1,
                    stop.getLine(),
                    stop.getCharPositionInLine() + stop.getText().length()
            );
        }
        return SourceRange.of(start, stop, fileName, charToByte);
    }

    // -- Entry points ---------------------------------------------------------

    /**
     * Builds a typed AST from the given parse tree root.
     *
     * @param tree the rosettaModel parse tree context
     * @return the root {@link RModel} with parent pointers set
     * @throws AstBuildException if the tree is null or structurally invalid
     */
    public RModel build(RosettaModelContext tree) {
        if (tree == null) {
            throw new AstBuildException("Parse tree is null");
        }
        RModel model = (RModel) visitRosettaModel(tree);
        AstBuilderHelper.setParents(model);
        return model;
    }

    /**
     * Parses the given file and builds a typed AST.
     *
     * @param rosettaFile path to the .rosetta source file
     * @return the root {@link RModel}
     * @throws AstBuildException if parsing produces errors or the file cannot be read
     */
    public static RModel buildFromFile(Path rosettaFile) {
        RosettaParseResult result = RosettaParserFacade.parseFile(rosettaFile);
        if (!result.errors().isEmpty()) {
            throw new AstBuildException(
                    "Parse errors in " + rosettaFile + ": " + result.errors());
        }
        String content;
        try {
            content = java.nio.file.Files.readString(rosettaFile);
        } catch (java.io.IOException e) {
            throw new AstBuildException("Cannot read source file " + rosettaFile + ": " + e.getMessage(), e);
        }
        AstBuilder builder = new AstBuilder(rosettaFile.getFileName().toString(), content);
        return builder.build((RosettaModelContext) result.tree());
    }

    /**
     * Parses the given source string and builds a typed AST.
     *
     * @param source   the Rune DSL source code
     * @param fileName the file name to embed in source ranges
     * @return the root {@link RModel}
     * @throws AstBuildException if parsing produces errors
     */
    public static RModel buildFromString(String source, String fileName) {
        RosettaParseResult result = RosettaParserFacade.parseString(source);
        if (!result.errors().isEmpty()) {
            throw new AstBuildException(
                    "Parse errors in " + fileName + ": " + result.errors());
        }
        AstBuilder builder = new AstBuilder(fileName, source);
        return builder.build((RosettaModelContext) result.tree());
    }

    // -- Model structure visitors ---------------------------------------------

    /**
     * Visits the top-level {@code rosettaModel} rule and builds an {@link RModel}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaModel:
     *     OVERRIDE? NAMESPACE (qualifiedName | STRING) (COLON definable?)?
     *     rosettaScope?
     *     versionDecl?
     *     importDecl*
     *     rosettaQualifiableConfiguration*
     *     rootElement*
     *     EOF
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaModel(RosettaParser.RosettaModelContext ctx) {
        RModel model = new RModel();

        // Source range
        model.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // fileHeader? (P1.4.2 H3 — appears first in source, before NAMESPACE)
        if (ctx.fileHeader() != null) {
            model.setFileHeader(
                (com.regnosys.rosetta.ast.RFileHeader) visitFileHeader(ctx.fileHeader()));
        }

        // OVERRIDE?
        if (ctx.OVERRIDE() != null) {
            model.setOverride(true);
            model.putTokenRange("override",
                    AstBuilderHelper.rangeOfToken(ctx.OVERRIDE().getSymbol(), fileName, charToByte));
        }

        // NAMESPACE keyword token range
        model.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.NAMESPACE().getSymbol(), fileName, charToByte));

        // Namespace: qualifiedName | STRING
        if (ctx.qualifiedName() != null) {
            model.setNamespace(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
            model.putTokenRange("name",
                    AstBuilderHelper.rangeOf(ctx.qualifiedName(), fileName, charToByte));
        } else if (ctx.STRING() != null) {
            model.setNamespace(AstBuilderHelper.stripQuotes(ctx.STRING().getText()));
            model.putTokenRange("name",
                    AstBuilderHelper.rangeOfToken(ctx.STRING().getSymbol(), fileName, charToByte));
        }

        // definable? (COLON definable?)
        if (ctx.definable() != null) {
            model.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        // rosettaScope?
        if (ctx.rosettaScope() != null) {
            RScope scope = (RScope) visitRosettaScope(ctx.rosettaScope());
            model.setScope(scope);
        }

        // versionDecl?
        if (ctx.versionDecl() != null) {
            model.setVersion(visitVersionDecl(ctx.versionDecl()));
        }

        // importDecl*
        for (RosettaParser.ImportDeclContext importCtx : ctx.importDecl()) {
            RImport imp = (RImport) visitImportDecl(importCtx);
            model.imports().add(imp);
        }

        // rosettaQualifiableConfiguration*
        for (RosettaParser.RosettaQualifiableConfigurationContext cfgCtx
                : ctx.rosettaQualifiableConfiguration()) {
            RQualifiableConfig cfg =
                    (RQualifiableConfig) visitRosettaQualifiableConfiguration(cfgCtx);
            model.configurations().add(cfg);
        }

        // rootElement* — delegate to child visitors. All root element kinds
        // have implementations as of Task 14. Any unexpected null result
        // (e.g. from a future unimplemented alternative) is safely skipped.
        for (RosettaParser.RootElementContext rootCtx : ctx.rootElement()) {
            Object result = visitRootElement(rootCtx);
            if (result instanceof RRootElement rootElement) {
                model.rootElements().add(rootElement);
            }
        }

        return model;
    }

    /**
     * Visits an {@code importDecl} rule and builds an {@link RImport}.
     *
     * <p>Grammar: {@code IMPORT qualifiedNameWithWildcard (AS validID)?}
     */
    @Override
    public Object visitImportDecl(RosettaParser.ImportDeclContext ctx) {
        RImport imp = new RImport();
        imp.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // IMPORT keyword token
        imp.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.IMPORT().getSymbol(), fileName, charToByte));

        // qualifiedNameWithWildcard
        var qnwCtx = ctx.qualifiedNameWithWildcard();
        var qualifiedNameCtx = qnwCtx.qualifiedName();
        imp.setQualifiedName(AstBuilderHelper.textOfQualifiedName(qualifiedNameCtx));
        imp.putTokenRange("name",
                AstBuilderHelper.rangeOf(qualifiedNameCtx, fileName, charToByte));

        // DOT STAR?
        imp.setWildcard(qnwCtx.STAR() != null);

        // AS validID?
        if (ctx.AS() != null && ctx.validID() != null) {
            imp.setAlias(ctx.validID().getText());
            imp.putTokenRange("alias",
                    AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));
        }

        return imp;
    }

    /**
     * Visits a {@code rosettaScope} rule and builds an {@link RScope}.
     *
     * <p>Grammar: {@code SCOPE validID definable?}
     */
    @Override
    public Object visitRosettaScope(RosettaParser.RosettaScopeContext ctx) {
        RScope scope = new RScope();
        scope.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // SCOPE keyword token
        scope.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.SCOPE().getSymbol(), fileName, charToByte));

        // validID — scope name
        scope.setName(ctx.validID().getText());
        scope.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // definable?
        if (ctx.definable() != null) {
            scope.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        return scope;
    }

    /**
     * Visits a {@code versionDecl} rule and extracts the version string.
     *
     * <p>Grammar: {@code VERSION STRING}
     *
     * <p>Note: this returns the unquoted version string directly rather than
     * an AST node, since the version is stored as a simple field on RModel.
     */
    public String visitVersionDecl(RosettaParser.VersionDeclContext ctx) {
        return AstBuilderHelper.stripQuotes(ctx.STRING().getText());
    }

    // ========================================================================
    // File header (P1.4.2 H3) — file-level metadata block at top of model.
    // ========================================================================

    /**
     * Visits a {@code fileHeader} rule and builds an
     * {@link com.regnosys.rosetta.ast.RFileHeader}.
     *
     * <p>Grammar:
     * <pre>
     * fileHeader:
     *     FILE_HEADER COLON
     *     versionField?
     *     dependsOnField?
     *     experimentalField?
     * ;
     * </pre>
     *
     * <p>Layer-1 only — fields parsed and stored, no runtime semantics.
     */
    @Override
    public Object visitFileHeader(RosettaParser.FileHeaderContext ctx) {
        com.regnosys.rosetta.ast.RFileHeader header = new com.regnosys.rosetta.ast.RFileHeader();
        header.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        header.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.FILE_HEADER().getSymbol(), fileName, charToByte));

        if (ctx.versionField() != null) {
            header.setVersion(
                AstBuilderHelper.stripQuotes(ctx.versionField().STRING().getText()));
        }
        if (ctx.dependsOnField() != null) {
            for (org.antlr.v4.runtime.tree.TerminalNode s : ctx.dependsOnField().STRING()) {
                header.dependsOn().add(AstBuilderHelper.stripQuotes(s.getText()));
            }
        }
        if (ctx.experimentalField() != null) {
            for (RosettaParser.ValidIDContext id : ctx.experimentalField().validID()) {
                header.experimental().add(id.getText());
            }
        }
        return header;
    }

    /**
     * Visits a {@code rosettaQualifiableConfiguration} rule and builds an
     * {@link RQualifiableConfig}.
     *
     * <p>Grammar: {@code (IS_EVENT | IS_PRODUCT) ROOT qualifiedName SEMI}
     */
    @Override
    public Object visitRosettaQualifiableConfiguration(
            RosettaParser.RosettaQualifiableConfigurationContext ctx) {
        RQualifiableConfig cfg = new RQualifiableConfig();
        cfg.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // IS_EVENT | IS_PRODUCT
        if (ctx.IS_EVENT() != null) {
            cfg.setKind(QualifiableKind.IS_EVENT);
            cfg.putTokenRange("keyword",
                    AstBuilderHelper.rangeOfToken(ctx.IS_EVENT().getSymbol(), fileName, charToByte));
        } else {
            cfg.setKind(QualifiableKind.IS_PRODUCT);
            cfg.putTokenRange("keyword",
                    AstBuilderHelper.rangeOfToken(ctx.IS_PRODUCT().getSymbol(), fileName, charToByte));
        }

        // ROOT qualifiedName
        cfg.setRootTypeName(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
        cfg.putTokenRange("rootTypeName",
                AstBuilderHelper.rangeOf(ctx.qualifiedName(), fileName, charToByte));

        return cfg;
    }

    /**
     * Visits a {@code rootElement} rule. Delegates to the appropriate child
     * visitor based on which alternative matched. All root element alternatives
     * have visitor implementations as of Task 14.
     */
    @Override
    public Object visitRootElement(RosettaParser.RootElementContext ctx) {
        // rootElement is a pass-through rule with a single child alternative.
        // Delegate to the child context's visitor.
        if (ctx.enumeration() != null) return visit(ctx.enumeration());
        if (ctx.rosettaBody() != null) return visit(ctx.rosettaBody());
        if (ctx.rosettaCorpus() != null) return visit(ctx.rosettaCorpus());
        if (ctx.rosettaSegment() != null) return visit(ctx.rosettaSegment());
        if (ctx.rosettaBasicType() != null) return visit(ctx.rosettaBasicType());
        if (ctx.rosettaRecordType() != null) return visit(ctx.rosettaRecordType());
        if (ctx.rosettaLibraryFunction() != null) return visit(ctx.rosettaLibraryFunction());
        if (ctx.rosettaSynonymSource() != null) return visit(ctx.rosettaSynonymSource());
        if (ctx.rosettaRule() != null) return visit(ctx.rosettaRule());
        if (ctx.rosettaMetaType() != null) return visit(ctx.rosettaMetaType());
        if (ctx.rosettaExternalSynonymSource() != null) return visit(ctx.rosettaExternalSynonymSource());
        if (ctx.rosettaExternalRuleSource() != null) return visit(ctx.rosettaExternalRuleSource());
        if (ctx.rosettaReport() != null) return visit(ctx.rosettaReport());
        if (ctx.rosettaTypeAlias() != null) return visit(ctx.rosettaTypeAlias());
        if (ctx.annotationDecl() != null) return visit(ctx.annotationDecl());
        if (ctx.dataType() != null) return visit(ctx.dataType());
        if (ctx.choice() != null) return visit(ctx.choice());
        if (ctx.function() != null) return visit(ctx.function());
        return null;
    }

    // -- Type declaration visitors (Task 11) ----------------------------------

    /**
     * Visits a {@code dataType} rule and builds an {@link RDataType}.
     *
     * <p>Grammar:
     * <pre>
     * dataType:
     *     TYPE validID (EXTENDS qualifiedName)? COLON definable?
     *     (docReference | annotationRef | classSynonym)*
     *     attribute*
     *     condition*
     * ;
     * </pre>
     */
    @Override
    public Object visitDataType(RosettaParser.DataTypeContext ctx) {
        RDataType dt = new RDataType();
        dt.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // runeAnnotations? (P1.4.2 H2 — prefix attach)
        attachRuneAnnotations(dt, ctx.runeAnnotations());

        // TYPE keyword
        dt.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.TYPE().getSymbol(), fileName, charToByte));

        // validID — name
        dt.setName(ctx.validID().getText());
        dt.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // (EXTENDS qualifiedName)?
        if (ctx.EXTENDS() != null && ctx.qualifiedName() != null) {
            dt.setSuperTypeName(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
            dt.putTokenRange("extends",
                    AstBuilderHelper.rangeOfToken(ctx.EXTENDS().getSymbol(), fileName, charToByte));
            dt.putTokenRange("superType",
                    AstBuilderHelper.rangeOf(ctx.qualifiedName(), fileName, charToByte));
        }

        // COLON
        if (ctx.COLON() != null) {
            dt.putTokenRange("colon",
                    AstBuilderHelper.rangeOfToken(ctx.COLON().getSymbol(), fileName, charToByte));
        }

        // definable?
        if (ctx.definable() != null) {
            dt.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        // (docReference | annotationRef | classSynonym)*
        for (RosettaParser.DocReferenceContext drCtx : ctx.docReference()) {
            RDocReference docRef = (RDocReference) visitDocReference(drCtx);
            dt.docReferences().add(docRef);
        }
        for (RosettaParser.AnnotationRefContext arCtx : ctx.annotationRef()) {
            RAnnotationRef annRef = (RAnnotationRef) visitAnnotationRef(arCtx);
            dt.annotationRefs().add(annRef);
        }
        for (RosettaParser.ClassSynonymContext csCtx : ctx.classSynonym()) {
            RClassSynonym cs = (RClassSynonym) visitClassSynonym(csCtx);
            dt.classSynonyms().add(cs);
        }

        // attribute*
        for (RosettaParser.AttributeContext attrCtx : ctx.attribute()) {
            RAttribute attr = (RAttribute) visitAttribute(attrCtx);
            dt.attributes().add(attr);
        }

        // condition*
        for (RosettaParser.ConditionContext condCtx : ctx.condition()) {
            RCondition cond = (RCondition) visitCondition(condCtx);
            dt.conditions().add(cond);
        }

        return dt;
    }

    /**
     * Visits an {@code enumeration} rule and builds an {@link REnumeration}.
     *
     * <p>Grammar:
     * <pre>
     * enumeration:
     *     ENUM validID (EXTENDS qualifiedName)? COLON definable?
     *     (docReference | annotationRef | rosettaSynonym)*
     *     rosettaEnumValue*
     * ;
     * </pre>
     */
    @Override
    public Object visitEnumeration(RosettaParser.EnumerationContext ctx) {
        REnumeration en = new REnumeration();
        en.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // runeAnnotations? (P1.4.2 H2 — prefix attach)
        attachRuneAnnotations(en, ctx.runeAnnotations());

        // ENUM keyword
        en.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.ENUM().getSymbol(), fileName, charToByte));

        // validID — name
        en.setName(ctx.validID().getText());
        en.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // (EXTENDS qualifiedName)?
        if (ctx.EXTENDS() != null && ctx.qualifiedName() != null) {
            en.setSuperTypeName(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
            en.putTokenRange("extends",
                    AstBuilderHelper.rangeOfToken(ctx.EXTENDS().getSymbol(), fileName, charToByte));
            en.putTokenRange("superType",
                    AstBuilderHelper.rangeOf(ctx.qualifiedName(), fileName, charToByte));
        }

        // COLON
        if (ctx.COLON() != null) {
            en.putTokenRange("colon",
                    AstBuilderHelper.rangeOfToken(ctx.COLON().getSymbol(), fileName, charToByte));
        }

        // definable?
        if (ctx.definable() != null) {
            en.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        // (docReference | annotationRef | rosettaSynonym)*
        for (RosettaParser.DocReferenceContext drCtx : ctx.docReference()) {
            RDocReference docRef = (RDocReference) visitDocReference(drCtx);
            en.docReferences().add(docRef);
        }
        for (RosettaParser.AnnotationRefContext arCtx : ctx.annotationRef()) {
            RAnnotationRef annRef = (RAnnotationRef) visitAnnotationRef(arCtx);
            en.annotationRefs().add(annRef);
        }
        for (RosettaParser.RosettaSynonymContext synCtx : ctx.rosettaSynonym()) {
            RSynonym syn = (RSynonym) visitRosettaSynonym(synCtx);
            en.synonyms().add(syn);
        }

        // rosettaEnumValue*
        for (RosettaParser.RosettaEnumValueContext valCtx : ctx.rosettaEnumValue()) {
            REnumValue val = (REnumValue) visitRosettaEnumValue(valCtx);
            en.values().add(val);
        }

        return en;
    }

    /**
     * Visits a {@code choice} rule and builds an {@link RChoice}.
     *
     * <p>Grammar:
     * <pre>
     * choice:
     *     CHOICE validID COLON definable?
     *     (annotationRef | classSynonym)*
     *     choiceOption*
     * ;
     * </pre>
     */
    @Override
    public Object visitChoice(RosettaParser.ChoiceContext ctx) {
        RChoice ch = new RChoice();
        ch.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // runeAnnotations? (P1.4.2 H2 — prefix attach)
        attachRuneAnnotations(ch, ctx.runeAnnotations());

        // CHOICE keyword
        ch.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.CHOICE().getSymbol(), fileName, charToByte));

        // validID — name
        ch.setName(ctx.validID().getText());
        ch.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // COLON
        if (ctx.COLON() != null) {
            ch.putTokenRange("colon",
                    AstBuilderHelper.rangeOfToken(ctx.COLON().getSymbol(), fileName, charToByte));
        }

        // definable?
        if (ctx.definable() != null) {
            ch.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        // (annotationRef | classSynonym)*
        for (RosettaParser.AnnotationRefContext arCtx : ctx.annotationRef()) {
            RAnnotationRef annRef = (RAnnotationRef) visitAnnotationRef(arCtx);
            ch.annotationRefs().add(annRef);
        }
        for (RosettaParser.ClassSynonymContext csCtx : ctx.classSynonym()) {
            RClassSynonym cs = (RClassSynonym) visitClassSynonym(csCtx);
            ch.classSynonyms().add(cs);
        }

        // choiceOption*
        for (RosettaParser.ChoiceOptionContext optCtx : ctx.choiceOption()) {
            RChoiceOption opt = (RChoiceOption) visitChoiceOption(optCtx);
            ch.options().add(opt);
        }

        return ch;
    }

    /**
     * Visits a {@code rosettaTypeAlias} rule and builds an {@link RTypeAlias}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaTypeAlias:
     *     TYPE_ALIAS validID typeParameters? COLON definable? typeCall condition*
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaTypeAlias(RosettaParser.RosettaTypeAliasContext ctx) {
        RTypeAlias ta = new RTypeAlias();
        ta.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // TYPE_ALIAS keyword
        ta.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.TYPE_ALIAS().getSymbol(), fileName, charToByte));

        // validID — name
        ta.setName(ctx.validID().getText());
        ta.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // typeParameters?
        if (ctx.typeParameters() != null) {
            for (RosettaParser.TypeParameterContext tpCtx : ctx.typeParameters().typeParameter()) {
                RTypeParameter tp = (RTypeParameter) visitTypeParameter(tpCtx);
                ta.typeParameters().add(tp);
            }
        }

        // COLON
        if (ctx.COLON() != null) {
            ta.putTokenRange("colon",
                    AstBuilderHelper.rangeOfToken(ctx.COLON().getSymbol(), fileName, charToByte));
        }

        // definable?
        if (ctx.definable() != null) {
            ta.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        // typeCall
        if (ctx.typeCall() != null) {
            RTypeCall tc = (RTypeCall) visitTypeCall(ctx.typeCall());
            ta.setTypeCall(tc);
        }

        // condition*
        for (RosettaParser.ConditionContext condCtx : ctx.condition()) {
            RCondition cond = (RCondition) visitCondition(condCtx);
            ta.conditions().add(cond);
        }

        return ta;
    }

    /**
     * Visits a {@code rosettaBasicType} rule and builds an {@link RBasicType}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaBasicType:
     *     BASIC_TYPE validID typeParameters? definable?
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaBasicType(RosettaParser.RosettaBasicTypeContext ctx) {
        RBasicType bt = new RBasicType();
        bt.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // BASIC_TYPE keyword
        bt.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.BASIC_TYPE().getSymbol(), fileName, charToByte));

        // validID — name
        bt.setName(ctx.validID().getText());
        bt.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // typeParameters?
        if (ctx.typeParameters() != null) {
            for (RosettaParser.TypeParameterContext tpCtx : ctx.typeParameters().typeParameter()) {
                RTypeParameter tp = (RTypeParameter) visitTypeParameter(tpCtx);
                bt.typeParameters().add(tp);
            }
        }

        // definable?
        if (ctx.definable() != null) {
            bt.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        return bt;
    }

    /**
     * Visits a {@code rosettaRecordType} rule and builds an {@link RRecordType}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaRecordType:
     *     RECORD_TYPE validID LBRACE definable? rosettaRecordFeature* RBRACE
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaRecordType(RosettaParser.RosettaRecordTypeContext ctx) {
        RRecordType rt = new RRecordType();
        rt.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // RECORD_TYPE keyword
        rt.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.RECORD_TYPE().getSymbol(), fileName, charToByte));

        // validID — name
        rt.setName(ctx.validID().getText());
        rt.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // definable?
        if (ctx.definable() != null) {
            rt.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        // rosettaRecordFeature*
        for (RosettaParser.RosettaRecordFeatureContext fCtx : ctx.rosettaRecordFeature()) {
            RRecordFeature feature = (RRecordFeature) visitRosettaRecordFeature(fCtx);
            rt.features().add(feature);
        }

        return rt;
    }

    /**
     * Visits a {@code rosettaMetaType} rule and builds an {@link RMetaType}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaMetaType:
     *     META_TYPE validID typeCall
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaMetaType(RosettaParser.RosettaMetaTypeContext ctx) {
        RMetaType mt = new RMetaType();
        mt.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // META_TYPE keyword
        mt.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.META_TYPE().getSymbol(), fileName, charToByte));

        // validID — name
        mt.setName(ctx.validID().getText());
        mt.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // typeCall
        if (ctx.typeCall() != null) {
            RTypeCall tc = (RTypeCall) visitTypeCall(ctx.typeCall());
            mt.setTypeCall(tc);
        }

        return mt;
    }

    /**
     * Visits a {@code rosettaLibraryFunction} rule and builds an {@link RLibraryFunction}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaLibraryFunction:
     *     LIBRARY FUNCTION validID
     *     LPAREN (rosettaParameter (COMMA rosettaParameter)*)? RPAREN
     *     typeCall definable?
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaLibraryFunction(RosettaParser.RosettaLibraryFunctionContext ctx) {
        RLibraryFunction lf = new RLibraryFunction();
        lf.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // LIBRARY keyword
        lf.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.LIBRARY().getSymbol(), fileName, charToByte));

        // FUNCTION keyword
        lf.putTokenRange("functionKeyword",
                AstBuilderHelper.rangeOfToken(ctx.FUNCTION().getSymbol(), fileName, charToByte));

        // validID — name
        lf.setName(ctx.validID().getText());
        lf.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // (rosettaParameter (COMMA rosettaParameter)*)?
        for (RosettaParser.RosettaParameterContext pCtx : ctx.rosettaParameter()) {
            RParameter param = (RParameter) visitRosettaParameter(pCtx);
            lf.parameters().add(param);
        }

        // typeCall
        if (ctx.typeCall() != null) {
            RTypeCall tc = (RTypeCall) visitTypeCall(ctx.typeCall());
            lf.setReturnType(tc);
        }

        // definable?
        if (ctx.definable() != null) {
            lf.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        return lf;
    }

    // -- Supporting type visitors (Task 11) ------------------------------------

    /**
     * Visits an {@code attribute} rule and builds an {@link RAttribute}.
     *
     * <p>Grammar:
     * <pre>
     * attribute:
     *     OVERRIDE? validID typeCall rosettaCardinality? definable?
     *     (docReference | annotationRef | rosettaSynonym | labelAnnotation | ruleReferenceAnnotation)*
     * ;
     * </pre>
     */
    @Override
    public Object visitAttribute(RosettaParser.AttributeContext ctx) {
        RAttribute attr = new RAttribute();
        attr.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // OVERRIDE?
        if (ctx.OVERRIDE() != null) {
            attr.setOverride(true);
            attr.putTokenRange("override",
                    AstBuilderHelper.rangeOfToken(ctx.OVERRIDE().getSymbol(), fileName, charToByte));
        }

        // validID — name
        attr.setName(ctx.validID().getText());
        attr.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // typeCall
        if (ctx.typeCall() != null) {
            RTypeCall tc = (RTypeCall) visitTypeCall(ctx.typeCall());
            attr.setTypeCall(tc);
        }

        // rosettaCardinality?
        if (ctx.rosettaCardinality() != null) {
            RCardinality card = (RCardinality) visitRosettaCardinality(ctx.rosettaCardinality());
            attr.setCardinality(card);
        }

        // definable?
        if (ctx.definable() != null) {
            attr.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        // (docReference | annotationRef | rosettaSynonym | labelAnnotation | ruleReferenceAnnotation)*
        for (RosettaParser.DocReferenceContext drCtx : ctx.docReference()) {
            RDocReference docRef = (RDocReference) visitDocReference(drCtx);
            attr.docReferences().add(docRef);
        }
        for (RosettaParser.AnnotationRefContext arCtx : ctx.annotationRef()) {
            RAnnotationRef annRef = (RAnnotationRef) visitAnnotationRef(arCtx);
            attr.annotationRefs().add(annRef);
        }
        for (RosettaParser.RosettaSynonymContext synCtx : ctx.rosettaSynonym()) {
            RSynonym syn = (RSynonym) visitRosettaSynonym(synCtx);
            attr.synonyms().add(syn);
        }
        for (RosettaParser.LabelAnnotationContext laCtx : ctx.labelAnnotation()) {
            RLabelAnnotation label = (RLabelAnnotation) visitLabelAnnotation(laCtx);
            attr.labelAnnotations().add(label);
        }
        for (RosettaParser.RuleReferenceAnnotationContext rraCtx : ctx.ruleReferenceAnnotation()) {
            RRuleReferenceAnnotation ruleRef = (RRuleReferenceAnnotation) visitRuleReferenceAnnotation(rraCtx);
            attr.ruleReferenceAnnotations().add(ruleRef);
        }

        return attr;
    }

    /**
     * Visits a {@code rosettaEnumValue} rule and builds an {@link REnumValue}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaEnumValue:
     *     validID (DISPLAY_NAME STRING)? definable?
     *     (docReference | annotationRef | rosettaEnumSynonym)*
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaEnumValue(RosettaParser.RosettaEnumValueContext ctx) {
        REnumValue val = new REnumValue();
        val.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // validID — name
        val.setName(ctx.validID().getText());
        val.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // (DISPLAY_NAME STRING)?
        if (ctx.DISPLAY_NAME() != null && ctx.STRING() != null) {
            val.setDisplayName(AstBuilderHelper.stripQuotes(ctx.STRING().getText()));
            val.putTokenRange("displayName",
                    AstBuilderHelper.rangeOfToken(ctx.STRING().getSymbol(), fileName, charToByte));
        }

        // definable?
        if (ctx.definable() != null) {
            val.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        // (docReference | annotationRef | rosettaEnumSynonym)*
        for (RosettaParser.DocReferenceContext drCtx : ctx.docReference()) {
            RDocReference docRef = (RDocReference) visitDocReference(drCtx);
            val.docReferences().add(docRef);
        }
        for (RosettaParser.AnnotationRefContext arCtx : ctx.annotationRef()) {
            RAnnotationRef annRef = (RAnnotationRef) visitAnnotationRef(arCtx);
            val.annotationRefs().add(annRef);
        }
        for (RosettaParser.RosettaEnumSynonymContext esCtx : ctx.rosettaEnumSynonym()) {
            REnumSynonym es = (REnumSynonym) visitRosettaEnumSynonym(esCtx);
            val.synonyms().add(es);
        }

        return val;
    }

    /**
     * Visits a {@code choiceOption} rule and builds an {@link RChoiceOption}.
     *
     * <p>Grammar:
     * <pre>
     * choiceOption:
     *     typeCall definable?
     *     (docReference | annotationRef | rosettaSynonym | labelAnnotation | ruleReferenceAnnotation)*
     * ;
     * </pre>
     */
    @Override
    public Object visitChoiceOption(RosettaParser.ChoiceOptionContext ctx) {
        RChoiceOption opt = new RChoiceOption();
        opt.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // typeCall
        if (ctx.typeCall() != null) {
            RTypeCall tc = (RTypeCall) visitTypeCall(ctx.typeCall());
            opt.setTypeCall(tc);
        }

        // definable?
        if (ctx.definable() != null) {
            opt.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        // (docReference | annotationRef | rosettaSynonym | labelAnnotation | ruleReferenceAnnotation)*
        for (RosettaParser.DocReferenceContext drCtx : ctx.docReference()) {
            RDocReference docRef = (RDocReference) visitDocReference(drCtx);
            opt.docReferences().add(docRef);
        }
        for (RosettaParser.AnnotationRefContext arCtx : ctx.annotationRef()) {
            RAnnotationRef annRef = (RAnnotationRef) visitAnnotationRef(arCtx);
            opt.annotationRefs().add(annRef);
        }
        for (RosettaParser.RosettaSynonymContext synCtx : ctx.rosettaSynonym()) {
            RSynonym syn = (RSynonym) visitRosettaSynonym(synCtx);
            opt.synonyms().add(syn);
        }
        for (RosettaParser.LabelAnnotationContext laCtx : ctx.labelAnnotation()) {
            RLabelAnnotation label = (RLabelAnnotation) visitLabelAnnotation(laCtx);
            opt.labelAnnotations().add(label);
        }
        for (RosettaParser.RuleReferenceAnnotationContext rraCtx : ctx.ruleReferenceAnnotation()) {
            RRuleReferenceAnnotation ruleRef = (RRuleReferenceAnnotation) visitRuleReferenceAnnotation(rraCtx);
            opt.ruleReferenceAnnotations().add(ruleRef);
        }

        return opt;
    }

    /**
     * Visits a {@code typeCall} rule and builds an {@link RTypeCall}.
     *
     * <p>Grammar:
     * <pre>
     * typeCall:
     *     qualifiedName (LPAREN typeCallArgument (COMMA typeCallArgument)* RPAREN)?
     * ;
     * </pre>
     */
    @Override
    public Object visitTypeCall(RosettaParser.TypeCallContext ctx) {
        RTypeCall tc = new RTypeCall();
        tc.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // qualifiedName
        tc.setTypeName(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
        tc.putTokenRange("typeName",
                AstBuilderHelper.rangeOf(ctx.qualifiedName(), fileName, charToByte));

        // (LPAREN typeCallArgument (COMMA typeCallArgument)* RPAREN)?
        for (RosettaParser.TypeCallArgumentContext argCtx : ctx.typeCallArgument()) {
            RTypeCallArgument arg = (RTypeCallArgument) visitTypeCallArgument(argCtx);
            tc.arguments().add(arg);
        }

        return tc;
    }

    /**
     * Visits a {@code typeCallArgument} rule and builds an {@link RTypeCallArgument}.
     *
     * <p>Grammar:
     * <pre>
     * typeCallArgument:
     *     typeParameterValidID COLON typeCallArgumentExpression
     * ;
     * </pre>
     */
    @Override
    public Object visitTypeCallArgument(RosettaParser.TypeCallArgumentContext ctx) {
        RTypeCallArgument arg = new RTypeCallArgument();
        arg.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // typeParameterValidID — parameter name
        arg.setParameterName(ctx.typeParameterValidID().getText());
        arg.putTokenRange("parameterName",
                AstBuilderHelper.rangeOf(ctx.typeParameterValidID(), fileName, charToByte));

        // typeCallArgumentExpression
        if (ctx.typeCallArgumentExpression() != null) {
            RTypeCallArgumentExpression expr =
                    (RTypeCallArgumentExpression) visitTypeCallArgumentExpression(ctx.typeCallArgumentExpression());
            arg.setValue(expr);
        }

        return arg;
    }

    /**
     * Visits a {@code typeCallArgumentExpression} rule and builds an
     * {@link RTypeCallArgumentExpression}.
     *
     * <p>Grammar:
     * <pre>
     * typeCallArgumentExpression:
     *     typeParameterValidID
     *     | MINUS? literal
     * ;
     * </pre>
     */
    @Override
    public Object visitTypeCallArgumentExpression(RosettaParser.TypeCallArgumentExpressionContext ctx) {
        RTypeCallArgumentExpression expr = new RTypeCallArgumentExpression();
        expr.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        if (ctx.typeParameterValidID() != null) {
            // Name reference
            expr.setNameValue(ctx.typeParameterValidID().getText());
        } else {
            // MINUS? literal
            if (ctx.MINUS() != null) {
                expr.setNegated(true);
            }
            if (ctx.literal() != null) {
                expr.setLiteralValue(ctx.literal().getText());
            }
        }

        return expr;
    }

    /**
     * Visits a {@code rosettaCardinality} rule and builds an {@link RCardinality}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaCardinality:
     *     LPAREN INT_LITERAL DOT_DOT (INT_LITERAL | STAR) RPAREN
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaCardinality(RosettaParser.RosettaCardinalityContext ctx) {
        RCardinality card = new RCardinality();
        card.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // inf = first INT_LITERAL — parsed as BigInteger to handle
        // arbitrary-precision integers (INT_LITERAL is unbounded in the
        // grammar — same fix as RIntLiteral and the synonym/map int fields).
        card.setInf(new BigInteger(ctx.INT_LITERAL(0).getText()));

        // sup = second INT_LITERAL or STAR
        if (ctx.STAR() != null) {
            // setUnbounded(true) also sets sup to UNBOUNDED via the invariant
            card.setUnbounded(true);
        } else {
            // setSup(non-UNBOUNDED) also clears the unbounded flag via the invariant
            card.setSup(new BigInteger(ctx.INT_LITERAL(1).getText()));
        }

        return card;
    }

    /**
     * Visits a {@code typeParameter} rule and builds an {@link RTypeParameter}.
     *
     * <p>Grammar:
     * <pre>
     * typeParameter:
     *     typeParameterValidID typeCall definable?
     * ;
     * </pre>
     */
    @Override
    public Object visitTypeParameter(RosettaParser.TypeParameterContext ctx) {
        RTypeParameter tp = new RTypeParameter();
        tp.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // typeParameterValidID — name
        tp.setName(ctx.typeParameterValidID().getText());
        tp.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.typeParameterValidID(), fileName, charToByte));

        // typeCall
        if (ctx.typeCall() != null) {
            RTypeCall tc = (RTypeCall) visitTypeCall(ctx.typeCall());
            tp.setTypeCall(tc);
        }

        // definable?
        if (ctx.definable() != null) {
            tp.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        return tp;
    }

    /**
     * Visits a {@code rosettaRecordFeature} rule and builds an {@link RRecordFeature}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaRecordFeature:
     *     validID typeCall
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaRecordFeature(RosettaParser.RosettaRecordFeatureContext ctx) {
        RRecordFeature feature = new RRecordFeature();
        feature.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // validID — name
        feature.setName(ctx.validID().getText());
        feature.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // typeCall
        if (ctx.typeCall() != null) {
            RTypeCall tc = (RTypeCall) visitTypeCall(ctx.typeCall());
            feature.setTypeCall(tc);
        }

        return feature;
    }

    /**
     * Visits a {@code rosettaParameter} rule and builds an {@link RParameter}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaParameter:
     *     validID typeCall (LBRACK RBRACK)?
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaParameter(RosettaParser.RosettaParameterContext ctx) {
        RParameter param = new RParameter();
        param.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // validID — name
        param.setName(ctx.validID().getText());
        param.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // typeCall
        if (ctx.typeCall() != null) {
            RTypeCall tc = (RTypeCall) visitTypeCall(ctx.typeCall());
            param.setTypeCall(tc);
        }

        // (LBRACK RBRACK)?
        if (ctx.LBRACK() != null && ctx.RBRACK() != null) {
            param.setArray(true);
        }

        return param;
    }

    // -- Function visitors (Task 12) ------------------------------------------

    /**
     * Visits a {@code function} rule and builds an {@link RFunction}.
     *
     * <p>Grammar:
     * <pre>
     * function:
     *     FUNC validID (LPAREN validID COLON enumValueReference RPAREN)?
     *     (EXTENDS qualifiedName)?
     *     COLON definable?
     *     (docReference | annotationRef)*
     *     (INPUTS COLON attribute+)?
     *     (OUTPUT COLON attribute)?
     *     shortcutDeclaration*
     *     condition*
     *     operation*
     *     postCondition*
     * ;
     * </pre>
     */
    @Override
    public Object visitFunction(RosettaParser.FunctionContext ctx) {
        RFunction fn = new RFunction();
        fn.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // runeAnnotations? (P1.4.2 H2 — prefix attach)
        attachRuneAnnotations(fn, ctx.runeAnnotations());

        // FUNC keyword
        fn.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.FUNC().getSymbol(), fileName, charToByte));

        // validID(0) — function name
        fn.setName(ctx.validID(0).getText());
        fn.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(0), fileName, charToByte));

        // (LPAREN validID COLON enumValueReference RPAREN)? — dispatch
        if (ctx.LPAREN() != null && ctx.enumValueReference() != null) {
            RDispatch dispatch = new RDispatch();
            // Source range covers full dispatch clause: (paramName : EnumType -> value)
            var lparen = ctx.LPAREN().getSymbol();
            var rparen = ctx.RPAREN().getSymbol();
            dispatch.setSourceRange(rangeOfTokenSpan(lparen, rparen));

            // validID(1) = dispatch param name
            dispatch.setParamName(ctx.validID(1).getText());

            // enumValueReference: qualifiedName ARROW validID
            var evr = ctx.enumValueReference();
            dispatch.setEnumRef(AstBuilderHelper.textOfQualifiedName(evr.qualifiedName()));
            dispatch.setValueName(evr.validID().getText());

            fn.setDispatch(dispatch);
        }

        // (EXTENDS qualifiedName)?
        if (ctx.EXTENDS() != null && ctx.qualifiedName() != null) {
            fn.setSuperFunctionName(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
            fn.putTokenRange("extends",
                    AstBuilderHelper.rangeOfToken(ctx.EXTENDS().getSymbol(), fileName, charToByte));
            fn.putTokenRange("superFunction",
                    AstBuilderHelper.rangeOf(ctx.qualifiedName(), fileName, charToByte));
        }

        // definable?
        if (ctx.definable() != null) {
            fn.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        // (docReference | annotationRef)*
        for (RosettaParser.DocReferenceContext drCtx : ctx.docReference()) {
            RDocReference docRef = (RDocReference) visitDocReference(drCtx);
            fn.docReferences().add(docRef);
        }
        for (RosettaParser.AnnotationRefContext arCtx : ctx.annotationRef()) {
            RAnnotationRef annRef = (RAnnotationRef) visitAnnotationRef(arCtx);
            fn.annotationRefs().add(annRef);
        }

        // (INPUTS COLON attribute+)? (OUTPUT COLON attribute)?
        // All attributes are in a single list. If OUTPUT is present, last attribute is output.
        List<RosettaParser.AttributeContext> allAttrs = ctx.attribute();
        boolean hasOutput = ctx.OUTPUT() != null;
        boolean hasInputs = ctx.INPUTS() != null;

        if (!allAttrs.isEmpty()) {
            int outputIndex = hasOutput ? allAttrs.size() - 1 : -1;
            for (int i = 0; i < allAttrs.size(); i++) {
                RAttribute attr = (RAttribute) visitAttribute(allAttrs.get(i));
                if (i == outputIndex) {
                    fn.setOutput(attr);
                } else {
                    fn.inputs().add(attr);
                }
            }
        }

        // shortcutDeclaration*
        for (RosettaParser.ShortcutDeclarationContext scCtx : ctx.shortcutDeclaration()) {
            RShortcut sc = (RShortcut) visitShortcutDeclaration(scCtx);
            fn.shortcuts().add(sc);
        }

        // condition*
        for (RosettaParser.ConditionContext condCtx : ctx.condition()) {
            RCondition cond = (RCondition) visitCondition(condCtx);
            fn.conditions().add(cond);
        }

        // operation*
        for (RosettaParser.OperationContext opCtx : ctx.operation()) {
            ROperation op = (ROperation) visitOperation(opCtx);
            fn.operations().add(op);
        }

        // postCondition*
        for (RosettaParser.PostConditionContext pcCtx : ctx.postCondition()) {
            RPostCondition pc = (RPostCondition) visitPostCondition(pcCtx);
            fn.postConditions().add(pc);
        }

        return fn;
    }

    /**
     * Visits a {@code shortcutDeclaration} rule and builds an {@link RShortcut}.
     *
     * <p>Grammar: {@code ALIAS validID COLON definable? expression}
     */
    @Override
    public Object visitShortcutDeclaration(RosettaParser.ShortcutDeclarationContext ctx) {
        RShortcut sc = new RShortcut();
        sc.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // ALIAS keyword token range
        sc.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.ALIAS().getSymbol(), fileName, charToByte));

        // validID — name
        sc.setName(ctx.validID().getText());
        sc.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // COLON token range
        sc.putTokenRange("colon",
                AstBuilderHelper.rangeOfToken(ctx.COLON().getSymbol(), fileName, charToByte));

        // definable?
        if (ctx.definable() != null) {
            sc.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        // exprWithThen — then-chain wrapper over the then-free expression rule
        if (ctx.exprWithThen() != null) {
            RExpression expr = buildExprWithThen(ctx.exprWithThen());
            if (expr != null) {
                sc.setExpression(expr);
            }
        }

        return sc;
    }

    /**
     * Visits a {@code condition} rule and builds an {@link RCondition}.
     *
     * <p>Grammar: {@code CONDITION validID? COLON definable? (docReference|annotationRef)* expression}
     */
    @Override
    public Object visitCondition(RosettaParser.ConditionContext ctx) {
        RCondition cond = new RCondition();
        cond.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // CONDITION keyword
        cond.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.CONDITION().getSymbol(), fileName, charToByte));

        // validID? — optional name
        if (ctx.validID() != null) {
            cond.setName(ctx.validID().getText());
            cond.putTokenRange("name",
                    AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));
        }

        // definable?
        if (ctx.definable() != null) {
            cond.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        // (docReference | annotationRef)*
        for (RosettaParser.DocReferenceContext drCtx : ctx.docReference()) {
            RDocReference docRef = (RDocReference) visitDocReference(drCtx);
            cond.docReferences().add(docRef);
        }
        for (RosettaParser.AnnotationRefContext arCtx : ctx.annotationRef()) {
            RAnnotationRef annRef = (RAnnotationRef) visitAnnotationRef(arCtx);
            cond.annotationRefs().add(annRef);
        }

        // exprWithThen — then-chain wrapper over the then-free expression rule
        if (ctx.exprWithThen() != null) {
            RExpression expr = buildExprWithThen(ctx.exprWithThen());
            if (expr != null) {
                cond.setExpression(expr);
            }
            // Capture the expression's normalized source token text (Xtext
            // getTokenText semantics) — the datarule DEFINITION source string
            // (upstream extractNodeText on the CONDITION__EXPRESSION feature).
            cond.setExpressionText(AstBuilderHelper.tokenText(ctx.exprWithThen()));
        }

        return cond;
    }

    /**
     * Visits a {@code postCondition} rule and builds an {@link RPostCondition}.
     *
     * <p>Grammar: {@code POST_CONDITION validID? COLON definable? expression}
     */
    @Override
    public Object visitPostCondition(RosettaParser.PostConditionContext ctx) {
        RPostCondition pc = new RPostCondition();
        pc.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // POST_CONDITION keyword
        pc.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.POST_CONDITION().getSymbol(), fileName, charToByte));

        // validID? — optional name
        if (ctx.validID() != null) {
            pc.setName(ctx.validID().getText());
            pc.putTokenRange("name",
                    AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));
        }

        // definable?
        if (ctx.definable() != null) {
            pc.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        // exprWithThen — then-chain wrapper over the then-free expression rule
        if (ctx.exprWithThen() != null) {
            RExpression expr = buildExprWithThen(ctx.exprWithThen());
            if (expr != null) {
                pc.setExpression(expr);
            }
        }

        return pc;
    }

    /**
     * Visits an {@code operation} rule and builds an {@link ROperation}.
     *
     * <p>Grammar: {@code (SET|ADD) validID segment? COLON definable? expressionWithAsKey}
     */
    @Override
    public Object visitOperation(RosettaParser.OperationContext ctx) {
        ROperation op = new ROperation();
        op.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // SET | ADD
        if (ctx.SET() != null) {
            op.setOperator(OperationOp.SET);
            op.putTokenRange("keyword",
                    AstBuilderHelper.rangeOfToken(ctx.SET().getSymbol(), fileName, charToByte));
        } else {
            op.setOperator(OperationOp.ADD);
            op.putTokenRange("keyword",
                    AstBuilderHelper.rangeOfToken(ctx.ADD().getSymbol(), fileName, charToByte));
        }

        // validID — target name
        op.setTargetName(ctx.validID().getText());
        op.putTokenRange("targetName",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // segment?
        if (ctx.segment() != null) {
            RSegment seg = (RSegment) visitSegment(ctx.segment());
            op.setSegment(seg);
        }

        // definable?
        if (ctx.definable() != null) {
            op.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        // expressionWithAsKey: expression AS_KEY?
        if (ctx.expressionWithAsKey() != null) {
            var ewak = ctx.expressionWithAsKey();
            if (ewak.AS_KEY() != null) {
                op.setAsKey(true);
            }
            if (ewak.exprWithThen() != null) {
                RExpression expr = buildExprWithThen(ewak.exprWithThen());
                if (expr != null) {
                    op.setExpression(expr);
                }
            }
        }

        return op;
    }

    /**
     * Visits a {@code segment} rule and builds an {@link RSegment}.
     *
     * <p>Grammar: {@code ARROW validID segment?}
     */
    @Override
    public Object visitSegment(RosettaParser.SegmentContext ctx) {
        RSegment seg = new RSegment();
        seg.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // validID — name
        seg.setName(ctx.validID().getText());
        seg.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // segment? — recursive next
        if (ctx.segment() != null) {
            RSegment next = (RSegment) visitSegment(ctx.segment());
            seg.setNext(next);
        }

        return seg;
    }

    // -- Rule visitor (Task 12) -----------------------------------------------

    /**
     * Visits a {@code rosettaRule} rule and builds an {@link RRule}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaRule:
     *     (REPORTING|ELIGIBILITY) RULE validID (FROM typeCall)?
     *     COLON definable? docReference* (expression (AS STRING)?)?
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaRule(RosettaParser.RosettaRuleContext ctx) {
        RRule rule = new RRule();
        rule.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // runeAnnotations? (P1.4.2 H11 — prefix attach, free reuse of H2 infra)
        attachRuneAnnotations(rule, ctx.runeAnnotations());

        // REPORTING | ELIGIBILITY
        if (ctx.REPORTING() != null) {
            rule.setKind(RuleKind.REPORTING);
            rule.putTokenRange("keyword",
                    AstBuilderHelper.rangeOfToken(ctx.REPORTING().getSymbol(), fileName, charToByte));
        } else {
            rule.setKind(RuleKind.ELIGIBILITY);
            rule.putTokenRange("keyword",
                    AstBuilderHelper.rangeOfToken(ctx.ELIGIBILITY().getSymbol(), fileName, charToByte));
        }

        // RULE keyword
        rule.putTokenRange("ruleKeyword",
                AstBuilderHelper.rangeOfToken(ctx.RULE().getSymbol(), fileName, charToByte));

        // validID — name
        rule.setName(ctx.validID().getText());
        rule.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // (FROM typeCall)?
        if (ctx.FROM() != null && ctx.typeCall() != null) {
            RTypeCall tc = (RTypeCall) visitTypeCall(ctx.typeCall());
            rule.setFromType(tc);
        }

        // definable?
        if (ctx.definable() != null) {
            rule.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        // docReference*
        for (RosettaParser.DocReferenceContext drCtx : ctx.docReference()) {
            RDocReference docRef = (RDocReference) visitDocReference(drCtx);
            rule.docReferences().add(docRef);
        }

        // exprWithThen? (AS STRING)?
        if (ctx.exprWithThen() != null) {
            RExpression expr = buildExprWithThen(ctx.exprWithThen());
            if (expr != null) {
                rule.setExpression(expr);
            }
        }
        if (ctx.AS() != null && ctx.STRING() != null) {
            rule.setAlias(AstBuilderHelper.stripQuotes(ctx.STRING().getText()));
        }

        return rule;
    }

    // -- Annotation visitors (Task 12) ----------------------------------------

    /**
     * Visits an {@code annotationDecl} rule and builds an {@link RAnnotation}.
     *
     * <p>Grammar:
     * <pre>
     * annotationDecl:
     *     ANNOTATION validID COLON definable?
     *     (LBRACK PREFIX validID RBRACK)?
     *     attribute*
     * ;
     * </pre>
     */
    @Override
    public Object visitAnnotationDecl(RosettaParser.AnnotationDeclContext ctx) {
        RAnnotation ann = new RAnnotation();
        ann.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // ANNOTATION keyword
        ann.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.ANNOTATION().getSymbol(), fileName, charToByte));

        // validID(0) — name
        ann.setName(ctx.validID(0).getText());
        ann.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(0), fileName, charToByte));

        // definable?
        if (ctx.definable() != null) {
            ann.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        // (LBRACK PREFIX validID RBRACK)?
        if (ctx.PREFIX() != null) {
            // validID(1) = prefix name
            ann.setPrefix(ctx.validID(1).getText());
        }

        // attribute*
        for (RosettaParser.AttributeContext attrCtx : ctx.attribute()) {
            RAttribute attr = (RAttribute) visitAttribute(attrCtx);
            ann.attributes().add(attr);
        }

        return ann;
    }

    /**
     * Visits an {@code annotationRef} rule and builds an {@link RAnnotationRef}.
     *
     * <p>Grammar: {@code LBRACK validID (validID annotationQualifier*)? RBRACK}
     */
    @Override
    public Object visitAnnotationRef(RosettaParser.AnnotationRefContext ctx) {
        RAnnotationRef ref = new RAnnotationRef();
        ref.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // validID(0) — annotation name
        ref.setAnnotationName(ctx.validID(0).getText());

        // validID(1)? — qualifier name
        if (ctx.validID().size() > 1) {
            ref.setQualifierName(ctx.validID(1).getText());
        }

        // annotationQualifier*
        for (RosettaParser.AnnotationQualifierContext aqCtx : ctx.annotationQualifier()) {
            RAnnotationQualifier q = (RAnnotationQualifier) visitAnnotationQualifier(aqCtx);
            ref.qualifiers().add(q);
        }

        return ref;
    }

    /**
     * Visits an {@code annotationQualifier} rule and builds an {@link RAnnotationQualifier}.
     *
     * <p>Grammar: {@code STRING EQ (STRING | rosettaAttributeReference)}
     */
    @Override
    public Object visitAnnotationQualifier(RosettaParser.AnnotationQualifierContext ctx) {
        RAnnotationQualifier q = new RAnnotationQualifier();
        q.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // STRING(0) — key
        q.setKey(AstBuilderHelper.stripQuotes(ctx.STRING(0).getText()));

        // STRING(1) | rosettaAttributeReference
        if (ctx.rosettaAttributeReference() != null) {
            // rosettaAttributeReference: qualifiedName (ARROW validID)+
            var rar = ctx.rosettaAttributeReference();
            var sb = new StringBuilder();
            sb.append(AstBuilderHelper.textOfQualifiedName(rar.qualifiedName()));
            for (var vid : rar.validID()) {
                sb.append(" -> ").append(vid.getText());
            }
            q.setValue(sb.toString());
            q.setAttributeRef(true);
        } else if (ctx.STRING().size() > 1) {
            q.setValue(AstBuilderHelper.stripQuotes(ctx.STRING(1).getText()));
            q.setAttributeRef(false);
        }

        return q;
    }

    // ========================================================================
    // Rune annotations (P1.4.2 H2 / H11) — @-prefix syntax distinct from the
    // legacy bracket-style annotationRef.
    // ========================================================================

    /**
     * Attaches {@code runeAnnotations?} prefix to a {@link RRootElement} subclass.
     * No-op when the optional prefix is absent. Called from each H2 / H11 attach
     * site visitor (visitDataType, visitChoice, visitEnumeration, visitFunction,
     * visitRosettaRule).
     */
    private void attachRuneAnnotations(RRootElement root,
                                       RosettaParser.RuneAnnotationsContext ctx) {
        if (ctx == null) {
            return;
        }
        for (RosettaParser.RuneAnnotationContext annCtx : ctx.runeAnnotation()) {
            root.addRuneAnnotation((RRuneAnnotation) visitRuneAnnotation(annCtx));
        }
    }

    /**
     * Visits a {@code runeAnnotation} rule and builds an {@link RRuneAnnotation}.
     *
     * <p>Grammar (P1.4.2 H2):
     * <pre>
     * runeAnnotation:
     *     AT qualifiedName (LPAREN runeAnnotationArgs? RPAREN)?
     * ;
     * </pre>
     */
    @Override
    public Object visitRuneAnnotation(RosettaParser.RuneAnnotationContext ctx) {
        RRuneAnnotation node = new RRuneAnnotation();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setAnnotationName(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
        node.putTokenRange("at",
                AstBuilderHelper.rangeOfToken(ctx.AT().getSymbol(), fileName, charToByte));
        node.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.qualifiedName(), fileName, charToByte));

        if (ctx.runeAnnotationArgs() != null) {
            for (RosettaParser.RuneAnnotationArgContext argCtx
                    : ctx.runeAnnotationArgs().runeAnnotationArg()) {
                node.arguments().add((RRuneAnnotationArg) visitRuneAnnotationArg(argCtx));
            }
        }
        return node;
    }

    /**
     * Visits a {@code runeAnnotationArg} rule and builds an {@link RRuneAnnotationArg}.
     *
     * <p>Grammar (P1.4.2 H2):
     * <pre>
     * runeAnnotationArg : validID EQ runeAnnotationLiteral ;
     * </pre>
     *
     * <p>The literal value is projected to a string for Layer-1 simplicity:
     * STRING tokens are unquoted; numeric / boolean / qualifiedName literals
     * are stored verbatim from the source.
     */
    @Override
    public Object visitRuneAnnotationArg(RosettaParser.RuneAnnotationArgContext ctx) {
        RRuneAnnotationArg arg = new RRuneAnnotationArg();
        arg.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        arg.setName(ctx.validID().getText());
        arg.setValueAsString(textOfRuneAnnotationLiteral(ctx.runeAnnotationLiteral()));
        return arg;
    }

    /**
     * Projects a {@code runeAnnotationLiteral} parse-tree to its string form.
     * STRING is unquoted; numerics/booleans use raw source text; qualifiedName
     * is dotted-form text.
     */
    private static String textOfRuneAnnotationLiteral(
            RosettaParser.RuneAnnotationLiteralContext ctx) {
        if (ctx.literal() != null) {
            RosettaParser.LiteralContext lit = ctx.literal();
            if (lit.STRING() != null) {
                return AstBuilderHelper.stripQuotes(lit.STRING().getText());
            }
            // INT_LITERAL / BIG_DECIMAL / TRUE / FALSE — raw text
            return lit.getText();
        }
        if (ctx.qualifiedName() != null) {
            return AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName());
        }
        throw new AstBuildException(
                "runeAnnotationLiteral matched no grammar alternative: " + ctx.getText());
    }

    /**
     * Visits a {@code labelAnnotation} rule and builds an {@link RLabelAnnotation}.
     *
     * <p>Grammar:
     * <pre>
     * labelAnnotation:
     *     LBRACK LABEL (FOR annotationPathExpression | annotationPathExpression? AS)? STRING RBRACK
     * ;
     * </pre>
     */
    @Override
    public Object visitLabelAnnotation(RosettaParser.LabelAnnotationContext ctx) {
        RLabelAnnotation la = new RLabelAnnotation();
        la.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // STRING — label text
        la.setLabel(AstBuilderHelper.stripQuotes(ctx.STRING().getText()));

        // FOR annotationPathExpression
        if (ctx.FOR() != null && ctx.annotationPathExpression() != null) {
            RAnnotationPathExpression forPath =
                    (RAnnotationPathExpression) visitAnnotationPathExpression(ctx.annotationPathExpression());
            la.setForPath(forPath);
        }

        // annotationPathExpression? AS (without FOR)
        if (ctx.AS() != null && ctx.annotationPathExpression() != null && ctx.FOR() == null) {
            RAnnotationPathExpression asPath =
                    (RAnnotationPathExpression) visitAnnotationPathExpression(ctx.annotationPathExpression());
            la.setAsPath(asPath);
        }

        return la;
    }

    /**
     * Visits a {@code ruleReferenceAnnotation} rule and builds an
     * {@link RRuleReferenceAnnotation}.
     *
     * <p>Grammar:
     * <pre>
     * ruleReferenceAnnotation:
     *     LBRACK RULE_REFERENCE (FOR annotationPathExpression)?
     *     (qualifiedName | EMPTY) RBRACK
     * ;
     * </pre>
     */
    @Override
    public Object visitRuleReferenceAnnotation(RosettaParser.RuleReferenceAnnotationContext ctx) {
        RRuleReferenceAnnotation rra = new RRuleReferenceAnnotation();
        rra.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // (FOR annotationPathExpression)?
        if (ctx.FOR() != null && ctx.annotationPathExpression() != null) {
            RAnnotationPathExpression forPath =
                    (RAnnotationPathExpression) visitAnnotationPathExpression(ctx.annotationPathExpression());
            rra.setForPath(forPath);
        }

        // qualifiedName | EMPTY
        if (ctx.qualifiedName() != null) {
            rra.setRuleName(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
        }
        // If EMPTY, ruleName stays null

        return rra;
    }

    /**
     * Visits an {@code annotationPathExpression} rule and builds an
     * {@link RAnnotationPathExpression}.
     *
     * <p>Grammar: {@code (validID | ITEM) (ARROW validID | DEEP_ARROW validID)*}
     */
    @Override
    public Object visitAnnotationPathExpression(RosettaParser.AnnotationPathExpressionContext ctx) {
        RAnnotationPathExpression path = new RAnnotationPathExpression();
        path.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // (validID | ITEM) — root
        if (ctx.ITEM() != null) {
            path.setRoot("item");
            path.setRootIsItem(true);
        } else {
            // validID(0) is the root
            path.setRoot(ctx.validID(0).getText());
            path.setRootIsItem(false);
        }

        // (ARROW validID | DEEP_ARROW validID)* — segments
        // The remaining validIDs after the root (if not ITEM) are segment names.
        // ARROW and DEEP_ARROW lists tell us which operator was used.
        int arrowIdx = 0;
        int deepArrowIdx = 0;
        List<RosettaParser.ValidIDContext> allIds = ctx.validID();
        // If root is not ITEM, root is validID(0), segments start at validID(1)
        // If root is ITEM, all validIDs are segment names
        int startIdx = (ctx.ITEM() != null) ? 0 : 1;

        // We need to iterate through the segments in order.
        // The grammar uses ARROW and DEEP_ARROW interleaved with validIDs.
        // Total segments = (number of ARROW tokens) + (number of DEEP_ARROW tokens)
        int totalArrows = ctx.ARROW().size();
        int totalDeepArrows = ctx.DEEP_ARROW().size();
        int totalSegments = totalArrows + totalDeepArrows;

        if (totalSegments > 0) {
            // Get all arrow/deep-arrow tokens in their original order
            // by examining their token positions
            for (int i = startIdx; i < allIds.size(); i++) {
                RAnnotationPathSegment seg = new RAnnotationPathSegment();
                seg.setSourceRange(AstBuilderHelper.rangeOf(allIds.get(i), fileName, charToByte));
                seg.setName(allIds.get(i).getText());

                // Determine if this segment uses ARROW or DEEP_ARROW
                // by comparing token indices
                boolean isDeep = false;
                if (arrowIdx < totalArrows && deepArrowIdx < totalDeepArrows) {
                    // Both still available — check which comes first
                    int arrowTokenIdx = ctx.ARROW(arrowIdx).getSymbol().getTokenIndex();
                    int deepTokenIdx = ctx.DEEP_ARROW(deepArrowIdx).getSymbol().getTokenIndex();
                    if (deepTokenIdx < arrowTokenIdx) {
                        isDeep = true;
                        deepArrowIdx++;
                    } else {
                        arrowIdx++;
                    }
                } else if (deepArrowIdx < totalDeepArrows) {
                    isDeep = true;
                    deepArrowIdx++;
                } else {
                    arrowIdx++;
                }

                seg.setDeep(isDeep);
                path.segments().add(seg);
            }
        }

        return path;
    }

    // -- Regulatory visitors (Task 12) ----------------------------------------

    /**
     * Visits a {@code rosettaBody} rule and builds an {@link RBody}.
     *
     * <p>Grammar: {@code BODY ID validID definable?}
     */
    @Override
    public Object visitRosettaBody(RosettaParser.RosettaBodyContext ctx) {
        RBody body = new RBody();
        body.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // BODY keyword
        body.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.BODY().getSymbol(), fileName, charToByte));

        // ID — body type keyword (e.g., "Authority")
        body.setBodyTypeKeyword(ctx.ID().getText());
        body.putTokenRange("bodyType",
                AstBuilderHelper.rangeOfToken(ctx.ID().getSymbol(), fileName, charToByte));

        // validID — name
        body.setName(ctx.validID().getText());
        body.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // definable?
        if (ctx.definable() != null) {
            body.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        return body;
    }

    /**
     * Visits a {@code rosettaCorpus} rule and builds an {@link RCorpus}.
     *
     * <p>Grammar: {@code CORPUS ID qualifiedName? STRING? validID definable?}
     */
    @Override
    public Object visitRosettaCorpus(RosettaParser.RosettaCorpusContext ctx) {
        RCorpus corpus = new RCorpus();
        corpus.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // CORPUS keyword
        corpus.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.CORPUS().getSymbol(), fileName, charToByte));

        // ID — corpus type keyword
        corpus.setCorpusTypeKeyword(ctx.ID().getText());
        corpus.putTokenRange("corpusType",
                AstBuilderHelper.rangeOfToken(ctx.ID().getSymbol(), fileName, charToByte));

        // qualifiedName? — body reference
        if (ctx.qualifiedName() != null) {
            corpus.setBodyRef(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
        }

        // STRING? — display name
        if (ctx.STRING() != null) {
            corpus.setDisplayName(AstBuilderHelper.stripQuotes(ctx.STRING().getText()));
        }

        // validID — name
        corpus.setName(ctx.validID().getText());
        corpus.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        // definable?
        if (ctx.definable() != null) {
            corpus.setDefinition(AstBuilderHelper.textOfDefinable(ctx.definable()));
        }

        return corpus;
    }

    /**
     * Visits a {@code rosettaSegment} rule and builds an {@link RSegmentDef}.
     *
     * <p>Grammar: {@code SEGMENT (validID | RATIONALE | RATIONALE_AUTHOR | STRUCTURED_PROVISION)}
     */
    @Override
    public Object visitRosettaSegment(RosettaParser.RosettaSegmentContext ctx) {
        RSegmentDef seg = new RSegmentDef();
        seg.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // SEGMENT keyword
        seg.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.SEGMENT().getSymbol(), fileName, charToByte));

        // (validID | RATIONALE | RATIONALE_AUTHOR | STRUCTURED_PROVISION)
        if (ctx.validID() != null) {
            seg.setName(ctx.validID().getText());
        } else if (ctx.RATIONALE() != null) {
            seg.setName(ctx.RATIONALE().getText());
        } else if (ctx.RATIONALE_AUTHOR() != null) {
            seg.setName(ctx.RATIONALE_AUTHOR().getText());
        } else if (ctx.STRUCTURED_PROVISION() != null) {
            seg.setName(ctx.STRUCTURED_PROVISION().getText());
        }

        return seg;
    }

    /**
     * Visits a {@code docReference} rule and builds an {@link RDocReference}.
     *
     * <p>Grammar:
     * <pre>
     * docReference:
     *     LBRACK (REGULATORY_REFERENCE|DOC_REFERENCE)
     *     (FOR annotationPathExpression)?
     *     regulatoryDocumentReference
     *     documentRationale*
     *     (STRUCTURED_PROVISION STRING)?
     *     (PROVISION STRING)?
     *     REPORTED_FIELD?
     *     RBRACK
     * ;
     * </pre>
     */
    @Override
    public Object visitDocReference(RosettaParser.DocReferenceContext ctx) {
        RDocReference ref = new RDocReference();
        ref.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // REGULATORY_REFERENCE | DOC_REFERENCE — the keyword token range feeds
        // the deprecation warning's anchor (upstream sites the issue at the
        // keyword, one past the opening bracket; PR #458 anchor wave).
        ref.setRegulatoryReference(ctx.REGULATORY_REFERENCE() != null);
        var docRefKeyword = ctx.REGULATORY_REFERENCE() != null
                ? ctx.REGULATORY_REFERENCE()
                : ctx.DOC_REFERENCE();
        if (docRefKeyword != null) {
            ref.putTokenRange("keyword",
                    AstBuilderHelper.rangeOfToken(docRefKeyword.getSymbol(), fileName, charToByte));
        }

        // (FOR annotationPathExpression)?
        if (ctx.FOR() != null && ctx.annotationPathExpression() != null) {
            RAnnotationPathExpression forPath =
                    (RAnnotationPathExpression) visitAnnotationPathExpression(ctx.annotationPathExpression());
            ref.setForPath(forPath);
        }

        // regulatoryDocumentReference
        if (ctx.regulatoryDocumentReference() != null) {
            RRegulatoryDocumentReference regDocRef =
                    (RRegulatoryDocumentReference) visitRegulatoryDocumentReference(ctx.regulatoryDocumentReference());
            ref.setRegulatoryDocRef(regDocRef);
        }

        // regulatoryReferenceArgs? (P1.4.2 H4)
        if (ctx.regulatoryReferenceArgs() != null) {
            for (RosettaParser.NamedArgContext argCtx : ctx.regulatoryReferenceArgs().namedArg()) {
                com.regnosys.rosetta.ast.regulatory.RRegulatoryReferenceArg arg =
                        new com.regnosys.rosetta.ast.regulatory.RRegulatoryReferenceArg();
                arg.setSourceRange(AstBuilderHelper.rangeOf(argCtx, fileName, charToByte));
                arg.setName(argCtx.validID().getText());
                arg.setValue(AstBuilderHelper.stripQuotes(argCtx.STRING().getText()));
                ref.addNamedArg(arg);
            }
        }

        // documentRationale*
        for (RosettaParser.DocumentRationaleContext drCtx : ctx.documentRationale()) {
            RDocumentRationale rationale = (RDocumentRationale) visitDocumentRationale(drCtx);
            ref.rationales().add(rationale);
        }

        // (STRUCTURED_PROVISION STRING)? (PROVISION STRING)?
        // STRING list contains structured provision first, then provision
        // We need to match by keyword presence
        if (ctx.STRUCTURED_PROVISION() != null) {
            // First STRING after STRUCTURED_PROVISION
            int strIdx = 0;
            ref.setStructuredProvision(AstBuilderHelper.stripQuotes(ctx.STRING(strIdx).getText()));
            if (ctx.PROVISION() != null) {
                ref.setProvision(AstBuilderHelper.stripQuotes(ctx.STRING(strIdx + 1).getText()));
            }
        } else if (ctx.PROVISION() != null) {
            ref.setProvision(AstBuilderHelper.stripQuotes(ctx.STRING(0).getText()));
        }

        // REPORTED_FIELD?
        if (ctx.REPORTED_FIELD() != null) {
            ref.setReportedField(true);
        }

        return ref;
    }

    /**
     * Visits a {@code regulatoryDocumentReference} rule and builds an
     * {@link RRegulatoryDocumentReference}.
     *
     * <p>Grammar: {@code qualifiedName qualifiedName+ rosettaSegmentRef*}
     */
    @Override
    public Object visitRegulatoryDocumentReference(RosettaParser.RegulatoryDocumentReferenceContext ctx) {
        RRegulatoryDocumentReference rdr = new RRegulatoryDocumentReference();
        rdr.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // qualifiedName(0) — body reference
        rdr.setBodyRef(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName(0)));

        // qualifiedName(1..n) — corpus references
        for (int i = 1; i < ctx.qualifiedName().size(); i++) {
            rdr.corpusRefs().add(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName(i)));
        }

        // rosettaSegmentRef*
        for (RosettaParser.RosettaSegmentRefContext srCtx : ctx.rosettaSegmentRef()) {
            RSegmentRef segRef = (RSegmentRef) visitRosettaSegmentRef(srCtx);
            rdr.segmentRefs().add(segRef);
        }

        return rdr;
    }

    /**
     * Visits a {@code rosettaSegmentRef} rule and builds an {@link RSegmentRef}.
     *
     * <p>Grammar: {@code qualifiedName STRING}
     */
    @Override
    public Object visitRosettaSegmentRef(RosettaParser.RosettaSegmentRefContext ctx) {
        RSegmentRef segRef = new RSegmentRef();
        segRef.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // qualifiedName — segment name
        segRef.setSegmentName(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));

        // STRING — value
        segRef.setValue(AstBuilderHelper.stripQuotes(ctx.STRING().getText()));

        return segRef;
    }

    /**
     * Visits a {@code documentRationale} rule and builds an {@link RDocumentRationale}.
     *
     * <p>Grammar: {@code RATIONALE STRING (RATIONALE_AUTHOR STRING)? | RATIONALE_AUTHOR STRING (RATIONALE STRING)?}
     */
    @Override
    public Object visitDocumentRationale(RosettaParser.DocumentRationaleContext ctx) {
        RDocumentRationale dr = new RDocumentRationale();
        dr.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // The STRING tokens appear in order after their respective keywords.
        // RATIONALE and RATIONALE_AUTHOR can appear in either order.
        if (ctx.RATIONALE() != null && ctx.RATIONALE_AUTHOR() != null) {
            // Both present — determine order by token position
            int rationaleIdx = ctx.RATIONALE().getSymbol().getTokenIndex();
            int authorIdx = ctx.RATIONALE_AUTHOR().getSymbol().getTokenIndex();
            if (rationaleIdx < authorIdx) {
                dr.setRationale(AstBuilderHelper.stripQuotes(ctx.STRING(0).getText()));
                dr.setRationaleAuthor(AstBuilderHelper.stripQuotes(ctx.STRING(1).getText()));
            } else {
                dr.setRationaleAuthor(AstBuilderHelper.stripQuotes(ctx.STRING(0).getText()));
                dr.setRationale(AstBuilderHelper.stripQuotes(ctx.STRING(1).getText()));
            }
        } else if (ctx.RATIONALE() != null) {
            dr.setRationale(AstBuilderHelper.stripQuotes(ctx.STRING(0).getText()));
        } else if (ctx.RATIONALE_AUTHOR() != null) {
            dr.setRationaleAuthor(AstBuilderHelper.stripQuotes(ctx.STRING(0).getText()));
        }

        return dr;
    }

    // -- Report visitor (Task 12) ---------------------------------------------

    /**
     * Visits a {@code rosettaReport} rule and builds an {@link RReport}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaReport:
     *     REPORT regulatoryDocumentReference
     *     IN (REAL_TIME|T_PLUS_*|ASATP)
     *     FROM typeCall
     *     WHEN qualifiedName (AND qualifiedName)*
     *     (USING STANDARD qualifiedName)?
     *     WITH TYPE qualifiedName
     *     (WITH SOURCE qualifiedName)?
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaReport(RosettaParser.RosettaReportContext ctx) {
        RReport report = new RReport();
        report.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // REPORT keyword
        report.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.REPORT().getSymbol(), fileName, charToByte));

        // regulatoryDocumentReference
        if (ctx.regulatoryDocumentReference() != null) {
            RRegulatoryDocumentReference regDocRef =
                    (RRegulatoryDocumentReference) visitRegulatoryDocumentReference(ctx.regulatoryDocumentReference());
            report.setRegulatoryDocRef(regDocRef);
        }

        // IN timing
        if (ctx.REAL_TIME() != null) {
            report.setTiming(ReportTiming.REAL_TIME);
        } else if (ctx.T_PLUS_1() != null) {
            report.setTiming(ReportTiming.T_PLUS_1);
        } else if (ctx.T_PLUS_2() != null) {
            report.setTiming(ReportTiming.T_PLUS_2);
        } else if (ctx.T_PLUS_3() != null) {
            report.setTiming(ReportTiming.T_PLUS_3);
        } else if (ctx.T_PLUS_4() != null) {
            report.setTiming(ReportTiming.T_PLUS_4);
        } else if (ctx.T_PLUS_5() != null) {
            report.setTiming(ReportTiming.T_PLUS_5);
        } else if (ctx.ASATP() != null) {
            report.setTiming(ReportTiming.ASATP);
        }

        // FROM typeCall
        if (ctx.typeCall() != null) {
            RTypeCall tc = (RTypeCall) visitTypeCall(ctx.typeCall());
            report.setFromType(tc);
        }

        // WHEN qualifiedName (AND qualifiedName)*
        // The qualifiedName list may include USING STANDARD, WITH TYPE, and WITH SOURCE refs.
        // We need to distinguish: whenConditions are listed after WHEN and before USING/WITH.
        // However, in the grammar, the WHEN conditions are just the first group of qualifiedNames
        // before any USING/WITH keywords. The report grammar orders these sections sequentially.
        //
        // Strategy: collect all qualifiedNames. The ones that belong to WHEN are before
        // USING STANDARD / WITH TYPE / WITH SOURCE. We detect them by checking how many
        // AND tokens there are — whenConditions = AND count + 1.
        int whenCount = ctx.AND().size() + 1;

        // However, the total qualifiedName list includes: whenConditions + usingStandard? + withType + withSource?
        // So first `whenCount` qualifiedNames are WHEN conditions
        for (int i = 0; i < whenCount && i < ctx.qualifiedName().size(); i++) {
            report.whenConditions().add(
                    AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName(i)));
        }

        // USING STANDARD qualifiedName?
        // WITH TYPE qualifiedName
        // WITH SOURCE qualifiedName?
        // These are the remaining qualifiedNames after WHEN conditions
        int nextIdx = whenCount;
        if (ctx.USING() != null && ctx.STANDARD() != null && nextIdx < ctx.qualifiedName().size()) {
            report.setUsingStandard(
                    AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName(nextIdx)));
            nextIdx++;
        }
        if (ctx.TYPE() != null && nextIdx < ctx.qualifiedName().size()) {
            report.setWithType(
                    AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName(nextIdx)));
            // v3.2 seat 4 (F7): the linker anchors its TYPE_NOT_FOUND on the reference's own range
            report.putTokenRange("withType",
                    AstBuilderHelper.rangeOf(ctx.qualifiedName(nextIdx), fileName, charToByte));
            nextIdx++;
        }
        if (ctx.SOURCE() != null && nextIdx < ctx.qualifiedName().size()) {
            report.setWithSource(
                    AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName(nextIdx)));
            report.putTokenRange("withSource",
                    AstBuilderHelper.rangeOf(ctx.qualifiedName(nextIdx), fileName, charToByte));
        }

        return report;
    }

    // =========================================================================
    // Synonym, mapping, and external source visitors (Task 14)
    //
    // Grammar families:
    //   1. Synonym declarations — rosettaSynonym, classSynonym, rosettaEnumSynonym,
    //      rosettaSynonymSource.
    //   2. Synonym bodies and values — rosettaSynonymBody (five alternatives),
    //      rosettaSynonymValue, rosettaMetaSynonymValue, classSynonymValue,
    //      rosettaMergeSynonymValue.
    //   3. Mapping forms — rosettaMapping, rosettaMappingInstance, rosettaMappingSetTo,
    //      rosettaMappingSetToInstance, rosettaMappingPathTests.
    //   4. Map-test alternatives (dispatch) — rosettaMapPath, rosettaMapRosettaPath,
    //      rosettaMapTestExpression (Exists/Absent/Equality), rosettaMapTestFunc.
    //      rosettaMapPathValue and rosettaMapPrimaryExpression are leaf forms.
    //   5. External sources — rosettaExternalSynonymSource, rosettaExternalRuleSource,
    //      rosettaExternalClass, rosettaExternalEnum, rosettaExternalRegularAttribute,
    //      rosettaExternalEnumValue, rosettaExternalClassSynonym, rosettaExternalSynonym,
    //      rosettaExternalEnumSynonym.
    //
    // Helpers at the end of this section factor out common patterns:
    //   • populating source names on synonym declarations
    //   • building a synonym value (name + optional ref/path/maps)
    //   • building a class synonym value (name + optional ref/path)
    //   • dispatching a rosettaMapTest to the correct alternative
    //   • dispatching a rosettaMapTestExpression to the correct labelled alternative
    //   • building the trailing modifiers on a synonym body
    //   • building an external source (classes + optional enums) — shared between
    //     synonym and rule sources
    // =========================================================================

    // -- Synonym declarations -------------------------------------------------

    /**
     * Visits a {@code rosettaSynonymSource} rule and builds an {@link RSynonymSource}.
     *
     * <p>Grammar: {@code SYNONYM SOURCE validID}
     */
    @Override
    public Object visitRosettaSynonymSource(RosettaParser.RosettaSynonymSourceContext ctx) {
        RSynonymSource src = new RSynonymSource();
        src.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // SYNONYM keyword
        src.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.SYNONYM().getSymbol(), fileName, charToByte));

        // SOURCE keyword
        src.putTokenRange("source",
                AstBuilderHelper.rangeOfToken(ctx.SOURCE().getSymbol(), fileName, charToByte));

        // validID — source name
        src.setName(ctx.validID().getText());
        src.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        return src;
    }

    /**
     * Visits a {@code rosettaSynonym} rule and builds an {@link RSynonym}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaSynonym:
     *     LBRACK SYNONYM qualifiedName (COMMA qualifiedName)* rosettaSynonymBody RBRACK
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaSynonym(RosettaParser.RosettaSynonymContext ctx) {
        RSynonym syn = new RSynonym();
        syn.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // LBRACK / RBRACK delimiter tokens
        putBracketRanges(syn, ctx.LBRACK(), ctx.RBRACK());

        // SYNONYM keyword
        syn.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.SYNONYM().getSymbol(), fileName, charToByte));

        // qualifiedName (COMMA qualifiedName)* — source names
        populateSourceNames(syn, "source", syn.sources(), ctx.qualifiedName());

        // rosettaSynonymBody
        if (ctx.rosettaSynonymBody() != null) {
            RSynonymBody body = (RSynonymBody) visitRosettaSynonymBody(ctx.rosettaSynonymBody());
            syn.setBody(body);
        } else {
            throw new AstBuildException(
                    "rosettaSynonym missing rosettaSynonymBody (grammar-impossible)");
        }

        return syn;
    }

    /**
     * Visits a {@code classSynonym} rule and builds an {@link RClassSynonym}.
     *
     * <p>Grammar:
     * <pre>
     * classSynonym:
     *     LBRACK SYNONYM qualifiedName (COMMA qualifiedName)*
     *     (VALUE classSynonymValue)?
     *     (META rosettaMetaSynonymValue)?
     *     RBRACK
     * ;
     * </pre>
     */
    @Override
    public Object visitClassSynonym(RosettaParser.ClassSynonymContext ctx) {
        RClassSynonym cs = new RClassSynonym();
        cs.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // LBRACK / RBRACK delimiter tokens
        putBracketRanges(cs, ctx.LBRACK(), ctx.RBRACK());

        // SYNONYM keyword
        cs.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.SYNONYM().getSymbol(), fileName, charToByte));

        // qualifiedName (COMMA qualifiedName)* — source names
        populateSourceNames(cs, "source", cs.sources(), ctx.qualifiedName());

        // (VALUE classSynonymValue)?
        if (ctx.VALUE() != null && ctx.classSynonymValue() != null) {
            cs.putTokenRange("value",
                    AstBuilderHelper.rangeOfToken(ctx.VALUE().getSymbol(), fileName, charToByte));
            RClassSynonymValue csv =
                    (RClassSynonymValue) visitClassSynonymValue(ctx.classSynonymValue());
            cs.setValue(csv);
        }

        // (META rosettaMetaSynonymValue)?
        if (ctx.META() != null && ctx.rosettaMetaSynonymValue() != null) {
            cs.putTokenRange("meta",
                    AstBuilderHelper.rangeOfToken(ctx.META().getSymbol(), fileName, charToByte));
            RMetaSynonymValue msv =
                    (RMetaSynonymValue) visitRosettaMetaSynonymValue(ctx.rosettaMetaSynonymValue());
            cs.setMeta(msv);
        }

        return cs;
    }

    /**
     * Visits a {@code rosettaEnumSynonym} rule and builds an {@link REnumSynonym}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaEnumSynonym:
     *     LBRACK SYNONYM qualifiedName (COMMA qualifiedName)*
     *     VALUE STRING (DEFINITION STRING)?
     *     (PATTERN STRING STRING)?
     *     REMOVE_HTML?
     *     RBRACK
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaEnumSynonym(RosettaParser.RosettaEnumSynonymContext ctx) {
        REnumSynonym es = new REnumSynonym();
        es.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // LBRACK / RBRACK delimiter tokens
        putBracketRanges(es, ctx.LBRACK(), ctx.RBRACK());

        // SYNONYM keyword
        es.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.SYNONYM().getSymbol(), fileName, charToByte));

        // qualifiedName (COMMA qualifiedName)* — source names
        populateSourceNames(es, "source", es.sources(), ctx.qualifiedName());

        // VALUE keyword
        es.putTokenRange("value",
                AstBuilderHelper.rangeOfToken(ctx.VALUE().getSymbol(), fileName, charToByte));

        // STRING list: value + optional definition + optional (patternMatch, patternReplace)
        List<TerminalNode> strings = ctx.STRING();
        if (strings.isEmpty()) {
            throw new AstBuildException(
                    "rosettaEnumSynonym missing VALUE STRING (grammar-impossible)");
        }
        int idx = 0;
        // value is always STRING(0)
        es.setValue(AstBuilderHelper.stripQuotes(strings.get(idx++).getText()));

        // (DEFINITION STRING)?
        if (ctx.DEFINITION() != null) {
            es.putTokenRange("definition",
                    AstBuilderHelper.rangeOfToken(ctx.DEFINITION().getSymbol(), fileName, charToByte));
            if (idx >= strings.size()) {
                throw new AstBuildException(
                        "rosettaEnumSynonym DEFINITION missing STRING (grammar-impossible)");
            }
            es.setDefinitionText(AstBuilderHelper.stripQuotes(strings.get(idx++).getText()));
        }

        // (PATTERN STRING STRING)?
        if (ctx.PATTERN() != null) {
            es.putTokenRange("pattern",
                    AstBuilderHelper.rangeOfToken(ctx.PATTERN().getSymbol(), fileName, charToByte));
            if (idx + 1 >= strings.size()) {
                throw new AstBuildException(
                        "rosettaEnumSynonym PATTERN missing two STRINGs (grammar-impossible)");
            }
            es.setPatternMatch(AstBuilderHelper.stripQuotes(strings.get(idx++).getText()));
            es.setPatternReplace(AstBuilderHelper.stripQuotes(strings.get(idx++).getText()));
        }

        // REMOVE_HTML?
        if (ctx.REMOVE_HTML() != null) {
            es.setRemoveHtml(true);
            es.putTokenRange("removeHtml",
                    AstBuilderHelper.rangeOfToken(ctx.REMOVE_HTML().getSymbol(), fileName, charToByte));
        }

        return es;
    }

    // -- Synonym bodies and values --------------------------------------------

    /**
     * Visits a {@code rosettaSynonymBody} rule and builds an {@link RSynonymBody}.
     *
     * <p>Dispatches on the five alternatives: VALUE, HINT, MERGE, SET_TO (via
     * rosettaMappingSetTo), and META_ONLY. Trailing modifiers (dateFormat,
     * pattern, removeHtml, mapper) are common across all alternatives.
     *
     * <p>Grammar:
     * <pre>
     * rosettaSynonymBody:
     *     ( VALUE (rosettaSynonymValue COMMA)* rosettaSynonymValue rosettaMapping? (META (STRING COMMA)* STRING)?
     *     | HINT (STRING COMMA)* STRING
     *     | MERGE rosettaMergeSynonymValue
     *     | rosettaMappingSetTo
     *     | META (STRING COMMA)* STRING
     *     )
     *     (DATE_FORMAT STRING)?
     *     (PATTERN STRING STRING)?
     *     REMOVE_HTML?
     *     (MAPPER STRING)?
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaSynonymBody(RosettaParser.RosettaSynonymBodyContext ctx) {
        RSynonymBody body = new RSynonymBody();
        body.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // The STRING list covers all alternatives that carry STRINGs directly
        // (HINT strings, META tail strings, META_ONLY strings), plus trailing
        // DATE_FORMAT / PATTERN / MAPPER strings. We track how many STRINGs
        // were consumed by the chosen head alternative so the trailing-modifier
        // parser can pick up the remaining ones.
        List<TerminalNode> strings = ctx.STRING();
        int stringIdx = 0;

        if (ctx.VALUE() != null) {
            // VALUE alternative
            body.setKind(SynonymBodyKind.VALUE);
            body.putTokenRange("keyword",
                    AstBuilderHelper.rangeOfToken(ctx.VALUE().getSymbol(), fileName, charToByte));

            for (RosettaParser.RosettaSynonymValueContext svCtx : ctx.rosettaSynonymValue()) {
                RSynonymValue sv = (RSynonymValue) visitRosettaSynonymValue(svCtx);
                body.values().add(sv);
            }

            // rosettaMapping?
            if (ctx.rosettaMapping() != null) {
                RMapping mapping = (RMapping) visitRosettaMapping(ctx.rosettaMapping());
                body.setMapping(mapping);
            }

            // (META (STRING COMMA)* STRING)? — trailing meta fields on VALUE
            if (ctx.META() != null) {
                body.putTokenRange("meta",
                        AstBuilderHelper.rangeOfToken(ctx.META().getSymbol(), fileName, charToByte));
                // All STRINGs up to trailing modifiers are meta field names.
                // We'll consume them all here, then let the trailing-modifier
                // section below take any STRINGs it needs.
                // The VALUE alternative's own STRINGs all live on
                // RosettaSynonymValueContext children, so any STRINGs on the body's
                // own STRING list (minus the trailing-modifier strings) belong to
                // this trailing META clause.
                int metaStringCount = countHeadStringsForHintOrMeta(ctx);
                for (int i = 0; i < metaStringCount; i++) {
                    body.metaFields().add(
                            AstBuilderHelper.stripQuotes(strings.get(stringIdx++).getText()));
                }
            }
        } else if (ctx.HINT() != null) {
            body.setKind(SynonymBodyKind.HINT);
            body.putTokenRange("keyword",
                    AstBuilderHelper.rangeOfToken(ctx.HINT().getSymbol(), fileName, charToByte));
            int hintStringCount = countHeadStringsForHintOrMeta(ctx);
            for (int i = 0; i < hintStringCount; i++) {
                body.hints().add(
                        AstBuilderHelper.stripQuotes(strings.get(stringIdx++).getText()));
            }
        } else if (ctx.MERGE() != null) {
            body.setKind(SynonymBodyKind.MERGE);
            body.putTokenRange("keyword",
                    AstBuilderHelper.rangeOfToken(ctx.MERGE().getSymbol(), fileName, charToByte));
            if (ctx.rosettaMergeSynonymValue() != null) {
                RMergeSynonymValue merge = (RMergeSynonymValue)
                        visitRosettaMergeSynonymValue(ctx.rosettaMergeSynonymValue());
                body.setMerge(merge);
                // The merge value may carry its own STRINGs (name, excludePath) — those
                // are on its own context, not on the body's STRING list.
            }
        } else if (ctx.rosettaMappingSetTo() != null) {
            body.setKind(SynonymBodyKind.SET_TO);
            RMappingSetTo setTo = (RMappingSetTo)
                    visitRosettaMappingSetTo(ctx.rosettaMappingSetTo());
            body.setSetTo(setTo);
        } else if (ctx.META() != null) {
            // META_ONLY alternative: META (STRING COMMA)* STRING
            body.setKind(SynonymBodyKind.META_ONLY);
            body.putTokenRange("keyword",
                    AstBuilderHelper.rangeOfToken(ctx.META().getSymbol(), fileName, charToByte));
            int metaStringCount = countHeadStringsForHintOrMeta(ctx);
            for (int i = 0; i < metaStringCount; i++) {
                body.metaFields().add(
                        AstBuilderHelper.stripQuotes(strings.get(stringIdx++).getText()));
            }
        } else {
            throw new AstBuildException(
                    "rosettaSynonymBody matched no alternative: " + ctx.getText());
        }

        // Trailing modifiers:
        //   (DATE_FORMAT STRING)?
        //   (PATTERN STRING STRING)?
        //   REMOVE_HTML?
        //   (MAPPER STRING)?
        if (ctx.DATE_FORMAT() != null) {
            body.putTokenRange("dateFormat",
                    AstBuilderHelper.rangeOfToken(ctx.DATE_FORMAT().getSymbol(), fileName, charToByte));
            if (stringIdx >= strings.size()) {
                throw new AstBuildException(
                        "rosettaSynonymBody DATE_FORMAT missing STRING (grammar-impossible)");
            }
            body.setDateFormat(AstBuilderHelper.stripQuotes(strings.get(stringIdx++).getText()));
        }
        if (ctx.PATTERN() != null) {
            body.putTokenRange("pattern",
                    AstBuilderHelper.rangeOfToken(ctx.PATTERN().getSymbol(), fileName, charToByte));
            if (stringIdx + 1 >= strings.size()) {
                throw new AstBuildException(
                        "rosettaSynonymBody PATTERN missing two STRINGs (grammar-impossible)");
            }
            body.setPatternMatch(AstBuilderHelper.stripQuotes(strings.get(stringIdx++).getText()));
            body.setPatternReplace(AstBuilderHelper.stripQuotes(strings.get(stringIdx++).getText()));
        }
        if (ctx.REMOVE_HTML() != null) {
            body.setRemoveHtml(true);
            body.putTokenRange("removeHtml",
                    AstBuilderHelper.rangeOfToken(ctx.REMOVE_HTML().getSymbol(), fileName, charToByte));
        }
        if (ctx.MAPPER() != null) {
            body.putTokenRange("mapper",
                    AstBuilderHelper.rangeOfToken(ctx.MAPPER().getSymbol(), fileName, charToByte));
            if (stringIdx >= strings.size()) {
                throw new AstBuildException(
                        "rosettaSynonymBody MAPPER missing STRING (grammar-impossible)");
            }
            body.setMapper(AstBuilderHelper.stripQuotes(strings.get(stringIdx++).getText()));
        }

        return body;
    }

    /**
     * Visits a {@code rosettaSynonymValue} rule and builds an {@link RSynonymValue}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaSynonymValue:
     *     STRING (rosettaSynonymRef INT_LITERAL)?
     *     (PATH STRING)?
     *     (MAPS INT_LITERAL)?
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaSynonymValue(RosettaParser.RosettaSynonymValueContext ctx) {
        RSynonymValue sv = new RSynonymValue();
        sv.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        populateSynonymValueCommon(ctx.STRING(), ctx.rosettaSynonymRef(),
                ctx.INT_LITERAL(), ctx.PATH(), ctx.MAPS(),
                sv::setName, sv::setRef, sv::setRefValue, sv::setPath, sv::setMaps,
                key -> {
                    // Synonym value tokens: path keyword, maps keyword
                    if ("path".equals(key) && ctx.PATH() != null) {
                        sv.putTokenRange("path",
                                AstBuilderHelper.rangeOfToken(ctx.PATH().getSymbol(), fileName, charToByte));
                    }
                    if ("maps".equals(key) && ctx.MAPS() != null) {
                        sv.putTokenRange("maps",
                                AstBuilderHelper.rangeOfToken(ctx.MAPS().getSymbol(), fileName, charToByte));
                    }
                });
        return sv;
    }

    /**
     * Visits a {@code rosettaMetaSynonymValue} rule and builds an {@link RMetaSynonymValue}.
     *
     * <p>Same structure as {@code rosettaSynonymValue}.
     */
    @Override
    public Object visitRosettaMetaSynonymValue(RosettaParser.RosettaMetaSynonymValueContext ctx) {
        RMetaSynonymValue msv = new RMetaSynonymValue();
        msv.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        populateSynonymValueCommon(ctx.STRING(), ctx.rosettaSynonymRef(),
                ctx.INT_LITERAL(), ctx.PATH(), ctx.MAPS(),
                msv::setName, msv::setRef, msv::setRefValue, msv::setPath, msv::setMaps,
                key -> {
                    if ("path".equals(key) && ctx.PATH() != null) {
                        msv.putTokenRange("path",
                                AstBuilderHelper.rangeOfToken(ctx.PATH().getSymbol(), fileName, charToByte));
                    }
                    if ("maps".equals(key) && ctx.MAPS() != null) {
                        msv.putTokenRange("maps",
                                AstBuilderHelper.rangeOfToken(ctx.MAPS().getSymbol(), fileName, charToByte));
                    }
                });
        return msv;
    }

    /**
     * Visits a {@code classSynonymValue} rule and builds an {@link RClassSynonymValue}.
     *
     * <p>Grammar:
     * <pre>
     * classSynonymValue:
     *     STRING (rosettaSynonymRef INT_LITERAL)?
     *     (PATH STRING)?
     * ;
     * </pre>
     */
    @Override
    public Object visitClassSynonymValue(RosettaParser.ClassSynonymValueContext ctx) {
        RClassSynonymValue csv = new RClassSynonymValue();
        csv.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        List<TerminalNode> strings = ctx.STRING();
        if (strings.isEmpty()) {
            throw new AstBuildException(
                    "classSynonymValue missing name STRING (grammar-impossible)");
        }
        csv.setName(AstBuilderHelper.stripQuotes(strings.get(0).getText()));

        // (rosettaSynonymRef INT_LITERAL)? — refValue is the single INT_LITERAL here.
        // Parsed as BigInteger to handle arbitrary-precision integers (INT_LITERAL
        // has no upper bound in the grammar — same fix as RIntLiteral).
        if (ctx.rosettaSynonymRef() != null) {
            csv.setRef(synonymRefFrom(ctx.rosettaSynonymRef()));
            if (ctx.INT_LITERAL() != null) {
                csv.setRefValue(new BigInteger(ctx.INT_LITERAL().getText()));
            }
        }

        // (PATH STRING)? — the path string is strings.get(1) when present
        if (ctx.PATH() != null) {
            csv.putTokenRange("path",
                    AstBuilderHelper.rangeOfToken(ctx.PATH().getSymbol(), fileName, charToByte));
            if (strings.size() < 2) {
                throw new AstBuildException(
                        "classSynonymValue PATH missing STRING (grammar-impossible)");
            }
            csv.setPath(AstBuilderHelper.stripQuotes(strings.get(1).getText()));
        }

        return csv;
    }

    /**
     * Visits a {@code rosettaMergeSynonymValue} rule and builds an
     * {@link RMergeSynonymValue}.
     *
     * <p>Grammar: {@code STRING (WHEN PATH NEQ STRING)?}
     */
    @Override
    public Object visitRosettaMergeSynonymValue(RosettaParser.RosettaMergeSynonymValueContext ctx) {
        RMergeSynonymValue merge = new RMergeSynonymValue();
        merge.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        List<TerminalNode> strings = ctx.STRING();
        if (strings.isEmpty()) {
            throw new AstBuildException(
                    "rosettaMergeSynonymValue missing name STRING (grammar-impossible)");
        }
        merge.setName(AstBuilderHelper.stripQuotes(strings.get(0).getText()));

        // (WHEN PATH NEQ STRING)?
        if (ctx.WHEN() != null) {
            merge.putTokenRange("when",
                    AstBuilderHelper.rangeOfToken(ctx.WHEN().getSymbol(), fileName, charToByte));
            if (ctx.PATH() != null) {
                merge.putTokenRange("path",
                        AstBuilderHelper.rangeOfToken(ctx.PATH().getSymbol(), fileName, charToByte));
            }
            if (ctx.NEQ() != null) {
                merge.putTokenRange("neq",
                        AstBuilderHelper.rangeOfToken(ctx.NEQ().getSymbol(), fileName, charToByte));
            }
            if (strings.size() < 2) {
                throw new AstBuildException(
                        "rosettaMergeSynonymValue WHEN missing excludePath STRING (grammar-impossible)");
            }
            merge.setExcludePath(AstBuilderHelper.stripQuotes(strings.get(1).getText()));
        }

        return merge;
    }

    // -- Mapping forms --------------------------------------------------------

    /**
     * Visits a {@code rosettaMapping} rule and builds an {@link RMapping}.
     *
     * <p>Grammar: {@code rosettaMappingInstance (COMMA rosettaMappingInstance)*}
     */
    @Override
    public Object visitRosettaMapping(RosettaParser.RosettaMappingContext ctx) {
        RMapping mapping = new RMapping();
        mapping.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        for (RosettaParser.RosettaMappingInstanceContext instCtx : ctx.rosettaMappingInstance()) {
            RMappingInstance inst = (RMappingInstance) visitRosettaMappingInstance(instCtx);
            mapping.instances().add(inst);
        }
        return mapping;
    }

    /**
     * Visits a {@code rosettaMappingInstance} rule and builds an
     * {@link RMappingInstance}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaMappingInstance:
     *     SET WHEN rosettaMappingPathTests
     *   | DEFAULT TO rosettaMapPrimaryExpression
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaMappingInstance(RosettaParser.RosettaMappingInstanceContext ctx) {
        RMappingInstance inst = new RMappingInstance();
        inst.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        if (ctx.SET() != null && ctx.WHEN() != null) {
            inst.setKind(MappingInstanceKind.SET_WHEN);
            inst.putTokenRange("set",
                    AstBuilderHelper.rangeOfToken(ctx.SET().getSymbol(), fileName, charToByte));
            inst.putTokenRange("when",
                    AstBuilderHelper.rangeOfToken(ctx.WHEN().getSymbol(), fileName, charToByte));
            if (ctx.rosettaMappingPathTests() != null) {
                RMappingPathTests tests = (RMappingPathTests)
                        visitRosettaMappingPathTests(ctx.rosettaMappingPathTests());
                inst.setTests(tests);
            }
        } else if (ctx.DEFAULT() != null && ctx.TO() != null) {
            inst.setKind(MappingInstanceKind.DEFAULT_TO);
            inst.putTokenRange("default",
                    AstBuilderHelper.rangeOfToken(ctx.DEFAULT().getSymbol(), fileName, charToByte));
            inst.putTokenRange("to",
                    AstBuilderHelper.rangeOfToken(ctx.TO().getSymbol(), fileName, charToByte));
            if (ctx.rosettaMapPrimaryExpression() != null) {
                RMapPrimaryExpression expr = (RMapPrimaryExpression)
                        visitRosettaMapPrimaryExpression(ctx.rosettaMapPrimaryExpression());
                inst.setDefaultValue(expr);
            }
        } else {
            throw new AstBuildException(
                    "rosettaMappingInstance matched no alternative: " + ctx.getText());
        }

        return inst;
    }

    /**
     * Visits a {@code rosettaMappingSetTo} rule and builds an {@link RMappingSetTo}.
     *
     * <p>Grammar: {@code rosettaMappingSetToInstance (COMMA rosettaMappingSetToInstance)*}
     */
    @Override
    public Object visitRosettaMappingSetTo(RosettaParser.RosettaMappingSetToContext ctx) {
        RMappingSetTo setTo = new RMappingSetTo();
        setTo.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        for (RosettaParser.RosettaMappingSetToInstanceContext instCtx
                : ctx.rosettaMappingSetToInstance()) {
            RMappingSetToInstance inst = (RMappingSetToInstance)
                    visitRosettaMappingSetToInstance(instCtx);
            setTo.instances().add(inst);
        }
        return setTo;
    }

    /**
     * Visits a {@code rosettaMappingSetToInstance} rule and builds an
     * {@link RMappingSetToInstance}.
     *
     * <p>Grammar: {@code SET TO rosettaMapPrimaryExpression (WHEN rosettaMappingPathTests)?}
     */
    @Override
    public Object visitRosettaMappingSetToInstance(
            RosettaParser.RosettaMappingSetToInstanceContext ctx) {
        RMappingSetToInstance inst = new RMappingSetToInstance();
        inst.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // SET keyword
        if (ctx.SET() != null) {
            inst.putTokenRange("set",
                    AstBuilderHelper.rangeOfToken(ctx.SET().getSymbol(), fileName, charToByte));
        }
        // TO keyword
        if (ctx.TO() != null) {
            inst.putTokenRange("to",
                    AstBuilderHelper.rangeOfToken(ctx.TO().getSymbol(), fileName, charToByte));
        }

        if (ctx.rosettaMapPrimaryExpression() != null) {
            RMapPrimaryExpression expr = (RMapPrimaryExpression)
                    visitRosettaMapPrimaryExpression(ctx.rosettaMapPrimaryExpression());
            inst.setValue(expr);
        }

        // (WHEN rosettaMappingPathTests)?
        if (ctx.WHEN() != null) {
            inst.putTokenRange("when",
                    AstBuilderHelper.rangeOfToken(ctx.WHEN().getSymbol(), fileName, charToByte));
            if (ctx.rosettaMappingPathTests() != null) {
                RMappingPathTests tests = (RMappingPathTests)
                        visitRosettaMappingPathTests(ctx.rosettaMappingPathTests());
                inst.setWhen(tests);
            }
        }

        return inst;
    }

    /**
     * Visits a {@code rosettaMappingPathTests} rule and builds an
     * {@link RMappingPathTests}.
     *
     * <p>Grammar: {@code rosettaMapTest (AND rosettaMapTest)*}
     */
    @Override
    public Object visitRosettaMappingPathTests(
            RosettaParser.RosettaMappingPathTestsContext ctx) {
        RMappingPathTests tests = new RMappingPathTests();
        tests.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        for (RosettaParser.RosettaMapTestContext testCtx : ctx.rosettaMapTest()) {
            RMapTest test = dispatchMapTest(testCtx);
            tests.tests().add(test);
        }
        return tests;
    }

    // -- Map test alternatives ------------------------------------------------

    /**
     * Visits a {@code rosettaMapPath} rule and builds an {@link RMapPath}.
     *
     * <p>Grammar: {@code PATH EQ rosettaMapPathValue}
     */
    @Override
    public Object visitRosettaMapPath(RosettaParser.RosettaMapPathContext ctx) {
        RMapPath node = new RMapPath();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.putTokenRange("path",
                AstBuilderHelper.rangeOfToken(ctx.PATH().getSymbol(), fileName, charToByte));
        node.putTokenRange("eq",
                AstBuilderHelper.rangeOfToken(ctx.EQ().getSymbol(), fileName, charToByte));
        if (ctx.rosettaMapPathValue() != null) {
            node.setPathValue((RMapPathValue) visitRosettaMapPathValue(ctx.rosettaMapPathValue()));
        }
        return node;
    }

    /**
     * Visits a {@code rosettaMapRosettaPath} rule and builds an
     * {@link RMapRosettaPath}.
     *
     * <p>Grammar: {@code ROSETTA_PATH EQ rosettaAttributeReference}
     *
     * <p>The attribute reference is rendered as a string:
     * {@code QualifiedName -> validID -> validID}.
     */
    @Override
    public Object visitRosettaMapRosettaPath(RosettaParser.RosettaMapRosettaPathContext ctx) {
        RMapRosettaPath node = new RMapRosettaPath();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.putTokenRange("rosettaPath",
                AstBuilderHelper.rangeOfToken(ctx.ROSETTA_PATH().getSymbol(), fileName, charToByte));
        node.putTokenRange("eq",
                AstBuilderHelper.rangeOfToken(ctx.EQ().getSymbol(), fileName, charToByte));
        if (ctx.rosettaAttributeReference() != null) {
            node.setAttributeReference(textOfAttributeReference(ctx.rosettaAttributeReference()));
        }
        return node;
    }

    /**
     * Visits a {@code rosettaMapPathValue} rule and builds an {@link RMapPathValue}.
     *
     * <p>Grammar: {@code STRING}
     */
    @Override
    public Object visitRosettaMapPathValue(RosettaParser.RosettaMapPathValueContext ctx) {
        RMapPathValue node = new RMapPathValue();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setValue(AstBuilderHelper.stripQuotes(ctx.STRING().getText()));
        return node;
    }

    /**
     * Visits the {@code MapTestExistsExpr} labelled alternative and builds an
     * {@link RMapTestExists}.
     *
     * <p>Grammar: {@code rosettaMapPathValue EXISTS}
     */
    @Override
    public Object visitMapTestExistsExpr(RosettaParser.MapTestExistsExprContext ctx) {
        RMapTestExists node = new RMapTestExists();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.putTokenRange("exists",
                AstBuilderHelper.rangeOfToken(ctx.EXISTS().getSymbol(), fileName, charToByte));
        if (ctx.rosettaMapPathValue() != null) {
            node.setPathValue((RMapPathValue) visitRosettaMapPathValue(ctx.rosettaMapPathValue()));
        }
        return node;
    }

    /**
     * Visits the {@code MapTestAbsentExpr} labelled alternative and builds an
     * {@link RMapTestAbsent}.
     *
     * <p>Grammar: {@code rosettaMapPathValue IS ABSENT}
     */
    @Override
    public Object visitMapTestAbsentExpr(RosettaParser.MapTestAbsentExprContext ctx) {
        RMapTestAbsent node = new RMapTestAbsent();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.putTokenRange("is",
                AstBuilderHelper.rangeOfToken(ctx.IS().getSymbol(), fileName, charToByte));
        node.putTokenRange("absent",
                AstBuilderHelper.rangeOfToken(ctx.ABSENT().getSymbol(), fileName, charToByte));
        if (ctx.rosettaMapPathValue() != null) {
            node.setPathValue((RMapPathValue) visitRosettaMapPathValue(ctx.rosettaMapPathValue()));
        }
        return node;
    }

    /**
     * Visits the {@code MapTestEqualityExpr} labelled alternative and builds
     * an {@link RMapTestEquality}.
     *
     * <p>Grammar: {@code rosettaMapPathValue (EQ | NEQ) rosettaMapPrimaryExpression}
     */
    @Override
    public Object visitMapTestEqualityExpr(RosettaParser.MapTestEqualityExprContext ctx) {
        RMapTestEquality node = new RMapTestEquality();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        if (ctx.rosettaMapPathValue() != null) {
            node.setPathValue((RMapPathValue) visitRosettaMapPathValue(ctx.rosettaMapPathValue()));
        }

        if (ctx.EQ() != null) {
            node.setOp(EqOp.EQ);
            node.putTokenRange("operator",
                    AstBuilderHelper.rangeOfToken(ctx.EQ().getSymbol(), fileName, charToByte));
        } else if (ctx.NEQ() != null) {
            node.setOp(EqOp.NEQ);
            node.putTokenRange("operator",
                    AstBuilderHelper.rangeOfToken(ctx.NEQ().getSymbol(), fileName, charToByte));
        } else {
            throw new AstBuildException(
                    "MapTestEqualityExpr missing EQ/NEQ operator (grammar-impossible)");
        }

        if (ctx.rosettaMapPrimaryExpression() != null) {
            node.setValue((RMapPrimaryExpression)
                    visitRosettaMapPrimaryExpression(ctx.rosettaMapPrimaryExpression()));
        }

        return node;
    }

    /**
     * Visits a {@code rosettaMapPrimaryExpression} rule and builds an
     * {@link RMapPrimaryExpression}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaMapPrimaryExpression:
     *     enumValueReference
     *   | STRING
     *   | TRUE
     *   | FALSE
     *   | INT_LITERAL
     *   | BIG_DECIMAL
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaMapPrimaryExpression(
            RosettaParser.RosettaMapPrimaryExpressionContext ctx) {
        RMapPrimaryExpression node = new RMapPrimaryExpression();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        if (ctx.enumValueReference() != null) {
            node.setKind(MapPrimaryKind.ENUM_VALUE);
            // enumValueReference: qualifiedName ARROW validID
            var evr = ctx.enumValueReference();
            String enumRef = AstBuilderHelper.textOfQualifiedName(evr.qualifiedName())
                    + " -> " + evr.validID().getText();
            node.setEnumRef(enumRef);
        } else if (ctx.STRING() != null) {
            node.setKind(MapPrimaryKind.STRING);
            node.setStringValue(AstBuilderHelper.stripQuotes(ctx.STRING().getText()));
        } else if (ctx.TRUE() != null) {
            node.setKind(MapPrimaryKind.BOOLEAN);
            node.setBoolValue(Boolean.TRUE);
        } else if (ctx.FALSE() != null) {
            node.setKind(MapPrimaryKind.BOOLEAN);
            node.setBoolValue(Boolean.FALSE);
        } else if (ctx.INT_LITERAL() != null) {
            node.setKind(MapPrimaryKind.INT);
            // BigInteger: INT_LITERAL is unbounded in the grammar (same fix as RIntLiteral).
            node.setIntValue(new BigInteger(ctx.INT_LITERAL().getText()));
        } else if (ctx.BIG_DECIMAL() != null) {
            node.setKind(MapPrimaryKind.DECIMAL);
            node.setDecimalValue(ctx.BIG_DECIMAL().getText());
        } else {
            throw new AstBuildException(
                    "rosettaMapPrimaryExpression matched no alternative: " + ctx.getText());
        }

        return node;
    }

    /**
     * Visits a {@code rosettaMapTestFunc} rule and builds an {@link RMapTestFunc}.
     *
     * <p>Grammar:
     * {@code CONDITION_FUNC qualifiedName (CONDITION_PATH rosettaMapPathValue)?}
     */
    @Override
    public Object visitRosettaMapTestFunc(RosettaParser.RosettaMapTestFuncContext ctx) {
        RMapTestFunc node = new RMapTestFunc();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.putTokenRange("func",
                AstBuilderHelper.rangeOfToken(ctx.CONDITION_FUNC().getSymbol(), fileName, charToByte));
        node.setFuncName(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
        if (ctx.CONDITION_PATH() != null) {
            node.putTokenRange("conditionPath",
                    AstBuilderHelper.rangeOfToken(ctx.CONDITION_PATH().getSymbol(), fileName, charToByte));
            if (ctx.rosettaMapPathValue() != null) {
                node.setConditionPath(
                        (RMapPathValue) visitRosettaMapPathValue(ctx.rosettaMapPathValue()));
            }
        }
        return node;
    }

    // -- External sources -----------------------------------------------------

    /**
     * Visits a {@code rosettaExternalSynonymSource} rule and builds an
     * {@link RExternalSynonymSource}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaExternalSynonymSource:
     *     SYNONYM SOURCE validID
     *     (EXTENDS qualifiedName (COMMA qualifiedName)*)?
     *     LBRACE rosettaExternalClass* (ENUMS rosettaExternalEnum*)? RBRACE
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaExternalSynonymSource(
            RosettaParser.RosettaExternalSynonymSourceContext ctx) {
        RExternalSynonymSource src = new RExternalSynonymSource();
        src.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // SYNONYM keyword
        src.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.SYNONYM().getSymbol(), fileName, charToByte));
        // SOURCE keyword
        src.putTokenRange("source",
                AstBuilderHelper.rangeOfToken(ctx.SOURCE().getSymbol(), fileName, charToByte));
        // validID — name
        src.setName(ctx.validID().getText());
        src.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        populateExternalSourceBody(src, src.superSourceNames(), src.classes(), src.enums(),
                ctx.EXTENDS(), ctx.qualifiedName(),
                ctx.LBRACE(), ctx.RBRACE(), ctx.ENUMS(),
                ctx.rosettaExternalClass(), ctx.rosettaExternalEnum(),
                src::putTokenRange);

        return src;
    }

    /**
     * Visits a {@code rosettaExternalRuleSource} rule and builds an
     * {@link RExternalRuleSource}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaExternalRuleSource:
     *     RULE SOURCE validID
     *     (EXTENDS qualifiedName (COMMA qualifiedName)*)?
     *     LBRACE rosettaExternalClass* (ENUMS rosettaExternalEnum*)? RBRACE
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaExternalRuleSource(
            RosettaParser.RosettaExternalRuleSourceContext ctx) {
        RExternalRuleSource src = new RExternalRuleSource();
        src.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // RULE keyword
        src.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.RULE().getSymbol(), fileName, charToByte));
        // SOURCE keyword
        src.putTokenRange("source",
                AstBuilderHelper.rangeOfToken(ctx.SOURCE().getSymbol(), fileName, charToByte));
        // validID — name
        src.setName(ctx.validID().getText());
        src.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        populateExternalSourceBody(src, src.superSourceNames(), src.classes(), src.enums(),
                ctx.EXTENDS(), ctx.qualifiedName(),
                ctx.LBRACE(), ctx.RBRACE(), ctx.ENUMS(),
                ctx.rosettaExternalClass(), ctx.rosettaExternalEnum(),
                src::putTokenRange);

        return src;
    }

    /**
     * Visits a {@code rosettaExternalClass} rule and builds an {@link RExternalClass}.
     *
     * <p>Grammar:
     * {@code qualifiedName COLON rosettaExternalClassSynonym* rosettaExternalRegularAttribute*}
     */
    @Override
    public Object visitRosettaExternalClass(RosettaParser.RosettaExternalClassContext ctx) {
        RExternalClass ec = new RExternalClass();
        ec.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        ec.setTypeName(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
        ec.putTokenRange("typeName",
                AstBuilderHelper.rangeOf(ctx.qualifiedName(), fileName, charToByte));
        if (ctx.COLON() != null) {
            ec.putTokenRange("colon",
                    AstBuilderHelper.rangeOfToken(ctx.COLON().getSymbol(), fileName, charToByte));
        }

        for (RosettaParser.RosettaExternalClassSynonymContext csCtx : ctx.rosettaExternalClassSynonym()) {
            RExternalClassSynonym ecs = (RExternalClassSynonym) visitRosettaExternalClassSynonym(csCtx);
            ec.classSynonyms().add(ecs);
        }
        for (RosettaParser.RosettaExternalRegularAttributeContext attrCtx
                : ctx.rosettaExternalRegularAttribute()) {
            RExternalRegularAttribute attr = (RExternalRegularAttribute)
                    visitRosettaExternalRegularAttribute(attrCtx);
            ec.attributes().add(attr);
        }

        return ec;
    }

    /**
     * Visits a {@code rosettaExternalEnum} rule and builds an {@link RExternalEnum}.
     *
     * <p>Grammar: {@code qualifiedName COLON rosettaExternalEnumValue*}
     */
    @Override
    public Object visitRosettaExternalEnum(RosettaParser.RosettaExternalEnumContext ctx) {
        RExternalEnum ee = new RExternalEnum();
        ee.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        ee.setTypeName(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
        ee.putTokenRange("typeName",
                AstBuilderHelper.rangeOf(ctx.qualifiedName(), fileName, charToByte));
        if (ctx.COLON() != null) {
            ee.putTokenRange("colon",
                    AstBuilderHelper.rangeOfToken(ctx.COLON().getSymbol(), fileName, charToByte));
        }

        for (RosettaParser.RosettaExternalEnumValueContext evCtx : ctx.rosettaExternalEnumValue()) {
            RExternalEnumValue ev = (RExternalEnumValue) visitRosettaExternalEnumValue(evCtx);
            ee.values().add(ev);
        }

        return ee;
    }

    /**
     * Visits a {@code rosettaExternalRegularAttribute} rule and builds an
     * {@link RExternalRegularAttribute}.
     *
     * <p>Grammar:
     * {@code (PLUS | MINUS) validID rosettaExternalSynonym* ruleReferenceAnnotation*}
     */
    @Override
    public Object visitRosettaExternalRegularAttribute(
            RosettaParser.RosettaExternalRegularAttributeContext ctx) {
        RExternalRegularAttribute attr = new RExternalRegularAttribute();
        attr.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        populateAdditionAndSign(ctx.PLUS(), ctx.MINUS(), attr::setAddition,
                attr::putTokenRange);

        attr.setName(ctx.validID().getText());
        attr.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        for (RosettaParser.RosettaExternalSynonymContext esCtx : ctx.rosettaExternalSynonym()) {
            RExternalSynonym es = (RExternalSynonym) visitRosettaExternalSynonym(esCtx);
            attr.synonyms().add(es);
        }
        for (RosettaParser.RuleReferenceAnnotationContext rraCtx : ctx.ruleReferenceAnnotation()) {
            RRuleReferenceAnnotation rra = (RRuleReferenceAnnotation)
                    visitRuleReferenceAnnotation(rraCtx);
            attr.ruleRefs().add(rra);
        }

        return attr;
    }

    /**
     * Visits a {@code rosettaExternalEnumValue} rule and builds an
     * {@link RExternalEnumValue}.
     *
     * <p>Grammar: {@code (PLUS | MINUS) validID rosettaExternalEnumSynonym*}
     */
    @Override
    public Object visitRosettaExternalEnumValue(
            RosettaParser.RosettaExternalEnumValueContext ctx) {
        RExternalEnumValue ev = new RExternalEnumValue();
        ev.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        populateAdditionAndSign(ctx.PLUS(), ctx.MINUS(), ev::setAddition,
                ev::putTokenRange);

        ev.setName(ctx.validID().getText());
        ev.putTokenRange("name",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));

        for (RosettaParser.RosettaExternalEnumSynonymContext esCtx : ctx.rosettaExternalEnumSynonym()) {
            RExternalEnumSynonym es = (RExternalEnumSynonym)
                    visitRosettaExternalEnumSynonym(esCtx);
            ev.synonyms().add(es);
        }

        return ev;
    }

    /**
     * Visits a {@code rosettaExternalClassSynonym} rule and builds an
     * {@link RExternalClassSynonym}.
     *
     * <p>Grammar:
     * {@code LBRACK (VALUE classSynonymValue)? META rosettaMetaSynonymValue RBRACK}
     */
    @Override
    public Object visitRosettaExternalClassSynonym(
            RosettaParser.RosettaExternalClassSynonymContext ctx) {
        RExternalClassSynonym ecs = new RExternalClassSynonym();
        ecs.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        putBracketRanges(ecs, ctx.LBRACK(), ctx.RBRACK());

        // (VALUE classSynonymValue)?
        if (ctx.VALUE() != null && ctx.classSynonymValue() != null) {
            ecs.putTokenRange("value",
                    AstBuilderHelper.rangeOfToken(ctx.VALUE().getSymbol(), fileName, charToByte));
            ecs.setValue((RClassSynonymValue) visitClassSynonymValue(ctx.classSynonymValue()));
        }

        // META rosettaMetaSynonymValue (required)
        if (ctx.META() != null) {
            ecs.putTokenRange("meta",
                    AstBuilderHelper.rangeOfToken(ctx.META().getSymbol(), fileName, charToByte));
        }
        if (ctx.rosettaMetaSynonymValue() != null) {
            ecs.setMeta((RMetaSynonymValue) visitRosettaMetaSynonymValue(ctx.rosettaMetaSynonymValue()));
        } else {
            throw new AstBuildException(
                    "rosettaExternalClassSynonym missing META rosettaMetaSynonymValue (grammar-impossible)");
        }

        return ecs;
    }

    /**
     * Visits a {@code rosettaExternalSynonym} rule and builds an
     * {@link RExternalSynonym}.
     *
     * <p>Grammar: {@code LBRACK rosettaSynonymBody RBRACK}
     */
    @Override
    public Object visitRosettaExternalSynonym(RosettaParser.RosettaExternalSynonymContext ctx) {
        RExternalSynonym es = new RExternalSynonym();
        es.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        putBracketRanges(es, ctx.LBRACK(), ctx.RBRACK());

        if (ctx.rosettaSynonymBody() != null) {
            es.setBody((RSynonymBody) visitRosettaSynonymBody(ctx.rosettaSynonymBody()));
        } else {
            throw new AstBuildException(
                    "rosettaExternalSynonym missing rosettaSynonymBody (grammar-impossible)");
        }

        return es;
    }

    /**
     * Visits a {@code rosettaExternalEnumSynonym} rule and builds an
     * {@link RExternalEnumSynonym}.
     *
     * <p>Grammar:
     * <pre>
     * rosettaExternalEnumSynonym:
     *     LBRACK VALUE STRING (DEFINITION STRING)? (PATTERN STRING STRING)? RBRACK
     * ;
     * </pre>
     */
    @Override
    public Object visitRosettaExternalEnumSynonym(
            RosettaParser.RosettaExternalEnumSynonymContext ctx) {
        RExternalEnumSynonym es = new RExternalEnumSynonym();
        es.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        putBracketRanges(es, ctx.LBRACK(), ctx.RBRACK());
        es.putTokenRange("value",
                AstBuilderHelper.rangeOfToken(ctx.VALUE().getSymbol(), fileName, charToByte));

        List<TerminalNode> strings = ctx.STRING();
        if (strings.isEmpty()) {
            throw new AstBuildException(
                    "rosettaExternalEnumSynonym missing VALUE STRING (grammar-impossible)");
        }
        int idx = 0;
        es.setValue(AstBuilderHelper.stripQuotes(strings.get(idx++).getText()));

        if (ctx.DEFINITION() != null) {
            es.putTokenRange("definition",
                    AstBuilderHelper.rangeOfToken(ctx.DEFINITION().getSymbol(), fileName, charToByte));
            if (idx >= strings.size()) {
                throw new AstBuildException(
                        "rosettaExternalEnumSynonym DEFINITION missing STRING (grammar-impossible)");
            }
            es.setDefinitionText(AstBuilderHelper.stripQuotes(strings.get(idx++).getText()));
        }

        if (ctx.PATTERN() != null) {
            es.putTokenRange("pattern",
                    AstBuilderHelper.rangeOfToken(ctx.PATTERN().getSymbol(), fileName, charToByte));
            if (idx + 1 >= strings.size()) {
                throw new AstBuildException(
                        "rosettaExternalEnumSynonym PATTERN missing two STRINGs (grammar-impossible)");
            }
            es.setPatternMatch(AstBuilderHelper.stripQuotes(strings.get(idx++).getText()));
            es.setPatternReplace(AstBuilderHelper.stripQuotes(strings.get(idx++).getText()));
        }

        return es;
    }

    // -- Task 14 helpers ------------------------------------------------------

    /**
     * Functional interface for storing a token range on a node. Matches
     * {@code RNode::putTokenRange}.
     */
    @FunctionalInterface
    private interface TokenRangeSetter {
        void put(String key, SourceRange range);
    }

    /**
     * Populates a source-names list from a list of {@code qualifiedName} parse
     * contexts. Used by the synonym-declaration forms and the external source
     * body that each carry {@code qualifiedName (COMMA qualifiedName)*}.
     *
     * <p>Also stores per-entry token ranges on the parent node, keyed as
     * {@code <tokenPrefix>_<index>} (e.g. {@code superSource_0},
     * {@code superSource_1}), so M3 can produce diagnostics that point at
     * the specific failing entry in multi-valued cross-references.
     *
     * <p>M3 carryover — see D5/E2 in the M3 design spec.
     */
    private void populateSourceNames(
            RNode parent,
            String tokenPrefix,
            List<String> target,
            List<RosettaParser.QualifiedNameContext> names) {
        for (int i = 0; i < names.size(); i++) {
            RosettaParser.QualifiedNameContext qn = names.get(i);
            target.add(AstBuilderHelper.textOfQualifiedName(qn));
            parent.putTokenRange(
                tokenPrefix + "_" + i,
                AstBuilderHelper.rangeOf(qn, fileName, charToByte));
        }
    }

    /**
     * Stores {@code openBrack}/{@code closeBrack} token ranges on a node.
     * Safe when either terminal is null.
     */
    private void putBracketRanges(
            RNode node, TerminalNode lbrack, TerminalNode rbrack) {
        if (lbrack != null) {
            node.putTokenRange("openBrack",
                    AstBuilderHelper.rangeOfToken(lbrack.getSymbol(), fileName, charToByte));
        }
        if (rbrack != null) {
            node.putTokenRange("closeBrack",
                    AstBuilderHelper.rangeOfToken(rbrack.getSymbol(), fileName, charToByte));
        }
    }

    /**
     * Shared logic for {@link RSynonymValue} and {@link RMetaSynonymValue}.
     * Both grammar rules share the structure:
     * {@code STRING (rosettaSynonymRef INT_LITERAL)? (PATH STRING)? (MAPS INT_LITERAL)?}.
     *
     * <p>The caller supplies setter references so the helper is usable for
     * both node types without reflection. The {@code tokenHook} is invoked
     * for optional keyword tokens (currently {@code "path"} and {@code "maps"})
     * so the caller can wire keyword ranges on its own node.
     */
    private void populateSynonymValueCommon(
            List<TerminalNode> strings,
            RosettaParser.RosettaSynonymRefContext refCtx,
            List<TerminalNode> intLiterals,
            TerminalNode pathKeyword,
            TerminalNode mapsKeyword,
            Consumer<String> setName,
            Consumer<RSynonymRef> setRef,
            Consumer<BigInteger> setRefValue,
            Consumer<String> setPath,
            Consumer<BigInteger> setMaps,
            Consumer<String> tokenHook) {

        if (strings.isEmpty()) {
            throw new AstBuildException(
                    "synonym value missing name STRING (grammar-impossible)");
        }
        setName.accept(AstBuilderHelper.stripQuotes(strings.get(0).getText()));

        // (rosettaSynonymRef INT_LITERAL)? — refValue is INT_LITERAL(0) if ref present.
        // Parsed as BigInteger to handle arbitrary-precision integers.
        int intIdx = 0;
        if (refCtx != null) {
            setRef.accept(synonymRefFrom(refCtx));
            if (intIdx < intLiterals.size()) {
                setRefValue.accept(new BigInteger(intLiterals.get(intIdx++).getText()));
            }
        }

        // (PATH STRING)? — path string is strings.get(1) when PATH is present
        if (pathKeyword != null) {
            tokenHook.accept("path");
            if (strings.size() < 2) {
                throw new AstBuildException(
                        "synonym value PATH missing STRING (grammar-impossible)");
            }
            setPath.accept(AstBuilderHelper.stripQuotes(strings.get(1).getText()));
        }

        // (MAPS INT_LITERAL)? — remaining INT_LITERAL after the optional refValue.
        // Parsed as BigInteger to handle arbitrary-precision integers.
        if (mapsKeyword != null) {
            tokenHook.accept("maps");
            if (intIdx >= intLiterals.size()) {
                throw new AstBuildException(
                        "synonym value MAPS missing INT_LITERAL (grammar-impossible)");
            }
            setMaps.accept(new BigInteger(intLiterals.get(intIdx++).getText()));
        }
    }

    /**
     * Returns the {@link RSynonymRef} corresponding to the TAG/COMPONENT_ID
     * terminal present on the given context. Throws if neither is set
     * (grammar-impossible).
     */
    private static RSynonymRef synonymRefFrom(RosettaParser.RosettaSynonymRefContext ctx) {
        if (ctx.TAG() != null) {
            return RSynonymRef.TAG;
        }
        if (ctx.COMPONENT_ID() != null) {
            return RSynonymRef.COMPONENT_ID;
        }
        throw new AstBuildException(
                "rosettaSynonymRef matched neither TAG nor COMPONENT_ID (grammar-impossible)");
    }

    /**
     * Dispatches a {@code rosettaMapTest} context to the correct concrete map
     * test visitor. Throws if no alternative is present (grammar-impossible).
     */
    private RMapTest dispatchMapTest(RosettaParser.RosettaMapTestContext ctx) {
        if (ctx.rosettaMapPath() != null) {
            return (RMapTest) visitRosettaMapPath(ctx.rosettaMapPath());
        }
        if (ctx.rosettaMapRosettaPath() != null) {
            return (RMapTest) visitRosettaMapRosettaPath(ctx.rosettaMapRosettaPath());
        }
        if (ctx.rosettaMapTestExpression() != null) {
            // rosettaMapTestExpression dispatches to the three labelled alternatives
            // (Exists/Absent/Equality) via the visitor base class automatically.
            Object result = visit(ctx.rosettaMapTestExpression());
            if (result instanceof RMapTest mt) {
                return mt;
            }
            throw new AstBuildException(
                    "rosettaMapTestExpression visitor returned non-RMapTest: "
                            + (result == null ? "null" : result.getClass().getName()));
        }
        if (ctx.rosettaMapTestFunc() != null) {
            return (RMapTest) visitRosettaMapTestFunc(ctx.rosettaMapTestFunc());
        }
        throw new AstBuildException(
                "rosettaMapTest matched no alternative: " + ctx.getText());
    }

    /**
     * Renders a {@code rosettaAttributeReference} context as a string of the
     * form {@code Qualified.Name -> attr1 -> attr2}.
     */
    private static String textOfAttributeReference(
            RosettaParser.RosettaAttributeReferenceContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
        for (var vid : ctx.validID()) {
            sb.append(" -> ").append(vid.getText());
        }
        return sb.toString();
    }

    /**
     * Counts how many of the body's own {@code STRING} tokens belong to the
     * head alternative (HINT, META_ONLY, or the VALUE alternative's optional
     * trailing META clause), excluding the STRINGs consumed by the body's
     * trailing modifiers.
     *
     * <p>Trailing modifiers that each consume STRINGs are:
     * <ul>
     *   <li>{@code DATE_FORMAT STRING} — 1 STRING</li>
     *   <li>{@code PATTERN STRING STRING} — 2 STRINGs</li>
     *   <li>{@code MAPPER STRING} — 1 STRING</li>
     * </ul>
     *
     * <p>The VALUE alternative's own value STRINGs live on
     * {@link RosettaParser.RosettaSynonymValueContext} children (NOT on the
     * body's own STRING list), so on the VALUE alternative this count returns
     * the number of trailing-META STRINGs only.
     */
    private static int countHeadStringsForHintOrMeta(
            RosettaParser.RosettaSynonymBodyContext ctx) {
        int total = ctx.STRING().size();
        int trailing = 0;
        if (ctx.DATE_FORMAT() != null) {
            trailing++;
        }
        if (ctx.PATTERN() != null) {
            trailing += 2;
        }
        if (ctx.MAPPER() != null) {
            trailing++;
        }
        return total - trailing;
    }

    /**
     * Populates an external source body — EXTENDS list, LBRACE/RBRACE tokens,
     * ENUMS keyword, nested classes, and nested enums. Shared between
     * {@code visitRosettaExternalSynonymSource} and
     * {@code visitRosettaExternalRuleSource} since those two rules have
     * identical body structure.
     */
    private void populateExternalSourceBody(
            RNode parentNode,
            List<String> superSourceNames,
            List<RExternalClass> classes,
            List<RExternalEnum> enums,
            TerminalNode extendsKw,
            List<RosettaParser.QualifiedNameContext> qualifiedNames,
            TerminalNode lbrace,
            TerminalNode rbrace,
            TerminalNode enumsKw,
            List<RosettaParser.RosettaExternalClassContext> classCtxs,
            List<RosettaParser.RosettaExternalEnumContext> enumCtxs,
            TokenRangeSetter tokenSetter) {

        // (EXTENDS qualifiedName (COMMA qualifiedName)*)?
        if (extendsKw != null) {
            tokenSetter.put("extends",
                    AstBuilderHelper.rangeOfToken(extendsKw.getSymbol(), fileName, charToByte));
            populateSourceNames(parentNode, "superSource", superSourceNames, qualifiedNames);
        }

        // LBRACE / RBRACE delimiter tokens
        if (lbrace != null) {
            tokenSetter.put("openBrace",
                    AstBuilderHelper.rangeOfToken(lbrace.getSymbol(), fileName, charToByte));
        }
        if (rbrace != null) {
            tokenSetter.put("closeBrace",
                    AstBuilderHelper.rangeOfToken(rbrace.getSymbol(), fileName, charToByte));
        }

        // rosettaExternalClass*
        for (RosettaParser.RosettaExternalClassContext ecCtx : classCtxs) {
            classes.add((RExternalClass) visitRosettaExternalClass(ecCtx));
        }

        // (ENUMS rosettaExternalEnum*)?
        if (enumsKw != null) {
            tokenSetter.put("enums",
                    AstBuilderHelper.rangeOfToken(enumsKw.getSymbol(), fileName, charToByte));
        }
        for (RosettaParser.RosettaExternalEnumContext eeCtx : enumCtxs) {
            enums.add((RExternalEnum) visitRosettaExternalEnum(eeCtx));
        }
    }

    /**
     * Sets the {@code addition} flag on an external attribute/enum value based
     * on the {@code PLUS}/{@code MINUS} terminal and records the sign token
     * range under {@code "sign"}. Throws if neither terminal is present.
     */
    private void populateAdditionAndSign(
            TerminalNode plus,
            TerminalNode minus,
            Consumer<Boolean> setAddition,
            TokenRangeSetter tokenSetter) {
        if (plus != null) {
            setAddition.accept(Boolean.TRUE);
            tokenSetter.put("sign",
                    AstBuilderHelper.rangeOfToken(plus.getSymbol(), fileName, charToByte));
        } else if (minus != null) {
            setAddition.accept(Boolean.FALSE);
            tokenSetter.put("sign",
                    AstBuilderHelper.rangeOfToken(minus.getSymbol(), fileName, charToByte));
        } else {
            throw new AstBuildException(
                    "external attribute/enum value missing PLUS/MINUS (grammar-impossible)");
        }
    }

    // =========================================================================
    // Expression visitors (Task 13)
    //
    // Visit methods are organised by grammar category:
    //   1. Binary expressions (with-left forms): visitDefaultExpr ... visitOrExpr
    //   2. Postfix expressions (with-left forms): visitFeatureCallExpr ... visitExtractExpr
    //   3. WithoutLeft expressions: same node types as their with-left counterparts
    //      with left/argument set to null. Includes the prefix unary form
    //      (visitAdditiveWithoutLeftExpr handles the (PLUS|MINUS) expression rule).
    //   4. Primary expressions: literals, references, super, item, empty, paren
    //
    // Many visitors share identical structure (create node, set source range,
    // set argument, set enum). Where the only variation is the operator enum
    // value, visitors delegate to private helpers below to eliminate duplication.
    // =========================================================================

    /**
     * Visits an expression child context and casts the result to {@link RExpression}.
     * Returns {@code null} if the context is null. Throws {@link AstBuildException}
     * if a visitor returns a non-null result that is not an {@code RExpression} —
     * this indicates a builder bug rather than a grammar issue.
     */
    private RExpression visitExpr(RosettaParser.ExpressionContext ctx) {
        if (ctx == null) {
            return null;
        }
        Object result = visit(ctx);
        if (result == null) {
            return null;
        }
        if (result instanceof RExpression expr) {
            return expr;
        }
        throw new AstBuildException(
                "Expression visitor returned non-RExpression type: "
                        + result.getClass().getName()
                        + " for context " + ctx.getClass().getSimpleName());
    }

    /**
     * Builds an {@link RInlineFunction} from an explicit
     * {@code inlineFunction} context: {@code (closureParameter ...)? LBRACK expression RBRACK}.
     * Tracks the {@code LBRACK} and {@code RBRACK} delimiters under
     * {@code "openBrack"} and {@code "closeBrack"}.
     */
    private RInlineFunction buildInlineFunction(RosettaParser.InlineFunctionContext ctx) {
        if (ctx == null) {
            return null;
        }
        RInlineFunction fn = new RInlineFunction();
        fn.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        fn.setImplicit(false);
        // v3.2 seat 8: every declared parameter is a NODE (upstream's ClosureParameter),
        // ranged on its ID token, reached through RInlineFunction.children() — the
        // binding target a reference to it resolves to. addParameter keeps the
        // name view (paramNames) in lockstep.
        for (RosettaParser.ClosureParameterContext cp : ctx.closureParameter()) {
            if (cp.ID() != null) {
                com.regnosys.rosetta.ast.expressions.supporting.RClosureParameter parameter =
                        new com.regnosys.rosetta.ast.expressions.supporting.RClosureParameter();
                parameter.setName(cp.ID().getText());
                parameter.setSourceRange(
                        AstBuilderHelper.rangeOfToken(cp.ID().getSymbol(), fileName, charToByte));
                fn.addParameter(parameter);
            }
        }
        fn.setBody(buildExprWithThen(ctx.exprWithThen()));
        if (ctx.LBRACK() != null) {
            fn.putTokenRange("openBrack",
                    AstBuilderHelper.rangeOfToken(ctx.LBRACK().getSymbol(), fileName, charToByte));
        }
        if (ctx.RBRACK() != null) {
            fn.putTokenRange("closeBrack",
                    AstBuilderHelper.rangeOfToken(ctx.RBRACK().getSymbol(), fileName, charToByte));
        }
        return fn;
    }

    /**
     * Builds an {@link RInlineFunction} from an
     * {@code implicitInlineFunction} context (a bare expression used as a
     * lambda body without explicit closure parameters). Sets {@code isImplicit=true}.
     */
    private RInlineFunction buildImplicitInlineFunction(
            RosettaParser.ImplicitInlineFunctionContext ctx) {
        if (ctx == null) {
            return null;
        }
        RInlineFunction fn = new RInlineFunction();
        fn.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        fn.setImplicit(true);
        fn.setBody(visitExpr(ctx.expression()));
        return fn;
    }

    /**
     * Builds an {@link RInlineFunction} from whichever form is present in the
     * grammar alternative {@code (inlineFunction | implicitInlineFunction)}.
     * Returns {@code null} if both are absent.
     */
    private RInlineFunction buildInlineFunctionOrImplicit(
            RosettaParser.InlineFunctionContext explicit,
            RosettaParser.ImplicitInlineFunctionContext implicit) {
        if (explicit != null) {
            return buildInlineFunction(explicit);
        }
        if (implicit != null) {
            return buildImplicitInlineFunction(implicit);
        }
        return null;
    }

    /**
     * Extracts the {@link CardMod} enum value from a
     * {@code cardinalityModifier} context, or {@code null} if absent.
     */
    private CardMod extractCardMod(RosettaParser.CardinalityModifierContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.ANY() != null) {
            return CardMod.ANY;
        }
        if (ctx.ALL() != null) {
            return CardMod.ALL;
        }
        throw new AstBuildException("cardinalityModifier matched neither ANY nor ALL");
    }

    /**
     * Extracts the {@link ExistsModifier} enum value from an
     * {@code existsModifier} context, or {@code null} if absent.
     */
    private ExistsModifier extractExistsModifier(RosettaParser.ExistsModifierContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.SINGLE() != null) {
            return ExistsModifier.SINGLE;
        }
        if (ctx.MULTIPLE() != null) {
            return ExistsModifier.MULTIPLE;
        }
        throw new AstBuildException("existsModifier matched neither SINGLE nor MULTIPLE");
    }

    /**
     * Extracts the {@link Necessity} enum value from a {@code necessity}
     * context. Throws if neither alternative is present (the grammar rule
     * is non-optional).
     */
    private Necessity extractNecessity(RosettaParser.NecessityContext ctx) {
        if (ctx == null) {
            return null;
        }
        if (ctx.OPTIONAL() != null) {
            return Necessity.OPTIONAL;
        }
        if (ctx.REQUIRED() != null) {
            return Necessity.REQUIRED;
        }
        throw new AstBuildException("necessity matched neither OPTIONAL nor REQUIRED");
    }

    /**
     * Determines the {@link CompOp} enum value from a comparison context.
     * Throws if none of the four operator tokens matched (grammar-impossible).
     */
    private CompOp extractCompOp(
            TerminalNode lt,
            TerminalNode gt,
            TerminalNode lte,
            TerminalNode gte) {
        if (lt != null) return CompOp.LT;
        if (gt != null) return CompOp.GT;
        if (lte != null) return CompOp.LTE;
        if (gte != null) return CompOp.GTE;
        throw new AstBuildException("ComparisonExpr matched no operator token");
    }

    /**
     * Returns the first non-null comparison operator {@link Token}. Used to
     * record the operator source range on comparison visitors. Mirrors the
     * precedence order of {@link #extractCompOp}.
     */
    private static Token compOpToken(
            TerminalNode lt,
            TerminalNode gt,
            TerminalNode lte,
            TerminalNode gte) {
        if (lt != null) return lt.getSymbol();
        if (gt != null) return gt.getSymbol();
        if (lte != null) return lte.getSymbol();
        if (gte != null) return gte.getSymbol();
        throw new AstBuildException("ComparisonExpr matched no operator token");
    }

    /**
     * Collects {@code validID} text values into a string list (used by
     * choice expressions which carry an attribute name list).
     */
    private List<String> collectValidIds(List<RosettaParser.ValidIDContext> ids) {
        List<String> result = new ArrayList<>(ids.size());
        for (RosettaParser.ValidIDContext vid : ids) {
            result.add(vid.getText());
        }
        return result;
    }

    /**
     * Builds an {@link RListOpExpr} for a postfix list operation. Handles
     * both with-argument and without-argument forms via a nullable argument
     * context. The {@code keywordToken} is the list-op keyword terminal
     * (e.g. {@code FLATTEN}, {@code DISTINCT}) whose source range is stored
     * under {@code "keyword"}.
     */
    private RListOpExpr buildListOpExpr(
            ParserRuleContext ctx,
            RosettaParser.ExpressionContext argCtx,
            ListOp op,
            Token keywordToken) {
        RListOpExpr node = new RListOpExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(argCtx != null
                ? visitExpr(argCtx)
                : syntheticImplicitInput(ctx, keywordToken));
        node.setOp(op);
        if (keywordToken != null) {
            node.putTokenRange("keyword",
                    AstBuilderHelper.rangeOfToken(keywordToken, fileName, charToByte));
        }
        return node;
    }

    /**
     * Builds an {@link RConversionExpr} for a {@code to-xxx} conversion.
     * Handles both with-argument and without-argument forms via a nullable
     * argument context. The optional target enum name applies only to
     * {@code to-enum qualifiedName} forms. The {@code keywordToken} is the
     * {@code TO_XXX} terminal whose source range is stored under
     * {@code "keyword"}.
     */
    private RConversionExpr buildConversionExpr(
            ParserRuleContext ctx,
            RosettaParser.ExpressionContext argCtx,
            ConversionKind kind,
            RosettaParser.QualifiedNameContext targetEnumQname,
            Token keywordToken) {
        RConversionExpr node = new RConversionExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(argCtx != null
                ? visitExpr(argCtx)
                : syntheticImplicitInput(ctx, keywordToken));
        node.setKind(kind);
        if (targetEnumQname != null) {
            node.setTargetEnumName(AstBuilderHelper.textOfQualifiedName(targetEnumQname));
        }
        if (keywordToken != null) {
            node.putTokenRange("keyword",
                    AstBuilderHelper.rangeOfToken(keywordToken, fileName, charToByte));
        }
        return node;
    }

    /**
     * Synthesises an {@link RImplicitVariable} for an operand elided in a
     * "without-argument" postfix form (e.g. a bare {@code only-element} or
     * {@code to-string} whose operand is the implicit input). Mirrors upstream
     * rune-dsl, which materialises {@code RosettaImplicitVariable} rather than
     * leaving the operand unset, so the AST stays faithful for every consumer
     * (type inference, Java codegen, IR) instead of each special-casing a null
     * operand. The node is source-ranged to the operation keyword — the closest
     * real source location, since the implicit operand itself has no token.
     */
    private RImplicitVariable syntheticImplicitInput(ParserRuleContext ctx, Token keywordToken) {
        RImplicitVariable node = new RImplicitVariable();
        node.setSourceRange(keywordToken != null
                ? AstBuilderHelper.rangeOfToken(keywordToken, fileName, charToByte)
                : AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        // Mark synthetic — distinguishes from the user-written literal `item`
        // keyword which can also appear in a parent op's argument slot
        // (e.g. `item only-element` inside a lambda body). See
        // RImplicitVariable javadoc + TypeInferenceEngine.isElidedOperandImplicitVariable
        // + ReferenceHandler.isElidedOperandTopLevel (Copilot R4 — predicate
        // tightening so non-synthetic literals route through the standard
        // Cat 8 + bare-item codegen paths).
        node.setSynthetic(true);
        return node;
    }

    /**
     * Builds an {@link RSwitchCase} for a {@code switchCaseOrDefault} alternative.
     * Tracks the {@code DEFAULT} or {@code THEN} keyword token.
     */
    private RSwitchCase buildSwitchCase(RosettaParser.SwitchCaseOrDefaultContext ctx) {
        RSwitchCase sc = new RSwitchCase();
        sc.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        if (ctx.DEFAULT() != null) {
            // DEFAULT exprWithThen (no guard, no THEN)
            sc.setDefault(true);
            sc.setExpression(buildExprWithThen(ctx.exprWithThen()));
            sc.putTokenRange("keyword",
                    AstBuilderHelper.rangeOfToken(ctx.DEFAULT().getSymbol(), fileName, charToByte));
        } else {
            // switchCaseGuard THEN exprWithThen
            sc.setDefault(false);
            if (ctx.switchCaseGuard() != null) {
                sc.setGuard(buildSwitchCaseGuard(ctx.switchCaseGuard()));
            }
            sc.setExpression(buildExprWithThen(ctx.exprWithThen()));
            if (ctx.THEN() != null) {
                sc.putTokenRange("keyword",
                        AstBuilderHelper.rangeOfToken(ctx.THEN().getSymbol(), fileName, charToByte));
            }
        }
        return sc;
    }

    /**
     * Builds an {@link RSwitchCaseGuard} from a {@code switchCaseGuard} context.
     * Either a literal (STRING quotes stripped; numeric/boolean raw text) — with the
     * lexer-terminal kind recorded beside the value, since the value text alone cannot
     * tell a STRING guard {@code "42"} from an INT_LITERAL guard {@code 42} — or a
     * qualified name reference.
     */
    private RSwitchCaseGuard buildSwitchCaseGuard(RosettaParser.SwitchCaseGuardContext ctx) {
        RSwitchCaseGuard guard = new RSwitchCaseGuard();
        guard.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        if (ctx.literal() != null) {
            guard.setKind(SwitchGuardKind.LITERAL);
            // STRING literals need quote stripping; numeric/boolean literals don't
            var lit = ctx.literal();
            if (lit.STRING() != null) {
                guard.setLiteralValue(AstBuilderHelper.stripQuotes(lit.STRING().getText()));
                guard.setLiteralKind(SwitchGuardLiteralKind.STRING);
            } else {
                guard.setLiteralValue(lit.getText());
                if (lit.INT_LITERAL() != null) {
                    guard.setLiteralKind(SwitchGuardLiteralKind.INT);
                } else if (lit.BIG_DECIMAL() != null) {
                    guard.setLiteralKind(SwitchGuardLiteralKind.DECIMAL);
                } else if (lit.TRUE() != null || lit.FALSE() != null) {
                    guard.setLiteralKind(SwitchGuardLiteralKind.BOOLEAN);
                } else {
                    // Fail closed: every alternative of the grammar's literal rule is named above
                    // (STRING | INT_LITERAL | BIG_DECIMAL | TRUE | FALSE); a sixth added later must
                    // surface here, never be classified BOOLEAN by fallthrough.
                    throw new IllegalStateException("switch-case guard literal of an unknown kind: "
                            + lit.getText() + " in " + fileName);
                }
            }
        } else if (ctx.qualifiedName() != null) {
            guard.setKind(SwitchGuardKind.NAME);
            guard.setQualifiedName(
                    AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
        }
        return guard;
    }

    /**
     * Builds an {@link RWithMetaEntry} from a {@code withMetaEntry} context.
     * Tracks the {@code COLON} separator token.
     */
    private RWithMetaEntry buildWithMetaEntry(RosettaParser.WithMetaEntryContext ctx) {
        RWithMetaEntry entry = new RWithMetaEntry();
        entry.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        entry.setMetaName(ctx.validID().getText());
        entry.setValue(buildExprWithThen(ctx.exprWithThen()));
        if (ctx.COLON() != null) {
            entry.putTokenRange("colon",
                    AstBuilderHelper.rangeOfToken(ctx.COLON().getSymbol(), fileName, charToByte));
        }
        return entry;
    }

    /**
     * Builds an {@link RKeyValuePair} from a {@code constructorKeyValuePair}
     * context. Tracks the {@code COLON} separator token.
     */
    private RKeyValuePair buildKeyValuePair(RosettaParser.ConstructorKeyValuePairContext ctx) {
        RKeyValuePair kvp = new RKeyValuePair();
        kvp.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        kvp.setKey(ctx.validID().getText());

        var ewak = ctx.expressionWithAsKey();
        if (ewak != null) {
            kvp.setValue(buildExprWithThen(ewak.exprWithThen()));
            kvp.setAsKey(ewak.AS_KEY() != null);
        }
        if (ctx.COLON() != null) {
            kvp.putTokenRange("colon",
                    AstBuilderHelper.rangeOfToken(ctx.COLON().getSymbol(), fileName, charToByte));
        }
        return kvp;
    }

    /**
     * Builds an {@link ROnlyExistsElement} from an {@code onlyExistsElement}
     * context. Tracks each {@code ARROW} token in the feature chain under a
     * numbered key {@code "arrow0"}, {@code "arrow1"}, ...
     */
    private ROnlyExistsElement buildOnlyExistsElement(
            RosettaParser.OnlyExistsElementContext ctx) {
        ROnlyExistsElement element = new ROnlyExistsElement();
        element.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        if (ctx.ITEM() != null) {
            element.setRootIsItem(true);
        } else if (ctx.qualifiedName() != null) {
            element.setRoot(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
        }

        for (RosettaParser.ValidIDContext vid : ctx.validID()) {
            element.featureChain().add(vid.getText());
        }
        List<TerminalNode> arrows = ctx.ARROW();
        for (int i = 0; i < arrows.size(); i++) {
            element.putTokenRange("arrow" + i,
                    AstBuilderHelper.rangeOfToken(arrows.get(i).getSymbol(), fileName, charToByte));
        }

        // Synthesize the written path as ONE expression — root -> chain[0] -> ... -> the
        // LEAF (v3.2 seat 8, F15: upstream models an only-exists argument as an ordinary
        // RosettaFeatureCall and binds its leaf like any other feature; v3.2 seat 12, D52 H3:
        // an `item` root is a real RImplicitVariable now, chained like a symbol root) — and wire it
        // into children() so the existing symbol-linker (GlobalResolutionPass) + type-directed
        // resolver populate resolvedSymbol/resolvedFeature on every hop. The generator keeps
        // reading receiverExpression() — the leaf's receiver, the same node it read before
        // the leaf was a node — for the resolved parent type it renders `only exists` from
        // (ExistenceHandler.handle(ROnlyExistsExpr)).
        RFeatureCall leaf = buildOnlyExistsPath(ctx);
        element.setLeafReference(leaf);
        element.setReceiverExpression(leaf == null ? null : leaf.receiver());
        return element;
    }

    /**
     * Synthesizes the whole written path of an only-exists element as a feature-call
     * chain — the root wrapped in an {@link RFeatureCall} for EVERY feature, the leaf
     * included ({@code root -> chain[0] -> ... -> chain[n-1]}) — and returns the LEAF
     * call; its receiver is the shared parent navigation the generator renders from
     * ({@code root -> ... -> chain[n-2]}, or the bare root for a one-hop path). Every hop
     * is ranged from the element's start to its own feature token, so a hop's range ends
     * at the feature it names (the byte-offset audits and the resolution differential
     * read the feature's position off the hop's end); each hop carries its
     * {@code operator} and {@code feature} token ranges from the element's real tokens
     * (qualifiedName / ITEM, validID, ARROW) — the navigation genuinely exists in source.
     *
     * <p>The root is a {@link RSymbolReference} for a named root, and — since v3.2 seat 12
     * (D52, H3: M6 the {@code item ->} only-exists root) — a real, NON-synthetic
     * {@link RImplicitVariable} for an {@code item} root: the user wrote {@code item}, the
     * node is ranged at the token and carries its {@code keyword} token range, and the
     * features chain off it exactly as off a symbol root. Upstream parses an only-exists
     * argument as an ordinary feature call over the implicit variable, so {@code item -> p}
     * and {@code p} are the SAME navigation there (the released 9.83.0 plugin renders both
     * {@code onlyExists(MapperS.of(paths), <all attrs>, ["p"])} in a data-type condition —
     * the hold-out group {@code only-exists-item-root}); the fork's generator resolves the
     * implicit receiver through the same gm-aware walk cdm's {@code item -> location}
     * condition renders through. Inside a lambda the type engine types the literal item
     * (Cat 8) and binds the hops (Cat 1) — the resolution differential's s25 rows; at a
     * type-condition top level the engine leaves the hops unbound and the generator's walk
     * resolves them by name. Before this seat the item root returned {@code null} here and
     * the generator wrote the legacy dotted placeholder ({@code onlyExists(item,
     * ["item.p"], ...)} — non-compiling in a condition, compiling and WRONG in a lambda).
     *
     * <p>Returns {@code null} for the ONE edge shape the generator still declines on
     * (keeping the legacy placeholder rendering, hence no regression): a root with no
     * feature chain ({@code x only exists}, or a bare {@code item only exists}) — the parent
     * is the implicit variable itself; the generator's arms 3 / 4 synthesise the implicit
     * instance for the bare-symbol form, and the bare {@code item} form has no vendored or
     * chaos carrier (the seat-12 census), so it stays banked rather than guessed.
     */
    private RFeatureCall buildOnlyExistsPath(RosettaParser.OnlyExistsElementContext ctx) {
        List<RosettaParser.ValidIDContext> features = ctx.validID();
        if (features.isEmpty()) {
            return null; // no navigation — implicit-variable parent, declined (a bare symbol or a bare `item`)
        }
        RExpression receiver;
        if (ctx.qualifiedName() != null) {
            RSymbolReference root = new RSymbolReference();
            root.setSourceRange(AstBuilderHelper.rangeOf(ctx.qualifiedName(), fileName, charToByte));
            root.setName(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
            receiver = root;
        } else if (ctx.ITEM() != null) {
            // v3.2 seat 12 (D52, H3): the item root — the implicit variable the user wrote, never synthetic
            RImplicitVariable root = new RImplicitVariable();
            root.setSourceRange(AstBuilderHelper.rangeOfToken(ctx.ITEM().getSymbol(), fileName, charToByte));
            root.putTokenRange("keyword",
                    AstBuilderHelper.rangeOfToken(ctx.ITEM().getSymbol(), fileName, charToByte));
            receiver = root;
        } else {
            return null; // parse-robustness: the grammar admits no third root form
        }

        RFeatureCall leaf = null;
        List<TerminalNode> arrows = ctx.ARROW();
        for (int i = 0; i < features.size(); i++) {
            RosettaParser.ValidIDContext vid = features.get(i);
            RFeatureCall fc = new RFeatureCall();
            // the hop spans the element's start to ITS feature token (the pre-seat-8 code
            // ranged every hop over the whole element, so a hop's end named the LAST
            // feature, not its own)
            fc.setSourceRange(SourceRange.of(ctx.getStart(), vid.getStop(), fileName, charToByte));
            fc.setReceiver(receiver);
            fc.setFeatureName(vid.getText());
            if (i < arrows.size()) {
                fc.putTokenRange("operator",
                        AstBuilderHelper.rangeOfToken(arrows.get(i).getSymbol(), fileName, charToByte));
            }
            fc.putTokenRange("feature",
                    AstBuilderHelper.rangeOf(vid, fileName, charToByte));
            receiver = fc;
            leaf = fc;
        }
        return leaf;
    }

    // =========================================================================
    // Binary expression visitors
    //
    // Each visitor builds the canonical node type for the operator, sets
    // source range over the full context, wires left/right operands via
    // {@link #visitExpr}, and records the operator terminal under token
    // range key {@code "operator"} so IDE tooling can hover/highlight the
    // exact operator span.
    //
    // Grammar alternatives covered (from expression rule):
    //   DefaultExpr, JoinExpr, ContainsExpr, DisjointExpr,
    //   MultiplicativeExpr, AdditiveExpr, ComparisonExpr, EqualityExpr,
    //   AndExpr, OrExpr, ThenExpr.
    // =========================================================================

    @Override
    public Object visitDefaultExpr(RosettaParser.DefaultExprContext ctx) {
        RDefaultExpr node = new RDefaultExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setLeft(visitExpr(ctx.expression(0)));
        node.setRight(visitExpr(ctx.expression(1)));
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(ctx.DEFAULT().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitJoinExpr(RosettaParser.JoinExprContext ctx) {
        RJoinExpr node = new RJoinExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setLeft(visitExpr(ctx.expression(0)));
        if (ctx.expression().size() > 1) {
            node.setSeparator(visitExpr(ctx.expression(1)));
        }
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(ctx.JOIN().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitContainsExpr(RosettaParser.ContainsExprContext ctx) {
        RContainsExpr node = new RContainsExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setLeft(visitExpr(ctx.expression(0)));
        node.setRight(visitExpr(ctx.expression(1)));
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(ctx.CONTAINS().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitDisjointExpr(RosettaParser.DisjointExprContext ctx) {
        RDisjointExpr node = new RDisjointExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setLeft(visitExpr(ctx.expression(0)));
        node.setRight(visitExpr(ctx.expression(1)));
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(ctx.DISJOINT().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitMultiplicativeExpr(RosettaParser.MultiplicativeExprContext ctx) {
        RArithmeticExpr node = new RArithmeticExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setLeft(visitExpr(ctx.expression(0)));
        node.setRight(visitExpr(ctx.expression(1)));
        node.setOp(ctx.STAR() != null ? ArithOp.MULTIPLY : ArithOp.DIVIDE);
        TerminalNode op = ctx.STAR() != null ? ctx.STAR() : ctx.SLASH();
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(op.getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitAdditiveExpr(RosettaParser.AdditiveExprContext ctx) {
        RArithmeticExpr node = new RArithmeticExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setLeft(visitExpr(ctx.expression(0)));
        node.setRight(visitExpr(ctx.expression(1)));
        node.setOp(ctx.PLUS() != null ? ArithOp.PLUS : ArithOp.MINUS);
        TerminalNode op = ctx.PLUS() != null ? ctx.PLUS() : ctx.MINUS();
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(op.getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitComparisonExpr(RosettaParser.ComparisonExprContext ctx) {
        RComparisonExpr node = new RComparisonExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setLeft(visitExpr(ctx.expression(0)));
        node.setRight(visitExpr(ctx.expression(1)));
        node.setMod(extractCardMod(ctx.cardinalityModifier()));
        node.setOp(extractCompOp(ctx.LT(), ctx.GT(), ctx.LTE(), ctx.GTE()));
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(compOpToken(ctx.LT(), ctx.GT(), ctx.LTE(), ctx.GTE()), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitEqualityExpr(RosettaParser.EqualityExprContext ctx) {
        REqualityExpr node = new REqualityExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setLeft(visitExpr(ctx.expression(0)));
        node.setRight(visitExpr(ctx.expression(1)));
        node.setMod(extractCardMod(ctx.cardinalityModifier()));
        node.setOp(ctx.EQ() != null ? EqOp.EQ : EqOp.NEQ);
        TerminalNode op = ctx.EQ() != null ? ctx.EQ() : ctx.NEQ();
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(op.getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitAndExpr(RosettaParser.AndExprContext ctx) {
        RLogicalExpr node = new RLogicalExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setLeft(visitExpr(ctx.expression(0)));
        node.setRight(visitExpr(ctx.expression(1)));
        node.setOp(LogOp.AND);
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(ctx.AND().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitOrExpr(RosettaParser.OrExprContext ctx) {
        RLogicalExpr node = new RLogicalExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setLeft(visitExpr(ctx.expression(0)));
        node.setRight(visitExpr(ctx.expression(1)));
        node.setOp(LogOp.OR);
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(ctx.OR().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitExprWithThen(RosettaParser.ExprWithThenContext ctx) {
        return buildExprWithThen(ctx);
    }

    /**
     * Builds the {@code then}-chain wrapper rule
     * ({@code exprWithThen : expression thenSuffix*}). Produces a
     * left-associative chain of {@link RThenExpr} nodes — identical in shape +
     * source ranges to the former left-recursive {@code #ThenExpr} alternative
     * (each node spans the base expression's start through that suffix's stop).
     * With zero suffixes it returns the base {@code expression} unchanged, so a
     * {@code then}-free expression yields a byte-identical AST to pre-split.
     *
     * <p>The grammar split (Phase X1 closure) moved {@code then} out of the
     * left-recursive {@code expression} rule into this thin outer layer so the
     * conditional's operands are {@code then}-free by construction — fixing
     * {@code if A then B then extract C} mis-parsing as
     * {@code if (A then B) then (extract C)}.
     */
    private RExpression buildExprWithThen(RosettaParser.ExprWithThenContext ctx) {
        if (ctx == null) {
            return null;
        }
        RExpression result = visitExpr(ctx.expression());
        for (RosettaParser.ThenSuffixContext suffix : ctx.thenSuffix()) {
            RThenExpr node = new RThenExpr();
            node.setSourceRange(rangeOfTokenSpan(
                    ctx.expression().getStart(), suffix.getStop()));
            node.setArgument(result);
            node.setBody(buildInlineFunctionOrImplicit(
                    suffix.inlineFunction(), suffix.implicitInlineFunction()));
            node.putTokenRange("operator",
                    AstBuilderHelper.rangeOfToken(suffix.THEN().getSymbol(), fileName, charToByte));
            result = node;
        }
        return result;
    }

    // =========================================================================
    // Postfix expression visitors (with-left forms)
    //
    // These visitors all apply a postfix operator to a receiver expression.
    // The operator keyword token is recorded under token range key
    // {@code "operator"} (arrow/deep-arrow/exists/absent) or {@code "keyword"}
    // (list-op, conversion, switch, with-meta, sort/min/max/reduce/filter/
    // extract, one-of, choice).
    //
    // List-op visitors (OnlyElement, Flatten, Distinct, Reverse, First, Last,
    // Sum) delegate to {@link #buildListOpExpr}; the 7 to-X forms (except
    // to-string) delegate to {@link #buildConversionExpr}. Both helpers
    // record the keyword token range internally.
    // =========================================================================

    @Override
    public Object visitFeatureCallExpr(RosettaParser.FeatureCallExprContext ctx) {
        RFeatureCall node = new RFeatureCall();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setReceiver(visitExpr(ctx.expression()));
        node.setFeatureName(ctx.validID().getText());
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(ctx.ARROW().getSymbol(), fileName, charToByte));
        node.putTokenRange("feature",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitDeepFeatureCallExpr(RosettaParser.DeepFeatureCallExprContext ctx) {
        RDeepFeatureCall node = new RDeepFeatureCall();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setReceiver(visitExpr(ctx.expression()));
        node.setFeatureName(ctx.validID().getText());
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(ctx.DEEP_ARROW().getSymbol(), fileName, charToByte));
        node.putTokenRange("feature",
                AstBuilderHelper.rangeOf(ctx.validID(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitExistsExpr(RosettaParser.ExistsExprContext ctx) {
        RExistenceExpr node = new RExistenceExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(visitExpr(ctx.expression()));
        node.setOp(ExistenceOp.EXISTS);
        node.setModifier(extractExistsModifier(ctx.existsModifier()));
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.EXISTS().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitAbsentExpr(RosettaParser.AbsentExprContext ctx) {
        RExistenceExpr node = new RExistenceExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(visitExpr(ctx.expression()));
        node.setOp(ExistenceOp.ABSENT);
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.ABSENT().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitOnlyElementExpr(RosettaParser.OnlyElementExprContext ctx) {
        return buildListOpExpr(ctx, ctx.expression(), ListOp.ONLY_ELEMENT,
                ctx.ONLY_ELEMENT().getSymbol());
    }

    @Override
    public Object visitCountExpr(RosettaParser.CountExprContext ctx) {
        RCountExpr node = new RCountExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(visitExpr(ctx.expression()));
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.COUNT().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitFlattenExpr(RosettaParser.FlattenExprContext ctx) {
        return buildListOpExpr(ctx, ctx.expression(), ListOp.FLATTEN,
                ctx.FLATTEN().getSymbol());
    }

    @Override
    public Object visitDistinctExpr(RosettaParser.DistinctExprContext ctx) {
        return buildListOpExpr(ctx, ctx.expression(), ListOp.DISTINCT,
                ctx.DISTINCT().getSymbol());
    }

    @Override
    public Object visitReverseExpr(RosettaParser.ReverseExprContext ctx) {
        return buildListOpExpr(ctx, ctx.expression(), ListOp.REVERSE,
                ctx.REVERSE().getSymbol());
    }

    @Override
    public Object visitFirstExpr(RosettaParser.FirstExprContext ctx) {
        return buildListOpExpr(ctx, ctx.expression(), ListOp.FIRST,
                ctx.FIRST().getSymbol());
    }

    @Override
    public Object visitLastExpr(RosettaParser.LastExprContext ctx) {
        return buildListOpExpr(ctx, ctx.expression(), ListOp.LAST,
                ctx.LAST().getSymbol());
    }

    @Override
    public Object visitSumExpr(RosettaParser.SumExprContext ctx) {
        return buildListOpExpr(ctx, ctx.expression(), ListOp.SUM,
                ctx.SUM().getSymbol());
    }

    @Override
    public Object visitOneOfExpr(RosettaParser.OneOfExprContext ctx) {
        RCardinalityCheckExpr node = new RCardinalityCheckExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(visitExpr(ctx.expression()));
        node.setOp(CardCheckOp.ONE_OF);
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.ONE_OF().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitChoiceExpr(RosettaParser.ChoiceExprContext ctx) {
        RCardinalityCheckExpr node = new RCardinalityCheckExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(visitExpr(ctx.expression()));
        node.setOp(CardCheckOp.CHOICE);
        node.setNecessity(extractNecessity(ctx.necessity()));
        node.attributes().addAll(collectValidIds(ctx.validID()));
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.CHOICE().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitToStringExpr(RosettaParser.ToStringExprContext ctx) {
        RToStringExpr node = new RToStringExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(visitExpr(ctx.expression()));
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.TO_STRING().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitToNumberExpr(RosettaParser.ToNumberExprContext ctx) {
        return buildConversionExpr(ctx, ctx.expression(), ConversionKind.NUMBER, null,
                ctx.TO_NUMBER().getSymbol());
    }

    @Override
    public Object visitToIntExpr(RosettaParser.ToIntExprContext ctx) {
        return buildConversionExpr(ctx, ctx.expression(), ConversionKind.INT, null,
                ctx.TO_INT().getSymbol());
    }

    @Override
    public Object visitToTimeExpr(RosettaParser.ToTimeExprContext ctx) {
        return buildConversionExpr(ctx, ctx.expression(), ConversionKind.TIME, null,
                ctx.TO_TIME().getSymbol());
    }

    @Override
    public Object visitToEnumExpr(RosettaParser.ToEnumExprContext ctx) {
        return buildConversionExpr(ctx, ctx.expression(), ConversionKind.ENUM,
                ctx.qualifiedName(), ctx.TO_ENUM().getSymbol());
    }

    @Override
    public Object visitToDateExpr(RosettaParser.ToDateExprContext ctx) {
        return buildConversionExpr(ctx, ctx.expression(), ConversionKind.DATE, null,
                ctx.TO_DATE().getSymbol());
    }

    @Override
    public Object visitToDateTimeExpr(RosettaParser.ToDateTimeExprContext ctx) {
        return buildConversionExpr(ctx, ctx.expression(), ConversionKind.DATE_TIME, null,
                ctx.TO_DATE_TIME().getSymbol());
    }

    @Override
    public Object visitToZonedDateTimeExpr(RosettaParser.ToZonedDateTimeExprContext ctx) {
        return buildConversionExpr(ctx, ctx.expression(), ConversionKind.ZONED_DATE_TIME, null,
                ctx.TO_ZONED_DATE_TIME().getSymbol());
    }

    @Override
    public Object visitSwitchExpr(RosettaParser.SwitchExprContext ctx) {
        RSwitchExpr node = new RSwitchExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(visitExpr(ctx.expression()));
        for (RosettaParser.SwitchCaseOrDefaultContext scCtx : ctx.switchCaseOrDefault()) {
            RSwitchCase sc = buildSwitchCase(scCtx);
            node.cases().add(sc);
        }
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.SWITCH().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitWithMetaExpr(RosettaParser.WithMetaExprContext ctx) {
        RWithMetaExpr node = new RWithMetaExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(visitExpr(ctx.expression()));
        for (RosettaParser.WithMetaEntryContext weCtx : ctx.withMetaEntry()) {
            RWithMetaEntry entry = buildWithMetaEntry(weCtx);
            node.entries().add(entry);
        }
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.WITH_META().getSymbol(), fileName, charToByte));
        if (ctx.LBRACE() != null) {
            node.putTokenRange("openBrace",
                    AstBuilderHelper.rangeOfToken(ctx.LBRACE().getSymbol(), fileName, charToByte));
        }
        if (ctx.RBRACE() != null) {
            node.putTokenRange("closeBrace",
                    AstBuilderHelper.rangeOfToken(ctx.RBRACE().getSymbol(), fileName, charToByte));
        }
        return node;
    }

    @Override
    public Object visitSortExpr(RosettaParser.SortExprContext ctx) {
        RSortExpr node = new RSortExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(visitExpr(ctx.expression()));
        if (ctx.inlineFunction() != null) {
            node.setBody(buildInlineFunction(ctx.inlineFunction()));
        }
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.SORT().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitMinExpr(RosettaParser.MinExprContext ctx) {
        RMinExpr node = new RMinExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(visitExpr(ctx.expression()));
        if (ctx.inlineFunction() != null) {
            node.setBody(buildInlineFunction(ctx.inlineFunction()));
        }
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.MIN().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitMaxExpr(RosettaParser.MaxExprContext ctx) {
        RMaxExpr node = new RMaxExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(visitExpr(ctx.expression()));
        if (ctx.inlineFunction() != null) {
            node.setBody(buildInlineFunction(ctx.inlineFunction()));
        }
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.MAX().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitReduceExpr(RosettaParser.ReduceExprContext ctx) {
        RReduceExpr node = new RReduceExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(visitExpr(ctx.expression()));
        node.setBody(buildInlineFunctionOrImplicit(
                ctx.inlineFunction(), ctx.implicitInlineFunction()));
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.REDUCE().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitFilterExpr(RosettaParser.FilterExprContext ctx) {
        RFilterExpr node = new RFilterExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(visitExpr(ctx.expression()));
        node.setBody(buildInlineFunctionOrImplicit(
                ctx.inlineFunction(), ctx.implicitInlineFunction()));
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.FILTER().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitExtractExpr(RosettaParser.ExtractExprContext ctx) {
        RExtractExpr node = new RExtractExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(visitExpr(ctx.expression()));
        node.setBody(buildInlineFunctionOrImplicit(
                ctx.inlineFunction(), ctx.implicitInlineFunction()));
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.EXTRACT().getSymbol(), fileName, charToByte));
        return reassociateExtractTrailingDefaults(node, ctx.implicitInlineFunction());
    }

    /**
     * facet extractDefaultAssociativity (PR #337): upstream's stratified grammar allows AT MOST
     * ONE {@code default} rung inside an implicit inline-function body — {@code
     * ImplicitInlineFunction: body=OrOperation} descends to {@code BinaryOperation}, whose binary
     * rung is {@code (...)?} (one optional, right operand bounded to {@code UnaryOperation}) — so
     * in {@code extract A default B default C} the SECOND {@code default} cannot be consumed by
     * the body and climbs OUT to the enclosing {@code BinaryOperation}: upstream AST =
     * {@code Default( Extract(body=Default(A,B)), C )}. The fork's single left-recursive
     * {@code expression} rule makes {@code default} left-associative REPEATING and the implicit
     * body fully greedy, mis-nesting the whole chain inside the extract —
     * {@code Extract(body=Default(Default(A,B),C))} — which mis-binds C's implicit input to the
     * extract ITEM (upstream: the OUTER scope, e.g. the rule input). This helper restores the
     * upstream shape post-parse: peel every outer rung of the top-level left-nested default CHAIN
     * (depth ≥ 2) off the IMPLICIT body (the innermost single rung stays inside — upstream keeps
     * exactly one) and re-wrap the extract in the peeled rungs, left-associatively outward.
     *
     * <p>The chain is detected on the PARSE TREE ({@code DefaultExprContext} left spine), NOT the
     * built AST: {@code visitParenExpr} is transparent (D9), so a parenthesized inner chain
     * ({@code extract (A default B) default C}) builds the same AST as the bare chain — but
     * upstream KEEPS the parenthesized form fully inside the body (the paren is a primary; the
     * body's one binary rung is {@code <paren> default C}), so the paren must bound the walk. On
     * the parse tree the paren is a {@code ParenExprContext} and stops the spine. The AST peel
     * then walks exactly {@code chainLen - 1} rungs — the AST mirrors the parse tree 1:1 on the
     * unparenthesized spine. (The anchor {@code AstBuilderExtractDefaultAssociativityTest}
     * caught the AST-level walk's paren mis-fire in the full parsersuite — the parse-tree gate
     * is the fix.) Explicit bracket bodies ({@code extract [ … ]}) decline — upstream's
     * bracketed {@code InlineFunction} body is a full expression, no climb-out.
     *
     * <p>Carrier: drr {@code OptionPremiumCurrency} ({@code extract cde.price.OptionPremiumCurrency
     * default common.execution.SettlementCurrencyLeg1 default cde.quantity.NotionalCurrencyLeg1})
     * — the sole chained-default expression in the frozen 9.83.0 corpus (census: every other
     * {@code default…default} window is doc-string text or separate constructor pairs). The
     * then/filter/reduce implicit-body seats share the upstream law but have ZERO corpus carriers
     * — left untouched (no unverified byte movement). Parent links are stamped post-build
     * ({@code AstBuilderHelper.setParents}), so the re-parenting is consistent by construction.
     */
    private RExpression reassociateExtractTrailingDefaults(RExtractExpr node,
            RosettaParser.ImplicitInlineFunctionContext implicit) {
        RInlineFunction fn = node.body();
        if (implicit == null || fn == null || !fn.isImplicit()) {
            return node;
        }
        // Parse-tree gate: the UNPARENTHESIZED top-level default chain length (a
        // ParenExprContext — or any other alternative — stops the left-spine walk).
        int chainLen = 0;
        org.antlr.v4.runtime.tree.ParseTree cur = implicit.expression();
        while (cur instanceof RosettaParser.DefaultExprContext dctx) {
            chainLen++;
            cur = dctx.expression(0);
        }
        if (chainLen < 2) {
            return node;
        }
        // AST peel: walk chainLen-1 outer rungs (the AST mirrors the parse tree 1:1 on the
        // unparenthesized spine); the rung at depth chainLen-1 is the INNERMOST (kept as body).
        java.util.List<RDefaultExpr> peeled = new java.util.ArrayList<>();
        RExpression astCur = fn.body();
        for (int i = 0; i < chainLen - 1; i++) {
            if (!(astCur instanceof RDefaultExpr d)) {
                return node; // defensive — AST/parse-tree skew, keep today's shape
            }
            peeled.add(d);
            astCur = d.rawLeft();
        }
        if (!(astCur instanceof RDefaultExpr innermost)) {
            return node; // defensive
        }
        fn.setBody(innermost);
        // Re-wrap the extract in the peeled rungs, inner→outer (left-associative outward):
        // Extract(body=innermost) default C2 default C3 …
        RExpression acc = node;
        for (int i = peeled.size() - 1; i >= 0; i--) {
            RDefaultExpr rung = peeled.get(i);
            rung.setLeft(acc);
            acc = rung;
        }
        return acc;
    }

    // =========================================================================
    // WithoutLeft expression visitors
    //
    // Grammar alternatives that match a binary/postfix operator without its
    // receiver (e.g. {@code AND expression}, {@code SWITCH ...} alone).
    // Shares node types with the corresponding with-left visitors but leaves
    // {@code left}/{@code argument} null. The one exception is
    // {@link #visitAdditiveWithoutLeftExpr}, which is actually the prefix
    // unary plus/minus form (see its dedicated Javadoc below).
    //
    // Each visitor records the operator or keyword token range under the
    // same key ({@code "operator"} or {@code "keyword"}) as its with-left
    // counterpart, so IDE tooling behaves uniformly across both forms.
    // =========================================================================

    @Override
    public Object visitOrWithoutLeftExpr(RosettaParser.OrWithoutLeftExprContext ctx) {
        RLogicalExpr node = new RLogicalExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setOp(LogOp.OR);
        node.setRight(visitExpr(ctx.expression()));
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(ctx.OR().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitAndWithoutLeftExpr(RosettaParser.AndWithoutLeftExprContext ctx) {
        RLogicalExpr node = new RLogicalExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setOp(LogOp.AND);
        node.setRight(visitExpr(ctx.expression()));
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(ctx.AND().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitEqualityWithoutLeftExpr(RosettaParser.EqualityWithoutLeftExprContext ctx) {
        REqualityExpr node = new REqualityExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setMod(extractCardMod(ctx.cardinalityModifier()));
        node.setOp(ctx.EQ() != null ? EqOp.EQ : EqOp.NEQ);
        node.setRight(visitExpr(ctx.expression()));
        TerminalNode op = ctx.EQ() != null ? ctx.EQ() : ctx.NEQ();
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(op.getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitComparisonWithoutLeftExpr(RosettaParser.ComparisonWithoutLeftExprContext ctx) {
        RComparisonExpr node = new RComparisonExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setMod(extractCardMod(ctx.cardinalityModifier()));
        node.setOp(extractCompOp(ctx.LT(), ctx.GT(), ctx.LTE(), ctx.GTE()));
        node.setRight(visitExpr(ctx.expression()));
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(compOpToken(ctx.LT(), ctx.GT(), ctx.LTE(), ctx.GTE()), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitMultiplicativeWithoutLeftExpr(
            RosettaParser.MultiplicativeWithoutLeftExprContext ctx) {
        RArithmeticExpr node = new RArithmeticExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setOp(ctx.STAR() != null ? ArithOp.MULTIPLY : ArithOp.DIVIDE);
        TerminalNode op = ctx.STAR() != null ? ctx.STAR() : ctx.SLASH();
        // facet omittedParamBinding (PR #437, finding #38): a left-less multiplicative
        // is upstream's OMITTED-PARAMETER form — `a extract [* 2]` means `item * 2`.
        // Upstream admits the left-less alternative for MULTIPLICATIVE only
        // (Rosetta.xtext line ~606) and materialises the default implicit variable as
        // the left operand via the derived-state computer
        // (ArithmeticOperationImpl.needsGeneratedInput() == left-null +
        // setGeneratedInputIfAbsent). Mirror with the established
        // syntheticImplicitInput so typing, codegen and IR see the faithful AST —
        // the pre-#437 left-null node fell into the generator's unary-sign path and
        // CONSTANT-FOLDED `[* 2]` to MapperS.of(-2). The ADDITIVE without-left form
        // (visitAdditiveWithoutLeftExpr below) is genuinely prefix unary sign —
        // upstream has no left-less additive (its lexer owns signed literals) — and
        // stays left-null.
        node.setLeft(syntheticImplicitInput(ctx, op.getSymbol()));
        node.setRight(visitExpr(ctx.expression()));
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(op.getSymbol(), fileName, charToByte));
        return node;
    }

    /**
     * Visits the {@code AdditiveWithoutLeftExpr} grammar alternative
     * ({@code (PLUS | MINUS) expression}).
     *
     * <p>Despite the {@code WithoutLeft} naming, this is actually the
     * <strong>prefix unary plus/minus</strong> operator (e.g., {@code -5}, {@code +x}).
     * The grammar groups it with the without-left forms because both share the
     * structure "operator + expression"; semantically it is unary prefix.
     *
     * <p>The AST representation reuses {@link RArithmeticExpr} with
     * {@code left == null} and the operand stored as {@code right}, consistent
     * with the rest of the without-left mapping. M3's derived state computer
     * (or downstream consumers) can recognise the prefix unary form by the
     * combination of {@code left == null} and the additive operator.
     */
    @Override
    public Object visitAdditiveWithoutLeftExpr(RosettaParser.AdditiveWithoutLeftExprContext ctx) {
        RArithmeticExpr node = new RArithmeticExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setOp(ctx.PLUS() != null ? ArithOp.PLUS : ArithOp.MINUS);
        node.setRight(visitExpr(ctx.expression()));
        TerminalNode op = ctx.PLUS() != null ? ctx.PLUS() : ctx.MINUS();
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(op.getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitContainsWithoutLeftExpr(RosettaParser.ContainsWithoutLeftExprContext ctx) {
        RContainsExpr node = new RContainsExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setRight(visitExpr(ctx.expression()));
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(ctx.CONTAINS().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitDisjointWithoutLeftExpr(RosettaParser.DisjointWithoutLeftExprContext ctx) {
        RDisjointExpr node = new RDisjointExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setRight(visitExpr(ctx.expression()));
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(ctx.DISJOINT().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitDefaultWithoutLeftExpr(RosettaParser.DefaultWithoutLeftExprContext ctx) {
        RDefaultExpr node = new RDefaultExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setRight(visitExpr(ctx.expression()));
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(ctx.DEFAULT().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitJoinWithoutLeftExpr(RosettaParser.JoinWithoutLeftExprContext ctx) {
        RJoinExpr node = new RJoinExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        if (ctx.expression() != null) {
            node.setSeparator(visitExpr(ctx.expression()));
        }
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(ctx.JOIN().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitExistsWithoutLeftExpr(RosettaParser.ExistsWithoutLeftExprContext ctx) {
        RExistenceExpr node = new RExistenceExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setOp(ExistenceOp.EXISTS);
        node.setModifier(extractExistsModifier(ctx.existsModifier()));
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.EXISTS().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitAbsentWithoutLeftExpr(RosettaParser.AbsentWithoutLeftExprContext ctx) {
        RExistenceExpr node = new RExistenceExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setOp(ExistenceOp.ABSENT);
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.ABSENT().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitOnlyElementWithoutLeftExpr(
            RosettaParser.OnlyElementWithoutLeftExprContext ctx) {
        return buildListOpExpr(ctx, null, ListOp.ONLY_ELEMENT,
                ctx.ONLY_ELEMENT().getSymbol());
    }

    @Override
    public Object visitCountWithoutLeftExpr(RosettaParser.CountWithoutLeftExprContext ctx) {
        RCountExpr node = new RCountExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(syntheticImplicitInput(ctx, ctx.COUNT().getSymbol()));
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.COUNT().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitFlattenWithoutLeftExpr(RosettaParser.FlattenWithoutLeftExprContext ctx) {
        return buildListOpExpr(ctx, null, ListOp.FLATTEN,
                ctx.FLATTEN().getSymbol());
    }

    @Override
    public Object visitDistinctWithoutLeftExpr(RosettaParser.DistinctWithoutLeftExprContext ctx) {
        return buildListOpExpr(ctx, null, ListOp.DISTINCT,
                ctx.DISTINCT().getSymbol());
    }

    @Override
    public Object visitReverseWithoutLeftExpr(RosettaParser.ReverseWithoutLeftExprContext ctx) {
        return buildListOpExpr(ctx, null, ListOp.REVERSE,
                ctx.REVERSE().getSymbol());
    }

    @Override
    public Object visitFirstWithoutLeftExpr(RosettaParser.FirstWithoutLeftExprContext ctx) {
        return buildListOpExpr(ctx, null, ListOp.FIRST,
                ctx.FIRST().getSymbol());
    }

    @Override
    public Object visitLastWithoutLeftExpr(RosettaParser.LastWithoutLeftExprContext ctx) {
        return buildListOpExpr(ctx, null, ListOp.LAST,
                ctx.LAST().getSymbol());
    }

    @Override
    public Object visitSumWithoutLeftExpr(RosettaParser.SumWithoutLeftExprContext ctx) {
        return buildListOpExpr(ctx, null, ListOp.SUM,
                ctx.SUM().getSymbol());
    }

    @Override
    public Object visitOneOfWithoutLeftExpr(RosettaParser.OneOfWithoutLeftExprContext ctx) {
        RCardinalityCheckExpr node = new RCardinalityCheckExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setOp(CardCheckOp.ONE_OF);
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.ONE_OF().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitChoiceWithoutLeftExpr(RosettaParser.ChoiceWithoutLeftExprContext ctx) {
        RCardinalityCheckExpr node = new RCardinalityCheckExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setOp(CardCheckOp.CHOICE);
        node.setNecessity(extractNecessity(ctx.necessity()));
        node.attributes().addAll(collectValidIds(ctx.validID()));
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.CHOICE().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitToStringWithoutLeftExpr(RosettaParser.ToStringWithoutLeftExprContext ctx) {
        RToStringExpr node = new RToStringExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(syntheticImplicitInput(ctx, ctx.TO_STRING().getSymbol()));
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.TO_STRING().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitToNumberWithoutLeftExpr(RosettaParser.ToNumberWithoutLeftExprContext ctx) {
        return buildConversionExpr(ctx, null, ConversionKind.NUMBER, null,
                ctx.TO_NUMBER().getSymbol());
    }

    @Override
    public Object visitToIntWithoutLeftExpr(RosettaParser.ToIntWithoutLeftExprContext ctx) {
        return buildConversionExpr(ctx, null, ConversionKind.INT, null,
                ctx.TO_INT().getSymbol());
    }

    @Override
    public Object visitToTimeWithoutLeftExpr(RosettaParser.ToTimeWithoutLeftExprContext ctx) {
        return buildConversionExpr(ctx, null, ConversionKind.TIME, null,
                ctx.TO_TIME().getSymbol());
    }

    @Override
    public Object visitToEnumWithoutLeftExpr(RosettaParser.ToEnumWithoutLeftExprContext ctx) {
        return buildConversionExpr(ctx, null, ConversionKind.ENUM, ctx.qualifiedName(),
                ctx.TO_ENUM().getSymbol());
    }

    @Override
    public Object visitToDateWithoutLeftExpr(RosettaParser.ToDateWithoutLeftExprContext ctx) {
        return buildConversionExpr(ctx, null, ConversionKind.DATE, null,
                ctx.TO_DATE().getSymbol());
    }

    @Override
    public Object visitToDateTimeWithoutLeftExpr(
            RosettaParser.ToDateTimeWithoutLeftExprContext ctx) {
        return buildConversionExpr(ctx, null, ConversionKind.DATE_TIME, null,
                ctx.TO_DATE_TIME().getSymbol());
    }

    @Override
    public Object visitToZonedDateTimeWithoutLeftExpr(
            RosettaParser.ToZonedDateTimeWithoutLeftExprContext ctx) {
        return buildConversionExpr(ctx, null, ConversionKind.ZONED_DATE_TIME, null,
                ctx.TO_ZONED_DATE_TIME().getSymbol());
    }

    @Override
    public Object visitSwitchWithoutLeftExpr(RosettaParser.SwitchWithoutLeftExprContext ctx) {
        RSwitchExpr node = new RSwitchExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        for (RosettaParser.SwitchCaseOrDefaultContext scCtx : ctx.switchCaseOrDefault()) {
            RSwitchCase sc = buildSwitchCase(scCtx);
            node.cases().add(sc);
        }
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.SWITCH().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitWithMetaWithoutLeftExpr(RosettaParser.WithMetaWithoutLeftExprContext ctx) {
        RWithMetaExpr node = new RWithMetaExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        for (RosettaParser.WithMetaEntryContext weCtx : ctx.withMetaEntry()) {
            RWithMetaEntry entry = buildWithMetaEntry(weCtx);
            node.entries().add(entry);
        }
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.WITH_META().getSymbol(), fileName, charToByte));
        if (ctx.LBRACE() != null) {
            node.putTokenRange("openBrace",
                    AstBuilderHelper.rangeOfToken(ctx.LBRACE().getSymbol(), fileName, charToByte));
        }
        if (ctx.RBRACE() != null) {
            node.putTokenRange("closeBrace",
                    AstBuilderHelper.rangeOfToken(ctx.RBRACE().getSymbol(), fileName, charToByte));
        }
        return node;
    }

    @Override
    public Object visitSortWithoutLeftExpr(RosettaParser.SortWithoutLeftExprContext ctx) {
        RSortExpr node = new RSortExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(syntheticImplicitInput(ctx, ctx.SORT().getSymbol()));
        if (ctx.inlineFunction() != null) {
            node.setBody(buildInlineFunction(ctx.inlineFunction()));
        }
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.SORT().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitMinWithoutLeftExpr(RosettaParser.MinWithoutLeftExprContext ctx) {
        RMinExpr node = new RMinExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(syntheticImplicitInput(ctx, ctx.MIN().getSymbol()));
        if (ctx.inlineFunction() != null) {
            node.setBody(buildInlineFunction(ctx.inlineFunction()));
        }
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.MIN().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitMaxWithoutLeftExpr(RosettaParser.MaxWithoutLeftExprContext ctx) {
        RMaxExpr node = new RMaxExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(syntheticImplicitInput(ctx, ctx.MAX().getSymbol()));
        if (ctx.inlineFunction() != null) {
            node.setBody(buildInlineFunction(ctx.inlineFunction()));
        }
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.MAX().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitReduceWithoutLeftExpr(RosettaParser.ReduceWithoutLeftExprContext ctx) {
        RReduceExpr node = new RReduceExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(syntheticImplicitInput(ctx, ctx.REDUCE().getSymbol()));
        node.setBody(buildInlineFunctionOrImplicit(
                ctx.inlineFunction(), ctx.implicitInlineFunction()));
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.REDUCE().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitFilterWithoutLeftExpr(RosettaParser.FilterWithoutLeftExprContext ctx) {
        RFilterExpr node = new RFilterExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(syntheticImplicitInput(ctx, ctx.FILTER().getSymbol()));
        node.setBody(buildInlineFunctionOrImplicit(
                ctx.inlineFunction(), ctx.implicitInlineFunction()));
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.FILTER().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitExtractWithoutLeftExpr(RosettaParser.ExtractWithoutLeftExprContext ctx) {
        RExtractExpr node = new RExtractExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setArgument(syntheticImplicitInput(ctx, ctx.EXTRACT().getSymbol()));
        node.setBody(buildInlineFunctionOrImplicit(
                ctx.inlineFunction(), ctx.implicitInlineFunction()));
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.EXTRACT().getSymbol(), fileName, charToByte));
        // facet extractDefaultAssociativity (PR #337): the without-left (rule-chain) extract is
        // the carrier seat — see reassociateExtractTrailingDefaults.
        return reassociateExtractTrailingDefaults(node, ctx.implicitInlineFunction());
    }

    // =========================================================================
    // Primary expression visitors
    //
    // Leaf/root forms: if-then-else, constructors, only-exists, function and
    // symbol references, super, item, empty, list literals, literals, and the
    // transparent parenthesised form.
    //
    // Primary visitors track structural keyword/delimiter tokens where they
    // aid IDE tooling:
    //   - ConditionalExpr:   IF/THEN/ELSE as keywords
    //   - FunctionCallExpr:  LPAREN/RPAREN as openParen/closeParen
    //   - ListLiteralExpr:   LBRACK/RBRACK as openBrack/closeBrack
    //   - ConstructorExpression: LBRACE/RBRACE as openBrace/closeBrace
    //   - SuperCallExpr / ImplicitVarExpr / EmptyExpr: single-keyword forms
    //     where the keyword token range equals the source range, so no
    //     additional token range entry is added (redundant with source range).
    //
    // {@link #visitParenExpr} is transparent per design decision D9 and
    // returns the inner expression directly — see its Javadoc.
    // =========================================================================

    @Override
    public Object visitConditionalExpr(RosettaParser.ConditionalExprContext ctx) {
        RConditionalExpr node = new RConditionalExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setCondition(visitExpr(ctx.expression(0)));
        node.setThenBranch(visitExpr(ctx.expression(1)));
        if (ctx.expression().size() > 2) {
            node.setElseBranch(visitExpr(ctx.expression(2)));
        }
        node.putTokenRange("if",
                AstBuilderHelper.rangeOfToken(ctx.IF().getSymbol(), fileName, charToByte));
        node.putTokenRange("then",
                AstBuilderHelper.rangeOfToken(ctx.THEN().getSymbol(), fileName, charToByte));
        if (ctx.ELSE() != null) {
            node.putTokenRange("else",
                    AstBuilderHelper.rangeOfToken(ctx.ELSE().getSymbol(), fileName, charToByte));
        }
        return node;
    }

    @Override
    public Object visitConstructorExpr(RosettaParser.ConstructorExprContext ctx) {
        return visitConstructorExpression(ctx.constructorExpression());
    }

    @Override
    public Object visitConstructorExpression(RosettaParser.ConstructorExpressionContext ctx) {
        RConstructorExpr node = new RConstructorExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));

        // typeCall
        RTypeCall tc = (RTypeCall) visitTypeCall(ctx.typeCall());
        node.setTypeCall(tc);

        // constructorKeyValuePair*
        for (RosettaParser.ConstructorKeyValuePairContext kvpCtx
                : ctx.constructorKeyValuePair()) {
            RKeyValuePair kvp = buildKeyValuePair(kvpCtx);
            node.pairs().add(kvp);
        }

        // spread: DOT_DOT DOT present?
        // The grammar has DOT_DOT() list — if any DOT_DOT tokens are present, spread is true
        node.setSpread(!ctx.DOT_DOT().isEmpty());

        if (ctx.LBRACE() != null) {
            node.putTokenRange("openBrace",
                    AstBuilderHelper.rangeOfToken(ctx.LBRACE().getSymbol(), fileName, charToByte));
        }
        if (ctx.RBRACE() != null) {
            node.putTokenRange("closeBrace",
                    AstBuilderHelper.rangeOfToken(ctx.RBRACE().getSymbol(), fileName, charToByte));
        }

        return node;
    }

    @Override
    public Object visitOnlyExistsExpr(RosettaParser.OnlyExistsExprContext ctx) {
        return visitOnlyExistsExpression(ctx.onlyExistsExpression());
    }

    @Override
    public Object visitOnlyExistsExpression(RosettaParser.OnlyExistsExpressionContext ctx) {
        ROnlyExistsExpr node = new ROnlyExistsExpr();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        for (RosettaParser.OnlyExistsElementContext oeCtx : ctx.onlyExistsElement()) {
            ROnlyExistsElement element = buildOnlyExistsElement(oeCtx);
            node.elements().add(element);
        }
        if (ctx.ONLY() != null) {
            node.putTokenRange("only",
                    AstBuilderHelper.rangeOfToken(ctx.ONLY().getSymbol(), fileName, charToByte));
        }
        if (ctx.EXISTS() != null) {
            node.putTokenRange("exists",
                    AstBuilderHelper.rangeOfToken(ctx.EXISTS().getSymbol(), fileName, charToByte));
        }
        return node;
    }

    @Override
    public Object visitFunctionCallExpr(RosettaParser.FunctionCallExprContext ctx) {
        RSymbolReference node = new RSymbolReference();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setName(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
        for (RosettaParser.ExprWithThenContext exprCtx : ctx.exprWithThen()) {
            RExpression arg = buildExprWithThen(exprCtx);
            if (arg != null) {
                node.args().add(arg);
            }
        }
        if (ctx.LPAREN() != null) {
            node.putTokenRange("openParen",
                    AstBuilderHelper.rangeOfToken(ctx.LPAREN().getSymbol(), fileName, charToByte));
        }
        if (ctx.RPAREN() != null) {
            node.putTokenRange("closeParen",
                    AstBuilderHelper.rangeOfToken(ctx.RPAREN().getSymbol(), fileName, charToByte));
        }
        return node;
    }

    @Override
    public Object visitEnumValueRefExpr(RosettaParser.EnumValueRefExprContext ctx) {
        REnumValueRef node = new REnumValueRef();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setEnumName(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
        node.setValueName(ctx.validID().getText());
        node.putTokenRange("operator",
                AstBuilderHelper.rangeOfToken(ctx.ARROW().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitSymbolRefExpr(RosettaParser.SymbolRefExprContext ctx) {
        RSymbolReference node = new RSymbolReference();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.setName(AstBuilderHelper.textOfQualifiedName(ctx.qualifiedName()));
        return node;
    }

    @Override
    public Object visitSuperCallExpr(RosettaParser.SuperCallExprContext ctx) {
        RSuperCall node = new RSuperCall();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.SUPER().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitImplicitVarExpr(RosettaParser.ImplicitVarExprContext ctx) {
        RImplicitVariable node = new RImplicitVariable();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.ITEM().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitEmptyExpr(RosettaParser.EmptyExprContext ctx) {
        REmptyLiteral node = new REmptyLiteral();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        node.putTokenRange("keyword",
                AstBuilderHelper.rangeOfToken(ctx.EMPTY().getSymbol(), fileName, charToByte));
        return node;
    }

    @Override
    public Object visitListLiteralExpr(RosettaParser.ListLiteralExprContext ctx) {
        RListLiteral node = new RListLiteral();
        node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
        for (RosettaParser.ExprWithThenContext exprCtx : ctx.exprWithThen()) {
            RExpression elem = buildExprWithThen(exprCtx);
            if (elem != null) {
                node.elements().add(elem);
            }
        }
        if (ctx.LBRACK() != null) {
            node.putTokenRange("openBrack",
                    AstBuilderHelper.rangeOfToken(ctx.LBRACK().getSymbol(), fileName, charToByte));
        }
        if (ctx.RBRACK() != null) {
            node.putTokenRange("closeBrack",
                    AstBuilderHelper.rangeOfToken(ctx.RBRACK().getSymbol(), fileName, charToByte));
        }
        return node;
    }

    @Override
    public Object visitLiteralExpr(RosettaParser.LiteralExprContext ctx) {
        return visitLiteral(ctx.literal());
    }

    /**
     * Visits the tight negative-literal alternative ({@code MINUS literal} —
     * upstream Xtext's {@code MINUS? literal}): the sign folds INTO the literal
     * node's value, so {@code -1} is an {@link RIntLiteral} of −1 (and
     * {@code -1.5} an {@link RNumberLiteral} of −1.5) exactly as upstream
     * parses it. Non-numeric literals cannot take a sign (grammar-permitted
     * through the shared {@code literal} rule but semantically impossible —
     * fail loud).
     */
    @Override
    public Object visitNegativeLiteralExpr(RosettaParser.NegativeLiteralExprContext ctx) {
        Object literal = visitLiteral(ctx.literal());
        if (literal instanceof RIntLiteral intLit) {
            intLit.setValue(intLit.value().negate());
            intLit.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
            return intLit;
        }
        if (literal instanceof RNumberLiteral numLit) {
            numLit.setValue(numLit.value().negate());
            numLit.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
            return numLit;
        }
        throw new AstBuildException(
                "Negative sign on a non-numeric literal: " + ctx.getText());
    }

    /**
     * Visits the {@code literal} rule and returns one of the five literal
     * node types ({@link RStringLiteral}, {@link RIntLiteral},
     * {@link RNumberLiteral}, or {@link RBooleanLiteral}). Throws
     * {@link AstBuildException} if no alternative matched (grammar-impossible).
     *
     * <p>Single-token literals do not store a separate token range — the
     * source range already covers the single token.
     *
     * <p>Grammar:
     * <pre>
     * literal:
     *     STRING | INT_LITERAL | BIG_DECIMAL | TRUE | FALSE
     * ;
     * </pre>
     */
    @Override
    public Object visitLiteral(RosettaParser.LiteralContext ctx) {
        if (ctx.STRING() != null) {
            RStringLiteral node = new RStringLiteral();
            node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
            node.setValue(AstBuilderHelper.stripQuotes(ctx.STRING().getText()));
            return node;
        }
        if (ctx.INT_LITERAL() != null) {
            RIntLiteral node = new RIntLiteral();
            node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
            // Use BigInteger to handle arbitrary-precision integer literals.
            // Real-world Rune DSL files contain literals exceeding long range
            // (e.g., 9999999999999999999999999) — this surfaced during the
            // Task 15 corpus regression test.
            node.setValue(new BigInteger(ctx.INT_LITERAL().getText()));
            return node;
        }
        if (ctx.BIG_DECIMAL() != null) {
            RNumberLiteral node = new RNumberLiteral();
            node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
            node.setValue(new BigDecimal(ctx.BIG_DECIMAL().getText()));
            return node;
        }
        if (ctx.TRUE() != null) {
            RBooleanLiteral node = new RBooleanLiteral();
            node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
            node.setValue(true);
            return node;
        }
        if (ctx.FALSE() != null) {
            RBooleanLiteral node = new RBooleanLiteral();
            node.setSourceRange(AstBuilderHelper.rangeOf(ctx, fileName, charToByte));
            node.setValue(false);
            return node;
        }
        throw new AstBuildException("literal matched no grammar alternative: " + ctx.getText());
    }

    /**
     * Visits a parenthesised expression. Per design decision D9, parentheses
     * are <strong>transparent</strong>: this returns the inner expression
     * directly with no wrapper node, so {@code (a + b)} produces the same AST
     * as {@code a + b}.
     *
     * <p><strong>Source range note:</strong> the inner expression's source
     * range covers only its own tokens, not the enclosing parentheses.
     * Consumers needing paren-aware ranges (e.g., for source-level rewriting
     * tools) must handle this loss of information explicitly.
     */
    @Override
    public Object visitParenExpr(RosettaParser.ParenExprContext ctx) {
        return buildExprWithThen(ctx.exprWithThen());
    }

}
