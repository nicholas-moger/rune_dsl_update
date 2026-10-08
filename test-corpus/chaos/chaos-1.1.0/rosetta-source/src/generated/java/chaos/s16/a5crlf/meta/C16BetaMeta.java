package chaos.s16.a5crlf.meta;

import chaos.s16.a5crlf.C16Beta;
import chaos.s16.a5crlf.validation.C16BetaTypeFormatValidator;
import chaos.s16.a5crlf.validation.C16BetaValidator;
import chaos.s16.a5crlf.validation.exists.C16BetaOnlyExistsValidator;
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
@RosettaMeta(model=C16Beta.class)
public class C16BetaMeta implements RosettaMetaData<C16Beta> {

	@Override
	public List<Validator<? super C16Beta>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C16Beta, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C16Beta> validator(ValidatorFactory factory) {
		return factory.<C16Beta>create(C16BetaValidator.class);
	}

	@Override
	public Validator<? super C16Beta> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C16Beta>create(C16BetaTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C16Beta> validator() {
		return new C16BetaValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C16Beta> typeFormatValidator() {
		return new C16BetaTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C16Beta, Set<String>> onlyExistsValidator() {
		return new C16BetaOnlyExistsValidator();
	}
}
