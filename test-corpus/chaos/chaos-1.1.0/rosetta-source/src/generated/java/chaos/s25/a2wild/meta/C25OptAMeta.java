package chaos.s25.a2wild.meta;

import chaos.s25.a2wild.C25OptA;
import chaos.s25.a2wild.validation.C25OptATypeFormatValidator;
import chaos.s25.a2wild.validation.C25OptAValidator;
import chaos.s25.a2wild.validation.exists.C25OptAOnlyExistsValidator;
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
@RosettaMeta(model=C25OptA.class)
public class C25OptAMeta implements RosettaMetaData<C25OptA> {

	@Override
	public List<Validator<? super C25OptA>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C25OptA, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C25OptA> validator(ValidatorFactory factory) {
		return factory.<C25OptA>create(C25OptAValidator.class);
	}

	@Override
	public Validator<? super C25OptA> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C25OptA>create(C25OptATypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C25OptA> validator() {
		return new C25OptAValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C25OptA> typeFormatValidator() {
		return new C25OptATypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C25OptA, Set<String>> onlyExistsValidator() {
		return new C25OptAOnlyExistsValidator();
	}
}
