package com.regnosys.rosetta.generator.java.ir;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.3 seat 9 (PR #645 commit 10) - THE IR ROUTE'S OWN {@link JavaType} VALUES, and the four questions the
 * generated ones answer. {@link IRJavaTypes} is what makes the compat members' coercion algebra usable from the
 * IR route at all: the algebra is generic text machinery parameterised by {@code JavaType} values, so the values
 * are where the IR's facts enter it. This class states what those values ARE - that a library name resolves on
 * the classpath and compares {@code equals} with the type table's own constants, that a model name becomes a
 * generated class carrying the reconciled facts, and that a subtype question the facts cannot answer REFUSES BY
 * NAME rather than guessing.
 */
class IRJavaTypesTest {

    private final JavaTypeUtil typeUtil = new JavaTypeUtil();
    private final IRTypeAlgebra algebra = new IRTypeAlgebra(typeUtil);

    // ------------------------------------------------------------------ the classpath half: resolution BY NAME

    @Test
    void aLibraryNameResolvesOnTheClasspathAndEqualsTheTypeTablesOwnConstant() {
        assertEquals(typeUtil.INTEGER, IRJavaTypes.item("java.lang.Integer", false, false),
                "JavaClass.equals is the same concrete class + package + simple name (JavaClass:213-219), so a"
                        + " by-name resolution of java.lang.Integer IS typeUtil.INTEGER - which is what makes the"
                        + " number ladder's isInteger / isLong / isBigInteger / isBigDecimal work unchanged");
        assertEquals(typeUtil.LONG, IRJavaTypes.item("java.lang.Long", false, false));
        assertEquals(typeUtil.BIG_INTEGER, IRJavaTypes.item("java.math.BigInteger", false, false));
        assertEquals(typeUtil.BIG_DECIMAL, IRJavaTypes.item("java.math.BigDecimal", false, false));
        assertEquals(typeUtil.STRING, IRJavaTypes.item("java.lang.String", false, false));
        assertEquals(typeUtil.DATE, IRJavaTypes.item("com.rosetta.model.lib.records.Date", false, false),
                "a rune-runtime record type is on this classpath too");

        assertTrue(algebra.isInteger(IRJavaTypes.item("java.lang.Integer", false, false)));
        assertFalse(algebra.isInteger(IRJavaTypes.item("java.lang.Long", false, false)));
    }

    @Test
    void aListRenderWrapsItsItemInvariantlyAndIsReadBackAsAList() {
        JavaType list = IRJavaTypes.of("List<java.lang.String>", false, false, algebra);
        assertTrue(algebra.isList(list), "typeUtil.isList compares the generic declaration against LIST");
        assertEquals(typeUtil.STRING, algebra.getItemType(list));
        assertFalse(algebra.hasWildcardArgument(list),
                "the property type's list wrap is INVARIANT - wrapExtendsIfNotFinal takes its wildcard arm only"
                        + " for a JavaPojoInterface item, and no model type at this seat is one");
        assertTrue(algebra.hasWildcardArgument(algebra.wrapExtends(typeUtil.STRING)),
                "while the bulk arm's DECLARED binding is the wildcard form - the pair the #412 corner's"
                        + " new ArrayList(...) copy is decided by");

        JavaType single = IRJavaTypes.of("java.lang.String", false, false, algebra);
        assertFalse(algebra.isList(single));
        assertEquals(typeUtil.STRING, single);
    }

    @Test
    void aLibraryNameThatDoesNotResolveIsRefusedByNameRatherThanInvented() {
        IRDataTypeEmitter.MissingIRFact refused = assertThrows(IRDataTypeEmitter.MissingIRFact.class,
                () -> IRJavaTypes.item("nowhere.at.all.Absent", false, false));
        assertEquals("type.nowhere.at.all.Absent.onTheClasspath", refused.fact(),
                "the IR says it is neither a model object nor an enum, so it must be a library class - and it"
                        + " is not one. The refusal NAMES the fact: " + refused.getMessage());
    }

    // ---------------------------------------------------- the generated half: the facts, and the four answers

    @Test
    void aModelNameBecomesAGeneratedClassWhoseCanonicalIsItsOwnSpelling() {
        JavaClass<?> model = IRJavaTypes.item("seat8.props.Leaf", true, false);
        assertEquals("seat8.props.Leaf", model.toString(),
                "JavaClass.toString is the canonical name - the very spelling the property surface carries");
        assertEquals("Leaf", model.getSimpleName());
        assertEquals("seat8.props", model.getPackageName().withDots());
        assertTrue(algebra.isRosettaModelObject(model));

        JavaClass<?> javaEnum = IRJavaTypes.item("seat8.props.Colour", false, true);
        assertEquals("seat8.props.Colour", javaEnum.toString(),
                "an ENUM is a generated class too - it is on no classpath either, and it is the case the"
                        + " rendered NAME cannot decide (seat8.props.Colour and seat8.props.Leaf are spelt alike)");
        assertFalse(algebra.isRosettaModelObject(javaEnum));
        assertTrue(javaEnum.isFinal(), "a Java enum is final; an emitted interface is not");
        assertFalse(IRJavaTypes.item("seat8.props.Leaf", true, false).isFinal());
    }

    @Test
    void twoGeneratedClassesOfOneCanonicalAreEqualAndHashAlike() {
        JavaClass<?> one = IRJavaTypes.item("seat8.props.Leaf", true, false);
        JavaClass<?> other = IRJavaTypes.item("seat8.props.Leaf", true, false);
        assertEquals(one, other,
                "the coercion table's FIRST question is srcItem.equals(tgtItem) (PojoCompatEmitter:612), so two"
                        + " independently built values of one canonical must be equal or every identity pair"
                        + " would take a conversion it does not need");
        assertEquals(one.hashCode(), other.hashCode());
        assertNotEquals(one, IRJavaTypes.item("seat8.props.Sub", true, false));
        assertNotEquals(one, typeUtil.STRING,
                "and a generated class is never equal to a classpath class - JavaClass.equals requires the same"
                        + " CONCRETE class, which is also why the old generator's RGeneratedJavaClass and this"
                        + " one never meet");
    }

    /**
     * THE DECLARED SUPERTYPE SET ANSWERS THE LATTICE, AND ONE RELATION REFUSES. The walk is
     * {@code RGeneratedJavaClass.isSubtypeOf} / {@code .extendsDeclaration} ({@code :123-135}) algorithm for
     * algorithm, so a generated value answers exactly what the old generator's own generated class answers -
     * including {@code false} for a library class it does not implement.
     *
     * <p><b>THE PROBE CORRECTED THE CONTRACT HERE</b> (measured, PR #645 commit 10): the seat contract said any
     * question but the four below should refuse. The run showed the type table asks
     * {@code extendsDeclaration(MAPPER)} of EVERY type it is handed - {@code JavaTypeUtil.isRosettaModelObject}
     * goes through {@code getItemType} to {@code isWrapper} to {@code extendsMapper} - so a blanket refusal is a
     * wall rather than a net, and the answer is fully determined by the declared set. The refusal is narrowed to
     * the relation that set genuinely cannot state: whether one GENERATED model type extends ANOTHER.
     */
    @Test
    void aGeneratedClassAnswersFromItsDeclaredSupertypeSetAndRefusesTheModelToModelRelation() {
        JavaClass<?> model = IRJavaTypes.item("seat8.props.Leaf", true, false);
        JavaClass<?> javaEnum = IRJavaTypes.item("seat8.props.Colour", false, true);

        assertTrue(model.isSubtypeOf(IRJavaTypes.item("seat8.props.Leaf", true, false)), "1 - equals");
        assertTrue(model.isSubtypeOf(JavaClass.OBJECT), "2 - Object, through the superclass");
        assertTrue(model.isSubtypeOf(IRJavaTypes.ROSETTA_MODEL_OBJECT),
                "3 - RosettaModelObject, BY THE RECONCILED FACT and by nothing else");
        assertFalse(javaEnum.isSubtypeOf(IRJavaTypes.ROSETTA_MODEL_OBJECT),
                "which the enum's own fact answers the other way");
        assertFalse(model.isSubtypeOf(IRJavaTypes.ROSETTA_MODEL_OBJECT_BUILDER),
                "4 - a POJO interface is never a builder");
        assertFalse(model.isSubtypeOf(typeUtil.STRING),
                "5 - a library class the declared set does not contain: FALSE, exactly as the old generator's"
                        + " own RGeneratedJavaClass answers it");
        assertFalse(model.extendsDeclaration(typeUtil.MAPPER),
                "AND THE QUESTION THE TABLE ACTUALLY ASKS OF EVERY TYPE: isRosettaModelObject goes through"
                        + " getItemType to isWrapper to extendsMapper, which asks this. A refusal here would"
                        + " stop every specialized type rendering");

        IRDataTypeEmitter.MissingIRFact refused = assertThrows(IRDataTypeEmitter.MissingIRFact.class,
                () -> model.isSubtypeOf(IRJavaTypes.item("seat8.props.Sub", true, false)));
        assertEquals("type.seat8.props.Leaf.extendsModelType(seat8.props.Sub)", refused.fact(),
                "THE ONE RELATION THE DECLARED SET CANNOT STATE: whether one generated model type extends"
                        + " another lives in the Rosetta supertype chain, not in this value. The compat algebra"
                        + " never asks it - the DSL restriction law fixes the model-vs-model direction - and if"
                        + " it ever does, this says so by name: " + refused.getMessage());
        assertThrows(IRDataTypeEmitter.MissingIRFact.class,
                () -> model.extendsDeclaration(IRJavaTypes.item("seat8.props.Sub", true, false)),
                "and the DECLARATION half refuses the same relation");
    }

    // ------------------------------------------------------- the meta value: the one fact, and its elimination

    @Test
    void aMetaValueTakesTheClasspathWhenItIsABuiltinAndTheGeneratedFormOtherwise() {
        assertEquals(typeUtil.STRING, IRJavaTypes.metaValue("java.lang.String", false),
                "[metadata scheme] over a builtin - the value is a real class on this classpath");
        JavaClass<?> modelValue = IRJavaTypes.metaValue("test.pojo.Child", true);
        assertEquals("test.pojo.Child", modelValue.toString());
        assertTrue(algebra.isRosettaModelObject(modelValue),
                "[metadata reference] over a declared type - the value's OWN kind, carried by the surface as"
                        + " metaValueIsRosettaModelObject");
        JavaClass<?> enumValue = IRJavaTypes.metaValue("some.ns.Colour", false);
        assertEquals("some.ns.Colour", enumValue.toString(),
                "a value that is neither a model object nor on the classpath can only be a declared enum - the"
                        + " kinds are exhaustive, so this is an elimination and not a guess");
        assertFalse(algebra.isRosettaModelObject(enumValue));
    }
}
