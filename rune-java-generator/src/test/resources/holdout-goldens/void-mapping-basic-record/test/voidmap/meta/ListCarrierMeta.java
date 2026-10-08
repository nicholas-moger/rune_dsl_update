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
import test.voidmap.ListCarrier;
import test.voidmap.validation.ListCarrierTypeFormatValidator;
import test.voidmap.validation.ListCarrierValidator;
import test.voidmap.validation.exists.ListCarrierOnlyExistsValidator;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=ListCarrier.class)
public class ListCarrierMeta implements RosettaMetaData<ListCarrier> {

	@Override
	public List<Validator<? super ListCarrier>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super ListCarrier, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super ListCarrier> validator(ValidatorFactory factory) {
		return factory.<ListCarrier>create(ListCarrierValidator.class);
	}

	@Override
	public Validator<? super ListCarrier> typeFormatValidator(ValidatorFactory factory) {
		return factory.<ListCarrier>create(ListCarrierTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super ListCarrier> validator() {
		return new ListCarrierValidator();
	}

	@Deprecated
	@Override
	public Validator<? super ListCarrier> typeFormatValidator() {
		return new ListCarrierTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super ListCarrier, Set<String>> onlyExistsValidator() {
		return new ListCarrierOnlyExistsValidator();
	}
}
