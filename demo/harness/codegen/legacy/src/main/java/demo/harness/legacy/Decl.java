package demo.harness.legacy;

import java.util.List;

/**
 * One declaration, reduced to exactly the facts the demo-contract section 5 queries need.
 *
 * <p>This is the SEAM between the two analytics lanes. Each lane's extractor walks its own
 * model -- the fork's {@code RModel} tree, or upstream's EMF {@code Resource} contents -- and
 * produces a {@code List<Decl>}. Everything after that point is {@link Queries}, which is the
 * SAME SOURCE TEXT in both lanes (duplicated byte-for-byte apart from its package line, because
 * the two lanes cannot share a jar). If the two lanes disagree on a query answer, the
 * disagreement is therefore located in an extractor, never in the query.
 *
 * <p>This file is duplicated into the legacy module for the same reason.
 *
 * @param name                     the declaration's simple name
 * @param kind                     see {@link Kind}
 * @param namespace                the declaring model's namespace
 * @param file                     the source file's bare name
 * @param superSimpleName          the declared super-type's SIMPLE name ({@code null} if none).
 *                                 Simple, not qualified: a source may write
 *                                 {@code extends cdm.base.Foo} or {@code extends Foo} for the
 *                                 same target, and only the simple form is derivable in every
 *                                 lane, including one with no resolver at all.
 * @param metaAnnotated            whether the declaration or ANY of its attributes carries a
 *                                 {@code [metadata ...]} annotation
 * @param attributeTypeSimpleNames the SIMPLE name of each attribute's declared type, in
 *                                 declaration order, with duplicates kept
 */
public record Decl(String name, Kind kind, String namespace, String file,
                   String superSimpleName, boolean metaAnnotated,
                   List<String> attributeTypeSimpleNames) {

    /**
     * The declaration kinds the queries distinguish.
     *
     * <p>{@link #TYPE} means a {@code type} declaration ONLY. {@code choice} is
     * {@link #CHOICE}, deliberately separate: the fork models it as its own AST class while
     * upstream's EMF {@code Choice} extends {@code Data}, so counting "data types" as
     * "everything that is a Data" would make the two lanes disagree by construction. The
     * grammar has two distinct productions and the queries follow the grammar.
     */
    public enum Kind {
        /** A {@code type} declaration. Counted by Q1 "types" and scanned by Q2/Q3/Q4. */
        TYPE,
        /** An {@code enum} declaration. Counted by Q1 "enums". */
        ENUM,
        /** A {@code func} declaration, including dispatch functions. Counted by Q1. */
        FUNCTION,
        /** A {@code reporting rule}. Counted by Q1 "rules". Eligibility rules are not. */
        REPORTING_RULE,
        /** An {@code eligibility rule}. Indexed, never counted by Q1. */
        ELIGIBILITY_RULE,
        /** A {@code choice} declaration. Indexed, never counted as a data type. */
        CHOICE,
        /** Anything else indexed for completeness: aliases, reports, bodies, corpora, ... */
        OTHER
    }

    /** The part of a possibly-qualified type reference after the last dot. */
    public static String simpleName(String maybeQualified) {
        if (maybeQualified == null) {
            return null;
        }
        int dot = maybeQualified.lastIndexOf('.');
        return dot >= 0 ? maybeQualified.substring(dot + 1) : maybeQualified;
    }
}
