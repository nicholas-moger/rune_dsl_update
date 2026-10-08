package chaos.s28.a1o2.meta;

import chaos.s28.a1o2.C28OptA;
import chaos.s28.a1o2.validation.C28OptATypeFormatValidator;
import chaos.s28.a1o2.validation.C28OptAValidator;
import chaos.s28.a1o2.validation.exists.C28OptAOnlyExistsValidator;
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
@RosettaMeta(model=C28OptA.class)
public class C28OptAMeta implements RosettaMetaData<C28OptA> {

	@Override
	public List<Validator<? super C28OptA>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C28OptA, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C28OptA> validator(ValidatorFactory factory) {
		return factory.<C28OptA>create(C28OptAValidator.class);
	}

	@Override
	public Validator<? super C28OptA> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C28OptA>create(C28OptATypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C28OptA> validator() {
		return new C28OptAValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C28OptA> typeFormatValidator() {
		return new C28OptATypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C28OptA, Set<String>> onlyExistsValidator() {
		return new C28OptAOnlyExistsValidator();
	}
}
