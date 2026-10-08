package chaos.s15.a2wild.meta;

import chaos.s15.a2wild.C15Carrier;
import chaos.s15.a2wild.validation.C15CarrierTypeFormatValidator;
import chaos.s15.a2wild.validation.C15CarrierValidator;
import chaos.s15.a2wild.validation.exists.C15CarrierOnlyExistsValidator;
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
@RosettaMeta(model=C15Carrier.class)
public class C15CarrierMeta implements RosettaMetaData<C15Carrier> {

	@Override
	public List<Validator<? super C15Carrier>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C15Carrier, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C15Carrier> validator(ValidatorFactory factory) {
		return factory.<C15Carrier>create(C15CarrierValidator.class);
	}

	@Override
	public Validator<? super C15Carrier> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C15Carrier>create(C15CarrierTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C15Carrier> validator() {
		return new C15CarrierValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C15Carrier> typeFormatValidator() {
		return new C15CarrierTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C15Carrier, Set<String>> onlyExistsValidator() {
		return new C15CarrierOnlyExistsValidator();
	}
}
