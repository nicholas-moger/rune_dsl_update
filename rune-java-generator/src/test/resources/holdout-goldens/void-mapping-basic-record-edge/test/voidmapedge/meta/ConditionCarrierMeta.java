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
import test.voidmapedge.ConditionCarrier;
import test.voidmapedge.validation.ConditionCarrierTypeFormatValidator;
import test.voidmapedge.validation.ConditionCarrierValidator;
import test.voidmapedge.validation.datarule.ConditionCarrierTokPresent;
import test.voidmapedge.validation.exists.ConditionCarrierOnlyExistsValidator;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=ConditionCarrier.class)
public class ConditionCarrierMeta implements RosettaMetaData<ConditionCarrier> {

	@Override
	public List<Validator<? super ConditionCarrier>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<ConditionCarrier>create(ConditionCarrierTokPresent.class)
		);
	}
	
	@Override
	public List<Function<? super ConditionCarrier, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super ConditionCarrier> validator(ValidatorFactory factory) {
		return factory.<ConditionCarrier>create(ConditionCarrierValidator.class);
	}

	@Override
	public Validator<? super ConditionCarrier> typeFormatValidator(ValidatorFactory factory) {
		return factory.<ConditionCarrier>create(ConditionCarrierTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super ConditionCarrier> validator() {
		return new ConditionCarrierValidator();
	}

	@Deprecated
	@Override
	public Validator<? super ConditionCarrier> typeFormatValidator() {
		return new ConditionCarrierTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super ConditionCarrier, Set<String>> onlyExistsValidator() {
		return new ConditionCarrierOnlyExistsValidator();
	}
}
