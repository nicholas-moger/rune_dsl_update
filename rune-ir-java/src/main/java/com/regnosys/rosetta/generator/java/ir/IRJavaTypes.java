package com.regnosys.rosetta.generator.java.ir;

import java.util.List;
import java.util.Objects;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.util.DottedPath;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;
import com.rosetta.util.types.JavaTypeDeclaration;

/**
 * THE IR ROUTE'S OWN {@link JavaType} VALUES (v3.3 seat 9, PR #645 commit 10 - the compat members). The coercion
 * algebra {@link IRPojoCompat} ports ({@code JavaStatementBuilder} / {@code JavaConditionalExpression} /
 * {@code JavaIfThenElseBuilder} / {@code JavaTypeJoiner}) is GENERIC TEXT MACHINERY parameterised by
 * {@link JavaType} values, exactly as {@code TemplateRenderer} and {@code ImportCollisionResolver} are generic text
 * machinery this emitter already calls. It is REUSED AS-IS; what this class supplies is the VALUES, and every one of
 * them is built from the IR's own RENDERED STRING plus its RECONCILED facts - never from an AST node, never from
 * {@code JavaTypeTranslator} and never from {@code RJavaPojoInterface}.
 *
 * <p><b>THE TWO KINDS OF VALUE, and the law that decides which a canonical name takes</b> - a DERIVATION from the
 * IR kinds, not a guess:
 * <ul>
 *   <li><b>a GENERATED type</b> - a declared {@code type} (STRUCT), {@code choice} (CHOICE) or {@code enum} (ENUM),
 *       and the {@code FieldWithMeta*} / {@code ReferenceWithMeta*} wrapper a {@code [metadata …]} attribute is
 *       wrapped into. The IR says so outright: {@code itemIsRosettaModelObject} is TRUE for a STRUCT, a CHOICE and
 *       every meta wrapper, and {@code itemIsEnum} is TRUE for an ENUM (both reconciled per property since PR #645
 *       commits 5 and 8). Such a name becomes an {@link IRGeneratedJavaClass} carrying those two facts - the IR
 *       route's counterpart of the old generator's {@code RGeneratedJavaClass} / {@code RJavaEnum}, which are
 *       likewise not on any classpath.</li>
 *   <li><b>a LIBRARY class</b> - everything else: {@code java.lang.String}, {@code java.lang.Integer},
 *       {@code java.math.BigDecimal}, {@code com.rosetta.model.lib.records.Date}, {@code java.time.LocalTime} …
 *       ({@link IRJavaTypeNames} renders every builtin as one of these). Such a name is resolved BY NAME ON THE
 *       CLASSPATH - {@code JavaClass.from(Class.forName(canonical))} - so the resulting {@code JavaClassImpl}
 *       compares {@code equals} with {@code JavaTypeUtil.INTEGER} / {@code LONG} / {@code BIG_INTEGER} /
 *       {@code BIG_DECIMAL} ({@code JavaClass:213-219}: the same concrete class, package and simple name) and the
 *       number ladder, {@code isList}, {@code getItemType} and {@code join} work unchanged.</li>
 * </ul>
 *
 * <p><b>THE ONE PLACE THE TWO FACTS DO NOT COVER</b> is a meta-wrapped property's BARE VALUE type: the surface
 * carries {@code metaValueIsRosettaModelObject} for it but no {@code isEnum}. The law closes without guessing,
 * because the kinds are exhaustive: a value that is not a model object is either a BUILTIN - and every builtin
 * {@link IRJavaTypeNames} renders is a real class on this classpath - or a declared {@code enum}, which is the only
 * remaining kind. So the classpath lookup is tried first for such a value and a MISS is an ENUM by elimination
 * ({@link #metaValue}), stated here once rather than inferred at a call site.
 *
 * <p><b>THE REFUSAL.</b> A canonical name the law says is a library class but that does not resolve on the
 * classpath is an {@link IRDataTypeEmitter.MissingIRFact} by name - the IR route does not invent a type it cannot
 * describe, exactly as {@link IRGeneratedJavaClass#isSubtypeOf} refuses a subtype question the reconciled facts
 * cannot answer.
 */
final class IRJavaTypes {

    /** The one interface a generated model type implements, as a classpath class - the fact's own target. */
    static final JavaClass<RosettaModelObject> ROSETTA_MODEL_OBJECT = JavaClass.from(RosettaModelObject.class);

    /** The builder interface a POJO interface is NEVER a subtype of - the second answer the refusal stops short of. */
    static final JavaClass<RosettaModelObjectBuilder> ROSETTA_MODEL_OBJECT_BUILDER =
            JavaClass.from(RosettaModelObjectBuilder.class);

    private IRJavaTypes() {
    }

    /**
     * THE WHOLE TYPE of a property or of one ancestor RUNG, from its rendered spelling and its two item facts.
     *
     * <p>The list wrap is INVARIANT ({@code List<X>}, never {@code List<? extends X>}) because that is what the old
     * generator's own property type is: {@code wrapExtendsIfNotFinal} ({@code JavaTypeUtil:191-198}) takes its
     * {@code wrapExtends} arm only for a {@code JavaPojoInterface} item, and {@code JavaTypeTranslator} renders
     * every model type as a plain {@code RGeneratedJavaClass} ({@code :101-121}, {@code :142-155}) - the very law
     * {@link IRPropertyModel}'s class javadoc states for the rendered STRING this method reads.
     *
     * @param rendered                 the property's or rung's Java type in the old generator's own spelling -
     *                                 {@code List<pkg.X>} or {@code pkg.X}
     * @param itemIsRosettaModelObject the reconciled {@code property.<name>.itemIsRosettaModelObject} fact (for a
     *                                 rung, the rung's own, added at PR #645 commit 10)
     * @param itemIsEnum               the reconciled {@code property.<name>.itemIsEnum} fact, likewise
     * @param algebra                  the type table the list wrap goes through - the host's own instance
     */
    static JavaType of(String rendered, boolean itemIsRosettaModelObject, boolean itemIsEnum,
                       IRTypeAlgebra algebra) {
        Objects.requireNonNull(rendered, "rendered");
        Objects.requireNonNull(algebra, "algebra");
        if (IRPropertyModel.isList(rendered)) {
            return algebra.wrapList(item(IRPropertyModel.itemOf(rendered), itemIsRosettaModelObject, itemIsEnum));
        }
        return item(rendered, itemIsRosettaModelObject, itemIsEnum);
    }

    /**
     * A GENERATED class BY NAME - the value the wing's scope machinery holds (v3.3 seat 9, PR #645 commit 12).
     * {@code JavaClassScope} / {@code JavaFileScope} are generic text machinery over {@code JavaTypeDeclaration}
     * values; the validator class the wing seeds its file scope with, and every condition class it injects, are
     * GENERATED classes on no classpath, exactly as the old generator's {@code RGeneratedJavaClass} values are.
     * Neither is a model object nor an enum and nothing asks either question of them - the scope reads their
     * package and their simple name and nothing else.
     */
    static JavaClass<?> generated(String canonical) {
        return new IRGeneratedJavaClass(canonical, false, false);
    }

    /**
     * ONE ITEM type - a class, never a list. The law of the class javadoc: a model object or an enum is a
     * GENERATED type the IR describes; anything else is a library class resolved by name on the classpath.
     */
    static JavaClass<?> item(String canonical, boolean isRosettaModelObject, boolean isEnum) {
        if (isRosettaModelObject || isEnum) {
            return new IRGeneratedJavaClass(canonical, isRosettaModelObject, isEnum);
        }
        return classpath(canonical);
    }

    /**
     * THE BARE VALUE type behind a meta wrap, from the ONE fact the surface carries about it
     * ({@code metaValueIsRosettaModelObject}). A model object is a generated type outright; anything else is tried
     * on the classpath FIRST - every builtin {@link IRJavaTypeNames} renders is a real class - and a miss is a
     * declared {@code enum} by elimination, which is the only other kind a non-model-object value can have. No
     * guess is taken and no byte is decided by the difference: the algebra asks a value type only for
     * {@code equals}, for the number-ladder constants and for {@code isRosettaModelObject}.
     */
    static JavaClass<?> metaValue(String canonical, boolean isRosettaModelObject) {
        if (isRosettaModelObject) {
            return new IRGeneratedJavaClass(canonical, true, false);
        }
        JavaClass<?> onClasspath = classpathOrNull(canonical);
        return onClasspath != null ? onClasspath : new IRGeneratedJavaClass(canonical, false, true);
    }

    /** The classpath class of a canonical name, or the named refusal ({@code MissingIRFact}) when there is none. */
    private static JavaClass<?> classpath(String canonical) {
        JavaClass<?> resolved = classpathOrNull(canonical);
        if (resolved == null) {
            throw new IRDataTypeEmitter.MissingIRFact("type." + canonical + ".onTheClasspath",
                    "the IR says this type is neither a model object nor an enum, so it must be a library class"
                            + " the generator can resolve by name - and it does not resolve. The compat member is"
                            + " refused by name rather than written against a type the route cannot describe");
        }
        return resolved;
    }

    private static JavaClass<?> classpathOrNull(String canonical) {
        try {
            return JavaClass.from(Class.forName(canonical));
        } catch (ClassNotFoundException | LinkageError absent) {
            return null;
        }
    }

    /**
     * A MODEL type the IR route describes from its facts alone - the counterpart of the old generator's
     * {@code RGeneratedJavaClass} ({@code rune-java-generator/.../types/RGeneratedJavaClass.java}) and
     * {@code RJavaEnum}, neither of which is on any classpath either.
     *
     * <p><b>THE LATTICE QUESTIONS IT ANSWERS, AND THE ONE IT REFUSES.</b> The type's DECLARED supertype set is
     * finite and wholly carried by the IR: {@code Object}, plus {@code RosettaModelObject} exactly when the
     * reconciled {@code itemIsRosettaModelObject} fact says so. {@link #isSubtypeOf} and
     * {@link #extendsDeclaration} therefore walk that set - equality, then the superclass, then the interfaces -
     * which is {@code RGeneratedJavaClass.isSubtypeOf} / {@code .extendsDeclaration}
     * ({@code rune-java-generator/.../types/RGeneratedJavaClass.java:123-135}) ALGORITHM FOR ALGORITHM. It is a
     * derivation, not a guess: the old generator's own generated classes carry the identical flat shape (they
     * hold no model-level supertype link either), so the two routes answer every one of these alike.
     *
     * <p><b>WHAT THE PROBE CORRECTED</b> (v3.3 seat 9, PR #645 commit 10, measured): the seat contract predicted
     * that the algebra asks a lattice question only through {@code JavaTypeUtil.isRosettaModelObject}
     * ({@code :236-238}) and that ANY other question should refuse by name. The run refuted it -
     * {@code isRosettaModelObject} calls {@code getItemType} ({@code :208-221}), which asks {@code isWrapper}
     * ({@code :281-284}), which asks {@code extendsMapper} ({@code :247-254}), which asks
     * {@code extendsDeclaration(MAPPER)} of EVERY type it is handed. A refusal there is a wall and not a net: it
     * stops the emitter rendering any specialized type at all, and the answer it refuses to give is fully
     * determined by the declared set above. So the refusal was NARROWED to the one question that set genuinely
     * cannot answer - whether one GENERATED model type extends ANOTHER. That relation lives in the Rosetta
     * supertype chain, not in this value; the compat algebra never asks it (the DSL restriction law fixes the
     * model-vs-model direction, {@code PojoCompatEmitter:626-634}, and every ternary it builds joins a type with
     * ITSELF or with {@code NULL_TYPE}, so {@code JavaTypeJoiner:95-135}'s unequal-class walk is never reached);
     * if it ever does, the type says so by name instead of answering {@code false} by construction.
     */
    static final class IRGeneratedJavaClass extends JavaClass<Object> {

        private final DottedPath packageName;
        private final DottedPath nestedTypeName;
        private final boolean rosettaModelObject;
        private final boolean javaEnum;

        IRGeneratedJavaClass(String canonical, boolean rosettaModelObject, boolean javaEnum) {
            Objects.requireNonNull(canonical, "canonical");
            int lastDot = canonical.lastIndexOf('.');
            this.packageName = lastDot < 0 ? DottedPath.of() : DottedPath.splitOnDots(canonical.substring(0, lastDot));
            this.nestedTypeName = DottedPath.of(lastDot < 0 ? canonical : canonical.substring(lastDot + 1));
            this.rosettaModelObject = rosettaModelObject;
            this.javaEnum = javaEnum;
        }

        /** Whether the declaration behind this name is emitted as a {@code RosettaModelObject} interface. */
        boolean isRosettaModelObject() {
            return rosettaModelObject;
        }

        /**
         * {@code RGeneratedJavaClass.isSubtypeOf} ({@code :123-128}) over the DECLARED supertype set the IR
         * carries: this type, then {@code Object}, then {@code RosettaModelObject} when the fact says so. A
         * question about ANOTHER generated model type is refused by name - that relation is the Rosetta
         * supertype chain's, which this value does not carry.
         */
        @Override
        public boolean isSubtypeOf(JavaType other) {
            if (this.equals(other)) {
                return true;
            }
            if (other instanceof IRGeneratedJavaClass) {
                throw refusal(other);   // model-vs-model: the one relation the declared set cannot state
            }
            if (getSuperclass().isSubtypeOf(other)) {
                return true;
            }
            return getInterfaces().stream().anyMatch(i -> i.isSubtypeOf(other));
        }

        /**
         * {@code RGeneratedJavaClass.extendsDeclaration} ({@code :130-135}), the declaration half of the same
         * walk. It is the one the type table reaches on EVERY type - {@code JavaTypeUtil.getItemType} asks
         * {@code isWrapper}, which asks {@code extendsMapper}, which asks this of whatever it is handed.
         */
        @Override
        public boolean extendsDeclaration(JavaTypeDeclaration<?> other) {
            if (this.equals(other)) {
                return true;
            }
            if (other instanceof IRGeneratedJavaClass) {
                throw refusal(other);   // model-vs-model, in its declaration form
            }
            if (getSuperclassDeclaration().extendsDeclaration(other)) {
                return true;
            }
            return getInterfaceDeclarations().stream().anyMatch(d -> d.extendsDeclaration(other));
        }

        private IRDataTypeEmitter.MissingIRFact refusal(Object other) {
            return new IRDataTypeEmitter.MissingIRFact(
                    "type." + getCanonicalName().withDots() + ".extendsModelType(" + other + ")",
                    "whether one GENERATED model type extends another is the Rosetta supertype chain's relation,"
                            + " and this value carries only the declared Java supertype set (Object, plus"
                            + " RosettaModelObject by the reconciled fact) - the question is refused by name"
                            + " rather than answered false by construction");
        }

        @Override
        public JavaClass<? super Object> getSuperclass() {
            return JavaClass.OBJECT;
        }

        @Override
        public JavaTypeDeclaration<? super Object> getSuperclassDeclaration() {
            return JavaClass.OBJECT;
        }

        @Override
        public List<JavaClass<?>> getInterfaces() {
            return rosettaModelObject
                    ? List.<JavaClass<?>>of(ROSETTA_MODEL_OBJECT) : List.<JavaClass<?>>of();
        }

        @Override
        public List<JavaTypeDeclaration<?>> getInterfaceDeclarations() {
            return rosettaModelObject
                    ? List.<JavaTypeDeclaration<?>>of(ROSETTA_MODEL_OBJECT)
                    : List.<JavaTypeDeclaration<?>>of();
        }

        @Override
        public DottedPath getPackageName() {
            return packageName;
        }

        @Override
        public DottedPath getNestedTypeName() {
            return nestedTypeName;
        }

        /** A Java {@code enum} is final; an emitted interface is not. Nothing at this seat asks, and it is stated anyway. */
        @Override
        public boolean isFinal() {
            return javaEnum;
        }

        @Override
        public Class<?> loadClass(ClassLoader classLoader) throws ClassNotFoundException {
            throw new UnsupportedOperationException("IR data-type emitter: "
                    + getCanonicalName().withDots() + " is a GENERATED class - it cannot be loaded");
        }
    }
}
