// Smoke fixture for scripts/ci/no-regex-on-structured-content.sh.
// EXPECTED: PASS — every regex call has the ci-allowlist marker.
public class KnownGood {

    public boolean check(String input) {
        // ci-allowlist: regex-on-structured-content (test fixture)
        return input.matches("^foo$");
    }

    public String indent(String s) {
        // ci-allowlist: regex-on-structured-content (test fixture)
        return s.replaceAll("(?m)^", "\t");
    }
}
