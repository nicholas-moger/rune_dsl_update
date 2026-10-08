package chaos.s03.a5crlf.meta;

import chaos.s03.a5crlf.C3Inner;
import chaos.s03.a5crlf.validation.C3InnerTypeFormatValidator;
import chaos.s03.a5crlf.validation.C3InnerValidator;
import chaos.s03.a5crlf.validation.datarule.C3InnerChoice;
import chaos.s03.a5crlf.validation.exists.C3InnerOnlyExistsValidator;
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
@RosettaMeta(model=C3Inner.class)
public class C3InnerMeta implements RosettaMetaData<C3Inner> {

	@Override
	public List<Validator<? super C3Inner>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C3Inner>create(C3InnerChoice.class)
		);
	}
	
	@Override
	public List<Function<? super C3Inner, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C3Inner> validator(ValidatorFactory factory) {
		return factory.<C3Inner>create(C3InnerValidator.class);
	}

	@Override
	public Validator<? super C3Inner> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C3Inner>create(C3InnerTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C3Inner> validator() {
		return new C3InnerValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C3Inner> typeFormatValidator() {
		return new C3InnerTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C3Inner, Set<String>> onlyExistsValidator() {
		return new C3InnerOnlyExistsValidator();
	}
}
