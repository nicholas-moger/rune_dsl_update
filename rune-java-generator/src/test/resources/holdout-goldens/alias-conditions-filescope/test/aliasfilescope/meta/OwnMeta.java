package test.aliasfilescope.meta;

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
import test.aliasfilescope.Own;
import test.aliasfilescope.validation.OwnTypeFormatValidator;
import test.aliasfilescope.validation.OwnValidator;
import test.aliasfilescope.validation.exists.OwnOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Own.class)
public class OwnMeta implements RosettaMetaData<Own> {

	@Override
	public List<Validator<? super Own>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Own, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Own> validator(ValidatorFactory factory) {
		return factory.<Own>create(OwnValidator.class);
	}

	@Override
	public Validator<? super Own> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Own>create(OwnTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Own> validator() {
		return new OwnValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Own> typeFormatValidator() {
		return new OwnTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Own, Set<String>> onlyExistsValidator() {
		return new OwnOnlyExistsValidator();
	}
}
