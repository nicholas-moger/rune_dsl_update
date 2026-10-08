package com.regnosys.rosetta.generator.java.ir;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.RRootElement;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;

import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.NAMESPACE;
import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.dataType;
import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.fixture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.3 seat 9 (PR #645 commit 4) - THE TYPE UNIT'S GATE, PROVED TO BE ABLE TO FAIL. The unit is the STRICT path's
 * whole point: a data type's six files are emitted TOGETHER or the type is REFUSED BY NAME, and no caller can ever
 * obtain five of six. This class states that in four ways:
 * <ul>
 *   <li>with 0..5 members ready the unit REFUSES, and the refusal NAMES the type and every missing member;</li>
 *   <li>with all six ready it emits exactly six keys, each in the legacy output-key convention;</li>
 *   <li>a member that THROWS refuses the whole unit - the map never reaches the caller;</li>
 *   <li>{@link IRTypeUnit#available()} is the six-member predicate and nothing looser.</li>
 * </ul>
 *
 * <p>The stubs are TEXT, not renders: what is under test is the unit's all-or-nothing law and its key convention,
 * never a member's bytes (the POJO member's bytes are {@code IRDataTypeEmitterTest}'s).
 */
class IRTypeUnitTest {

    private static IRTypeNode leaf() {
        IRPropertyModelTest.Fixture f = fixture();
        return f.index().node(NAMESPACE, dataType(f.lib(), "Leaf"));
    }

    /** The fixture's flat two-option choice, as the index mints it - a CHOICE node, all six members' subject. */
    private static IRTypeNode either() {
        IRPropertyModelTest.Fixture f = fixture();
        return f.index().node(NAMESPACE, IRPropertyModelTest.choice(f.lib(), "Either"));
    }

    // ============================================== v3.3 seat 10 (PR #646 commit 4): THE KIND-SCOPED SWITCH

    /**
     * ARM (a) and ARM (c) - A CHOICE NODE IS REFUSED WHOLE BY NAME WHILE THE KIND SWITCH IS OFF, AND A STRUCT
     * NODE'S VERDICT ON THE SAME UNIT IS UNMOVED. This is the commit's whole byte-inert claim at the unit level:
     * the POJO emitter admits the CHOICE kind now, the six shadow lines measure it, and the production path still
     * refuses every choice - so the old generator writes all six of its files and not one byte moves.
     *
     * <p>MUTANT: {@code IRTypeUnitWiring.CHOICE_AVAILABLE} flipped to {@code true} (lane C4), or the kind arm
     * dropped from {@code IRTypeUnit.verdict} - either way the choice is WRITTEN and this reads GREEN-when-lying.
     */
    @Test
    void aChoiceIsRefusedWholeByNameWhileTheKindSwitchIsOffAndAStructVerdictIsUnmoved() {
        Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> ready = stubs(IRTypeUnit.Member.values());
        IRTypeUnit unit = new IRTypeUnit(ready, true, false);
        assertTrue(unit.available(), "the whole-route switch is ON - available() is UNMOVED by the kind switch");
        assertTrue(unit.available(com.regnosys.rosetta.ir.core.IRKind.STRUCT),
                "and so is the STRUCT kind's answer");
        assertFalse(unit.available(com.regnosys.rosetta.ir.core.IRKind.CHOICE),
                "the CHOICE kind alone is not available while CHOICE_AVAILABLE is false");

        IRTypeUnit.Refusal refusal = assertThrows(IRTypeUnit.Refusal.class, () -> unit.emit(either()));
        assertTrue(refusal.getMessage().contains("seat8.props.Either"),
                "the refusal names the choice: " + refusal.getMessage());
        assertTrue(refusal.getMessage().contains("IRTypeUnitWiring.CHOICE_AVAILABLE"),
                "and names the SWITCH as the reason: " + refusal.getMessage());
        assertTrue(refusal.getMessage().contains("no per-file fallback"),
                "the refusal states the law it is enforcing: " + refusal.getMessage());
        assertFalse(refusal.getMessage().contains("are not ready"),
                "no member is missing: " + refusal.getMessage());
        assertTrue(unit.refusedTypes().contains("seat8.props.Either"),
                "and the memo BOOKS the refusal, so the D11 verdict line sees it: " + unit.refusedTypes());

        // ARM (c): the SAME unit still writes a data type WHOLE - six keys, the legacy convention
        assertEquals(6, unit.emit(leaf()).size(), "a STRUCT node's verdict is UNMOVED by the kind switch");
    }

    /**
     * ARM (b) - THE SHADOW'S DOOR IS OPEN ANYWAY. {@link IRTypeUnit#inspect} never requires availability of any
     * kind, and that is precisely what lets the six D11 {@code ... CHOICES} lines measure a choice's six files on
     * every cell BEFORE the route for that kind flips (the strict path: the measurement comes first).
     *
     * <p>MUTANT: an availability check added to {@code inspect} - every CHOICES line would then read
     * {@code refused=<the whole population>} instead of measuring anything.
     */
    @Test
    void theInspectionPathStillAnswersForEveryMemberOfARefusedChoice() {
        Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> ready = stubs(IRTypeUnit.Member.values());
        IRTypeUnit unit = new IRTypeUnit(ready, true, false);
        IRTypeNode choiceNode = either();
        assertThrows(IRTypeUnit.Refusal.class, () -> unit.verdict(choiceNode),
                "the production path refuses it");
        for (IRTypeUnit.Member member : IRTypeUnit.Member.values()) {
            assertEquals(Optional.of("// " + member + " of seat8.props.Either\n"),
                    unit.inspect(choiceNode, member),
                    "the shadow's door is open for " + member + " even though the verdict refused");
        }
    }

    /**
     * ARM (d) - THE PRODUCTION LAW since v3.3 seat 10, PR #646 commit 5. With the kind switch ON a CHOICE node
     * gets a full {@link IRTypeUnit.Verdict}: six texts, six keys, the legacy convention. It was a REHEARSAL at
     * commit 4 - an arm the wiring could not reach, written because an arm no witness reaches is an arm nobody
     * has proved - and the flip made it the shape the route writes on every cell.
     */
    @Test
    void withTheKindSwitchOnAChoiceIsWrittenWholeExactlyAsADataTypeIs() {
        Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> ready = stubs(IRTypeUnit.Member.values());
        IRTypeUnit on = new IRTypeUnit(ready, true, true);
        assertTrue(on.available(com.regnosys.rosetta.ir.core.IRKind.CHOICE));
        IRTypeUnit.Verdict verdict = on.verdict(either());
        assertEquals(6, verdict.texts().size(), "six texts, no member not-applicable on this stub unit");
        assertEquals(List.of(
                        "seat8/props/Either.java",
                        "seat8/props/validation/EitherTypeFormatValidator.java",
                        "seat8/props/validation/EitherValidator.java",
                        "seat8/props/validation/exists/EitherOnlyExistsValidator.java",
                        "seat8/props/meta/EitherMeta.java",
                        "seat8/props/util/EitherDeepPathUtil.java"),
                List.copyOf(on.emit(either()).keySet()),
                "a choice owns the SAME six files a data type owns, at the same convention");
    }

    /**
     * THE WIRING'S THIRD DECLARATION AT THIS COMMIT: the kind switch is ON, and the seam the D11 host reads says
     * the same. The {@code UNIT READY} line asserts {@code CHOICE_AVAILABLE=true} on every cell beside this; the
     * flip commit re-cut both together, with the register's CHOICE rows and the file meter.
     *
     * <p>MUTANT: {@code IRTypeUnitWiring.CHOICE_AVAILABLE = false} (lane C4) - every choice goes back to the old
     * generator, the register's deleted rows are owed back, and the meter's choice legs are a lie.
     */
    @Test
    void theWiringOfThisCommitDeclaresTheKindSwitchOnSoEveryChoiceIsWrittenFromTheIr() {
        assertTrue(IRTypeUnitWiring.CHOICE_AVAILABLE, "since PR #646 commit 5 the CHOICE route is ON: the unit"
                + " writes every choice's six files from the IR alone or refuses the choice whole, the register's"
                + " CHOICE rows are re-cut to that run's own dump and the file meter counts them");
        assertTrue(IRTypeUnitWiring.choiceAvailable(), "the seam the D11 host reads says the same");
        assertTrue(IRTypeUnitWiring.AVAILABLE, "and the WHOLE-ROUTE switch is untouched by it");
    }

    /**
     * THE CHOICE PASS'S DELEGATE, REFUSED BY NAME (v3.3 seat 10, PR #646 commit 4). The choice POJO shadow runs
     * on the DELEGATE's index and the delegate's memoised shadow emitter, so a plain legacy
     * {@code ModelObjectGenerator} would give it neither - and a shadow built over a second index would compare
     * a node the POJO pass never minted, which is a measurement of two different things. The construction
     * refuses it rather than shadowing nothing quietly.
     *
     * <p>The IR-routed delegate is accepted in the same test, so the check is held in BOTH directions: a
     * refusal that also refused the right argument would be a refusal nobody could use.
     *
     * <p>MUTANT: the {@code instanceof} check dropped (lane C7) - the constructor then fails on the cast with a
     * {@code ClassCastException} and names nothing.
     */
    @Test
    void theChoicePassRefusesAnyDelegateButTheIrPojoPassByName() {
        IRPropertyModelTest.Fixture f = fixture();
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        com.regnosys.rosetta.generator.java.types.JavaTypeTranslator translator =
                new com.regnosys.rosetta.generator.java.types.JavaTypeTranslator(typeUtil);
        // THE THROWN THING IS CAUGHT AS A Throwable AND JUDGED HERE, never asserted to be a type first. With
        // the check dropped (lane C7) the constructor still throws - a ClassCastException off the cast below it
        // - and an assertThrows(IllegalArgumentException.class, ...) would then fail with JUnit's own
        // "unexpected exception type" message, which names the LAW nowhere. A refusal test whose failure does
        // not name the refusal is a test nobody can read from a log.
        Throwable thrown = assertThrows(Throwable.class,
                () -> new IRChoiceObjectGenerator(f.gm(), translator, typeUtil,
                        new com.regnosys.rosetta.generator.java.object.ModelObjectGenerator(
                                f.gm(), translator, typeUtil)));
        String message = thrown.getMessage() == null ? "" : thrown.getMessage();
        assertTrue(thrown instanceof IllegalArgumentException
                        && message.contains("the IR choice pass needs the IR POJO pass as its delegate"),
                "the choice pass must REFUSE any delegate but the IR POJO pass BY NAME - \"the IR choice pass"
                        + " needs the IR POJO pass as its delegate - one index, one node per declaration\" - and"
                        + " it threw " + thrown.getClass().getName() + ": " + message);
        assertTrue(message.contains("ModelObjectGenerator"),
                "and names what it was handed: " + message);

        // the RIGHT delegate is accepted, and the pass's shadow map starts empty
        IRChoiceObjectGenerator accepted = new IRChoiceObjectGenerator(f.gm(), translator, typeUtil,
                new IRModelObjectGenerator(f.gm(), translator, typeUtil));
        assertTrue(accepted.pojoShadowRenders().isEmpty(), "nothing is booked until a model is generated");
        assertEquals(IRTypeUnitWiring.readyNames(), accepted.unitReady(),
                "and it reads the SAME one wiring declaration the six passes read");
    }

    // ====================================== v3.3 seat 10 (PR #646 commit 5): THE ROUTING LAW OVER BOTH KINDS

    /**
     * THE ROUTING LAW ITSELF, over the two kinds, at the ONE declaration all seven callers take (v3.3 seat 10,
     * PR #646 commit 5). With the kind switch ON a choice is WRITTEN at the member's own output key and the
     * inherited path never runs; with it OFF the SAME population takes the inherited generator's path WHOLE -
     * every element, no file of the unit's, and the inherited renderer's own error for each. The two halves are
     * driven on one fixture so the law is read in both directions: a route that always wrote, or always
     * inherited, would pass half of this.
     *
     * <p>MUTANTS: the {@code RChoice} admission dropped from {@link IRUnitPass#validated} (lane R2) - the ON half
     * writes nothing and inherits everything; the kind gate dropped from the loop ({@code inherit =
     * !unit.available(node.kind())}) - the unit's own kind arm still refuses the node and the element is still
     * inherited, so the output map, the written set, the errors and the inherited-path count all read as before;
     * what moves is that the refusal is now BOOKED ({@code IRTypeUnit.remember}), so the OFF half's assertion that
     * the unit's attempted AND refused sets are EMPTY is the one that reddens (round 1 cq SF-3: the commit-5 law
     * "the gate stands BEFORE the verdict, so an unavailable kind never enters attempted" had no witness).
     */
    @Test
    void theRoutingLawWritesAChoiceWhenTheKindIsAvailableAndInheritsItWholeWhenItIsNot() {
        IRPropertyModelTest.Fixture f = fixture();
        List<RRootElement> choices = f.lib().rootElements().stream()
                .filter(e -> e instanceof com.regnosys.rosetta.ast.types.RChoice).toList();
        assertFalse(choices.isEmpty(), "the fixture declares at least one choice for the law to be read on");

        for (boolean kindAvailable : List.of(true, false)) {
            Map<String, String> output = new java.util.LinkedHashMap<>();
            java.util.Set<String> written = new java.util.LinkedHashSet<>();
            List<RRootElement> walked = new java.util.ArrayList<>();
            AtomicInteger inheritedRuns = new AtomicInteger();
            IRTypeUnit unit = new IRTypeUnit(stubs(IRTypeUnit.Member.values()), true, kindAvailable);
            List<com.regnosys.rosetta.generator.GenerationException> errors = IRUnitPass.routeElements(
                    unit, choices,
                    NAMESPACE, "1.0.0", f.index(), IRTypeUnit.Member.POJO, output, written, walked,
                    e -> {
                        inheritedRuns.incrementAndGet();
                        // the inherited generator's own failure for this element, which `inherited` books as a
                        // GenerationException with the element's target path - never raised past the loop
                        throw new IllegalStateException("the INHERITED path ran for "
                                + IRUnitPass.declaredName(e));
                    },
                    (e, r, v) -> {
                        throw new AssertionError("unreachable: the representation refused first");
                    });
            assertEquals(choices.size(), walked.size(), "the loop walks its whole population either way");
            if (kindAvailable) {
                assertEquals(0, inheritedRuns.get(), "the kind is available: the inherited path never runs");
                assertEquals(List.of(), errors, "and the unit's own path raises nothing");
                assertEquals(written, output.keySet(), "every key in the output map is the unit's own");
                assertEquals(choices.size(), written.size(), "one POJO file per choice, written whole");
                assertTrue(written.contains("seat8/props/Either.java"), "at the member's own key: " + written);
                assertEquals(choices.size(), unit.attemptedTypes().size(),
                        "every choice was ATTEMPTED through the unit, and none refused: " + unit.refusedTypes());
                assertTrue(unit.refusedTypes().isEmpty(), "none refused: " + unit.refusedTypes());
            } else {
                assertEquals(choices.size(), inheritedRuns.get(),
                        "the kind is NOT available: every choice takes the inherited path, whole");
                assertEquals(choices.size(), errors.size(),
                        "and the inherited generator's OWN errors are the ones that reach the caller");
                assertEquals(Map.of(), output, "the unit wrote no file for a kind it may not write");
                assertEquals(java.util.Set.of(), written, "and claimed none");
                assertTrue(unit.attemptedTypes().isEmpty() && unit.refusedTypes().isEmpty(),
                        "and the unit was never ASKED: the kind gate stands BEFORE the verdict, so an unavailable"
                                + " kind enters neither attempted nor refused (PR #646 commit 5) - attempted "
                                + unit.attemptedTypes() + " refused " + unit.refusedTypes());
            }
        }
    }

    private static Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> stubs(IRTypeUnit.Member... members) {
        Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> map = new EnumMap<>(IRTypeUnit.Member.class);
        for (IRTypeUnit.Member member : members) {
            map.put(member, node -> Optional.of("// " + member + " of " + node.name() + "\n"));
        }
        return map;
    }

    // --------------------------------------------------------------------------------- the six output keys

    @Test
    void theSixMembersTakeTheLegacyOutputKeyConvention() {
        IRTypeNode node = leaf();
        assertEquals(List.of(
                        "seat8/props/Leaf.java",
                        "seat8/props/validation/LeafTypeFormatValidator.java",
                        "seat8/props/validation/LeafValidator.java",
                        "seat8/props/validation/exists/LeafOnlyExistsValidator.java",
                        "seat8/props/meta/LeafMeta.java",
                        "seat8/props/util/LeafDeepPathUtil.java"),
                List.of(IRTypeUnit.outputKey(node, IRTypeUnit.Member.POJO),
                        IRTypeUnit.outputKey(node, IRTypeUnit.Member.TYPE_FORMAT_VALIDATOR),
                        IRTypeUnit.outputKey(node, IRTypeUnit.Member.CARDINALITY_VALIDATOR),
                        IRTypeUnit.outputKey(node, IRTypeUnit.Member.ONLY_EXISTS_VALIDATOR),
                        IRTypeUnit.outputKey(node, IRTypeUnit.Member.META),
                        IRTypeUnit.outputKey(node, IRTypeUnit.Member.DEEP_PATH_UTIL)),
                "the six keys are JavaClassGenerator's own: the escaped package as directories, the member's"
                        + " sub-package, and the simple name with the member's suffix");
    }

    @Test
    void aSixStubUnitEmitsExactlySixFilesAndIsAvailable() {
        IRTypeNode node = leaf();
        IRTypeUnit unit = new IRTypeUnit(stubs(IRTypeUnit.Member.values()));
        assertTrue(unit.available(), "six of six members ready is the only available state");
        Map<String, String> written = unit.emit(node);
        assertEquals(6, written.size(), "one file per member, no more and no fewer");
        assertEquals(List.of("seat8/props/Leaf.java",
                        "seat8/props/validation/LeafTypeFormatValidator.java",
                        "seat8/props/validation/LeafValidator.java",
                        "seat8/props/validation/exists/LeafOnlyExistsValidator.java",
                        "seat8/props/meta/LeafMeta.java",
                        "seat8/props/util/LeafDeepPathUtil.java"),
                List.copyOf(written.keySet()), "and in the members' declared order");
        assertEquals("// POJO of seat8.props.Leaf\n", written.get("seat8/props/Leaf.java"));
    }

    // ------------------------------------------------------------------- 0..5 members ready: REFUSED BY NAME

    @Test
    void aUnitWithNoMemberReadyRefusesTheTypeByNameAndNamesAllSixMissingMembers() {
        IRTypeNode node = leaf();
        IRTypeUnit unit = new IRTypeUnit(Map.of());
        assertFalse(unit.available());
        IRTypeUnit.Refusal refusal = assertThrows(IRTypeUnit.Refusal.class, () -> unit.emit(node));
        assertTrue(refusal.getMessage().contains("seat8.props.Leaf"),
                "the refusal names the type: " + refusal.getMessage());
        for (IRTypeUnit.Member member : IRTypeUnit.Member.values()) {
            assertTrue(refusal.getMessage().contains(member.name()),
                    "the refusal names the missing member " + member + ": " + refusal.getMessage());
        }
        assertTrue(refusal.getMessage().contains("6 of 6"), refusal.getMessage());
    }

    @Test
    void everyPartiallyReadyUnitFromOneMemberToFiveRefusesAndNamesExactlyWhatIsMissing() {
        IRTypeNode node = leaf();
        IRTypeUnit.Member[] all = IRTypeUnit.Member.values();
        for (int ready = 1; ready <= 5; ready++) {
            IRTypeUnit.Member[] present = new IRTypeUnit.Member[ready];
            System.arraycopy(all, 0, present, 0, ready);
            IRTypeUnit unit = new IRTypeUnit(stubs(present));
            assertFalse(unit.available(), ready + " of 6 members is NOT available");
            IRTypeUnit.Refusal refusal = assertThrows(IRTypeUnit.Refusal.class, () -> unit.emit(node),
                    "a unit with " + ready + " of 6 members must refuse, never write part of the type");
            assertTrue(refusal.getMessage().contains((6 - ready) + " of 6"), refusal.getMessage());
            for (int i = 0; i < all.length; i++) {
                boolean missing = i >= ready;
                assertEquals(missing, refusal.getMessage().contains(all[i].name()),
                        "the refusal names " + all[i] + " iff it is missing (ready=" + ready + "): "
                                + refusal.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------- a member that throws refuses the WHOLE unit

    @Test
    void aMemberThatThrowsRefusesTheWholeUnitAndNoPartialMapIsReturned() {
        IRTypeNode node = leaf();
        Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> members = stubs(IRTypeUnit.Member.values());
        members.put(IRTypeUnit.Member.META, n -> {
            throw new IllegalStateException("the *Meta member is not derived yet");
        });
        IRTypeUnit unit = new IRTypeUnit(members);
        assertTrue(unit.available(), "all six are DECLARED ready - the refusal comes from the render, not the gate");
        IRTypeUnit.Refusal refusal = assertThrows(IRTypeUnit.Refusal.class, () -> unit.emit(node));
        assertTrue(refusal.getMessage().contains("seat8.props.Leaf"), refusal.getMessage());
        assertTrue(refusal.getMessage().contains("META"), refusal.getMessage());
        assertTrue(refusal.getMessage().contains("the *Meta member is not derived yet"), refusal.getMessage());
        assertTrue(refusal.getMessage().contains("no per-file fallback"),
                "the refusal states the law it is enforcing: " + refusal.getMessage());
    }

    @Test
    void aMemberThatReturnsNoTextRefusesTheWholeUnitToo() {
        IRTypeNode node = leaf();
        Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> members = stubs(IRTypeUnit.Member.values());
        members.put(IRTypeUnit.Member.DEEP_PATH_UTIL, n -> null);
        IRTypeUnit.Refusal refusal = assertThrows(IRTypeUnit.Refusal.class, () -> new IRTypeUnit(members).emit(node));
        assertTrue(refusal.getMessage().contains("DEEP_PATH_UTIL"), refusal.getMessage());
        assertTrue(refusal.getMessage().contains("answered null"), refusal.getMessage());
    }

    // -------------------------------------------------------------------- what THIS commit's wiring declares

    /**
     * v3.3 seat 9, PR #645 commit 15 - THE SWITCH IS ON. The wiring declares ALL SIX members ready (the POJO at
     * commit 11, the THREE VALIDATORS at commit 12, the {@code META} at commit 13, the {@code DEEP_PATH_UTIL} at
     * commit 14), each of whose emitters renders its whole file from the IR alone and is held byte-identical by its
     * own D11 {@code UNIT SHADOW} gate - AND the wiring's SECOND declaration, {@link IRTypeUnitWiring#AVAILABLE},
     * is now {@code true}. So a unit built from that declaration is AVAILABLE and WRITES every data type whole:
     * six keys, the legacy convention. The OFF direction is stated beside it - a unit built with the switch
     * {@code false} still refuses every type and NAMES the switch - because that arm is the whole-route kill
     * switch and an arm no witness reaches is an arm nobody has proved. Commit 14's form of this test read the
     * switch OFF; commit 13's read five members and no switch; commit 12's read four; commit 11's the POJO alone.
     */
    @Test
    void theWiringOfThisCommitDeclaresAllSixMembersReadyAndTheSwitchIsOnSoTheUnitWritesEveryDataTypeWhole() {
        assertEquals(java.util.Set.of(IRTypeUnit.Member.values()), IRModelObjectGenerator.READY_MEMBERS,
                "the wiring declares every one of the six members READY - the set IS Member.values()");
        assertEquals(IRModelObjectGenerator.READY_MEMBERS, IRTypeUnitWiring.READY_MEMBERS,
                "ONE declaration: the POJO pass's name for the set IS the shared wiring's set");
        assertTrue(IRTypeUnitWiring.AVAILABLE, "AND THE WIRING'S SWITCH IS ON - at commit 15 the route writes every"
                + " data type whole, the register's DATA_TYPE rows go to zero and the file meter moves with it");
        assertTrue(IRTypeUnitWiring.available(), "the seam the D11 host reads says the same");

        Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> ready =
                new EnumMap<>(IRTypeUnit.Member.class);
        for (IRTypeUnit.Member member : IRModelObjectGenerator.READY_MEMBERS) {
            ready.put(member, node -> Optional.of("the " + member + " text"));
        }
        IRTypeUnit unit = new IRTypeUnit(ready, IRTypeUnitWiring.AVAILABLE);
        assertEquals(IRModelObjectGenerator.READY_MEMBERS, unit.ready());
        assertTrue(unit.available(), "six of six members are ready and the SWITCH is on, so the unit IS"
                + " available - available() is the two facts, and both read true at this commit");
        assertEquals(List.of(
                        "seat8/props/Leaf.java",
                        "seat8/props/validation/LeafTypeFormatValidator.java",
                        "seat8/props/validation/LeafValidator.java",
                        "seat8/props/validation/exists/LeafOnlyExistsValidator.java",
                        "seat8/props/meta/LeafMeta.java",
                        "seat8/props/util/LeafDeepPathUtil.java"),
                List.copyOf(unit.emit(leaf()).keySet()),
                "the AVAILABLE unit writes the type WHOLE - all six keys, the legacy convention");

        // THE KILL SWITCH, in the other direction: the SAME six members with the switch OFF refuse every type and
        // name the switch as the reason. Flipping AVAILABLE back sends every data type to the old generator.
        IRTypeUnit off = new IRTypeUnit(ready, false);
        assertFalse(off.available(), "six ready and the switch OFF is NOT available");
        IRTypeUnit.Refusal refusal = assertThrows(IRTypeUnit.Refusal.class, () -> off.emit(leaf()));
        assertTrue(refusal.getMessage().contains("AVAILABLE switch is OFF"),
                "and the refusal names the REASON, which is not a missing member: " + refusal.getMessage());
        assertTrue(refusal.getMessage().contains("seat8.props.Leaf"), refusal.getMessage());
        assertFalse(refusal.getMessage().contains("are not ready"),
                "no member is missing at this commit: " + refusal.getMessage());
    }

    /**
     * v3.3 seat 9, PR #645 commit 15 - THE NON-DATA-TYPE SPLIT, WHICH IS EMPTY BY CONSTRUCTION, PROVED ABLE TO
     * FAIL. When the unit is AVAILABLE the POJO pass no longer calls the inherited generator's loop at all, so the
     * question "what does that loop write besides the data types?" has to be ANSWERED rather than assumed. It is
     * answered by {@code ModelObjectGenerator.streamObjects:68-71}, which yields the model's {@code RDataType}
     * elements and nothing else - and {@link IRModelObjectGenerator#assertUnitLoopCoversTheInheritedPopulation}
     * holds that answer against the elements the unit's loop actually walked, element for element and in order.
     *
     * <p>Both failure arms are witnessed through the package-private seam - a HOLE (the inherited population
     * carries an element the loop never walked) and a DOUBLE WRITER (the loop walked one the population does not
     * yield) - because an assertion that can only pass is not an assertion.
     */
    @Test
    void theUnitLoopIsHeldAgainstTheInheritedGeneratorsOwnPopulationElementForElement() {
        IRPropertyModelTest.Fixture f = fixture();
        List<RDataType> population = new java.util.ArrayList<>();
        for (RRootElement element : f.lib().rootElements()) {
            if (element instanceof RDataType dataType) {
                population.add(dataType);
            }
        }
        assertTrue(population.size() >= 3, "the fixture must carry enough data types to drop one: " + population);

        // the covering walk: the same elements, the same order - no throw
        IRModelObjectGenerator.assertUnitLoopCoversTheInheritedPopulation(NAMESPACE, population,
                List.copyOf(population));

        // A HOLE: the loop dropped the LAST element - the message names it
        List<RDataType> short_ = population.subList(0, population.size() - 1);
        IllegalStateException hole = assertThrows(IllegalStateException.class,
                () -> IRModelObjectGenerator.assertUnitLoopCoversTheInheritedPopulation(NAMESPACE, population,
                        short_));
        assertTrue(hole.getMessage().contains("a HOLE"), hole.getMessage());
        assertTrue(hole.getMessage().contains(population.get(population.size() - 1).name()),
                "the throw NAMES the element the loop never walked: " + hole.getMessage());

        // A DOUBLE WRITER: the loop walked one the inherited population does not yield
        IllegalStateException twice = assertThrows(IllegalStateException.class,
                () -> IRModelObjectGenerator.assertUnitLoopCoversTheInheritedPopulation(NAMESPACE, short_,
                        population));
        assertTrue(twice.getMessage().contains("TWO writers"), twice.getMessage());
        assertTrue(twice.getMessage().contains(population.get(population.size() - 1).name()), twice.getMessage());

        // AN ORDER DISAGREEMENT: the same elements, the first two swapped - named at its index
        List<RDataType> swapped = new java.util.ArrayList<>(population);
        java.util.Collections.swap(swapped, 0, 1);
        IllegalStateException order = assertThrows(IllegalStateException.class,
                () -> IRModelObjectGenerator.assertUnitLoopCoversTheInheritedPopulation(NAMESPACE, population,
                        swapped));
        assertTrue(order.getMessage().contains("DISAGREE at index 0"), order.getMessage());
    }

    /**
     * LANE X5 (v3.3 seat 9, PR #645 commit 14) - THE REAL DEEP-PATH MEMBER ANSWERING EMPTY FOR AN ELIGIBLE TYPE.
     * The deep-path util is the ONE member the unit lets answer NO FILE BY LAW, which makes it the ONE member
     * whose empty answer the unit cannot catch by the contract alone: an eligible type that suddenly answered
     * empty would make a FIVE-file unit where the old generator writes six, and the unit would book it as a
     * lawful absence. The D11 shadow's {@code noFileWhereLegacyWrote} column is the corpus-wide catch; this is the
     * unit-level statement that the member does NOT do it - the REAL emitter over an ELIGIBLE fixture type, its
     * key present in the six-member map.
     */
    @Test
    void theRealDeepPathMemberAnswersAPresentTextForAnEligibleTypeSoTheUnitNeverLosesAFile() {
        IRDeepPathUtilEmitterTest.InjectionFixture fixture = IRDeepPathUtilEmitterTest.injectionFixture();
        IRTypeNode obs = fixture.index().node("seat9.deeppath",
                IRPropertyModelTest.dataType(fixture.model(), "Obs"));
        Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> members = stubs(IRTypeUnit.Member.values());
        members.put(IRTypeUnit.Member.DEEP_PATH_UTIL, new IRDeepPathUtilEmitter(fixture.index(),
                new IRDerivedFacts(fixture.index(), IRDerivedLie.NONE)));
        Map<String, String> written = new IRTypeUnit(members).emit(obs);
        String key = IRTypeUnit.outputKey(obs, IRTypeUnit.Member.DEEP_PATH_UTIL);
        assertEquals("seat9/deeppath/util/ObsDeepPathUtil.java", key);
        assertEquals(6, written.size(), "an ELIGIBLE type's unit writes all six keys: " + written.keySet());
        assertTrue(written.get(key).contains("class ObsDeepPathUtil {"), written.get(key));
    }

    /**
     * LANE X6 (v3.3 seat 9, PR #645 commit 14) - THE OTHER DIRECTION. A NOT-eligible type writes no util, so the
     * member answers empty, the unit books it NOT APPLICABLE by name and FIVE keys are written - never six. A text
     * here would be the shadow's {@code renderedWhereLegacyRefused}, a file the old generator never wrote.
     */
    @Test
    void theRealDeepPathMemberAnswersNoFileForANotEligibleTypeAndTheUnitBooksItByName() {
        IRDeepPathUtilEmitterTest.InjectionFixture fixture = IRDeepPathUtilEmitterTest.injectionFixture();
        IRTypeNode tag = fixture.index().node("seat9.deeppath",
                IRPropertyModelTest.dataType(fixture.model(), "Tag"));
        Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> members = stubs(IRTypeUnit.Member.values());
        members.put(IRTypeUnit.Member.DEEP_PATH_UTIL, new IRDeepPathUtilEmitter(fixture.index(),
                new IRDerivedFacts(fixture.index(), IRDerivedLie.NONE)));
        IRTypeUnit unit = new IRTypeUnit(members);
        IRTypeUnit.Verdict verdict = unit.verdict(tag);
        assertEquals(java.util.Set.of(IRTypeUnit.Member.DEEP_PATH_UTIL), verdict.notApplicable(),
                "Tag declares no one-of, so the util is NOT APPLICABLE - and the verdict NAMES it rather than"
                        + " leaving the caller to infer an absence from a missing key");
        Map<String, String> written = unit.emit(tag);
        assertEquals(5, written.size(), "five keys, the util's absent BY LAW: " + written.keySet());
        assertFalse(written.containsKey("seat9/deeppath/util/TagDeepPathUtil.java"), written.keySet().toString());
    }

    /**
     * LANE X4 (v3.3 seat 9, PR #645 commit 13) - THE REAL {@code META} MEMBER UNDER THE UNIT'S CONTRACT. Every
     * other statement of the member contract in this class is made with STUBS, which is right for the unit's own
     * law but blind to the emitter that now carries the member: a {@code META} emitter that answered
     * {@link Optional#empty()} would make a five-file unit where the old generator writes six, and no stub can
     * prove that it does not. So the REAL emitter is put in a six-member map beside five stubs and the whole unit
     * is emitted - the meta's key present with its own text, where a NO FILE answer would REFUSE THE TYPE BY NAME.
     */
    @Test
    void theRealMetaMemberAnswersAPresentTextSoTheUnitNeverWritesFiveFilesWhereSixBelong() {
        IRPropertyModelTest.Fixture f = fixture();
        IRTypeNode node = f.index().node(NAMESPACE, dataType(f.lib(), "Leaf"));
        Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> members = stubs(IRTypeUnit.Member.values());
        members.put(IRTypeUnit.Member.META, new IRModelMetaEmitter(
                new IRDerivedFacts(f.index(), IRDerivedLie.NONE),
                new IRModelIndex(f.gm().workspace(), f.passReconciler().adapter()), "0.0.0"));
        Map<String, String> written = new IRTypeUnit(members).emit(node);
        String key = IRTypeUnit.outputKey(node, IRTypeUnit.Member.META);
        assertEquals("seat8/props/meta/LeafMeta.java", key);
        assertTrue(written.containsKey(key),
                "the META member wrote no file - a member other than the deep-path util answering NO FILE is a"
                        + " refusal by name, never an absence, and the unit must never write five files where the"
                        + " old generator writes six");
        assertTrue(written.get(key).contains("class LeafMeta implements"),
                "the META key carries the meta emitter's own text: " + written.get(key));
    }

    // ------------------------------------------------------- the member contract: Optional, and who may say NO FILE

    /**
     * v3.3 seat 9, PR #645 commit 12 (the planning review's Q3(a)): an EMPTY answer means NO FILE BY LAW, and only
     * the deep-path util may give it. Every other member writes a file for every validated type, so an empty answer
     * from one of them is a REFUSAL BY NAME - booking it as an absence would let the unit write five files where the
     * old generator writes six, which is the per-file fallback the order forbids.
     */
    @Test
    void aMemberOtherThanTheDeepPathUtilMayNotAnswerNoFileAndIsRefusedByName() {
        IRTypeNode node = leaf();
        for (IRTypeUnit.Member member : IRTypeUnit.Member.values()) {
            if (member == IRTypeUnit.Member.DEEP_PATH_UTIL) {
                continue;
            }
            Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> members = stubs(IRTypeUnit.Member.values());
            members.put(member, n -> Optional.empty());
            IRTypeUnit.Refusal refusal =
                    assertThrows(IRTypeUnit.Refusal.class, () -> new IRTypeUnit(members).emit(node),
                            member + " may not answer NO FILE");
            assertTrue(refusal.getMessage().contains(member.name()), refusal.getMessage());
            assertTrue(refusal.getMessage().contains("answered NO FILE"), refusal.getMessage());
        }
    }

    /**
     * The deep-path util's eligibility fact IS a lawful absence: the key is simply not written, no error is raised,
     * and the verdict NAMES the member as not applicable - so a consumer never has to infer absence from a missing
     * key (the projection's third outcome).
     */
    @Test
    void theDeepPathUtilMayAnswerNoFileAndItsKeyIsAbsentWhileTheVerdictNamesIt() {
        IRTypeNode node = leaf();
        Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> members = stubs(IRTypeUnit.Member.values());
        members.put(IRTypeUnit.Member.DEEP_PATH_UTIL, n -> Optional.empty());
        IRTypeUnit unit = new IRTypeUnit(members);
        IRTypeUnit.Verdict verdict = unit.verdict(node);
        assertEquals(java.util.Set.of(IRTypeUnit.Member.DEEP_PATH_UTIL), verdict.notApplicable());
        assertTrue(verdict.textOf(IRTypeUnit.Member.DEEP_PATH_UTIL).isEmpty());
        Map<String, String> written = unit.emit(node);
        assertEquals(5, written.size(), "five keys, the util's absent by law");
        assertFalse(written.containsKey("seat8/props/util/LeafDeepPathUtil.java"), written.keySet().toString());
    }

    // --------------------------------------------------------------------------------------------- the MEMO

    /**
     * LANE X1: the per-node memo, by IDENTITY. Two asks of one node hand back the SAME verdict instance and run
     * every member's emitter exactly ONCE - which is what makes "every kind's ask for a type gets the same verdict"
     * true WITHIN one unit by construction rather than by hope.
     */
    @Test
    void twoAsksOfOneNodeReturnTheSameVerdictAndRunEachMemberExactlyOnce() {
        IRTypeNode node = leaf();
        AtomicInteger calls = new AtomicInteger();
        Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> members = new EnumMap<>(IRTypeUnit.Member.class);
        for (IRTypeUnit.Member member : IRTypeUnit.Member.values()) {
            members.put(member, n -> {
                calls.incrementAndGet();
                return Optional.of("// " + member + "\n");
            });
        }
        IRTypeUnit unit = new IRTypeUnit(members);
        IRTypeUnit.Verdict first = unit.verdict(node);
        IRTypeUnit.Verdict second = unit.verdict(node);
        assertSame(first, second, "the memo hands back the SAME verdict, never a second render");
        assertEquals(6, calls.get(), "six member renders in all, not twelve");
    }

    /** The memo holds a REFUSAL too: the same refusal comes back, and the emitters are not re-run. */
    @Test
    void aRefusedNodeStaysRefusedAndItsEmittersAreNotRerun() {
        IRTypeNode node = leaf();
        AtomicInteger calls = new AtomicInteger();
        Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> members = stubs(IRTypeUnit.Member.values());
        members.put(IRTypeUnit.Member.META, n -> {
            calls.incrementAndGet();
            throw new IllegalStateException("the *Meta member is not derived yet");
        });
        IRTypeUnit unit = new IRTypeUnit(members);
        IRTypeUnit.Refusal first = assertThrows(IRTypeUnit.Refusal.class, () -> unit.emit(node));
        IRTypeUnit.Refusal second = assertThrows(IRTypeUnit.Refusal.class, () -> unit.emit(node));
        assertSame(first, second, "the memo hands back the SAME refusal");
        assertEquals(1, calls.get(), "the refusing member ran once");
        assertEquals(java.util.Set.of("seat8.props.Leaf"), unit.refusedTypes(),
                "the unit books every type it REFUSED, by name");
        assertEquals(java.util.Set.of("seat8.props.Leaf"), unit.attemptedTypes(),
                "the unit books every type the production path asked it for, by name");
    }

    /**
     * LANE X2: the cross-instance equality the D11 host asserts CAN fail. Two units over one node whose member
     * answers differently the second time part company on their refused sets - which is exactly the disagreement
     * the host's {@code TYPE UNIT VERDICTS} line books RED by name. (The lane is a unit test because the D11 host
     * is not a lane host.)
     */
    @Test
    void twoUnitsOverOneNodeCanDisagreeAndTheRefusedSetsAreWhatShowsIt() {
        IRTypeNode node = leaf();
        AtomicInteger asks = new AtomicInteger();
        IRTypeUnit.MemberEmitter flaky = n -> {
            if (asks.incrementAndGet() > 1) {
                throw new IllegalStateException("not a pure function of the node");
            }
            return Optional.of("// POJO\n");
        };
        Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> first = stubs(IRTypeUnit.Member.values());
        first.put(IRTypeUnit.Member.POJO, flaky);
        Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> second = stubs(IRTypeUnit.Member.values());
        second.put(IRTypeUnit.Member.POJO, flaky);
        IRTypeUnit unitA = new IRTypeUnit(first);
        IRTypeUnit unitB = new IRTypeUnit(second);
        unitA.emit(node);
        assertThrows(IRTypeUnit.Refusal.class, () -> unitB.emit(node));
        assertEquals(unitA.attemptedTypes(), unitB.attemptedTypes(), "both passes ATTEMPTED the same type");
        assertFalse(unitA.refusedTypes().equals(unitB.refusedTypes()),
                "the refused sets DIVERGE - which is the host's red");
    }

    // ------------------------------------------------------------------------------------------ the SHADOW path

    /**
     * v3.3 seat 9, PR #645 commit 12 (the planning review's Q1(a)): {@link IRTypeUnit#verdict} refuses every type
     * while a member is missing, so a shadow built on it would measure nothing until the last member lands. The
     * inspection path runs ONE ready member over the same construction, whatever the unit's availability - and
     * REFUSES a member the wiring never declared, because a shadow line for one would be a measurement of nothing.
     */
    @Test
    void inspectRunsAReadyMemberWhileTheUnitIsUnavailableAndRefusesAnUnreadyOne() {
        IRTypeNode node = leaf();
        IRTypeUnit unit = new IRTypeUnit(Map.of(IRTypeUnit.Member.POJO,
                n -> Optional.of("// the POJO of " + n.name() + "\n")));
        assertFalse(unit.available());
        assertEquals(Optional.of("// the POJO of seat8.props.Leaf\n"),
                unit.inspect(node, IRTypeUnit.Member.POJO));
        assertThrows(IllegalStateException.class, () -> unit.inspect(node, IRTypeUnit.Member.META),
                "a member the wiring never declared has nothing to inspect");
        assertTrue(unit.attemptedTypes().isEmpty(), "the shadow books nothing on the production counters");
    }

    // ------------------------------------------------------------------- the per-pass emitter memo (PR #646 c3)

    /** The pure structural type table the wiring parameterises the compat algebra by - one per test, as elsewhere. */
    private static final JavaTypeUtil MEMO_TYPE_UTIL = new JavaTypeUtil();

    /** A pass of the memo's own kind, over a FRESH fixture workspace - its index is its own, so its memo is too. */
    private static IRUnitPass memoPass() {
        return new IRUnitPass(fixture().gm(), MEMO_TYPE_UTIL, IRTypeUnit.Member.CARDINALITY_VALIDATOR);
    }

    /**
     * v3.3 seat 10, PR #646 commit 3 (PR #645 round 1 cq NIT-1) - THE PER-PASS EMITTER MEMO, ARM (a): TWO MODELS OF
     * ONE STAMP ON ONE PASS GET THE SAME EMITTER INSTANCES. The wiring built six emitters - six
     * {@code TemplateRenderer.loadGroupFromClasspath} calls - for every model of every pass; an emitter is a pure
     * function of the pass's index, its model index, the pruning set, the type util and the version stamp, and
     * within a pass only the stamp varies, so the six are built once per stamp and handed to every unit of it.
     *
     * <p>The UNIT is still fresh every time (its per-node memo and its attempted / refused sets are per-unit
     * state), which this test reads too: same emitters, DIFFERENT unit.
     *
     * <p>THE NULL STAMP IS THE EMPTY ONE, in one spelling ({@link IRTypeUnitWiring#stamp}), so a pass that
     * normalised it itself could memo under one key and construct under another.
     *
     * <p>THE MUTANT (lane M1c): the memo keyed by a CONSTANT instead of the stamp. This arm stays green under it -
     * arm (b) below is the one it reddens, which is why the two arms are separate tests.
     */
    @Test
    void twoUnitsOfOneStampOnOnePassShareTheSameSixEmitterInstancesAndTheUnitItselfIsStillFresh() {
        IRUnitPass pass = memoPass();
        IRTypeUnit first = pass.unitFor("6.20.6");
        IRTypeUnit second = pass.unitFor("6.20.6");
        assertNotSame(first, second, "the UNIT is per generateClassesAsIR call - its per-node memo is per model");
        assertEquals(first.emitters().keySet(), second.emitters().keySet());
        for (IRTypeUnit.Member member : first.emitters().keySet()) {
            assertSame(first.emitters().get(member), second.emitters().get(member),
                    "the memo hands the SAME " + member + " emitter to every unit of one stamp on one pass");
        }
        IRTypeUnit nullStamped = pass.unitFor(null);
        IRTypeUnit emptyStamped = pass.unitFor("");
        for (IRTypeUnit.Member member : nullStamped.emitters().keySet()) {
            assertSame(nullStamped.emitters().get(member), emptyStamped.emitters().get(member),
                    "a null version and an empty one are ONE stamp, so they are ONE memo entry: " + member);
        }
    }

    /**
     * v3.3 seat 10, PR #646 commit 3 - THE MEMO, ARM (b): TWO STAMPS GET DIFFERENT EMITTERS. The stamp is not
     * decoration: {@code IRModelMetaEmitter} writes it into the {@code *Meta} javadoc and the POJO's config carries
     * it, so a memo that ignored it would render one model's version stamp into another model's file. The host
     * passes a version PER MODEL, which is exactly why the memo is keyed by it and not by the pass alone.
     *
     * <p>THE MUTANT (lane M1c): {@code membersByStamp.computeIfAbsent(stamp, ...)} keyed by a CONSTANT. Then both
     * stamps answer with one set of emitters and this test is RED on the first member it reads.
     */
    @Test
    void twoVersionStampsOnOnePassGetDifferentEmittersBecauseTheStampIsWrittenIntoTheBytes() {
        IRUnitPass pass = memoPass();
        IRTypeUnit older = pass.unitFor("6.20.5");
        IRTypeUnit newer = pass.unitFor("6.20.6");
        assertEquals(older.emitters().keySet(), newer.emitters().keySet());
        for (IRTypeUnit.Member member : older.emitters().keySet()) {
            assertNotSame(older.emitters().get(member), newer.emitters().get(member),
                    "the memo is keyed by the VERSION STAMP, so two stamps never share the " + member
                            + " emitter - a memo keyed by nothing would write one model's stamp into another's");
        }
    }

    /**
     * v3.3 seat 10, PR #646 commit 3 - THE MEMO, ARM (c): TWO PASSES NEVER SHARE, AND A FRESH PASS STARTS EMPTY.
     * The memo's soundness rests on ONE thing: it lives BESIDE the index it was built over and dies with the pass.
     * The emitters close over the pass's {@link IRTypeIndex} and the {@link IRDerivedFacts} they share caches per
     * node BY IDENTITY on that index, so an emitter handed to another pass would answer from a cache keyed on
     * nodes that pass never minted. Hence: no static memo, no provider-level cache.
     *
     * <p>THE MUTANT (lane M2c): {@code membersByStamp} declared {@code static}. Then the second pass reads the
     * first pass's emitters - built over the first pass's index - and this test is RED on the first member.
     */
    @Test
    void twoPassesNeverShareAnEmitterBecauseTheMemoLivesBesideTheIndexItWasBuiltOver() {
        IRUnitPass first = memoPass();
        IRUnitPass second = memoPass();
        IRTypeUnit fromFirst = first.unitFor("6.20.6");
        IRTypeUnit fromSecond = second.unitFor("6.20.6");
        assertEquals(fromFirst.emitters().keySet(), fromSecond.emitters().keySet());
        for (IRTypeUnit.Member member : fromFirst.emitters().keySet()) {
            assertNotSame(fromFirst.emitters().get(member), fromSecond.emitters().get(member),
                    "a FRESH pass starts with an EMPTY memo - the " + member + " emitter of one pass closes over"
                            + " that pass's own index, and may never be handed to another");
        }
    }
}
