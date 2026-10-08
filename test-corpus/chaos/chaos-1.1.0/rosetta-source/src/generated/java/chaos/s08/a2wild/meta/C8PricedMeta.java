package chaos.s08.a2wild.meta;

import chaos.s08.a2wild.C8Priced;
import chaos.s08.a2wild.validation.C8PricedTypeFormatValidator;
import chaos.s08.a2wild.validation.C8PricedValidator;
import chaos.s08.a2wild.validation.datarule.C8PricedC8WeightCap;
import chaos.s08.a2wild.validation.exists.C8PricedOnlyExistsValidator;
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


/**
 * @version 1.0.0
 */
@RosettaMeta(model=C8Priced.class)
public class C8PricedMeta implements RosettaMetaData<C8Priced> {

	@Override
	public List<Validator<? super C8Priced>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C8Priced>create(C8PricedC8WeightCap.class)
		);
	}
	
	@Override
	public List<Function<? super C8Priced, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C8Priced> validator(ValidatorFactory factory) {
		return factory.<C8Priced>create(C8PricedValidator.class);
	}

	@Override
	public Validator<? super C8Priced> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C8Priced>create(C8PricedTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C8Priced> validator() {
		return new C8PricedValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C8Priced> typeFormatValidator() {
		return new C8PricedTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C8Priced, Set<String>> onlyExistsValidator() {
		return new C8PricedOnlyExistsValidator();
	}
}
