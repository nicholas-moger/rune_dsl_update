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
import test.voidmapedge.AliasCarrier;
import test.voidmapedge.validation.AliasCarrierTypeFormatValidator;
import test.voidmapedge.validation.AliasCarrierValidator;
import test.voidmapedge.validation.exists.AliasCarrierOnlyExistsValidator;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=AliasCarrier.class)
public class AliasCarrierMeta implements RosettaMetaData<AliasCarrier> {

	@Override
	public List<Validator<? super AliasCarrier>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super AliasCarrier, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super AliasCarrier> validator(ValidatorFactory factory) {
		return factory.<AliasCarrier>create(AliasCarrierValidator.class);
	}

	@Override
	public Validator<? super AliasCarrier> typeFormatValidator(ValidatorFactory factory) {
		return factory.<AliasCarrier>create(AliasCarrierTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super AliasCarrier> validator() {
		return new AliasCarrierValidator();
	}

	@Deprecated
	@Override
	public Validator<? super AliasCarrier> typeFormatValidator() {
		return new AliasCarrierTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super AliasCarrier, Set<String>> onlyExistsValidator() {
		return new AliasCarrierOnlyExistsValidator();
	}
}
