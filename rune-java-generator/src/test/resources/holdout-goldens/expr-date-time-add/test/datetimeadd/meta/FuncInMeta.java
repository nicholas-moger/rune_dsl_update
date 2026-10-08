package test.datetimeadd.meta;

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
import test.datetimeadd.FuncIn;
import test.datetimeadd.validation.FuncInTypeFormatValidator;
import test.datetimeadd.validation.FuncInValidator;
import test.datetimeadd.validation.exists.FuncInOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=FuncIn.class)
public class FuncInMeta implements RosettaMetaData<FuncIn> {

	@Override
	public List<Validator<? super FuncIn>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super FuncIn, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super FuncIn> validator(ValidatorFactory factory) {
		return factory.<FuncIn>create(FuncInValidator.class);
	}

	@Override
	public Validator<? super FuncIn> typeFormatValidator(ValidatorFactory factory) {
		return factory.<FuncIn>create(FuncInTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super FuncIn> validator() {
		return new FuncInValidator();
	}

	@Deprecated
	@Override
	public Validator<? super FuncIn> typeFormatValidator() {
		return new FuncInTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super FuncIn, Set<String>> onlyExistsValidator() {
		return new FuncInOnlyExistsValidator();
	}
}
