package chaos.s18.a3third.p1.meta;

import chaos.s18.a3third.p1.C18Sub;
import chaos.s18.a3third.p1.validation.C18SubTypeFormatValidator;
import chaos.s18.a3third.p1.validation.C18SubValidator;
import chaos.s18.a3third.p1.validation.exists.C18SubOnlyExistsValidator;
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
@RosettaMeta(model=C18Sub.class)
public class C18SubMeta implements RosettaMetaData<C18Sub> {

	@Override
	public List<Validator<? super C18Sub>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C18Sub, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C18Sub> validator(ValidatorFactory factory) {
		return factory.<C18Sub>create(C18SubValidator.class);
	}

	@Override
	public Validator<? super C18Sub> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C18Sub>create(C18SubTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C18Sub> validator() {
		return new C18SubValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C18Sub> typeFormatValidator() {
		return new C18SubTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C18Sub, Set<String>> onlyExistsValidator() {
		return new C18SubOnlyExistsValidator();
	}
}
