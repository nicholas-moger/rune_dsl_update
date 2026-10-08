package com.regnosys.rosetta.ir.contract;

import com.regnosys.rosetta.ir.core.IREnum;
import com.regnosys.rosetta.ir.core.IREnumValue;
import com.regnosys.rosetta.ir.core.IRField;
import com.regnosys.rosetta.ir.core.IRFunction;
import com.regnosys.rosetta.ir.core.IRModel;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.core.IRParameter;
import com.regnosys.rosetta.ir.core.IRRule;
import com.regnosys.rosetta.ir.core.IRType;
import com.regnosys.rosetta.ir.core.Metadata;
import com.regnosys.rosetta.ir.core.MetadataKey;
import com.regnosys.rosetta.ir.rune.IRAnnotationDecl;
import com.regnosys.rosetta.ir.rune.IRRegulatoryUnit;
import com.regnosys.rosetta.ir.rune.IRReport;
import com.regnosys.rosetta.ir.rune.IRSourceDecl;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Reflection-based signature pin for the IR interface surface. Locks every
 * declared method's signature against a committed table.
 *
 * <p><b>THREE LEGS, and the table is a CLOSED set since v3.3 seat 6 (PR #642, round 1 cq SF-7; the third leg
 * round 1 cq NIT-3).</b>
 * <ol>
 *   <li>{@link #everyContractMethodMatchesSpec} — the signature leg: every row of {@link #CONTRACT}
 *       names a method that must exist with exactly the pinned generic return type. RED when a
 *       pinned method is deleted, renamed or re-typed (retype {@code IRType.isAbstract()} to
 *       {@code Boolean}, or delete {@code IREnumValue.synonyms()}, and its row goes red by name).
 *   <li>{@link #everyPinnedTypeIsPinnedWhole} — the COMPLETENESS leg: for each pinned interface,
 *       the set of public methods it DECLARES must EQUAL the set its rows pin. RED when a public
 *       method is added to a pinned interface with no row ({@code DECLARED BUT NOT PINNED}), and
 *       red the other way when a row pins a method the interface no longer declares
 *       ({@code PINNED BUT NOT DECLARED} — the same mutation also reds leg 1).
 *   <li>{@link #everyContractPackageInterfaceIsPinned} — the PACKAGE leg: every public interface the
 *       {@code ir.core} and {@code ir.rune} source directories declare has a row, so a NEW interface
 *       cannot stay invisible to the first two legs (which close only the interfaces already pinned).
 * </ol>
 * Until seat 6 the table was subset-only (the original "additive-only" reading, per spec Section 7
 * reviewer fix I3): an added public method could not fail anything, so the surface grew unwitnessed.
 * Adding a method is still ADDITIVE for implementors and BC-safe under japicmp — what changed is
 * that it must now be DECLARED here in the same commit. Removing a method remains a deliberate
 * breaking change, with the tombstone workflow below.
 *
 * <p><b>Removing a method:</b> the canonical workflow is —
 * <ol>
 *   <li>Replace the row in {@link #CONTRACT} with a {@link Sig#tombstone}
 *       entry, which inverts the assertion: the method must NOT be
 *       reachable via {@code getMethod}. The tombstone row keeps the row
 *       in source so that re-adding the same method is caught (that would
 *       be a reintroduction of formerly-removed surface, which itself
 *       deserves explicit re-approval).
 *   <li>Run a japicmp Layer-1 ADDED/REMOVED audit.
 *   <li>Land a matching D-entry per D21.
 * </ol>
 * After a release containing the tombstone, the row may be deleted from
 * {@link #CONTRACT} if there is no concern about a downstream branch
 * reintroducing the removed surface.
 *
 * <p>How many rows there are is ASSERTED, never typed: {@link #everyPinnedTypeIsPinnedWhole} holds each pinned
 * interface's declared public surface EQUAL to its rows, so the count is read off {@link #CONTRACT} and off the
 * interfaces at run time and a number written in prose here could only rot. The rows of v3.3 seat 5 (PR #641,
 * decision D55 - the declaration-IR enrichment) are DEFAULT methods: additive for every implementation, and contract
 * surface all the same - a backend reads them, so a dropped or re-typed one is a break.
 *
 * <p>Return-type strings are produced by
 * {@link java.lang.reflect.Type#getTypeName()}. For parameterised types
 * this includes the type-argument names verbatim (e.g.
 * {@code java.util.Optional<com.regnosys.rosetta.ir.core.IRType>}); for
 * arrays {@code Foo[]}; for nested types {@code Outer$Inner}. If a future
 * contract row uses an intersection type or a more exotic shape, write a
 * tiny throwaway test to print {@code getTypeName()} for it before
 * committing the pinned string.
 */
class IRContractSurfaceTest {

    /**
     * Contract row. {@link #returnType} is ignored when {@link #tombstone}
     * is true (use {@link #tombstone(Class, String, Class[])} for tombstone
     * rows so the row reads as intent-revealing rather than carrying a
     * vestigial return-type string).
     */
    private record Sig(Class<?> iface, String method, String returnType,
                       boolean tombstone, Class<?>... paramTypes) {

        static Sig of(Class<?> iface, String method, String returnType, Class<?>... params) {
            return new Sig(iface, method, returnType, false, params);
        }

        /**
         * Tombstone row — asserts the method is NOT present on the
         * interface. Used for deliberate removals: the row stays in source
         * after the method has been removed, so a future PR that
         * reintroduces the same method (perhaps without realising it was
         * formerly removed) is caught.
         */
        static Sig tombstone(Class<?> iface, String method, Class<?>... params) {
            return new Sig(iface, method, "(tombstoned)", true, params);
        }
    }

    /**
     * The pinned surface. A row pins a method the named interface DECLARES ITSELF — never one it
     * inherits (the completeness leg compares against {@code getDeclaredMethods}, so a row pinning
     * an inherited method reads {@code PINNED BUT NOT DECLARED}). No count is written here: the
     * completeness leg asserts each group is whole, per {@link #everyPinnedTypeIsPinnedWhole}.
     */
    private static final List<Sig> CONTRACT = List.of(
            // IRNode
            Sig.of(IRNode.class, "name", "java.lang.String"),
            Sig.of(IRNode.class, "kind", "com.regnosys.rosetta.ir.core.IRKind"),
            Sig.of(IRNode.class, "sourceRange", "java.util.Optional<com.regnosys.rosetta.ir.core.SourceRange>"),
            Sig.of(IRNode.class, "children", "java.util.List<? extends com.regnosys.rosetta.ir.core.IRNode>"),
            Sig.of(IRNode.class, "metadata", "com.regnosys.rosetta.ir.core.Metadata"),
            // IRType
            Sig.of(IRType.class, "fields", "java.util.List<com.regnosys.rosetta.ir.core.IRField>"),
            Sig.of(IRType.class, "baseType", "java.util.Optional<com.regnosys.rosetta.ir.core.IRType>"),
            Sig.of(IRType.class, "isAbstract", "boolean"),
            // IRType - the declaration facts (default methods, PR #641)
            Sig.of(IRType.class, "namespace", "java.util.Optional<java.lang.String>"),
            Sig.of(IRType.class, "resolvedName", "java.util.Optional<java.lang.String>"),
            Sig.of(IRType.class, "resolvedQualifiedName", "java.util.Optional<java.lang.String>"),
            Sig.of(IRType.class, "definition", "java.util.Optional<java.lang.String>"),
            Sig.of(IRType.class, "docReferences", "java.util.List<com.regnosys.rosetta.ir.core.IRDocReference>"),
            Sig.of(IRType.class, "annotations", "java.util.List<com.regnosys.rosetta.ir.core.IRAnnotationUse>"),
            Sig.of(IRType.class, "conditionNames", "java.util.List<java.util.Optional<java.lang.String>>"),
            // IRType - the type gate's facts (default methods, PR #643): the collapsed alias chain on a TYPE_ALIAS
            // reference; a TYPE_ALIAS declaration's parameters and the arguments its body writes
            Sig.of(IRType.class, "effectiveBase", "java.util.Optional<com.regnosys.rosetta.ir.core.IREffectiveBase>"),
            Sig.of(IRType.class, "typeParameters", "java.util.List<com.regnosys.rosetta.ir.core.IRTypeParameter>"),
            Sig.of(IRType.class, "baseTypeArguments", "java.util.List<com.regnosys.rosetta.ir.core.IRTypeArgument>"),
            // IRType - the property gate's facts (default methods, PR #644): each condition's KIND token,
            // index-parallel to conditionNames(); and, on a TYPE_ALIAS reference, the chain's RUNGS
            Sig.of(IRType.class, "conditionKinds", "java.util.List<java.lang.String>"),
            Sig.of(IRType.class, "aliasChain", "java.util.List<com.regnosys.rosetta.ir.core.IRAliasLink>"),
            // IRModel (inherits IRNode) - the model-level node of the property gate (PR #644). Its six
            // fact accessors are default methods, additive for every implementation and contract all the
            // same: the data-type emitter's derived files read them.
            Sig.of(IRModel.class, "namespace", "java.lang.String"),
            Sig.of(IRModel.class, "definition", "java.util.Optional<java.lang.String>"),
            Sig.of(IRModel.class, "version", "java.util.Optional<java.lang.String>"),
            Sig.of(IRModel.class, "qualifiableConfigs", "java.util.List<com.regnosys.rosetta.ir.core.IRQualifiableConfig>"),
            Sig.of(IRModel.class, "qualificationFunctions", "java.util.List<com.regnosys.rosetta.ir.core.IRQualificationFunction>"),
            Sig.of(IRModel.class, "functionSignatures", "java.util.List<com.regnosys.rosetta.ir.core.IRFunctionSignature>"),
            Sig.of(IRModel.class, "withMetaUses", "java.util.List<com.regnosys.rosetta.ir.core.IRWithMetaUse>"),
            // IRField (inherits IRNode)
            Sig.of(IRField.class, "type", "com.regnosys.rosetta.ir.core.IRType"),
            Sig.of(IRField.class, "cardinality", "com.regnosys.rosetta.ir.core.Cardinality"),
            // IRField - the declaration facts (default methods, PR #641)
            Sig.of(IRField.class, "bounds", "java.util.Optional<com.regnosys.rosetta.ir.core.IRBounds>"),
            Sig.of(IRField.class, "isOverride", "boolean"),
            Sig.of(IRField.class, "typeArguments", "java.util.List<com.regnosys.rosetta.ir.core.IRTypeArgument>"),
            Sig.of(IRField.class, "definition", "java.util.Optional<java.lang.String>"),
            Sig.of(IRField.class, "docReferences", "java.util.List<com.regnosys.rosetta.ir.core.IRDocReference>"),
            Sig.of(IRField.class, "annotations", "java.util.List<com.regnosys.rosetta.ir.core.IRAnnotationUse>"),
            Sig.of(IRField.class, "labels", "java.util.List<com.regnosys.rosetta.ir.core.IRLabel>"),
            Sig.of(IRField.class, "ruleReferences", "java.util.List<com.regnosys.rosetta.ir.core.IRRuleReference>"),
            // IREnum (inherits IRNode)
            Sig.of(IREnum.class, "values", "java.util.List<com.regnosys.rosetta.ir.core.IREnumValue>"),
            // IREnum - the declaration facts (default methods, PR #641)
            Sig.of(IREnum.class, "parent", "java.util.Optional<com.regnosys.rosetta.ir.core.IRType>"),
            Sig.of(IREnum.class, "namespace", "java.util.Optional<java.lang.String>"),
            Sig.of(IREnum.class, "definition", "java.util.Optional<java.lang.String>"),
            Sig.of(IREnum.class, "docReferences", "java.util.List<com.regnosys.rosetta.ir.core.IRDocReference>"),
            Sig.of(IREnum.class, "annotations", "java.util.List<com.regnosys.rosetta.ir.core.IRAnnotationUse>"),
            // IREnumValue (inherits IRNode)
            Sig.of(IREnumValue.class, "displayName", "java.util.Optional<java.lang.String>"),
            // IREnumValue - the declaration facts (default methods, PR #641)
            Sig.of(IREnumValue.class, "definition", "java.util.Optional<java.lang.String>"),
            Sig.of(IREnumValue.class, "docReferences", "java.util.List<com.regnosys.rosetta.ir.core.IRDocReference>"),
            Sig.of(IREnumValue.class, "annotations", "java.util.List<com.regnosys.rosetta.ir.core.IRAnnotationUse>"),
            Sig.of(IREnumValue.class, "synonyms", "java.util.List<com.regnosys.rosetta.ir.core.IREnumSynonym>"),
            // IRFunction (inherits IRNode)
            Sig.of(IRFunction.class, "parameters", "java.util.List<com.regnosys.rosetta.ir.core.IRParameter>"),
            Sig.of(IRFunction.class, "returnType", "java.util.Optional<com.regnosys.rosetta.ir.core.IRType>"),
            Sig.of(IRFunction.class, "isLibrary", "boolean"),
            Sig.of(IRFunction.class, "hasBody", "boolean"),
            // IRParameter (inherits IRNode)
            Sig.of(IRParameter.class, "type", "com.regnosys.rosetta.ir.core.IRType"),
            Sig.of(IRParameter.class, "cardinality", "com.regnosys.rosetta.ir.core.Cardinality"),
            // IRRule (inherits IRNode)
            Sig.of(IRRule.class, "inputType", "java.util.Optional<com.regnosys.rosetta.ir.core.IRType>"),
            Sig.of(IRRule.class, "outputType", "java.util.Optional<com.regnosys.rosetta.ir.core.IRType>"),
            Sig.of(IRRule.class, "hasExpression", "boolean"),
            // IRReport (inherits IRNode)
            Sig.of(IRReport.class, "regulatoryUnit", "java.util.Optional<com.regnosys.rosetta.ir.rune.IRRegulatoryUnit>"),
            Sig.of(IRReport.class, "rules", "java.util.List<com.regnosys.rosetta.ir.core.IRRule>"),
            Sig.of(IRReport.class, "outputType", "java.util.Optional<com.regnosys.rosetta.ir.core.IRType>"),
            // IRRegulatoryUnit (inherits IRNode)
            Sig.of(IRRegulatoryUnit.class, "unitKind", "com.regnosys.rosetta.ir.rune.IRRegulatoryUnit$UnitKind"),
            Sig.of(IRRegulatoryUnit.class, "parent", "java.util.Optional<com.regnosys.rosetta.ir.rune.IRRegulatoryUnit>"),
            // IRAnnotationDecl (inherits IRNode)
            Sig.of(IRAnnotationDecl.class, "parameters", "java.util.List<com.regnosys.rosetta.ir.core.IRParameter>"),
            // IRSourceDecl (inherits IRNode)
            Sig.of(IRSourceDecl.class, "sourceKind", "com.regnosys.rosetta.ir.rune.IRSourceDecl$SourceKind"),
            Sig.of(IRSourceDecl.class, "parent", "java.util.Optional<com.regnosys.rosetta.ir.rune.IRSourceDecl>"),
            // Metadata + MetadataKey (v3.3 seat 6, PR #642 round 1 cq SF-7): the metadata bag was UNPINNED
            // surface until the completeness leg landed — it is reached straight off the pinned
            // IRNode#metadata(), a backend reads it, so a dropped or re-typed accessor there is a break on
            // exactly the same footing as one on IRType. A type variable prints as its bare name.
            Sig.of(Metadata.class, "get", "java.util.Optional<java.lang.Object>", String.class),
            Sig.of(Metadata.class, "keys", "java.util.Set<java.lang.String>"),
            Sig.of(Metadata.class, "getAs", "java.util.Optional<T>", MetadataKey.class),
            Sig.of(MetadataKey.class, "name", "java.lang.String"),
            Sig.of(MetadataKey.class, "type", "java.lang.Class<T>")
    );

    @TestFactory
    Stream<DynamicTest> everyContractMethodMatchesSpec() {
        return CONTRACT.stream().map(sig -> DynamicTest.dynamicTest(
                (sig.tombstone() ? "[tombstone] " : "") +
                        sig.iface().getSimpleName() + "." + sig.method() +
                        (sig.tombstone() ? "" : " : " + sig.returnType()),
                () -> {
                    if (sig.tombstone()) {
                        assertThrows(NoSuchMethodException.class,
                                () -> sig.iface().getMethod(sig.method(), sig.paramTypes()),
                                sig.iface().getName() + "#" + sig.method() +
                                        " is tombstoned (deliberately removed) but is reachable " +
                                        "again — a reintroduction of formerly-removed surface " +
                                        "needs explicit re-approval (D-entry per D21).");
                        return;
                    }
                    Method m;
                    try {
                        m = sig.iface().getMethod(sig.method(), sig.paramTypes());
                    } catch (NoSuchMethodException e) {
                        throw new AssertionError(
                                sig.iface().getName() + "#" + sig.method() + " missing — " +
                                        "either restore the method or replace the contract row " +
                                        "with a Sig.tombstone(...) entry (removal requires japicmp " +
                                        "Layer-1 audit + D-entry per D21)",
                                e);
                    }
                    assertEquals(sig.returnType(), m.getGenericReturnType().getTypeName(),
                            sig.iface().getName() + "#" + sig.method() +
                                    " return type drift — either restore the prior signature " +
                                    "or update the contract row (signature change requires " +
                                    "japicmp Layer-1 audit + D-entry per D21)");
                }));
    }

    /**
     * THE COMPLETENESS LEG (v3.3 seat 6, PR #642 — round 1 cq SF-7). {@link #everyContractMethodMatchesSpec}
     * is subset-only: it can only fail on a method somebody already pinned, so an UNPINNED public method
     * on an IR interface was surface no test could see appear, change or go. This leg closes the set —
     * per pinned interface, the public methods it DECLARES ITSELF must EQUAL the methods its rows pin.
     *
     * <p>Own methods only ({@code getDeclaredMethods}, so nothing inherited from a supertype or from
     * {@code Object}); {@code default} bodies included (they are read by every backend and are contract);
     * synthetic and bridge methods excluded (compiler output, not surface); non-public members excluded
     * (a {@code private} interface helper is an implementation detail).
     *
     * <p><b>What makes it red:</b> add {@code IRType#anything()} without a {@link #CONTRACT} row and this
     * leg fails on {@code IRType} naming {@code anything()} as {@code DECLARED BUT NOT PINNED}; delete
     * {@code IREnum#parent()} and it fails naming it {@code PINNED BUT NOT DECLARED} (and the signature
     * leg fails on the same row — a removal is meant to be loud twice). The probe that enumerated the
     * surface this leg now closes is {@code target/v33-seat6-instruments/scratch/bank-B-contract-probe.md}.
     */
    @TestFactory
    Stream<DynamicTest> everyPinnedTypeIsPinnedWhole() {
        return CONTRACT.stream().map(Sig::iface).distinct().map(iface -> DynamicTest.dynamicTest(
                "whole: " + iface.getSimpleName(),
                () -> {
                    Set<String> declared = new TreeSet<>();
                    for (Method m : iface.getDeclaredMethods()) {
                        if (m.isSynthetic() || m.isBridge() || !Modifier.isPublic(m.getModifiers())) {
                            continue;
                        }
                        declared.add(signature(m.getName(), m.getParameterTypes()));
                    }
                    Set<String> pinned = new TreeSet<>();
                    for (Sig sig : CONTRACT) {
                        // a tombstone row pins the ABSENCE of a method, so it is not part of the expected set
                        if (sig.iface().equals(iface) && !sig.tombstone()) {
                            pinned.add(signature(sig.method(), sig.paramTypes()));
                        }
                    }
                    TreeSet<String> unpinned = new TreeSet<>(declared);
                    unpinned.removeAll(pinned);
                    TreeSet<String> undeclared = new TreeSet<>(pinned);
                    undeclared.removeAll(declared);
                    if (!unpinned.isEmpty() || !undeclared.isEmpty()) {
                        throw new AssertionError(iface.getName() + " contract surface is not pinned whole — "
                                + (unpinned.isEmpty() ? "" : "DECLARED BUT NOT PINNED " + unpinned
                                        + ": add a Sig.of(...) row in the commit that adds the method (an unpinned"
                                        + " public method is surface no test can see change). ")
                                + (undeclared.isEmpty() ? "" : "PINNED BUT NOT DECLARED " + undeclared
                                        + ": the interface no longer declares it — restore it, or replace its row"
                                        + " with a Sig.tombstone(...) entry (removal requires japicmp Layer-1 audit"
                                        + " + D-entry per D21); a row pinning an INHERITED method reads the same way"
                                        + " and belongs on the interface that declares it."));
                    }
                }));
    }

    /**
     * THE PACKAGE LEG (v3.3 seat 6, PR #642 — round 1 cq NIT-3): the completeness leg closes each PINNED interface,
     * so a NEW public interface with no row was invisible to both legs — exactly how {@link Metadata} and
     * {@link MetadataKey} stayed unpinned until this seat's probe. This leg lists the source files of the two
     * contract packages ({@code ir.core}, {@code ir.rune}) from the module's own tree, loads each top-level type by
     * name (no parse of the source, no regex — the file name IS the class name) and holds: every public interface
     * of those packages has at least one non-tombstone row in {@link #CONTRACT}. Records, enums and classes are not
     * interfaces and are not pinned here (a record's components are gated by name and value by the reconcile;
     * {@code target/v33-seat6-instruments/scratch/bank-B-contract-probe.md} § 3 states that decision).
     *
     * <p><b>What makes it red:</b> add {@code public interface IRSomething} under {@code ir.core} with no row and this
     * leg fails naming it; move the source tree so the directory is not found and it fails loudly (a leg that finds
     * no file is not a gate).
     */
    @TestFactory
    Stream<DynamicTest> everyContractPackageInterfaceIsPinned() throws java.io.IOException {
        java.nio.file.Path root = java.nio.file.Path.of("src", "main", "java", "com", "regnosys", "rosetta", "ir");
        if (!java.nio.file.Files.isDirectory(root)) {
            root = java.nio.file.Path.of("rune-ir").resolve(root);   // the surefire cwd is the module; a reactor cwd is the tree
        }
        Set<Class<?>> pinned = CONTRACT.stream().filter(sig -> !sig.tombstone()).map(Sig::iface).collect(Collectors.toSet());
        List<DynamicTest> tests = new java.util.ArrayList<>();
        for (String pkg : List.of("core", "rune")) {
            java.nio.file.Path dir = root.resolve(pkg);
            if (!java.nio.file.Files.isDirectory(dir)) {
                throw new AssertionError("the contract package directory was not found: " + dir.toAbsolutePath());
            }
            List<String> names;
            try (Stream<java.nio.file.Path> files = java.nio.file.Files.list(dir)) {
                names = files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".java") && !n.equals("package-info.java"))   // a package-info is no type (PR #643, the #642 round-2 NIT-3)
                        .map(n -> n.substring(0, n.length() - ".java".length())).sorted().toList();
            }
            if (names.isEmpty()) {
                throw new AssertionError("no source file under " + dir.toAbsolutePath() + " - a leg that finds nothing is not a gate");
            }
            for (String name : names) {
                tests.add(DynamicTest.dynamicTest("package " + pkg + ": " + name, () -> {
                    Class<?> type = Class.forName("com.regnosys.rosetta.ir." + pkg + "." + name);
                    if (type.isInterface() && Modifier.isPublic(type.getModifiers()) && !pinned.contains(type)) {
                        throw new AssertionError(type.getName() + " is a public interface of a contract package with NO row in CONTRACT"
                                + " - pin every public method in the commit that adds the interface");
                    }
                }));
            }
        }
        return tests.stream();
    }

    /** {@code method(param.type.Name,…)} — the erasure key both halves of the completeness leg are read into. */
    private static String signature(String method, Class<?>... paramTypes) {
        return method + Arrays.stream(paramTypes).map(Class::getName).collect(Collectors.joining(",", "(", ")"));
    }
}
