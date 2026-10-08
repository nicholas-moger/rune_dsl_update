package test.deeppathedgetypes.meta;

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
import test.deeppathedgetypes.ROuter;
import test.deeppathedgetypes.validation.ROuterTypeFormatValidator;
import test.deeppathedgetypes.validation.ROuterValidator;
import test.deeppathedgetypes.validation.datarule.ROuterChoice;
import test.deeppathedgetypes.validation.exists.ROuterOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=ROuter.class)
public class ROuterMeta implements RosettaMetaData<ROuter> {

	@Override
	public List<Validator<? super ROuter>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<ROuter>create(ROuterChoice.class)
		);
	}
	
	@Override
	public List<Function<? super ROuter, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super ROuter> validator(ValidatorFactory factory) {
		return factory.<ROuter>create(ROuterValidator.class);
	}

	@Override
	public Validator<? super ROuter> typeFormatValidator(ValidatorFactory factory) {
		return factory.<ROuter>create(ROuterTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super ROuter> validator() {
		return new ROuterValidator();
	}

	@Deprecated
	@Override
	public Validator<? super ROuter> typeFormatValidator() {
		return new ROuterTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super ROuter, Set<String>> onlyExistsValidator() {
		return new ROuterOnlyExistsValidator();
	}
}
