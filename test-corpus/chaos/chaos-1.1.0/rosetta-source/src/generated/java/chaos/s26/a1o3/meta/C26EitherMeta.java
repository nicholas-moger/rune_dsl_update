package chaos.s26.a1o3.meta;

import chaos.s26.a1o3.C26Either;
import chaos.s26.a1o3.validation.C26EitherTypeFormatValidator;
import chaos.s26.a1o3.validation.C26EitherValidator;
import chaos.s26.a1o3.validation.datarule.C26EitherChoice;
import chaos.s26.a1o3.validation.exists.C26EitherOnlyExistsValidator;
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
@RosettaMeta(model=C26Either.class)
public class C26EitherMeta implements RosettaMetaData<C26Either> {

	@Override
	public List<Validator<? super C26Either>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C26Either>create(C26EitherChoice.class)
		);
	}
	
	@Override
	public List<Function<? super C26Either, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C26Either> validator(ValidatorFactory factory) {
		return factory.<C26Either>create(C26EitherValidator.class);
	}

	@Override
	public Validator<? super C26Either> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C26Either>create(C26EitherTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C26Either> validator() {
		return new C26EitherValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C26Either> typeFormatValidator() {
		return new C26EitherTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C26Either, Set<String>> onlyExistsValidator() {
		return new C26EitherOnlyExistsValidator();
	}
}
