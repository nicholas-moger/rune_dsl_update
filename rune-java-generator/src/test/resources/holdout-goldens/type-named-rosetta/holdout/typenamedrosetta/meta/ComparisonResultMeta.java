package holdout.typenamedrosetta.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.ComparisonResult;
import holdout.typenamedrosetta.validation.ComparisonResultTypeFormatValidator;
import holdout.typenamedrosetta.validation.ComparisonResultValidator;
import holdout.typenamedrosetta.validation.exists.ComparisonResultOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=ComparisonResult.class)
public class ComparisonResultMeta implements RosettaMetaData<ComparisonResult> {

	@Override
	public List<Validator<? super ComparisonResult>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super ComparisonResult, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super ComparisonResult> validator(ValidatorFactory factory) {
		return factory.<ComparisonResult>create(ComparisonResultValidator.class);
	}

	@Override
	public Validator<? super ComparisonResult> typeFormatValidator(ValidatorFactory factory) {
		return factory.<ComparisonResult>create(ComparisonResultTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super ComparisonResult> validator() {
		return new ComparisonResultValidator();
	}

	@Deprecated
	@Override
	public Validator<? super ComparisonResult> typeFormatValidator() {
		return new ComparisonResultTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super ComparisonResult, Set<String>> onlyExistsValidator() {
		return new ComparisonResultOnlyExistsValidator();
	}
}
