package test.chswitchedge.meta;

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
import test.chswitchedge.Sub2;
import test.chswitchedge.validation.Sub2TypeFormatValidator;
import test.chswitchedge.validation.Sub2Validator;
import test.chswitchedge.validation.exists.Sub2OnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Sub2.class)
public class Sub2Meta implements RosettaMetaData<Sub2> {

	@Override
	public List<Validator<? super Sub2>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Sub2, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Sub2> validator(ValidatorFactory factory) {
		return factory.<Sub2>create(Sub2Validator.class);
	}

	@Override
	public Validator<? super Sub2> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Sub2>create(Sub2TypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Sub2> validator() {
		return new Sub2Validator();
	}

	@Deprecated
	@Override
	public Validator<? super Sub2> typeFormatValidator() {
		return new Sub2TypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Sub2, Set<String>> onlyExistsValidator() {
		return new Sub2OnlyExistsValidator();
	}
}
