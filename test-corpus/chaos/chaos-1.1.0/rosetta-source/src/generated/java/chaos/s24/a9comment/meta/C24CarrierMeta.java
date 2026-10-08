package chaos.s24.a9comment.meta;

import chaos.s24.a9comment.C24Carrier;
import chaos.s24.a9comment.validation.C24CarrierTypeFormatValidator;
import chaos.s24.a9comment.validation.C24CarrierValidator;
import chaos.s24.a9comment.validation.exists.C24CarrierOnlyExistsValidator;
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
@RosettaMeta(model=C24Carrier.class)
public class C24CarrierMeta implements RosettaMetaData<C24Carrier> {

	@Override
	public List<Validator<? super C24Carrier>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C24Carrier, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C24Carrier> validator(ValidatorFactory factory) {
		return factory.<C24Carrier>create(C24CarrierValidator.class);
	}

	@Override
	public Validator<? super C24Carrier> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C24Carrier>create(C24CarrierTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C24Carrier> validator() {
		return new C24CarrierValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C24Carrier> typeFormatValidator() {
		return new C24CarrierTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C24Carrier, Set<String>> onlyExistsValidator() {
		return new C24CarrierOnlyExistsValidator();
	}
}
