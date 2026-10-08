// Smoke fixture for scripts/ci/no-regex-on-structured-content.sh.
// EXPECTED: FAIL — the marker's comment block is followed by an UNRELATED code
// line, which ends the block's reach; the regex call two lines below it is
// therefore unmarked. Pins that the block reach is exactly one code line and
// cannot be stretched by a marker left stranded above other code.
public class KnownBadStaleMarker {

    public boolean check(String input) {
        // ci-allowlist: regex-on-structured-content (stranded: the next line
        // is not the regex call)
        String trimmed = input.trim();
        return trimmed.matches("^bar$");
    }
}
