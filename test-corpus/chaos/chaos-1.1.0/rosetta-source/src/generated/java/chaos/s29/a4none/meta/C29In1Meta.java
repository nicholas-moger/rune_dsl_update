package chaos.s29.a4none.meta;

import chaos.s29.a4none.C29In1;
import chaos.s29.a4none.validation.C29In1TypeFormatValidator;
import chaos.s29.a4none.validation.C29In1Validator;
import chaos.s29.a4none.validation.exists.C29In1OnlyExistsValidator;
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
 * @version 0.0.0
 */
@RosettaMeta(model=C29In1.class)
public class C29In1Meta implements RosettaMetaData<C29In1> {

	@Override
	public List<Validator<? super C29In1>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C29In1, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C29In1> validator(ValidatorFactory factory) {
		return factory.<C29In1>create(C29In1Validator.class);
	}

	@Override
	public Validator<? super C29In1> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C29In1>create(C29In1TypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C29In1> validator() {
		return new C29In1Validator();
	}

	@Deprecated
	@Override
	public Validator<? super C29In1> typeFormatValidator() {
		return new C29In1TypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C29In1, Set<String>> onlyExistsValidator() {
		return new C29In1OnlyExistsValidator();
	}
}
