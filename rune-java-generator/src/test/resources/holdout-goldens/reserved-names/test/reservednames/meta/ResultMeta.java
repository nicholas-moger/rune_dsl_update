package test.reservednames.meta;

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
import test.reservednames.Result;
import test.reservednames.validation.ResultTypeFormatValidator;
import test.reservednames.validation.ResultValidator;
import test.reservednames.validation.datarule.ResultValueExists;
import test.reservednames.validation.exists.ResultOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Result.class)
public class ResultMeta implements RosettaMetaData<Result> {

	@Override
	public List<Validator<? super Result>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<Result>create(ResultValueExists.class)
		);
	}
	
	@Override
	public List<Function<? super Result, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Result> validator(ValidatorFactory factory) {
		return factory.<Result>create(ResultValidator.class);
	}

	@Override
	public Validator<? super Result> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Result>create(ResultTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Result> validator() {
		return new ResultValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Result> typeFormatValidator() {
		return new ResultTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Result, Set<String>> onlyExistsValidator() {
		return new ResultOnlyExistsValidator();
	}
}
