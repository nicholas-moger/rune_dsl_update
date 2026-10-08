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
import test.deeppathedgetypes.RWrap;
import test.deeppathedgetypes.validation.RWrapTypeFormatValidator;
import test.deeppathedgetypes.validation.RWrapValidator;
import test.deeppathedgetypes.validation.exists.RWrapOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RWrap.class)
public class RWrapMeta implements RosettaMetaData<RWrap> {

	@Override
	public List<Validator<? super RWrap>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RWrap, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RWrap> validator(ValidatorFactory factory) {
		return factory.<RWrap>create(RWrapValidator.class);
	}

	@Override
	public Validator<? super RWrap> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RWrap>create(RWrapTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RWrap> validator() {
		return new RWrapValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RWrap> typeFormatValidator() {
		return new RWrapTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RWrap, Set<String>> onlyExistsValidator() {
		return new RWrapOnlyExistsValidator();
	}
}
