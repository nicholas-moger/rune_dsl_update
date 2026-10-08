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
import test.deeppathedge.Outer;
import test.deeppathedge.validation.OuterTypeFormatValidator;
import test.deeppathedge.validation.OuterValidator;
import test.deeppathedge.validation.datarule.OuterChoice;
import test.deeppathedge.validation.exists.OuterOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Outer.class)
public class OuterMeta implements RosettaMetaData<Outer> {

	@Override
	public List<Validator<? super Outer>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<Outer>create(OuterChoice.class)
		);
	}
	
	@Override
	public List<Function<? super Outer, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Outer> validator(ValidatorFactory factory) {
		return factory.<Outer>create(OuterValidator.class);
	}

	@Override
	public Validator<? super Outer> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Outer>create(OuterTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Outer> validator() {
		return new OuterValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Outer> typeFormatValidator() {
		return new OuterTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Outer, Set<String>> onlyExistsValidator() {
		return new OuterOnlyExistsValidator();
	}
}
