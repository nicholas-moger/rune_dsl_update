package holdout.typenamedrosetta.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.QualifyResult;
import holdout.typenamedrosetta.validation.QualifyResultTypeFormatValidator;
import holdout.typenamedrosetta.validation.QualifyResultValidator;
import holdout.typenamedrosetta.validation.exists.QualifyResultOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=QualifyResult.class)
public class QualifyResultMeta implements RosettaMetaData<QualifyResult> {

	@Override
	public List<Validator<? super QualifyResult>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super QualifyResult, com.rosetta.model.lib.qualify.QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super QualifyResult> validator(ValidatorFactory factory) {
		return factory.<QualifyResult>create(QualifyResultValidator.class);
	}

	@Override
	public Validator<? super QualifyResult> typeFormatValidator(ValidatorFactory factory) {
		return factory.<QualifyResult>create(QualifyResultTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super QualifyResult> validator() {
		return new QualifyResultValidator();
	}

	@Deprecated
	@Override
	public Validator<? super QualifyResult> typeFormatValidator() {
		return new QualifyResultTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super QualifyResult, Set<String>> onlyExistsValidator() {
		return new QualifyResultOnlyExistsValidator();
	}
}
