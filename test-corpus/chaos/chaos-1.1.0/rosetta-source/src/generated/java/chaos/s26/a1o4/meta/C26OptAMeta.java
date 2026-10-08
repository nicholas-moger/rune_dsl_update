package chaos.s26.a1o4.meta;

import chaos.s26.a1o4.C26OptA;
import chaos.s26.a1o4.validation.C26OptATypeFormatValidator;
import chaos.s26.a1o4.validation.C26OptAValidator;
import chaos.s26.a1o4.validation.exists.C26OptAOnlyExistsValidator;
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
@RosettaMeta(model=C26OptA.class)
public class C26OptAMeta implements RosettaMetaData<C26OptA> {

	@Override
	public List<Validator<? super C26OptA>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C26OptA, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C26OptA> validator(ValidatorFactory factory) {
		return factory.<C26OptA>create(C26OptAValidator.class);
	}

	@Override
	public Validator<? super C26OptA> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C26OptA>create(C26OptATypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C26OptA> validator() {
		return new C26OptAValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C26OptA> typeFormatValidator() {
		return new C26OptATypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C26OptA, Set<String>> onlyExistsValidator() {
		return new C26OptAOnlyExistsValidator();
	}
}
