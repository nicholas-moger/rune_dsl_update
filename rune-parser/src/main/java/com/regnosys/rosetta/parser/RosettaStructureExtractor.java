package com.regnosys.rosetta.parser;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import java.util.*;

/**
 * Extracts structural information from a parsed .rosetta file.
 * Used for structural comparison against Xtext parser output.
 *
 * Walks the ANTLR4 parse tree using the listener pattern and collects
 * top-level model elements: types, enums, functions, rules, reports,
 * choices, annotations, type aliases, basic types, record types,
 * library functions, meta types, bodies, corpora, segments,
 * synonym sources, external synonym sources, and external rule sources.
 */
public class RosettaStructureExtractor extends RosettaParserBaseListener {

    public record FileStructure(
        String namespace,
        List<ElementInfo> elements
    ) {}

    public record ElementInfo(
        String kind,           // "type", "enum", "func", "rule", "report", "choice",
                               // "annotation", "typeAlias", "basicType", "recordType",
                               // "libraryFunction", "metaType", "body", "corpus",
                               // "segment", "synonymSource", "externalSynonymSource",
                               // "externalRuleSource"
        String name,
        int attributeCount,    // for types: number of attributes; for enums: number of values;
                               // for recordTypes: number of features; for choices: number of options
        String superType       // for types/enums/functions with extends: the supertype name
    ) {}

    private String namespace;
    private final List<ElementInfo> elements = new ArrayList<>();

    // Transient state for tracking the current element being built
    private String currentKind;
    private String currentName;
    private int currentAttributeCount;
    private String currentSuperType;

    // ========================================================================
    // Namespace extraction
    // ========================================================================

    @Override
    public void enterRosettaModel(RosettaParser.RosettaModelContext ctx) {
        if (ctx.qualifiedName() != null) {
            namespace = ctx.qualifiedName().getText();
        } else if (ctx.STRING() != null) {
            // Namespace given as a string literal — strip surrounding quotes
            String raw = ctx.STRING().getText();
            namespace = raw.substring(1, raw.length() - 1);
        }
    }

    // ========================================================================
    // Data type
    // ========================================================================

    @Override
    public void enterDataType(RosettaParser.DataTypeContext ctx) {
        currentKind = "type";
        currentName = extractValidID(ctx.validID());
        currentAttributeCount = ctx.attribute() != null ? ctx.attribute().size() : 0;
        currentSuperType = ctx.qualifiedName() != null ? ctx.qualifiedName().getText() : null;
    }

    @Override
    public void exitDataType(RosettaParser.DataTypeContext ctx) {
        elements.add(new ElementInfo(currentKind, currentName, currentAttributeCount, currentSuperType));
        resetCurrent();
    }

    // ========================================================================
    // Enumeration
    // ========================================================================

    @Override
    public void enterEnumeration(RosettaParser.EnumerationContext ctx) {
        currentKind = "enum";
        currentName = extractValidID(ctx.validID());
        currentAttributeCount = ctx.rosettaEnumValue() != null ? ctx.rosettaEnumValue().size() : 0;
        currentSuperType = ctx.qualifiedName() != null ? ctx.qualifiedName().getText() : null;
    }

    @Override
    public void exitEnumeration(RosettaParser.EnumerationContext ctx) {
        elements.add(new ElementInfo(currentKind, currentName, currentAttributeCount, currentSuperType));
        resetCurrent();
    }

    // ========================================================================
    // Function
    // ========================================================================

    @Override
    public void enterFunction(RosettaParser.FunctionContext ctx) {
        currentKind = "func";
        // validID(0) is the function name; validID(1) would be the dispatch parameter if present
        currentName = extractValidID(ctx.validID(0));
        // Count input attributes + output attribute
        int attrCount = 0;
        if (ctx.attribute() != null) {
            attrCount = ctx.attribute().size();
        }
        currentAttributeCount = attrCount;
        currentSuperType = ctx.qualifiedName() != null ? ctx.qualifiedName().getText() : null;
    }

    @Override
    public void exitFunction(RosettaParser.FunctionContext ctx) {
        elements.add(new ElementInfo(currentKind, currentName, currentAttributeCount, currentSuperType));
        resetCurrent();
    }

    // ========================================================================
    // Choice
    // ========================================================================

    @Override
    public void enterChoice(RosettaParser.ChoiceContext ctx) {
        currentKind = "choice";
        currentName = extractValidID(ctx.validID());
        currentAttributeCount = ctx.choiceOption() != null ? ctx.choiceOption().size() : 0;
        currentSuperType = null;
    }

    @Override
    public void exitChoice(RosettaParser.ChoiceContext ctx) {
        elements.add(new ElementInfo(currentKind, currentName, currentAttributeCount, currentSuperType));
        resetCurrent();
    }

    // ========================================================================
    // Rosetta Rule (reporting rule / eligibility rule)
    // ========================================================================

    @Override
    public void enterRosettaRule(RosettaParser.RosettaRuleContext ctx) {
        currentKind = "rule";
        currentName = extractValidID(ctx.validID());
        currentAttributeCount = 0;
        currentSuperType = null;
    }

    @Override
    public void exitRosettaRule(RosettaParser.RosettaRuleContext ctx) {
        elements.add(new ElementInfo(currentKind, currentName, currentAttributeCount, currentSuperType));
        resetCurrent();
    }

    // ========================================================================
    // Rosetta Report
    // ========================================================================

    @Override
    public void enterRosettaReport(RosettaParser.RosettaReportContext ctx) {
        // Reports don't have a simple validID name. We use the "with type" qualified name
        // as a proxy identifier, since it's the most distinguishing piece.
        String reportTypeName = null;
        // The last qualifiedName in the rule is the "with type" QualifiedName.
        // Per the grammar:  ... WITH TYPE qualifiedName (WITH SOURCE qualifiedName)?
        // We look for qualifiedName entries. The first is from regulatoryDocumentReference,
        // and the WHEN clause; the WITH TYPE one is a specific qualifiedName.
        // Since reports don't have a validID name, we construct one from the body+corpus+type.
        List<RosettaParser.QualifiedNameContext> qnames = ctx.qualifiedName();
        if (qnames != null && !qnames.isEmpty()) {
            // The last (or second to last if WITH SOURCE is present) qualifiedName is the type
            // Actually, the grammar structure is:
            //   regulatoryDocumentReference (has qualifiedNames inside it, not directly on ctx)
            //   WHEN qualifiedName (AND qualifiedName)*
            //   WITH TYPE qualifiedName
            //   (WITH SOURCE qualifiedName)?
            // The qualifiedName() list on ctx only includes the WHEN and WITH TYPE/SOURCE ones
            // The WITH TYPE one is the second to last (or last if no WITH SOURCE)
            if (ctx.SOURCE() != null && qnames.size() >= 2) {
                reportTypeName = qnames.get(qnames.size() - 2).getText();
            } else if (!qnames.isEmpty()) {
                reportTypeName = qnames.get(qnames.size() - 1).getText();
            }
        }
        elements.add(new ElementInfo("report", reportTypeName, 0, null));
    }

    // ========================================================================
    // Annotation declaration
    // ========================================================================

    @Override
    public void enterAnnotationDecl(RosettaParser.AnnotationDeclContext ctx) {
        currentKind = "annotation";
        // validID(0) is the annotation name
        currentName = extractValidID(ctx.validID(0));
        currentAttributeCount = ctx.attribute() != null ? ctx.attribute().size() : 0;
        currentSuperType = null;
    }

    @Override
    public void exitAnnotationDecl(RosettaParser.AnnotationDeclContext ctx) {
        elements.add(new ElementInfo(currentKind, currentName, currentAttributeCount, currentSuperType));
        resetCurrent();
    }

    // ========================================================================
    // Type alias
    // ========================================================================

    @Override
    public void enterRosettaTypeAlias(RosettaParser.RosettaTypeAliasContext ctx) {
        currentKind = "typeAlias";
        currentName = extractValidID(ctx.validID());
        currentAttributeCount = 0;
        currentSuperType = null;
    }

    @Override
    public void exitRosettaTypeAlias(RosettaParser.RosettaTypeAliasContext ctx) {
        elements.add(new ElementInfo(currentKind, currentName, currentAttributeCount, currentSuperType));
        resetCurrent();
    }

    // ========================================================================
    // Basic type
    // ========================================================================

    @Override
    public void enterRosettaBasicType(RosettaParser.RosettaBasicTypeContext ctx) {
        elements.add(new ElementInfo("basicType",
            extractValidID(ctx.validID()), 0, null));
    }

    // ========================================================================
    // Record type
    // ========================================================================

    @Override
    public void enterRosettaRecordType(RosettaParser.RosettaRecordTypeContext ctx) {
        int featureCount = ctx.rosettaRecordFeature() != null ? ctx.rosettaRecordFeature().size() : 0;
        elements.add(new ElementInfo("recordType",
            extractValidID(ctx.validID()), featureCount, null));
    }

    // ========================================================================
    // Library function
    // ========================================================================

    @Override
    public void enterRosettaLibraryFunction(RosettaParser.RosettaLibraryFunctionContext ctx) {
        int paramCount = ctx.rosettaParameter() != null ? ctx.rosettaParameter().size() : 0;
        elements.add(new ElementInfo("libraryFunction",
            extractValidID(ctx.validID()), paramCount, null));
    }

    // ========================================================================
    // Meta type
    // ========================================================================

    @Override
    public void enterRosettaMetaType(RosettaParser.RosettaMetaTypeContext ctx) {
        elements.add(new ElementInfo("metaType",
            extractValidID(ctx.validID()), 0, null));
    }

    // ========================================================================
    // Body
    // ========================================================================

    @Override
    public void enterRosettaBody(RosettaParser.RosettaBodyContext ctx) {
        elements.add(new ElementInfo("body",
            extractValidID(ctx.validID()), 0, null));
    }

    // ========================================================================
    // Corpus
    // ========================================================================

    @Override
    public void enterRosettaCorpus(RosettaParser.RosettaCorpusContext ctx) {
        elements.add(new ElementInfo("corpus",
            extractValidID(ctx.validID()), 0, null));
    }

    // ========================================================================
    // Segment
    // ========================================================================

    @Override
    public void enterRosettaSegment(RosettaParser.RosettaSegmentContext ctx) {
        String name;
        if (ctx.validID() != null) {
            name = extractValidID(ctx.validID());
        } else if (ctx.RATIONALE() != null) {
            name = "rationale";
        } else if (ctx.RATIONALE_AUTHOR() != null) {
            name = "rationale_author";
        } else if (ctx.STRUCTURED_PROVISION() != null) {
            name = "structured_provision";
        } else {
            name = "<unknown>";
        }
        elements.add(new ElementInfo("segment", name, 0, null));
    }

    // ========================================================================
    // Synonym source (simple declaration)
    // ========================================================================

    @Override
    public void enterRosettaSynonymSource(RosettaParser.RosettaSynonymSourceContext ctx) {
        elements.add(new ElementInfo("synonymSource",
            extractValidID(ctx.validID()), 0, null));
    }

    // ========================================================================
    // External synonym source
    // ========================================================================

    @Override
    public void enterRosettaExternalSynonymSource(RosettaParser.RosettaExternalSynonymSourceContext ctx) {
        elements.add(new ElementInfo("externalSynonymSource",
            extractValidID(ctx.validID()), 0, null));
    }

    // ========================================================================
    // External rule source
    // ========================================================================

    @Override
    public void enterRosettaExternalRuleSource(RosettaParser.RosettaExternalRuleSourceContext ctx) {
        elements.add(new ElementInfo("externalRuleSource",
            extractValidID(ctx.validID()), 0, null));
    }

    // ========================================================================
    // Public API
    // ========================================================================

    /**
     * Walk the given parse tree and return the extracted file structure.
     */
    public FileStructure extract(RosettaParser.RosettaModelContext tree) {
        ParseTreeWalker.DEFAULT.walk(this, tree);
        return new FileStructure(namespace, List.copyOf(elements));
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private void resetCurrent() {
        currentKind = null;
        currentName = null;
        currentAttributeCount = 0;
        currentSuperType = null;
    }

    /**
     * Extract the text from a ValidIDContext. This handles the soft-keyword pattern
     * where validID can be ID | CONDITION | SOURCE | VALUE | VERSION | PATTERN | SCOPE.
     */
    private static String extractValidID(RosettaParser.ValidIDContext ctx) {
        if (ctx == null) return null;
        return ctx.getText();
    }
}
