package chaos.s21.a2alias.meta;

import chaos.s21.a2alias.C21Exactly;
import chaos.s21.a2alias.validation.C21ExactlyTypeFormatValidator;
import chaos.s21.a2alias.validation.C21ExactlyValidator;
import chaos.s21.a2alias.validation.datarule.C21ExactlyOneOf0;
import chaos.s21.a2alias.validation.exists.C21ExactlyOnlyExistsValidator;
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
@RosettaMeta(model=C21Exactly.class)
public class C21ExactlyMeta implements RosettaMetaData<C21Exactly> {

	@Override
	public List<Validator<? super C21Exactly>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C21Exactly>create(C21ExactlyOneOf0.class)
		);
	}
	
	@Override
	public List<Function<? super C21Exactly, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C21Exactly> validator(ValidatorFactory factory) {
		return factory.<C21Exactly>create(C21ExactlyValidator.class);
	}

	@Override
	public Validator<? super C21Exactly> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C21Exactly>create(C21ExactlyTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C21Exactly> validator() {
		return new C21ExactlyValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C21Exactly> typeFormatValidator() {
		return new C21ExactlyTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C21Exactly, Set<String>> onlyExistsValidator() {
		return new C21ExactlyOnlyExistsValidator();
	}
}
