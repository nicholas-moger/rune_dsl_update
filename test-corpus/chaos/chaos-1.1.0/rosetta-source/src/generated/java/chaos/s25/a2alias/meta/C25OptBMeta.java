package chaos.s25.a2alias.meta;

import chaos.s25.a2alias.C25OptB;
import chaos.s25.a2alias.validation.C25OptBTypeFormatValidator;
import chaos.s25.a2alias.validation.C25OptBValidator;
import chaos.s25.a2alias.validation.exists.C25OptBOnlyExistsValidator;
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
@RosettaMeta(model=C25OptB.class)
public class C25OptBMeta implements RosettaMetaData<C25OptB> {

	@Override
	public List<Validator<? super C25OptB>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C25OptB, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C25OptB> validator(ValidatorFactory factory) {
		return factory.<C25OptB>create(C25OptBValidator.class);
	}

	@Override
	public Validator<? super C25OptB> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C25OptB>create(C25OptBTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C25OptB> validator() {
		return new C25OptBValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C25OptB> typeFormatValidator() {
		return new C25OptBTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C25OptB, Set<String>> onlyExistsValidator() {
		return new C25OptBOnlyExistsValidator();
	}
}
