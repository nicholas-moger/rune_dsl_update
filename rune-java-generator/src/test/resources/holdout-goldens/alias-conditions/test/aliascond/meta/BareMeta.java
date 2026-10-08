package test.aliascond.meta;

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
import test.aliascond.Bare;
import test.aliascond.validation.BareTypeFormatValidator;
import test.aliascond.validation.BareValidator;
import test.aliascond.validation.exists.BareOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Bare.class)
public class BareMeta implements RosettaMetaData<Bare> {

	@Override
	public List<Validator<? super Bare>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Bare, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Bare> validator(ValidatorFactory factory) {
		return factory.<Bare>create(BareValidator.class);
	}

	@Override
	public Validator<? super Bare> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Bare>create(BareTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Bare> validator() {
		return new BareValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Bare> typeFormatValidator() {
		return new BareTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Bare, Set<String>> onlyExistsValidator() {
		return new BareOnlyExistsValidator();
	}
}
