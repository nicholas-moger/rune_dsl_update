package chaos.s01.a1o1.meta;

import chaos.s01.a1o1.C1Ref;
import chaos.s01.a1o1.validation.C1RefTypeFormatValidator;
import chaos.s01.a1o1.validation.C1RefValidator;
import chaos.s01.a1o1.validation.exists.C1RefOnlyExistsValidator;
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
@RosettaMeta(model=C1Ref.class)
public class C1RefMeta implements RosettaMetaData<C1Ref> {

	@Override
	public List<Validator<? super C1Ref>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C1Ref, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C1Ref> validator(ValidatorFactory factory) {
		return factory.<C1Ref>create(C1RefValidator.class);
	}

	@Override
	public Validator<? super C1Ref> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C1Ref>create(C1RefTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C1Ref> validator() {
		return new C1RefValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C1Ref> typeFormatValidator() {
		return new C1RefTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C1Ref, Set<String>> onlyExistsValidator() {
		return new C1RefOnlyExistsValidator();
	}
}
