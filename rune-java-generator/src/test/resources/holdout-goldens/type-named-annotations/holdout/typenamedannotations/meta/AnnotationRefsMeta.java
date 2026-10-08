package holdout.typenamedannotations.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedannotations.AnnotationRefs;
import holdout.typenamedannotations.validation.AnnotationRefsTypeFormatValidator;
import holdout.typenamedannotations.validation.AnnotationRefsValidator;
import holdout.typenamedannotations.validation.exists.AnnotationRefsOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=AnnotationRefs.class)
public class AnnotationRefsMeta implements RosettaMetaData<AnnotationRefs> {

	@Override
	public List<Validator<? super AnnotationRefs>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super AnnotationRefs, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super AnnotationRefs> validator(ValidatorFactory factory) {
		return factory.<AnnotationRefs>create(AnnotationRefsValidator.class);
	}

	@Override
	public Validator<? super AnnotationRefs> typeFormatValidator(ValidatorFactory factory) {
		return factory.<AnnotationRefs>create(AnnotationRefsTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super AnnotationRefs> validator() {
		return new AnnotationRefsValidator();
	}

	@Deprecated
	@Override
	public Validator<? super AnnotationRefs> typeFormatValidator() {
		return new AnnotationRefsTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super AnnotationRefs, Set<String>> onlyExistsValidator() {
		return new AnnotationRefsOnlyExistsValidator();
	}
}
