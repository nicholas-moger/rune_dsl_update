package chaos.s03.a2wild.meta;

import chaos.s03.a2wild.C3Outer;
import chaos.s03.a2wild.validation.C3OuterTypeFormatValidator;
import chaos.s03.a2wild.validation.C3OuterValidator;
import chaos.s03.a2wild.validation.datarule.C3OuterChoice;
import chaos.s03.a2wild.validation.exists.C3OuterOnlyExistsValidator;
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
@RosettaMeta(model=C3Outer.class)
public class C3OuterMeta implements RosettaMetaData<C3Outer> {

	@Override
	public List<Validator<? super C3Outer>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C3Outer>create(C3OuterChoice.class)
		);
	}
	
	@Override
	public List<Function<? super C3Outer, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C3Outer> validator(ValidatorFactory factory) {
		return factory.<C3Outer>create(C3OuterValidator.class);
	}

	@Override
	public Validator<? super C3Outer> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C3Outer>create(C3OuterTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C3Outer> validator() {
		return new C3OuterValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C3Outer> typeFormatValidator() {
		return new C3OuterTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C3Outer, Set<String>> onlyExistsValidator() {
		return new C3OuterOnlyExistsValidator();
	}
}
