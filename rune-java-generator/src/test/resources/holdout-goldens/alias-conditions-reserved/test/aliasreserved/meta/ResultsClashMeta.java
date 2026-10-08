package test.aliasreserved.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import test.aliasreserved.ResultsClash;
import test.aliasreserved.validation.ResultsClashTypeFormatValidator;
import test.aliasreserved.validation.ResultsClashValidator;
import test.aliasreserved.validation.exists.ResultsClashOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=ResultsClash.class)
public class ResultsClashMeta implements RosettaMetaData<ResultsClash> {

	@Override
	public List<Validator<? super ResultsClash>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super ResultsClash, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super ResultsClash> validator(ValidatorFactory factory) {
		return factory.<ResultsClash>create(ResultsClashValidator.class);
	}

	@Override
	public Validator<? super ResultsClash> typeFormatValidator(ValidatorFactory factory) {
		return factory.<ResultsClash>create(ResultsClashTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super ResultsClash> validator() {
		return new ResultsClashValidator();
	}

	@Deprecated
	@Override
	public Validator<? super ResultsClash> typeFormatValidator() {
		return new ResultsClashTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super ResultsClash, Set<String>> onlyExistsValidator() {
		return new ResultsClashOnlyExistsValidator();
	}
}
