package test.voidrender.meta;

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
import test.voidrender.Carrier;
import test.voidrender.validation.CarrierTypeFormatValidator;
import test.voidrender.validation.CarrierValidator;
import test.voidrender.validation.datarule.CarrierTokAbsent;
import test.voidrender.validation.datarule.CarrierToksExist;
import test.voidrender.validation.exists.CarrierOnlyExistsValidator;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=Carrier.class)
public class CarrierMeta implements RosettaMetaData<Carrier> {

	@Override
	public List<Validator<? super Carrier>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<Carrier>create(CarrierTokAbsent.class),
			factory.<Carrier>create(CarrierToksExist.class)
		);
	}
	
	@Override
	public List<Function<? super Carrier, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Carrier> validator(ValidatorFactory factory) {
		return factory.<Carrier>create(CarrierValidator.class);
	}

	@Override
	public Validator<? super Carrier> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Carrier>create(CarrierTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Carrier> validator() {
		return new CarrierValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Carrier> typeFormatValidator() {
		return new CarrierTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Carrier, Set<String>> onlyExistsValidator() {
		return new CarrierOnlyExistsValidator();
	}
}
