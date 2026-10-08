package com.regnosys.rosetta.generator.java.ir;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.object.PojoSectionOracle;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ir.adapter.IRTypeNode;

import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.COLLIDE_LIB_NAMESPACE;
import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.COLLIDE_NAMESPACE;
import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.NAMESPACE;
import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.choice;
import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.collisionFixture;
import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.dataType;
import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.fixture;
import static com.regnosys.rosetta.generator.java.ir.IRPropertyModelTest.property;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.3 seat 9 (PR #645 commit 4) - THE DATA-TYPE POJO EMITTER, SECTION BY SECTION, AGAINST THE OLD GENERATOR'S OWN
 * BYTES. Each rendered section of {@link IRDataTypeEmitter} is held against the text
 * {@code ModelObjectGenerator.buildBody} wrote for the SAME fixture type, read through the commit's package-private
 * seam ({@link PojoSectionOracle}). The two halves are two PRODUCERS (LAW 69): the IR half computes the section from
 * {@link IRTypeNode} + {@link IRTypeIndex} + {@link IRPropertyModel} + the emitter's config and never touches an AST
 * node; the oracle half builds {@code RJavaPojoInterface} and reads what it writes.
 *
 * <p><b>THE COMPARISON IS RAW, sentinels and all.</b> Both halves write the D50 first-claim sentinels at their type
 * positions and neither resolves them here, so the comparison is of the bytes BEFORE
 * {@code ImportCollisionResolver.resolveClass} runs - which is the only honest place to compare a SECTION, since the
 * resolution is a whole-class pass.
 *
 * <p><b>THE TWO FACTS COMMIT 5 ADDED have their own witnesses</b> in the middle of this class (v3.3 seat 9, PR #645
 * commit 5 - the gate commit): {@code EnumHolder} (an ENUM-item list, whose getter must NOT take the
 * {@code ? extends} arm) and {@code ChainLeaf} (a depth-2 specialization chain with a cardinality change in it,
 * whose rungs decide the import set). Commit 4 REFUSED both carriers by name; each is now rendered and held against
 * the old generator's own bytes, and {@link #DECIDABLE} is the whole fixture population rather than the twelve
 * types the gaps left standing.
 */
class IRDataTypeEmitterTest {

    /** The host's version stamp - given to BOTH halves, because it is the generator's fact and not the model's. */
    private static final String VERSION = "1.2.3";

    /**
     * EVERY {@code type} OF THE FIXTURE (v3.3 seat 9, PR #645 commit 5 - the gate commit). Commit 4 could only hold
     * twelve of them against the oracle: the other nine carried a MULTI property of a declared item type (its named
     * gap 1) or a specialization chain (gap 2), and the emitter refused them by name. The property surface carries
     * both facts now, so the list is the whole population - enumerated rather than derived, because a list a test
     * computes from the fixture can silently shrink with it (Rule 4).
     */
    private static final List<String> DECIDABLE = List.of(
            "Leaf", "Sub", "Base", "Child", "ListToSingle", "ListToSingleOther", "Incompatible",
            "SeedBase", "SeedChild", "Keyed", "Templated", "MetaCarrier", "Flags", "Documented",
            "MetaBase", "MetaChild", "ChoiceExtender", "EnumHolder", "ChainTop", "ChainMid", "ChainLeaf");

    /**
     * Section 6's population is the same one now. The constant is kept, and kept EQUAL, so the lane table and the
     * commit-4 reading of these tests still name the same thing.
     */
    private static final List<String> GETTERS_DECIDABLE = DECIDABLE;

    /**
     * The emitter under an EMPTY pruning set - the config every test but the pruning one takes (v3.3 seat 9,
     * PR #645 commit 9 re-cut {@code Config}'s second component from a boolean nothing read to the generator
     * model's own {@code doNotPrune} set). An empty set is the D11 corpus's own shape for every cell but iso.
     */
    private static IRDataTypeEmitter emitter(IRPropertyModelTest.Fixture f) {
        return new IRDataTypeEmitter(f.index(), new IRDataTypeEmitter.Config(VERSION, Set.of()),
                new JavaTypeUtil());
    }

    private static IRTypeNode node(IRPropertyModelTest.Fixture f, String typeName) {
        return f.index().node(NAMESPACE, dataType(f.lib(), typeName));
    }

    private static PojoSectionOracle.Sections oracle(IRPropertyModelTest.Fixture f, String typeName) {
        RDataType type = dataType(f.lib(), typeName);
        return PojoSectionOracle.of(type, f.gm(), VERSION);
    }

    // ================================================================= v3.3 seat 10 (PR #646 commit 4): THE CHOICE

    /**
     * THE FIXTURE'S CHOICES, enumerated rather than derived (Rule 4): {@code Either} (two options, one of them
     * defined, NO metadata) and {@code KeyedChoice} (three defined options and a {@code [metadata key]}, which is
     * what puts {@code GlobalKey} / {@code MetaFields} into a choice POJO). {@code TemplatedChoice} is NOT here:
     * the old generator refuses a top-level choice carrying {@code [metadata template]} and so does the property
     * model ({@code IRPropertyModel:508-512}), which is its own witness in {@code IRPropertyModelTest}.
     */
    private static final List<String> CHOICES = List.of("Either", "KeyedChoice");

    /**
     * THE VERSION A CHOICE IS RENDERED WITH - <b>THE SIXTH LEGACY DIFFERENCE, FOUND BY THIS TEST AND PINNED
     * HERE</b> (v3.3 seat 10, PR #646 commit 4). The contract named FIVE {@code type == null} differences in
     * {@code ModelObjectGenerator}; there is a SIXTH, and it is inside difference (4).
     *
     * <p>The data-type path writes the javadoc's {@code @version} line from the VERSION ARGUMENT
     * {@code buildModel} was handed ({@code :572-574}: {@code generatorUtil.javadoc(definition, docReferences,
     * version)}). The CHOICE path writes it from {@code pojo.getJavadoc()} ({@code :576}), which is
     * {@code generatorUtil.javadoc(definition, List.of(), getVersion())} ({@code RJavaPojoInterface:114-118}) -
     * and {@code getVersion()} is the DECLARING MODEL'S OWN version ({@code :132-141}: {@code model.version()},
     * defaulted to {@code 0.0.0}), never the argument. The {@code @RosettaDataType} / {@code @RuneDataType}
     * annotation lines keep taking the ARGUMENT on both paths, so the two sources sit in ONE file.
     *
     * <p><b>WHY IT MOVES NO BYTE ANYWHERE.</b> Every caller of this pipeline passes exactly
     * {@code GeneratorModel.version(model)} for the model it is rendering - the D11 host, the plugin runner and
     * the IR passes alike - and {@code RJavaPojoInterface.getVersion()} is that same expression. The two sources
     * therefore agree on every cell of the corpus, which is what the 167 identical choice renders of this
     * commit's two scoped cells read. They can only disagree when a caller renders a declaration under a version
     * that is not its own model's, and no route does.
     *
     * <p>So the IR emitter, whose only version is {@link IRDataTypeEmitter.Config#version()}, is CORRECT for
     * every caller that exists - and these fixture tests drive it with the FIXTURE MODEL'S OWN version, which is
     * what the corpus does. {@link #theChoiceJavadocTakesTheDECLARINGMODELsVersionInTheOldGenerator} pins the
     * difference itself, so a reader of this class cannot mistake it for an oversight.
     */
    private static final String CHOICE_VERSION = "1.0.0";

    /** The emitter under the fixture model's OWN version - see {@link #CHOICE_VERSION}. */
    private static IRDataTypeEmitter choiceEmitter(IRPropertyModelTest.Fixture f) {
        return new IRDataTypeEmitter(f.index(), new IRDataTypeEmitter.Config(CHOICE_VERSION, Set.of()),
                new JavaTypeUtil());
    }

    private static IRTypeNode choiceNode(IRPropertyModelTest.Fixture f, String choiceName) {
        return f.index().node(NAMESPACE, choice(f.lib(), choiceName));
    }

    /** The LEGACY choice pipeline's own answers for a fixture choice - {@code ChoiceObjectGenerator.generate}'s. */
    private static PojoSectionOracle.Sections choiceOracle(IRPropertyModelTest.Fixture f, String choiceName) {
        return PojoSectionOracle.ofChoice(choice(f.lib(), choiceName), f.gm(), CHOICE_VERSION);
    }

    /**
     * THE SIXTH DIFFERENCE, PINNED (v3.3 seat 10, PR #646 commit 4) - see {@link #CHOICE_VERSION} for the law and
     * for why it moves no byte on any route. Driven with a version that is NOT the fixture model's, the OLD
     * generator's choice javadoc keeps the MODEL'S version while its annotation lines take the argument; the IR
     * emitter has only its config's version and writes that in both places. The divergence is therefore REAL,
     * it is exactly ONE line, and it is unreachable through any caller.
     *
     * <p>A test that only ever drove both halves with the model's own version would hide this; this one states
     * it, so the next reader inherits the finding rather than rediscovering it.
     */
    @Test
    void theChoiceJavadocTakesTheDECLARINGMODELsVersionInTheOldGenerator() {
        IRPropertyModelTest.Fixture f = fixture();
        String foreign = "9.9.9";
        assertFalse(CHOICE_VERSION.equals(foreign), "the two versions must differ for this to say anything");
        PojoSectionOracle.Sections underForeign =
                PojoSectionOracle.ofChoice(choice(f.lib(), "Either"), f.gm(), foreign);
        assertEquals("/**\n * @version " + CHOICE_VERSION + "\n */\n",
                underForeign.sections().get("TYPE_JAVADOC"),
                "the OLD generator's choice javadoc keeps the DECLARING MODEL's version even when the caller"
                        + " passes another (RJavaPojoInterface:114-118 -> :132-141)");
        assertTrue(underForeign.sections().get("ANNOTATIONS").contains("version=\"" + foreign + "\""),
                "while its annotation lines DO take the caller's: " + underForeign.sections().get("ANNOTATIONS"));
        // and the IR emitter, which has only its config's version, writes that in both places
        IRDataTypeEmitter foreignEmitter = new IRDataTypeEmitter(f.index(),
                new IRDataTypeEmitter.Config(foreign, Set.of()), new JavaTypeUtil());
        assertEquals("/**\n * @version " + foreign + "\n */\n",
                foreignEmitter.typeJavadoc(choiceNode(f, "Either")),
                "the IR emitter has ONE version - its config's - and no caller ever hands it one that is not the"
                        + " declaring model's, which is why this difference moves no byte (see CHOICE_VERSION)");
    }

    /**
     * CASE (a) and CASE (b) TOGETHER - THE WHOLE FILE. The emitter's CHOICE arm is held against the OLD generator's
     * own {@code renderPojoFile} for the same declaration, through the same {@code buildModelForChoice} the legacy
     * {@code ChoiceObjectGenerator} calls: {@code KeyedChoice} carries a {@code [metadata key]} and three DEFINED
     * options (case (a)), {@code Either} carries no metadata at all (case (b)). The whole file is the honest
     * oracle, exactly as it is for a data type, because the D50 first-claim resolution is a whole-class pass.
     *
     * <p>MUTANTS, each proved to make this RED: the {@code RuneChoiceType} IMPORT arm dropped
     * ({@code IRDataTypeEmitter.imports}), the {@code @RuneChoiceType} LINE arm dropped
     * ({@code IRDataTypeEmitter.annotations}), and the CHOICE kind removed from
     * {@code IRDataTypeEmitter.propertiesOf} (which refuses the node instead). Lanes C1, C3 and C5.
     */
    @Test
    void theWholeChoicePojoIsTheOldGeneratorsOwnOnEveryChoiceOfTheFixture() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = choiceEmitter(f);
        for (String name : CHOICES) {
            assertEquals(choiceOracle(f, name).wholeFile(), emitter.render(choiceNode(f, name)),
                    "the WHOLE choice POJO - the frame, the resolved import block and every section - is the old"
                            + " generator's own renderPojoFile for " + name);
        }
    }

    /**
     * THE FIVE ARMS, SECTION BY SECTION, so a divergence names the section rather than the file. The
     * {@code ANNOTATIONS} section carries arm (5), the {@code TYPE_JAVADOC} section arm (4), the
     * {@code INTERFACE_DECLARATION} section arm (3) - a choice has no {@code extends} - and the
     * {@code META_DATA} section the meta-class name, which is arm (2) and needs NO arm on the IR side (both kinds
     * take the node's own namespace and simple name). Arm (1), the import, has no section of its own: it is in the
     * import block, which the whole-file test above holds.
     */
    @Test
    void everySectionOfAChoicePojoIsTheOldGeneratorsOwn() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = choiceEmitter(f);
        for (String name : CHOICES) {
            IRTypeNode n = choiceNode(f, name);
            PojoSectionOracle.Sections expected = choiceOracle(f, name);
            assertEquals(expected.sections().get("TYPE_JAVADOC"), emitter.typeJavadoc(n),
                    "section 2 (the javadoc, arm 4 - the definition with NO doc references) on " + name);
            assertEquals(expected.sections().get("ANNOTATIONS"), emitter.annotations(n),
                    "section 3 (arm 5 - @RuneChoiceType after @RuneDataType) on " + name);
            assertEquals(expected.sections().get("INTERFACE_DECLARATION"),
                    emitter.interfaceDeclaration(n, emitter.propertiesOf(n)),
                    "section 4 (arm 3 - a choice extends nothing) on " + name);
            assertEquals(expected.sections().get("META_DATA"), emitter.metaDataField(n),
                    "section 5 (arm 2 - the meta class from the choice's own namespace and name) on " + name);
            assertEquals(expected.rawImports(), emitter.imports(n, emitter.propertiesOf(n)),
                    "arm 1 - the UNRESOLVED import set, RuneChoiceType included, on " + name);
        }
    }

    /**
     * CASE (d) - THE {@code @RuneChoiceType} LINE IS PRESENT EXACTLY ONCE, AFTER {@code @RuneDataType}, and a
     * {@code data} type carries none. Stated on the emitter's own text rather than only through the oracle, so the
     * law survives an oracle that ever agreed with a wrong emitter.
     *
     * <p>MUTANT: the arm in {@code IRDataTypeEmitter.annotations} dropped (lane C1).
     */
    @Test
    void theRuneChoiceTypeLineIsWrittenExactlyOnceAndOnlyForAChoice() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        for (String name : CHOICES) {
            String annotations = emitter.annotations(choiceNode(f, name));
            List<String> lines = List.of(annotations.split("\\n", -1));
            long runeChoice = lines.stream().filter(l -> l.equals("@" + IRDataTypeEmitter.T_RUNE_CHOICE_TYPE)).count();
            assertEquals(1, runeChoice, "exactly one @RuneChoiceType line on " + name + ": " + annotations);
            int choiceAt = lines.indexOf("@" + IRDataTypeEmitter.T_RUNE_CHOICE_TYPE);
            int dataAt = -1;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith("@" + IRDataTypeEmitter.T_RUNE_DATA_TYPE + "(")) {
                    dataAt = i;
                }
            }
            assertTrue(dataAt >= 0 && choiceAt == dataAt + 1,
                    "and it stands immediately AFTER @RuneDataType (ModelObjectGenerator:594-599) on " + name
                            + ": " + annotations);
        }
        assertFalse(emitter.annotations(node(f, "Leaf")).contains("RuneChoiceType"),
                "a data type carries NO @RuneChoiceType line");
    }

    /**
     * CASE (c) - A CHOICE NODE THAT DECLARES DOC REFERENCES RENDERS NONE OF THEM. The javadoc of a choice is
     * {@code generatorUtil.javadoc(definition, List.of(), version)} ({@code RJavaPojoInterface:114-118}), never the
     * node's own doc-reference list, and a data type's is the opposite.
     *
     * <p><b>WHY THE NODE IS HAND-BUILT.</b> No CHOICE node the ADAPTER produces can carry a doc reference at all:
     * {@code RChoice} declares no {@code docReferences()} accessor and {@code AstToIRAdapter.adaptChoice:240-243}
     * passes {@code List.of()}. So the corpus cannot witness this arm - the probe over the two scoped cells of this
     * commit reads ZERO choices with a doc reference, by construction rather than by luck - and a fixture node
     * carrying one is the only way to state the emitter's OWN law instead of inheriting three other files'.
     *
     * <p>MUTANT: the arm in {@code IRDataTypeEmitter.typeJavadoc} dropped, so the node's own doc references are
     * threaded (lane C2, which is this test rather than a cell for exactly the reason above).
     */
    @Test
    void aChoiceNodeCarryingDocReferencesRendersNoneOfThemInItsJavadoc() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRTypeNode bare = choiceNode(f, "Either");
        assertTrue(bare.docReferences().isEmpty(),
                "the adapter gives a choice NO doc references - the arm's own reason to be coded here");
        // a REAL doc reference, taken from the fixture's own `Documented.described` property rather than
        // constructed here: the point of the case is the CHOICE arm, not a hand-rolled reference the renderer
        // might treat differently from one the adapter built
        List<com.regnosys.rosetta.ir.core.IRDocReference> real =
                property(IRPropertyModelTest.model(f, "Documented"), "described").docReferences();
        assertFalse(real.isEmpty(), "the fixture's Documented.described carries a docReference");
        IRTypeNode carrying = withDocReferences(bare, real);
        assertFalse(carrying.docReferences().isEmpty(), "the fixture node DOES carry one");
        assertEquals(emitter.typeJavadoc(bare), emitter.typeJavadoc(carrying),
                "a CHOICE node's javadoc is its definition alone - the doc references it carries change NOTHING"
                        + " (RJavaPojoInterface:114-118)");
        assertFalse(emitter.typeJavadoc(carrying).contains("Agreement One"),
                "and none of the reference's text reaches the block: " + emitter.typeJavadoc(carrying));
    }

    /**
     * CASE (e) - A CHOICE NODE THAT REPORTS A BASE TYPE IS REFUSED BY NAME. A choice has no supertype anywhere in
     * the old generator's world ({@code ModelObjectGenerator:337-339} forces {@code extendsChoice} false,
     * {@code RJavaPojoInterface:155-161} answers {@code null}), so a CHOICE node with an ancestry would make the
     * emitter write an {@code extends} clause the old generator never writes. The emitter asserts it rather than
     * assuming it, and refuses the WHOLE node.
     *
     * <p>MUTANT: the {@code baseType().isPresent()} guard in {@code IRDataTypeEmitter.propertiesOf} dropped - the
     * node then renders, with an ancestry, and the message below never appears.
     */
    @Test
    void aChoiceNodeThatReportsABaseTypeIsRefusedWholeByName() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRTypeNode flat = choiceNode(f, "Either");
        assertTrue(flat.baseType().isEmpty(), "a real choice reports no base type");
        // the base type reference of the fixture's own `ChoiceExtender extends Either` - a REAL IRType reference
        IRTypeNode withParent = withBaseType(flat, node(f, "ChoiceExtender").baseType().orElseThrow());
        GenerationException refusal = assertThrows(GenerationException.class,
                () -> emitter.propertiesOf(withParent));
        assertTrue(refusal.getMessage().contains("seat8.props.Either"), refusal.getMessage());
        assertTrue(refusal.getMessage().contains("reports a base type"), refusal.getMessage());
        assertTrue(refusal.getMessage().contains("a choice has NO supertype"),
                "the refusal states the law it is enforcing: " + refusal.getMessage());
    }

    /** The same node with a doc-reference list - every other fact of the node kept as the adapter minted it. */
    private static IRTypeNode withDocReferences(IRTypeNode node,
            List<com.regnosys.rosetta.ir.core.IRDocReference> docReferences) {
        return rebuild(node, node.baseType(), docReferences);
    }

    /** The same node with a BASE TYPE - the one fact a real CHOICE node never carries. */
    private static IRTypeNode withBaseType(IRTypeNode node, com.regnosys.rosetta.ir.core.IRType baseType) {
        return rebuild(node, java.util.Optional.of(baseType), node.docReferences());
    }

    private static IRTypeNode rebuild(IRTypeNode node, java.util.Optional<com.regnosys.rosetta.ir.core.IRType> base,
            List<com.regnosys.rosetta.ir.core.IRDocReference> docReferences) {
        return new IRTypeNode(node.name(), node.kind(), node.fields(), base, node.isAbstract(),
                node.sourceRange(), node.metadata(), node.namespace(), node.resolvedName(), node.definition(),
                docReferences, node.annotations(), node.conditionNames());
    }

    // ------------------------------------------------------------------------------------------ L0: the sections

    @Test
    void section2TheTypeJavadocIsTheOldGeneratorsOwnOnEveryDecidableType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        for (String name : DECIDABLE) {
            assertEquals(oracle(f, name).sections().get("TYPE_JAVADOC"), emitter.typeJavadoc(node(f, name)),
                    "section 2 (the type javadoc, with its @version line) on " + name);
        }
    }

    @Test
    void section3TheAnnotationsAreTheOldGeneratorsOwnOnEveryDecidableType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        for (String name : DECIDABLE) {
            assertEquals(oracle(f, name).sections().get("ANNOTATIONS"), emitter.annotations(node(f, name)),
                    "section 3 (@RosettaDataType / @RuneDataType, the model= short name and the version) on " + name);
        }
    }

    @Test
    void section4TheInterfaceDeclarationIsTheOldGeneratorsOwnOnEveryDecidableType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        for (String name : DECIDABLE) {
            IRTypeNode n = node(f, name);
            assertEquals(oracle(f, name).sections().get("INTERFACE_DECLARATION"),
                    emitter.interfaceDeclaration(n, emitter.propertiesOf(n)),
                    "section 4 (the extends clause and the interface list) on " + name);
        }
    }

    @Test
    void section4CarriesGlobalKeyAndTemplatableExactlyWhereTheDeclarationCarriesTheMetadata() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRTypeNode keyed = node(f, "Keyed");
        IRTypeNode templated = node(f, "Templated");
        IRTypeNode leaf = node(f, "Leaf");
        assertEquals(List.of("com.rosetta.model.lib.RosettaModelObject", "com.rosetta.model.lib.GlobalKey"),
                emitter.interfaceDeclarationFqns(keyed, emitter.propertiesOf(keyed)),
                "[metadata key] alone adds GlobalKey");
        assertEquals(List.of("com.rosetta.model.lib.RosettaModelObject", "com.rosetta.model.lib.GlobalKey",
                        "com.rosetta.model.lib.Templatable"),
                emitter.interfaceDeclarationFqns(templated, emitter.propertiesOf(templated)),
                "[metadata template] adds Templatable AFTER GlobalKey, in the old generator's own order");
        assertEquals(List.of("com.rosetta.model.lib.RosettaModelObject"),
                emitter.interfaceDeclarationFqns(leaf, emitter.propertiesOf(leaf)),
                "a plain type carries neither");
    }

    @Test
    void section5TheMetaDataFieldIsTheOldGeneratorsOwnOnEveryDecidableType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        for (String name : DECIDABLE) {
            assertEquals(oracle(f, name).sections().get("META_DATA"), emitter.metaDataField(node(f, name)),
                    "section 5 (the metaData static field and its <ns>.meta.<Simple>Meta class) on " + name);
        }
    }

    @Test
    void section6TheGettersAreTheOldGeneratorsOwnOnEveryDecidableType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        for (String name : GETTERS_DECIDABLE) {
            IRTypeNode n = node(f, name);
            assertEquals(oracle(f, name).sections().get("GETTERS"), emitter.getters(emitter.propertiesOf(n)),
                    "section 6 (the banner, the per-property javadoc, @Override and the return types) on " + name);
        }
    }

    @Test
    void section7TheBuildMethodsAreTheOldGeneratorsOwnOnEveryDecidableType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        for (String name : DECIDABLE) {
            assertEquals(oracle(f, name).sections().get("BUILD_METHODS"), emitter.buildMethods(node(f, name)),
                    "section 7 (the Build Methods banner, build(), toBuilder() and the static builder() factory,"
                            + " with the old generator's own TAB-on-its-own-line separators) on " + name);
        }
    }

    @Test
    void section8TheUtilityMethodsAreTheOldGeneratorsOwnOnEveryDecidableType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        for (String name : DECIDABLE) {
            assertEquals(oracle(f, name).sections().get("UTILITY_METHODS"), emitter.utilityMethods(node(f, name)),
                    "section 8 (metaData() over the RosettaMetaData token, and getType() under"
                            + " @RuneAttribute(\"@type\") with Class written bare) on " + name);
        }
    }

    @Test
    void section1TheImportSetAndTheStaticImportsAreTheOldGeneratorsOwnOnEveryDecidableType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        for (String name : DECIDABLE) {
            IRTypeNode n = node(f, name);
            IRPropertyModel properties = emitter.propertiesOf(n);
            PojoSectionOracle.Sections expected = oracle(f, name);
            assertEquals(expected.imports(), emitter.imports(n, properties),
                    "section 1's import SET on " + name);
            assertEquals(expected.staticImports(), emitter.staticImports(n, properties),
                    "section 1's STATIC import set on " + name);
            assertEquals(expected.packageName(), IRDataTypeEmitter.packageOf(n),
                    "section 1's package on " + name);
        }
    }

    @Test
    void section1TheFileFrameRendersThePackageAndTheImportBlock() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRTypeNode keyed = node(f, "Keyed");
        String header = emitter.header(keyed, emitter.propertiesOf(keyed));
        assertTrue(header.startsWith("package seat8.props;\n"), "the frame opens on the package: " + header);
        assertTrue(header.contains("import com.rosetta.model.lib.GlobalKey;\n"),
                "and carries the import block the set decided: " + header);
        assertTrue(header.contains("import static java.util.Optional.ofNullable;\n"),
                "and the static import block: " + header);
    }

    @Test
    void section14IsEmptyOnEveryTypeBecauseItsOwnPositionAppendsNoByte() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        for (String name : DECIDABLE) {
            IRTypeNode n = node(f, name);
            assertEquals("", emitter.compatMembers(emitter.propertiesOf(n)),
                    "ModelObjectGenerator:621-623 only BUILDS the PojoCompatEmitter (or leaves it null) and appends"
                            + " nothing; every arm it writes lands in sections 11 and 12. So the section is empty on"
                            + " a SPECIALIZED type too - and since commit 5 it no longer refuses one: " + name);
        }
    }

    // ------------------------------------------- 9 and 10: THE PROCESS METHOD AND THE WHOLE BUILDER INTERFACE

    @Test
    void section9TheProcessMethodIsTheOldGeneratorsOwnOnEveryDecidableType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        for (String name : DECIDABLE) {
            IRTypeNode n = node(f, name);
            assertEquals(oracle(f, name).sections().get("PROCESS"), emitter.process(emitter.propertiesOf(n)),
                    "section 9 (the interface's default process(RosettaPath, Processor): one line per property of"
                            + " the WHOLE surface, the processRosetta / processBasic split decided by the item-KIND"
                            + " fact, the [metadata id] flag where the property carries it) on " + name);
        }
        IRPropertyModelTest.Fixture c = collisionFixture();
        IRDataTypeEmitter collide = emitter(c);
        for (String name : List.of("ListLoser", "ListWinner")) {
            IRTypeNode n = collideNode(c, name);
            assertEquals(collideOracle(c, name).sections().get("PROCESS"), collide.process(collide.propertiesOf(n)),
                    "section 9 on the COLLIDING fixture, where the value-site sentinel is what decides the"
                            + " spelling of the .class reference on " + name);
        }
    }

    @Test
    void section9NamesEachOfItsThreeArmsByTheFactThatDecidesIt() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);

        String flags = emitter.process(emitter.propertiesOf(node(f, "Flags")));
        assertTrue(flags.contains("processRosetta(path.newSubPath(\"ided\"), processor, "
                        + IRDataTypeEmitter.valueSiteTypeRef("com.rosetta.model.metafields.FieldWithMetaString")
                        + ".class, getIded(), " + IRDataTypeEmitter.T_ATTRIBUTE_META + ".GLOBAL_KEY_FIELD);\n"),
                "the META flag is the list form of ModelObjectBoilerplate:270-275's nullable single - Flags.ided"
                        + " carries [metadata id], so its line ends on AttributeMeta.GLOBAL_KEY_FIELD. Note the ARM:"
                        + " a [metadata id] attribute is meta-WRAPPED, so its item is the generated"
                        + " FieldWithMetaString and the line is the ROSETTA one, not the basic one - the two facts"
                        + " are independent and this witness holds both: " + flags);

        String holder = emitter.process(emitter.propertiesOf(node(f, "EnumHolder")));
        assertTrue(holder.contains("processor.processBasic(path.newSubPath(\"colours\"), "
                        + IRDataTypeEmitter.valueSiteTypeRef("seat8.props.Colour") + ".class, getColours(), this);\n"),
                "an ENUM item is no RosettaModelObject, so EnumHolder.colours takes the BASIC arm: " + holder);

        String base = emitter.process(emitter.propertiesOf(node(f, "Base")));
        assertTrue(base.contains("processRosetta(path.newSubPath(\"leaves\"), processor, "
                        + IRDataTypeEmitter.valueSiteTypeRef("seat8.props.Leaf") + ".class, getLeaves());\n"),
                "and a `type` item IS one, so Base.leaves takes the ROSETTA arm - which visits the VALUE site,"
                        + " not the X.XBuilder the builder-process arm of section 10 visits: " + base);
    }

    /**
     * THE ONE REFUSAL SECTION 9 CAN RAISE, WITNESSED (v3.3 seat 9, PR #645 commit 7). The old generator's meta flag
     * is a NULLABLE SINGLE {@code AttributeMeta} ({@code ModelObjectBoilerplate:270-275}); the IR carries the LIST
     * form of that fact. A surface stating TWO of them is one the old generator can never produce, and the emitter
     * must not pick between them - it names the fact and refuses. No fixture and no corpus cell carries the shape,
     * so it is stated here directly through {@link IRPropertyModel#stating}: a refusal channel nothing exercises
     * is a claim, not a mechanism.
     */
    @Test
    void section9RefusesByNameRatherThanPickBetweenTwoAttributeMetaFacts() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRPropertyModel.IRProperty ided = property(emitter.propertiesOf(node(f, "Flags")), "ided");
        IRPropertyModel.IRProperty twoFlags = new IRPropertyModel.IRProperty(
                ided.name(), ided.javaType(), ided.required(), ided.multi(),
                ided.getterCompatibilityName(), ided.setterCompatibilityName(), ided.getterOverridesParentGetter(),
                ided.compatibleTypeWithParent(), ided.sameTypeAsParent(), ided.parentChainDepth(),
                ided.metaValueType(), ided.hasLocation(), ided.attributeMetaTypes(),
                List.of("GLOBAL_KEY_FIELD", "GLOBAL_KEY_FIELD"),
                ided.docReferences(), ided.synthetic(), ided.inheritedChoiceOption(), ided.definition(),
                ided.javadoc(), ided.itemIsRosettaModelObject(), ided.parentChain(),
                ided.itemIsEnum(), ided.metaValueIsRosettaModelObject(), ided.runeName());
        IRPropertyModel lying = IRPropertyModel.stating(List.of(twoFlags), List.of(twoFlags));
        IRDataTypeEmitter.MissingIRFact refusal = assertThrows(IRDataTypeEmitter.MissingIRFact.class,
                () -> emitter.process(lying));
        assertEquals("property.ided.attributeMeta", refusal.fact(),
                "the refusal NAMES the fact it wanted, so the D11 POJO SHADOW line books it as refused with a"
                        + " reason rather than as a byte nobody can account for: " + refusal.getMessage());
    }

    @Test
    void section10TheBuilderInterfaceIsTheOldGeneratorsOwnOnEveryDecidableType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        for (String name : DECIDABLE) {
            IRTypeNode n = node(f, name);
            assertEquals(oracle(f, name).sections().get("BUILDER_INTERFACE"),
                    emitter.builderInterface(n, emitter.propertiesOf(n)),
                    "section 10 (the declaration, the getOrCreate pairs, the GENERATION-MAJOR setter groups, the"
                            + " builder's own process and prune()) on " + name);
        }
        IRPropertyModelTest.Fixture c = collisionFixture();
        IRDataTypeEmitter collide = emitter(c);
        for (String name : List.of("ListLoser", "ListWinner")) {
            IRTypeNode n = collideNode(c, name);
            assertEquals(collideOracle(c, name).sections().get("BUILDER_INTERFACE"),
                    collide.builderInterface(n, collide.propertiesOf(n)),
                    "section 10 on the COLLIDING fixture, where fileWrittenSimpleNames is read off the UNRESOLVED"
                            + " import set and so decides the parameter names on " + name);
        }
    }

    @Test
    void section10PutsEveryAncestorGenerationBeforeTheMainOneAndOverridesOnlyTheAncestorsLines() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRTypeNode child = node(f, "Child");
        String section = emitter.builderInterface(child, emitter.propertiesOf(child));

        int inheritedPlain = section.indexOf("\t\t@Override\n\t\tChild.ChildBuilder setPlain(");
        int ownLine = section.indexOf("\n\t\tChild.ChildBuilder setOwn(");
        assertTrue(inheritedPlain >= 0,
                "Base's generation contributes `plain` - a property Child redeclares as Case 0, so it is the"
                        + " ANCESTOR's line that carries it, under @Override: " + section);
        assertTrue(ownLine >= 0, "and Child's own `own` is declared bare: " + section);
        assertTrue(inheritedPlain < ownLine,
                "the walk is GENERATION-MAJOR (ModelObjectGenerator:846-857): the ROOT ancestor's own properties"
                        + " come first, the main generation's last: " + section);
    }

    @Test
    void section10TakesTheMetaValueSetterArmExactlyWhereThePropertyCarriesAValueType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRTypeNode carrier = node(f, "MetaCarrier");
        String section = emitter.builderInterface(carrier, emitter.propertiesOf(carrier));
        assertTrue(section.contains("setRefdValue("),
                "MetaCarrier.refd is a SINGLE with a meta value, so its group is setRefd + setRefdValue: " + section);
        assertTrue(section.contains("addManyValue("),
                "MetaCarrier.many is a LIST with one, so its group carries the four *Value lines: " + section);
        IRTypeNode base = node(f, "Base");
        assertFalse(emitter.builderInterface(base, emitter.propertiesOf(base)).contains("Value("),
                "and a type with no meta-wrapped property writes not one *Value line");
    }

    @Test
    void section10EscapesAChoiceOptionsParameterBecauseTheFileWritesItsNameAsAType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRTypeNode extender = node(f, "ChoiceExtender");
        String section = emitter.builderInterface(extender, emitter.propertiesOf(extender));
        assertTrue(section.contains(" _Leaf);"),
                "ChoiceExtender inherits the choice's options, whose NAMES are their own type names - the file"
                        + " writes `Leaf` as a type, so the parameter escapes to _Leaf"
                        + " (ModelObjectGenerator:1726-1744 over fileWrittenSimpleNames): " + section);
        assertTrue(section.contains(" _Sub);"), "and the second option likewise: " + section);
    }

    @Test
    void section10PutsAListToSingleSpecializationUnderItsOwnGenerationNotTheAncestors() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRTypeNode listToSingle = node(f, "ListToSingle");
        String section = emitter.builderInterface(listToSingle, emitter.propertiesOf(listToSingle));

        String single = "\t\tListToSingle.ListToSingleBuilder setLeaves("
                + IRDataTypeEmitter.valueSiteTypeRef("seat8.props.Leaf") + " leaves);\n";
        int at = section.indexOf(single);
        assertTrue(at >= 0, "the specialization declares the SINGLE-typed setter: " + section);
        assertFalse(section.startsWith("\t\t@Override\n", at - "\t\t@Override\n".length()),
                "and it is the MAIN generation's line, so it is bare - while Base's list-shaped `leaves` group"
                        + " above it carries @Override on every line: " + section);
        assertTrue(section.indexOf("\t\t@Override\n\t\tListToSingle.ListToSingleBuilder addLeaves(") >= 0
                        && section.indexOf("\t\t@Override\n\t\tListToSingle.ListToSingleBuilder addLeaves(") < at,
                "Base's own generation writes the list group first, under @Override: " + section);
    }

    // ------------------------------------------------------ THE SETTER SHAPE MATRIX (PR #645 commit 7, § 4)

    /** The setter fixture's own population - its own namespace, so {@link #DECIDABLE} stays what it is. */
    /**
     * The setter-shape fixture's population - the two of commit 7 plus the three section-12 escape carriers of
     * commit 9 ({@code Indexed} the {@code _index} parameter, {@code Resulting} the {@code _result} local,
     * {@code Located} the scoped-key {@code addKey} arm in BOTH its shapes). Enumerated, not derived (Rule 4).
     */
    private static final List<String> SETTER_SHAPES =
            List.of("SLeaf", "Setters", "Indexed", "Resulting", "Located");

    private static IRTypeNode settersNode(IRPropertyModelTest.Fixture f, String typeName) {
        return f.index().node(IRPropertyModelTest.SETTERS_NAMESPACE, dataType(f.lib(), typeName));
    }

    private static PojoSectionOracle.Sections settersOracle(IRPropertyModelTest.Fixture f, String typeName) {
        return PojoSectionOracle.of(dataType(f.lib(), typeName), f.gm(), VERSION);
    }

    @Test
    void section10TheSetterMatrixIsTheOldGeneratorsOwnOnEveryShape() {
        IRPropertyModelTest.Fixture f = IRPropertyModelTest.settersFixture();
        IRDataTypeEmitter emitter = emitter(f);
        for (String name : SETTER_SHAPES) {
            IRTypeNode n = settersNode(f, name);
            assertEquals(settersOracle(f, name).sections().get("BUILDER_INTERFACE"),
                    emitter.builderInterface(n, emitter.propertiesOf(n)),
                    "section 10's setter SHAPE matrix (single / single+value / list / list+value) and BOTH halves"
                            + " of the parameter-name law on " + name);
        }
    }

    @Test
    void section9IsTheOldGeneratorsOwnOnEverySetterShapeToo() {
        IRPropertyModelTest.Fixture f = IRPropertyModelTest.settersFixture();
        IRDataTypeEmitter emitter = emitter(f);
        for (String name : SETTER_SHAPES) {
            IRTypeNode n = settersNode(f, name);
            assertEquals(settersOracle(f, name).sections().get("PROCESS"), emitter.process(emitter.propertiesOf(n)),
                    "section 9 on " + name + " - the meta-wrapped properties' items are the GENERATED wrapper"
                            + " classes, so they take the ROSETTA arm whatever their value type is");
        }
    }

    @Test
    void section10EscapesAJavaKeywordParameterInBothArmsAndAFileWrittenNameInNeither() {
        IRPropertyModelTest.Fixture f = IRPropertyModelTest.settersFixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRTypeNode setters = settersNode(f, "Setters");
        String section = emitter.builderInterface(setters, emitter.propertiesOf(setters));
        assertTrue(section.contains(" _new);"),
                "`new` is a Java keyword the Rune lexer does NOT reserve, and the SINGLE arm escapes it: " + section);
        assertTrue(section.contains(" _package);"),
                "and the LIST arm escapes `package` by the same one law: " + section);
        assertTrue(section.contains(" _Object);"),
                "`Object` is not a Java keyword at all - it escapes because the FILE writes it as a type, which is"
                        + " the OTHER half of the predicate (ModelObjectGenerator:1809): " + section);
        assertTrue(section.contains("addCodesValue("),
                "the BASIC list with a meta value takes the eight-line arm: " + section);
        assertTrue(section.contains("addRefsValue("),
                "and so does the MODEL list with [metadata reference]: " + section);
    }

    // ---------------------------------------- 11-13: RENDERED, with THREE compat sub-sections refusing by name

    /**
     * RENAMED at v3.3 seat 9, PR #645 commit 10, because the commit-9 name
     * ({@code theWholeFileRendersForATypeThatNeedsNoCompatMember}) would now LIE: there is no type of any
     * fixture whose whole file this emitter does not render. EVERY {@code type} - specialized or not, compat
     * members and all - renders the WHOLE POJO file, frame, imports, every section and the class-closing brace,
     * and this test holds that whole against the old generator's own whole, which {@link PojoSectionOracle}
     * takes from {@code renderPojoFile(model)}: literally the string {@code ModelObjectGenerator.generate}
     * returns.
     *
     * <p>THE FILE IS THE ORACLE, not a concatenation of its sections: the sections are the RAW,
     * sentinel-carrying text and carry no frame at all, so re-assembling them would compare the IR emitter
     * against a second implementation of the file rather than against the generator's own output.
     *
     * <p><b>THE POPULATION IS COUNTED, not trusted</b> (Rule 4): the 21 {@code seat8.props} types, the two
     * halves of the simple-name collision pair and the five setter shapes - 28 whole files, every one of them
     * byte-equal. The commit-9 constant {@code COMPAT_REFUSING_TYPES} and its three feeder lists are GONE: a
     * list of the types this emitter cannot finish is a list with no members.
     */
    @Test
    void theWholeFileRendersByteEqualOnEveryDecidableType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        int whole = 0;
        for (String name : DECIDABLE) {
            assertEquals(oracle(f, name).wholeFile(), emitter.render(node(f, name)),
                    "the WHOLE POJO file on " + name + " - the package line, the resolved import block, every"
                            + " section, every compat member and the class-closing brace, byte for byte off the"
                            + " old generator's own renderPojoFile");
            whole++;
        }
        assertEquals(DECIDABLE.size(), whole,
                "the population this test actually compared, stated rather than trusted: EVERY DECIDABLE type");
        assertEquals(21, DECIDABLE.size(), "and the fixture's own population has not moved");

        // the collision pair - both directions of the D50 first-claim law, whole files
        IRPropertyModelTest.Fixture c = collisionFixture();
        IRDataTypeEmitter collide = emitter(c);
        int collided = 0;
        for (String name : List.of("ListLoser", "ListWinner")) {
            IRTypeNode n = c.index().node(COLLIDE_NAMESPACE, dataType(c.lib(), name));
            assertEquals(collideOracle(c, name).wholeFile(), collide.render(n),
                    "the WHOLE file on " + name);
            collided++;
        }
        assertEquals(2, collided);

        // the setter-shape fixture renders whole too
        IRPropertyModelTest.Fixture s = IRPropertyModelTest.settersFixture();
        IRDataTypeEmitter setters = emitter(s);
        int shapes = 0;
        for (String name : SETTER_SHAPES) {
            assertEquals(settersOracle(s, name).wholeFile(), setters.render(settersNode(s, name)),
                    "the WHOLE file on " + name);
            shapes++;
        }
        assertEquals(SETTER_SHAPES.size(), shapes);
        assertEquals(5, SETTER_SHAPES.size());
    }

    /*
     * DELETED at v3.3 seat 9, PR #645 commit 10, with its reason written down rather than dropped:
     * `theWholeFileRenderStopsAtTheCompatSubSectionForAListedType` asserted that ListToSingle stopped at
     * IMPL_COMPAT_GETTERS, ChainLeaf at BUILDER_COMPAT_GETTERS and Child at BUILDER_COMPAT_ARMS, and that the
     * NotYetRendered carried the prefix it had rendered. All three walks render now, the three Section
     * constants are gone and no site raises NotYetRendered at all, so the test could only be kept by asserting
     * a refusal this emitter no longer performs. What it proved that is still true is proved elsewhere and by
     * stronger means: the three carriers' WHOLE FILES are byte-equal to the old generator's own
     * (theWholeFileRendersByteEqualOnEveryDecidableType, which now includes them), the sections' own line-start
     * map is read by theSectionMapNamesTheFrameForEveryLineBeforeTheFirstRecordedBoundary, and the three walks'
     * own bytes are held per carrier by the compat tests below.
     */

    /**
     * RENAMED at v3.3 seat 9, PR #645 commit 10, because the commit-9 name
     * ({@code everyPositionalSectionRendersAndOnlyTheThreeCompatSubSectionsStillRefuseByName}) would now LIE:
     * the three compat sub-sections render too. NOTHING of the POJO refuses any more, on a SPECIALIZED type as
     * well as on a plain one - which is the claim this test states by rendering both.
     */
    @Test
    void everySectionAndEverySubSectionRendersOnAPlainTypeAndOnASpecializedOne() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter e = emitter(f);
        IRTypeNode leaf = node(f, "Leaf");
        IRPropertyModel leafProps = e.propertiesOf(leaf);
        assertFalse(e.implClass(leaf, leafProps).isEmpty(),
                "section 11 RENDERS since commit 8 - it no longer refuses for an unspecialized type");
        assertFalse(e.builderImplClass(leaf, leafProps).isEmpty(),
                "and section 12 RENDERS since commit 9, for the same kind of type");
        assertEquals("", e.equalsHashCodeToString(),
                "section 13's POSITION in buildBody appends NOTHING - the two boilerplates live inside sections"
                        + " 11 and 12, which is why the positional section is empty and the TEXT LAW is the"
                        + " boilerplate(...) method those sections call (the COMPAT_MEMBERS precedent)");

        // the three compat walks, on the three carriers that reached the three refusals at commit 9
        IRTypeNode listToSingle = node(f, "ListToSingle");
        IRPropertyModel listToSingleProps = e.propertiesOf(listToSingle);
        assertFalse(IRDataTypeEmitter.implExtended(listToSingle, listToSingleProps),
                "ListToSingle's Impl does NOT extend, so the impl compat walk IS reached (:1029)");
        assertTrue(e.implClass(listToSingle, listToSingleProps).contains(
                        "@" + IRDataTypeEmitter.T_ROSETTA_IGNORE),
                "and it writes the ignore-annotated compat getter rather than refusing");
        IRTypeNode chainLeaf = node(f, "ChainLeaf");
        IRPropertyModel chainLeafProps = e.propertiesOf(chainLeaf);
        assertFalse(IRDataTypeEmitter.builderExtended(chainLeaf, chainLeafProps),
                "ChainLeaf's BuilderImpl does NOT extend, so the builder compat walk IS reached (:1225) - and"
                        + " its chain is TWO rungs deep, which is the shape commit 8 and commit 9 over-refused"
                        + " for because IRParentLink did not carry a rung's own getter-override verdict");
        assertFalse(e.builderImplClass(chainLeaf, chainLeafProps).isEmpty());
        IRTypeNode child = node(f, "Child");
        IRPropertyModel childProps = e.propertiesOf(child);
        assertTrue(childProps.allProperties().stream().anyMatch(prop -> !prop.parentChain().isEmpty()),
                "Child carries a chain, which is the arm walk's ONE gate (:1266-1270)");
        assertFalse(e.builderImplClass(child, childProps).isEmpty());
    }

    // ------------------------------------------------ SECTION 11: the Impl class (PR #645 commit 8, contract 4)

    /**
     * RENAMED at v3.3 seat 9, PR #645 commit 10 (the commit-9 name ended
     * {@code …OnEveryDecidableTypeThatNeedsNoCompatGetter}, and the {@code IMPL_COMPAT_GETTER_TYPES} list it
     * excluded is GONE): section 11 is the old generator's own on EVERY type of the fixture, the four whose
     * {@code Impl} appends widening compat getters included.
     *
     * <p>The four are {@code ListToSingle} / {@code ListToSingleOther} ({@code override leaves} collapses a list
     * to a single), {@code Incompatible} ({@code override plain} changes {@code string} to {@code boolean}) and
     * {@code ChainMid} ({@code override hop} collapses {@code ChainTop}'s list) - the fixture's four
     * cardinality- or type-incompatible overrides, which are exactly the types whose {@code implExtended} is
     * false ({@code ModelObjectGenerator:1029-1033}). The test asserts that gate per type as well as the bytes,
     * so a walk that silently stopped being reached could not pass by rendering an Impl with no compat member
     * in it.
     */
    @Test
    void section11TheImplClassIsTheOldGeneratorsOwnOnEveryDecidableType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        int reachedTheWalk = 0;
        for (String name : DECIDABLE) {
            IRTypeNode n = node(f, name);
            IRPropertyModel properties = emitter.propertiesOf(n);
            assertEquals(oracle(f, name).sections().get("IMPL"), emitter.implClass(n, properties),
                    "section 11 (the whole Impl class, its compat getters and its boilerplate included) on "
                            + name);
            if (!IRDataTypeEmitter.implExtended(n, properties)
                    && properties.allProperties().stream().anyMatch(p -> p.parentChainDepth() > 0)) {
                reachedTheWalk++;
            }
        }
        assertEquals(4, reachedTheWalk,
                "the fixture's four cardinality- or type-incompatible overrides are the types whose Impl does"
                        + " not extend AND whose surface carries a chain - the two gates of :1029-1033, read off"
                        + " this emitter's own facts. A change that stopped reaching the walk would show here"
                        + " rather than quietly rendering an Impl with no compat member");
    }

    @Test
    void section11IsTheOldGeneratorsOwnOnTheCollisionPairAndOnEverySetterShape() {
        IRPropertyModelTest.Fixture c = collisionFixture();
        IRDataTypeEmitter collide = emitter(c);
        for (String name : List.of("ListLoser", "ListWinner")) {
            IRTypeNode n = c.index().node(COLLIDE_NAMESPACE, dataType(c.lib(), name));
            assertEquals(collideOracle(c, name).sections().get("IMPL"),
                    collide.implClass(n, collide.propertiesOf(n)),
                    "section 11 on " + name + " - the field and getter types carry the D50 sentinels RAW, in"
                            + " both collision directions");
        }
        IRPropertyModelTest.Fixture f = IRPropertyModelTest.settersFixture();
        IRDataTypeEmitter setters = emitter(f);
        for (String name : SETTER_SHAPES) {
            IRTypeNode n = settersNode(f, name);
            assertEquals(settersOracle(f, name).sections().get("IMPL"),
                    setters.implClass(n, setters.propertiesOf(n)),
                    "section 11 on " + name + " - the keyword-escaped FIELD identifiers (_new, _package) and the"
                            + " upper-initial one (object) in the fields, the constructor and the getters");
        }
    }

    @Test
    void section11NamesEachOfItsFourConstructorArmsByTheFactThatDecidesIt() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRTypeNode base = node(f, "Base");
        String section = emitter.implClass(base, emitter.propertiesOf(base));
        assertTrue(section.contains("this.leaves = ofNullable(builder.getLeaves()).filter(_l->!_l.isEmpty())"
                        + ".map(list -> list.stream().filter("),
                "leaves is a LIST of a model item - the four-step build arm: " + section);
        assertTrue(section.contains("this.leaf = ofNullable(builder.getLeaf()).map(f->f.build()).orElse(null);"),
                "leaf is a SINGLE model item - the build arm: " + section);
        assertTrue(section.contains("this.plain = builder.getPlain();"),
                "plain is basic - the bare assignment, no ofNullable at all: " + section);

        IRTypeNode holder = node(f, "EnumHolder");
        String enums = emitter.implClass(holder, emitter.propertiesOf(holder));
        assertTrue(enums.contains("this.colours = ofNullable(builder.getColours()).filter(_l->!_l.isEmpty()).map("),
                "an ENUM-item list takes the PLAIN list arm, not the list+model one - the item-kind fact, not"
                        + " the rendered spelling, decides it: " + enums);
        assertFalse(enums.contains("map(f->f.build())"),
                "and nothing in EnumHolder's constructor calls build() on an element: " + enums);
    }

    @Test
    void section11WritesTheGetterAnnotationStackAndTheExtensionFactByTheirOwnFacts() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);

        IRTypeNode flags = node(f, "Flags");
        String flagsImpl = emitter.implClass(flags, emitter.propertiesOf(flags));
        assertTrue(flagsImpl.contains("\t\t@" + IRDataTypeEmitter.T_RUNE_SCOPED_REF + "\n\t\t@"
                        + IRDataTypeEmitter.T_RUNE_SCOPED_KEY + "\n\t\tpublic "),
                "`both` carries [metadata address] then [metadata location], in that order, immediately above its"
                        + " getter: " + flagsImpl);
        assertTrue(flagsImpl.contains("\t\t@" + IRDataTypeEmitter.T_RUNE_ATTRIBUTE + "(\"ided\")\n\t\tpublic "),
                "`ided` carries [metadata id], which is an AttributeMeta flag in process and NO annotation in the"
                        + " Impl getter stack at all: " + flagsImpl);

        IRTypeNode keyed = node(f, "Keyed");
        assertTrue(emitter.implClass(keyed, emitter.propertiesOf(keyed))
                        .contains("\t\t@" + IRDataTypeEmitter.T_RUNE_META_TYPE + "\n"),
                "the synthetic MetaFields property is the ONE carrier of @RuneMetaType");

        IRTypeNode child = node(f, "Child");
        IRPropertyModel childProps = emitter.propertiesOf(child);
        assertTrue(IRDataTypeEmitter.implExtended(child, childProps),
                "every one of Child's own overrides is subtype-compatible, so its Impl EXTENDS");
        String childImpl = emitter.implClass(child, childProps);
        assertTrue(childImpl.contains("class ChildImpl extends Base.BaseImpl implements Child {"), childImpl);
        assertTrue(childImpl.contains("\t\t\tsuper(builder);\n"), childImpl);
        assertTrue(childImpl.contains("\t\t\tsuper.setBuilderFields(builder);\n"), childImpl);
        assertFalse(childImpl.contains("private final " + IRDataTypeEmitter.T_LIST),
                "and it declares its OWN properties only - Base's list-shaped `leaves` is the parent Impl's"
                        + " field, not a second copy: " + childImpl);
    }

    // ------------------------------------------ SECTION 12: the BuilderImpl class (PR #645 commit 9, contract 3)

    /**
     * RENAMED at v3.3 seat 9, PR #645 commit 10 (the commit-9 name ended
     * {@code …OnEveryTypeThatNeedsNoCompatMember}, and the two lists it excluded -
     * {@code BUILDER_COMPAT_GETTER_TYPES}, {@code BUILDER_COMPAT_ARM_TYPES} - are GONE): section 12 is the old
     * generator's own on EVERY type of the fixture, the compat getters, their {@code getOrCreate} delegates and
     * the ancestor setter arms included.
     *
     * <p><b>THE TWO WALKS HAVE TWO DIFFERENT GATES</b> and this test counts both, so a change that stopped
     * reaching either would show here rather than pass by rendering a BuilderImpl with nothing in it:
     * <ul>
     *   <li>the GETTER walk ({@code :1225-1227}) runs when the {@code BuilderImpl} does NOT extend - which needs
     *       every own property IDENTICALLY typed with its parent's, a STRICTER test than the {@code Impl}'s
     *       subtype-compatibility - and some property carries a chain. On this fixture that is ALL SIX
     *       specialized types, two more than the impl walk's four: {@code ChainLeaf} (overrides {@code hop} as
     *       {@code Sub} where its parent has {@code Leaf}) and {@code Child} (overrides {@code leaf} the same
     *       way) are subtype-compatible, so their {@code Impl}s extend and section 11 renders them whole, while
     *       their builders cannot extend;</li>
     *   <li>the ARM walk ({@code :1266-1270}) is gated on NOTHING but a non-empty chain - not on the builder's
     *       extension, not on the override verdict - so the same six reach it.</li>
     * </ul>
     *
     * <p><b>REACHING A WALK IS NOT WRITING A MEMBER</b>, and the two are counted apart on purpose: {@code Child}
     * reaches the getter walk and writes nothing there, because every one of its rungs' cursors overrides its
     * parent's getter. That is exactly the distinction commit 9's enumerated lists blurred - they listed
     * {@code Child} under the ARM refusal because the getter REFUSAL let it through, not because the getter
     * WALK was never reached.</p>
     */
    @Test
    void section12TheBuilderImplIsTheOldGeneratorsOwnOnEveryDecidableType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        int reachedTheGetterWalk = 0;
        int reachedTheArmWalk = 0;
        for (String name : DECIDABLE) {
            IRTypeNode n = node(f, name);
            IRPropertyModel properties = emitter.propertiesOf(n);
            assertEquals(oracle(f, name).sections().get("BUILDER_IMPL"),
                    emitter.builderImplClass(n, properties),
                    "section 12 (the whole BuilderImpl - its compat getters, its getOrCreate delegates, its"
                            + " ancestor setter arms and its boilerplate, WITHOUT the class-closing brace) on "
                            + name);
            boolean anyChain = properties.allProperties().stream().anyMatch(p -> !p.parentChain().isEmpty());
            if (!IRDataTypeEmitter.builderExtended(n, properties) && anyChain) {
                reachedTheGetterWalk++;
            }
            if (anyChain) {
                reachedTheArmWalk++;
            }
        }
        assertEquals(6, reachedTheGetterWalk,
                "ALL SIX specialized types - ListToSingle, ListToSingleOther, Incompatible, ChainMid, ChainLeaf"
                        + " and Child: the builder's extension test is the IDENTICAL-TYPE one, which the two"
                        + " covariant overrides fail although their Impls extend");
        assertEquals(6, reachedTheArmWalk,
                "and the arm walk's ONE gate is a non-empty chain, which the same six carry - the widest gate"
                        + " of the three");

        // reaching a walk is not writing a member: Child's every rung cursor overrides, so its getter walk
        // appends nothing, while its ARM walk appends for every rung (the two gates, measured apart)
        IRTypeNode child = node(f, "Child");
        IRPropertyModel childProps = emitter.propertiesOf(child);
        assertFalse(IRDataTypeEmitter.builderExtended(child, childProps));
        assertTrue(childProps.allProperties().stream()
                        .filter(p -> !p.parentChain().isEmpty())
                        .allMatch(IRPropertyModel.IRProperty::getterOverridesParentGetter),
                "every specialized property of Child keeps its parent's getter name, which is why the getter"
                        + " walk writes nothing for it and the arm walk still writes for every rung");
    }

    /**
     * THE DEPTH-TWO CHAIN, RUNG BY RUNG (v3.3 seat 9, PR #645 commit 10) - the case commits 8 and 9 refused for
     * by name, and the one the rung's own {@code getterOverridesParentGetter} fact decides.
     *
     * <p>{@code ChainLeaf.hop} is {@code Sub (0..1)} over {@code ChainMid.hop} {@code Leaf (0..1)} over
     * {@code ChainTop.hop} {@code List<Leaf>}. The walk's cursor is the one {@code PojoCompatEmitter:172-188}
     * holds - it starts AT the property and climbs - so the flag that decides the NEAREST rung is the MAIN
     * property's, and the flag that decides the rung ABOVE it is the nearest rung's own:
     * <ul>
     *   <li>{@code ChainLeaf.hop} keeps {@code ChainMid}'s compatibility name
     *       ({@code hopOverriddenAsSingle}), so NO member is written for the nearest rung;</li>
     *   <li>{@code ChainMid.hop} does NOT keep {@code ChainTop}'s name ({@code hopOverriddenAsSingle} against
     *       {@code hop}), so a member IS written for the TOP rung - in that rung's own list shape and under
     *       that rung's own name, {@code getHop()}.</li>
     * </ul>
     * Neither of those two facts is carried by the specialized property: both are the RUNGS' own, which is why
     * this shape could not be decided before commit 10 and was honestly refused instead.
     */
    @Test
    void section12WritesTheDepthTwoChainsTopRungAndSkipsItsNearestOne() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRTypeNode chainLeaf = node(f, "ChainLeaf");
        IRPropertyModel properties = emitter.propertiesOf(chainLeaf);
        IRPropertyModel.IRProperty hop = property(properties, "hop");
        List<IRPropertyModel.IRParentLink> chain = hop.parentChain();
        assertEquals(2, chain.size(), "the subject IS a depth-2 chain - the test's own subject must exist");
        assertTrue(hop.getterOverridesParentGetter(),
                "the MAIN property's getter keeps ChainMid's name, so the NEAREST rung appends nothing (:175)");
        assertFalse(chain.get(0).getterOverridesParentGetter(),
                "and ChainMid.hop's OWN getter does not keep ChainTop's - the rung fact commit 10 added, and"
                        + " the reason the TOP rung's member is written");
        assertEquals("hopOverriddenAsSingle", chain.get(0).getterCompatibilityName());
        assertEquals("hop", chain.get(1).getterCompatibilityName(),
                "the name that member is called by is the TOP RUNG's own");

        String section = emitter.builderImplClass(chainLeaf, properties);
        assertTrue(section.contains(" getHop() {"),
                "the compat getter is written in the TOP rung's name and shape: " + section);
        assertEquals(oracle(f, "ChainLeaf").sections().get("BUILDER_IMPL"), section,
                "and the whole section is the old generator's own, byte for byte - which is what proves the"
                        + " skip and the write are both in the right place");
    }

    @Test
    void section12IsTheOldGeneratorsOwnOnTheCollisionPairAndOnEverySetterShape() {
        IRPropertyModelTest.Fixture c = collisionFixture();
        IRDataTypeEmitter collide = emitter(c);
        for (String name : List.of("ListLoser", "ListWinner")) {
            IRTypeNode n = c.index().node(COLLIDE_NAMESPACE, dataType(c.lib(), name));
            assertEquals(collideOracle(c, name).sections().get("BUILDER_IMPL"),
                    collide.builderImplClass(n, collide.propertiesOf(n)),
                    "section 12 on " + name + " - the builder field, getter, getOrCreate and setter types carry"
                            + " the D50 sentinels RAW, in both collision directions");
        }
        IRPropertyModelTest.Fixture f = IRPropertyModelTest.settersFixture();
        IRDataTypeEmitter setters = emitter(f);
        for (String name : SETTER_SHAPES) {
            IRTypeNode n = settersNode(f, name);
            assertEquals(settersOracle(f, name).sections().get("BUILDER_IMPL"),
                    setters.builderImplClass(n, setters.propertiesOf(n)),
                    "section 12 on " + name + " - the whole setter family of every shape, the keyword-escaped"
                            + " field identifiers and both parameter-escape laws");
        }
    }

    /**
     * THE TWO CLASS-SCOPE ESCAPES, each on the fixture type that was built for it (v3.3 seat 9, PR #645
     * commit 9). Both are the SAME upstream law - a method-local or parameter whose desired name the class scope
     * already claims takes a leading underscore - and the fixture states both directions: {@code Indexed} and
     * {@code Resulting} claim the names, while {@code SLeaf} and {@code Setters} do not and write them bare.
     *
     * <p>Note what does NOT escape: the builder INTERFACE's {@code getOrCreate*(int index)} declaration
     * ({@code ModelObjectGenerator:797-798}) writes {@code index} unconditionally - the escape is the
     * {@code BuilderImpl}'s alone. Asserted here so the asymmetry is a witnessed fact rather than an oversight.
     */
    @Test
    void section12EscapesTheIndexParameterAndTheResultLocalOnlyWhereTheClassScopeClaimsTheName() {
        IRPropertyModelTest.Fixture f = IRPropertyModelTest.settersFixture();
        IRDataTypeEmitter emitter = emitter(f);

        IRTypeNode indexed = settersNode(f, "Indexed");
        String indexedImpl = emitter.builderImplClass(indexed, emitter.propertiesOf(indexed));
        assertTrue(indexedImpl.contains("getOrCreateItems(int _index) {"),
                "an attribute literally named `index` is in the class scope, so the getOrCreate parameter"
                        + " escapes: " + indexedImpl);
        assertTrue(indexedImpl.contains("return getIndex(items, _index, () -> {"),
                "and the escaped name is used at the call too, not only in the signature: " + indexedImpl);
        assertFalse(emitter.builderInterface(indexed, emitter.propertiesOf(indexed)).contains("int _index"),
                "but the builder INTERFACE declaration writes `int index` bare - the escape is the impl's alone");

        IRTypeNode resulting = settersNode(f, "Resulting");
        String resultingImpl = emitter.builderImplClass(resulting, emitter.propertiesOf(resulting));
        assertTrue(resultingImpl.contains("SLeaf.SLeafBuilder _result;"),
                "an attribute named `result` is in the class scope, so the getOrCreate local escapes: "
                        + resultingImpl);

        IRTypeNode setters = settersNode(f, "Setters");
        String settersImpl = emitter.builderImplClass(setters, emitter.propertiesOf(setters));
        assertTrue(settersImpl.contains("getOrCreatePlainLeaves(int index) {"),
                "a class scope that claims NEITHER name writes both bare - the other direction: " + settersImpl);
        assertTrue(settersImpl.contains("SLeaf.SLeafBuilder result;"), settersImpl);
    }

    /**
     * THE SCOPED-KEY {@code addKey} ARM, in BOTH its shapes, and they are NOT the same text
     * ({@code ModelObjectGenerator:1177-1179} vs {@code :1199-1201}): the LIST form calls
     * {@code getOrCreateMeta().addKey(...)} on the freshly built element, the SINGLE form calls
     * {@code getOrCreateMeta().toBuilder().addKey(...)} on the local. {@code Located} carries a
     * {@code [metadata location]} attribute of each cardinality; the {@code seat8.props} fixture's
     * {@code Flags.both} is the SINGLE carrier there and nothing reached the LIST arm before this commit.
     */
    @Test
    void section12WritesTheScopedKeyArmInBothShapesAndOnlyWhereMetadataLocationIsDeclared() {
        IRPropertyModelTest.Fixture f = IRPropertyModelTest.settersFixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRTypeNode located = settersNode(f, "Located");
        String impl = emitter.builderImplClass(located, emitter.propertiesOf(located));
        assertTrue(impl.contains("newAnchors.getOrCreateMeta().addKey(" + IRDataTypeEmitter.T_KEY
                        + ".builder().setScope(\"DOCUMENT\"));"),
                "the LIST arm keys the fresh element directly: " + impl);
        assertTrue(impl.contains("result.getOrCreateMeta().toBuilder().addKey(" + IRDataTypeEmitter.T_KEY
                        + ".builder().setScope(\"DOCUMENT\"));"),
                "the SINGLE arm keys through toBuilder() on the local - a different line, copied not unified: "
                        + impl);

        IRPropertyModelTest.Fixture plain = fixture();
        IRDataTypeEmitter plainEmitter = emitter(plain);
        IRTypeNode flags = node(plain, "Flags");
        assertTrue(plainEmitter.builderImplClass(flags, plainEmitter.propertiesOf(flags))
                        .contains(".getOrCreateMeta().toBuilder().addKey("),
                "Flags.both ([metadata address] [metadata location] on Leaf (0..1)) is the seat8.props"
                        + " fixture's scoped-key carrier, and it is the SINGLE shape");
        IRTypeNode base = node(plain, "Base");
        assertFalse(plainEmitter.builderImplClass(base, plainEmitter.propertiesOf(base)).contains("addKey("),
                "and a model-object property with no [metadata location] writes no addKey line at all");
    }

    /**
     * THE PLURAL-PARAMETER SIBLING ESCAPE, and the law beside it that does NOT escape
     * ({@code ModelObjectGenerator:1499-1502} vs {@code :1645-1646}). {@code Setters.codes} pluralises to
     * {@code codess}, which is a SIBLING attribute's field identifier, so the {@code add(List)} / {@code set(List)}
     * parameter escapes to {@code _codess} - while the BULK meta-value setters of the very same property write
     * the raw name plus {@code "s"} with no escape at all. Two laws, one class, opposite answers.
     */
    @Test
    void section12EscapesThePluralListParameterAgainstASiblingFieldAndLeavesTheBulkMetaParameterBare() {
        IRPropertyModelTest.Fixture f = IRPropertyModelTest.settersFixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRTypeNode setters = settersNode(f, "Setters");
        String impl = emitter.builderImplClass(setters, emitter.propertiesOf(setters));
        // the parameter names are read off the BODIES, which spell them without any type beside them - the type
        // positions carry D50 sentinels and are held byte-equal by the section test above, not re-spelt here
        assertTrue(impl.contains("\t\t\tif (_codess != null) {\n"),
                "`codess` names a sibling FIELD, so the add(List) / set(List) parameter escapes: " + impl);
        assertTrue(impl.contains("\t\t\tif (_codess == null) {\n"),
                "and the set(List) null guard reads the escaped name too - while the line under it assigns"
                        + " this.codes, the FIELD, which is a different identifier again: " + impl);
        assertTrue(impl.contains("\t\t\t\tthis.codes = new "),
                "the set(List) null arm assigns the FIELD, not the parameter: " + impl);
        assertTrue(impl.contains("\t\t\tif (codess != null) {\n"),
                "while the BULK meta-value setter of the SAME property writes the plural BARE: " + impl);
        assertTrue(impl.contains("\t\t\tthis.codes.clear();\n"),
                "and its setValue arm clears through the RAW name, not the field identifier: " + impl);
        assertTrue(impl.contains("\t\t\tif (plainLeavess != null) {\n"),
                "a plural that names no sibling stays bare - the other direction (and note the plural is the RAW"
                        + " name + \"s\", so an attribute already ending in `s` pluralises to `plainLeavess`; it"
                        + " is the old generator's own law, copied): " + impl);
    }

    /**
     * THE META-VALUE SETTERS, single and bulk, and the {@code .toBuilder()} suffix the RECONCILED
     * {@code metaValueIsRosettaModelObject} fact decides ({@code ModelObjectGenerator:1608}).
     * {@code Setters.codes} wraps a BASIC value ({@code String}) so its {@code addCodesValue} passes the value
     * straight through; {@code Setters.refs} wraps a MODEL one ({@code SLeaf}) so its {@code addRefsValue} calls
     * {@code .toBuilder()}. The SINGLE-cardinality meta setter never calls it at all ({@code :1692-1694}), which
     * {@code Setters.schemed} witnesses - three arms, three facts, none of them a guess from a rendered name.
     */
    @Test
    void section12DecidesTheMetaValueToBuilderSuffixByTheReconciledValueKindAndNeverOnTheSingleArm() {
        IRPropertyModelTest.Fixture f = IRPropertyModelTest.settersFixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRTypeNode setters = settersNode(f, "Setters");
        String impl = emitter.builderImplClass(setters, emitter.propertiesOf(setters));
        assertTrue(impl.contains("this.getOrCreateCodes(-1).setValue(_codes);"),
                "a meta wrap around a BASIC value passes it straight through: " + impl);
        assertTrue(impl.contains("this.getOrCreateRefs(-1).setValue(_refs.toBuilder());"),
                "and a meta wrap around a MODEL value takes .toBuilder(): " + impl);
        assertTrue(impl.contains("this.getOrCreateSchemed().setValue(_schemed);"),
                "while the SINGLE-cardinality meta setter never calls toBuilder, whatever the value kind: "
                        + impl);
    }

    /**
     * THE MODEL-OBJECT PROPERTY of {@code seat8.props} that carries a meta wrap around a BASIC value -
     * {@code MetaCarrier.many} ({@code Leaf (0..*) [metadata scheme]}, whose items are the generated
     * {@code FieldWithMetaLeaf} wrapper and whose VALUE is {@code Leaf}) and {@code MetaCarrier.scheme}
     * ({@code string (0..1) [metadata scheme]}, a wrapper around a basic). The point of the test is the
     * {@code hasData()} arm each of them produces, which is the byte the meta-value-kind fact decides in section
     * 12 ({@code :1346-1347}, {@code :1362-1363}) - and the whole section is already held byte-equal above, so
     * this states WHICH line is which rather than re-proving equality.
     */
    @Test
    void section12WritesThePresenceOnlyHasDataArmExactlyWhereTheMetaWrapHidesABasicValue() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRTypeNode carrier = node(f, "MetaCarrier");
        String impl = emitter.builderImplClass(carrier, emitter.propertiesOf(carrier));
        assertTrue(impl.contains("if (getScheme()!=null) return true;"),
                "`scheme` wraps a basic value, so its hasData arm is presence-only: " + impl);
        assertTrue(impl.contains("if (getRefd()!=null && getRefd().hasData()) return true;"),
                "`refd` wraps a MODEL value, so its arm asks hasData(): " + impl);
        assertTrue(impl.contains("if (getMany()!=null && getMany().stream().filter("
                        + IRDataTypeEmitter.T_OBJECTS + "::nonNull).anyMatch(a->a.hasData())) return true;"),
                "and `many` is a LIST whose meta wrap holds a MODEL value, so it takes the stream arm: " + impl);
    }

    /**
     * THE PRUNING CONFIG MOVES BYTES, and it is the only thing that does: the SAME {@code seat8.props}
     * declarations, the SAME {@code Base} type, rendered under TWO generator models that differ in nothing but
     * their {@code doNotPrune} set - and the IR emitter's section 12 is byte-equal to the OLD GENERATOR'S OWN
     * section 12 under BOTH ({@code ModelObjectGenerator:1315-1316} and {@code :1359-1360} ask the same two
     * strings of the same set).
     *
     * <p>The two listed keys are {@code Base#leaf} (single, model) and {@code Base#leaves} (list, model), which
     * are exactly the two properties {@code prune()} and {@code hasData()} branch on. The test also states WHICH
     * lines move, in both directions, so a config that were silently ignored could not pass by rendering the
     * default bytes twice.
     */
    @Test
    void section12TakesTheKeepFormsForEveryPairTheGeneratorModelsPruningConfigLists() {
        Set<String> doNotPrune = Set.of("seat8.props.Base#leaf", "seat8.props.Base#leaves");
        IRPropertyModelTest.Fixture p = IRPropertyModelTest.pruningFixture(doNotPrune);
        IRDataTypeEmitter emitter = new IRDataTypeEmitter(p.index(),
                new IRDataTypeEmitter.Config(VERSION, doNotPrune), new JavaTypeUtil());
        IRTypeNode base = p.index().node(NAMESPACE, dataType(p.lib(), "Base"));
        String section = emitter.builderImplClass(base, emitter.propertiesOf(base));

        assertEquals(PojoSectionOracle.of(dataType(p.lib(), "Base"), p.gm(), VERSION).sections().get("BUILDER_IMPL"),
                section,
                "section 12 on Base under the pruning config - byte-equal to the old generator's own under the"
                        + " SAME config");

        assertTrue(section.contains("if (leaf!=null) leaf.prune();"),
                "the listed SINGLE pair renders the KEEP form: " + section);
        assertFalse(section.contains(".filter(b->b.hasData())"),
                "and the listed LIST pair drops the hasData filter from its prune stream: " + section);
        assertTrue(section.contains("if (getLeaf()!=null) return true;"),
                "the listed single pair's hasData arm is presence-only too: " + section);

        // the other direction, on the very same declarations under an EMPTY set
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter plain = emitter(f);
        IRTypeNode plainBase = node(f, "Base");
        String defaultSection = plain.builderImplClass(plainBase, plain.propertiesOf(plainBase));
        assertEquals(oracle(f, "Base").sections().get("BUILDER_IMPL"), defaultSection,
                "and byte-equal to the old generator's own under the EMPTY config");
        assertTrue(defaultSection.contains("if (leaf!=null && !leaf.prune().hasData()) leaf = null;"),
                "which writes the PRUNING form for the same property: " + defaultSection);
        assertTrue(defaultSection.contains(".filter(b->b.hasData())"), defaultSection);
        assertFalse(defaultSection.equals(section),
                "the two renders DIFFER - which is what proves the emitter reads the config rather than a"
                        + " property of the type");
    }

    // ------------------------ THE HOLD-OUT POJO BATTERIES: the compat algebra's own witnesses (commit 10)

    /**
     * THE FIVE HOLD-OUT POJO MODELS, EVERY TYPE, WHOLE FILE, AGAINST THE OLD GENERATOR'S OWN (v3.3 seat 9,
     * PR #645 commit 10). {@code holdout-goldens/pojo-inheritance}, {@code pojo-number-ladder},
     * {@code pojo-bulk-meta-drop}, {@code pojo-bulk-meta-kind} and {@code pojo-bulk-value-narrow} are the
     * batteries {@code PojoCompatEmitter}'s class javadoc ({@code :88-118}) names as the compat arms' own
     * oracle witnesses, and they carry between them every arm the {@code seat8.props} fixture does not reach:
     * <ul>
     *   <li>the bulk {@code new ArrayList(...)} wildcard-to-invariant copy - the #412 recorded corner, witnessed
     *       by {@code pojo-bulk-meta-kind}'s same-value meta-KIND change;</li>
     *   <li>the number ladder's guarded narrowings - {@code pojo-inheritance}'s {@code Foo2} / {@code Foo3}
     *       ({@code number} to {@code int(digits: 30)} to {@code int}) and
     *       {@code pojo-bulk-value-narrow}'s int-under-number bulk stream form;</li>
     *   <li>the meta wrap / unwrap and the model downcast - {@code Foo2.parent} ({@code Parent} to {@code Child})
     *       and {@code Foo2.parentList} ({@code List<Parent>} to a meta-wrapped single {@code Child});</li>
     *   <li>the {@code Collections.singletonList} / {@code Collections.emptyList} / {@code MapperC.of(x).get()}
     *       cardinality arms, which that same list-to-single override is the carrier of;</li>
     *   <li>the meta-DROPPING override that does NOT specialize at all ({@code pojo-bulk-meta-drop}), which is
     *       the NEGATIVE witness: a battery whose child emits the parent's shape with covariant returns and NO
     *       compat member.</li>
     * </ul>
     *
     * <p><b>THE ORACLE IS {@code PojoSectionOracle.wholeFile()}, not the golden FILES</b>, and the sources are
     * in the tree for every one of the five ({@code rune-java-generator/src/test/resources/holdout/pojo-*}), so
     * nothing here is held against a file this tree did not produce: {@code wholeFile()} is literally what
     * {@code ModelObjectGenerator.generate} returns for the same parsed model on the same tree, which is the
     * only oracle that makes this a two-producer comparison. The goldens remain the RELEASED plugin's bar and
     * are held by {@code HoldOutByteCompareTest} in the sibling module, where they belong.
     */
    @Test
    void theHoldOutPojoFixturesRenderByteEqualToTheOldGeneratorOnEveryType() {
        Map<String, List<String>> expected = new LinkedHashMap<>();
        expected.put("pojo-inheritance",
                List.of("Foo1", "Foo2", "Foo3", "Parent", "Child", "GrandChild", "Level1", "Level2"));
        expected.put("pojo-number-ladder", List.of("A"));
        expected.put("pojo-bulk-meta-drop", List.of("Parent", "Child"));
        expected.put("pojo-bulk-meta-kind", List.of("Parent", "Child"));
        expected.put("pojo-bulk-value-narrow", List.of("Parent", "Child"));
        assertEquals(IRPropertyModelTest.HOLDOUT_POJO_GROUPS, List.copyOf(expected.keySet()),
                "the groups this test compares ARE the enumerated population (Rule 4)");

        int files = 0;
        for (Map.Entry<String, List<String>> group : expected.entrySet()) {
            IRPropertyModelTest.Fixture f = IRPropertyModelTest.holdOutFixture(group.getKey());
            assertEquals(group.getValue(), IRPropertyModelTest.holdOutTypes(f),
                    group.getKey() + ": the types it declares, enumerated rather than derived - a battery that"
                            + " grew a type would say so here rather than leave it uncompared");
            IRDataTypeEmitter emitter = emitter(f);
            String namespace = f.lib().namespace();
            for (String name : group.getValue()) {
                RDataType type = dataType(f.lib(), name);
                IRTypeNode n = f.index().node(namespace, type);
                assertEquals(PojoSectionOracle.of(type, f.gm(), VERSION).wholeFile(), emitter.render(n),
                        "the WHOLE POJO file on " + group.getKey() + "/" + name
                                + " - every compat member of it included, byte for byte off the old generator's"
                                + " own renderPojoFile");
                files++;
            }
        }
        assertEquals(15, files,
                "the population this test actually compared, stated rather than trusted: 8 + 1 + 2 + 2 + 2");
    }

    /**
     * THE ARMS THE HOLD-OUT BATTERIES EXIST FOR, NAMED IN THE BYTES (v3.3 seat 9, PR #645 commit 10). The test
     * above proves the whole file; this one says WHICH lines the compat algebra contributed, so that a change
     * which silently stopped emitting an arm could not pass by matching an oracle that stopped with it - the
     * oracle would have to be wrong in exactly the same way, and these are the shapes upstream's own goldens
     * record.
     */
    @Test
    void theHoldOutCompatArmsCarryTheNumberLadderTheMetaWrapAndTheCardinalityWitnesses() {
        IRPropertyModelTest.Fixture f = IRPropertyModelTest.holdOutFixture("pojo-inheritance");
        IRDataTypeEmitter emitter = emitter(f);
        RDataType foo3Type = dataType(f.lib(), "Foo3");
        String foo3 = emitter.render(f.index().node("test.pojo", foo3Type));

        assertTrue(foo3.contains("BigInteger.valueOf(numberAttr)"),
                "the widening rung of the number ladder, plain: " + foo3);
        assertTrue(foo3.contains(".intValue()).equals("),
                "and the NARROWING rung's round-trip guard, which is the arm lane U22 drops: " + foo3);
        assertTrue(foo3.contains("Collections.singletonList("),
                "the single-to-list cardinality arm - Foo1.parentList is a list and Foo3's override is not");
        assertTrue(foo3.contains("MapperC.of("),
                "and the list-to-single unwrap head in the setter direction");
        assertTrue(foo3.contains("@RosettaIgnore") && foo3.contains("@RuneIgnore"),
                "every compat member carries the ignore pair");

        IRPropertyModelTest.Fixture kind = IRPropertyModelTest.holdOutFixture("pojo-bulk-meta-kind");
        RDataType kindChildType = dataType(kind.lib(), "Child");
        String kindChild = emitter(kind).render(kind.index().node("test.pojo", kindChildType));
        assertTrue(kindChild.contains("new ArrayList("),
                "THE #412 RECORDED CORNER: the bulk arm's wildcard-to-invariant RAW ArrayList copy, which the"
                        + " released plugin's own golden records and which only a meta-KIND change reaches: "
                        + kindChild);

        IRPropertyModelTest.Fixture drop = IRPropertyModelTest.holdOutFixture("pojo-bulk-meta-drop");
        RDataType dropChildType = dataType(drop.lib(), "Child");
        String dropChild = emitter(drop).render(drop.index().node("test.pojo", dropChildType));
        assertFalse(dropChild.contains("@RosettaIgnore"),
                "THE NEGATIVE WITNESS: a meta-DROPPING override does not specialize at all, so its POJO carries"
                        + " no compat member - and the emitter must not invent one: " + dropChild);
    }

    // ---------------------------------------------- SECTION 13: the boilerplate TEXT LAW (commit 8, contract 3)

    /**
     * THE SEAM HOLDS THE <b>BUILDER</b> VARIANT, and the test says why: {@code buildBody} calls
     * {@code boilerPlate} from {@code generateImplClass} and {@code builderBoilerPlate} from
     * {@code generateBuilderImplClass} AFTER it, so {@code ModelObjectBoilerplate.lastBoilerPlate()} - the text
     * this oracle reads - is the builder's. The IMPL variant is held by section 11's own whole, byte for byte,
     * in the test above.
     */
    @Test
    void section13TheBoilerplateIsTheOldGeneratorsOwnOnEveryDecidableType() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        for (String name : DECIDABLE) {
            IRTypeNode n = node(f, name);
            IRPropertyModel properties = emitter.propertiesOf(n);
            boolean builderExtended = IRDataTypeEmitter.builderExtended(n, properties);
            List<IRPropertyModel.IRProperty> builderProps = builderExtended
                    ? properties.ownProperties() : properties.allProperties();
            assertEquals(oracle(f, name).boilerplate(),
                    emitter.boilerplate(name, name + "Builder", builderExtended, builderProps),
                    "section 13 (equals / hashCode / toString) on " + name + "Builder");
        }
    }

    @Test
    void section13HashesAnEnumItemByItsClassNameAndEveryOtherItemByItsOwnHashCode() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRPropertyModel holder = emitter.propertiesOf(node(f, "EnumHolder"));
        String text = emitter.boilerplate("EnumHolder", "EnumHolder", false, holder.allProperties());
        assertTrue(text.contains("_result = 31 * _result + (colours != null ? colours.stream()"
                        + ".map(Object::getClass).map(Class::getName).mapToInt(String::hashCode).sum() : 0);"),
                "the LIST arm of the enum branch: " + text);
        assertTrue(text.contains("_result = 31 * _result + (tone != null ? tone.getClass().getName()"
                        + ".hashCode() : 0);"),
                "and the SINGLE arm: " + text);

        IRPropertyModel base = emitter.propertiesOf(node(f, "Base"));
        String plain = emitter.boilerplate("Base", "Base", false, base.allProperties());
        assertTrue(plain.contains("_result = 31 * _result + (plain != null ? plain.hashCode() : 0);"),
                "a non-enum item hashes itself - the RECONCILED itemIsEnum fact is what tells the two apart,"
                        + " never the rendered type name: " + plain);
    }

    // ------------------------------------------------- the two facts commit 4 refused for, now READ and RENDERED

    @Test
    void theItemKindFactDecidesTheGetterArmForAnEnumItemListAndForADataTypeItemList() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);

        IRPropertyModel holder = emitter.propertiesOf(node(f, "EnumHolder"));
        IRPropertyModel.IRProperty colours = property(holder, "colours");
        assertFalse(emitter.itemIsModelObject(colours), "an ENUM item is no RosettaModelObject");
        assertEquals(IRDataTypeEmitter.T_LIST + "<" + IRDataTypeEmitter.valueSiteTypeRef("seat8.props.Colour") + ">",
                emitter.interfaceGetterType(colours),
                "so the getter returns a PLAIN List - interfaceGetterType:1917-1925 takes its `? extends` arm on"
                        + " the model-object item alone (the List token is the D50 first-claim SENTINEL both halves"
                        + " write, never the bare word)");

        IRPropertyModel base = emitter.propertiesOf(node(f, "Base"));
        IRPropertyModel.IRProperty leaves = property(base, "leaves");
        assertTrue(emitter.itemIsModelObject(leaves), "a `type` item IS one - the case commit 4 REFUSED by name");
        assertEquals(IRDataTypeEmitter.T_LIST + "<? extends "
                        + IRDataTypeEmitter.valueSiteTypeRef("seat8.props.Leaf") + ">",
                emitter.interfaceGetterType(leaves));

        IRPropertyModel carrier = emitter.propertiesOf(node(f, "MetaCarrier"));
        assertTrue(emitter.itemIsModelObject(property(carrier, "many")),
                "a meta-wrapped item is the GENERATED wrapper class");
        assertFalse(emitter.itemIsModelObject(property(emitter.propertiesOf(node(f, "Leaf")), "id")),
                "a java.lang item is not one");
        assertTrue(emitter.itemIsModelObject(property(emitter.propertiesOf(node(f, "Keyed")), "meta")),
                "and MetaFields is");
    }

    @Test
    void section6TheEnumItemListGettersAreTheOldGeneratorsOwn() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRTypeNode holder = node(f, "EnumHolder");
        assertEquals(oracle(f, "EnumHolder").sections().get("GETTERS"),
                emitter.getters(emitter.propertiesOf(holder)),
                "section 6 on the ENUM-item list type: the one getter whose return type the item-kind fact decides"
                        + " against the old generator's own bytes");
    }

    @Test
    void section14StaysEmptyOnTheDepthTwoChainAndItsImportSetIsTheOldGeneratorsOwn() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = emitter(f);
        IRTypeNode chainLeaf = node(f, "ChainLeaf");
        IRPropertyModel properties = emitter.propertiesOf(chainLeaf);

        assertEquals(2, property(properties, "hop").parentChainDepth(),
                "the subject IS a depth-2 chain, with a cardinality change in it - the lane's own subject must exist");
        assertEquals("", emitter.compatMembers(properties),
                "section 14 is where ModelObjectGenerator:621-623 only BUILDS the PojoCompatEmitter; every arm it"
                        + " writes is appended later, by sections 11 and 12, so the old generator's own bytes at"
                        + " THIS position are empty for a specialized type too - and the emitter no longer refuses");

        PojoSectionOracle.Sections expected = oracle(f, "ChainLeaf");
        assertEquals(expected.imports(), emitter.imports(chainLeaf, properties),
                "THE CHAIN'S BYTE-BEARING CONSUMER AT THIS COMMIT (ModelObjectGenerator:288-330): each rung's own"
                        + " type imports, the RosettaIgnore / RuneIgnore pair, Collections + MapperC for the"
                        + " cardinality change and the bare java.util.List for the list-shaped rung");
        assertEquals(expected.staticImports(), emitter.staticImports(chainLeaf, properties));
    }

    // ------------------------------------------------------------------ THE SECTION MAP (PR #645 commit 6, PART 2)

    @Test
    void theSectionMapNamesTheFrameForEveryLineBeforeTheFirstRecordedBoundary() {
        // a two-boundary table, hand-written so the law is read off the map alone and not off a rendered file
        Map<String, Integer> boundaries = new LinkedHashMap<>();
        boundaries.put("ANNOTATIONS", 10);
        boundaries.put("GETTERS", 20);
        IRDataTypeEmitter.Rendered rendered = new IRDataTypeEmitter.Rendered("", boundaries);
        for (int line = 1; line <= 9; line++) {
            assertEquals("HEADER", rendered.sectionOfLine(line),
                    "a line BEFORE the first recorded boundary is the frame - section 1 - never UNKNOWN: " + line);
        }
        for (int line = 10; line <= 19; line++) {
            assertEquals("ANNOTATIONS", rendered.sectionOfLine(line), "line " + line);
        }
        for (int line = 20; line <= 25; line++) {
            assertEquals("GETTERS", rendered.sectionOfLine(line), "line " + line);
        }
        assertEquals("UNKNOWN", new IRDataTypeEmitter.Rendered("", Map.of()).sectionOfLine(1),
                "a map that names NO boundary states nothing, and a guess would be worse than UNKNOWN");
    }

    // ------------------------------------------------- THE SIMPLE-NAME COLLISION (PR #645 commit 6, PART 1)

    private static IRTypeNode collideNode(IRPropertyModelTest.Fixture f, String typeName) {
        return f.index().node(COLLIDE_NAMESPACE, dataType(f.lib(), typeName));
    }

    private static PojoSectionOracle.Sections collideOracle(IRPropertyModelTest.Fixture f, String typeName) {
        return PojoSectionOracle.of(dataType(f.lib(), typeName), f.gm(), VERSION);
    }

    /**
     * THE FIRST-CLAIM LAW AT SECTION 1, on a fixture that actually collides (v3.3 seat 9, PR #645 commit 6). Two
     * different canonical types of ONE simple name - {@code seat9.collide.lib.List} and {@code java.util.List} -
     * enter one POJO's import set, and which of them keeps the bare name is decided by the ORDER of the getters.
     * The two fixture types decide it in opposite directions, so a law that always prefers one side is red here.
     *
     * <p>Three byte-compares, each between TWO PRODUCERS: the UNRESOLVED import set (the old generator's own
     * collector list, through {@code lastRawImports()} - the RESOLVED list cannot be held against an unresolved one
     * on a colliding type); the RAW getters section, where the claim order is written; and the RESOLVED import
     * block of the emitter's own rendered prefix, line by line, against the old generator's resolved list.
     */
    @Test
    void section1ResolvesASimpleNameCollisionExactlyAsTheOldGeneratorDoesInBothDirections() {
        IRPropertyModelTest.Fixture f = collisionFixture();
        IRDataTypeEmitter emitter = emitter(f);
        for (String name : List.of("ListLoser", "ListWinner")) {
            IRTypeNode n = collideNode(f, name);
            IRPropertyModel properties = emitter.propertiesOf(n);
            PojoSectionOracle.Sections expected = collideOracle(f, name);

            assertEquals(expected.rawImports(), emitter.imports(n, properties),
                    "section 1's UNRESOLVED import set on " + name + " - both List canonicals are in it");
            assertEquals(expected.staticImports(), emitter.staticImports(n, properties));
            assertEquals(expected.sections().get("GETTERS"), emitter.getters(properties),
                    "section 6 is where the claim ORDER is written, sentinels and all, on " + name);

            assertTrue(expected.rawImports().size() == expected.imports().size() + 1,
                    "THE FIXTURE MUST COLLIDE: the old generator drops exactly one import on " + name
                            + " - raw=" + expected.rawImports() + " resolved=" + expected.imports());

            // v3.3 seat 9 (PR #645 commit 9): neither collision type is specialized, so the render no longer
            // stops at all - the WHOLE file is what carries the resolved import block now
            String file = emitter.render(n);
            assertEquals(expected.wholeFile(), file,
                    "the WHOLE file on " + name + ", resolution and all");
            for (String fqn : expected.rawImports()) {
                boolean kept = expected.imports().contains(fqn);
                assertEquals(kept, file.contains("import " + fqn + ";\n"),
                        "the RESOLVED import block of " + name + " must carry " + fqn + " exactly when the old"
                                + " generator kept it (kept=" + kept + ")");
            }
        }
    }

    @Test
    void theCollisionFixtureLetsTheModelTypeWinWhenItsGetterComesFirst() {
        IRPropertyModelTest.Fixture f = collisionFixture();
        assertTrue(collideOracle(f, "ListLoser").rawImports().contains(COLLIDE_LIB_NAMESPACE + ".List"),
                "THE TWO NAMESPACES ARE REAL: the model type is declared in " + COLLIDE_LIB_NAMESPACE
                        + " and reaches the POJO of " + COLLIDE_NAMESPACE + " through a wildcard import");
        assertTrue(collideOracle(f, "ListLoser").imports().contains("java.util.List"),
                "ListLoser's first getter is the LIST-shaped one, so the library token claims the name");
        assertFalse(collideOracle(f, "ListLoser").imports().contains("seat9.collide.lib.List"),
                "and the model type is FQN-inlined, its import dropped");
        assertTrue(collideOracle(f, "ListWinner").imports().contains("seat9.collide.lib.List"),
                "ListWinner's first getter returns the MODEL type, which claims the name first");
        assertFalse(collideOracle(f, "ListWinner").imports().contains("java.util.List"),
                "so java.util.List is the loser here - the direction a fixture with one type could not witness");
    }

    // ---------------------------------------------------------------------------- the emitter's own population

    /**
     * THE EMITTER'S OWN POPULATION, RE-CUT (v3.3 seat 10, PR #646 commit 4). Through commit 3 this test read "a
     * CHOICE is refused by name"; the CHOICE kind is this emitter's population NOW, so what the test states is
     * the population's EDGE: the two kinds it admits, and an ENUM - which owns a file of its own, written by
     * {@link IREnumEmitter} - still refused by name.
     */
    @Test
    void theEmittersPopulationIsTheTwoVALIDATEDKindsAndEveryOtherKindIsRefusedByName() {
        IRPropertyModelTest.Fixture f = fixture();
        IRDataTypeEmitter emitter = choiceEmitter(f);
        // the CHOICE kind is admitted since this commit - it has a property surface and a POJO
        assertFalse(emitter.propertiesOf(choiceNode(f, "Either")).allProperties().isEmpty(),
                "a choice's options ARE its properties (RJavaPojoInterface:343-389)");
        assertFalse(emitter.propertiesOf(node(f, "Leaf")).allProperties().isEmpty(), "and a data type's are too");
        // every other kind still names no POJO of this emitter's - the fixture's typeAlias, which writes no file
        com.regnosys.rosetta.ast.types.RTypeAlias small = f.lib().rootElements().stream()
                .filter(com.regnosys.rosetta.ast.types.RTypeAlias.class::isInstance)
                .map(com.regnosys.rosetta.ast.types.RTypeAlias.class::cast)
                .filter(a -> a.name().equals("Small")).findFirst().orElseThrow();
        IRTypeNode aliasNode = f.index().node(NAMESPACE, small);
        GenerationException refusal = assertThrows(GenerationException.class,
                () -> emitter.propertiesOf(aliasNode));
        assertTrue(refusal.getMessage().contains("is a TYPE_ALIAS"), refusal.getMessage());
        assertTrue(refusal.getMessage().contains("this emitter writes the `type` POJO only"),
                refusal.getMessage());
    }
}
