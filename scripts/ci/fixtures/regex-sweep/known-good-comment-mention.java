/**
 * Smoke fixture for scripts/ci/no-regex-on-structured-content.sh.
 * EXPECTED: PASS — every forbidden token below sits in a COMMENT: a javadoc
 * sentence that renders {@code of(Pattern.compile("…"))} describes generated
 * text, and a line comment that says input.matches("x") is prose. Neither is
 * a call, so neither is a site.
 */
public class KnownGoodCommentMention {

    /* a block comment mentioning s.replaceAll("a", "b") is not a site */
    public String render() {
        // and a line comment mentioning SENTINEL.matcher(body) is not a site
        return "rendered";
    }
}
