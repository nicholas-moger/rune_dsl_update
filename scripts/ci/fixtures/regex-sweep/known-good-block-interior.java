/* Smoke fixture for scripts/ci/no-regex-on-structured-content.sh.
   EXPECTED: PASS — an UNSTARRED block comment: its interior lines carry no
   leading `*`, so only the block state can classify them as comment-only;
   the marker on the block's first line reaches the call that follows the
   block, and the mention of Pattern.compile inside the block is prose. */
public class KnownGoodBlockInterior {

    public boolean check(String input) {
        /* ci-allowlist: regex-on-structured-content (this block explains the
           call below; it even names Pattern.compile("x") as prose)
         */
        return input.matches("^foo$");
    }
}
