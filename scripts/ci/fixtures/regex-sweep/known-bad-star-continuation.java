// Smoke fixture for scripts/ci/no-regex-on-structured-content.sh.
// EXPECTED: FAIL — a CODE line that begins with `*` (a multiplication
// continuation) is not a javadoc line: the leading-star rule applies only
// inside an open block comment. Pins the #606 review's finding that a
// comment-only skip keyed on the first character alone would let a real
// regex call through.
public class KnownBadStarContinuation {

    public int weight(String s) {
        return 2
                * s.replaceAll("a", "b").length();
    }
}
