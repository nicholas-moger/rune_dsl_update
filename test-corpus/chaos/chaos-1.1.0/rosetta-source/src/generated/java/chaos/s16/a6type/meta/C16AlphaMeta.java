package chaos.s16.a6type.meta;

import chaos.s16.a6type.C16Alpha;
import chaos.s16.a6type.validation.C16AlphaTypeFormatValidator;
import chaos.s16.a6type.validation.C16AlphaValidator;
import chaos.s16.a6type.validation.exists.C16AlphaOnlyExistsValidator;
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
@RosettaMeta(model=C16Alpha.class)
public class C16AlphaMeta implements RosettaMetaData<C16Alpha> {

	@Override
	public List<Validator<? super C16Alpha>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C16Alpha, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C16Alpha> validator(ValidatorFactory factory) {
		return factory.<C16Alpha>create(C16AlphaValidator.class);
	}

	@Override
	public Validator<? super C16Alpha> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C16Alpha>create(C16AlphaTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C16Alpha> validator() {
		return new C16AlphaValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C16Alpha> typeFormatValidator() {
		return new C16AlphaTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C16Alpha, Set<String>> onlyExistsValidator() {
		return new C16AlphaOnlyExistsValidator();
	}
}
