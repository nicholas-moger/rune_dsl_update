package test.deeppathedge.meta;

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
import test.deeppathedge.Wrap;
import test.deeppathedge.validation.WrapTypeFormatValidator;
import test.deeppathedge.validation.WrapValidator;
import test.deeppathedge.validation.exists.WrapOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Wrap.class)
public class WrapMeta implements RosettaMetaData<Wrap> {

	@Override
	public List<Validator<? super Wrap>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Wrap, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Wrap> validator(ValidatorFactory factory) {
		return factory.<Wrap>create(WrapValidator.class);
	}

	@Override
	public Validator<? super Wrap> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Wrap>create(WrapTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Wrap> validator() {
		return new WrapValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Wrap> typeFormatValidator() {
		return new WrapTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Wrap, Set<String>> onlyExistsValidator() {
		return new WrapOnlyExistsValidator();
	}
}
