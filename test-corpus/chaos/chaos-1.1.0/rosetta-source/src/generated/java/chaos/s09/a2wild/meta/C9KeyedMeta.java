package chaos.s09.a2wild.meta;

import chaos.s09.a2wild.C9Keyed;
import chaos.s09.a2wild.validation.C9KeyedTypeFormatValidator;
import chaos.s09.a2wild.validation.C9KeyedValidator;
import chaos.s09.a2wild.validation.exists.C9KeyedOnlyExistsValidator;
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
@RosettaMeta(model=C9Keyed.class)
public class C9KeyedMeta implements RosettaMetaData<C9Keyed> {

	@Override
	public List<Validator<? super C9Keyed>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C9Keyed, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C9Keyed> validator(ValidatorFactory factory) {
		return factory.<C9Keyed>create(C9KeyedValidator.class);
	}

	@Override
	public Validator<? super C9Keyed> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C9Keyed>create(C9KeyedTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C9Keyed> validator() {
		return new C9KeyedValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C9Keyed> typeFormatValidator() {
		return new C9KeyedTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C9Keyed, Set<String>> onlyExistsValidator() {
		return new C9KeyedOnlyExistsValidator();
	}
}
