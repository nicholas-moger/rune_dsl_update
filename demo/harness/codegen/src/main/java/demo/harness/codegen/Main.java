package demo.harness.codegen;

/**
 * Entry point for the Rune DSL stats demo's FORK code-generation harness.
 *
 * <pre>
 * generate --mode m1|m2|m3 --src "dir;dir;..." --builtins &lt;dir&gt; --out &lt;dir&gt;
 *          [--filter drr|all] [--receipt &lt;file&gt;] [--command "&lt;exact line&gt;"]
 * ir-dump  --src &lt;file&gt; --builtins &lt;dir&gt; --outJson &lt;file&gt; --outText &lt;file&gt; [--outPython &lt;file&gt;] [--receipt &lt;file&gt;]
 * ast-dump --src &lt;file&gt; --builtins &lt;dir&gt; --out &lt;file&gt; [--receipt &lt;file&gt;]
 * </pre>
 *
 * <p>Exit codes: {@code 0} success, {@code 2} usage error, {@code 3} the run produced
 * generation errors or could not find its subject, {@code 4} an unexpected failure.
 *
 * <p>Modes m1/m2/m3 are a LABEL here: the route is selected by the outer command line
 * (system properties plus classpath), never by this code. See the module README.
 */
public final class Main {

    private Main() {
    }

    private static final String USAGE = String.join("\n",
            "demo-harness-codegen (org.finos.rune.demo:demo-harness-codegen:1.0.0)",
            "",
            "  generate  --mode m1|m2|m3 --src \"dir;dir;...\" --builtins <dir> --out <dir>",
            "            [--filter drr|all] [--receipt <file>] [--command \"<exact line>\"]",
            "  ir-dump   --src <file> --builtins <dir> --outJson <file> --outText <file> [--outPython <file>]",
            "            [--receipt <file>] [--command \"<exact line>\"]",
            "  ast-dump  --src <file> --builtins <dir> --out <file>",
            "            [--receipt <file>] [--command \"<exact line>\"]",
            "",
            "  --mode is a label only; m2/m3 are selected by the outer command line's system",
            "  properties and classpath (see README.md).",
            "  --filter defaults to 'all' (everything except the com.rosetta.model builtins).");

    public static void main(String[] argv) {
        if (argv.length == 0) {
            System.err.println(USAGE);
            System.exit(2);
        }
        String verb = argv[0];
        int code;
        try {
            Args args = Args.parse(argv, 1);
            code = switch (verb) {
                case "generate" -> GenerateCommand.run(args);
                case "ir-dump" -> IrDumpCommand.run(args);
                case "ast-dump" -> AstDumpCommand.run(args);
                case "-h", "--help", "help" -> {
                    System.out.println(USAGE);
                    yield 0;
                }
                default -> {
                    System.err.println("unknown verb '" + verb + "'\n\n" + USAGE);
                    yield 2;
                }
            };
        } catch (IllegalArgumentException e) {
            DemoOut.error(String.valueOf(e.getMessage()));
            System.err.println(USAGE);
            code = 2;
        } catch (Throwable t) {
            // A demo that dies silently is worse than one that dies loudly: emit the protocol
            // error event so the dashboard shows the failure, then the stack trace for a human.
            DemoOut.error(t.getClass().getSimpleName() + ": " + t.getMessage());
            t.printStackTrace();
            code = 4;
        }
        System.exit(code);
    }
}
