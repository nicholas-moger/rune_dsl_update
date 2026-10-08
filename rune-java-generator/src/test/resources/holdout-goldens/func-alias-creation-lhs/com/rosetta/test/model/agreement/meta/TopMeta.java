package com.rosetta.test.model.agreement.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import com.rosetta.test.model.agreement.Top;
import com.rosetta.test.model.agreement.validation.TopTypeFormatValidator;
import com.rosetta.test.model.agreement.validation.TopValidator;
import com.rosetta.test.model.agreement.validation.exists.TopOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version test
 */
@RosettaMeta(model=Top.class)
public class TopMeta implements RosettaMetaData<Top> {

	@Override
	public List<Validator<? super Top>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Top, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Top> validator(ValidatorFactory factory) {
		return factory.<Top>create(TopValidator.class);
	}

	@Override
	public Validator<? super Top> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Top>create(TopTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Top> validator() {
		return new TopValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Top> typeFormatValidator() {
		return new TopTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Top, Set<String>> onlyExistsValidator() {
		return new TopOnlyExistsValidator();
	}
}
