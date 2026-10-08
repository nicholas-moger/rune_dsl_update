package chaos.s03.a2alias.meta;

import chaos.s03.a2alias.C3Wrap;
import chaos.s03.a2alias.validation.C3WrapTypeFormatValidator;
import chaos.s03.a2alias.validation.C3WrapValidator;
import chaos.s03.a2alias.validation.exists.C3WrapOnlyExistsValidator;
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
@RosettaMeta(model=C3Wrap.class)
public class C3WrapMeta implements RosettaMetaData<C3Wrap> {

	@Override
	public List<Validator<? super C3Wrap>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C3Wrap, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C3Wrap> validator(ValidatorFactory factory) {
		return factory.<C3Wrap>create(C3WrapValidator.class);
	}

	@Override
	public Validator<? super C3Wrap> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C3Wrap>create(C3WrapTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C3Wrap> validator() {
		return new C3WrapValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C3Wrap> typeFormatValidator() {
		return new C3WrapTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C3Wrap, Set<String>> onlyExistsValidator() {
		return new C3WrapOnlyExistsValidator();
	}
}
