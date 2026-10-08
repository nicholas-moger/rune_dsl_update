package com.regnosys.rosetta.generator.java.object;

import java.util.Objects;
import java.util.stream.Stream;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.JavaClassGenerator;
import com.regnosys.rosetta.generator.java.template.model.PojoTemplateModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaPojoInterface;

/**
 * Generates the complete Java source file for a {@code choice} declaration:
 * interface, Impl class, Builder interface, BuilderImpl class.
 *
 * <p>P2.1.3c β1 fix: prior to this generator, choice-type POJOs (top-level
 * {@code choice X: Foo Bar Baz}) were not emitted at all — the corpus loader
 * surfaced them as {@code missingOutput} D11 verdicts. {@link ModelObjectGenerator}'s
 * {@link ModelObjectGenerator#streamObjects(RModel) streamObjects} only filters
 * for {@link com.regnosys.rosetta.ast.types.RDataType RDataType} root elements,
 * so {@code RChoice} root elements went un-emitted.
 *
 * <p>This generator mirrors {@code ModelObjectGenerator} in shape (same template
 * group, same per-element flow) and delegates the heavy lifting of the imports +
 * body assembly to {@link ModelObjectGenerator#buildModelForChoice(RChoice,
 * RJavaPojoInterface, String)}. The {@code RJavaPojoInterface(RChoice,
 * GeneratorModel, JavaTypeTranslator, JavaTypeUtil)} ctor + its
 * {@code initializeChoiceProperties} routine already map each choice option to a
 * single-valued, non-required, model-object property; the resulting POJO shape
 * matches the upstream byte-identity golden for top-level choices.
 *
 * <p>Wired alongside {@link ModelObjectGenerator} in every generators-list
 * construction site (see T2 step 2.8 of the P2.1.3c plan).
 */
public class ChoiceObjectGenerator extends JavaClassGenerator<RChoice, RJavaPojoInterface> {

    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final JavaTypeUtil typeUtil;
    private final ModelObjectGenerator delegate;

    /**
     * Preferred constructor — shares the caller's {@link ModelObjectGenerator}
     * instance so the {@code java-pojo.stg} template/imports/body machinery is
     * loaded once per generator pipeline rather than twice.
     *
     * <p>P2.1.3c T2 R1 F6/F9/F10: every wired call site constructs a
     * {@link ModelObjectGenerator} first, then this generator; threading the
     * existing instance avoids both the duplicate
     * {@code renderer.loadGroupFromClasspath(...)} cost and the asymmetric-wiring
     * footgun that previously left this generator constructing its own private
     * delegate.
     */
    public ChoiceObjectGenerator(GeneratorModel generatorModel, JavaTypeTranslator typeTranslator,
                                  JavaTypeUtil typeUtil, ModelObjectGenerator delegate) {
        this.generatorModel = generatorModel;
        this.typeTranslator = typeTranslator;
        this.typeUtil = typeUtil;
        this.delegate = Objects.requireNonNull(delegate,
                "ChoiceObjectGenerator requires a shared ModelObjectGenerator instance "
                + "(construct ModelObjectGenerator first, then pass it here)");
    }

    @Override
    protected Stream<? extends RChoice> streamObjects(RModel model) {
        return model.rootElements().stream()
                .filter(e -> e instanceof RChoice)
                .map(e -> (RChoice) e);
    }

    @Override
    protected RJavaPojoInterface createTypeRepresentation(RChoice choiceNode) {
        return new RJavaPojoInterface(choiceNode, generatorModel, typeTranslator, typeUtil);
    }

    @Override
    protected String generate(RChoice choiceNode, RJavaPojoInterface pojo, String version) {
        PojoTemplateModel model = delegate.buildModelForChoice(choiceNode, pojo, version);
        return delegate.renderPojoFile(model);
    }
}
