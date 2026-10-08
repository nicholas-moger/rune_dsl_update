package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.types.RTypeAlias;
import com.regnosys.rosetta.ir.adapter.AstToIRAdapter;
import com.regnosys.rosetta.ir.core.IREnum;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRKind;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRType;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural-coherence check for {@link AstToIRAdapter} over real corpus models
 * (a representative cell). Complements the flag-on D11 byte ring (which gates
 * byte-identity of the emitted Java at the full population; the lab's
 * {@code Path2ByteIdentityTest} predecessor was deliberately not ported — that
 * ring supersedes it): here we assert the IR the adapter builds is
 * structurally faithful to the AST — correct {@link IRKind}, qualified names,
 * field/value counts, leaf-ness, cardinality presence, and 1:1 preservation of
 * enum display names. Parse-only (no workspace build needed); skips without the
 * corpus.
 *
 * <p>This is the AST↔IR coherence relation R the maintainer spec anticipates for
 * P2 (lab decision L-004); the in-emitter reconciliation additionally exercises
 * the adapter over every cell during the byte-identity run.
 *
 * <p><b>Geometry (the IR-train PR-4 retarget):</b> ported from the lab at its
 * {@code ../../phase1-bundle} staging (the {@code ir.lab.bundle} property override
 * dropped with it); now resolves THIS repo's {@code test-corpus/} cell layout,
 * module-relative like the D11 gate's {@code ../test-corpus} convention.
 */
class IRAdapterCoherenceTest {

    private static final Path SAMPLE_DIR =
            Path.of("../test-corpus/cdm/cdm-5.38.0/rosetta-source/src/main/rosetta");

    private final AstToIRAdapter adapter = new AstToIRAdapter();

    @Test
    void adapterIsStructurallyCoherentOverRealModels() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(SAMPLE_DIR),
                "test-corpus sample sources absent (" + SAMPLE_DIR.toAbsolutePath() + ") — skipped");

        List<Path> files;
        try (Stream<Path> s = Files.walk(SAMPLE_DIR)) {
            files = s.filter(p -> p.toString().endsWith(".rosetta")).sorted().toList();
        }
        Assumptions.assumeFalse(files.isEmpty(), "no sample .rosetta files");

        List<String> problems = new ArrayList<>();
        int structs = 0;
        int enums = 0;
        int choices = 0;
        int aliases = 0;

        for (Path file : files) {
            RModel model = AstBuilder.buildFromFile(file);
            String ns = model.namespace();
            for (RRootElement element : model.rootElements()) {
                Optional<IRNode> node = adapter.adaptRootElement(ns, element);
                if (element instanceof RDataType dataType) {
                    structs++;
                    IRType ir = asType(node, problems, "data " + dataType.name());
                    if (ir == null) {
                        continue;
                    }
                    check(problems, ir.kind() == IRKind.STRUCT, "STRUCT kind for " + dataType.name());
                    check(problems, qn(ns, dataType.name()).equals(ir.name()),
                            "qualified name for " + dataType.name() + " got " + ir.name());
                    check(problems, ir.fields().size() == dataType.attributes().size(),
                            "field count for " + dataType.name());
                    check(problems, ir.children().size() == ir.fields().size(),
                            "children==fields for " + dataType.name());
                    for (IRField field : ir.fields()) {
                        check(problems, field.kind() == IRKind.FIELD, "field kind in " + dataType.name());
                        check(problems, field.cardinality() != null, "field cardinality in " + dataType.name());
                        check(problems, field.type() != null, "field type in " + dataType.name());
                        check(problems, field.children().isEmpty(), "field is leaf in " + dataType.name());
                    }
                } else if (element instanceof REnumeration enumeration) {
                    enums++;
                    IREnum ir = asEnum(node, problems, "enum " + enumeration.name());
                    if (ir == null) {
                        continue;
                    }
                    check(problems, ir.kind() == IRKind.ENUM, "ENUM kind for " + enumeration.name());
                    check(problems, ir.values().size() == enumeration.values().size(),
                            "value count for " + enumeration.name());
                    int n = Math.min(ir.values().size(), enumeration.values().size());
                    for (int i = 0; i < n; i++) {
                        IREnumValue v = ir.values().get(i);
                        REnumValue av = enumeration.values().get(i);
                        check(problems, v.kind() == IRKind.ENUM_VALUE, "value kind in " + enumeration.name());
                        check(problems, v.name().equals(av.name()), "value name " + av.name());
                        check(problems, v.displayName().equals(av.displayName()),
                                "displayName preserved for " + enumeration.name() + "." + av.name());
                    }
                } else if (element instanceof RChoice choice) {
                    choices++;
                    IRType ir = asType(node, problems, "choice " + choice.name());
                    if (ir == null) {
                        continue;
                    }
                    check(problems, ir.kind() == IRKind.CHOICE, "CHOICE kind for " + choice.name());
                    check(problems, ir.fields().size() == choice.options().size(),
                            "option count for " + choice.name());
                } else if (element instanceof RTypeAlias alias) {
                    // v3.3 seat 7 (PR #643, the type gate): an alias is adapted too — a declaration with no
                    // field, whose facts are its body reference, that body's arguments and its parameters.
                    aliases++;
                    IRType ir = asType(node, problems, "typeAlias " + alias.name());
                    if (ir == null) {
                        continue;
                    }
                    check(problems, ir.kind() == IRKind.TYPE_ALIAS, "TYPE_ALIAS kind for " + alias.name());
                    check(problems, qn(ns, alias.name()).equals(ir.name()),
                            "qualified name for " + alias.name() + " got " + ir.name());
                    check(problems, ir.fields().isEmpty(), "an alias declares no field: " + alias.name());
                    check(problems, ir.baseType().isPresent() == (alias.typeCall() != null),
                            "base type presence for " + alias.name());
                    check(problems, ir.typeParameters().size() == alias.typeParameters().size(),
                            "type parameter count for " + alias.name());
                    int bodyArguments = alias.typeCall() == null ? 0 : alias.typeCall().arguments().size();
                    check(problems, ir.baseTypeArguments().size() == bodyArguments,
                            "base type argument count for " + alias.name());
                } else {
                    // Kinds not yet handled in Phase 1 are extension points — must return empty.
                    check(problems, node.isEmpty(),
                            "unsupported root kind should map to empty: " + element.getClass().getSimpleName());
                }
            }
        }

        final int s = structs;
        final int e = enums;
        final int c = choices;
        final int a = aliases;
        assertTrue(problems.isEmpty(), () -> "IR adapter coherence problems (" + problems.size() + "):\n"
                + problems.stream().limit(25).collect(Collectors.joining("\n")));
        assertTrue(structs + enums > 0, "expected the sample cell to contain data types and/or enums");
        System.out.println("IRAdapterCoherence cdm/5.38.0: structs=" + s + " enums=" + e
                + " choices=" + c + " typeAliases=" + a + " — all coherent");
    }

    private static IRType asType(Optional<IRNode> node, List<String> problems, String what) {
        IRNode n = node.orElse(null);
        if (n instanceof IRType t) {
            return t;
        }
        problems.add("expected IRType for " + what + " but got " + (n == null ? "empty" : n.getClass().getSimpleName()));
        return null;
    }

    private static IREnum asEnum(Optional<IRNode> node, List<String> problems, String what) {
        IRNode n = node.orElse(null);
        if (n instanceof IREnum en) {
            return en;
        }
        problems.add("expected IREnum for " + what + " but got " + (n == null ? "empty" : n.getClass().getSimpleName()));
        return null;
    }

    private static void check(List<String> problems, boolean condition, String description) {
        if (!condition) {
            problems.add(description);
        }
    }

    private static String qn(String namespace, String simpleName) {
        return namespace == null || namespace.isEmpty() ? simpleName : namespace + "." + simpleName;
    }
}
