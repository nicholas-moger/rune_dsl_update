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
import test.chswitchedge.Bag;
import test.chswitchedge.validation.BagTypeFormatValidator;
import test.chswitchedge.validation.BagValidator;
import test.chswitchedge.validation.datarule.BagTexts;
import test.chswitchedge.validation.exists.BagOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Bag.class)
public class BagMeta implements RosettaMetaData<Bag> {

	@Override
	public List<Validator<? super Bag>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<Bag>create(BagTexts.class)
		);
	}
	
	@Override
	public List<Function<? super Bag, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Bag> validator(ValidatorFactory factory) {
		return factory.<Bag>create(BagValidator.class);
	}

	@Override
	public Validator<? super Bag> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Bag>create(BagTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Bag> validator() {
		return new BagValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Bag> typeFormatValidator() {
		return new BagTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Bag, Set<String>> onlyExistsValidator() {
		return new BagOnlyExistsValidator();
	}
}
