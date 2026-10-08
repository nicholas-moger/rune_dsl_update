package holdout.typenamedrosetta.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.ValidatorFactory;
import holdout.typenamedrosetta.validation.ValidatorFactoryTypeFormatValidator;
import holdout.typenamedrosetta.validation.ValidatorFactoryValidator;
import holdout.typenamedrosetta.validation.exists.ValidatorFactoryOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=ValidatorFactory.class)
public class ValidatorFactoryMeta implements RosettaMetaData<ValidatorFactory> {

	@Override
	public List<Validator<? super ValidatorFactory>> dataRules(com.rosetta.model.lib.validation.ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super ValidatorFactory, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super ValidatorFactory> validator(com.rosetta.model.lib.validation.ValidatorFactory factory) {
		return factory.<ValidatorFactory>create(ValidatorFactoryValidator.class);
	}

	@Override
	public Validator<? super ValidatorFactory> typeFormatValidator(com.rosetta.model.lib.validation.ValidatorFactory factory) {
		return factory.<ValidatorFactory>create(ValidatorFactoryTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super ValidatorFactory> validator() {
		return new ValidatorFactoryValidator();
	}

	@Deprecated
	@Override
	public Validator<? super ValidatorFactory> typeFormatValidator() {
		return new ValidatorFactoryTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super ValidatorFactory, Set<String>> onlyExistsValidator() {
		return new ValidatorFactoryOnlyExistsValidator();
	}
}
