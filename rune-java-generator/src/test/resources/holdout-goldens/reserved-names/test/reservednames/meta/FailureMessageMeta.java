package test.reservednames.meta;

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
import test.reservednames.FailureMessage;
import test.reservednames.validation.FailureMessageTypeFormatValidator;
import test.reservednames.validation.FailureMessageValidator;
import test.reservednames.validation.datarule.FailureMessageTextExists;
import test.reservednames.validation.exists.FailureMessageOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=FailureMessage.class)
public class FailureMessageMeta implements RosettaMetaData<FailureMessage> {

	@Override
	public List<Validator<? super FailureMessage>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<FailureMessage>create(FailureMessageTextExists.class)
		);
	}
	
	@Override
	public List<Function<? super FailureMessage, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super FailureMessage> validator(ValidatorFactory factory) {
		return factory.<FailureMessage>create(FailureMessageValidator.class);
	}

	@Override
	public Validator<? super FailureMessage> typeFormatValidator(ValidatorFactory factory) {
		return factory.<FailureMessage>create(FailureMessageTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super FailureMessage> validator() {
		return new FailureMessageValidator();
	}

	@Deprecated
	@Override
	public Validator<? super FailureMessage> typeFormatValidator() {
		return new FailureMessageTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super FailureMessage, Set<String>> onlyExistsValidator() {
		return new FailureMessageOnlyExistsValidator();
	}
}
