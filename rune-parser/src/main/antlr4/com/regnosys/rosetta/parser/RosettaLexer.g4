lexer grammar RosettaLexer;

// NOTE: Hyphenated keywords (e.g., 'post-condition', 'to-date') are single lexer tokens.
// The input 'to-date' matches TO_DATE, while 'to - date' (with spaces) matches TO MINUS ID.
// This is by design — Rosetta syntax requires these to be written without spaces.
// Similarly, T+1 through T+5 are single tokens; 'T + 1' with spaces is ID PLUS INT_LITERAL.
//
// The '^' prefix on ID allows keyword escaping: ^type is a valid identifier, not the TYPE keyword.

// ============================================================================
// Keywords — Top-level structure
// ============================================================================

NAMESPACE       : 'namespace' ;
OVERRIDE        : 'override' ;
VERSION         : 'version' ;
FILE_HEADER     : 'fileHeader' ;     // P1.4.2 H3
DEPENDS_ON      : 'depends-on' ;     // P1.4.2 H3
EXPERIMENTAL    : 'experimental' ;   // P1.4.2 H3
SCOPE           : 'scope' ;
IMPORT          : 'import' ;
AS              : 'as' ;

// ============================================================================
// Keywords — Type declarations
// ============================================================================

TYPE            : 'type' ;
EXTENDS         : 'extends' ;
ENUM            : 'enum' ;
CHOICE          : 'choice' ;
BASIC_TYPE      : 'basicType' ;
RECORD_TYPE     : 'recordType' ;
TYPE_ALIAS      : 'typeAlias' ;
META_TYPE       : 'metaType' ;
LIBRARY         : 'library' ;
FUNCTION        : 'function' ;

// ============================================================================
// Keywords — Annotations
// ============================================================================

ANNOTATION      : 'annotation' ;
PREFIX          : 'prefix' ;

// ============================================================================
// Keywords — Attributes
// ============================================================================

DISPLAY_NAME    : 'displayName' ;

// ============================================================================
// Keywords — Functions
// ============================================================================

FUNC            : 'func' ;
INPUTS          : 'inputs' ;
OUTPUT          : 'output' ;
ALIAS           : 'alias' ;
SET             : 'set' ;
ADD             : 'add' ;

// Hyphenated keyword — must be before ID to prevent matching as separate tokens
POST_CONDITION  : 'post-condition' ;

// ============================================================================
// Keywords — Conditions / Control
// ============================================================================

CONDITION       : 'condition' ;
IF              : 'if' ;
THEN            : 'then' ;
ELSE            : 'else' ;
SWITCH          : 'switch' ;
DEFAULT         : 'default' ;
CASE            : 'case' ;
WHEN            : 'when' ;
FROM            : 'from' ;
TO              : 'to' ;

// ============================================================================
// Keywords — Logic / Comparison operators
// ============================================================================

AND             : 'and' ;
OR              : 'or' ;
IS              : 'is' ;
EXISTS          : 'exists' ;
ABSENT          : 'absent' ;
CONTAINS        : 'contains' ;
DISJOINT        : 'disjoint' ;
ONLY            : 'only' ;
EMPTY           : 'empty' ;

// ============================================================================
// Keywords — Cardinality modifiers
// ============================================================================

ANY             : 'any' ;
ALL             : 'all' ;
SINGLE          : 'single' ;
MULTIPLE        : 'multiple' ;
OPTIONAL        : 'optional' ;
REQUIRED        : 'required' ;

// ============================================================================
// Keywords — List operations (hyphenated keywords first)
// ============================================================================

ONLY_ELEMENT    : 'only-element' ;
ONE_OF          : 'one-of' ;

EXTRACT         : 'extract' ;
FILTER          : 'filter' ;
REDUCE          : 'reduce' ;
SORT            : 'sort' ;
FIRST           : 'first' ;
LAST            : 'last' ;
FLATTEN         : 'flatten' ;
DISTINCT        : 'distinct' ;
REVERSE         : 'reverse' ;
SUM             : 'sum' ;
MIN             : 'min' ;
MAX             : 'max' ;
COUNT           : 'count' ;

// ============================================================================
// Keywords — Conversion operations (all hyphenated)
// ============================================================================

// Longer hyphenated keywords must come before shorter ones
TO_ZONED_DATE_TIME : 'to-zoned-date-time' ;
TO_DATE_TIME    : 'to-date-time' ;
TO_STRING       : 'to-string' ;
TO_NUMBER       : 'to-number' ;
TO_INT          : 'to-int' ;
TO_TIME         : 'to-time' ;
TO_ENUM         : 'to-enum' ;
TO_DATE         : 'to-date' ;

// ============================================================================
// Keywords — Special (hyphenated first)
// ============================================================================

AS_KEY          : 'as-key' ;
WITH_META       : 'with-meta' ;

SUPER           : 'super' ;
ITEM            : 'item' ;
JOIN            : 'join' ;
TRUE            : 'True' ;
FALSE           : 'False' ;

// ============================================================================
// Keywords — Synonyms
// ============================================================================

SYNONYM         : 'synonym' ;
SOURCE          : 'source' ;
VALUE           : 'value' ;
HINT            : 'hint' ;
MERGE           : 'merge' ;
MAPPER          : 'mapper' ;
META            : 'meta' ;
REMOVE_HTML     : 'removeHtml' ;
DATE_FORMAT     : 'dateFormat' ;
PATTERN         : 'pattern' ;
PATH            : 'path' ;
MAPS            : 'maps' ;
TAG             : 'tag' ;
COMPONENT_ID    : 'componentID' ;
DEFINITION      : 'definition' ;
ENUMS           : 'enums' ;

// ============================================================================
// Keywords — Mapping (hyphenated)
// ============================================================================

CONDITION_FUNC  : 'condition-func' ;
CONDITION_PATH  : 'condition-path' ;

ROSETTA_PATH    : 'rosettaPath' ;

// ============================================================================
// Keywords — Regulatory / Reporting
// ============================================================================

BODY            : 'body' ;
CORPUS          : 'corpus' ;
SEGMENT         : 'segment' ;
REPORT          : 'report' ;
RULE            : 'rule' ;
ELIGIBILITY     : 'eligibility' ;
REPORTING       : 'reporting' ;
REGULATORY_REFERENCE : 'regulatoryReference' ;
DOC_REFERENCE   : 'docReference' ;
FOR             : 'for' ;
RATIONALE       : 'rationale' ;
RATIONALE_AUTHOR : 'rationale_author' ;
STRUCTURED_PROVISION : 'structured_provision' ;
PROVISION       : 'provision' ;
REPORTED_FIELD  : 'reportedField' ;
RULE_REFERENCE  : 'ruleReference' ;
LABEL           : 'label' ;
IN              : 'in' ;
USING           : 'using' ;
STANDARD        : 'standard' ;
WITH            : 'with' ;
ROOT            : 'root' ;

// ============================================================================
// Keywords — Timing (report) — hyphenated and special tokens first
// ============================================================================

REAL_TIME       : 'real-time' ;
T_PLUS_1        : 'T+1' ;
T_PLUS_2        : 'T+2' ;
T_PLUS_3        : 'T+3' ;
T_PLUS_4        : 'T+4' ;
T_PLUS_5        : 'T+5' ;
ASATP           : 'ASATP' ;

// ============================================================================
// Keywords — Qualifiable
// ============================================================================

IS_EVENT        : 'isEvent' ;
IS_PRODUCT      : 'isProduct' ;

// ============================================================================
// Punctuation / Operators
// ============================================================================

// Multi-character operators MUST come before their single-character prefixes

DEEP_ARROW      : '->>' ;
ARROW           : '->' ;
DOT_DOT         : '..' ;
NEQ             : '<>' ;
LTE             : '<=' ;
GTE             : '>=' ;
AT              : '@' ;       // P1.4.2 H2 — rune annotations

LPAREN          : '(' ;
RPAREN          : ')' ;
LBRACE          : '{' ;
RBRACE          : '}' ;
LBRACK          : '[' ;
RBRACK          : ']' ;
DOT             : '.' ;
COMMA           : ',' ;
COLON           : ':' ;
SEMI            : ';' ;
STAR            : '*' ;
PLUS            : '+' ;
MINUS           : '-' ;
SLASH           : '/' ;
EQ              : '=' ;
LT              : '<' ;
GT              : '>' ;

// ============================================================================
// Literals
// ============================================================================

STRING
    : '"' ( '\\' . | ~["\\] )* '"'
    | '\'' ( '\\' . | ~['\\] )* '\''
    ;

fragment DIGIT : [0-9] ;
fragment EXPONENT : [eE] [+\-]? DIGIT+ ;

// BIG_DECIMAL must come BEFORE INT_LITERAL so that "3.14" matches here
// Supports: 3.14, .5, 3.14e10, 3.14E-5, .5e2, 3.e4, 1e5
// Note: bare trailing-dot forms like '3.' are NOT supported — they would
// prevent '1..5' from lexing as INT DOT_DOT INT range syntax.
BIG_DECIMAL
    : DIGIT+ '.' DIGIT+ EXPONENT?
    | '.' DIGIT+ EXPONENT?
    | DIGIT+ '.' EXPONENT
    | DIGIT+ EXPONENT
    ;

INT_LITERAL     : DIGIT+ ;

// ============================================================================
// Identifiers — MUST come AFTER all keywords
// ============================================================================

ID              : '^'? [a-zA-Z_] [a-zA-Z_0-9]* ;

// ============================================================================
// Whitespace and Comments — sent to HIDDEN channel
// ============================================================================

WS              : [ \t\r\n]+ -> channel(HIDDEN) ;
LINE_COMMENT    : '//' ~[\r\n]* -> channel(HIDDEN) ;
BLOCK_COMMENT   : '/*' .*? '*/' -> channel(HIDDEN) ;
