package chaos.s18.a5uni.meta;

import chaos.s18.a5uni.C18OptA;
import chaos.s18.a5uni.validation.C18OptATypeFormatValidator;
import chaos.s18.a5uni.validation.C18OptAValidator;
import chaos.s18.a5uni.validation.exists.C18OptAOnlyExistsValidator;
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
@RosettaMeta(model=C18OptA.class)
public class C18OptAMeta implements RosettaMetaData<C18OptA> {

	@Override
	public List<Validator<? super C18OptA>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C18OptA, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C18OptA> validator(ValidatorFactory factory) {
		return factory.<C18OptA>create(C18OptAValidator.class);
	}

	@Override
	public Validator<? super C18OptA> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C18OptA>create(C18OptATypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C18OptA> validator() {
		return new C18OptAValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C18OptA> typeFormatValidator() {
		return new C18OptATypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C18OptA, Set<String>> onlyExistsValidator() {
		return new C18OptAOnlyExistsValidator();
	}
}
