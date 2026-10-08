package test.voidmap.meta;

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
import test.voidmap.RequiredCarrier;
import test.voidmap.validation.RequiredCarrierTypeFormatValidator;
import test.voidmap.validation.RequiredCarrierValidator;
import test.voidmap.validation.exists.RequiredCarrierOnlyExistsValidator;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=RequiredCarrier.class)
public class RequiredCarrierMeta implements RosettaMetaData<RequiredCarrier> {

	@Override
	public List<Validator<? super RequiredCarrier>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RequiredCarrier, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RequiredCarrier> validator(ValidatorFactory factory) {
		return factory.<RequiredCarrier>create(RequiredCarrierValidator.class);
	}

	@Override
	public Validator<? super RequiredCarrier> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RequiredCarrier>create(RequiredCarrierTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RequiredCarrier> validator() {
		return new RequiredCarrierValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RequiredCarrier> typeFormatValidator() {
		return new RequiredCarrierTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RequiredCarrier, Set<String>> onlyExistsValidator() {
		return new RequiredCarrierOnlyExistsValidator();
	}
}
