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
import test.deeppathedge.Deep;
import test.deeppathedge.validation.DeepTypeFormatValidator;
import test.deeppathedge.validation.DeepValidator;
import test.deeppathedge.validation.datarule.DeepLambdaNav;
import test.deeppathedge.validation.exists.DeepOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Deep.class)
public class DeepMeta implements RosettaMetaData<Deep> {

	@Override
	public List<Validator<? super Deep>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<Deep>create(DeepLambdaNav.class)
		);
	}
	
	@Override
	public List<Function<? super Deep, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Deep> validator(ValidatorFactory factory) {
		return factory.<Deep>create(DeepValidator.class);
	}

	@Override
	public Validator<? super Deep> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Deep>create(DeepTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Deep> validator() {
		return new DeepValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Deep> typeFormatValidator() {
		return new DeepTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Deep, Set<String>> onlyExistsValidator() {
		return new DeepOnlyExistsValidator();
	}
}
