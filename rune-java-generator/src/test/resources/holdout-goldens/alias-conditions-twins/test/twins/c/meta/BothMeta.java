package test.twins.c.meta;

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
import test.twins.c.Both;
import test.twins.c.validation.BothTypeFormatValidator;
import test.twins.c.validation.BothValidator;
import test.twins.c.validation.exists.BothOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Both.class)
public class BothMeta implements RosettaMetaData<Both> {

	@Override
	public List<Validator<? super Both>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Both, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Both> validator(ValidatorFactory factory) {
		return factory.<Both>create(BothValidator.class);
	}

	@Override
	public Validator<? super Both> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Both>create(BothTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Both> validator() {
		return new BothValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Both> typeFormatValidator() {
		return new BothTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Both, Set<String>> onlyExistsValidator() {
		return new BothOnlyExistsValidator();
	}
}
