package test.datesubtract.meta;

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
import test.datesubtract.Test;
import test.datesubtract.validation.TestTypeFormatValidator;
import test.datesubtract.validation.TestValidator;
import test.datesubtract.validation.exists.TestOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Test.class)
public class TestMeta implements RosettaMetaData<Test> {

	@Override
	public List<Validator<? super Test>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Test, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Test> validator(ValidatorFactory factory) {
		return factory.<Test>create(TestValidator.class);
	}

	@Override
	public Validator<? super Test> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Test>create(TestTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Test> validator() {
		return new TestValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Test> typeFormatValidator() {
		return new TestTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Test, Set<String>> onlyExistsValidator() {
		return new TestOnlyExistsValidator();
	}
}
