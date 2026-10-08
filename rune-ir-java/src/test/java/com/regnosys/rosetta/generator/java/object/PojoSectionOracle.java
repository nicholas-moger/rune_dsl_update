package com.regnosys.rosetta.generator.java.object;

import java.util.List;
import java.util.Map;

import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.template.model.PojoTemplateModel;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaPojoInterface;

/**
 * THE ORACLE HALF OF THE DATA-TYPE EMITTER'S SECTION TESTS (v3.3 seat 9, PR #645 commit 4): the OLD generator's own
 * bytes for a data type, section by section, read through {@link ModelObjectGenerator}'s package-private seams.
 *
 * <p><b>WHY THIS CLASS LIVES IN {@code com.regnosys.rosetta.generator.java.object} inside {@code rune-ir-java}'s
 * TEST tree.</b> The seat's contract requires the seam on {@code ModelObjectGenerator} to be PACKAGE-PRIVATE - no
 * public surface is widened on the OFF route, so no behaviour of the shipping generator can change. A
 * package-private member is only reachable from its own package, so the bridge that reads it must declare that
 * package; it is test scope, it has no production caller, and it ships in no jar. The alternative - making
 * {@code buildModel} and {@code lastBodySections()} public - would widen the old generator's API for the sake of a
 * test, which is the weaker option of the two.
 *
 * <p>What it reads: {@link ModelObjectGenerator#buildModel} (the import set and the static imports, AFTER the D50
 * first-claim resolution - for a fixture with no simple-name collision that is the collector's own sorted set) and
 * {@link ModelObjectGenerator#lastBodySections()} (the RAW, sentinel-carrying text of the ELEVEN body sections the
 * IR emitter renders since PR #645 commit 9, where {@code BUILDER_IMPL} joined the {@code IMPL} of commit 8, the
 * {@code PROCESS} and {@code BUILDER_INTERFACE} of commit 7 and the seven of commit 6),
 * {@link ModelObjectGenerator#lastRawImports()} (the collector's import list BEFORE the resolution), - since
 * PR #645 commit 8 - {@link ModelObjectGenerator#lastBoilerPlate()} (the {@code equals} / {@code hashCode} /
 * {@code toString} text of the LAST boilerplate call the body made, which is the BUILDER variant) and - since
 * PR #645 commit 9 - the WHOLE FILE ({@link ModelObjectGenerator#renderPojoFile}, the very call
 * {@code ModelObjectGenerator.generate} makes at {@code :80-83}). All five come from ONE {@code buildModel} call,
 * so the sections, both import lists, the boilerplate text and the whole file are the same pass's.
 */
public final class PojoSectionOracle {

    /**
     * The old generator's answers for one data type.
     *
     * @param sections      the RAW body sections, keyed by {@code IRDataTypeEmitter.Section}'s own names
     * @param imports       the RESOLVED import list the template renders (the D50 losers already dropped)
     * @param rawImports    the UNRESOLVED import list the collector built, before the first-claim resolution
     *                      (v3.3 seat 9, PR #645 commit 6): equal to {@code imports} for a class text with no
     *                      simple-name collision, and LONGER by the losers for one that has - which is the only
     *                      list an IR emitter's own (unresolved) {@code imports(node, properties)} can be held
     *                      against on a COLLIDING fixture
     * @param staticImports the static import list the template renders
     * @param packageName   the POJO's package
     * @param boilerplate   the {@code equals} / {@code hashCode} / {@code toString} text of the LAST
     *                      {@code boilerPlate} / {@code builderBoilerPlate} call {@code buildBody} made (v3.3
     *                      seat 9, PR #645 commit 8) - the BUILDER variant, since
     *                      {@code generateBuilderImplClass} runs after {@code generateImplClass}. It is the
     *                      section-13 text on its OWN, un-indented and outside the {@code Impl} /
     *                      {@code BuilderImpl} whole it is embedded in
     * @param wholeFile     the WHOLE POJO file the old generator writes for this type (v3.3 seat 9, PR #645
     *                      commit 9) - {@code renderPojoFile(model)}, which is literally what
     *                      {@code ModelObjectGenerator.generate} returns ({@code :80-83}): the package line, the
     *                      RESOLVED import block, the static imports and the class text with the D50 losers
     *                      already dropped and the class-closing brace in place. It is the only honest oracle for
     *                      {@code IRDataTypeEmitter.render(node)}, which renders that same whole through its own
     *                      template copy - a concatenation of {@link #sections} would be the UNRESOLVED text and
     *                      would carry no frame at all
     */
    public record Sections(Map<String, String> sections, List<String> imports, List<String> rawImports,
                           List<String> staticImports, String packageName, String boilerplate, String wholeFile) {
    }

    private PojoSectionOracle() {
    }

    /**
     * THE SAME ORACLE FOR A {@code choice} (v3.3 seat 10, PR #646 commit 4) - the LEGACY choice pipeline exactly as
     * {@code ChoiceObjectGenerator.generate} drives it ({@code :80-83}):
     * {@code ModelObjectGenerator.buildModelForChoice(choice, pojo, version)}, which is
     * {@code buildModel(null, choice, pojo, version)}, over an {@code RJavaPojoInterface(RChoice, ...)} whose
     * properties are the options. It reads the SAME five seams the data-type overload reads, from the SAME single
     * {@code buildModel} call, so a choice's sections, both import lists, its boilerplate and its WHOLE FILE are
     * the old code's own bytes for that declaration.
     */
    public static Sections ofChoice(RChoice choiceNode, GeneratorModel generatorModel, String version) {
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);
        ModelObjectGenerator generator = new ModelObjectGenerator(generatorModel, typeTranslator, typeUtil);
        RJavaPojoInterface pojo = new RJavaPojoInterface(choiceNode, generatorModel, typeTranslator, typeUtil);
        PojoTemplateModel model = generator.buildModelForChoice(choiceNode, pojo, version);
        return new Sections(generator.lastBodySections(), model.getImports(), generator.lastRawImports(),
                model.getStaticImports(), model.getPackageName(), generator.lastBoilerPlate(),
                generator.renderPojoFile(model));
    }

    /** Builds the old generator's POJO model for {@code type} and hands back its sections and its imports. */
    public static Sections of(RDataType type, GeneratorModel generatorModel, String version) {
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);
        ModelObjectGenerator generator = new ModelObjectGenerator(generatorModel, typeTranslator, typeUtil);
        RJavaPojoInterface pojo = new RJavaPojoInterface(type, generatorModel, typeTranslator, typeUtil);
        PojoTemplateModel model = generator.buildModel(type, null, pojo, version);
        return new Sections(generator.lastBodySections(), model.getImports(), generator.lastRawImports(),
                model.getStaticImports(), model.getPackageName(), generator.lastBoilerPlate(),
                generator.renderPojoFile(model));
    }
}
