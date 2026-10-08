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
import test.voidmapedge.MetaCarrier;
import test.voidmapedge.validation.MetaCarrierTypeFormatValidator;
import test.voidmapedge.validation.MetaCarrierValidator;
import test.voidmapedge.validation.datarule.MetaCarrierTokPresent;
import test.voidmapedge.validation.exists.MetaCarrierOnlyExistsValidator;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=MetaCarrier.class)
public class MetaCarrierMeta implements RosettaMetaData<MetaCarrier> {

	@Override
	public List<Validator<? super MetaCarrier>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<MetaCarrier>create(MetaCarrierTokPresent.class)
		);
	}
	
	@Override
	public List<Function<? super MetaCarrier, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super MetaCarrier> validator(ValidatorFactory factory) {
		return factory.<MetaCarrier>create(MetaCarrierValidator.class);
	}

	@Override
	public Validator<? super MetaCarrier> typeFormatValidator(ValidatorFactory factory) {
		return factory.<MetaCarrier>create(MetaCarrierTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super MetaCarrier> validator() {
		return new MetaCarrierValidator();
	}

	@Deprecated
	@Override
	public Validator<? super MetaCarrier> typeFormatValidator() {
		return new MetaCarrierTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super MetaCarrier, Set<String>> onlyExistsValidator() {
		return new MetaCarrierOnlyExistsValidator();
	}
}
