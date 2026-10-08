package holdout.typenamedannotations.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedannotations.RuneAttribute;
import holdout.typenamedannotations.validation.RuneAttributeTypeFormatValidator;
import holdout.typenamedannotations.validation.RuneAttributeValidator;
import holdout.typenamedannotations.validation.exists.RuneAttributeOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RuneAttribute.class)
public class RuneAttributeMeta implements RosettaMetaData<RuneAttribute> {

	@Override
	public List<Validator<? super RuneAttribute>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RuneAttribute, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RuneAttribute> validator(ValidatorFactory factory) {
		return factory.<RuneAttribute>create(RuneAttributeValidator.class);
	}

	@Override
	public Validator<? super RuneAttribute> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RuneAttribute>create(RuneAttributeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RuneAttribute> validator() {
		return new RuneAttributeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RuneAttribute> typeFormatValidator() {
		return new RuneAttributeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RuneAttribute, Set<String>> onlyExistsValidator() {
		return new RuneAttributeOnlyExistsValidator();
	}
}
