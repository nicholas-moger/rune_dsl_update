package test.singletolistset.meta;

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
import test.singletolistset.NumberList;
import test.singletolistset.validation.NumberListTypeFormatValidator;
import test.singletolistset.validation.NumberListValidator;
import test.singletolistset.validation.exists.NumberListOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=NumberList.class)
public class NumberListMeta implements RosettaMetaData<NumberList> {

	@Override
	public List<Validator<? super NumberList>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super NumberList, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super NumberList> validator(ValidatorFactory factory) {
		return factory.<NumberList>create(NumberListValidator.class);
	}

	@Override
	public Validator<? super NumberList> typeFormatValidator(ValidatorFactory factory) {
		return factory.<NumberList>create(NumberListTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super NumberList> validator() {
		return new NumberListValidator();
	}

	@Deprecated
	@Override
	public Validator<? super NumberList> typeFormatValidator() {
		return new NumberListTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super NumberList, Set<String>> onlyExistsValidator() {
		return new NumberListOnlyExistsValidator();
	}
}
