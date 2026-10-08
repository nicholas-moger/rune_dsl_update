package test.chswitch.meta;

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
import test.chswitch.Sub1;
import test.chswitch.validation.Sub1TypeFormatValidator;
import test.chswitch.validation.Sub1Validator;
import test.chswitch.validation.exists.Sub1OnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Sub1.class)
public class Sub1Meta implements RosettaMetaData<Sub1> {

	@Override
	public List<Validator<? super Sub1>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Sub1, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Sub1> validator(ValidatorFactory factory) {
		return factory.<Sub1>create(Sub1Validator.class);
	}

	@Override
	public Validator<? super Sub1> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Sub1>create(Sub1TypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Sub1> validator() {
		return new Sub1Validator();
	}

	@Deprecated
	@Override
	public Validator<? super Sub1> typeFormatValidator() {
		return new Sub1TypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Sub1, Set<String>> onlyExistsValidator() {
		return new Sub1OnlyExistsValidator();
	}
}
