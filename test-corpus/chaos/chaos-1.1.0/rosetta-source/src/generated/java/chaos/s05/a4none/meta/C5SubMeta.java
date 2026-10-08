package chaos.s05.a4none.meta;

import chaos.s05.a4none.C5Sub;
import chaos.s05.a4none.validation.C5SubTypeFormatValidator;
import chaos.s05.a4none.validation.C5SubValidator;
import chaos.s05.a4none.validation.exists.C5SubOnlyExistsValidator;
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
@RosettaMeta(model=C5Sub.class)
public class C5SubMeta implements RosettaMetaData<C5Sub> {

	@Override
	public List<Validator<? super C5Sub>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C5Sub, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C5Sub> validator(ValidatorFactory factory) {
		return factory.<C5Sub>create(C5SubValidator.class);
	}

	@Override
	public Validator<? super C5Sub> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C5Sub>create(C5SubTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C5Sub> validator() {
		return new C5SubValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C5Sub> typeFormatValidator() {
		return new C5SubTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C5Sub, Set<String>> onlyExistsValidator() {
		return new C5SubOnlyExistsValidator();
	}
}
