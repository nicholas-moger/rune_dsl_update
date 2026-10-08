package holdout.typenamedrosetta.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.Validator;
import holdout.typenamedrosetta.validation.ValidatorTypeFormatValidator;
import holdout.typenamedrosetta.validation.ValidatorValidator;
import holdout.typenamedrosetta.validation.exists.ValidatorOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Validator.class)
public class ValidatorMeta implements RosettaMetaData<Validator> {

	@Override
	public List<com.rosetta.model.lib.validation.Validator<? super Validator>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Validator, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public com.rosetta.model.lib.validation.Validator<? super Validator> validator(ValidatorFactory factory) {
		return factory.<Validator>create(ValidatorValidator.class);
	}

	@Override
	public com.rosetta.model.lib.validation.Validator<? super Validator> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Validator>create(ValidatorTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public com.rosetta.model.lib.validation.Validator<? super Validator> validator() {
		return new ValidatorValidator();
	}

	@Deprecated
	@Override
	public com.rosetta.model.lib.validation.Validator<? super Validator> typeFormatValidator() {
		return new ValidatorTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Validator, Set<String>> onlyExistsValidator() {
		return new ValidatorOnlyExistsValidator();
	}
}
