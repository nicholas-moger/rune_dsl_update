// Smoke fixture for scripts/ci/no-regex-on-structured-content.sh.
// EXPECTED: FAIL — regex call has no ci-allowlist marker.
public class KnownBad {
    public boolean check(String input) {
        return input.matches("^bar$");
    }
}
