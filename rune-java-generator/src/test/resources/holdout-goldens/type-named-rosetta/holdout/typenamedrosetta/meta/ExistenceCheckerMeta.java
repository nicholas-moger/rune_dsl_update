package holdout.typenamedrosetta.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.ExistenceChecker;
import holdout.typenamedrosetta.validation.ExistenceCheckerTypeFormatValidator;
import holdout.typenamedrosetta.validation.ExistenceCheckerValidator;
import holdout.typenamedrosetta.validation.exists.ExistenceCheckerOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=ExistenceChecker.class)
public class ExistenceCheckerMeta implements RosettaMetaData<ExistenceChecker> {

	@Override
	public List<Validator<? super ExistenceChecker>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super ExistenceChecker, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super ExistenceChecker> validator(ValidatorFactory factory) {
		return factory.<ExistenceChecker>create(ExistenceCheckerValidator.class);
	}

	@Override
	public Validator<? super ExistenceChecker> typeFormatValidator(ValidatorFactory factory) {
		return factory.<ExistenceChecker>create(ExistenceCheckerTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super ExistenceChecker> validator() {
		return new ExistenceCheckerValidator();
	}

	@Deprecated
	@Override
	public Validator<? super ExistenceChecker> typeFormatValidator() {
		return new ExistenceCheckerTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super ExistenceChecker, Set<String>> onlyExistsValidator() {
		return new ExistenceCheckerOnlyExistsValidator();
	}
}
