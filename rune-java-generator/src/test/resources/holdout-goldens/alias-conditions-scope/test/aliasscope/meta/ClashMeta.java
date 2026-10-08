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
import test.aliasscope.Clash;
import test.aliasscope.validation.ClashTypeFormatValidator;
import test.aliasscope.validation.ClashValidator;
import test.aliasscope.validation.exists.ClashOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Clash.class)
public class ClashMeta implements RosettaMetaData<Clash> {

	@Override
	public List<Validator<? super Clash>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Clash, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Clash> validator(ValidatorFactory factory) {
		return factory.<Clash>create(ClashValidator.class);
	}

	@Override
	public Validator<? super Clash> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Clash>create(ClashTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Clash> validator() {
		return new ClashValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Clash> typeFormatValidator() {
		return new ClashTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Clash, Set<String>> onlyExistsValidator() {
		return new ClashOnlyExistsValidator();
	}
}
