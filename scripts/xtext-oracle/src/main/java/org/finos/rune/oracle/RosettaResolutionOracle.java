package org.finos.rune.oracle;

import com.google.inject.Injector;
import com.regnosys.rosetta.RosettaStandaloneSetup;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.diagnostics.Severity;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * THE UPSTREAM RESOLUTION ORACLE — runs the RELEASED rune-lang 9.83.0 Xtext
 * pipeline over a set of {@code .rosetta} sources and dumps every resolved
 * cross-reference as sorted TSV.
 *
 * <p>This is the ground truth for the fork's Layer-1 resolution spec. Upstream
 * decides; where a "sensible" rule and this dump disagree, this dump wins.
 *
 * <p>It runs in its OWN JVM by necessity, not by preference — see the scaffold
 * pom's header for why the two pipelines cannot share a classpath.
 *
 * <h2>Output contract</h2>
 * One record per cross-reference, tab-separated, sorted, LF-terminated:
 * <pre>
 * XREF &lt;file&gt; &lt;offset&gt; &lt;length&gt; &lt;srcEClass&gt; &lt;EClass.feature&gt; &lt;sourceText&gt; &lt;targetEClass&gt; &lt;targetFqn&gt;
 * </pre>
 * An unresolved reference emits {@code UNRESOLVED} in both target columns.
 * Resource-level problems emit {@code LINKERR <file> <line> <message>} records
 * (validator errors {@code VALIDERR <file> <line> <message>}), which are part
 * of the contract: upstream tolerating a dangling reference is itself a fact
 * the fork must reproduce, and the line column lets the conformance gate
 * compare per-file sorted line multisets rather than bare counts (SF-4).
 *
 * <p>{@code offset} is the offset of the CROSS-REFERENCE's own node, not its
 * container's — so each link in {@code a -> b -> c} gets a distinct key and the
 * fork's dump can be paired against this one positionally.
 *
 * <h2>Usage</h2>
 * <pre>
 * java -jar rune-xtext-resolution-oracle.jar \
 *      --builtins &lt;dir of annotations.rosetta + basictypes.rosetta&gt; \
 *      [--context &lt;dir&gt;]...   # loaded for resolution, NOT dumped
 *      [--out &lt;file&gt;]         # default stdout
 *      &lt;file.rosetta&gt;...      # loaded AND dumped
 * </pre>
 */
public final class RosettaResolutionOracle {

    private RosettaResolutionOracle() {}

    /**
     * The error upstream MUST report on the positive control. If it does not,
     * the validation leg is dead and every "upstream is silent" reading taken
     * from this tool is worthless — see {@link #runPositiveControl}.
     */
    private static final String CONTROL_EXPECTED_ERROR =
            "Expected type `boolean`, but got `string`";

    public static void main(String[] args) throws Exception {
        Path builtins = null;
        Path out = null;
        Path control = null;
        Path issuesOut = null;
        List<Path> context = new ArrayList<>();
        List<Path> subjects = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--builtins" -> builtins = Paths.get(args[++i]);
                case "--context"  -> context.add(Paths.get(args[++i]));
                case "--out"      -> out = Paths.get(args[++i]);
                case "--control"  -> control = Paths.get(args[++i]);
                case "--issues"   -> issuesOut = Paths.get(args[++i]);
                default           -> subjects.add(Paths.get(args[i]));
            }
        }
        if (subjects.isEmpty()) {
            System.err.println("usage: --builtins <dir> [--context <dir>]... "
                    + "[--control <file>] [--out <file>] [--issues <file>] <file.rosetta>...");
            System.exit(2);
        }

        // Xtext logs its own progress to stdout in places; keep the dump clean
        // by buffering records and writing them in one deterministic pass.
        List<String> records = new ArrayList<>();
        // v3.2 seat 8 round 2: with --issues, every validation issue of EVERY severity with
        // its anchor (ISSUE <file> <line> <column> <offset> <length> <severity> <message>;
        // line and column 1-based; offset a 0-based CHARACTER index and length a character
        // count, Xtext's own units - the fork's SourceRange offsets are UTF-8 BYTES, the two
        // coinciding on ASCII input, so a cross-tool comparison reads line and column) - a
        // separate output, so a fork diagnostic anchor can be measured against the released
        // validator's on an invalid fixture. The dump's own contract above is untouched.
        // Sorted by (file, line, column, offset) as NUMBERS (round 3: a plain string sort put
        // line 100 before line 26).
        List<String> issueRecords = new ArrayList<>();

        Injector injector = new RosettaStandaloneSetup().createInjectorAndDoEMFRegistration();
        IQualifiedNameProvider names = injector.getInstance(IQualifiedNameProvider.class);
        IResourceValidator validator = injector.getInstance(IResourceValidator.class);

        // THE INSTRUMENT PROVES IT CAN FAIL, BEFORE IT IS BELIEVED. Runs first,
        // exits non-zero on failure, and nothing downstream writes a dump.
        if (control != null) {
            runPositiveControl(injector, validator, builtins, control);
        }

        XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);

        // The builtin models (annotations.rosetta, basictypes.rosetta) must be
        // in the resource set or `string`, `boolean` and every [metadata ...]
        // annotation resolve to nothing and the dump is meaningless.
        if (builtins != null) {
            for (Path p : rosettaFilesUnder(builtins)) {
                load(resourceSet, p);
            }
        }
        for (Path dir : context) {
            for (Path p : rosettaFilesUnder(dir)) {
                load(resourceSet, p);
            }
        }
        Set<Resource> dumped = new LinkedHashSet<>();
        for (Path p : subjects) {
            for (Path f : Files.isDirectory(p) ? rosettaFilesUnder(p) : List.of(p)) {
                dumped.add(load(resourceSet, f));
            }
        }

        EcoreUtil.resolveAll(resourceSet);

        for (Resource resource : dumped) {
            String file = resource.getURI().lastSegment();
            if (resource instanceof XtextResource xtextResource) {
                for (var error : xtextResource.getErrors()) {
                    // The LINE rides as its own column (SF-4): per-file error
                    // COUNTS tie when one side misses an error and invents
                    // another, so the conformance gate compares sorted line
                    // multisets — still wording-free, but a cross-line
                    // coincidence no longer reads as agreement.
                    records.add("LINKERR\t" + file + "\t" + error.getLine()
                            + "\t" + oneLine(error.getMessage()));
                }
                // VALIDATION IS A SEPARATE PASS AND MUST BE ASKED FOR.
                // XtextResource.getErrors() carries syntax and linking problems
                // ONLY; every type check upstream performs lives in its
                // validator and fires nowhere near here. Reporting just the
                // former and comparing it against the fork's validation errors
                // is an apples-to-oranges gate that reads as upstream staying
                // silent when it was never asked the question.
                try {
                    var issues = validator.validate(
                            xtextResource, CheckMode.ALL, CancelIndicator.NullImpl);
                    System.err.println("VALIDATE " + file + " -> " + issues.size() + " issue(s)");
                    for (var issue : issues) {
                        System.err.println("   " + issue.getSeverity() + " " + issue.getMessage());
                        if (issuesOut != null) {
                            issueRecords.add("ISSUE\t" + file + "\t" + issue.getLineNumber()
                                    + "\t" + issue.getColumn() + "\t" + issue.getOffset()
                                    + "\t" + issue.getLength() + "\t" + issue.getSeverity()
                                    + "\t" + oneLine(issue.getMessage()));
                        }
                        if (issue.getSeverity() == Severity.ERROR) {
                            records.add("VALIDERR\t" + file + "\t" + issue.getLineNumber()
                                    + "\t" + oneLine(issue.getMessage()));
                        }
                    }
                } catch (Throwable t) {
                    System.err.println("VALIDATE FAILED for " + file + ": " + t);
                    t.printStackTrace();
                }
            }
            // Identity-scoped: a Choice exposes its options through BOTH the
            // inherited `attributes` containment and its own derived `options`
            // one, so getAllContents() hands the same ChoiceOption (and its
            // TypeCall children) back twice. Visiting once keeps the dump a
            // faithful record of the source rather than of EMF's traversal.
            Set<EObject> visited = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (var it = resource.getAllContents(); it.hasNext(); ) {
                EObject object = it.next();
                if (visited.add(object)) {
                    emitAll(records, file, object, names);
                }
            }
        }

        records.sort(Comparator.naturalOrder());

        PrintStream sink = out == null
                ? System.out
                : new PrintStream(Files.newOutputStream(out), false, StandardCharsets.UTF_8);
        try {
            // A pinned header makes an empty dump distinguishable from a dump
            // whose oracle silently failed to boot — the exact failure mode the
            // superseded in-process harness shipped with.
            sink.print("# rune-xtext-resolution-oracle 9.83.0 records=" + records.size() + "\n");
            for (String record : records) {
                sink.print(record);
                sink.print('\n');
            }
        } finally {
            if (out != null) {
                sink.close();
            }
        }
        if (issuesOut != null) {
            issueRecords.sort(Comparator.comparing((String r) -> r.split("\t", 3)[1])
                    .thenComparingInt(r -> Integer.parseInt(r.split("\t")[2]))
                    .thenComparingInt(r -> Integer.parseInt(r.split("\t")[3]))
                    .thenComparingInt(r -> Integer.parseInt(r.split("\t")[4]))
                    .thenComparing(Comparator.naturalOrder()));
            try (PrintStream isink = new PrintStream(Files.newOutputStream(issuesOut), false, StandardCharsets.UTF_8)) {
                isink.print("# rune-xtext-resolution-oracle 9.83.0 issues=" + issueRecords.size()
                        + " columns=file,line,column,offset,length,severity,message\n");
                for (String record : issueRecords) {
                    isink.print(record);
                    isink.print('\n');
                }
            }
        }
    }

    /**
     * Validates the positive control and KILLS THE RUN if upstream does not
     * report the error the control is built to provoke.
     *
     * <p>The validation leg once returned zero issues for every input (a shaded
     * uber-jar dropped the {@code plugin.properties} EMF needs to build its
     * diagnostic root), which is indistinguishable from a language with no
     * complaints and invalidated two recorded conclusions. A control that runs
     * only when someone remembers it would not have caught that; this one runs
     * on every dump.
     *
     * <p>It gets its OWN {@link XtextResourceSet} — the control's declarations
     * must never enter the subjects' scope and perturb what they resolve to.
     * That isolation is the reason this cannot simply be another
     * {@code --context} input.
     */
    private static void runPositiveControl(Injector injector, IResourceValidator validator,
                                           Path builtins, Path control) throws Exception {
        if (!Files.isRegularFile(control)) {
            System.err.println("FATAL: positive control not found at " + control.toAbsolutePath());
            System.err.println("       The oracle refuses to run unvalidated. Restore the file, or");
            System.err.println("       pass --control explicitly if it has moved.");
            System.exit(6);
        }

        XtextResourceSet controlSet = injector.getInstance(XtextResourceSet.class);
        if (builtins != null) {
            for (Path p : rosettaFilesUnder(builtins)) {
                load(controlSet, p);
            }
        }
        Resource controlResource = load(controlSet, control);
        EcoreUtil.resolveAll(controlSet);

        boolean sawExpectedError = false;
        int errorCount = 0;
        if (controlResource instanceof XtextResource xtextControl) {
            try {
                for (var issue : validator.validate(
                        xtextControl, CheckMode.ALL, CancelIndicator.NullImpl)) {
                    if (issue.getSeverity() != Severity.ERROR) {
                        continue;
                    }
                    errorCount++;
                    if (issue.getMessage() != null
                            && issue.getMessage().contains(CONTROL_EXPECTED_ERROR)) {
                        sawExpectedError = true;
                    }
                }
            } catch (Throwable t) {
                System.err.println("FATAL: the positive control could not be validated: " + t);
                t.printStackTrace();
                System.exit(6);
            }
        }

        if (!sawExpectedError) {
            System.err.println("FATAL: THE POSITIVE CONTROL DID NOT FIRE.");
            System.err.println("       expected an ERROR containing: " + CONTROL_EXPECTED_ERROR);
            System.err.println("       got " + errorCount + " error(s) from " + control.getFileName());
            System.err.println();
            System.err.println("       Upstream's validator is not answering, so this run cannot tell");
            System.err.println("       'upstream reports nothing' from 'upstream was never asked'.");
            System.err.println("       NO DUMP HAS BEEN WRITTEN. Do not commit anything from this run.");
            System.err.println("       Check the dependency copy in target/lib (a SHADED jar breaks");
            System.err.println("       EMF's diagnostic root and reproduces exactly this symptom).");
            System.exit(6);
        }
        System.err.println("CONTROL ok — upstream's validator answers ("
                + control.getFileName() + ": " + errorCount + " error(s), expected one present)");
    }

    private static Resource load(XtextResourceSet resourceSet, Path file) throws Exception {
        Resource resource = resourceSet.createResource(
                URI.createFileURI(file.toAbsolutePath().normalize().toString()));
        resource.load(resourceSet.getLoadOptions());
        return resource;
    }

    private static List<Path> rosettaFilesUnder(Path dir) throws Exception {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(p -> p.toString().endsWith(".rosetta")).sorted().toList();
        }
    }

    /** Emits one record per cross-reference held by {@code source}. */
    private static void emitAll(List<String> records, String file, EObject source,
                                IQualifiedNameProvider names) {
        for (EReference reference : source.eClass().getEAllReferences()) {
            if (reference.isContainment() || reference.isContainer() || reference.isDerived()) {
                continue;
            }
            Object value = source.eGet(reference, false);
            if (value instanceof EObject target) {
                emit(records, file, source, reference, target, 0, names);
            } else if (value instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) instanceof EObject target) {
                        emit(records, file, source, reference, target, i, names);
                    }
                }
            }
        }
    }

    private static void emit(List<String> records, String file, EObject source, EReference reference,
                             EObject target, int index, IQualifiedNameProvider names) {
        // The reference's OWN node, so `a -> b -> c` yields three distinct keys.
        // A multi-valued reference pairs positionally; when the node count does
        // not match the value count (a syntax the grammar spells more than one
        // way) fall back to the container node rather than mispairing.
        List<INode> nodes = NodeModelUtils.findNodesForFeature(source, reference);
        INode node = index < nodes.size() ? nodes.get(index) : null;
        if (node == null) {
            node = NodeModelUtils.findActualNodeFor(source);
        }
        int offset = node == null ? -1 : node.getOffset();
        int length = node == null ? -1 : node.getLength();
        String text = node == null ? "?" : NodeModelUtils.getTokenText(node).trim();

        String targetEClass;
        String targetFqn;
        if (target.eIsProxy()) {
            targetEClass = "UNRESOLVED";
            targetFqn = "UNRESOLVED";
        } else {
            targetEClass = target.eClass().getName();
            QualifiedName qualifiedName = names.getFullyQualifiedName(target);
            targetFqn = qualifiedName == null ? "<anonymous>" : qualifiedName.toString();
        }

        records.add("XREF\t" + file
                + "\t" + pad(offset)
                + "\t" + length
                + "\t" + source.eClass().getName()
                + "\t" + reference.getEContainingClass().getName() + "." + reference.getName()
                + "\t" + oneLine(text)
                + "\t" + targetEClass
                + "\t" + targetFqn);
    }

    /** Zero-pads the offset so the plain string sort is also a positional sort. */
    private static String pad(int offset) {
        return offset < 0 ? "-0000001" : String.format("%08d", offset);
    }

    private static String oneLine(String s) {
        return s == null ? "" : s.replace("\r", " ").replace("\n", " ").replace("\t", " ");
    }
}
