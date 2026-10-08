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
import test.chswitchedge.Either;
import test.chswitchedge.validation.EitherTypeFormatValidator;
import test.chswitchedge.validation.EitherValidator;
import test.chswitchedge.validation.datarule.EitherChoice;
import test.chswitchedge.validation.exists.EitherOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Either.class)
public class EitherMeta implements RosettaMetaData<Either> {

	@Override
	public List<Validator<? super Either>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<Either>create(EitherChoice.class)
		);
	}
	
	@Override
	public List<Function<? super Either, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Either> validator(ValidatorFactory factory) {
		return factory.<Either>create(EitherValidator.class);
	}

	@Override
	public Validator<? super Either> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Either>create(EitherTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Either> validator() {
		return new EitherValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Either> typeFormatValidator() {
		return new EitherTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Either, Set<String>> onlyExistsValidator() {
		return new EitherOnlyExistsValidator();
	}
}
