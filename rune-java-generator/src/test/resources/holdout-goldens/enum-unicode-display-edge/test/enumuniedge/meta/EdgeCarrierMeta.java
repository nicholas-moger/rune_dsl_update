package test.enumuniedge.meta;

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
import test.enumuniedge.EdgeCarrier;
import test.enumuniedge.validation.EdgeCarrierTypeFormatValidator;
import test.enumuniedge.validation.EdgeCarrierValidator;
import test.enumuniedge.validation.exists.EdgeCarrierOnlyExistsValidator;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=EdgeCarrier.class)
public class EdgeCarrierMeta implements RosettaMetaData<EdgeCarrier> {

	@Override
	public List<Validator<? super EdgeCarrier>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super EdgeCarrier, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super EdgeCarrier> validator(ValidatorFactory factory) {
		return factory.<EdgeCarrier>create(EdgeCarrierValidator.class);
	}

	@Override
	public Validator<? super EdgeCarrier> typeFormatValidator(ValidatorFactory factory) {
		return factory.<EdgeCarrier>create(EdgeCarrierTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super EdgeCarrier> validator() {
		return new EdgeCarrierValidator();
	}

	@Deprecated
	@Override
	public Validator<? super EdgeCarrier> typeFormatValidator() {
		return new EdgeCarrierTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super EdgeCarrier, Set<String>> onlyExistsValidator() {
		return new EdgeCarrierOnlyExistsValidator();
	}
}
