package chaos.s18.a2dangle.meta;

import chaos.s18.a2dangle.C18OptB;
import chaos.s18.a2dangle.validation.C18OptBTypeFormatValidator;
import chaos.s18.a2dangle.validation.C18OptBValidator;
import chaos.s18.a2dangle.validation.exists.C18OptBOnlyExistsValidator;
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
@RosettaMeta(model=C18OptB.class)
public class C18OptBMeta implements RosettaMetaData<C18OptB> {

	@Override
	public List<Validator<? super C18OptB>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C18OptB, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C18OptB> validator(ValidatorFactory factory) {
		return factory.<C18OptB>create(C18OptBValidator.class);
	}

	@Override
	public Validator<? super C18OptB> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C18OptB>create(C18OptBTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C18OptB> validator() {
		return new C18OptBValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C18OptB> typeFormatValidator() {
		return new C18OptBTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C18OptB, Set<String>> onlyExistsValidator() {
		return new C18OptBOnlyExistsValidator();
	}
}
