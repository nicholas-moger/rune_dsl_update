package test.chswitchbare.meta;

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
import test.chswitchbare.Outer2;
import test.chswitchbare.validation.Outer2TypeFormatValidator;
import test.chswitchbare.validation.Outer2Validator;
import test.chswitchbare.validation.datarule.Outer2Choice;
import test.chswitchbare.validation.exists.Outer2OnlyExistsValidator;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=Outer2.class)
public class Outer2Meta implements RosettaMetaData<Outer2> {

	@Override
	public List<Validator<? super Outer2>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<Outer2>create(Outer2Choice.class)
		);
	}
	
	@Override
	public List<Function<? super Outer2, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Outer2> validator(ValidatorFactory factory) {
		return factory.<Outer2>create(Outer2Validator.class);
	}

	@Override
	public Validator<? super Outer2> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Outer2>create(Outer2TypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Outer2> validator() {
		return new Outer2Validator();
	}

	@Deprecated
	@Override
	public Validator<? super Outer2> typeFormatValidator() {
		return new Outer2TypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Outer2, Set<String>> onlyExistsValidator() {
		return new Outer2OnlyExistsValidator();
	}
}
