package chaos.s18.base.meta;

import chaos.s18.base.C18Either;
import chaos.s18.base.validation.C18EitherTypeFormatValidator;
import chaos.s18.base.validation.C18EitherValidator;
import chaos.s18.base.validation.datarule.C18EitherChoice;
import chaos.s18.base.validation.exists.C18EitherOnlyExistsValidator;
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
@RosettaMeta(model=C18Either.class)
public class C18EitherMeta implements RosettaMetaData<C18Either> {

	@Override
	public List<Validator<? super C18Either>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C18Either>create(C18EitherChoice.class)
		);
	}
	
	@Override
	public List<Function<? super C18Either, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C18Either> validator(ValidatorFactory factory) {
		return factory.<C18Either>create(C18EitherValidator.class);
	}

	@Override
	public Validator<? super C18Either> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C18Either>create(C18EitherTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C18Either> validator() {
		return new C18EitherValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C18Either> typeFormatValidator() {
		return new C18EitherTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C18Either, Set<String>> onlyExistsValidator() {
		return new C18EitherOnlyExistsValidator();
	}
}
