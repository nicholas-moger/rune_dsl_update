package chaos.s29.a2qual.meta;

import chaos.s29.a2qual.C29In2;
import chaos.s29.a2qual.validation.C29In2TypeFormatValidator;
import chaos.s29.a2qual.validation.C29In2Validator;
import chaos.s29.a2qual.validation.exists.C29In2OnlyExistsValidator;
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
@RosettaMeta(model=C29In2.class)
public class C29In2Meta implements RosettaMetaData<C29In2> {

	@Override
	public List<Validator<? super C29In2>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C29In2, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C29In2> validator(ValidatorFactory factory) {
		return factory.<C29In2>create(C29In2Validator.class);
	}

	@Override
	public Validator<? super C29In2> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C29In2>create(C29In2TypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C29In2> validator() {
		return new C29In2Validator();
	}

	@Deprecated
	@Override
	public Validator<? super C29In2> typeFormatValidator() {
		return new C29In2TypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C29In2, Set<String>> onlyExistsValidator() {
		return new C29In2OnlyExistsValidator();
	}
}
