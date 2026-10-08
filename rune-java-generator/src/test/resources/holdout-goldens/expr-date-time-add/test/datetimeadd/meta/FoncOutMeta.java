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
import test.datetimeadd.FoncOut;
import test.datetimeadd.validation.FoncOutTypeFormatValidator;
import test.datetimeadd.validation.FoncOutValidator;
import test.datetimeadd.validation.exists.FoncOutOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=FoncOut.class)
public class FoncOutMeta implements RosettaMetaData<FoncOut> {

	@Override
	public List<Validator<? super FoncOut>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super FoncOut, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super FoncOut> validator(ValidatorFactory factory) {
		return factory.<FoncOut>create(FoncOutValidator.class);
	}

	@Override
	public Validator<? super FoncOut> typeFormatValidator(ValidatorFactory factory) {
		return factory.<FoncOut>create(FoncOutTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super FoncOut> validator() {
		return new FoncOutValidator();
	}

	@Deprecated
	@Override
	public Validator<? super FoncOut> typeFormatValidator() {
		return new FoncOutTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super FoncOut, Set<String>> onlyExistsValidator() {
		return new FoncOutOnlyExistsValidator();
	}
}
