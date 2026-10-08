package chaos.s29.a2wild.meta;

import chaos.s29.a2wild.C29Outer;
import chaos.s29.a2wild.validation.C29OuterTypeFormatValidator;
import chaos.s29.a2wild.validation.C29OuterValidator;
import chaos.s29.a2wild.validation.datarule.C29OuterChoice;
import chaos.s29.a2wild.validation.exists.C29OuterOnlyExistsValidator;
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
@RosettaMeta(model=C29Outer.class)
public class C29OuterMeta implements RosettaMetaData<C29Outer> {

	@Override
	public List<Validator<? super C29Outer>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C29Outer>create(C29OuterChoice.class)
		);
	}
	
	@Override
	public List<Function<? super C29Outer, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C29Outer> validator(ValidatorFactory factory) {
		return factory.<C29Outer>create(C29OuterValidator.class);
	}

	@Override
	public Validator<? super C29Outer> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C29Outer>create(C29OuterTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C29Outer> validator() {
		return new C29OuterValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C29Outer> typeFormatValidator() {
		return new C29OuterTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C29Outer, Set<String>> onlyExistsValidator() {
		return new C29OuterOnlyExistsValidator();
	}
}
