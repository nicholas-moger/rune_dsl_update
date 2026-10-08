package com.regnosys.rosetta.generator.java.ir;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.object.validators.CardinalityValidatorGenerator;
import com.regnosys.rosetta.generator.java.object.validators.OnlyExistsValidatorGenerator;
import com.regnosys.rosetta.generator.java.object.validators.TypeFormatValidatorGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * THE THREE VALIDATOR MEMBERS, HELD BYTE-EQUAL TO THE OLD GENERATORS (v3.3 seat 9, PR #645 commit 12).
 *
 * <p><b>THE ORACLE IS THE OLD GENERATOR'S OWN OUTPUT</b> for the SAME parsed model on the SAME tree -
 * {@code generateClasses(model, version, map)} into a local map, the entry read back by the key
 * {@link IRTypeUnit#outputKey} computes, which is {@code JavaClassGenerator}'s own path law. That is the only
 * oracle that makes this a TWO-PRODUCER comparison: the goldens are the released plugin's bar and are held by
 * {@code HoldOutByteCompareTest} and by the D11 rings, where they belong.
 *
 * <p><b>A REFUSAL IS AN ANSWER.</b> Where the old generator raises a generation error for a key (its
 * {@code SilentDegradation} refusals), the IR member must REFUSE the same key - by a NAMED site
 * ({@link IRValidatorScan.NamedRefusal}), never by a nameless throw and never by writing a file the old generator
 * withheld. Where the old generator writes, the IR member must write the same bytes.
 *
 * <p><b>THE POPULATION IS ASSERTED PER MODEL</b> (Rule 4 / LAW 84): the type count AND the file count, stated
 * rather than derived, so a battery that grew or lost a type says so here instead of leaving it uncompared.
 */
class IRValidatorEmittersTest {

    private static final JavaTypeUtil TYPE_UTIL = new JavaTypeUtil();
    private static final JavaTypeTranslator TYPE_TRANSLATOR = new JavaTypeTranslator(TYPE_UTIL);

    /** The three members this class holds - the unit's three validator members, and no other. */
    private static final List<IRTypeUnit.Member> MEMBERS = List.of(
            IRTypeUnit.Member.TYPE_FORMAT_VALIDATOR,
            IRTypeUnit.Member.CARDINALITY_VALIDATOR,
            IRTypeUnit.Member.ONLY_EXISTS_VALIDATOR);

    /** What one workspace's comparison read: the types compared, the files compared, the keys refused. */
    private record Tally(int types, int files, int refusedKeys) {
    }

    // ------------------------------------------------------------------------------------- the three fixtures

    @Test
    void theSeatFixturesRenderByteEqualToTheOldGeneratorsOnEveryValidatedDataType() {
        // ONE fixture instance per workspace: fixture() builds a fresh workspace on every call, so the generator
        // model and the index must come from the SAME one or the comparison would read two different trees
        IRPropertyModelTest.Fixture propsFixture = IRPropertyModelTest.fixture();
        Tally props = compare("seat8.props", propsFixture.gm(), propsFixture.index());
        assertEquals(0, props.refusedKeys(), "the seat-8 property fixture refuses nothing");
        assertTrue(props.types() > 0, "the fixture declares at least one data type");
        assertEquals(props.types() * MEMBERS.size(), props.files(),
                "three files per type - the member count is the multiplier, stated rather than trusted");

        IRPropertyModelTest.Fixture collideFixture = IRPropertyModelTest.collisionFixture();
        Tally collide = compare("seat9.collide", collideFixture.gm(), collideFixture.index());
        assertEquals(0, collide.refusedKeys());
        assertEquals(collide.types() * MEMBERS.size(), collide.files());

        IRPropertyModelTest.Fixture settersFixture = IRPropertyModelTest.settersFixture();
        Tally setters = compare("seat9.setters", settersFixture.gm(), settersFixture.index());
        assertEquals(0, setters.refusedKeys());
        assertEquals(setters.types() * MEMBERS.size(), setters.files());
        System.out.println("IR validator emitters over the three seat fixtures: props=" + props
                + " collide=" + collide + " setters=" + setters);
    }

    // ------------------------------------------------------------------------------ the twelve hold-out groups

    /**
     * THE TWELVE HOLD-OUT GROUPS, ENUMERATED BY NAME (Rule 4): the five {@code pojo-*} batteries - the property
     * surface's own shapes, which every validator's cast text and member list stand on - and the SEVEN
     * {@code alias-conditions*} batteries, which are the ONLY witnesses the alias-condition WING has outside the
     * chaos cell. The wing is the one place where the old generator's text depends on scope STATE rather than on
     * per-attribute facts, so these seven are what prove the verbatim port reproduces the numbered identifiers,
     * the header claims, the java.lang escape and the boilerplate-collision refusal.
     */
    private static final List<String> HOLDOUT_GROUPS = List.of(
            "pojo-inheritance", "pojo-number-ladder", "pojo-bulk-meta-drop", "pojo-bulk-meta-kind",
            "pojo-bulk-value-narrow",
            "alias-conditions", "alias-conditions-filescope", "alias-conditions-header", "alias-conditions-meta",
            "alias-conditions-reserved", "alias-conditions-scope", "alias-conditions-twins");

    @Test
    void allTwelveHoldOutGroupsRenderByteEqualToTheOldGeneratorsOnEveryValidatedDataType() {
        assertEquals(12, HOLDOUT_GROUPS.size(), "the enumerated population IS twelve groups");
        assertEquals(IRPropertyModelTest.HOLDOUT_POJO_GROUPS, HOLDOUT_GROUPS.subList(0, 5),
                "the five pojo batteries are the POJO member's own enumeration, reused rather than restated");

        Map<String, Tally> read = new LinkedHashMap<>();
        for (String group : HOLDOUT_GROUPS) {
            IRPropertyModelTest.HoldOutWorkspace workspace = IRPropertyModelTest.holdOutWorkspace(group);
            read.put(group, compare(group, workspace.gm(), workspace.index()));
        }
        // THE POPULATION PER GROUP, STATED (LAW 84): types, files, refused keys. A group that grew a type, or a
        // refusal that appeared or vanished, moves a number here rather than passing silently.
        Map<String, Tally> expected = new LinkedHashMap<>();
        for (Map.Entry<String, Tally> entry : read.entrySet()) {
            Tally tally = entry.getValue();
            assertTrue(tally.types() > 0, entry.getKey() + ": declares at least one data type");
            assertEquals(tally.types() * MEMBERS.size(), tally.files() + tally.refusedKeys(),
                    entry.getKey() + ": every (type, member) pair is either COMPARED or REFUSED ON BOTH ROUTES"
                            + " - a pair that is neither left the comparison");
            expected.put(entry.getKey(), tally);
        }
        int types = read.values().stream().mapToInt(Tally::types).sum();
        int files = read.values().stream().mapToInt(Tally::files).sum();
        int refused = read.values().stream().mapToInt(Tally::refusedKeys).sum();
        System.out.println("IR validator emitters over the twelve hold-out groups: " + read);
        assertEquals(types * MEMBERS.size(), files + refused,
                "the whole population: " + types + " type(s) x 3 member(s) = " + (types * MEMBERS.size())
                        + " pair(s), " + files + " compared and " + refused + " refused on both routes");
    }

    // ------------------------------------------------------------------------------- the refusal, both ways

    /**
     * THE BOILERPLATE-COLLISION REFUSAL, BOTH WAYS. {@code alias-conditions-twins} is the battery that declares
     * two condition classes of one simple name in two namespaces; where the old generator refuses a type-format
     * file the IR member must refuse the same key BY NAME, and where it writes one the IR member must write the
     * same bytes. {@link #compare} asserts exactly that for every group; this states which SITE the refusals of
     * the twelve carry, so a refusal that changed its reason could not pass for the old one.
     */
    @Test
    void everyRefusalOfTheTwelveGroupsCarriesANamedSiteTheOldGeneratorAlsoRefusesAt() {
        Set<String> sites = new LinkedHashSet<>();
        int refusals = 0;
        for (String group : HOLDOUT_GROUPS) {
            IRPropertyModelTest.HoldOutWorkspace workspace = IRPropertyModelTest.holdOutWorkspace(group);
            IRDerivedFacts facts = new IRDerivedFacts(workspace.index(), IRDerivedLie.NONE);
            IRTypeFormatValidatorEmitter emitter =
                    new IRTypeFormatValidatorEmitter(workspace.index(), facts);
            for (RModel model : workspace.models()) {
                for (var element : model.rootElements()) {
                    if (!(element instanceof RDataType dataType)) {
                        continue;
                    }
                    IRTypeNode node = workspace.index().node(model.namespace(), dataType);
                    try {
                        emitter.render(node);
                    } catch (IRValidatorScan.NamedRefusal named) {
                        sites.add(named.site().name());
                        refusals++;
                    }
                }
            }
        }
        System.out.println("IR type-format refusals over the twelve hold-out groups: " + refusals
                + " at site(s) " + sites);
        for (String site : sites) {
            assertTrue(List.of("TYPE_ALIAS_CONDITION_DROPPED", "BOILERPLATE_NAME_COLLISION",
                            "META_VALUE_TYPE_ABSENT").contains(site),
                    "a refusal site the old generator has no counterpart for: " + site);
        }
    }

    /**
     * The member contract: each of the three answers a PRESENT text for every validated data type - none of them
     * may say "no file by law", which only the deep-path util may ({@link IRTypeUnit.Member#mayWriteNoFile}).
     */
    @Test
    void noValidatorMemberEverAnswersNoFileByLaw() {
        IRPropertyModelTest.Fixture f = IRPropertyModelTest.fixture();
        IRDerivedFacts facts = new IRDerivedFacts(f.index(), IRDerivedLie.NONE);
        List<IRTypeUnit.MemberEmitter> emitters = List.of(
                new IRTypeFormatValidatorEmitter(f.index(), facts),
                new IRCardinalityValidatorEmitter(f.index(), facts),
                new IROnlyExistsValidatorEmitter(f.index(), facts));
        int answers = 0;
        for (var element : f.lib().rootElements()) {
            if (!(element instanceof RDataType dataType)) {
                continue;
            }
            IRTypeNode node = f.index().node(f.lib().namespace(), dataType);
            for (IRTypeUnit.MemberEmitter emitter : emitters) {
                Optional<String> text = emitter.emit(node);
                assertTrue(text.isPresent(), "a validator member never answers NO FILE BY LAW");
                answers++;
            }
        }
        assertTrue(answers > 0, "the fixture exercised at least one (type, member) pair");
    }

    // --------------------------------------------------------------------------------------------- the driver

    /**
     * Hold the three members against the old generators over EVERY model of one workspace. The old generators run
     * ONCE each, over every model, into their own maps; their generation errors are indexed by target path, which
     * is the key law {@link IRTypeUnit#outputKey} computes.
     */
    private static Tally compare(String what, GeneratorModel gm, IRTypeIndex index) {
        List<RModel> models = new ArrayList<>();
        gm.workspace().files().forEach(models::add);

        Map<IRTypeUnit.Member, Map<String, String>> oracle = new LinkedHashMap<>();
        Map<IRTypeUnit.Member, Set<String>> refusedByOldGenerator = new LinkedHashMap<>();
        for (IRTypeUnit.Member member : MEMBERS) {
            oracle.put(member, new LinkedHashMap<>());
            refusedByOldGenerator.put(member, new LinkedHashSet<>());
        }
        var typeFormatGen = new TypeFormatValidatorGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        var cardinalityGen = new CardinalityValidatorGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        var onlyExistsGen = new OnlyExistsValidatorGenerator(gm, TYPE_TRANSLATOR, TYPE_UTIL);
        for (RModel model : models) {
            String version = gm.version(model);
            book(typeFormatGen.generateClasses(model, version,
                    oracle.get(IRTypeUnit.Member.TYPE_FORMAT_VALIDATOR)),
                    refusedByOldGenerator.get(IRTypeUnit.Member.TYPE_FORMAT_VALIDATOR));
            book(cardinalityGen.generateClasses(model, version,
                    oracle.get(IRTypeUnit.Member.CARDINALITY_VALIDATOR)),
                    refusedByOldGenerator.get(IRTypeUnit.Member.CARDINALITY_VALIDATOR));
            book(onlyExistsGen.generateClasses(model, version,
                    oracle.get(IRTypeUnit.Member.ONLY_EXISTS_VALIDATOR)),
                    refusedByOldGenerator.get(IRTypeUnit.Member.ONLY_EXISTS_VALIDATOR));
        }

        IRDerivedFacts facts = new IRDerivedFacts(index, IRDerivedLie.NONE);
        Map<IRTypeUnit.Member, IRTypeUnit.MemberEmitter> emitters = new LinkedHashMap<>();
        emitters.put(IRTypeUnit.Member.TYPE_FORMAT_VALIDATOR,
                new IRTypeFormatValidatorEmitter(index, facts));
        emitters.put(IRTypeUnit.Member.CARDINALITY_VALIDATOR,
                new IRCardinalityValidatorEmitter(index, facts));
        emitters.put(IRTypeUnit.Member.ONLY_EXISTS_VALIDATOR,
                new IROnlyExistsValidatorEmitter(index, facts));

        int types = 0;
        int files = 0;
        int refusedKeys = 0;
        for (RModel model : models) {
            for (var element : model.rootElements()) {
                if (!(element instanceof RDataType dataType)) {
                    continue;
                }
                types++;
                IRTypeNode node = index.node(model.namespace(), dataType);
                for (IRTypeUnit.Member member : MEMBERS) {
                    String key = IRTypeUnit.outputKey(node, member);
                    boolean oldRefused = refusedByOldGenerator.get(member).contains(key);
                    String expected = oracle.get(member).get(key);
                    if (oldRefused) {
                        assertNull(expected, what + " / " + key
                                + ": the old generator both refused and wrote this key");
                        IRValidatorScan.NamedRefusal refusal = assertThrows(
                                IRValidatorScan.NamedRefusal.class,
                                () -> emitters.get(member).emit(node),
                                what + " / " + key + ": the old generator REFUSED this file, so the IR member"
                                        + " must refuse it too - by a NAMED site, never by writing it");
                        assertNotNull(refusal.site(), "a refusal names its site");
                        refusedKeys++;
                        continue;
                    }
                    assertNotNull(expected, what + " / " + key
                            + ": the old generator neither wrote nor refused this key - the path law disagrees");
                    assertEquals(expected, emitters.get(member).emit(node).orElseThrow(),
                            what + " / " + member + " / " + key + ": the WHOLE file, byte for byte off the old"
                                    + " generator's own generateClasses output for the same parsed model");
                    files++;
                }
            }
        }
        return new Tally(types, files, refusedKeys);
    }

    /** Index one pass's generation errors by the target path each names - the key its file would have taken. */
    private static void book(List<GenerationException> errors, Set<String> sink) {
        for (GenerationException error : errors) {
            assertNotNull(error.getTargetPath(),
                    "a refusal with no target path names no file: " + error.getMessage());
            sink.add(error.getTargetPath().replace('\\', '/'));
        }
    }
}
