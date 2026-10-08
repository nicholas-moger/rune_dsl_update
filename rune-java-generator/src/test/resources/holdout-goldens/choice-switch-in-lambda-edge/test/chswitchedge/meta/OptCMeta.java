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
import test.chswitchedge.OptC;
import test.chswitchedge.validation.OptCTypeFormatValidator;
import test.chswitchedge.validation.OptCValidator;
import test.chswitchedge.validation.exists.OptCOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=OptC.class)
public class OptCMeta implements RosettaMetaData<OptC> {

	@Override
	public List<Validator<? super OptC>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super OptC, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super OptC> validator(ValidatorFactory factory) {
		return factory.<OptC>create(OptCValidator.class);
	}

	@Override
	public Validator<? super OptC> typeFormatValidator(ValidatorFactory factory) {
		return factory.<OptC>create(OptCTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super OptC> validator() {
		return new OptCValidator();
	}

	@Deprecated
	@Override
	public Validator<? super OptC> typeFormatValidator() {
		return new OptCTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super OptC, Set<String>> onlyExistsValidator() {
		return new OptCOnlyExistsValidator();
	}
}
