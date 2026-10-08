package chaos.s25.a2wild.h.meta;

import chaos.s25.a2wild.h.C25Sub;
import chaos.s25.a2wild.h.validation.C25SubTypeFormatValidator;
import chaos.s25.a2wild.h.validation.C25SubValidator;
import chaos.s25.a2wild.h.validation.exists.C25SubOnlyExistsValidator;
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
@RosettaMeta(model=C25Sub.class)
public class C25SubMeta implements RosettaMetaData<C25Sub> {

	@Override
	public List<Validator<? super C25Sub>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C25Sub, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C25Sub> validator(ValidatorFactory factory) {
		return factory.<C25Sub>create(C25SubValidator.class);
	}

	@Override
	public Validator<? super C25Sub> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C25Sub>create(C25SubTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C25Sub> validator() {
		return new C25SubValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C25Sub> typeFormatValidator() {
		return new C25SubTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C25Sub, Set<String>> onlyExistsValidator() {
		return new C25SubOnlyExistsValidator();
	}
}
