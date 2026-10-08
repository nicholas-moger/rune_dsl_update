package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.JavaNamingUtil;
import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.template.ImportCollector;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.IRKind;
import com.rosetta.util.DottedPath;

/**
 * THE DATA-TYPE POJO EMITTER (v3.3 seat 9, PR #645 commit 4 - the POJO member of {@link IRTypeUnit}, UNDER
 * CONSTRUCTION): a {@code type}'s POJO file rendered from {@link IRTypeNode} + {@link IRTypeIndex} +
 * {@link IRPropertyModel} + this emitter's own {@link Config} ALONE. It reads NO AST node, no {@code GeneratorModel},
 * no {@code JavaTypeTranslator} and no {@code RJavaPojoInterface} - the shape {@link IREnumEmitter} set at PR #642,
 * generalised to the type kind.
 *
 * <p><b>THE ORACLE</b> is {@code ModelObjectGenerator} ({@code rune-java-generator/.../java/object/ModelObjectGenerator.java}),
 * section by section, at the file:line sites the seat's contract mapped:
 * <ul>
 *   <li><b>1 HEADER</b> - {@code buildModel} {@code :150-353} (the import set) + {@code :362-365}
 *       ({@code ImportCollisionResolver.resolveClass}) + the {@code .stg} {@code pojoFile}. RENDERED.</li>
 *   <li><b>2 TYPE_JAVADOC</b> - {@code :511-524} -&gt; {@code ModelGeneratorUtil.javadoc(definition, docRefs, version)},
 *       the POJO style (the Xtend continuation prefix, the {@code for} path line). RENDERED.</li>
 *   <li><b>3 ANNOTATIONS</b> - {@code :527-542}. RENDERED.</li>
 *   <li><b>4 INTERFACE_DECLARATION</b> - {@code :544-566}, the interface list
 *       {@code RJavaPojoInterface.getInterfaceDeclarations} {@code :178-195}. RENDERED.</li>
 *   <li><b>5 META_DATA</b> - {@code :569-571}, the class {@code JavaTypeTranslator.toJavaMetaDataClass}
 *       {@code :240-248}. RENDERED.</li>
 *   <li><b>6 GETTERS</b> - {@code :576-579} -&gt; {@code generateInterfaceGetter} {@code :638-655}. RENDERED, with
 *       ONE named gap (below).</li>
 *   <li><b>7 BUILD_METHODS</b> - {@code :613-625} ({@code build()} / {@code toBuilder()} / {@code builder()}).
 *       RENDERED (v3.3 seat 9, PR #645 commit 6): a pure text law of the simple name.</li>
 *   <li><b>8 UTILITY_METHODS</b> - {@code :627-640} ({@code metaData()} and the {@code @RuneAttribute("@type")}
 *       {@code getType()}). RENDERED (commit 6): the simple name and the two library tokens.</li>
 *   <li><b>9 PROCESS</b> - {@code :668-669} -&gt; {@code ModelObjectBoilerplate.processMethod} {@code :64-96} (the
 *       meta flag {@code :270-275}). RENDERED (v3.3 seat 9, PR #645 commit 7): one line per property of the whole
 *       surface, the {@code processRosetta} / {@code processBasic} split decided by the item-KIND fact.</li>
 *   <li><b>10 BUILDER_INTERFACE</b> - {@code :672} -&gt; {@code generateBuilderInterface} {@code :728-836}.
 *       RENDERED (commit 7): the declaration, the {@code getOrCreate} pairs, the GENERATION-MAJOR setter
 *       declarations ({@code :846-946}), the builder's own {@code process} and {@code prune()}.</li>
 *   <li><b>11 IMPL</b> - {@code :689} -&gt; {@code generateImplClass} {@code :957-1073}. RENDERED (v3.3 seat 9,
 *       PR #645 commit 8): the fields, the four-armed constructor, the annotated getters, {@code build()},
 *       {@code toBuilder()}, {@code setBuilderFields} and the re-indented section-13 text. Its ONE sub-section,
 *       the widening compat getters of a specialized chain ({@code PojoCompatEmitter:171-190}), RENDERS too
 *       (v3.3 seat 9, PR #645 commit 10) - {@link IRPojoCompat#appendImplDerivedGetters}, at the exact property
 *       and in the exact property order the old generator appends them.</li>
 *   <li><b>12 BUILDER_IMPL</b> - {@code :711} -&gt; {@code generateBuilderImplClass} {@code :1101-1417} and the
 *       setter helpers {@code :1440-1697}. RENDERED (v3.3 seat 9, PR #645 commit 9): the fields, the annotated
 *       builder getters and their {@code getOrCreate} pairs, the full setter family of every property,
 *       {@code build()}, {@code toBuilder()}, the config-aware {@code prune()} and {@code hasData()},
 *       {@code merge()} and the re-indented BUILDER variant of the section-13 text. Its TWO sub-sections - the
 *       builder-side compat getters with their {@code getOrCreate} delegates
 *       ({@link IRPojoCompat#appendBuilderDerivedGetters}) and the ancestor SETTER arms
 *       ({@link IRPojoCompat#appendAncestorSetterArms}) - RENDER at commit 10, at the two sites the refusals
 *       stood. The class-closing brace is NOT this section's: it is the file frame's, appended by
 *       {@link #renderWithSections} after the last section.</li>
 *   <li><b>13 EQUALS_HASHCODE_TOSTRING</b> - {@code ModelObjectBoilerplate:141-268}. Its POSITION in
 *       {@code buildBody} appends NOTHING (the {@code COMPAT_MEMBERS} precedent): the two boilerplates live
 *       INSIDE sections 11 and 12, so the positional section renders {@code ""} and the TEXT LAW is the method
 *       {@link #boilerplate} those sections call (v3.3 seat 9, PR #645 commit 8).</li>
 *   <li><b>14 COMPAT_MEMBERS</b> - {@code :621-623} -&gt; {@code PojoCompatEmitter}. RENDERED, and EMPTY for
 *       every type: {@code :621-623} only CONSTRUCTS the compat emitter and appends no byte of its own - every
 *       member it writes is appended later, inside sections 11 and 12.</li>
 * </ul>
 *
 * <p><b>THE TWO IR FACTS THIS EMITTER USED TO REFUSE FOR</b> (commit 4 named them as gaps and refused every carrier
 * by name; v3.3 seat 9, PR #645 commit 5 - THE GATE COMMIT - added both to the property surface, each derived from
 * the IR alone and RECONCILED per property against the old generator's own answer, so the refusals are GONE and the
 * D11 POJO SHADOW line's {@code refused} column loses their carriers):
 * <ol>
 *   <li><b>{@code property.&lt;name&gt;.itemIsRosettaModelObject}</b> - the old generator asks
 *       {@code JavaTypeUtil.isRosettaModelObject(prop.getType())} ({@code :236-238}), a SUBTYPE test on the Java
 *       type lattice, at three sites this commit renders: the {@code List&lt;? extends X&gt;} arm of the getter
 *       return type ({@code interfaceGetterType}, {@code :1917-1925}), the {@code java.util.Objects} import gate
 *       ({@code :180-184}) and the {@code java.util.function.Consumer} import gate ({@code :273-286}). The IR half
 *       is {@code IRType.kind()} on the declaration behind the ITEM (STRUCT / CHOICE true, ENUM / BASIC_TYPE /
 *       RECORD_TYPE false, an alias on its collapsed base, a meta-wrapped item on its GENERATED wrapper) - never a
 *       package-prefix test on the rendered name, which is what commit 4's decidable arms were and which this
 *       commit DELETED.</li>
 *   <li><b>{@code property.&lt;name&gt;.parentChain.types}</b> - the ancestors of a specialized property, nearest
 *       first, each with its Java type, its cardinality, its requiredness and its bare meta value type
 *       ({@link IRPropertyModel.IRParentLink}). Every compat arm {@code PojoCompatEmitter} writes is in an
 *       ANCESTOR's Java shape ({@code :140-858}, reached from {@code :621-623}), and the specialization chain's own
 *       imports are decided from the same rungs ({@code :288-330}) - which is the byte-bearing consumer AT THIS
 *       COMMIT, since sections 11 and 12 still refuse. Section 14 itself renders empty for every type, specialized
 *       or not, because {@code :621-623} appends no byte of its own.</li>
 * </ol>
 *
 * <p><b>WHAT IS SHARED</b>, said plainly (the {@link IREnumEmitter} paragraph's law): {@link ImportCollector} (the
 * sorted, same-package-and-{@code java.lang}-filtered set), {@link ImportCollisionResolver} (the file-scope
 * first-claim law of D50 - a PURE function over this emitter's own private-use-area sentinels, which is why the
 * first-claim law is reproduced by CALLING it rather than by a second copy that could drift),
 * {@link JavaPackageName#escape} and {@link DottedPath} (pure namespace spelling),
 * {@link JavaNamingUtil#toFirstUpper} (the accessor name's first letter), {@link IRJavaLangCollision} (the ONE
 * {@code java.lang} collision producer of the IR route, PR #644), {@link TemplateRenderer} (the ST4 wrapper that
 * renders this emitter's OWN template copy {@code templates/ir-java-pojo.stg}) and
 * {@link IRPropertyModel#javadocOf} (the POJO javadoc law, already derived from the IR alone at PR #644). None of
 * them reads a model.
 */
public final class IRDataTypeEmitter {

    static final String TEMPLATE_GROUP = "templates/ir-java-pojo.stg";

    // ------------------------------------------------------------------- the library tokens the POJO's text writes
    // Mirrors of ModelObjectGenerator's T_* constants (:400-434). The emitter carries its OWN copies, as the enum
    // emitter does: the old generator could be deleted and these bytes would still be written.
    static final String T_ROSETTA_MODEL_OBJECT = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.RosettaModelObject");
    static final String T_ROSETTA_DATA_TYPE = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RosettaDataType");
    static final String T_RUNE_DATA_TYPE = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RuneDataType");
    /**
     * THE CHOICE ARM'S ONE ANNOTATION TOKEN (v3.3 seat 10, PR #646 commit 4) - {@code ModelObjectGenerator:426}'s
     * own {@code T_RUNE_CHOICE_TYPE}, spelled through the SAME {@code ImportCollisionResolver.typeRefOrBare} law as
     * {@link #T_RUNE_DATA_TYPE} beside it, so a first-claim collision on the simple name resolves identically.
     */
    static final String T_RUNE_CHOICE_TYPE =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RuneChoiceType");
    static final String T_LIST = ImportCollisionResolver.typeRefOrBare("java.util.List");
    /** Section 8's two tokens ({@code ModelObjectGenerator:630} and {@code :636}), this emitter's own copies. */
    static final String T_ROSETTA_META_DATA =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.meta.RosettaMetaData");
    static final String T_RUNE_ATTRIBUTE =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RuneAttribute");
    /**
     * Sections 9 and 10's four tokens ({@code ModelObjectGenerator:415}, {@code :417}, {@code :419} and {@code :421}),
     * this emitter's own copies: the two {@code process} signatures' path and processor types, the builder
     * interface's {@code RosettaModelObjectBuilder} super, and the {@code AttributeMeta} the {@code [metadata id]}
     * flag is written through (v3.3 seat 9, PR #645 commit 7).
     */
    static final String T_ROSETTA_PATH =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.path.RosettaPath");
    static final String T_PROCESSOR =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.process.Processor");
    static final String T_BUILDER_PROCESSOR =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.process.BuilderProcessor");
    static final String T_ATTRIBUTE_META =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.process.AttributeMeta");
    static final String T_ROSETTA_MODEL_OBJECT_BUILDER =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.RosettaModelObjectBuilder");
    /**
     * Sections 11 and 13's tokens ({@code ModelObjectGenerator:403-434}), this emitter's own copies (v3.3 seat 9,
     * PR #645 commit 8): the two the {@code Impl} constructor's list arms write, the two the {@code equals} body
     * writes, and the seven annotations of the {@code Impl} getter stack.
     */
    static final String T_OBJECTS = ImportCollisionResolver.typeRefOrBare("java.util.Objects");
    static final String T_IMMUTABLE_LIST =
            ImportCollisionResolver.typeRefOrBare("com.google.common.collect.ImmutableList");
    static final String T_LIST_EQUALS = ImportCollisionResolver.typeRefOrBare("com.rosetta.util.ListEquals");
    static final String T_ROSETTA_ATTRIBUTE =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RosettaAttribute");
    static final String T_ACCESSOR =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.Accessor");
    static final String T_ACCESSOR_TYPE =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.AccessorType");
    static final String T_MULTI = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.Multi");
    static final String T_REQUIRED =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.Required");
    static final String T_RUNE_META_TYPE =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RuneMetaType");
    static final String T_RUNE_SCOPED_REF = ImportCollisionResolver.typeRefOrBare(
            "com.rosetta.model.lib.annotations.RuneScopedAttributeReference");
    static final String T_RUNE_SCOPED_KEY = ImportCollisionResolver.typeRefOrBare(
            "com.rosetta.model.lib.annotations.RuneScopedAttributeKey");
    /**
     * Section 12's five tokens ({@code ModelObjectGenerator:404}, {@code :405}, {@code :411}, {@code :420} and
     * {@code :422}), this emitter's own copies (v3.3 seat 9, PR #645 commit 9): the {@code ArrayList} every
     * list-shaped builder field and every {@code set(List)} null arm constructs, the {@code Collectors} the
     * {@code prune()} stream and the {@code set(List)} collector name, the {@code Consumer} cast of
     * {@code merge}'s basic-list arm, the {@code BuilderMerger} of {@code merge}'s own signature, and the
     * {@code Key} the scoped-key {@code getOrCreate} arms build.
     *
     * <p>NOTE the ONE literal that is NOT a token: {@code new ArrayList<>()} INSIDE
     * {@code Collectors.toCollection(...)} is literal text in the released plugin's template
     * ({@code ModelObjectGenerator:1583} and its D50 note), so it is written BARE there and a model type named
     * {@code ArrayList} that wins the simple name leaves it non-compiling upstream. This emitter copies that
     * literal rather than correcting it - a byte the oracle writes wrong is still the byte.
     */
    static final String T_ARRAY_LIST = ImportCollisionResolver.typeRefOrBare("java.util.ArrayList");
    static final String T_COLLECTORS = ImportCollisionResolver.typeRefOrBare("java.util.stream.Collectors");
    static final String T_CONSUMER = ImportCollisionResolver.typeRefOrBare("java.util.function.Consumer");
    static final String T_BUILDER_MERGER =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.process.BuilderMerger");
    static final String T_KEY = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.meta.Key");
    /**
     * THE COMPAT MEMBERS' six tokens ({@code ModelObjectGenerator:406-408}, {@code :423}, {@code :436-437}),
     * this emitter's own copies (v3.3 seat 9, PR #645 commit 10): the ignore pair every compat member is
     * annotated with, the {@code Collections} of the {@code singletonList} / {@code emptyList} cardinality
     * arms, the {@code MapperC} of the list-to-single unwrap head, and the two {@code java.math} classes the
     * number-conversion ladder writes.
     */
    static final String T_ROSETTA_IGNORE =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RosettaIgnore");
    static final String T_RUNE_IGNORE =
            ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.annotations.RuneIgnore");
    static final String T_COLLECTIONS = ImportCollisionResolver.typeRefOrBare("java.util.Collections");
    static final String T_MAPPER_C = ImportCollisionResolver.typeRefOrBare("com.rosetta.model.lib.mapper.MapperC");
    static final String T_BIG_DECIMAL = ImportCollisionResolver.typeRefOrBare("java.math.BigDecimal");
    static final String T_BIG_INTEGER = ImportCollisionResolver.typeRefOrBare("java.math.BigInteger");

    // The four meta / library PACKAGE-PREFIX constants commit 4 carried here are GONE at commit 5: they existed only
    // for the hard-coded arms of the item-kind gap, and the property surface now carries that fact outright
    // (property.<name>.itemIsRosettaModelObject) - a prefix test on a rendered name is not a kind.

    /** The bound on the {@code extends} chain the generation-major setter walk climbs - {@link IRPropertyModel}'s own. */
    private static final int MAX_GENERATIONS = 100;

    private static final String GLOBAL_KEY = "com.rosetta.model.lib.GlobalKey";
    private static final String TEMPLATABLE = "com.rosetta.model.lib.Templatable";

    /**
     * The emitter's CONFIG - the two facts that are the GENERATOR's, not the model's, and are therefore given rather
     * than guessed: the version stamp the host passes per model ({@code GeneratorModel.version(model)}) and the
     * PRUNING-DISABLED SET the model project's {@code rosetta-config.yml} declares under
     * {@code generators.doNotPrune}. Neither is an IR fact and neither is invented here.
     *
     * <p><b>WHY THE SET AND NOT A BOOLEAN</b> (v3.3 seat 9, PR #645 commit 9). Commit 4 carried a
     * {@code boolean pruningDisabled} placeholder that no section read, and a boolean cannot say what the old
     * generator's own fact says: {@code GeneratorModel.isPruningDisabled} ({@code :116-119}) is a per-{@code (type,
     * attribute)} membership test over a set of {@code <typeFqn>#<attrRuneName>} keys, so ONE type can have one
     * attribute pruned and its sibling kept - which is exactly the iso cell's shape (the three
     * {@code ClearingPartyAndTime*Choice__1.dtls} entries). The set is passed through the seam
     * {@code GeneratorModel.doNotPrune()}, so the two routes read the SAME set and nothing is re-read from the yml.
     *
     * @param version     the host's per-model version stamp; empty string when the host passes none
     * @param doNotPrune  the {@code <typeFqn>#<attrRuneName>} keys, as the generator model holds them
     */
    public record Config(String version, Set<String> doNotPrune) {
        public Config {
            Objects.requireNonNull(version, "version");
            doNotPrune = Set.copyOf(Objects.requireNonNull(doNotPrune, "doNotPrune"));
        }

        /**
         * {@code GeneratorModel.isPruningDisabled} ({@code :116-119}) VERBATIM - the same emptiness short-circuit
         * and the same {@code fqn + "#" + runeName} key. The type half is the POJO's CANONICAL name
         * ({@code pojo.getCanonicalName().withDots()}, the key {@code ModelObjectGenerator:1286} builds); the
         * attribute half is the property's RUNE name, which the IR property surface carries and the property
         * reconcile asserts.
         */
        public boolean isPruningDisabled(String typeCanonicalFqn, String attributeRuneName) {
            return !doNotPrune.isEmpty() && doNotPrune.contains(typeCanonicalFqn + "#" + attributeRuneName);
        }
    }

    /** The POJO file's sections, in the order {@code buildBody} writes them ({@code :502-634}). */
    public enum Section {
        /** 1 - {@code package}, the imports and the static imports. */
        HEADER,
        /** 2 - the type javadoc. */
        TYPE_JAVADOC,
        /** 3 - {@code @RosettaDataType} / {@code @RuneDataType}. */
        ANNOTATIONS,
        /** 4 - {@code public interface X extends …}. */
        INTERFACE_DECLARATION,
        /** 5 - the {@code metaData} static field. */
        META_DATA,
        /** 6 - the interface getters. */
        GETTERS,
        /** 7 - {@code build()} / {@code toBuilder()} / {@code builder()}. */
        BUILD_METHODS,
        /** 8 - {@code metaData()} / {@code getType()}. */
        UTILITY_METHODS,
        /** 9 - {@code process(RosettaPath, Processor)}. */
        PROCESS,
        /** 10 - the builder interface. */
        BUILDER_INTERFACE,
        /**
         * 11 - the {@code Impl} class, INCLUDING the widening compat getters of a specialized chain
         * ({@code IRPojoCompat.appendImplDerivedGetters}). The sub-section constant
         * {@code IMPL_COMPAT_GETTERS} that commit 8 carried is GONE at commit 10: no site throws it, the walk
         * renders, and a Section that names a refusal nobody raises is a name the {@code partialAt} histogram
         * would never print.
         */
        IMPL,
        /**
         * 12 - the {@code BuilderImpl} class, INCLUDING the builder-side compat getters, their
         * {@code getOrCreate} delegates and the ancestor SETTER arms. The two sub-section constants commit 9
         * carried ({@code BUILDER_COMPAT_GETTERS}, {@code BUILDER_COMPAT_ARMS}) are GONE at commit 10, for the
         * same reason.
         */
        BUILDER_IMPL,
        /** 13 - {@code equals} / {@code hashCode} / {@code toString}. */
        EQUALS_HASHCODE_TOSTRING,
        /** 14 - the compat members of a specialized chain. */
        COMPAT_MEMBERS
    }

    /**
     * A section this commit does not render. It is NOT a failure: it is the emitter saying, by name, that the bytes
     * of that section are not derived yet - the D11 POJO SHADOW line books it as {@code partial} at the named
     * section. Since commit 9 every section of the POJO renders and no production path raises this; since
     * commit 11 the POJO member is READY, so a stop here would be a REFUSAL of the WHOLE type by
     * {@link IRTypeUnit}, which then takes all six of that type's files from the old generator - never a
     * half-written file (PR #645 round 1 cq SF-1).
     */
    public static final class NotYetRendered extends GenerationException {
        private static final long serialVersionUID = 1L;
        private final Section section;
        private final Rendered prefix;

        NotYetRendered(Section section) {
            this(section, null);
        }

        private NotYetRendered(Section section, Rendered prefix) {
            super("IR data-type emitter: section " + section + " is NOT YET RENDERED - the emitter refuses rather"
                    + " than guessing its bytes", null, null);
            this.section = section;
            this.prefix = prefix;
        }

        public Section section() {
            return section;
        }

        /**
         * THE PREFIX (v3.3 seat 9, PR #645 commit 5): the file as it stood when the render stopped - the frame, the
         * imports and every section that DID render, with the section line starts of that text. Present when the
         * stop came out of {@link IRDataTypeEmitter#renderWithSections}, which is the only caller that has a file to
         * hand back; empty when a single section method was called on its own.
         *
         * <p>It exists so the D11 POJO SHADOW line can compare the rendered prefix with the GOLDEN's first N lines
         * and read the divergence histogram BEFORE the stubbed sections land - a partial render that is never
         * compared is a measurement deferred, and this seat does not defer measurements.
         */
        public Optional<Rendered> prefix() {
            return Optional.ofNullable(prefix);
        }

        /** This same stop, carrying the prefix the render had produced when it happened. */
        NotYetRendered withPrefix(Rendered rendered) {
            return new NotYetRendered(section, rendered);
        }
    }

    /**
     * A fact the rendering needs that the IR property surface does not carry. Named, never guessed. The D11 POJO
     * SHADOW line books it as {@code refused} with {@link #fact()} as the reason, so a gap is MEASURED per cell
     * rather than argued, and {@code IRUnitPass.reason} decodes it by this type - the ONE declaration all seven
     * shadow sites take since v3.3 seat 10, PR #646 commit 3 (the POJO pass's own {@code shadowReason} is gone).
     *
     * <p><b>THREE SITES RAISE IT AT THIS COMMIT</b> (v3.3 seat 9, PR #645 commit 7), none of them with a carrier on
     * the fixture or the corpus so far: a property whose surface states MORE than one {@code AttributeMeta} (the
     * old generator's fact is a nullable SINGLE, so which one the byte names is not a fact the IR expresses); an
     * ANCESTOR generation of the setter walk that is neither a STRUCT nor a CHOICE and so has no own property
     * surface; and the meta-value setter arm reached for a property carrying no bare value type. Each names the
     * fact it wanted. The alternative to a named refusal is a guessed byte.
     */
    public static final class MissingIRFact extends GenerationException {
        private static final long serialVersionUID = 1L;
        private final String fact;

        MissingIRFact(String fact, String why) {
            super("IR data-type emitter: the fact '" + fact + "' is not carried by the IR property surface - " + why,
                    null, null);
            this.fact = fact;
        }

        public String fact() {
            return fact;
        }
    }

    private final IRTypeIndex index;
    private final Config config;
    private final TemplateRenderer renderer;
    /**
     * THE PURE STRUCTURAL TYPE TABLE the compat members' coercion algebra is parameterised by (v3.3 seat 9,
     * PR #645 commit 10). It is {@code JavaTypeUtil} behind {@link IRTypeAlgebra}, which exposes only the
     * model-free structural methods and hands the table itself to nothing but the three statement-algebra
     * constructors that take one. The instance is the HOST'S own ({@code IRModelObjectGenerator:99-101}, the
     * very table it passes to {@code super}), so both routes arithmetic their types through one table; a test
     * builds its own with {@code new JavaTypeUtil()}, whose constructor is Guice-free
     * ({@code JavaTypeUtil:46-47}) and which reads no model, no AST node and no {@code GeneratorModel}.
     */
    private final IRTypeAlgebra algebra;

    IRDataTypeEmitter(IRTypeIndex index, Config config, JavaTypeUtil typeUtil) {
        this.index = Objects.requireNonNull(index, "index");
        this.config = Objects.requireNonNull(config, "config");
        this.algebra = new IRTypeAlgebra(Objects.requireNonNull(typeUtil, "typeUtil"));
        this.renderer = new TemplateRenderer();
        this.renderer.loadGroupFromClasspath(TEMPLATE_GROUP);
    }

    /** The POJO's output key - {@link IRTypeUnit}'s own, so the unit and the emitter can never spell it apart. */
    public static String outputKey(IRTypeNode node) {
        return IRTypeUnit.outputKey(node, IRTypeUnit.Member.POJO);
    }

    /**
     * THE WHOLE FILE, from the IR alone. EVERY section AND every sub-section renders since v3.3 seat 9,
     * PR #645 commit 10 - the three compat walks included - so every {@code type} node hands back the whole
     * POJO file, frame, closing brace and all, or names by itself the fact it is missing.
     *
     * @throws MissingIRFact when a rendered section needs a fact the property surface does not carry - section 9's
     *     {@code [metadata id]} flag and section 10's meta-value arm each name one (v3.3 seat 9, PR #645 commit 7),
     *     and neither has a carrier on the fixture
     * @throws GenerationException for a node that is neither a STRUCT nor a CHOICE (the CHOICE kind is admitted
     *     since v3.3 seat 10, PR #646 commit 4 - see {@link #propertiesOf} for the five arms), and for every
     *     refusal the index or the property model raises on the way
     */
    public String render(IRTypeNode node) {
        return renderWithSections(node).text();
    }

    /**
     * THE WHOLE FILE and, for each section, the 1-BASED LINE at which it starts - so a consumer that byte-compares
     * the file with a golden can say WHICH SECTION the first differing line falls in, rather than only which line.
     * The D11 POJO SHADOW line reads exactly that.
     *
     * <p>The body is assembled section by section in {@code buildBody}'s own order, the frame is rendered around it
     * and the whole class text goes through the D50 first-claim resolution ({@code ModelObjectGenerator:362-365}) -
     * the losers' imports dropped - before the template writes the file.
     *
     * <p><b>THE PREFIX (v3.3 seat 9, PR #645 commit 5).</b> When a section refuses as {@link NotYetRendered}, the
     * stop is re-thrown CARRYING the file as it stood - the frame, the imports and every section that did render,
     * with that text's own section line starts ({@link NotYetRendered#prefix()}). NO SECTION RAISES IT SINCE
     * PR #645 commit 10, which landed the last three walks that did; the machinery stays because it is the
     * shape any future stubbed section refuses in and because the D11 POJO SHADOW line's {@code partialAt}
     * reader is written against it.
     */
    public Rendered renderWithSections(IRTypeNode node) {
        IRPropertyModel properties = propertiesOf(node);
        StringBuilder body = new StringBuilder();
        java.util.Map<Section, Integer> offsets = new java.util.LinkedHashMap<>();
        try {
            section(body, offsets, Section.TYPE_JAVADOC, () -> typeJavadoc(node));
            section(body, offsets, Section.ANNOTATIONS, () -> annotations(node));
            section(body, offsets, Section.INTERFACE_DECLARATION, () -> interfaceDeclaration(node, properties));
            section(body, offsets, Section.META_DATA, () -> metaDataField(node));
            section(body, offsets, Section.GETTERS, () -> getters(properties));
            section(body, offsets, Section.COMPAT_MEMBERS, () -> compatMembers(properties));
            // 7 and 8 RENDER since v3.3 seat 9, PR #645 commit 6 - two pure text laws of the simple name.
            section(body, offsets, Section.BUILD_METHODS, () -> buildMethods(node));
            section(body, offsets, Section.UTILITY_METHODS, () -> utilityMethods(node));
            // 9 and 10 RENDER since v3.3 seat 9, PR #645 commit 7 - the interface `process` and the whole builder
            // interface, both from the property surface alone.
            section(body, offsets, Section.PROCESS, () -> process(properties));
            section(body, offsets, Section.BUILDER_INTERFACE, () -> builderInterface(node, properties));
            // 11 RENDERS since v3.3 seat 9, PR #645 commit 8 - the whole Impl class from the property surface
            // alone, with its ONE compat-getter sub-section refusing by its own name where it is reached.
            section(body, offsets, Section.IMPL, () -> implClass(node, properties));
            // 12 RENDERS since v3.3 seat 9, PR #645 commit 9 - the whole BuilderImpl from the property surface
            // and the pruning config alone, with its TWO compat sub-sections refusing by their own names; 13's
            // POSITION renders nothing at all, which is the old generator's own text there.
            section(body, offsets, Section.BUILDER_IMPL, () -> builderImplClass(node, properties));
            section(body, offsets, Section.EQUALS_HASHCODE_TOSTRING, this::equalsHashCodeToString);
        } catch (NotYetRendered stopped) {
            // the PREFIX is deliberately handed back UNCLOSED: the old generator's file is not closed at this
            // point of its own run either, and a brace appended to a partial file would be a byte nobody wrote
            throw stopped.withPrefix(file(node, properties, body.toString(), offsets));
        }
        // THE FRAME LAW (v3.3 seat 9, PR #645 commit 9): the class-closing brace is buildBody's OWN last append,
        // made once generateBuilderImplClass returns, and it belongs to NO section - the old generator's
        // BUILDER_IMPL seam is taken before it. It is appended here, after the LAST section, so that the file
        // this emitter renders closes exactly where the old generator's does, and section 13's empty POSITION
        // still sits between section 12 and the brace, as it does there.
        body.append("}\n");
        return file(node, properties, body.toString(), offsets);
    }

    /**
     * ONE section appended: its text is produced FIRST and its start offset recorded only once it exists, so a
     * section that refuses leaves no offset of its own behind and the prefix's section map names exactly the
     * sections the prefix contains.
     */
    private void section(StringBuilder body, java.util.Map<Section, Integer> offsets, Section which,
                         java.util.function.Supplier<String> render) {
        int at = body.length();
        String text = render.get();
        offsets.put(which, at);
        body.append(text);
    }

    /**
     * The file's own view: the class text resolved first-claim-wins from the ONE seed the file's own top-level class
     * is, the losers' imports dropped, then the frame rendered around it. The section line starts are computed on
     * the FINISHED text, so they count the header's lines too.
     */
    private Rendered file(IRTypeNode node, IRPropertyModel properties, String classText,
                          java.util.Map<Section, Integer> bodyOffsets) {
        String packageName = packageOf(node);
        ImportCollisionResolver.ClassResolution resolved = ImportCollisionResolver.resolveClass(
                classText, packageName + "." + IRTypeUnit.simpleName(node), imports(node, properties));
        PojoFile model = new PojoFile(packageName, resolved.imports(), staticImports(node, properties),
                resolved.classText());
        String text = renderer.render(TEMPLATE_GROUP, "pojoFile", "m", model);
        int headerLines = countLines(text) - countLines(resolved.classText());
        java.util.Map<String, Integer> lineStarts = new java.util.LinkedHashMap<>();
        // SECTION 1 STARTS AT LINE 1 (v3.3 seat 9, PR #645 commit 6). The frame - the package line, the blank, the
        // import block and its blank - IS section 1, and the map used to begin at section 2, so a consumer that
        // asked which section a line of the FRAME falls in got no answer at all (the D11 POJO SHADOW line read
        // firstDivergence{UNKNOWN=2} at s9c5, both of them line 3 = the first import). The boundary was MISSING,
        // not mis-mapped: it is recorded here, first, so every line of every rendered file names its section.
        lineStarts.put(Section.HEADER.name(), 1);
        for (java.util.Map.Entry<Section, Integer> entry : bodyOffsets.entrySet()) {
            lineStarts.put(entry.getKey().name(),
                    headerLines + countLines(classText.substring(0, entry.getValue())) + 1);
        }
        return new Rendered(text, lineStarts);
    }

    private static int countLines(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                n++;
            }
        }
        return n;
    }

    /**
     * A rendered POJO file and the 1-based line at which each section starts in it - the D11 POJO SHADOW line's
     * input for {@code firstDivergence}.
     */
    public record Rendered(String text, java.util.Map<String, Integer> sectionLineStarts) {

        /**
         * THE SECTION A 1-BASED LINE FALLS IN (v3.3 seat 9, PR #645 commit 6) - the LAST section whose start is at
         * or before the line. A line BEFORE every recorded boundary is {@link Section#HEADER}: the frame is
         * section 1 and there is no earlier place for a line of a POJO file to be. An EMPTY map answers
         * {@code UNKNOWN}, because a map that names no boundary states nothing and a guess would be worse.
         *
         * <p>THE D11 HOST MIRRORS THIS LAW over the {@code SECTION:line,...} string this record is printed as
         * ({@code D11CorpusRegressionTest.sectionOfLine}): the host cannot call into {@code rune-ir-java}, which
         * is not on its standing test classpath - every seam of that channel is reflective - so the reader is a
         * second copy by necessity. This one is the DECLARATION, unit-tested on its own boundary table.
         */
        public String sectionOfLine(int line) {
            String found = "UNKNOWN";
            int best = Integer.MIN_VALUE;
            for (java.util.Map.Entry<String, Integer> entry : sectionLineStarts.entrySet()) {
                if (entry.getValue() <= line && entry.getValue() > best) {
                    best = entry.getValue();
                    found = entry.getKey();
                }
            }
            if ("UNKNOWN".equals(found) && !sectionLineStarts.isEmpty()) {
                return Section.HEADER.name();   // a line before every boundary is the FRAME, never nowhere
            }
            return found;
        }
    }

    /**
     * The property surface of a STRUCT or a CHOICE node; any other kind names no POJO of this emitter's.
     *
     * <p><b>THE CHOICE KIND IS ADMITTED since v3.3 seat 10, PR #646 commit 4.</b> A {@code choice} declaration owns
     * exactly the six files a {@code data} type owns, and the OLD generator writes its POJO through the SAME
     * pipeline - {@code ChoiceObjectGenerator.generate} is {@code ModelObjectGenerator.buildModelForChoice} =
     * {@code buildModel(null, choice, pojo, version)} over an {@code RJavaPojoInterface(RChoice, ...)} whose
     * properties are the options ({@code RJavaPojoInterface:343-389}). The {@code type == null} discriminator
     * carries FIVE differences in that class, and each one is an arm of this emitter named at its own section:
     * <ol>
     *   <li>{@code :156-160} the {@code RuneChoiceType} import - {@link #imports};</li>
     *   <li>{@code :242-246} the meta-class {@code ModelSymbolId} from the choice's RAW namespace and name - NO ARM
     *       on the IR side: {@link #metaClassCanonical} already derives it from {@code node.namespace()} and the
     *       simple name for BOTH kinds, which is the same pair the old generator builds for either;</li>
     *   <li>{@code :337-339} {@code extendsChoice = false} and no super - a CHOICE node's {@code baseType()} is
     *       EMPTY by construction ({@code AstToIRAdapter.adaptChoice:240-243}) exactly as
     *       {@code RJavaPojoInterface.getSuperPojo:155-161} answers {@code null}, so {@code extended},
     *       {@code implExtended} and {@code builderExtended} are all false. It is ASSERTED here rather than
     *       assumed: a CHOICE node that reports a base type is REFUSED BY NAME;</li>
     *   <li>{@code :567-577} the javadoc from the definition with an EMPTY doc-reference list - {@link #typeJavadoc};</li>
     *   <li>{@code :594-599} the {@code @RuneChoiceType} line after {@code @RuneDataType} - {@link #annotations}.</li>
     * </ol>
     * Nothing else differs: the getters, the {@code Impl}, the builders, the {@code metaData} field and the
     * {@code [metadata key]} arm are the SHARED body of {@code buildModel}, and the property surface a choice hands
     * them is {@link IRPropertyModel}'s own {@code buildChoice} ({@code :498-517}), reconciled per choice on every
     * cell since PR #644.
     */
    IRPropertyModel propertiesOf(IRTypeNode node) {
        Objects.requireNonNull(node, "node");
        if (node.kind() != IRKind.STRUCT && node.kind() != IRKind.CHOICE) {
            throw new GenerationException("IR data-type emitter: " + node.name() + " is a " + node.kind()
                    + ", not a data type - this emitter writes the `type` POJO only", null, null);
        }
        if (node.kind() == IRKind.CHOICE && node.baseType().isPresent()) {
            // difference (3), ASSERTED: a choice has no supertype anywhere in the old generator's world
            // (ModelObjectGenerator:337-339, RJavaPojoInterface:155-161), so a CHOICE node that reports one would
            // make this emitter render an `extends` clause the old generator never writes. Refused WHOLE, by name.
            throw new GenerationException("IR data-type emitter: the choice " + node.name() + " reports a base type "
                    + node.baseType().get().name() + " - a choice has NO supertype (ModelObjectGenerator:337-339,"
                    + " RJavaPojoInterface:155-161), so its POJO is refused rather than rendered with an ancestry"
                    + " the old generator never gives it", null, null);
        }
        return IRPropertyModel.of(node, index);
    }

    // ------------------------------------------------------------------------------------------- 1 the HEADER

    /**
     * SECTION 1 - the file frame: {@code package}, the resolved import block and the static imports, rendered through
     * this emitter's own template copy. The import SET is {@link #imports}; the first-claim law that decides which of
     * them survive is applied by the CALLER of this section over the whole class text
     * ({@code ImportCollisionResolver.resolveClass}, {@code ModelObjectGenerator:362-365}) - it cannot be applied
     * here, because it needs the body, which this commit does not render. So this section renders the frame from the
     * UNRESOLVED set, which is what {@code buildModel} hands the template when a class text carries no collision
     * ({@code resolveClass}'s own no-sentinel arm).
     */
    String header(IRTypeNode node, IRPropertyModel properties) {
        String packageName = packageOf(node);
        PojoFile model = new PojoFile(packageName, imports(node, properties), staticImports(node, properties), "");
        return renderer.render(TEMPLATE_GROUP, "pojoFile", "m", model);
    }

    /**
     * SECTION 1's IMPORT SET ({@code buildModel}, {@code :150-353}), decision for decision, over the IR property
     * surface. The order of the {@code addImport} calls is immaterial - {@link ImportCollector} sorts - but the
     * MEMBERSHIP is byte-bearing, so every gate of the original is reproduced here and named by its site.
     */
    List<String> imports(IRTypeNode node, IRPropertyModel properties) {
        String packageName = packageOf(node);
        List<IRPropertyModel.IRProperty> allProps = properties.allProperties();
        List<IRPropertyModel.IRProperty> ownProps = properties.ownProperties();
        boolean extended = node.baseType().isPresent();
        boolean implExtended = implExtended(node, properties);
        boolean builderExtended = builderExtended(node, properties);
        List<IRPropertyModel.IRProperty> implProps = implExtended ? ownProps : allProps;
        List<IRPropertyModel.IRProperty> builderProps = builderExtended ? ownProps : allProps;

        ImportCollector imports = new ImportCollector(packageName);
        imports.addImport("com.rosetta.model.lib.RosettaModelObject");
        imports.addImport("com.rosetta.model.lib.RosettaModelObjectBuilder");
        imports.addImport("com.rosetta.model.lib.annotations.RosettaDataType");
        imports.addImport("com.rosetta.model.lib.annotations.RuneDataType");
        // THE CHOICE ARM, difference (1) (v3.3 seat 10, PR #646 commit 4) - ModelObjectGenerator:156-160: the
        // `type == null` path adds this one import beside @RuneDataType. It goes in as a CLAIM on the collector,
        // never as a literal line, so the D50 first-claim resolution (ModelObjectGenerator:362-365, applied by
        // this emitter's own caller over the whole class text) decides a simple-name collision exactly as it does
        // for every other claim in this set.
        if (node.kind() == IRKind.CHOICE) {
            imports.addImport("com.rosetta.model.lib.annotations.RuneChoiceType");
        }
        imports.addImport("com.rosetta.model.lib.meta.RosettaMetaData");
        imports.addImport("com.rosetta.model.lib.path.RosettaPath");
        imports.addImport("com.rosetta.model.lib.process.BuilderMerger");
        imports.addImport("com.rosetta.model.lib.process.BuilderProcessor");
        imports.addImport("com.rosetta.model.lib.process.Processor");
        imports.addImport("com.rosetta.model.lib.annotations.RuneAttribute");
        if (!allProps.isEmpty()) {   // :168-173
            imports.addImport("com.rosetta.model.lib.annotations.RosettaAttribute");
            imports.addImport("com.rosetta.model.lib.annotations.Accessor");
            imports.addImport("com.rosetta.model.lib.annotations.AccessorType");
        }
        boolean hasMulti = allProps.stream().anyMatch(IRPropertyModel.IRProperty::multi);
        boolean hasRequired = allProps.stream().anyMatch(IRPropertyModel.IRProperty::required);
        if (hasMulti) {
            imports.addImport("com.rosetta.model.lib.annotations.Multi");
        }
        if (hasRequired) {
            imports.addImport("com.rosetta.model.lib.annotations.Required");
        }
        if (hasMulti) {
            imports.addImport("java.util.List");
        }
        // :180-184 - Objects for every non-list impl property, and for a list one whose item is a model object
        boolean needsObjects = implProps.stream().anyMatch(p -> !p.multi())
                || implProps.stream().anyMatch(p -> p.multi() && itemIsModelObject(p));
        if (needsObjects) {
            imports.addImport("java.util.Objects");
        }
        // :186-203 - the interface declarations, minus a supertype whose nested refs are FQN-inlined
        boolean superInlined = superFqnInlined(node, packageName);
        for (String ifaceFqn : interfaceDeclarationFqns(node, properties)) {
            if (extended && superInlined && ifaceFqn.equals(superCanonical(node))) {
                continue;
            }
            if (!ifaceFqn.startsWith("java.lang.")) {
                imports.addImport(ifaceFqn);
            }
        }
        if (allProps.stream().anyMatch(IRDataTypeEmitter::isMetaProperty)) {   // :205-209
            imports.addImport("com.rosetta.model.lib.annotations.RuneMetaType");
        }
        if (allProps.stream().anyMatch(p -> p.attributeMetaTypes().contains("SCOPED_REFERENCE"))) {   // :211-217
            imports.addImport("com.rosetta.model.lib.annotations.RuneScopedAttributeReference");
        }
        if (allProps.stream().anyMatch(p -> p.attributeMetaTypes().contains("SCOPED_KEY"))) {   // :219-226
            imports.addImport("com.rosetta.model.lib.annotations.RuneScopedAttributeKey");
            imports.addImport("com.rosetta.model.lib.meta.Key");
        }
        if (allProps.stream().anyMatch(p -> !p.attributeMeta().isEmpty())) {   // :228-234
            imports.addImport("com.rosetta.model.lib.process.AttributeMeta");
        }
        for (IRPropertyModel.IRProperty prop : allProps) {   // :236-238
            addTypeImports(imports, prop);
        }
        imports.addImport(metaClassCanonical(node));   // :240-252
        if (extended && !superInlined) {   // :253-262
            imports.addImport(superCanonical(node));
        }
        if (allProps.stream().anyMatch(IRPropertyModel.IRProperty::multi)) {   // :264-268
            imports.addImport("java.util.ArrayList");
            imports.addImport("java.util.stream.Collectors");
        }
        if (implProps.stream().anyMatch(IRPropertyModel.IRProperty::multi)) {   // :269-272
            imports.addImport("com.google.common.collect.ImmutableList");
            imports.addImport("com.rosetta.util.ListEquals");
        }
        if (builderProps.stream().anyMatch(p -> p.multi() && !itemIsModelObject(p))) {   // :273-286
            imports.addImport("java.util.function.Consumer");
        }
        // :288-330 - the specialization chain's imports, from the ANCESTOR CHAIN the property surface now carries
        // (v3.3 seat 9, PR #645 commit 5 - commit 4's named gap 2, closed). Decision for decision: every ancestor's
        // own type imports, the RosettaIgnore / RuneIgnore pair when ANY property is specialized, Collections +
        // MapperC when ANY rung's cardinality differs from its specializing property's, and the bare java.util.List
        // when ANY rung is list-shaped (a list-shaped ancestor's compat arms declare List positions even when the
        // type has no list-shaped MAIN property).
        boolean anySpecialized = false;
        boolean anyCardinalityChangeInChain = false;
        boolean anyChainList = false;
        for (IRPropertyModel.IRProperty prop : allProps) {
            boolean mainIsList = prop.multi();
            for (IRPropertyModel.IRParentLink ancestor : prop.parentChain()) {
                anySpecialized = true;
                addTypeImports(imports, ancestor);
                anyChainList |= ancestor.multi();
                if (ancestor.multi() != mainIsList) {
                    anyCardinalityChangeInChain = true;
                }
            }
        }
        if (anySpecialized) {
            imports.addImport("com.rosetta.model.lib.annotations.RosettaIgnore");
            imports.addImport("com.rosetta.model.lib.annotations.RuneIgnore");
        }
        if (anyCardinalityChangeInChain) {
            imports.addImport("java.util.Collections");
            imports.addImport("com.rosetta.model.lib.mapper.MapperC");
        }
        if (anyChainList) {
            imports.addImport("java.util.List");
        }
        if (builderProps.stream().anyMatch(IRPropertyModel.IRProperty::multi)) {   // :344-352
            imports.addImport("com.rosetta.util.ListEquals");
        }
        return imports.getImports();
    }

    /** SECTION 1's STATIC imports ({@code :332-337}): {@code ofNullable}, iff the {@code Impl} has any field. */
    List<String> staticImports(IRTypeNode node, IRPropertyModel properties) {
        boolean implExtended = implExtended(node, properties);
        List<IRPropertyModel.IRProperty> implProps = implExtended
                ? properties.ownProperties() : properties.allProperties();
        ImportCollector staticImports = new ImportCollector(packageOf(node));
        if (!implProps.isEmpty()) {
            staticImports.addStaticImport("java.util.Optional.ofNullable");
        }
        return staticImports.getStaticImports();
    }

    /** {@code addTypeImports} ({@code :1775-1808}) over the IR property. */
    private static void addTypeImports(ImportCollector imports, IRPropertyModel.IRProperty prop) {
        // the FULL type: a list-wrapped property is a parameterized type, never a JavaClass, so the first arm of the
        // original does not fire for it - reproduced here as the `!multi` guard
        if (!prop.multi()) {
            addClassImport(imports, prop.javaType(), true);
        } else {
            addClassImport(imports, IRPropertyModel.itemOf(prop.javaType()), true);
        }
        // the meta VALUE type - the collidesWithJavaLang skip is deliberately NOT applied here (:1795-1800)
        prop.metaValueType().ifPresent(value -> addClassImport(imports, value, false));
    }

    /**
     * {@code addTypeImports} ({@code :1775-1808}) over ONE RUNG of a specialization chain - the call
     * {@code ModelObjectGenerator:299} makes for every ancestor. The same two arms as the property overload above,
     * because the old generator passes an ancestor {@code JavaPojoProperty} to the very same method: the rung's own
     * type (its ITEM when it is list-shaped, since a parameterized type is no {@code JavaClass}) and, ungated by the
     * {@code java.lang} collision skip, the BARE value type behind its meta wrap.
     */
    private static void addTypeImports(ImportCollector imports, IRPropertyModel.IRParentLink ancestor) {
        if (!ancestor.multi()) {
            addClassImport(imports, ancestor.javaType(), true);
        } else {
            addClassImport(imports, IRPropertyModel.itemOf(ancestor.javaType()), true);
        }
        ancestor.metaValueType().ifPresent(value -> addClassImport(imports, value, false));
    }

    private static void addClassImport(ImportCollector imports, String fqn, boolean collisionGated) {
        if (!isClassName(fqn) || fqn.startsWith("java.lang.")) {
            return;
        }
        if (collisionGated && IRJavaLangCollision.collides(simpleOf(fqn))) {
            return;
        }
        imports.addImport(fqn);
    }

    // -------------------------------------------------------------------------------------- 2 the TYPE JAVADOC

    /**
     * SECTION 2 ({@code :511-524}) - the type's javadoc block plus its newline, or the empty string when the old
     * generator's {@code javadoc(...)} answers {@code null}. The block itself is
     * {@link IRPropertyModel#javadocOf(Optional, List, String)}, which reproduces
     * {@code ModelGeneratorUtil.javadoc(definition, docReferences, version, pojoStyle = true)} from the IR facts.
     */
    String typeJavadoc(IRTypeNode node) {
        // THE CHOICE ARM, difference (4) (v3.3 seat 10, PR #646 commit 4) - ModelObjectGenerator:567-577 takes the
        // CHOICE path's javadoc off pojo.getJavadoc(), which is
        // generatorUtil.javadoc(choiceNode.definition(), List.of(), version) (RJavaPojoInterface:114-118): the
        // definition alone, with an EMPTY doc-reference list, where the data-type path threads docReferences().
        // The arm is CODED rather than inherited: today no CHOICE node can carry a doc reference at all (RChoice
        // declares no docReferences() accessor, and AstToIRAdapter.adaptChoice:243 passes List.of()), so the law
        // would be true here by three accidents of other files. A fixture node built with doc references states it
        // as this emitter's own law and IRDataTypeEmitterTest's choice case (c) is its witness.
        List<com.regnosys.rosetta.ir.core.IRDocReference> docReferences =
                node.kind() == IRKind.CHOICE ? List.of() : node.docReferences();
        return IRPropertyModel.javadocOf(node.definition(), docReferences, config.version())
                .map(block -> block + "\n").orElse("");
    }

    // --------------------------------------------------------------------------------------- 3 the ANNOTATIONS

    /**
     * SECTION 3 ({@code :527-542}) - {@code @RosettaDataType} then {@code @RuneDataType}. The {@code model=} short
     * name is the package's FIRST dotted segment (the whole package when it has none), and the version is the
     * config's, empty when the host passes none.
     */
    String annotations(IRTypeNode node) {
        String typeName = IRTypeUnit.simpleName(node);
        String packageName = packageOf(node);
        String version = config.version();
        String modelShortName = packageName.contains(".")
                ? packageName.substring(0, packageName.indexOf('.'))
                : packageName;
        StringBuilder body = new StringBuilder();
        body.append("@").append(T_ROSETTA_DATA_TYPE).append("(value=\"").append(typeName).append("\", builder=")
                .append(typeName).append(".").append(typeName).append("BuilderImpl.class, version=\"")
                .append(version).append("\")\n");
        body.append("@").append(T_RUNE_DATA_TYPE).append("(value=\"").append(typeName).append("\", model=\"")
                .append(modelShortName).append("\", builder=")
                .append(typeName).append(".").append(typeName).append("BuilderImpl.class, version=\"")
                .append(version).append("\")\n");
        // THE CHOICE ARM, difference (5) (v3.3 seat 10, PR #646 commit 4) - ModelObjectGenerator:594-599: the
        // `type == null` path appends the bare @RuneChoiceType line AFTER @RuneDataType, spelled through the same
        // typeRefOrBare law as the two above it (:426 vs this emitter's own T_RUNE_CHOICE_TYPE).
        if (node.kind() == IRKind.CHOICE) {
            body.append("@").append(T_RUNE_CHOICE_TYPE).append("\n");
        }
        return body.toString();
    }

    // ----------------------------------------------------------------------------- 4 the INTERFACE DECLARATION

    /**
     * SECTION 4 ({@code :544-566}) - {@code public interface X extends <super|RosettaModelObject>[, GlobalKey]
     * [, Templatable] &#123;} and the blank line after it. The supertype is FQN-inlined when its SIMPLE name collides
     * with {@code java.lang} (the #304 facet); every other interface is a first-claim sentinel.
     */
    String interfaceDeclaration(IRTypeNode node, IRPropertyModel properties) {
        String typeName = IRTypeUnit.simpleName(node);
        boolean extended = node.baseType().isPresent();
        StringBuilder body = new StringBuilder();
        body.append("public interface ").append(typeName);
        String superSimple = null;
        if (extended) {
            String canonical = superCanonical(node);
            superSimple = simpleOf(canonical);
            body.append(" extends ").append(IRJavaLangCollision.collides(superSimple)
                    ? canonical : ImportCollisionResolver.typeRefOrBare(canonical));
        } else {
            body.append(" extends ").append(T_ROSETTA_MODEL_OBJECT);
        }
        for (String ifaceFqn : interfaceDeclarationFqns(node, properties)) {
            String ifaceSimple = simpleOf(ifaceFqn);
            if (!"RosettaModelObject".equals(ifaceSimple)
                    && !ifaceSimple.equals(extended ? superSimple : "")) {
                body.append(", ").append(ImportCollisionResolver.typeRefOrBare(ifaceFqn));
            }
        }
        body.append(" {\n\n");
        return body.toString();
    }

    /**
     * {@code RJavaPojoInterface.getInterfaceDeclarations} ({@code :178-195}): the supertype (or
     * {@code RosettaModelObject}) FIRST, then {@code GlobalKey} for {@code [metadata key]}, then {@code Templatable}
     * for {@code [metadata template]} - both read off the DECLARATION's own annotations, never the chain's.
     */
    List<String> interfaceDeclarationFqns(IRTypeNode node, IRPropertyModel properties) {
        List<String> interfaces = new ArrayList<>();
        interfaces.add(node.baseType().isPresent()
                ? superCanonical(node) : "com.rosetta.model.lib.RosettaModelObject");
        if (IRPropertyModel.hasTypeMeta(node, "key")) {
            interfaces.add(GLOBAL_KEY);
        }
        if (IRPropertyModel.hasTypeMeta(node, "template")) {
            interfaces.add(TEMPLATABLE);
        }
        return interfaces;
    }

    // ------------------------------------------------------------------------------------------ 5 the METADATA

    /**
     * SECTION 5 ({@code :569-571}) - the {@code metaData} static field. Its class is
     * {@code JavaTypeTranslator.toJavaMetaDataClass} ({@code :240-248}): the declaring namespace's {@code meta}
     * sub-package, escaped, and {@code <Simple>Meta}.
     */
    String metaDataField(IRTypeNode node) {
        String metaClassRef = ImportCollisionResolver.typeRefOrBare(metaClassCanonical(node));
        return "\t" + metaClassRef + " metaData = new " + metaClassRef + "();\n\n";
    }

    /** {@code <ns>.meta.<Simple>Meta}, the escaped package - the ONE spelling the unit's META member also takes. */
    static String metaClassCanonical(IRTypeNode node) {
        DottedPath metaPackage = DottedPath.splitOnDots(IRTypeUnit.namespaceOf(node)).child("meta");
        return JavaPackageName.escape(metaPackage).getName().withDots() + "." + IRTypeUnit.simpleName(node) + "Meta";
    }

    // ------------------------------------------------------------------------------------------- 6 the GETTERS

    /**
     * SECTION 6 ({@code :576-579} -&gt; {@code generateInterfaceGetter}, {@code :638-655}) - the banner, one getter
     * per OWN property (the javadoc block tab-indented, {@code @Override} iff the getter keeps the parent's name),
     * and the blank line after.
     */
    String getters(IRPropertyModel properties) {
        StringBuilder body = new StringBuilder();
        body.append("\t/*********************** Getter Methods  ***********************/\n");
        for (IRPropertyModel.IRProperty prop : properties.ownProperties()) {
            String javadoc = prop.javadoc().orElse(null);
            if (javadoc != null && !javadoc.isEmpty()) {
                body.append(indent(javadoc, "\t")).append("\n");
            }
            if (prop.getterOverridesParentGetter()) {
                body.append("\t@Override\n");
            }
            body.append("\t").append(interfaceGetterType(prop)).append(" ").append(getterName(prop)).append("();\n");
        }
        body.append("\n");
        return body.toString();
    }

    /**
     * {@code interfaceGetterType} ({@code :1917-1925}): a LIST property's element takes {@code ? extends} when the
     * item is a {@code RosettaModelObject}, and not otherwise; a single property is its item type. The value-site
     * render is {@code valueSiteTypeRef} ({@code :1887-1900}) - a MODEL type colliding with {@code java.lang} is
     * fully qualified, every other class is a first-claim sentinel.
     */
    static String interfaceGetterType(IRPropertyModel.IRProperty prop) {
        return interfaceGetterType(prop.javaType(), prop.multi(), itemIsModelObject(prop));
    }

    /**
     * THE SAME LAW OVER A SHAPE'S THREE FACTS (v3.3 seat 9, PR #645 commit 10), so that an ANCESTOR RUNG's
     * compat getter and the MAIN property's own getter are written by ONE declaration (LAW 69): the compat
     * member's signature is in the ANCESTOR's Java shape, and {@code PojoCompatEmitter:181} asks the very same
     * {@code gen.interfaceGetterType} of it that {@code ModelObjectGenerator:1051} asks of the main property.
     *
     * @param rendered          the shape's Java type in the old generator's own spelling
     * @param multi             the shape's type is list-wrapped
     * @param itemIsModelObject the shape's ITEM is a {@code RosettaModelObject}
     */
    static String interfaceGetterType(String rendered, boolean multi, boolean itemIsModelObject) {
        String item = valueSiteTypeRef(itemTypeOf(rendered));
        if (!multi) {
            return item;
        }
        return itemIsModelObject ? T_LIST + "<? extends " + item + ">" : T_LIST + "<" + item + ">";
    }

    /**
     * {@code JavaPojoProperty.getOperationName(GET)} ({@code :120-133}): {@code get} + the GETTER compatibility name
     * with its first letter upper-cased, then the {@code Object} / {@code RosettaModelObject} escape
     * ({@code :148-153}: {@code getClass} and {@code getType} take a leading underscore).
     */
    static String getterName(IRPropertyModel.IRProperty prop) {
        return operationName("get", prop.getterCompatibilityName(), "");
    }

    /**
     * {@code JavaPojoProperty.getOperationName} ({@code :121-133}) factored to its three inputs - the ONE accessor
     * naming law of this emitter (v3.3 seat 9, PR #645 commit 7). The name is the prefix, the compatibility name
     * with its first letter upper-cased and the postfix, then {@code escapeOperationName} ({@code :148-153}): the
     * two names that clash with an inherited {@code Object} / {@code RosettaModelObject} method take a leading
     * underscore. The escape is applied to EVERY operation, not only the getters, because that is where the old
     * generator applies it - a {@code set} or {@code add} can never reach the set, and a law that only fires where
     * it can is a law that drifts the day the set grows.
     *
     * @param prefix            {@code get} / {@code getOrCreate} / {@code set} / {@code add}
     *                          ({@code JavaPojoPropertyOperationType})
     * @param compatibilityName the GETTER compatibility name for {@code get} and {@code getOrCreate}, the SETTER
     *                          one for {@code set} and {@code add} ({@code :122-128}) - the caller's to pick, as
     *                          the old generator picks it off the operation type
     * @param postfix           {@code Value} for the meta-value operations, empty otherwise
     */
    static String operationName(String prefix, String compatibilityName, String postfix) {
        String opName = prefix + JavaNamingUtil.toFirstUpper(compatibilityName) + postfix;
        return "getClass".equals(opName) || "getType".equals(opName) ? "_" + opName : opName;
    }

    // ------------------------------------------------------------------------------------ 14 the COMPAT MEMBERS

    /**
     * SECTION 14 ({@code :621-623}) - the compat members of the specialized chains, at THIS position in
     * {@code buildBody}. It is EMPTY for every type, specialized or not, and that is the old generator's own text:
     * {@code :621-623} only CONSTRUCTS the {@code PojoCompatEmitter} (or leaves it null when
     * {@code anySpecialized(allProps)} is false) and appends nothing - every arm the emitter writes is appended
     * later, by {@code generateImplClass} ({@code :626}) and {@code generateBuilderImplClass} ({@code :629}),
     * which are sections 11 and 12 and do not render at this commit.
     *
     * <p><b>WHAT COMMIT 5 CHANGED HERE</b> (v3.3 seat 9, PR #645 - the gate commit): the refusal. Commit 4 threw
     * {@code MissingIRFact("property.<name>.parentChain.types")} from this section for any property with
     * {@code parentChainDepth > 0}, because the chain's Java shapes were not in the IR. They are now
     * ({@link IRPropertyModel.IRParentLink}, reconciled per property), so nothing is refused: the section renders
     * its honest empty text for a specialized type too, and the chain's byte-bearing consumer AT THIS COMMIT is
     * section 1's own import arm ({@code :288-330}, {@link #imports}), which reads the same rungs. Sections 11 and
     * 12 will write the arms themselves from these rungs when they land.
     */
    String compatMembers(IRPropertyModel properties) {
        Objects.requireNonNull(properties, "properties");
        return "";
    }

    // ------------------------------------------------------------------------------ 7 the BUILD METHODS (commit 6)

    /**
     * SECTION 7 ({@code ModelObjectGenerator:613-625}) - the {@code Build Methods} banner, {@code build()},
     * {@code toBuilder()} and the static {@code builder()} factory, then the blank line that closes the section.
     * EVERY byte of it is a function of the type's SIMPLE NAME alone: no property, no supertype, no config, no
     * library token (the nested {@code Builder} / {@code BuilderImpl} are same-file types the old generator writes
     * bare, outside the D50 sentinel law - {@code ImportCollisionResolver}'s own "a same-package nested builder
     * written bare under a lost outer" paragraph).
     *
     * <p>The whitespace is copied from the old generator's own appends, not retyped from memory: the two
     * separators between the three members are {@code "\t\n"} - a TAB on its own line, which is what upstream
     * writes - and the section ends on a BARE newline.
     */
    String buildMethods(IRTypeNode node) {
        String typeName = IRTypeUnit.simpleName(node);
        StringBuilder body = new StringBuilder();
        body.append("\t/*********************** Build Methods  ***********************/\n");
        body.append("\t").append(typeName).append(" build();\n");
        body.append("\t\n");
        body.append("\t").append(typeName).append(".").append(typeName).append("Builder toBuilder();\n");
        body.append("\t\n");
        body.append("\tstatic ").append(typeName).append(".").append(typeName).append("Builder builder() {\n");
        body.append("\t\treturn new ").append(typeName).append(".").append(typeName)
                .append("BuilderImpl();\n");
        body.append("\t}\n");
        body.append("\n");
        return body.toString();
    }

    // ---------------------------------------------------------------------------- 8 the UTILITY METHODS (commit 6)

    /**
     * SECTION 8 ({@code ModelObjectGenerator:627-640}) - the {@code Utility Methods} banner, the {@code metaData()}
     * default that returns the section-5 field, and the {@code getType()} default under
     * {@code @RuneAttribute("@type")}. Its facts are the type's SIMPLE NAME and two library tokens
     * ({@code RosettaMetaData} and {@code RuneAttribute}), both written as D50 first-claim SENTINELS exactly as the
     * old generator writes them, so a file whose model carries a rival {@code RosettaMetaData} resolves the same
     * way on both routes. {@code Class} stays a BARE word: it is a top-level {@code java.lang} type, which
     * {@code typeRefOrBare} takes implicitly and never claims.
     *
     * <p>The section ends on {@code "\t\n"} - a TAB line, not a bare newline - because the old generator's
     * {@code process} section opens directly after it. Copied from {@code :640}, never retyped.
     */
    String utilityMethods(IRTypeNode node) {
        String typeName = IRTypeUnit.simpleName(node);
        StringBuilder body = new StringBuilder();
        body.append("\t/*********************** Utility Methods  ***********************/\n");
        body.append("\t@Override\n");
        body.append("\tdefault ").append(T_ROSETTA_META_DATA).append("<? extends ").append(typeName)
                .append("> metaData() {\n");
        body.append("\t\treturn metaData;\n");
        body.append("\t}\n");
        body.append("\t\n");
        body.append("\t@Override\n");
        body.append("\t@").append(T_RUNE_ATTRIBUTE).append("(\"@type\")\n");
        body.append("\tdefault Class<? extends ").append(typeName).append("> getType() {\n");
        body.append("\t\treturn ").append(typeName).append(".class;\n");
        body.append("\t}\n");
        body.append("\t\n");
        return body.toString();
    }

    // ------------------------------------------------------------------------- 9 the PROCESS METHOD (commit 7)

    /**
     * SECTION 9 ({@code ModelObjectGenerator:668-669} -&gt; {@code ModelObjectBoilerplate.processMethod},
     * {@code :64-96}) - the interface's {@code default void process(RosettaPath, Processor)} and the blank line
     * {@code :669} writes after it. One line per property of the WHOLE surface, in the surface's own order, each
     * taking one of two arms:
     * <ul>
     *   <li>{@code processRosetta(path.newSubPath("<name>"), processor, <ITEM>.class, <getter>()<META>);} when the
     *       property's ITEM is a {@code RosettaModelObject} ({@code ModelObjectBoilerplate:83}), and</li>
     *   <li>{@code processor.processBasic(path.newSubPath("<name>"), <ITEM>.class, <getter>(), this<META>);}
     *       otherwise ({@code :88-90}).</li>
     * </ul>
     * BOTH arms visit the VALUE site ({@code :79}, {@code ModelObjectGenerator.valueSiteTypeRef}) - the builder
     * arm's bare {@code X.XBuilder} law is section 10's, not this one's.
     *
     * <p>Every fact is READ off the reconciled property: the name, the getter's compatibility name, the item type,
     * the item-KIND verdict ({@code property.<name>.itemIsRosettaModelObject}, reconciled at commit 5) and the
     * {@code [metadata id]} flag. None is inferred from a rendered spelling. The whitespace - the two leading tabs
     * of a property line, the closing {@code "\t}\n"}, the {@code "\t\n"} tab line and the bare newline after it -
     * is copied off {@code ModelObjectBoilerplate:82-95} and {@code ModelObjectGenerator:669}, never retyped.
     */
    String process(IRPropertyModel properties) {
        StringBuilder body = new StringBuilder();
        body.append("\t@Override\n");
        body.append("\tdefault void process(").append(T_ROSETTA_PATH).append(" path, ")
                .append(T_PROCESSOR).append(" processor) {\n");
        for (IRPropertyModel.IRProperty prop : properties.allProperties()) {
            String itemTypeName = valueSiteTypeRef(itemTypeOf(prop));
            String metaFlags = attributeMetaFlags(prop);
            boolean itemIsModel = itemIsModelObject(prop);   // ModelObjectBoilerplate:83 - the arm's ONE fact
            body.append("\t\t");
            if (itemIsModel) {
                body.append("processRosetta(path.newSubPath(\"").append(prop.name())
                        .append("\"), processor, ").append(itemTypeName).append(".class, ")
                        .append(getterName(prop)).append("()").append(metaFlags).append(");\n");
            } else {
                body.append("processor.processBasic(path.newSubPath(\"").append(prop.name())
                        .append("\"), ").append(itemTypeName).append(".class, ")
                        .append(getterName(prop)).append("(), this").append(metaFlags).append(");\n");
            }
        }
        body.append("\t}\n");
        body.append("\t\n");
        body.append("\n");   // ModelObjectGenerator:669 - the blank line between the process method and the builder
        return body.toString();
    }

    /**
     * {@code ModelObjectBoilerplate.getMetaFlags} ({@code :270-275}): {@code ", " + AttributeMeta.<NAME>} when the
     * property carries one, and nothing when it does not. The old generator's fact is a NULLABLE single
     * {@code AttributeMeta}; the IR carries the LIST form of the same fact ({@code IRProperty.attributeMeta},
     * {@code GLOBAL_KEY_FIELD} for {@code [metadata id]}). An empty list is the old generator's {@code null}; a
     * list of ONE is its single value; a list of MORE is a surface the old generator can never produce, so it is
     * REFUSED by name rather than picked from - the emitter never chooses between two facts.
     */
    private static String attributeMetaFlags(IRPropertyModel.IRProperty prop) {
        List<String> meta = prop.attributeMeta();
        if (meta.isEmpty()) {
            return "";
        }
        if (meta.size() > 1) {
            throw new MissingIRFact("property." + prop.name() + ".attributeMeta",
                    "the old generator carries at most ONE AttributeMeta per property (a nullable single), and this"
                            + " property's surface states " + meta + " - which of them the byte would name is not a"
                            + " fact the IR expresses, so it is refused rather than picked");
        }
        return ", " + T_ATTRIBUTE_META + "." + meta.get(0);
    }

    // --------------------------------------------------------------------- 10 the BUILDER INTERFACE (commit 7)

    /**
     * SECTION 10 ({@code ModelObjectGenerator:672} -&gt; {@code generateBuilderInterface}, {@code :728-836}) - the
     * whole {@code XBuilder} interface, in the old generator's own order: the banner ({@code :732}), the
     * {@code interface XBuilder extends X, …} declaration ({@code :733-752}), the {@code getOrCreate} + covariant
     * {@code @Override} getter pair of every OWN model-object property ({@code :757-772}), the setter declarations
     * GENERATION-MAJOR ({@code :787}, {@link #emitBuilderInterfaceSetters}), the builder's own
     * {@code process(RosettaPath, BuilderProcessor)} ({@code :789-830}) and {@code prune()} with the closing brace
     * ({@code :832-835}).
     *
     * <p>THE ONE PLACE THIS SECTION DIFFERS FROM SECTION 9, and it is deliberate: the builder-{@code process}'s
     * MODEL arm visits {@code X.XBuilder.class} written with the item's BARE simple name ({@code :802}, facet
     * javaLangAttrFqn), while its BASIC arm visits the VALUE site ({@code :824}). Section 9 visits the value site in
     * both arms. Copied from the two appends, not reasoned from one.
     */
    String builderInterface(IRTypeNode node, IRPropertyModel properties) {
        String typeName = IRTypeUnit.simpleName(node);
        boolean extended = node.baseType().isPresent();
        java.util.Set<String> written = fileWrittenSimpleNames(node, properties);
        StringBuilder body = new StringBuilder();
        body.append("\t/*********************** Builder Interface  ***********************/\n");
        body.append("\tinterface ").append(typeName).append("Builder extends ").append(typeName);
        if (!extended) {
            body.append(", ").append(T_ROSETTA_MODEL_OBJECT_BUILDER);
        } else {
            body.append(", ").append(superNestedQualifier(node))
                    .append(".").append(simpleOf(superCanonical(node))).append("Builder");
        }
        for (String ifaceFqn : interfaceDeclarationFqns(node, properties)) {
            String ifaceSimple = simpleOf(ifaceFqn);
            if ("GlobalKey".equals(ifaceSimple)) {
                body.append(", GlobalKey.GlobalKeyBuilder");
            } else if ("Templatable".equals(ifaceSimple)) {
                body.append(", Templatable.TemplatableBuilder");
            }
        }
        body.append(" {\n");

        String builderRetType = typeName + "." + typeName + "Builder";

        for (IRPropertyModel.IRProperty prop : properties.ownProperties()) {   // :757-772
            if (!itemIsModelObject(prop)) {
                continue;
            }
            String getOrCreateName = operationName("getOrCreate", prop.getterCompatibilityName(), "");
            if (prop.multi()) {
                body.append("\t\t").append(builderSingleType(prop))
                        .append(" ").append(getOrCreateName).append("(int index);\n");
            } else {
                body.append("\t\t").append(builderSingleType(prop))
                        .append(" ").append(getOrCreateName).append("();\n");
            }
            body.append("\t\t@Override\n");
            body.append("\t\t").append(builderGetterType(prop))
                    .append(" ").append(getterName(prop)).append("();\n");
        }

        emitBuilderInterfaceSetters(body, node, builderRetType, written);
        body.append("\n");
        body.append("\t\t@Override\n");
        body.append("\t\tdefault void process(").append(T_ROSETTA_PATH).append(" path, ")
                .append(T_BUILDER_PROCESSOR).append(" processor) {\n");
        for (IRPropertyModel.IRProperty prop : properties.allProperties()) {   // :792-828
            String metaFlags = attributeMetaFlags(prop);
            body.append("\t\t\t");
            if (itemIsModelObject(prop)) {
                // :802 - the BUILDER arm's bare simple name, which golden leaves bare even for a java.lang collider
                String processTypeName = simpleOf(itemTypeOf(prop));
                body.append("processRosetta(path.newSubPath(\"").append(prop.name())
                        .append("\"), processor, ").append(processTypeName)
                        .append(".").append(processTypeName)
                        .append("Builder.class, ").append(getterName(prop)).append("()")
                        .append(metaFlags).append(");\n");
            } else {
                // :824 - the BASIC arm's VALUE site, the same law section 9 takes in both arms
                body.append("processor.processBasic(path.newSubPath(\"").append(prop.name())
                        .append("\"), ").append(valueSiteTypeRef(itemTypeOf(prop))).append(".class, ")
                        .append(getterName(prop)).append("(), this")
                        .append(metaFlags).append(");\n");
            }
        }
        body.append("\t\t}\n");
        body.append("\t\t\n");

        body.append("\n");
        body.append("\t\t").append(typeName).append(".").append(typeName).append("Builder prune();\n");
        body.append("\t}\n");
        body.append("\n");
        return body.toString();
    }

    /**
     * {@code emitBuilderInterfaceSetters} ({@code :846-857}) - the GENERATION-MAJOR walk: recurse to the ROOT
     * ancestor FIRST, then let each generation contribute exactly its OWN properties in its own order. Every
     * ancestor generation's declarations carry {@code @Override}; the main generation's are bare. The
     * {@code builderRetType} is the MAIN type's on every line, whichever generation declared the property.
     *
     * <p>The old generator climbs {@code RJavaPojoInterface.getSuperPojo()}; this climbs
     * {@link IRTypeIndex#parent(com.regnosys.rosetta.ir.core.IRType)} over the node's own {@code extends} base,
     * which is the SAME link reconciled at PR #643, and reads each generation's own surface from
     * {@link #ancestorProperties}. The recursion is written as an ORDERED list of generations rather than as the
     * old generator's super-first recursion, so that the ordering is ONE readable fact ({@link #generationsRootFirst})
     * a lane can invert on its own - the emitted bytes are the recursion's, generation for generation.
     */
    private void emitBuilderInterfaceSetters(StringBuilder body, IRTypeNode node, String builderRetType,
                                             java.util.Set<String> written) {
        List<IRTypeNode> generations = generationsRootFirst(node);
        for (int i = 0; i < generations.size(); i++) {
            // :853 - `inherited = currentPojo != mainPojo`: every generation but the LAST one (the main type's)
            boolean inherited = i < generations.size() - 1;
            for (IRPropertyModel.IRProperty prop : ancestorProperties(generations.get(i)).ownProperties()) {
                emitBuilderInterfaceSetterGroup(body, prop, builderRetType, inherited, written);
            }
        }
    }

    /**
     * THE GENERATIONS OF A TYPE, ROOT FIRST ({@code :849-852}): {@code emitBuilderInterfaceSetters} recurses into
     * {@code getSuperPojo()} BEFORE writing its own group, so the TOP-MOST ancestor's own properties are declared
     * first and the main type's last. The chain is climbed through
     * {@link IRTypeIndex#parent(com.regnosys.rosetta.ir.core.IRType)}, the same link PR #643 reconciled, and bounded
     * so a cyclic {@code extends} refuses by name rather than hanging - the bound {@link IRPropertyModel} applies to
     * the very same walk.
     */
    private List<IRTypeNode> generationsRootFirst(IRTypeNode node) {
        List<IRTypeNode> generations = new ArrayList<>();
        for (IRTypeNode current = node; current != null;
                current = current.baseType().map(index::parent).orElse(null)) {
            if (generations.size() > MAX_GENERATIONS) {
                throw new GenerationException("IR data-type emitter: the supertype chain of '" + node.name()
                        + "' is deeper than " + MAX_GENERATIONS + " - the builder interface's generation-major"
                        + " setter walk is refused rather than run away", null, null);
            }
            generations.add(current);
        }
        java.util.Collections.reverse(generations);   // :849-852 - the SUPER chain is emitted BEFORE the own group
        return generations;
    }

    /**
     * ONE GENERATION'S OWN SURFACE - {@code RJavaPojoInterface.getOwnProperties()} of an ancestor pojo. The old
     * generator's walk reaches a {@code choice} super exactly as it reaches a {@code type} one (a
     * {@code type extends choice} inherits the options as its parent generation's own properties), and
     * {@link IRPropertyModel#of} carries both kinds, so a CHOICE ancestor walks here without a special case.
     *
     * <p>{@link #propertiesOf} is NOT the door for this: that is the EMITTER's population gate, and it refuses
     * every kind but the two VALIDATED ones because this emitter writes the {@code type} and {@code choice} POJOs
     * only (the CHOICE kind joined it at v3.3 seat 10, PR #646 commit 4). A generation of the setter walk is a
     * different question - "what does this ancestor declare?" - and a node of any OTHER kind answering it is a
     * fact the IR does not carry, so it is refused BY NAME rather than guessed at. The two guards have read the
     * same two kinds since PR #644 and they are separate declarations on purpose: one is a population, the other
     * a walk.
     */
    private IRPropertyModel ancestorProperties(IRTypeNode node) {
        if (node.kind() != IRKind.STRUCT && node.kind() != IRKind.CHOICE) {
            throw new MissingIRFact("ancestor." + node.name() + ".ownProperties",
                    "the generation-major setter walk (ModelObjectGenerator:846-857) needs the OWN property surface"
                            + " of every ancestor, and a node of kind " + node.kind() + " has none - the section"
                            + " refuses rather than emitting a generation's setters it cannot derive");
        }
        return IRPropertyModel.of(node, index);
    }

    /**
     * {@code emitBuilderInterfaceSetterGroup} ({@code :866-946}) - ONE property's setter declarations, in the old
     * generator's own order and with its own whitespace. The SHAPE matrix, per property:
     * <ul>
     *   <li><b>single, no meta value</b> - {@code set(<I>)};</li>
     *   <li><b>single, meta value</b> - and then {@code setValue(<V>)};</li>
     *   <li><b>list, no meta value</b> - {@code add(<I>)}, {@code add(<I>, int idx)}, {@code add(<LP>)},
     *       {@code set(<LP>)};</li>
     *   <li><b>list, meta value</b> - {@code add(<I>)}, {@code add(<I>, int idx)}, {@code addValue(<V>)},
     *       {@code addValue(<V>, int idx)}, {@code add(<LP>)}, {@code set(<LP>)}, {@code addValue(<VLP>)},
     *       {@code setValue(<VLP>)}.</li>
     * </ul>
     * {@code <LP>} is {@code List<? extends <I>>} for a model item and {@code List<<I>>} otherwise ({@code :883-885});
     * {@code <VLP>} is always {@code List<? extends <V>>} ({@code :918}). Each line is prefixed with
     * {@code "\t\t@Override\n"} when the property belongs to an ANCESTOR generation.
     */
    private void emitBuilderInterfaceSetterGroup(StringBuilder body, IRPropertyModel.IRProperty prop,
                                                 String builderRetType, boolean inherited,
                                                 java.util.Set<String> written) {
        String itemType = valueSiteTypeRef(itemTypeOf(prop));
        String setterName = operationName("set", prop.setterCompatibilityName(), "");
        String paramName = interfaceParamName(prop.name(), written);
        // :882 (the list arm) and :937 (the single arm) ask the SAME question of the SAME fact - one local, so the
        // meta-value arms of both shapes are gated by one readable decision a lane can take away whole
        boolean hasMetaValue = prop.metaValueType().isPresent();
        if (prop.multi()) {
            String addName = operationName("add", prop.setterCompatibilityName(), "");
            boolean isModel = itemIsModelObject(prop);
            String listParam = isModel
                    ? T_LIST + "<? extends " + itemType + ">"
                    : T_LIST + "<" + itemType + ">";
            if (inherited) {
                body.append("\t\t@Override\n");
            }
            body.append("\t\t").append(builderRetType).append(" ")
                    .append(addName).append("(").append(itemType).append(" ")
                    .append(paramName).append(");\n");
            if (inherited) {
                body.append("\t\t@Override\n");
            }
            body.append("\t\t").append(builderRetType).append(" ")
                    .append(addName).append("(").append(itemType).append(" ")
                    .append(paramName).append(", int idx);\n");
            if (hasMetaValue) {
                String addValueName = operationName("add", prop.setterCompatibilityName(), "Value");
                String valueTypeName = metaValueTypeName(prop);
                if (inherited) {
                    body.append("\t\t@Override\n");
                }
                body.append("\t\t").append(builderRetType).append(" ")
                        .append(addValueName).append("(").append(valueTypeName).append(" ")
                        .append(paramName).append(");\n");
                if (inherited) {
                    body.append("\t\t@Override\n");
                }
                body.append("\t\t").append(builderRetType).append(" ")
                        .append(addValueName).append("(").append(valueTypeName).append(" ")
                        .append(paramName).append(", int idx);\n");
            }
            if (inherited) {
                body.append("\t\t@Override\n");
            }
            body.append("\t\t").append(builderRetType).append(" ")
                    .append(addName).append("(").append(listParam)
                    .append(" ").append(paramName).append(");\n");
            if (inherited) {
                body.append("\t\t@Override\n");
            }
            body.append("\t\t").append(builderRetType).append(" ")
                    .append(setterName).append("(").append(listParam)
                    .append(" ").append(paramName).append(");\n");
            if (hasMetaValue) {
                String addValueName = operationName("add", prop.setterCompatibilityName(), "Value");
                String setValueName = operationName("set", prop.setterCompatibilityName(), "Value");
                String valueListParam = T_LIST + "<? extends " + metaValueTypeName(prop) + ">";
                if (inherited) {
                    body.append("\t\t@Override\n");
                }
                body.append("\t\t").append(builderRetType).append(" ")
                        .append(addValueName).append("(").append(valueListParam)
                        .append(" ").append(paramName).append(");\n");
                if (inherited) {
                    body.append("\t\t@Override\n");
                }
                body.append("\t\t").append(builderRetType).append(" ")
                        .append(setValueName).append("(").append(valueListParam)
                        .append(" ").append(paramName).append(");\n");
            }
        } else {
            if (inherited) {
                body.append("\t\t@Override\n");
            }
            body.append("\t\t").append(builderRetType).append(" ")
                    .append(setterName).append("(").append(itemType).append(" ")
                    .append(paramName).append(");\n");
            if (hasMetaValue) {
                String setValueName = operationName("set", prop.setterCompatibilityName(), "Value");
                if (inherited) {
                    body.append("\t\t@Override\n");
                }
                body.append("\t\t").append(builderRetType).append(" ")
                        .append(setValueName).append("(").append(metaValueTypeName(prop)).append(" ")
                        .append(paramName).append(");\n");
            }
        }
    }

    /**
     * THE PARAMETER NAME, ONE LAW FOR BOTH ARMS ({@code interfaceParamName}, {@code :1726-1744}, and the list arm's
     * own copy of it at {@code :878-879}): a name the FILE writes as any type escapes with a leading underscore,
     * and otherwise a Java keyword escapes through {@link JavaNamingUtil#escapeJavaKeyword}. The two are ONE method
     * here because the old generator's two sites are byte-identical predicates over the same set.
     */
    private static String interfaceParamName(String name, java.util.Set<String> fileWrittenSimpleNames) {
        if (fileWrittenSimpleNames.contains(name)) {   // :1734 / :878 - the FILE-SCOPE half of the law
            return "_" + name;
        }
        return JavaNamingUtil.escapeJavaKeyword(name);   // :1743 - and the JAVA-KEYWORD half
    }

    /**
     * {@code fileWrittenSimpleNames} ({@code :1804-1829}) - every simple name the FILE writes as a type, which is
     * the set the parameter-name law reads. Computed ONCE per section-10 render and never cached across types,
     * exactly as {@code buildModel} recomputes it per POJO ({@code :353}).
     *
     * <p>Its members: the LAST SEGMENT of every import in the UNRESOLVED collector set ({@link #imports}, the same
     * list {@code PojoSectionOracle.rawImports} holds for the old generator - the RESOLVED list is short by the
     * D50 losers and would silently un-escape a colliding parameter), {@code Object}, {@code String}, and for EVERY
     * property (all, not own) the ITEM type's simple name UNLESS the item is a class outside {@code java.lang}
     * whose simple name collides with {@code java.lang} (such a type is written FULLY QUALIFIED, so the file never
     * claims its simple name) plus the meta value type's simple name when the property has one.
     */
    private java.util.Set<String> fileWrittenSimpleNames(IRTypeNode node, IRPropertyModel properties) {
        java.util.Set<String> names = new java.util.HashSet<>();
        for (String imported : imports(node, properties)) {
            names.add(simpleOf(imported));
        }
        names.add("Object");
        names.add("String");
        for (IRPropertyModel.IRProperty prop : properties.allProperties()) {
            String item = itemTypeOf(prop);
            String simple = simpleOf(item);
            if (!isClassName(item) || item.startsWith("java.lang.") || !IRJavaLangCollision.collides(simple)) {
                names.add(simple);
            }
            prop.metaValueType().ifPresent(value -> names.add(simpleOf(value)));
        }
        return names;
    }

    /**
     * {@code metaValueTypeName} ({@code :1997-2007}): the meta VALUE type at a write - a D50 first-claim sentinel
     * when its simple name IS the last segment of its canonical name, and the bare simple name otherwise. The #306
     * {@code java.lang} collision law is deliberately NOT applied here, exactly as the old generator does not
     * apply it.
     */
    private static String metaValueTypeName(IRPropertyModel.IRProperty prop) {
        String value = prop.metaValueType().orElseThrow(() -> new MissingIRFact(
                "property." + prop.name() + ".metaValueType",
                "the meta-value setter arm was reached for a property whose surface carries no bare value type"));
        String simple = simpleOf(value);
        if (isClassName(value) && ImportCollisionResolver.simpleIsLastSegment(value, simple)) {
            return ImportCollisionResolver.typeRefOrBare(value);
        }
        return simple;
    }

    /**
     * {@code dotQualifiedBuilderType} ({@code :2019-2036}): the nested builder type {@code X.XBuilder} over the
     * item's BARE simple name - golden leaves it bare even when {@code X} collides with {@code java.lang}, because
     * only the VALUE sites FQN-inline.
     */
    private static String dotQualifiedBuilderType(String rendered) {
        String item = simpleOf(itemTypeOf(rendered));
        return item + "." + item + "Builder";
    }

    /** {@code builderGetterType} ({@code :2038-2049}) over the IR property. */
    String builderGetterType(IRPropertyModel.IRProperty prop) {
        return builderGetterType(prop.javaType(), prop.multi(), itemIsModelObject(prop));
    }

    /** The same law over a SHAPE's three facts - the rung's compat getter and the property's, ONE declaration. */
    String builderGetterType(String rendered, boolean multi, boolean itemIsModelObject) {
        if (multi) {
            return itemIsModelObject
                    ? T_LIST + "<? extends " + dotQualifiedBuilderType(rendered) + ">"
                    : T_LIST + "<" + valueSiteTypeRef(itemTypeOf(rendered)) + ">";
        }
        return builderSingleType(rendered, itemIsModelObject);
    }

    /** {@code builderSingleType} ({@code :2064-2069}) over the IR property. */
    String builderSingleType(IRPropertyModel.IRProperty prop) {
        return builderSingleType(prop.javaType(), itemIsModelObject(prop));
    }

    /** The same law over a SHAPE's two facts - the rung's {@code getOrCreate} compat delegate reads it too. */
    String builderSingleType(String rendered, boolean itemIsModelObject) {
        return itemIsModelObject ? dotQualifiedBuilderType(rendered) : valueSiteTypeRef(itemTypeOf(rendered));
    }

    /**
     * {@code superNestedQualifier} ({@code :499-503}): the supertype's CANONICAL name when its nested references are
     * FQN-inlined (the #305 facet), else its bare simple name. The caller appends {@code "." + <Super>Builder}.
     */
    private String superNestedQualifier(IRTypeNode node) {
        String canonical = superCanonical(node);
        return superFqnInlined(node, packageOf(node)) ? canonical : simpleOf(canonical);
    }

    // ------------------------------------------------------------------------------ 11 the IMPL CLASS (commit 8)

    /**
     * SECTION 11 ({@code ModelObjectGenerator:689} -&gt; {@code generateImplClass}, {@code :957-1073}) - the whole
     * immutable {@code Impl} class, byte for byte off that method's own appends: the banner, the class header, the
     * final fields, the four-armed constructor, the annotated getters, {@code build()}, {@code toBuilder()},
     * {@code setBuilderFields} and - re-indented by ONE tab ({@code :1067}) - the section-13 text of
     * {@link #boilerplate}.
     *
     * <p><b>THE EXTENSION FACT IS {@code implExtended}, NOT {@code extended}</b> ({@code :145-146}): the
     * {@code Impl} extends its parent's {@code Impl} only when EVERY own property is a subtype-compatible
     * specialization of its parent's, because a type-changing override would make the inherited field's type
     * conflict. It decides three bytes at once - the {@code extends} clause, the {@code super(builder)} /
     * {@code super.setBuilderFields(builder)} lines, and WHICH properties get fields at all ({@code :971}: own
     * when extended, the whole surface when not) - so it is computed ONCE, by {@link #implExtended}, and the
     * import set ({@link #imports}) and the static imports read the very same method.
     *
     * <p><b>ITS ONE SUB-SECTION</b> is the widening compat-getter walk ({@code :1029-1033} -&gt;
     * {@code PojoCompatEmitter.appendImplDerivedGetters}, {@code :171-190}): per specialized property, one
     * {@code @Override @RosettaIgnore @RuneIgnore} getter per ancestor whose getter the specialized getter does
     * not override, its body COERCING the field to the ancestor's interface shape. It RENDERS since v3.3 seat 9,
     * PR #645 commit 10 ({@link IRPojoCompat#appendImplDerivedGetters}), appended exactly where the old
     * generator appends it - inside the getter loop, after the property's own getter and in the old generator's
     * own property order.
     */
    String implClass(IRTypeNode node, IRPropertyModel properties) {
        String typeName = IRTypeUnit.simpleName(node);
        boolean implExtended = implExtended(node, properties);
        List<IRPropertyModel.IRProperty> implProps = implExtended                       // :971
                ? properties.ownProperties() : properties.allProperties();
        // :684-687 - the compat emitter is built at all only when SOME property of the whole surface carries a
        // specialization chain (PojoCompatEmitter.anySpecialized, :133-135, over the getParentProperty() walk;
        // the IR's rung count is the same walk's length). ONE declaration of the predicate (LAW 69): both
        // sections read IRPojoCompat.anySpecialized, which is the port of that very method.
        IRPojoCompat compat = IRPojoCompat.anySpecialized(properties.allProperties())
                ? new IRPojoCompat(this, algebra, properties.allProperties()) : null;

        StringBuilder body = new StringBuilder();
        body.append("\t/*********************** Immutable Implementation of ").append(typeName)
                .append("  ***********************/\n");
        body.append("\tclass ").append(typeName).append("Impl");
        if (implExtended) {
            // :966-968 - the #305 facet: a cross-package java.lang-colliding super is FQN-inlined here
            body.append(" extends ").append(superNestedQualifier(node))
                    .append(".").append(simpleOf(superCanonical(node))).append("Impl");
        }
        body.append(" implements ").append(typeName).append(" {\n");

        for (IRPropertyModel.IRProperty prop : implProps) {                             // :973-980
            body.append("\t\tprivate final ").append(interfaceGetterType(prop))
                    .append(" ").append(fieldName(prop)).append(";\n");
        }
        body.append("\t\t\n");

        body.append("\t\tprotected ").append(typeName).append("Impl(")                  // :983-984
                .append(typeName).append(".").append(typeName).append("Builder builder) {\n");
        if (implExtended) {
            body.append("\t\t\tsuper(builder);\n");
        }
        for (IRPropertyModel.IRProperty prop : implProps) {                             // :988-1006
            String getter = getterName(prop);
            String field = fieldName(prop);
            if (prop.multi() && itemIsModelObject(prop)) {
                body.append("\t\t\tthis.").append(field)
                        .append(" = ofNullable(builder.").append(getter)
                        .append("()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(").append(T_OBJECTS)
                        .append("::nonNull).map(f->f.build()).filter(").append(T_OBJECTS).append("::nonNull).collect(")
                        .append(T_IMMUTABLE_LIST).append(".toImmutableList())).orElse(null);\n");
            } else if (prop.multi()) {
                body.append("\t\t\tthis.").append(field)
                        .append(" = ofNullable(builder.").append(getter)
                        .append("()).filter(_l->!_l.isEmpty()).map(").append(T_IMMUTABLE_LIST)
                        .append("::copyOf).orElse(null);\n");
            } else if (itemIsModelObject(prop)) {
                body.append("\t\t\tthis.").append(field)
                        .append(" = ofNullable(builder.").append(getter)
                        .append("()).map(f->f.build()).orElse(null);\n");
            } else {
                body.append("\t\t\tthis.").append(field)
                        .append(" = builder.").append(getter).append("();\n");
            }
        }
        body.append("\t\t}\n");
        body.append("\t\t\n");

        for (IRPropertyModel.IRProperty prop : implProps) {                             // :1009-1033
            body.append(getterAnnotations(prop));
            body.append("\t\tpublic ").append(interfaceGetterType(prop))
                    .append(" ").append(getterName(prop)).append("() {\n");
            body.append("\t\t\treturn ").append(fieldName(prop)).append(";\n");
            body.append("\t\t}\n");
            body.append("\t\t\n");
            if (!implExtended && compat != null) {                                      // :1029-1033
                compat.appendImplDerivedGetters(body, prop);
            }
        }

        body.append("\t\t@Override\n");                                                  // :1036-1040
        body.append("\t\tpublic ").append(typeName).append(" build() {\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        body.append("\t\t@Override\n");                                                  // :1042-1049
        body.append("\t\tpublic ").append(typeName).append(".").append(typeName)
                .append("Builder toBuilder() {\n");
        body.append("\t\t\t").append(typeName).append(".").append(typeName)
                .append("Builder builder = builder();\n");
        body.append("\t\t\tsetBuilderFields(builder);\n");
        body.append("\t\t\treturn builder;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        body.append("\t\tprotected void setBuilderFields(").append(typeName).append(".")   // :1051-1064
                .append(typeName).append("Builder builder) {\n");
        if (implExtended) {
            body.append("\t\t\tsuper.setBuilderFields(builder);\n");
        }
        for (IRPropertyModel.IRProperty prop : implProps) {
            // :1061 - the SETTER compatibility name (the #323 facet), never the getter's
            body.append("\t\t\tofNullable(").append(getterName(prop)).append("()).ifPresent(builder::")
                    .append(operationName("set", prop.setterCompatibilityName(), "")).append(");\n");
        }
        body.append("\t\t}\n");

        body.append("\n");                                                               // :1066
        body.append(reindent(boilerplate(typeName, typeName, implExtended, implProps), "\t"));   // :1067
        body.append("\t}\n");                                                            // :1069
        body.append("\n");                                                               // :1070
        return body.toString();
    }

    /**
     * THE GETTER ANNOTATION STACK, ONE DECLARATION (v3.3 seat 9, PR #645 commit 9). Sections 11 and 12 write the
     * SAME nine lines above their getters - {@code ModelObjectGenerator:1010-1027} (the {@code Impl}) and
     * {@code :1134-1142} (the {@code BuilderImpl}) are byte-identical appends over the same facts - so this
     * emitter states them ONCE and both sections call it (LAW 69). Before commit 9 section 11 carried its own
     * copy; a second copy is a place for the two to drift the day an annotation is added to one of them.
     *
     * <p>In order: {@code @Override}; {@code @RosettaAttribute("<name>")}; {@code @Accessor(AccessorType.GETTER)};
     * {@code @Required} iff the property is required; {@code @Multi} iff it is list-shaped;
     * {@code @RuneAttribute("<name>")}; {@code @RuneMetaType} iff it is the synthetic {@code MetaFields} property;
     * {@code @RuneScopedAttributeReference} iff {@code [metadata address]}; {@code @RuneScopedAttributeKey} iff
     * {@code [metadata location]}. The label in both annotations is the property's RAW name, never its field
     * identifier. Two tabs of indent, which is what both classes are written at.
     */
    private static String getterAnnotations(IRPropertyModel.IRProperty prop) {
        StringBuilder stack = new StringBuilder();
        stack.append("\t\t@Override\n");
        stack.append("\t\t@").append(T_ROSETTA_ATTRIBUTE).append("(\"").append(prop.name()).append("\")\n");
        stack.append("\t\t@").append(T_ACCESSOR).append("(").append(T_ACCESSOR_TYPE).append(".GETTER)\n");
        if (prop.required()) {
            stack.append("\t\t@").append(T_REQUIRED).append("\n");
        }
        if (prop.multi()) {
            stack.append("\t\t@").append(T_MULTI).append("\n");
        }
        stack.append("\t\t@").append(T_RUNE_ATTRIBUTE).append("(\"").append(prop.name()).append("\")\n");
        if (isMetaProperty(prop)) {
            stack.append("\t\t@").append(T_RUNE_META_TYPE).append("\n");
        }
        if (hasScopedReference(prop)) {
            stack.append("\t\t@").append(T_RUNE_SCOPED_REF).append("\n");
        }
        if (hasScopedKey(prop)) {
            stack.append("\t\t@").append(T_RUNE_SCOPED_KEY).append("\n");
        }
        return stack.toString();
    }

    // ------------------------------------------------------------------ 12 the BUILDER IMPL CLASS (commit 9)

    /**
     * SECTION 12 ({@code ModelObjectGenerator:711} -&gt; {@code generateBuilderImplClass}, {@code :1101-1417}, with
     * the setter helpers at {@code :1440-1697}) - the whole mutable {@code BuilderImpl} class, byte for byte off
     * those methods' own appends: the banner and class header, the (list-initialised) builder fields, the
     * annotated builder getters and their {@code getOrCreate} pairs, the FULL setter family of every property,
     * {@code build()}, {@code toBuilder()}, {@code prune()}, {@code hasData()}, {@code merge()} and - re-indented
     * by ONE tab ({@code :1415}) - the BUILDER variant of the section-13 text of {@link #boilerplate}.
     *
     * <p><b>THE EXTENSION FACT IS {@code builderExtended}, NOT {@code implExtended}</b> ({@code :147-148}): the
     * {@code BuilderImpl} extends its parent's only when every own property has the IDENTICAL type as its
     * parent's - any type change at all, covariant or not, would make the inherited BUILDER field conflict, which
     * is a stricter test than the {@code Impl}'s subtype-compatibility. It decides the {@code extends} clause, the
     * three {@code super} calls ({@code prune}, {@code hasData}, {@code merge}), which properties get fields at
     * all ({@code builderProps}: own when extended, the whole surface when not) and the {@code extended} argument
     * of the boilerplate.
     *
     * <p><b>THE PRUNING CONFIG IS READ PER PROPERTY</b>, not per type: {@code :1315-1316} and {@code :1359-1360}
     * both ask {@code generatorModel.isPruningDisabled(pojo.getCanonicalName().withDots(), prop.getRuneName())},
     * so one attribute of a type can render the KEEP form while its sibling renders the pruning one. This emitter
     * asks {@link Config#isPruningDisabled} with the same two strings - the POJO's canonical name and the
     * property's RECONCILED rune name.
     *
     * <p><b>ITS TWO SUB-SECTIONS</b>, each appended exactly where the OLD GENERATOR APPENDS IT and both
     * RENDERED since v3.3 seat 9, PR #645 commit 10: the builder-side compat getters and their
     * {@code getOrCreate} delegates inside the getter loop ({@code :1225-1227} -&gt;
     * {@link IRPojoCompat#appendBuilderDerivedGetters}), and the ancestor SETTER arms inside the setter loop
     * ({@code :1266-1270} -&gt; {@link IRPojoCompat#appendAncestorSetterArms}). Their GATES differ and both are
     * the old generator's: the getter walk runs only when the {@code BuilderImpl} does NOT extend and skips a
     * rung whose getter keeps its parent's name, while the arm walk is gated on nothing but a non-empty chain
     * and writes an arm for EVERY rung.
     *
     * <p><b>WHAT THIS SECTION DOES NOT WRITE</b> is the class-closing brace: {@code buildBody} appends it itself
     * once {@code generateBuilderImplClass} returns, so the brace belongs to the file FRAME and
     * {@link #renderWithSections} appends it after the LAST section. The old generator's own {@code BUILDER_IMPL}
     * seam is taken before that append, which is what makes the two comparable byte for byte.
     */
    String builderImplClass(IRTypeNode node, IRPropertyModel properties) {
        String typeName = IRTypeUnit.simpleName(node);
        String canonical = packageOf(node) + "." + typeName;                            // :1286 - the key's type half
        boolean builderExtended = builderExtended(node, properties);
        List<IRPropertyModel.IRProperty> allProps = properties.allProperties();
        List<IRPropertyModel.IRProperty> builderProps = builderExtended                 // :346-348
                ? properties.ownProperties() : allProps;
        // :707-710 - the compat emitter is built at all only when SOME property of the whole surface is
        // specialized; the predicate is IRPojoCompat's own, as section 11's is (LAW 69)
        IRPojoCompat compat = IRPojoCompat.anySpecialized(allProps)
                ? new IRPojoCompat(this, algebra, allProps) : null;
        // :1171 / :1200 - the two SCOPE escapes, both over the WHOLE surface (the class scope holds every field,
        // own and inherited), both comparing the property name's first-letter-lower-cased form
        String idxParam = hasPropertyInScope(allProps, "index") ? "_index" : "index";
        String resLocal = hasPropertyInScope(allProps, "result") ? "_result" : "result";
        // :1233-1235 - the FIELD identifiers of the whole surface: what a pluralised list parameter would shadow
        java.util.Set<String> siblingFieldNames = new java.util.LinkedHashSet<>();
        for (IRPropertyModel.IRProperty prop : allProps) {
            siblingFieldNames.add(fieldName(prop));
        }
        String builderRetType = typeName + "." + typeName + "Builder";

        StringBuilder body = new StringBuilder();
        body.append("\t/*********************** Builder Implementation of ").append(typeName)   // :1107-1108
                .append("  ***********************/\n");
        body.append("\tclass ").append(typeName).append("BuilderImpl");
        if (builderExtended) {
            // :1112-1113 - the #305 facet: a cross-package java.lang-colliding super is FQN-inlined here
            body.append(" extends ").append(superNestedQualifier(node))
                    .append(".").append(simpleOf(superCanonical(node))).append("BuilderImpl");
        }
        body.append(" implements ").append(typeName).append(".").append(typeName).append("Builder {\n");
        body.append("\t\n");

        for (IRPropertyModel.IRProperty prop : builderProps) {                          // :1120-1127
            body.append("\t\tprotected ").append(builderFieldType(prop))
                    .append(" ").append(fieldName(prop));
            if (prop.multi()) {
                body.append(" = new ").append(T_ARRAY_LIST).append("<>()");
            }
            body.append(";\n");
        }
        body.append("\t\t\n");

        for (IRPropertyModel.IRProperty prop : builderProps) {                          // :1129-1227
            body.append(getterAnnotations(prop));
            body.append("\t\tpublic ").append(builderGetterType(prop)).append(" ")
                    .append(getterName(prop)).append("() {\n");
            body.append("\t\t\treturn ").append(fieldName(prop)).append(";\n");
            body.append("\t\t}\n");
            body.append("\t\t\n");
            if (itemIsModelObject(prop)) {
                appendGetOrCreate(body, prop, idxParam, resLocal);
            }
            if (!builderExtended && compat != null) {                                   // :1225-1227
                compat.appendBuilderDerivedGetters(body, prop);
            }
        }

        for (IRPropertyModel.IRProperty prop : allProps) {                              // :1240-1270
            if (prop.multi()) {
                appendListSetters(body, prop, builderRetType, siblingFieldNames);
            } else {
                appendSingleSetter(body, prop, builderRetType);
                // :1255-1257 - the meta convenience setter belongs to the MAIN group and precedes the arms
                if (prop.metaValueType().isPresent()) {
                    appendMetaValueSetter(body, prop, builderRetType);
                }
            }
            if (compat != null) {                                                       // :1266-1270
                compat.appendAncestorSetterArms(body, prop, builderRetType, siblingFieldNames);
            }
        }

        body.append("\t\t@Override\n");                                                 // :1272-1276
        body.append("\t\tpublic ").append(typeName).append(" build() {\n");
        body.append("\t\t\treturn new ").append(typeName).append(".").append(typeName).append("Impl(this);\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        body.append("\t\t@Override\n");                                                 // :1278-1283
        body.append("\t\tpublic ").append(builderRetType).append(" toBuilder() {\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\n");   // :1283 - ONE tab, not two: copied off the append, not reasoned from its siblings

        body.append("\t\t@SuppressWarnings(\"unchecked\")\n");                           // :1285-1307
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" prune() {\n");
        if (builderExtended) {
            body.append("\t\t\tsuper.prune();\n");
        }
        for (IRPropertyModel.IRProperty prop : builderProps) {
            if (!itemIsModelObject(prop)) {
                continue;
            }
            String field = fieldName(prop);
            // facet isoPruneConfig - the KEEP form when the (type, attribute) pair is config-listed (:1315-1316)
            boolean pruningDisabled = config.isPruningDisabled(canonical, prop.runeName());
            if (prop.multi()) {
                body.append("\t\t\t").append(field).append(" = ").append(field)
                        .append(".stream().filter(b->b!=null).<").append(builderSingleType(prop))
                        .append(">map(b->b.prune())")
                        .append(pruningDisabled ? "" : ".filter(b->b.hasData())")
                        .append(".collect(").append(T_COLLECTORS).append(".toList());\n");
            } else if (pruningDisabled) {
                body.append("\t\t\tif (").append(field).append("!=null) ").append(field).append(".prune();\n");
            } else {
                body.append("\t\t\tif (").append(field).append("!=null && !").append(field)
                        .append(".prune().hasData()) ").append(field).append(" = null;\n");
            }
        }
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        body.append("\t\t@Override\n");                                                 // :1309-1353
        body.append("\t\tpublic boolean hasData() {\n");
        if (builderExtended) {
            body.append("\t\t\tif (super.hasData()) return true;\n");
        }
        for (IRPropertyModel.IRProperty prop : builderProps) {
            if ("meta".equals(prop.name())) {                                           // :1315 - by the RAW name
                continue;
            }
            String getter = getterName(prop);
            boolean pruningDisabled = config.isPruningDisabled(canonical, prop.runeName());
            boolean model = itemIsModelObject(prop);
            // the RECONCILED meta-value-kind fact (:1346-1347, :1362-1363): a meta wrap around a NON-model value
            // presents as present-or-absent, never as hasData(). Empty - no meta value at all - is not `false`.
            boolean metaWrappingBasic = prop.metaValueIsRosettaModelObject().map(v -> !v).orElse(false);
            if (prop.multi()) {
                if (model && !metaWrappingBasic && !pruningDisabled) {
                    body.append("\t\t\tif (").append(getter).append("()!=null && ").append(getter)
                            .append("().stream().filter(").append(T_OBJECTS)
                            .append("::nonNull).anyMatch(a->a.hasData())) return true;\n");
                } else {
                    body.append("\t\t\tif (").append(getter).append("()!=null && !").append(getter)
                            .append("().isEmpty()) return true;\n");
                }
            } else if (model) {
                if (metaWrappingBasic || pruningDisabled) {
                    body.append("\t\t\tif (").append(getter).append("()!=null) return true;\n");
                } else {
                    body.append("\t\t\tif (").append(getter).append("()!=null && ").append(getter)
                            .append("().hasData()) return true;\n");
                }
            } else {
                body.append("\t\t\tif (").append(getter).append("()!=null) return true;\n");
            }
        }
        body.append("\t\t\treturn false;\n");
        body.append("\t\t}\n");
        body.append("\t\n");   // :1353 - ONE tab again

        body.append("\t\t@SuppressWarnings(\"unchecked\")\n");                           // :1355-1398
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" merge(")
                .append(T_ROSETTA_MODEL_OBJECT_BUILDER).append(" other, ")
                .append(T_BUILDER_MERGER).append(" merger) {\n");
        if (builderExtended) {
            body.append("\t\t\tsuper.merge(other, merger);\n");
        }
        body.append("\t\t\t").append(builderRetType).append(" o = (").append(builderRetType).append(") other;\n");
        body.append("\t\t\t\n");
        for (IRPropertyModel.IRProperty prop : builderProps) {                          // :1372-1386, the MODEL half
            if (!itemIsModelObject(prop)) {
                continue;
            }
            String getter = getterName(prop);
            String target = prop.multi()
                    ? operationName("getOrCreate", prop.getterCompatibilityName(), "")
                    // :1381-1385 - the SETTER compatibility name (#323), never the getter's
                    : operationName("set", prop.setterCompatibilityName(), "");
            body.append("\t\t\tmerger.mergeRosetta(").append(getter).append("(), o.").append(getter)
                    .append("(), this::").append(target).append(");\n");
        }
        body.append("\t\t\t\n");
        for (IRPropertyModel.IRProperty prop : builderProps) {                          // :1389-1397, the BASIC half
            if (itemIsModelObject(prop)) {
                continue;
            }
            String getter = getterName(prop);
            if (prop.multi()) {
                body.append("\t\t\tmerger.mergeBasic(").append(getter).append("(), o.").append(getter)
                        .append("(), (").append(T_CONSUMER).append("<")
                        .append(valueSiteTypeRef(itemTypeOf(prop))).append(">) this::")
                        .append(operationName("add", prop.setterCompatibilityName(), "")).append(");\n");
            } else {
                body.append("\t\t\tmerger.mergeBasic(").append(getter).append("(), o.").append(getter)
                        .append("(), this::").append(operationName("set", prop.setterCompatibilityName(), ""))
                        .append(");\n");
            }
        }
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\n");   // :1398 - ONE tab, the third of them

        // :1400 - the BUILDER variant of section 13: the type's own simple name is still the cast's type, and
        // only the toString LABEL carries the "Builder" suffix
        body.append(reindent(boilerplate(typeName, typeName + "Builder", builderExtended, builderProps), "\t"));
        body.append("\t}\n");                                                           // :1402
        return body.toString();
    }

    /**
     * {@code hasIndexPropertyInScope} / {@code hasResultPropertyInScope} ({@code :1789-1805}) as ONE law, because
     * the old generator's two methods are byte-identical predicates over the same collection with a different
     * literal: the BUILDER CLASS SCOPE holds every property of the whole surface, own and inherited, and a
     * method-local or parameter whose desired name one of them claims escapes with a leading underscore. The
     * comparison is on the property name's first letter lower-cased, so a choice option {@code Index} claims
     * {@code index} exactly as an attribute {@code index} does.
     */
    static boolean hasPropertyInScope(List<IRPropertyModel.IRProperty> allProps, String desired) {
        return allProps.stream().anyMatch(p -> desired.equals(JavaNamingUtil.toFirstLower(p.name())));
    }

    /**
     * {@code builderFieldType} ({@code :2058-2069}) over the IR property. NOTE the difference from
     * {@link #builderGetterType}, which is the old generator's and not a slip: a list of model items is declared
     * {@code List<X.XBuilder>} as a FIELD and {@code List<? extends X.XBuilder>} as a GETTER return.
     */
    private String builderFieldType(IRPropertyModel.IRProperty prop) {
        if (prop.multi()) {
            return itemIsModelObject(prop)
                    ? T_LIST + "<" + dotQualifiedBuilderType(prop.javaType()) + ">"
                    : T_LIST + "<" + valueSiteTypeRef(itemTypeOf(prop)) + ">";
        }
        return builderSingleType(prop);
    }

    /**
     * The {@code getOrCreate} member of a MODEL-object builder property, in its two shapes
     * ({@code :1150-1204}): the LIST form takes the index parameter and lazily creates through {@code getIndex},
     * the SINGLE form assigns the field through a local. Each appends the scoped-key {@code addKey} line when the
     * property carries {@code [metadata location]} - and the two lines are NOT the same text: the list form calls
     * {@code getOrCreateMeta().addKey(...)} on the fresh builder ({@code :1177-1179}) while the single form calls
     * {@code getOrCreateMeta().toBuilder().addKey(...)} on the local ({@code :1199-1201}). Copied off both
     * appends, never reasoned from one of them.
     *
     * @param idxParam the {@code index} parameter's name, already escaped by the class-scope law
     * @param resLocal the {@code result} local's name, likewise
     */
    private void appendGetOrCreate(StringBuilder body, IRPropertyModel.IRProperty prop,
                                   String idxParam, String resLocal) {
        String getOrCreateName = operationName("getOrCreate", prop.getterCompatibilityName(), "");
        String singleType = builderSingleType(prop);
        String field = fieldName(prop);
        String itemType = valueSiteTypeRef(itemTypeOf(prop));
        boolean scopedKey = hasScopedKey(prop);
        if (prop.multi()) {                                                             // :1150-1178
            String upper = JavaNamingUtil.toFirstUpper(field);
            body.append("\t\t@Override\n");
            body.append("\t\tpublic ").append(singleType).append(" ").append(getOrCreateName)
                    .append("(int ").append(idxParam).append(") {\n");
            body.append("\t\t\tif (").append(field).append("==null) {\n");
            body.append("\t\t\t\tthis.").append(field).append(" = new ").append(T_ARRAY_LIST).append("<>();\n");
            body.append("\t\t\t}\n");
            body.append("\t\t\treturn getIndex(").append(field).append(", ").append(idxParam).append(", () -> {\n");
            body.append("\t\t\t\t\t\t").append(singleType).append(" new").append(upper)
                    .append(" = ").append(itemType).append(".builder();\n");
            if (scopedKey) {
                body.append("\t\t\t\t\t\tnew").append(upper).append(".getOrCreateMeta().addKey(").append(T_KEY)
                        .append(".builder().setScope(\"DOCUMENT\"));\n");
            }
            body.append("\t\t\t\t\t\treturn new").append(upper).append(";\n");
            body.append("\t\t\t\t\t});\n");
            body.append("\t\t}\n");
            body.append("\t\t\n");
        } else {                                                                        // :1179-1204
            body.append("\t\t@Override\n");
            body.append("\t\tpublic ").append(singleType).append(" ").append(getOrCreateName).append("() {\n");
            body.append("\t\t\t").append(singleType).append(" ").append(resLocal).append(";\n");
            body.append("\t\t\tif (").append(field).append("!=null) {\n");
            body.append("\t\t\t\t").append(resLocal).append(" = ").append(field).append(";\n");
            body.append("\t\t\t}\n");
            body.append("\t\t\telse {\n");
            body.append("\t\t\t\t").append(resLocal).append(" = ").append(field).append(" = ")
                    .append(itemType).append(".builder();\n");
            if (scopedKey) {
                body.append("\t\t\t\t").append(resLocal).append(".getOrCreateMeta().toBuilder().addKey(")
                        .append(T_KEY).append(".builder().setScope(\"DOCUMENT\"));\n");
            }
            body.append("\t\t\t}\n");
            body.append("\t\t\t\n");
            body.append("\t\t\treturn ").append(resLocal).append(";\n");
            body.append("\t\t}\n");
            body.append("\t\t\n");
        }
    }

    /**
     * {@code generateSingleSetter} ({@code :1440-1469}) - the SETTER annotation stack and the one-line body. Note
     * what the stack has that the LIST stacks do not: {@code @RuneMetaType} ({@code :1450}); and note the
     * {@code @Override} is the LAST annotation, below the Rune ones, which is the opposite of the getter stack's
     * order. Both are copied off the appends.
     */
    private void appendSingleSetter(StringBuilder body, IRPropertyModel.IRProperty prop, String builderRetType) {
        String setterName = operationName("set", prop.setterCompatibilityName(), "");
        String itemType = valueSiteTypeRef(itemTypeOf(prop));
        String field = fieldName(prop);
        body.append("\t\t@").append(T_ROSETTA_ATTRIBUTE).append("(\"").append(prop.name()).append("\")\n");
        body.append("\t\t@").append(T_ACCESSOR).append("(").append(T_ACCESSOR_TYPE).append(".SETTER)\n");
        if (prop.required()) {
            body.append("\t\t@").append(T_REQUIRED).append("\n");
        }
        body.append("\t\t@").append(T_RUNE_ATTRIBUTE).append("(\"").append(prop.name()).append("\")\n");
        if (isMetaProperty(prop)) {
            body.append("\t\t@").append(T_RUNE_META_TYPE).append("\n");
        }
        if (hasScopedReference(prop)) {
            body.append("\t\t@").append(T_RUNE_SCOPED_REF).append("\n");
        }
        if (hasScopedKey(prop)) {
            body.append("\t\t@").append(T_RUNE_SCOPED_KEY).append("\n");
        }
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ").append(setterName)
                .append("(").append(itemType).append(" _").append(field).append(") {\n");
        body.append("\t\t\tthis.").append(field).append(" = _").append(field)
                .append(" == null ? null : _").append(field)
                .append(itemIsModelObject(prop) ? ".toBuilder()" : "").append(";\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");
    }

    /**
     * {@code generateMetaValueSetter} ({@code :1668-1697}) - the SINGLE meta-value setter. It passes the value
     * STRAIGHT through: no {@code .toBuilder()} suffix, unlike the LIST meta-value setters
     * ({@link #appendListMetaValueSettersSingle}), because the meta wrapper's own {@code setValue} converts. The
     * parameter is {@code "_"} plus the FIELD identifier, not the raw name - which is the opposite of the list
     * arms' choice, and is again copied rather than unified.
     */
    private void appendMetaValueSetter(StringBuilder body, IRPropertyModel.IRProperty prop, String builderRetType) {
        String paramName = "_" + fieldName(prop);
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ")
                .append(operationName("set", prop.setterCompatibilityName(), "Value"))
                .append("(").append(metaValueTypeName(prop)).append(" ").append(paramName).append(") {\n");
        body.append("\t\t\tthis.").append(operationName("getOrCreate", prop.getterCompatibilityName(), ""))
                .append("().setValue(").append(paramName).append(");\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");
    }

    /**
     * {@code generateListSetters} ({@code :1471-1596}) - the whole LIST setter family, in the old generator's own
     * order: {@code add(<I>)}, {@code add(<I>, int idx)}, the single meta-value pair when the property carries a
     * value type, {@code add(<LP>)}, {@code set(<LP>)}, and the bulk meta-value pair.
     *
     * <p>THE PLURAL PARAMETER ({@code :1499}): the property's RAW name, first letter lower-cased, plus {@code "s"},
     * keyword-escaped AS A WHOLE - so a keyword attribute {@code new} pluralises to {@code news} (not a keyword,
     * so bare) while {@code extend} pluralises to the keyword {@code extends} and escapes to {@code _extends}.
     * It then escapes AGAIN, with a leading underscore, when it names a SIBLING property's field identifier. The
     * parameter's OWN field can never equal its plural, so only genuine sibling collisions escape.
     */
    private void appendListSetters(StringBuilder body, IRPropertyModel.IRProperty prop, String builderRetType,
                                   java.util.Set<String> siblingFieldNames) {
        String addName = operationName("add", prop.setterCompatibilityName(), "");
        String setterName = operationName("set", prop.setterCompatibilityName(), "");
        String itemType = valueSiteTypeRef(itemTypeOf(prop));
        boolean isModel = itemIsModelObject(prop);
        String toBuilderSuffix = isModel ? ".toBuilder()" : "";
        String field = fieldName(prop);
        String pluralParam = JavaNamingUtil.escapeJavaKeyword(JavaNamingUtil.toFirstLower(prop.name()) + "s");
        if (siblingFieldNames.contains(pluralParam)) {
            pluralParam = "_" + pluralParam;
        }
        String listParam = isModel
                ? T_LIST + "<? extends " + itemType + ">"
                : T_LIST + "<" + itemType + ">";

        body.append("\t\t@").append(T_ROSETTA_ATTRIBUTE).append("(\"").append(prop.name()).append("\")\n");
        body.append("\t\t@").append(T_ACCESSOR).append("(").append(T_ACCESSOR_TYPE).append(".ADDER)\n");
        if (prop.required()) {
            body.append("\t\t@").append(T_REQUIRED).append("\n");
        }
        body.append("\t\t@").append(T_MULTI).append("\n");
        body.append("\t\t@").append(T_RUNE_ATTRIBUTE).append("(\"").append(prop.name()).append("\")\n");
        if (hasScopedReference(prop)) {
            body.append("\t\t@").append(T_RUNE_SCOPED_REF).append("\n");
        }
        if (hasScopedKey(prop)) {
            body.append("\t\t@").append(T_RUNE_SCOPED_KEY).append("\n");
        }
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ").append(addName)
                .append("(").append(itemType).append(" _").append(field).append(") {\n");
        body.append("\t\t\tif (_").append(field).append(" != null) {\n");
        body.append("\t\t\t\tthis.").append(field).append(".add(_").append(field)
                .append(toBuilderSuffix).append(");\n");
        body.append("\t\t\t}\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ").append(addName)
                .append("(").append(itemType).append(" _").append(field).append(", int idx) {\n");
        body.append("\t\t\tgetIndex(this.").append(field).append(", idx, () -> _").append(field)
                .append(toBuilderSuffix).append(");\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        if (prop.metaValueType().isPresent()) {                                         // :1540-1542
            appendListMetaValueSettersSingle(body, prop, builderRetType);
        }

        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ").append(addName)
                .append("(").append(listParam).append(" ").append(pluralParam).append(") {\n");
        body.append("\t\t\tif (").append(pluralParam).append(" != null) {\n");
        body.append("\t\t\t\tfor (final ").append(itemType).append(" toAdd : ").append(pluralParam).append(") {\n");
        body.append("\t\t\t\t\tthis.").append(field).append(".add(toAdd").append(toBuilderSuffix).append(");\n");
        body.append("\t\t\t\t}\n");
        body.append("\t\t\t}\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        body.append("\t\t@").append(T_ROSETTA_ATTRIBUTE).append("(\"").append(prop.name()).append("\")\n");
        body.append("\t\t@").append(T_ACCESSOR).append("(").append(T_ACCESSOR_TYPE).append(".SETTER)\n");
        if (prop.required()) {
            body.append("\t\t@").append(T_REQUIRED).append("\n");
        }
        body.append("\t\t@").append(T_MULTI).append("\n");
        body.append("\t\t@").append(T_RUNE_ATTRIBUTE).append("(\"").append(prop.name()).append("\")\n");
        if (hasScopedReference(prop)) {
            body.append("\t\t@").append(T_RUNE_SCOPED_REF).append("\n");
        }
        if (hasScopedKey(prop)) {
            body.append("\t\t@").append(T_RUNE_SCOPED_KEY).append("\n");
        }
        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ").append(setterName)
                .append("(").append(listParam).append(" ").append(pluralParam).append(") {\n");
        body.append("\t\t\tif (").append(pluralParam).append(" == null) {\n");
        body.append("\t\t\t\tthis.").append(field).append(" = new ").append(T_ARRAY_LIST).append("<>();\n");
        body.append("\t\t\t} else {\n");
        body.append("\t\t\t\tthis.").append(field).append(" = ").append(pluralParam).append(".stream()\n");
        if (isModel) {
            body.append("\t\t\t\t\t.map(_a->_a.toBuilder())\n");
        }
        // :1583 - the bare `new ArrayList<>()` inside toCollection is LITERAL text in the released template, never
        // a scoped type write: copied, not corrected (see the T_ARRAY_LIST note above)
        body.append("\t\t\t\t\t.collect(").append(T_COLLECTORS).append(".toCollection(()->new ArrayList<>()));\n");
        body.append("\t\t\t}\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        if (prop.metaValueType().isPresent()) {                                         // :1594-1596
            appendListMetaValueSettersBulk(body, prop, builderRetType);
        }
    }

    /**
     * {@code generateListMetaValueSettersSingle} ({@code :1598-1637}) - {@code addValue(<V>)} and
     * {@code addValue(<V>, int idx)}, both delegating to {@code getOrCreate(-1|idx).setValue(...)}. TWO details
     * that are the old generator's and are copied rather than harmonised: the parameter is {@code "_"} plus the
     * RAW property name (not the field identifier the single-cardinality setters use), and the
     * {@code .toBuilder()} suffix is written iff the RECONCILED {@code metaValueIsRosettaModelObject} fact is
     * TRUE.
     *
     * <p>The fact is read with {@code orElseThrow}, not with a default: the caller has already tested that the
     * property HAS a meta value type, so the empty case is unreachable - and stating it is better than defaulting
     * it, because a silent {@code false} would drop a suffix the golden has.
     */
    private void appendListMetaValueSettersSingle(StringBuilder body, IRPropertyModel.IRProperty prop,
                                                  String builderRetType) {
        String valueTypeName = metaValueTypeName(prop);
        String addValueName = operationName("add", prop.setterCompatibilityName(), "Value");
        String getOrCreateName = operationName("getOrCreate", prop.getterCompatibilityName(), "");
        String rawName = prop.name();
        boolean isModelValue = prop.metaValueIsRosettaModelObject().orElseThrow(() -> new MissingIRFact(
                "property." + prop.name() + ".metaValueIsRosettaModelObject",
                "the list meta-value setter arm was reached for a property whose surface states no meta-value"
                        + " kind at all, which means it carries no meta value type - the arm is refused rather"
                        + " than defaulted, because a silent `false` writes a `.toBuilder()` the golden has not"));
        String toBuilderSuffix = isModelValue ? ".toBuilder()" : "";

        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ").append(addValueName)
                .append("(").append(valueTypeName).append(" _").append(rawName).append(") {\n");
        body.append("\t\t\tthis.").append(getOrCreateName).append("(-1).setValue(_").append(rawName)
                .append(toBuilderSuffix).append(");\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ").append(addValueName)
                .append("(").append(valueTypeName).append(" _").append(rawName).append(", int idx) {\n");
        body.append("\t\t\tthis.").append(getOrCreateName).append("(idx).setValue(_").append(rawName)
                .append(toBuilderSuffix).append(");\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");
    }

    /**
     * {@code generateListMetaValueSettersBulk} ({@code :1639-1666}) - {@code addValue(List<? extends <V>>)} and
     * {@code setXxxValue(List<? extends <V>>)}. The parameter is the RAW property name plus {@code "s"},
     * UNESCAPED (no keyword escape, no sibling escape - the old generator writes it bare here, unlike the
     * {@code add(List)} / {@code set(List)} plural), and the {@code clear()} call likewise references the RAW
     * name rather than the field identifier. Both are copied off {@code :1653} and {@code :1661}.
     */
    private void appendListMetaValueSettersBulk(StringBuilder body, IRPropertyModel.IRProperty prop,
                                                String builderRetType) {
        String valueTypeName = metaValueTypeName(prop);
        String addValueName = operationName("add", prop.setterCompatibilityName(), "Value");
        String setValueName = operationName("set", prop.setterCompatibilityName(), "Value");
        String rawName = prop.name();

        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ").append(addValueName)
                .append("(").append(T_LIST).append("<? extends ").append(valueTypeName).append("> ")
                .append(rawName).append("s) {\n");
        body.append("\t\t\tif (").append(rawName).append("s != null) {\n");
        body.append("\t\t\t\tfor (final ").append(valueTypeName).append(" toAdd : ").append(rawName).append("s) {\n");
        body.append("\t\t\t\t\tthis.").append(addValueName).append("(toAdd);\n");
        body.append("\t\t\t\t}\n");
        body.append("\t\t\t}\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");

        body.append("\t\t@Override\n");
        body.append("\t\tpublic ").append(builderRetType).append(" ").append(setValueName)
                .append("(").append(T_LIST).append("<? extends ").append(valueTypeName).append("> ")
                .append(rawName).append("s) {\n");
        body.append("\t\t\tthis.").append(rawName).append(".clear();\n");
        body.append("\t\t\tif (").append(rawName).append("s != null) {\n");
        body.append("\t\t\t\t").append(rawName).append("s.forEach(this::").append(addValueName).append(");\n");
        body.append("\t\t\t}\n");
        body.append("\t\t\treturn this;\n");
        body.append("\t\t}\n");
        body.append("\t\t\n");
    }

    // ------------------------------------------------------------------------------------ 13 (commit 8's shape)

    /**
     * SECTION 13's POSITION appends NOTHING, and that is the old generator's own text. {@code buildBody} writes no
     * {@code equals} / {@code hashCode} / {@code toString} of its own: both copies live INSIDE sections 11 and 12,
     * appended by {@code generateImplClass} ({@code :1067}) and {@code generateBuilderImplClass} ({@code :1413})
     * from {@code ModelObjectBoilerplate}. So this positional section renders the empty string - the
     * {@link #compatMembers} precedent - and the TEXT LAW itself is {@link #boilerplate}, the method those two
     * sections call (v3.3 seat 9, PR #645 commit 8).
     */
    String equalsHashCodeToString() {
        return "";
    }

    /**
     * SECTION 13's TEXT LAW ({@code ModelObjectBoilerplate:33-59} -&gt; {@code :141-268}), byte for byte off
     * {@code contributeEquals}, {@code contributeHashCode} and {@code contributeToString}, appended in that order
     * with the {@code "\t\n"} separators exactly as those three methods write them.
     *
     * <p><b>WHY TWO NAMES AND NOT ONE.</b> The contract's sketch took a single {@code className}; the old
     * generator's own text needs BOTH, and this method states them rather than deriving one from the other by
     * stripping a suffix (which would be a string guess about a type called {@code FooBuilder}):
     * {@code contributeEquals} casts to {@code javaType.getSimpleName()} - the TYPE's simple name - in the
     * {@code Impl} variant AND in the builder variant ({@code :51-58} passes the same {@code javaType}), while
     * {@code contributeToString} labels with {@code getSimpleName()} for the {@code Impl} and
     * {@code getSimpleName() + "Builder"} for the builder ({@code :43} vs {@code :56}).
     *
     * @param simpleName  the TYPE's simple name - the {@code _that} cast's type in BOTH variants
     * @param className   the class the {@code toString} labels: {@code simpleName} for the {@code Impl} and
     *                    {@code simpleName + "Builder"} for the {@code BuilderImpl}
     * @param extended    the class extends its parent's - {@code implExtended} for the {@code Impl} and
     *                    {@code builderExtended} for the {@code BuilderImpl}; it decides the
     *                    {@code super.equals(o)} guard, the {@code _result} seed and the {@code super.toString()}
     *                    tail
     * @param properties  the class's OWN field list - {@code implProps} / {@code builderProps}, already narrowed
     *                    by the caller's extension fact
     */
    String boilerplate(String simpleName, String className, boolean extended,
                       List<IRPropertyModel.IRProperty> properties) {
        StringBuilder sb = new StringBuilder();
        // -- equals (ModelObjectBoilerplate:141-179)
        sb.append("\t@Override\n");
        sb.append("\tpublic boolean equals(Object o) {\n");
        sb.append("\t\tif (this == o) return true;\n");
        sb.append("\t\tif (o == null || !(o instanceof ").append(T_ROSETTA_MODEL_OBJECT)
                .append(") || !getType().equals(((").append(T_ROSETTA_MODEL_OBJECT)
                .append(")o).getType())) return false;\n");
        if (extended) {
            sb.append("\t\tif (!super.equals(o)) return false;\n");
        }
        sb.append("\t\n");
        if (!properties.isEmpty()) {
            sb.append("\t\t").append(simpleName).append(" _that = getType().cast(o);\n");
            sb.append("\t\n");
        } else {
            sb.append("\t\n");   // :157-161 - a property-less class keeps the separator the FOR loop would have
        }
        for (IRPropertyModel.IRProperty prop : properties) {
            if (prop.multi()) {
                sb.append("\t\tif (!").append(T_LIST_EQUALS).append(".listEquals(").append(fieldName(prop))
                        .append(", _that.").append(getterName(prop)).append("())) return false;\n");
            } else {
                sb.append("\t\tif (!").append(T_OBJECTS).append(".equals(").append(fieldName(prop))
                        .append(", _that.").append(getterName(prop)).append("())) return false;\n");
            }
        }
        sb.append("\t\treturn true;\n");
        sb.append("\t}\n");
        sb.append("\t\n");

        // -- hashCode (ModelObjectBoilerplate:182-213)
        sb.append("\t@Override\n");
        sb.append("\tpublic int hashCode() {\n");
        sb.append("\t\tint _result = ").append(extended ? "super.hashCode()" : "0").append(";\n");
        for (IRPropertyModel.IRProperty prop : properties) {
            String field = fieldName(prop);
            // :205-206 - the RECONCILED item-is-enum fact, never a test on the rendered type name
            if (prop.itemIsEnum()) {
                if (prop.multi()) {
                    sb.append("\t\t_result = 31 * _result + (").append(field)
                            .append(" != null ? ").append(field)
                            .append(".stream().map(Object::getClass).map(Class::getName)")
                            .append(".mapToInt(String::hashCode).sum() : 0);\n");
                } else {
                    sb.append("\t\t_result = 31 * _result + (").append(field)
                            .append(" != null ? ").append(field)
                            .append(".getClass().getName().hashCode() : 0);\n");
                }
            } else {
                sb.append("\t\t_result = 31 * _result + (").append(field)
                        .append(" != null ? ").append(field).append(".hashCode() : 0);\n");
            }
        }
        sb.append("\t\treturn _result;\n");
        sb.append("\t}\n");
        sb.append("\t\n");

        // -- toString (ModelObjectBoilerplate:216-249)
        sb.append("\t@Override\n");
        sb.append("\tpublic String toString() {\n");
        sb.append("\t\treturn \"").append(className).append(" {\" +\n");
        for (int i = 0; i < properties.size(); i++) {
            IRPropertyModel.IRProperty prop = properties.get(i);
            // :234-238 - the LABEL is the property's own name (PascalCase for a choice option); only the FIELD
            // reference is lower-cased and keyword-escaped
            sb.append("\t\t\t\"").append(prop.name()).append("=\" + this.").append(fieldName(prop));
            sb.append(i < properties.size() - 1 ? " + \", \" +\n" : " +\n");
        }
        sb.append("\t\t'}'");
        if (extended) {
            sb.append(" + \" \" + super.toString()");
        }
        sb.append(";\n");
        sb.append("\t}\n");
        return sb.toString();
    }

    // ------------------------------------------------------------------------------------------- the small laws

    /** The declaring namespace, escaped, with dots - the POJO's own package ({@code pojo.getPackageName()}). */
    static String packageOf(IRTypeNode node) {
        return JavaPackageName.escape(DottedPath.splitOnDots(IRTypeUnit.namespaceOf(node))).getName().withDots();
    }

    /** The supertype's canonical Java name, through the index's RECONCILED parent - never a name built from a string. */
    String superCanonical(IRTypeNode node) {
        IRTypeNode parent = index.parent(node.baseType().orElseThrow(() -> new GenerationException(
                "IR data-type emitter: " + node.name() + " has no supertype", null, null)));
        return packageOf(parent) + "." + IRTypeUnit.simpleName(parent);
    }

    /**
     * {@code superFqnInlined} ({@code :485-490}): the supertype's nested refs are FQN-inlined, and its import
     * suppressed, when its SIMPLE name collides with {@code java.lang} AND it lives in a DIFFERENT package.
     */
    private boolean superFqnInlined(IRTypeNode node, String packageName) {
        if (node.baseType().isEmpty()) {
            return false;
        }
        String canonical = superCanonical(node);
        return IRJavaLangCollision.collides(simpleOf(canonical))
                && !packageOfCanonical(canonical).equals(packageName);
    }

    /** {@code isMetaProperty} ({@code :1841-1844}): the synthetic {@code meta} whose item is {@code MetaFields}. */
    private static boolean isMetaProperty(IRPropertyModel.IRProperty prop) {
        return "meta".equals(prop.name()) && "MetaFields".equals(simpleOf(itemTypeOf(prop)));
    }

    /**
     * {@code valueSiteTypeRef} ({@code :1887-1900}): a MODEL type whose simple name collides with {@code java.lang}
     * is written fully qualified; every other CLASS is a first-claim sentinel; anything that is not a dotted class
     * name keeps its own spelling.
     */
    static String valueSiteTypeRef(String itemFqn) {
        String simple = simpleOf(itemFqn);
        if (isClassName(itemFqn)) {
            if (!itemFqn.startsWith("java.lang.") && IRJavaLangCollision.collides(simple)) {
                return itemFqn;
            }
            return ImportCollisionResolver.typeRefOrBare(itemFqn);
        }
        return simple;
    }

    /**
     * {@code JavaTypeUtil.isRosettaModelObject(prop.getType())} ({@code ModelObjectGenerator.isModelObj},
     * {@code :1846-1848}) - READ OFF THE PROPERTY, never inferred from its rendered spelling (v3.3 seat 9, PR #645
     * commit 5 - the gate commit closed what commit 4 named as gap 1). The fact is derived in
     * {@link IRPropertyModel} from {@link com.regnosys.rosetta.ir.core.IRType#kind()} on the declaration behind the
     * ITEM, and RECONCILED per property against the old generator's own answer through that generator's seam
     * ({@code property.<name>.itemIsRosettaModelObject}) - so the arms commit 4 hard-coded here (a
     * {@code java.*} item, a {@code com.rosetta.model.metafields.*} item, every other
     * {@code com.rosetta.model.lib.*} item) are GONE: a package-prefix test on a rendered name was a decision this
     * emitter had no right to make, and the fact now carries the answer for every property including those.
     */
    static boolean itemIsModelObject(IRPropertyModel.IRProperty prop) {
        return prop.itemIsRosettaModelObject();
    }

    /**
     * The property's ITEM type, as the rendering law spells it: the element of a list-wrapped render, the render
     * itself otherwise. {@link IRPropertyModel#itemOf} unwraps unconditionally (its callers already know the
     * property is a list), so the {@code multi} guard is this emitter's to state.
     */
    static String itemTypeOf(IRPropertyModel.IRProperty prop) {
        return itemTypeOf(prop.javaType());
    }

    /**
     * The same law over a RENDERED TYPE alone (v3.3 seat 9, PR #645 commit 10) - what an ANCESTOR RUNG's item
     * is, read from the rung's own spelling. {@link IRPropertyModel#itemOf} unwraps unconditionally, so the
     * list guard is this emitter's to state, once, for both entries.
     */
    static String itemTypeOf(String rendered) {
        return IRPropertyModel.isList(rendered) ? IRPropertyModel.itemOf(rendered) : rendered;
    }

    private static boolean allMatchCompatible(List<IRPropertyModel.IRProperty> ownProps) {
        return ownProps.stream().allMatch(IRPropertyModel.IRProperty::compatibleTypeWithParent);
    }

    /**
     * {@code implExtended} ({@code ModelObjectGenerator:145-146}) - declared ONCE (v3.3 seat 9, PR #645 commit 8)
     * because THREE surfaces read it: section 1's import set ({@link #imports}), section 1's static imports
     * ({@link #staticImports}) and section 11 itself ({@link #implClass}). The {@code Impl} extends its parent's
     * only when the type HAS a parent and every OWN property is a subtype-compatible specialization of the
     * parent's - a type-changing override would make the inherited field's type conflict.
     */
    static boolean implExtended(IRTypeNode node, IRPropertyModel properties) {
        return node.baseType().isPresent() && allMatchCompatible(properties.ownProperties());
    }

    /**
     * {@code builderExtended} ({@code ModelObjectGenerator:147-148}), declared once beside
     * {@link #implExtended} (v3.3 seat 9, PR #645 commit 8): the {@code BuilderImpl} extends its parent's only
     * when every OWN property has the IDENTICAL type as its parent's - any type change at all, covariant or not,
     * would make the inherited builder field conflict. Section 1's import set reads it; section 12 will, and
     * section 13's BUILDER variant is held against the old generator's own bytes through it.
     */
    static boolean builderExtended(IRTypeNode node, IRPropertyModel properties) {
        return node.baseType().isPresent() && allMatchSameType(properties.ownProperties());
    }

    /**
     * {@code fieldName} ({@code ModelObjectGenerator:1678-1701}) - the Java FIELD identifier of a property: its
     * name with the first letter lower-cased (a choice option's name is PascalCase and its field is not) and then
     * keyword-escaped ({@code new} becomes {@code _new}). The {@code toString} LABEL keeps the raw name; only the
     * field reference goes through this.
     */
    static String fieldName(IRPropertyModel.IRProperty prop) {
        return JavaNamingUtil.escapeJavaKeyword(JavaNamingUtil.toFirstLower(prop.name()));
    }

    /** {@code hasScopedReference} ({@code :1917-1920}) over the IR property's own meta-type list. */
    private static boolean hasScopedReference(IRPropertyModel.IRProperty prop) {
        return prop.attributeMetaTypes().contains("SCOPED_REFERENCE");
    }

    /** {@code hasScopedKey} ({@code :1922-1925}) over the IR property's own meta-type list. */
    private static boolean hasScopedKey(IRPropertyModel.IRProperty prop) {
        return prop.attributeMetaTypes().contains("SCOPED_KEY");
    }

    /**
     * {@code reindent} ({@code ModelObjectGenerator:726-732}): every NON-EMPTY line of the block prefixed, the
     * lines rejoined with {@code \n}, and the trailing newline kept when the block had one. An EMPTY line keeps
     * its emptiness - which is why a {@code "\t\n"} separator (a TAB, not an empty line) becomes {@code "\t\t\n"}
     * and a bare {@code "\n"} stays bare.
     */
    private static String reindent(String code, String extraIndent) {
        List<String> lines = new ArrayList<>();
        for (String line : code.lines().toList()) {
            lines.add(line.isEmpty() ? line : extraIndent + line);
        }
        return String.join("\n", lines) + (code.endsWith("\n") ? "\n" : "");
    }

    private static boolean allMatchSameType(List<IRPropertyModel.IRProperty> ownProps) {
        return ownProps.stream().allMatch(IRPropertyModel.IRProperty::sameTypeAsParent);
    }

    /** {@code indentJavadoc} ({@code :657-661}): every line of the block prefixed, joined without a trailing newline. */
    private static String indent(String block, String prefix) {
        List<String> lines = new ArrayList<>();
        for (String line : block.lines().toList()) {
            lines.add(prefix + line);
        }
        return String.join("\n", lines);
    }

    /** A dotted class name - the rendering law's own test: a parameterized or bare spelling is not one. */
    private static boolean isClassName(String rendered) {
        return rendered.indexOf('.') >= 0 && rendered.indexOf('<') < 0 && rendered.indexOf('[') < 0;
    }

    private static String simpleOf(String canonical) {
        int dot = canonical.lastIndexOf('.');
        return dot >= 0 ? canonical.substring(dot + 1) : canonical;
    }

    private static String packageOfCanonical(String canonical) {
        int dot = canonical.lastIndexOf('.');
        return dot >= 0 ? canonical.substring(0, dot) : "";
    }

    // ------------------------------------------------------------------------------ the template's view (getters)

    /** The file frame's view for {@code ir-java-pojo.stg} - the shape {@code PojoTemplateModel} carries. */
    public static final class PojoFile {
        private final String packageName;
        private final List<String> imports;
        private final List<String> staticImports;
        private final String body;

        PojoFile(String packageName, List<String> imports, List<String> staticImports, String body) {
            this.packageName = packageName;
            this.imports = List.copyOf(imports);
            this.staticImports = List.copyOf(staticImports);
            this.body = body;
        }

        public String getPackageName() { return packageName; }
        public List<String> getImports() { return imports; }
        public List<String> getStaticImports() { return staticImports; }
        public String getBody() { return body; }
    }
}
