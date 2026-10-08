package test.chswitchedge.meta;

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
import test.chswitchedge.Base;
import test.chswitchedge.validation.BaseTypeFormatValidator;
import test.chswitchedge.validation.BaseValidator;
import test.chswitchedge.validation.exists.BaseOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Base.class)
public class BaseMeta implements RosettaMetaData<Base> {

	@Override
	public List<Validator<? super Base>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Base, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Base> validator(ValidatorFactory factory) {
		return factory.<Base>create(BaseValidator.class);
	}

	@Override
	public Validator<? super Base> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Base>create(BaseTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Base> validator() {
		return new BaseValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Base> typeFormatValidator() {
		return new BaseTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Base, Set<String>> onlyExistsValidator() {
		return new BaseOnlyExistsValidator();
	}
}
