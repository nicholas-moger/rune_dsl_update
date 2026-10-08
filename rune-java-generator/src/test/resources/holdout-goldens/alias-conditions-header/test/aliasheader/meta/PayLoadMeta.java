package test.aliasheader.meta;

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
import test.aliasheader.PayLoad;
import test.aliasheader.validation.PayLoadTypeFormatValidator;
import test.aliasheader.validation.PayLoadValidator;
import test.aliasheader.validation.exists.PayLoadOnlyExistsValidator;


/**
 * @version 0.0.0.test
 */
@RosettaMeta(model=PayLoad.class)
public class PayLoadMeta implements RosettaMetaData<PayLoad> {

	@Override
	public List<Validator<? super PayLoad>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super PayLoad, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super PayLoad> validator(ValidatorFactory factory) {
		return factory.<PayLoad>create(PayLoadValidator.class);
	}

	@Override
	public Validator<? super PayLoad> typeFormatValidator(ValidatorFactory factory) {
		return factory.<PayLoad>create(PayLoadTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super PayLoad> validator() {
		return new PayLoadValidator();
	}

	@Deprecated
	@Override
	public Validator<? super PayLoad> typeFormatValidator() {
		return new PayLoadTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super PayLoad, Set<String>> onlyExistsValidator() {
		return new PayLoadOnlyExistsValidator();
	}
}
