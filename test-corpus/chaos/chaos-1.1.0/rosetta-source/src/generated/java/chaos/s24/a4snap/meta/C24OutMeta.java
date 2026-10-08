package chaos.s24.a4snap.meta;

import chaos.s24.a4snap.C24Out;
import chaos.s24.a4snap.validation.C24OutTypeFormatValidator;
import chaos.s24.a4snap.validation.C24OutValidator;
import chaos.s24.a4snap.validation.exists.C24OutOnlyExistsValidator;
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
 * @version 1.0.0-SNAPSHOT
 */
@RosettaMeta(model=C24Out.class)
public class C24OutMeta implements RosettaMetaData<C24Out> {

	@Override
	public List<Validator<? super C24Out>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C24Out, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C24Out> validator(ValidatorFactory factory) {
		return factory.<C24Out>create(C24OutValidator.class);
	}

	@Override
	public Validator<? super C24Out> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C24Out>create(C24OutTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C24Out> validator() {
		return new C24OutValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C24Out> typeFormatValidator() {
		return new C24OutTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C24Out, Set<String>> onlyExistsValidator() {
		return new C24OutOnlyExistsValidator();
	}
}
