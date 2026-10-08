package test.aliasscope.meta;

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
import test.aliasscope.SingleNames;
import test.aliasscope.validation.SingleNamesTypeFormatValidator;
import test.aliasscope.validation.SingleNamesValidator;
import test.aliasscope.validation.exists.SingleNamesOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=SingleNames.class)
public class SingleNamesMeta implements RosettaMetaData<SingleNames> {

	@Override
	public List<Validator<? super SingleNames>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super SingleNames, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super SingleNames> validator(ValidatorFactory factory) {
		return factory.<SingleNames>create(SingleNamesValidator.class);
	}

	@Override
	public Validator<? super SingleNames> typeFormatValidator(ValidatorFactory factory) {
		return factory.<SingleNames>create(SingleNamesTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super SingleNames> validator() {
		return new SingleNamesValidator();
	}

	@Deprecated
	@Override
	public Validator<? super SingleNames> typeFormatValidator() {
		return new SingleNamesTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super SingleNames, Set<String>> onlyExistsValidator() {
		return new SingleNamesOnlyExistsValidator();
	}
}
