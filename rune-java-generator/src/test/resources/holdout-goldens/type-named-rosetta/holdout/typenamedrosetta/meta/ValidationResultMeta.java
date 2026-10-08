package holdout.typenamedrosetta.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.ValidationResult;
import holdout.typenamedrosetta.validation.ValidationResultTypeFormatValidator;
import holdout.typenamedrosetta.validation.ValidationResultValidator;
import holdout.typenamedrosetta.validation.exists.ValidationResultOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=ValidationResult.class)
public class ValidationResultMeta implements RosettaMetaData<ValidationResult> {

	@Override
	public List<Validator<? super ValidationResult>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super ValidationResult, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super ValidationResult> validator(ValidatorFactory factory) {
		return factory.<ValidationResult>create(ValidationResultValidator.class);
	}

	@Override
	public Validator<? super ValidationResult> typeFormatValidator(ValidatorFactory factory) {
		return factory.<ValidationResult>create(ValidationResultTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super ValidationResult> validator() {
		return new ValidationResultValidator();
	}

	@Deprecated
	@Override
	public Validator<? super ValidationResult> typeFormatValidator() {
		return new ValidationResultTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super ValidationResult, Set<String>> onlyExistsValidator() {
		return new ValidationResultOnlyExistsValidator();
	}
}
