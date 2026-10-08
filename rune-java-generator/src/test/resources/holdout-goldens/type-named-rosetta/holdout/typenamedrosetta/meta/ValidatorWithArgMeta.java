package holdout.typenamedrosetta.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import holdout.typenamedrosetta.ValidatorWithArg;
import holdout.typenamedrosetta.validation.ValidatorWithArgTypeFormatValidator;
import holdout.typenamedrosetta.validation.ValidatorWithArgValidator;
import holdout.typenamedrosetta.validation.exists.ValidatorWithArgOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=ValidatorWithArg.class)
public class ValidatorWithArgMeta implements RosettaMetaData<ValidatorWithArg> {

	@Override
	public List<Validator<? super ValidatorWithArg>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super ValidatorWithArg, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super ValidatorWithArg> validator(ValidatorFactory factory) {
		return factory.<ValidatorWithArg>create(ValidatorWithArgValidator.class);
	}

	@Override
	public Validator<? super ValidatorWithArg> typeFormatValidator(ValidatorFactory factory) {
		return factory.<ValidatorWithArg>create(ValidatorWithArgTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super ValidatorWithArg> validator() {
		return new ValidatorWithArgValidator();
	}

	@Deprecated
	@Override
	public Validator<? super ValidatorWithArg> typeFormatValidator() {
		return new ValidatorWithArgTypeFormatValidator();
	}
	
	@Override
	public com.rosetta.model.lib.validation.ValidatorWithArg<? super ValidatorWithArg, Set<String>> onlyExistsValidator() {
		return new ValidatorWithArgOnlyExistsValidator();
	}
}
