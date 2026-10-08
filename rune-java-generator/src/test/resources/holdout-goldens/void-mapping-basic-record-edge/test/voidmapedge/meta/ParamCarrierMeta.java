package test.voidmapedge.meta;

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
import test.voidmapedge.ParamCarrier;
import test.voidmapedge.validation.ParamCarrierTypeFormatValidator;
import test.voidmapedge.validation.ParamCarrierValidator;
import test.voidmapedge.validation.exists.ParamCarrierOnlyExistsValidator;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=ParamCarrier.class)
public class ParamCarrierMeta implements RosettaMetaData<ParamCarrier> {

	@Override
	public List<Validator<? super ParamCarrier>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super ParamCarrier, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super ParamCarrier> validator(ValidatorFactory factory) {
		return factory.<ParamCarrier>create(ParamCarrierValidator.class);
	}

	@Override
	public Validator<? super ParamCarrier> typeFormatValidator(ValidatorFactory factory) {
		return factory.<ParamCarrier>create(ParamCarrierTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super ParamCarrier> validator() {
		return new ParamCarrierValidator();
	}

	@Deprecated
	@Override
	public Validator<? super ParamCarrier> typeFormatValidator() {
		return new ParamCarrierTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super ParamCarrier, Set<String>> onlyExistsValidator() {
		return new ParamCarrierOnlyExistsValidator();
	}
}
