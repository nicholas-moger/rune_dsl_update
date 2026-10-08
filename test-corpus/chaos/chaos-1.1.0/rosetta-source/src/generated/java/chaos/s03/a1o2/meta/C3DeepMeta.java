package chaos.s03.a1o2.meta;

import chaos.s03.a1o2.C3Deep;
import chaos.s03.a1o2.validation.C3DeepTypeFormatValidator;
import chaos.s03.a1o2.validation.C3DeepValidator;
import chaos.s03.a1o2.validation.datarule.C3DeepC3CommonNav;
import chaos.s03.a1o2.validation.datarule.C3DeepC3TextWhenPicked;
import chaos.s03.a1o2.validation.exists.C3DeepOnlyExistsValidator;
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
@RosettaMeta(model=C3Deep.class)
public class C3DeepMeta implements RosettaMetaData<C3Deep> {

	@Override
	public List<Validator<? super C3Deep>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C3Deep>create(C3DeepC3TextWhenPicked.class),
			factory.<C3Deep>create(C3DeepC3CommonNav.class)
		);
	}
	
	@Override
	public List<Function<? super C3Deep, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C3Deep> validator(ValidatorFactory factory) {
		return factory.<C3Deep>create(C3DeepValidator.class);
	}

	@Override
	public Validator<? super C3Deep> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C3Deep>create(C3DeepTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C3Deep> validator() {
		return new C3DeepValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C3Deep> typeFormatValidator() {
		return new C3DeepTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C3Deep, Set<String>> onlyExistsValidator() {
		return new C3DeepOnlyExistsValidator();
	}
}
