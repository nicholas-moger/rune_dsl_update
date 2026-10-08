package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.scoping.JavaPackageName;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;
import com.regnosys.rosetta.ir.core.IRKind;
import com.rosetta.util.DottedPath;

/**
 * THE TYPE UNIT (v3.3 seat 9, PR #645 - the emitter PR, the STRICT path): a data type's SIX generated files emitted
 * TOGETHER from {@link IRTypeNode} alone, or the type REFUSED BY NAME and all six left to the old generator. There is
 * NO per-file fallback: the user's order of 2026-09-18 ("A the type gate + B the property gate then ONE
 * all-or-nothing emitter PR, NO per-file fallback") is this class's whole reason to exist, and the amendment of
 * 2026-09-18 ~22:50 ("the emitter PR emits the {@code type} file AND its per-type DERIVED files as ONE unit") is what
 * fixes the six members below.
 *
 * <p><b>THE LAW.</b> A unit is AVAILABLE only when every one of the six {@link Member}s is READY. Until then
 * {@link #verdict} REFUSES - a {@link Refusal} naming the type's qualified name and every member that is not ready -
 * and the caller writes nothing, so the old generator's six files stand and the fallback register's DATA_TYPE rows
 * are unmoved. A member that THROWS while the unit is emitting refuses the WHOLE unit for the same reason: a partial
 * map is never returned, because a map with five of six entries would let the IR route write a type's POJO beside the
 * old generator's validators - exactly the per-file fallback the order forbids.
 *
 * <p><b>THE MEMBER CONTRACT</b> (v3.3 seat 9, PR #645 commit 12; the planning review's Q3(a)).
 * {@link MemberEmitter#emit} answers {@code Optional<String>}:
 * <ul>
 *   <li>a PRESENT text is the member's file and is written at {@link #outputKey};</li>
 *   <li>an EMPTY answer means NO FILE BY LAW - the type's facts say this member writes nothing - and is allowed for
 *       {@link Member#DEEP_PATH_UTIL} ALONE (its eligibility fact). Every other member answering empty is a
 *       REFUSAL BY NAME: a member that has a file for every type and suddenly has none is a bug, and booking it as
 *       "no file" would make the unit write five files where the old generator writes six;</li>
 *   <li>a THROW is a refusal, whatever it is.</li>
 * </ul>
 * The {@link Verdict} therefore records the PRESENT members' texts and the NOT-APPLICABLE members separately, and
 * {@link #emit} writes only the present members' keys. An absent file is never inferred from an absent key: the D11
 * host's projection asks the unit which members were not applicable (Q3(b)'s three outcomes).
 *
 * <p><b>THE MEMO</b> is per NODE, by IDENTITY (the index hands out ONE node per declaration), and lives on the unit
 * INSTANCE - which {@code unitFor(version)} constructs per {@code generateClassesAsIR} call, i.e. per model per pass.
 * There is no cross-model memo and no provider-level cache. Within one unit every kind's ask for a type therefore
 * gets the SAME verdict - the all-or-nothing by construction. ACROSS the six passes the verdicts are equal only if
 * every member's emit is a PURE function of the node, which is the emitters' whole contract; that is ASSERTED, not
 * assumed, by the D11 host's {@code TYPE UNIT VERDICTS} line, which holds the six passes' ATTEMPTED and REFUSED type
 * NAME SETS equal (by name, so a pass that silently omits a type is caught).
 *
 * <p><b>THE INSPECTION PATH.</b> {@link #inspect} runs ONE ready member's emitter over the same construction the
 * production path takes, writes nothing, memoises nothing, and does NOT require the unit to be available. It exists
 * for the D11 SHADOW lines alone: {@link #verdict} refuses every type while a member is missing, so a shadow built on
 * it would measure nothing until the last member lands, which is the whole measurement the strict path needs before
 * it lands.
 *
 * <p><b>THE OUTPUT KEYS</b> are the legacy convention, one per member, as {@code JavaClassGenerator.generateClasses}
 * computes it ({@code rune-java-generator/.../java/JavaClassGenerator.java:55-78}):
 * {@code typeRepresentation.getCanonicalName().withForwardSlashes() + ".java"} - the ESCAPED package as directories,
 * then the simple name with the member's suffix. The six canonical names are
 * {@code JavaTypeTranslator}'s own ({@code :236-290}, {@code :448-465}):
 * <ul>
 *   <li>{@link Member#POJO} - {@code <ns>/<Simple>.java}</li>
 *   <li>{@link Member#TYPE_FORMAT_VALIDATOR} - {@code <ns>/validation/<Simple>TypeFormatValidator.java}</li>
 *   <li>{@link Member#CARDINALITY_VALIDATOR} - {@code <ns>/validation/<Simple>Validator.java}</li>
 *   <li>{@link Member#ONLY_EXISTS_VALIDATOR} - {@code <ns>/validation/exists/<Simple>OnlyExistsValidator.java}</li>
 *   <li>{@link Member#META} - {@code <ns>/meta/<Simple>Meta.java}</li>
 *   <li>{@link Member#DEEP_PATH_UTIL} - {@code <ns>/util/<Simple>DeepPathUtil.java}</li>
 * </ul>
 * Each sub-package is escaped as a whole ({@code JavaPackageName.escape}), which is what the translator does before
 * it takes the canonical name.
 */
public final class IRTypeUnit {

    /** The six files a data type owns. The unit is AVAILABLE only when every one of them is READY. */
    public enum Member {
        /** The {@code type} POJO - interface, {@code Impl}, {@code Builder}, {@code BuilderImpl}. */
        POJO(List.of(), ""),
        /** {@code <ns>.validation.<Simple>TypeFormatValidator}. */
        TYPE_FORMAT_VALIDATOR(List.of("validation"), "TypeFormatValidator"),
        /** {@code <ns>.validation.<Simple>Validator} - the CARDINALITY validator, whose suffix is bare. */
        CARDINALITY_VALIDATOR(List.of("validation"), "Validator"),
        /** {@code <ns>.validation.exists.<Simple>OnlyExistsValidator}. */
        ONLY_EXISTS_VALIDATOR(List.of("validation", "exists"), "OnlyExistsValidator"),
        /** {@code <ns>.meta.<Simple>Meta}. */
        META(List.of("meta"), "Meta"),
        /** {@code <ns>.util.<Simple>DeepPathUtil}. */
        DEEP_PATH_UTIL(List.of("util"), "DeepPathUtil");

        private final List<String> subPackage;
        private final String suffix;

        Member(List<String> subPackage, String suffix) {
            this.subPackage = subPackage;
            this.suffix = suffix;
        }

        /** The sub-package segments the member's file lives under, below the declaring namespace. */
        public List<String> subPackage() {
            return subPackage;
        }

        /** The simple-name suffix the member's class carries. */
        public String suffix() {
            return suffix;
        }

        /**
         * Whether this member may answer "NO FILE BY LAW" ({@link Optional#empty()}). The deep-path util alone may:
         * a type its eligibility fact refuses writes no util and the corpus holds no golden for one. Every other
         * member writes a file for every validated type, so an empty answer from one of them is a refusal.
         */
        public boolean mayWriteNoFile() {
            return this == DEEP_PATH_UTIL;
        }
    }

    /**
     * ONE member's emitter: the file it would write for a node, from the IR alone. A member that cannot write the
     * node THROWS - it never returns a guess, and its throw refuses the WHOLE unit. An EMPTY answer is NOT a throw:
     * it is the claim that this member writes NO FILE for this type BY LAW, and only {@link Member#DEEP_PATH_UTIL}
     * may make it.
     */
    public interface MemberEmitter {
        /** The file's text, from the IR alone, or {@link Optional#empty()} for "no file by law". */
        Optional<String> emit(IRTypeNode node);
    }

    /**
     * The unit's named refusal: the type's qualified name and every member that is not ready (or the member that
     * threw). Carries the POJO's output key as its target path, so the host's error gate names a file.
     */
    public static final class Refusal extends GenerationException {
        private static final long serialVersionUID = 1L;

        Refusal(String message, Throwable cause) {
            super(message, null, null, cause);
        }
    }

    /**
     * ONE type's whole answer: the texts of the members that HAVE a file, and by name the members that have NONE by
     * law. The two together always cover all six members, so a consumer never infers absence from a missing key.
     */
    public static final class Verdict {
        private final Map<Member, String> texts;
        private final Set<Member> notApplicable;

        Verdict(Map<Member, String> texts, Set<Member> notApplicable) {
            // built key by key, never through EnumMap/EnumSet.copyOf: those refuse an EMPTY source collection whose
            // element type they cannot infer, and both halves of a verdict are legitimately empty
            EnumMap<Member, String> copiedTexts = new EnumMap<>(Member.class);
            copiedTexts.putAll(texts);
            EnumSet<Member> copiedNotApplicable = EnumSet.noneOf(Member.class);
            copiedNotApplicable.addAll(notApplicable);
            this.texts = Collections.unmodifiableMap(copiedTexts);
            this.notApplicable = Collections.unmodifiableSet(copiedNotApplicable);
        }

        /** The members that HAVE a file, and their texts. */
        public Map<Member, String> texts() {
            return texts;
        }

        /** The members that write NO FILE for this type by law. */
        public Set<Member> notApplicable() {
            return notApplicable;
        }

        /** One member's text, or empty when the member writes no file for this type. */
        public Optional<String> textOf(Member member) {
            return Optional.ofNullable(texts.get(member));
        }
    }

    private final Map<Member, MemberEmitter> ready;
    /** The wiring's own AVAILABLE switch - see the two-argument constructor. */
    private final boolean wiringAvailable;
    /** The wiring's KIND-SCOPED switch for the CHOICE kind - see the three-argument constructor. */
    private final boolean choiceAvailable;
    /**
     * THE PER-NODE MEMO, by IDENTITY. The value is a {@link Verdict} or a {@link Refusal} - never both, never a
     * partial map - so the second ask of a node cannot answer differently from the first within this unit.
     */
    private final Map<IRTypeNode, Object> memo = new IdentityHashMap<>();
    /** Every node the production path was asked for, by qualified name - the host's {@code attempted} set. */
    private final Set<String> attempted = new TreeSet<>();
    /** Every node the production path REFUSED, by qualified name - the host's {@code refused} set. */
    private final Set<String> refused = new TreeSet<>();

    /**
     * @param readyMembers the members the WIRING declares ready - the emitters that write from the IR alone. A member
     *     absent from this map is NOT ready, and every type is refused whole until all six are present.
     */
    public IRTypeUnit(Map<Member, MemberEmitter> readyMembers) {
        this(readyMembers, true);
    }

    /**
     * @param readyMembers the members the WIRING declares ready
     * @param wiringAvailable the WIRING'S OWN SWITCH (v3.3 seat 9, PR #645 commit 14): the unit is available only
     *     when every member is ready AND the wiring says so. The two are separate facts on purpose - the commit
     *     that declares the LAST member ready is not the commit that turns the route on, because switching the
     *     unit on also moves the register's DATA_TYPE rows to zero and the file meter, and a member's bytes are
     *     held against the golden on the full corpus BEFORE any of that (the strict path's whole shape). The
     *     one-argument constructor above passes {@code true}: a caller that builds a unit from an explicit member
     *     map - every test stub - is stating the member set and nothing else.
     */
    public IRTypeUnit(Map<Member, MemberEmitter> readyMembers, boolean wiringAvailable) {
        this(readyMembers, wiringAvailable, true);
    }

    /**
     * @param readyMembers the members the WIRING declares ready
     * @param wiringAvailable the WIRING'S OWN SWITCH, as the two-argument constructor above
     * @param choiceAvailable the WIRING'S KIND-SCOPED SWITCH for the CHOICE kind (v3.3 seat 10, PR #646 commit 4):
     *     the unit may write a CHOICE node only when it is available AND this is true. Three facts, on purpose -
     *     landing the POJO emitter's choice arm and the six choice shadow lines is not the same event as turning
     *     the choice route on, which also re-cuts the register's {@code CHOICE} rows and moves the file meter
     *     ({@link IRTypeUnitWiring#CHOICE_AVAILABLE}). The TWO-argument form above passes {@code true} here, as
     *     the ONE-argument form passes {@code true} for both: a caller that builds a unit from an explicit member
     *     map - every test stub - is stating the member set and nothing else, and a stub that silently lost the
     *     choice kind would make a member's own witness disagree with the wiring's for a reason no one wrote.
     *     {@link IRTypeUnitWiring#unitFor(Map)} is the ONE caller that passes the wiring's own two switches.
     */
    public IRTypeUnit(Map<Member, MemberEmitter> readyMembers, boolean wiringAvailable, boolean choiceAvailable) {
        Objects.requireNonNull(readyMembers, "readyMembers");
        this.wiringAvailable = wiringAvailable;
        this.choiceAvailable = choiceAvailable;
        EnumMap<Member, MemberEmitter> copy = new EnumMap<>(Member.class);
        for (Map.Entry<Member, MemberEmitter> entry : readyMembers.entrySet()) {
            copy.put(Objects.requireNonNull(entry.getKey(), "member"),
                    Objects.requireNonNull(entry.getValue(), "emitter"));
        }
        this.ready = Collections.unmodifiableMap(copy);
    }

    /** The members the wiring declared ready. */
    public Set<Member> ready() {
        return ready.keySet();
    }

    /**
     * THE READY MEMBERS' EMITTERS, BY MEMBER - package-private, and read for IDENTITY alone (v3.3 seat 10,
     * PR #646 commit 3). The per-pass emitter memo of {@link IRUnitPass} and {@link IRModelObjectGenerator}
     * claims that two units of ONE pass and ONE version stamp carry the SAME six emitter instances, and that
     * two stamps or two passes never do; a claim about instances can only be read off the instances. No
     * production caller: {@link #verdict} and {@link #inspect} reach the map directly.
     */
    Map<Member, MemberEmitter> emitters() {
        return ready;
    }

    /**
     * True iff EVERY member is ready AND the wiring's own {@code AVAILABLE} switch is on - the only state in which
     * {@link #emit} writes anything. TWO facts, ONE predicate (v3.3 seat 9, PR #645 commit 14).
     */
    public boolean available() {
        return ready.size() == Member.values().length && wiringAvailable;
    }

    /**
     * THE SAME PREDICATE, FOR ONE KIND (v3.3 seat 10, PR #646 commit 4): {@link #available()} AND, for
     * {@link IRKind#CHOICE} alone, the wiring's kind-scoped switch. Every other kind reads exactly
     * {@link #available()}, so a STRUCT node's answer is UNMOVED by this commit.
     *
     * <p>ONE PREDICATE, not two literals: {@link #verdict} refuses through THIS method, and the commit that flips
     * {@link IRTypeUnitWiring#CHOICE_AVAILABLE} is the commit that makes {@link IRUnitPass#routeWith} read it too.
     */
    public boolean available(IRKind kind) {
        Objects.requireNonNull(kind, "kind");
        return available() && (kind != IRKind.CHOICE || choiceAvailable);
    }

    /** Every type name the PRODUCTION path was asked for on this unit - the host's per-pass attempted set. */
    public Set<String> attemptedTypes() {
        return Set.copyOf(attempted);
    }

    /** Every type name the PRODUCTION path REFUSED on this unit - the host's per-pass refused set. */
    public Set<String> refusedTypes() {
        return Set.copyOf(refused);
    }

    /**
     * The type's whole verdict, memoised by node identity. ALL OR NOTHING: every ready member is asked, the texts are
     * held in a local map, and the verdict is handed back only when every one of them answered - so no caller can
     * ever see a partial unit.
     *
     * @throws Refusal when a member is not ready, when a ready member throws while rendering, or when a member that
     *     may not answer "no file" does
     */
    public Verdict verdict(IRTypeNode node) {
        Objects.requireNonNull(node, "node");
        Object held = memo.get(node);
        if (held instanceof Verdict verdict) {
            return verdict;
        }
        if (held instanceof Refusal refusal) {
            throw refusal;
        }
        attempted.add(qualifiedName(node));
        List<Member> missing = new ArrayList<>();
        for (Member member : Member.values()) {
            if (!ready.containsKey(member)) {
                missing.add(member);
            }
        }
        if (!missing.isEmpty()) {
            throw remember(node, refusal(node, "the unit is UNAVAILABLE - " + missing.size() + " of "
                    + Member.values().length + " member(s) are not ready: " + missing, null));
        }
        if (!wiringAvailable) {
            // Every member is ready and the WIRING has not switched the route on. The type is refused whole,
            // exactly as a missing member refuses it, so the old generator's six files stand and not one byte
            // moves - the commit that flips the switch is the commit that moves the register and the meter.
            throw remember(node, refusal(node, "the unit is UNAVAILABLE - all "
                    + Member.values().length + " member(s) are ready but the wiring's AVAILABLE switch is OFF"
                    + " (IRTypeUnitWiring.AVAILABLE), so no type is written from the IR yet", null));
        }
        if (!available(node.kind())) {
            // Every member is ready and the whole-route switch is ON, so the only way this predicate can be false
            // is the KIND-SCOPED one (v3.3 seat 10, PR #646 commit 4). The node is refused WHOLE by name, exactly
            // as the switch refusal above refuses a data type while AVAILABLE is off: the old generator's six
            // files stand and not one byte moves (the routing law of IRUnitPass was untouched at commit 4; since
            // commit 5 its loop gates on this kind BEFORE asking for a verdict, so this arm is the unit's own second
            // belt - reached only by a caller that skips the loop's gate). INSPECT is NOT
            // gated by any of this - that is what lets the six CHOICES shadow lines measure the kind now.
            throw remember(node, refusal(node, "the choice route is OFF -"
                    + " IRTypeUnitWiring.CHOICE_AVAILABLE is false, so a " + node.kind() + " node is refused whole"
                    + " and the old generator writes all six of its files", null));
        }
        EnumMap<Member, String> texts = new EnumMap<>(Member.class);
        EnumSet<Member> notApplicable = EnumSet.noneOf(Member.class);
        for (Member member : Member.values()) {
            Optional<String> text;
            try {
                text = ready.get(member).emit(node);
            } catch (RuntimeException e) {
                throw remember(node, refusal(node, "the member " + member + " REFUSED - the whole unit is refused"
                        + " rather than written in part (" + e.getClass().getSimpleName() + ": " + e.getMessage()
                        + ")", e));
            }
            if (text == null) {
                throw remember(node, refusal(node, "the member " + member + " answered null - the member contract is"
                        + " an Optional and null is neither a file nor a lawful absence", null));
            }
            if (text.isEmpty()) {
                if (!member.mayWriteNoFile()) {
                    throw remember(node, refusal(node, "the member " + member + " answered NO FILE, which only "
                            + Member.DEEP_PATH_UTIL + " may do - a member that writes a file for every validated type"
                            + " and none for this one is refused, never booked as an absence", null));
                }
                notApplicable.add(member);
                continue;
            }
            texts.put(member, text.get());
        }
        Verdict verdict = new Verdict(texts, notApplicable);
        memo.put(node, verdict);
        return verdict;
    }

    /**
     * The type's files, keyed by {@link #outputKey} - the PRESENT members only. A member that writes no file for this
     * type by law contributes no key, and {@link Verdict#notApplicable()} names it.
     *
     * @throws Refusal as {@link #verdict}
     */
    public Map<String, String> emit(IRTypeNode node) {
        Verdict verdict = verdict(node);
        Map<String, String> written = new LinkedHashMap<>();
        for (Map.Entry<Member, String> entry : verdict.texts().entrySet()) {
            written.put(outputKey(node, entry.getKey()), entry.getValue());
        }
        return written;
    }

    /**
     * THE SHADOW'S ENTRY POINT (v3.3 seat 9, PR #645 commit 12, the planning review's Q1(a)): ONE ready member's
     * emitter over this unit's own construction, run for MEASUREMENT. It writes nothing, memoises nothing and does
     * NOT require the unit to be available - the whole point of the strict path is that a member's bytes are held
     * against the golden on the full corpus BEFORE the unit can switch on.
     *
     * @throws IllegalStateException when the member is not ready on this unit - a shadow line for a member the wiring
     *     never declared would be a measurement of nothing
     */
    public Optional<String> inspect(IRTypeNode node, Member member) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(member, "member");
        MemberEmitter emitter = ready.get(member);
        if (emitter == null) {
            throw new IllegalStateException("IR type unit: the member " + member + " is not ready on this unit, so it"
                    + " has nothing to inspect (ready: " + ready.keySet() + ")");
        }
        return emitter.emit(node);
    }

    private Refusal remember(IRTypeNode node, Refusal refusal) {
        memo.put(node, refusal);
        refused.add(qualifiedName(node));
        return refusal;
    }

    private Refusal refusal(IRTypeNode node, String why, Throwable cause) {
        Refusal refusal = new Refusal("IR type unit: " + qualifiedName(node) + " is REFUSED WHOLE - " + why
                + " (no per-file fallback: the old generator writes all six files for this type)", cause);
        refusal.setTargetPath(outputKey(node, Member.POJO));
        return refusal;
    }

    // --------------------------------------------------------------------------------------------- the output keys

    /**
     * The member's output key for the node - the legacy convention
     * ({@code JavaClassGenerator.generateClasses}, {@code :55-78}): the ESCAPED package, as directories, then the
     * simple name with the member's suffix and {@code .java}.
     */
    public static String outputKey(IRTypeNode node, Member member) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(member, "member");
        DottedPath namespace = DottedPath.splitOnDots(namespaceOf(node));
        for (String segment : member.subPackage()) {
            namespace = namespace.child(segment);
        }
        return JavaPackageName.escape(namespace).getName()
                .child(simpleName(node) + member.suffix()).withForwardSlashes() + ".java";
    }

    /** The declaring namespace, or a refusal - a node with no namespace names no file. */
    static String namespaceOf(IRTypeNode node) {
        return node.namespace().orElseThrow(() -> new GenerationException(
                "IR type unit: " + node.name() + " carries no namespace", null, null));
    }

    /**
     * {@link IRTypeNode#name()} is {@code namespace.Simple}; a name that does not start with its namespace is refused
     * rather than split on the last dot ({@link IREnumEmitter#simpleName}'s law, generalised to the type kinds).
     */
    static String simpleName(IRTypeNode node) {
        String namespace = node.namespace().orElse("");
        String prefix = namespace.isEmpty() ? "" : namespace + ".";
        if (!node.name().startsWith(prefix) || node.name().length() == prefix.length()) {
            throw new GenerationException("IR type unit: the name " + node.name()
                    + " does not start with its namespace " + namespace, null, null);
        }
        return node.name().substring(prefix.length());
    }

    /** The type's qualified name for a refusal message - {@code namespace.Simple}, the IR's own spelling. */
    static String qualifiedName(IRTypeNode node) {
        return node.name();
    }
}
