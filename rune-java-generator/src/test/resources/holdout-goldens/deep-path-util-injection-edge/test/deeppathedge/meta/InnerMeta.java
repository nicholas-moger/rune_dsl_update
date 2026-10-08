package test.deeppathedge.meta;

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
import test.deeppathedge.Inner;
import test.deeppathedge.validation.InnerTypeFormatValidator;
import test.deeppathedge.validation.InnerValidator;
import test.deeppathedge.validation.datarule.InnerChoice;
import test.deeppathedge.validation.exists.InnerOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Inner.class)
public class InnerMeta implements RosettaMetaData<Inner> {

	@Override
	public List<Validator<? super Inner>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<Inner>create(InnerChoice.class)
		);
	}
	
	@Override
	public List<Function<? super Inner, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Inner> validator(ValidatorFactory factory) {
		return factory.<Inner>create(InnerValidator.class);
	}

	@Override
	public Validator<? super Inner> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Inner>create(InnerTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Inner> validator() {
		return new InnerValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Inner> typeFormatValidator() {
		return new InnerTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Inner, Set<String>> onlyExistsValidator() {
		return new InnerOnlyExistsValidator();
	}
}
