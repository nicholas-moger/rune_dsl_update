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
import test.chswitchedge.OptA;
import test.chswitchedge.validation.OptATypeFormatValidator;
import test.chswitchedge.validation.OptAValidator;
import test.chswitchedge.validation.exists.OptAOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=OptA.class)
public class OptAMeta implements RosettaMetaData<OptA> {

	@Override
	public List<Validator<? super OptA>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super OptA, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super OptA> validator(ValidatorFactory factory) {
		return factory.<OptA>create(OptAValidator.class);
	}

	@Override
	public Validator<? super OptA> typeFormatValidator(ValidatorFactory factory) {
		return factory.<OptA>create(OptATypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super OptA> validator() {
		return new OptAValidator();
	}

	@Deprecated
	@Override
	public Validator<? super OptA> typeFormatValidator() {
		return new OptATypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super OptA, Set<String>> onlyExistsValidator() {
		return new OptAOnlyExistsValidator();
	}
}
