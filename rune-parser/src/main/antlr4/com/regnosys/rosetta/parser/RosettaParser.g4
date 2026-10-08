parser grammar RosettaParser;

options { tokenVocab = RosettaLexer; }

// ============================================================================
// Model structure
// ============================================================================

rosettaModel
    : fileHeader?
      OVERRIDE? NAMESPACE (qualifiedName | STRING) (COLON definable?)?
      rosettaScope?
      versionDecl?
      importDecl*
      rosettaQualifiableConfiguration*
      rootElement*
      EOF
    ;

qualifiedName
    : validID (DOT validID)*
    ;

qualifiedNameWithWildcard
    : qualifiedName (DOT STAR)?
    ;

importDecl
    : IMPORT qualifiedNameWithWildcard (AS validID)?
    ;

rosettaScope
    : SCOPE validID definable?
    ;

versionDecl
    : VERSION STRING
    ;

// ============================================================================
// File header (P1.4.2 H3) — optional file-level metadata block at the top
// of a Rune source file. Distinct from versionDecl (namespace-level version).
// Layer-1 only: parsed and stored on RModel.fileHeader, no runtime semantics.
// ============================================================================

fileHeader
    : FILE_HEADER COLON
      versionField?
      dependsOnField?
      experimentalField?
    ;

versionField
    : VERSION STRING
    ;

dependsOnField
    : DEPENDS_ON STRING (COMMA STRING)*
    ;

experimentalField
    : EXPERIMENTAL COLON LBRACK validID (COMMA validID)* RBRACK
    ;

// ============================================================================
// Identifiers (soft-keyword pattern)
// ============================================================================

validID
    : ID
    | CONDITION
    | SOURCE
    | VALUE
    | VERSION
    | PATTERN
    | SCOPE
    | FILE_HEADER          // P1.4.2 H3 — soft-keyword
    | EXPERIMENTAL         // P1.4.2 H3 — soft-keyword
    // Note: DEPENDS_ON ('depends-on') is intentionally NOT admitted here.
    // The hyphen makes it impossible for any existing identifier (validID
    // requires [a-zA-Z][a-zA-Z0-9_]*) to collide; admitting it would expose
    // an unusable token surface inside non-fileHeader contexts.
    ;

typeParameterValidID
    : validID
    | MIN
    | MAX
    ;

// ============================================================================
// Common fragments (regular rules in ANTLR4)
// ============================================================================

definable
    : LT STRING GT
    ;

annotations
    : annotationRef
    ;

// ============================================================================
// Annotations
// ============================================================================

annotationDecl
    : ANNOTATION validID COLON definable?
      (LBRACK PREFIX validID RBRACK)?
      attribute*
    ;

annotationRef
    : LBRACK validID (validID annotationQualifier*)? RBRACK
    ;

annotationQualifier
    : STRING EQ (STRING | rosettaAttributeReference)
    ;

// ============================================================================
// Data model
// ============================================================================

dataType
    : runeAnnotations?
      TYPE validID (EXTENDS qualifiedName)? COLON definable?
      (docReference | annotationRef | classSynonym)*
      attribute*
      condition*
    ;

choice
    : runeAnnotations?
      CHOICE validID COLON definable?
      (annotationRef | classSynonym)*
      choiceOption*
    ;

choiceOption
    : typeCall definable?
      (docReference | annotationRef | rosettaSynonym | labelAnnotation | ruleReferenceAnnotation)*
    ;

attribute
    : OVERRIDE? validID typeCall rosettaCardinality? definable?
      (docReference | annotationRef | rosettaSynonym | labelAnnotation | ruleReferenceAnnotation)*
    ;

// ============================================================================
// Enums
// ============================================================================

enumeration
    : runeAnnotations?
      ENUM validID (EXTENDS qualifiedName)? COLON definable?
      (docReference | annotationRef | rosettaSynonym)*
      rosettaEnumValue*
    ;

rosettaEnumValue
    : validID (DISPLAY_NAME STRING)? definable?
      (docReference | annotationRef | rosettaEnumSynonym)*
    ;

// ============================================================================
// Types
// ============================================================================

typeCall
    : qualifiedName (LPAREN typeCallArgument (COMMA typeCallArgument)* RPAREN)?
    ;

typeCallArgument
    : typeParameterValidID COLON typeCallArgumentExpression
    ;

typeCallArgumentExpression
    : typeParameterValidID
    | MINUS? literal
    ;

// ============================================================================
// Cardinality
// ============================================================================

rosettaCardinality
    : LPAREN INT_LITERAL DOT_DOT (INT_LITERAL | STAR) RPAREN
    ;

// ============================================================================
// Root element dispatch
// ============================================================================

rootElement
    : enumeration
    | rosettaBody
    | rosettaCorpus
    | rosettaSegment
    | rosettaBasicType
    | rosettaRecordType
    | rosettaLibraryFunction
    | rosettaSynonymSource
    | rosettaRule
    | rosettaMetaType
    | rosettaExternalSynonymSource
    | rosettaExternalRuleSource
    | rosettaReport
    | rosettaTypeAlias
    | annotationDecl
    | dataType
    | choice
    | function
    // rosettaQualifiableConfiguration is handled in rosettaModel, not here (matches Xtext)
    ;

// ============================================================================
// Literal (for typeCallArgumentExpression)
// ============================================================================

literal
    : STRING
    | INT_LITERAL
    | BIG_DECIMAL
    | TRUE
    | FALSE
    ;

// ============================================================================
// Functions, Conditions, Operations (Task 4)
// ============================================================================

function
    : runeAnnotations?
      FUNC validID (LPAREN validID COLON enumValueReference RPAREN)?
      (EXTENDS qualifiedName)? COLON definable?
      (docReference | annotationRef)*
      (INPUTS COLON attribute+)?
      (OUTPUT COLON attribute)?
      shortcutDeclaration*
      condition*
      operation*
      postCondition*
    ;

enumValueReference
    : qualifiedName ARROW validID
    ;

shortcutDeclaration
    : ALIAS validID COLON definable? exprWithThen
    ;

condition
    : CONDITION validID? COLON definable?
      (docReference | annotationRef)*
      exprWithThen
    ;

postCondition
    : POST_CONDITION validID? COLON definable? exprWithThen
    ;

operation
    : (SET | ADD) validID segment? COLON definable? expressionWithAsKey
    ;

segment
    : ARROW validID segment?
    ;

expressionWithAsKey
    : exprWithThen AS_KEY?
    ;

// ============================================================================
// Expression — comprehensive left-recursive rule (~90 labelled alternatives)
// ============================================================================
//
// Precedence is determined by alternative ORDER for left-recursive alts:
// alternatives listed FIRST = HIGHEST precedence (tightest binding).
//
// ANTLR4 collapses the Xtext manually-layered chain
// (ThenOp → OrOp → AndOp → EqualityOp → ComparisonOp → AdditiveOp → MultiplicativeOp → ... → Primary)
// into a single left-recursive rule.

expression
    // ---- Postfix operations (highest precedence — tightest binding) ----
    // Upstream Rune precedence: postfix operations (feature call, exists,
    // count, only-element, the chain operators, etc.) bind tighter than the
    // binary/boolean operators below. They must therefore precede the binary
    // alternatives so that e.g. `a = b or c exists` parses as
    // `(a = b) or (c exists)` — NOT `((a = b) or c) exists`. (Phase X1 Gap #6.)
    : expression ARROW validID                                             #FeatureCallExpr
    | expression DEEP_ARROW validID                                        #DeepFeatureCallExpr
    | expression existsModifier? EXISTS                                    #ExistsExpr
    | expression IS ABSENT                                                 #AbsentExpr
    | expression ONLY_ELEMENT                                              #OnlyElementExpr
    | expression COUNT                                                     #CountExpr
    | expression FLATTEN                                                   #FlattenExpr
    | expression DISTINCT                                                  #DistinctExpr
    | expression REVERSE                                                   #ReverseExpr
    | expression FIRST                                                     #FirstExpr
    | expression LAST                                                      #LastExpr
    | expression SUM                                                       #SumExpr
    | expression ONE_OF                                                    #OneOfExpr
    | expression necessity CHOICE validID (COMMA validID)*                 #ChoiceExpr
    | expression TO_STRING                                                 #ToStringExpr
    | expression TO_NUMBER                                                 #ToNumberExpr
    | expression TO_INT                                                    #ToIntExpr
    | expression TO_TIME                                                   #ToTimeExpr
    | expression TO_ENUM qualifiedName                                     #ToEnumExpr
    | expression TO_DATE                                                   #ToDateExpr
    | expression TO_DATE_TIME                                              #ToDateTimeExpr
    | expression TO_ZONED_DATE_TIME                                        #ToZonedDateTimeExpr
    | expression SWITCH (switchCaseOrDefault (COMMA switchCaseOrDefault)*)?  #SwitchExpr
    | expression WITH_META (LBRACE (withMetaEntry (COMMA withMetaEntry)*)? RBRACE)?  #WithMetaExpr
    | expression SORT inlineFunction?                                      #SortExpr
    | expression MIN inlineFunction?                                       #MinExpr
    | expression MAX inlineFunction?                                       #MaxExpr
    | expression REDUCE (inlineFunction | implicitInlineFunction)          #ReduceExpr
    | expression FILTER (inlineFunction | implicitInlineFunction)?         #FilterExpr
    | expression EXTRACT (inlineFunction | implicitInlineFunction)?        #ExtractExpr

    // ---- Binary operations (in decreasing precedence) ----
    | expression DEFAULT expression                                        #DefaultExpr
    | expression JOIN expression?                                          #JoinExpr
    | expression CONTAINS expression                                       #ContainsExpr
    | expression DISJOINT expression                                       #DisjointExpr
    | expression (STAR | SLASH) expression                                 #MultiplicativeExpr
    | expression (PLUS | MINUS) expression                                 #AdditiveExpr
    | expression cardinalityModifier? (LT | GT | LTE | GTE) expression    #ComparisonExpr
    | expression cardinalityModifier? (EQ | NEQ) expression                #EqualityExpr
    | expression AND expression                                            #AndExpr
    | expression OR expression                                             #OrExpr

    // ---- "Without left parameter" prefix forms ----
    | OR expression                                                        #OrWithoutLeftExpr
    | AND expression                                                       #AndWithoutLeftExpr
    | cardinalityModifier? (EQ | NEQ) expression                          #EqualityWithoutLeftExpr
    | cardinalityModifier? (LT | GT | LTE | GTE) expression              #ComparisonWithoutLeftExpr
    | (STAR | SLASH) expression                                            #MultiplicativeWithoutLeftExpr
    | CONTAINS expression                                                  #ContainsWithoutLeftExpr
    | DISJOINT expression                                                  #DisjointWithoutLeftExpr
    | DEFAULT expression                                                   #DefaultWithoutLeftExpr
    | JOIN expression?                                                     #JoinWithoutLeftExpr
    | existsModifier? EXISTS                                               #ExistsWithoutLeftExpr
    | IS ABSENT                                                            #AbsentWithoutLeftExpr
    | ONLY_ELEMENT                                                         #OnlyElementWithoutLeftExpr
    | COUNT                                                                #CountWithoutLeftExpr
    | FLATTEN                                                              #FlattenWithoutLeftExpr
    | DISTINCT                                                             #DistinctWithoutLeftExpr
    | REVERSE                                                              #ReverseWithoutLeftExpr
    | FIRST                                                                #FirstWithoutLeftExpr
    | LAST                                                                 #LastWithoutLeftExpr
    | SUM                                                                  #SumWithoutLeftExpr
    | ONE_OF                                                               #OneOfWithoutLeftExpr
    | necessity CHOICE validID (COMMA validID)*                            #ChoiceWithoutLeftExpr
    | TO_STRING                                                            #ToStringWithoutLeftExpr
    | TO_NUMBER                                                            #ToNumberWithoutLeftExpr
    | TO_INT                                                               #ToIntWithoutLeftExpr
    | TO_TIME                                                              #ToTimeWithoutLeftExpr
    | TO_ENUM qualifiedName                                                #ToEnumWithoutLeftExpr
    | TO_DATE                                                              #ToDateWithoutLeftExpr
    | TO_DATE_TIME                                                         #ToDateTimeWithoutLeftExpr
    | TO_ZONED_DATE_TIME                                                   #ToZonedDateTimeWithoutLeftExpr
    | SWITCH (switchCaseOrDefault (COMMA switchCaseOrDefault)*)?           #SwitchWithoutLeftExpr
    | WITH_META (LBRACE (withMetaEntry (COMMA withMetaEntry)*)? RBRACE)?  #WithMetaWithoutLeftExpr
    | SORT inlineFunction?                                                 #SortWithoutLeftExpr
    | MIN inlineFunction?                                                  #MinWithoutLeftExpr
    | MAX inlineFunction?                                                  #MaxWithoutLeftExpr
    | REDUCE (inlineFunction | implicitInlineFunction)                    #ReduceWithoutLeftExpr
    | FILTER (inlineFunction | implicitInlineFunction)?                   #FilterWithoutLeftExpr
    | EXTRACT (inlineFunction | implicitInlineFunction)?                  #ExtractWithoutLeftExpr

    // ---- Prefix unary operations ----
    // Negative LITERAL binds TIGHT (upstream Xtext: `MINUS? literal` at the
    // literal level) — placed BEFORE the general prefix form so `x > -1 and y`
    // parses as `(x > -1) and y`, never `x > -(1 and y)` (the prefix
    // alternative's operand binds at prefix precedence and absorbed every
    // weaker operator — the coverage wave-D datarule catch, golden
    // CorrelationReturnTermsCorrelationValue).
    | MINUS literal                                                        #NegativeLiteralExpr
    | (PLUS | MINUS) expression                                           #AdditiveWithoutLeftExpr

    // ---- Primary expressions (highest precedence) ----
    | IF expression THEN expression (ELSE expression)?                    #ConditionalExpr
    | constructorExpression                                               #ConstructorExpr
    | onlyExistsExpression                                                #OnlyExistsExpr
    | qualifiedName LPAREN (exprWithThen (COMMA exprWithThen)*)? RPAREN   #FunctionCallExpr
    | qualifiedName ARROW validID                                          #EnumValueRefExpr
    | qualifiedName                                                        #SymbolRefExpr
    | SUPER                                                                #SuperCallExpr
    | ITEM                                                                 #ImplicitVarExpr
    | EMPTY                                                                #EmptyExpr
    | LBRACK (exprWithThen (COMMA exprWithThen)*)? RBRACK                 #ListLiteralExpr
    | literal                                                              #LiteralExpr
    | LPAREN exprWithThen RPAREN                                          #ParenExpr
    ;

// The `then` chain combinator is the OUTERMOST (lowest-precedence) expression
// layer — it must sit ABOVE the conditional so that `if A then B then extract C`
// parses as `(if A then B) then extract C`, not `if (A then B) then (extract C)`.
// Upstream Rosetta.xtext realises this with a dedicated `ThenOperation` rule that
// wraps `OrOperation` (`ThenOperation: OrOperation (then function)*`) and uses
// `OrOperation` — NOT the full expression — for the conditional's three operands.
// Mirroring that here: the left-recursive `expression` rule above is `then`-free,
// the conditional's `IF expression THEN expression (ELSE expression)?` operands
// are therefore `then`-free by construction, and this thin wrapper layers the
// left-associative `then` chain on top. Every "fresh" expression context (rule /
// function-operation / alias / condition bodies, inline-function bodies, list
// elements, parenthesised + function-call arguments) routes through `exprWithThen`;
// binary/postfix operands and the `then`-body implicit inline function stay on the
// `then`-free `expression` so chain associativity + operator precedence are preserved.
exprWithThen
    : expression thenSuffix*
    ;

thenSuffix
    : THEN (inlineFunction | implicitInlineFunction)?
    ;

// ============================================================================
// Expression support rules
// ============================================================================

cardinalityModifier : ANY | ALL ;
existsModifier : SINGLE | MULTIPLE ;
necessity : OPTIONAL | REQUIRED ;

inlineFunction
    : (closureParameter (COMMA closureParameter)*)? LBRACK exprWithThen RBRACK
    ;

// NOTE: implicitInlineFunction stays on the `then`-free `expression` (NOT
// exprWithThen). It serves as the body of chain ops (extract/filter/reduce) AND
// of the `then` combinator itself; keeping it `then`-free preserves left-assoc
// `then` chaining (the wrapper's thenSuffix* loop owns the chain) and matches
// upstream's ImplicitInlineFunction-over-OrOperation layering.
implicitInlineFunction
    : expression
    ;

closureParameter : ID ;

switchCaseOrDefault
    : DEFAULT exprWithThen
    | switchCaseGuard THEN exprWithThen
    ;

switchCaseGuard
    : literal
    | qualifiedName
    ;

withMetaEntry
    : validID COLON exprWithThen
    ;

constructorExpression
    : typeCall LBRACE (constructorKeyValuePair (COMMA constructorKeyValuePair)* (COMMA | COMMA DOT_DOT DOT)?)? (DOT_DOT DOT)? RBRACE
    ;

constructorKeyValuePair
    : validID COLON expressionWithAsKey
    ;

onlyExistsExpression
    : (onlyExistsElement | LPAREN onlyExistsElement (COMMA onlyExistsElement)* RPAREN) ONLY EXISTS
    ;

onlyExistsElement
    : (qualifiedName | ITEM) (ARROW validID)*
    ;

// ============================================================================
// Synonyms (Xtext lines 293-354)
// ============================================================================

// Attribute-level synonym: [synonym SOURCE value "field" path "p" maps 2]
rosettaSynonym
    : LBRACK SYNONYM qualifiedName (COMMA qualifiedName)*
      rosettaSynonymBody
      RBRACK
    ;

// Complex synonym body with five alternatives + optional trailing modifiers
rosettaSynonymBody
    : ( VALUE (rosettaSynonymValue COMMA)* rosettaSynonymValue rosettaMapping? (META (STRING COMMA)* STRING)?
      | HINT (STRING COMMA)* STRING
      | MERGE rosettaMergeSynonymValue
      | rosettaMappingSetTo
      | META (STRING COMMA)* STRING
      )
      (DATE_FORMAT STRING)?
      (PATTERN STRING STRING)?
      REMOVE_HTML?
      (MAPPER STRING)?
    ;

// Synonym value: name (refType value)? ('path' path)? ('maps' count)?
rosettaSynonymValue
    : STRING (rosettaSynonymRef INT_LITERAL)?
      (PATH STRING)?
      (MAPS INT_LITERAL)?
    ;

// Meta synonym value (same structure as rosettaSynonymValue)
rosettaMetaSynonymValue
    : STRING (rosettaSynonymRef INT_LITERAL)?
      (PATH STRING)?
      (MAPS INT_LITERAL)?
    ;

// Class-level synonym: [synonym SOURCE value "className" meta "metaField"]
classSynonym
    : LBRACK SYNONYM qualifiedName (COMMA qualifiedName)*
      (VALUE classSynonymValue)?
      (META rosettaMetaSynonymValue)?
      RBRACK
    ;

// Class synonym value: name (refType value)? ('path' path)?
classSynonymValue
    : STRING (rosettaSynonymRef INT_LITERAL)?
      (PATH STRING)?
    ;

// Merge synonym value: name ('when' 'path' '<>' excludePath)?
rosettaMergeSynonymValue
    : STRING (WHEN PATH NEQ STRING)?
    ;

// Enum synonym: [synonym SOURCE value "enumVal" definition "def" pattern "m" "r" removeHtml]
rosettaEnumSynonym
    : LBRACK SYNONYM qualifiedName (COMMA qualifiedName)*
      VALUE STRING (DEFINITION STRING)?
      (PATTERN STRING STRING)?
      REMOVE_HTML?
      RBRACK
    ;

// Synonym source declaration: synonym source MY_SOURCE
rosettaSynonymSource
    : SYNONYM SOURCE validID
    ;

// Synonym ref enum: tag | componentID
rosettaSynonymRef
    : TAG
    | COMPONENT_ID
    ;

// ============================================================================
// Mapping Logic (Xtext lines 360-427)
// ============================================================================

// Comma-separated mapping instances attached to a synonym value
rosettaMapping
    : rosettaMappingInstance (COMMA rosettaMappingInstance)*
    ;

// "set when <tests>" or "default to <value>"
rosettaMappingInstance
    : SET WHEN rosettaMappingPathTests
    | DEFAULT TO rosettaMapPrimaryExpression
    ;

// "set to" form (used as standalone body alternative in rosettaSynonymBody)
rosettaMappingSetTo
    : rosettaMappingSetToInstance (COMMA rosettaMappingSetToInstance)*
    ;

// "set to <value> (when <tests>)?"
rosettaMappingSetToInstance
    : SET TO rosettaMapPrimaryExpression (WHEN rosettaMappingPathTests)?
    ;

// AND-chained mapping path tests
rosettaMappingPathTests
    : rosettaMapTest (AND rosettaMapTest)*
    ;

// Dispatch to specific mapping test forms
rosettaMapTest
    : rosettaMapPath
    | rosettaMapRosettaPath
    | rosettaMapTestExpression
    | rosettaMapTestFunc
    ;

// path = "some.path"
rosettaMapPath
    : PATH EQ rosettaMapPathValue
    ;

// rosettaPath = DataType -> attribute -> subAttribute
rosettaMapRosettaPath
    : ROSETTA_PATH EQ rosettaAttributeReference
    ;

// String path value with optional exists/absent/equality test
rosettaMapTestExpression
    : rosettaMapPathValue EXISTS                                 #MapTestExistsExpr
    | rosettaMapPathValue IS ABSENT                              #MapTestAbsentExpr
    | rosettaMapPathValue (EQ | NEQ) rosettaMapPrimaryExpression #MapTestEqualityExpr
    ;

// Primary expressions for mapping tests (literals and enum value refs)
rosettaMapPrimaryExpression
    : enumValueReference
    | STRING
    | TRUE
    | FALSE
    | INT_LITERAL
    | BIG_DECIMAL
    ;

// String path value wrapper
rosettaMapPathValue
    : STRING
    ;

// condition-func reference with optional condition-path
rosettaMapTestFunc
    : CONDITION_FUNC qualifiedName (CONDITION_PATH rosettaMapPathValue)?
    ;

// ============================================================================
// Attribute Reference (Xtext lines 396-402)
// ============================================================================

// Chained data->attribute reference: DataType -> attr1 -> attr2
rosettaAttributeReference
    : qualifiedName (ARROW validID)+
    ;

// ============================================================================
// External Sources (Xtext lines 810-876)
// ============================================================================

// External synonym source with extends and body
rosettaExternalSynonymSource
    : SYNONYM SOURCE validID
      (EXTENDS qualifiedName (COMMA qualifiedName)*)?
      LBRACE
          rosettaExternalClass*
          (ENUMS rosettaExternalEnum*)?
      RBRACE
    ;

// External rule source with extends and body
rosettaExternalRuleSource
    : RULE SOURCE validID
      (EXTENDS qualifiedName (COMMA qualifiedName)*)?
      LBRACE
          rosettaExternalClass*
          (ENUMS rosettaExternalEnum*)?
      RBRACE
    ;

// External class mapping with optional class synonyms and attribute mappings
rosettaExternalClass
    : qualifiedName COLON
      rosettaExternalClassSynonym*
      rosettaExternalRegularAttribute*
    ;

// External enum mapping with enum value mappings
rosettaExternalEnum
    : qualifiedName COLON
      rosettaExternalEnumValue*
    ;

// +/- attribute with synonyms and rule references
rosettaExternalRegularAttribute
    : (PLUS | MINUS) validID
      rosettaExternalSynonym*
      ruleReferenceAnnotation*
    ;

// +/- enum value with synonyms
rosettaExternalEnumValue
    : (PLUS | MINUS) validID
      rosettaExternalEnumSynonym*
    ;

// Class synonym in external source: [value "x" meta "y"]
rosettaExternalClassSynonym
    : LBRACK
      (VALUE classSynonymValue)?
      META rosettaMetaSynonymValue
      RBRACK
    ;

// External synonym wrapping a synonym body
rosettaExternalSynonym
    : LBRACK rosettaSynonymBody RBRACK
    ;

// External enum synonym: [value "name" definition "def" pattern "m" "r"]
rosettaExternalEnumSynonym
    : LBRACK
      VALUE STRING (DEFINITION STRING)?
      (PATTERN STRING STRING)?
      RBRACK
    ;

// ============================================================================
// Regulatory — Body, Corpus, Segment (Xtext lines 430-460)
// ============================================================================

// body Authority ESMA <"European Securities and Markets Authority">
rosettaBody
    : BODY ID validID definable?
    ;

// corpus Regulation ESMA "MiFIR" MiFIR_RTS <"...">
rosettaCorpus
    : CORPUS ID qualifiedName? STRING? validID definable?
    ;

// segment article
// Note: 'rationale', 'rationale_author', 'structured_provision' are allowed as segment
// names for backwards compatibility — they were previously used as segments before
// becoming keywords.
rosettaSegment
    : SEGMENT (validID | RATIONALE | RATIONALE_AUTHOR | STRUCTURED_PROVISION)
    ;

// ============================================================================
// Document References (Xtext lines 433-447, 883-889)
// ============================================================================

// [docReference ESMA MiFIR_RTS article "1" section "2"
//     rationale "..." rationale_author "..." provision "..."]
docReference
    : LBRACK (REGULATORY_REFERENCE | DOC_REFERENCE)
      (FOR annotationPathExpression)?
      regulatoryDocumentReference
      regulatoryReferenceArgs?              // P1.4.2 H4 — named-args
      documentRationale*
      (STRUCTURED_PROVISION STRING)?
      (PROVISION STRING)?
      REPORTED_FIELD?
      RBRACK
    ;

// P1.4.2 H4 — optional named-arg list after positional regulatoryDocumentReference.
// LL(*) safe: LPAREN unique vs documentRationale's RATIONALE/RATIONALE_AUTHOR.
regulatoryReferenceArgs
    : LPAREN namedArg (COMMA namedArg)* RPAREN
    ;

namedArg
    : validID EQ STRING
    ;

// body corpusList+ segmentRef*
regulatoryDocumentReference
    : qualifiedName qualifiedName+ rosettaSegmentRef*
    ;

// segment "value"
rosettaSegmentRef
    : qualifiedName STRING
    ;

// rationale "..." (rationale_author "...")? | rationale_author "..." (rationale "...")?
documentRationale
    : RATIONALE STRING (RATIONALE_AUTHOR STRING)?
    | RATIONALE_AUTHOR STRING (RATIONALE STRING)?
    ;

// ============================================================================
// Label and Rule-Reference Annotations (Xtext lines 906-937)
// ============================================================================

// [label "Trade ID"]  or  [label for item -> name "Party Name"]
labelAnnotation
    : LBRACK LABEL
      (FOR annotationPathExpression | annotationPathExpression? AS)?
      STRING
      RBRACK
    ;

// [ruleReference TradeIdRule]  or  [ruleReference empty]
ruleReferenceAnnotation
    : LBRACK RULE_REFERENCE
      (FOR annotationPathExpression)?
      (qualifiedName | EMPTY)
      RBRACK
    ;

// Annotation path: item -> name  or  parties ->> name
// PrimaryAnnotationPath is validID or ITEM, then chained with -> or ->>
annotationPathExpression
    : (validID | ITEM) (ARROW validID | DEEP_ARROW validID)*
    ;

// ============================================================================
// Rune annotations (P1.4.2 H2 / H11) — @-prefix syntax distinct from the
// legacy bracket-style annotation/annotationRef rules above. Reserved for
// fork-specific features (W13 opt-in markers and future feature-flags).
// Layer-1 only: parser + AST surface; no runtime semantics in P1.4.2.
// ============================================================================

runeAnnotations
    : runeAnnotation+
    ;

runeAnnotation
    : AT qualifiedName (LPAREN runeAnnotationArgs? RPAREN)?
    ;

runeAnnotationArgs
    : runeAnnotationArg (COMMA runeAnnotationArg)*
    ;

runeAnnotationArg
    : validID EQ runeAnnotationLiteral
    ;

// Reuses existing `literal` rule (STRING | INT_LITERAL | BIG_DECIMAL | TRUE | FALSE)
// + qualifiedName for symbol-style values like `@feature(target = MyType)`.
runeAnnotationLiteral
    : literal
    | qualifiedName
    ;

// ============================================================================
// Reporting (Xtext lines 895-917)
// ============================================================================

// report ESMA MiFIR_RTS in T+1 from Trade when IsReportable with type TradeReport
rosettaReport
    : REPORT regulatoryDocumentReference
      IN (REAL_TIME | T_PLUS_1 | T_PLUS_2 | T_PLUS_3 | T_PLUS_4 | T_PLUS_5 | ASATP)
      FROM typeCall
      WHEN qualifiedName (AND qualifiedName)*
      (USING STANDARD qualifiedName)?
      WITH TYPE qualifiedName
      (WITH SOURCE qualifiedName)?
    ;

// reporting rule TradeIdRule from Trade: <"..."> extract item -> tradeId
// eligibility rule IsReportable from Trade: <"..."> extract item -> isActive
rosettaRule
    : runeAnnotations?
      (REPORTING | ELIGIBILITY) RULE validID (FROM typeCall)? COLON
      definable?
      docReference*
      (exprWithThen (AS STRING)?)?
    ;

// ============================================================================
// Qualifiable Configuration (Xtext lines 522-528)
// ============================================================================

// isEvent root MyEvent;  or  isProduct root MyProduct;
rosettaQualifiableConfiguration
    : (IS_EVENT | IS_PRODUCT) ROOT qualifiedName SEMI
    ;

// ============================================================================
// Built-in Types (Xtext lines 230-277)
// ============================================================================

// basicType myBool <"A boolean.">
// basicType myNum(digits int, fractionalDigits int) <"A number.">
rosettaBasicType
    : BASIC_TYPE validID typeParameters? definable?
    ;

// Parenthesized type parameter list: (digits int, fractionalDigits int)
typeParameters
    : LPAREN typeParameter (COMMA typeParameter)* COMMA? RPAREN
    ;

// Single type parameter: digits int <"Number of digits.">
typeParameter
    : typeParameterValidID typeCall definable?
    ;

// recordType myRecord { <"A record."> field1 int  field2 string }
rosettaRecordType
    : RECORD_TYPE validID LBRACE definable? rosettaRecordFeature* RBRACE
    ;

// field1 int
rosettaRecordFeature
    : validID typeCall
    ;

// library function MyFunc(x number, y number) number <"Add two numbers.">
rosettaLibraryFunction
    : LIBRARY FUNCTION validID
      LPAREN (rosettaParameter (COMMA rosettaParameter)*)? RPAREN
      typeCall definable?
    ;

// x number  or  items number[]
rosettaParameter
    : validID typeCall (LBRACK RBRACK)?
    ;

// typeAlias posInt: <"Positive integer."> number(fractionalDigits: 0, min: 0)
rosettaTypeAlias
    : TYPE_ALIAS validID typeParameters? COLON definable? typeCall condition*
    ;

// metaType myMeta string
rosettaMetaType
    : META_TYPE validID typeCall
    ;
