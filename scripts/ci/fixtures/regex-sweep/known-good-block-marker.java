// Smoke fixture for scripts/ci/no-regex-on-structured-content.sh.
// EXPECTED: PASS — the marker heads a multi-line comment BLOCK that immediately
// precedes the regex call (the TypeFormatConstraintScan shape: marker three
// lines above the call, inside its own explanation).
public class KnownGoodBlockMarker {

    public boolean check(String input) {
        // ci-allowlist: regex-on-structured-content (the marker opens an
        // explanatory block; the block's reach is the one code line that
        // follows it, which is the call below)
        return input.matches("^foo$");
    }
}
