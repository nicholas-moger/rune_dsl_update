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
import test.aliasscope.Loop;
import test.aliasscope.validation.LoopTypeFormatValidator;
import test.aliasscope.validation.LoopValidator;
import test.aliasscope.validation.exists.LoopOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Loop.class)
public class LoopMeta implements RosettaMetaData<Loop> {

	@Override
	public List<Validator<? super Loop>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Loop, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Loop> validator(ValidatorFactory factory) {
		return factory.<Loop>create(LoopValidator.class);
	}

	@Override
	public Validator<? super Loop> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Loop>create(LoopTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Loop> validator() {
		return new LoopValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Loop> typeFormatValidator() {
		return new LoopTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Loop, Set<String>> onlyExistsValidator() {
		return new LoopOnlyExistsValidator();
	}
}
