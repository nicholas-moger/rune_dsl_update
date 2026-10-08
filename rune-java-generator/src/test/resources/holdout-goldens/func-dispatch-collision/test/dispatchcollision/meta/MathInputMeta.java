package test.dispatchcollision.meta;

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
import test.dispatchcollision.MathInput;
import test.dispatchcollision.validation.MathInputTypeFormatValidator;
import test.dispatchcollision.validation.MathInputValidator;
import test.dispatchcollision.validation.exists.MathInputOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=MathInput.class)
public class MathInputMeta implements RosettaMetaData<MathInput> {

	@Override
	public List<Validator<? super MathInput>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super MathInput, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super MathInput> validator(ValidatorFactory factory) {
		return factory.<MathInput>create(MathInputValidator.class);
	}

	@Override
	public Validator<? super MathInput> typeFormatValidator(ValidatorFactory factory) {
		return factory.<MathInput>create(MathInputTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super MathInput> validator() {
		return new MathInputValidator();
	}

	@Deprecated
	@Override
	public Validator<? super MathInput> typeFormatValidator() {
		return new MathInputTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super MathInput, Set<String>> onlyExistsValidator() {
		return new MathInputOnlyExistsValidator();
	}
}
