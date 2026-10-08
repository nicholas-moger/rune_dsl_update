package chaos.s24.a1o3.meta;

import chaos.s24.a1o3.C24Keyed;
import chaos.s24.a1o3.validation.C24KeyedTypeFormatValidator;
import chaos.s24.a1o3.validation.C24KeyedValidator;
import chaos.s24.a1o3.validation.exists.C24KeyedOnlyExistsValidator;
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
@RosettaMeta(model=C24Keyed.class)
public class C24KeyedMeta implements RosettaMetaData<C24Keyed> {

	@Override
	public List<Validator<? super C24Keyed>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C24Keyed, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C24Keyed> validator(ValidatorFactory factory) {
		return factory.<C24Keyed>create(C24KeyedValidator.class);
	}

	@Override
	public Validator<? super C24Keyed> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C24Keyed>create(C24KeyedTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C24Keyed> validator() {
		return new C24KeyedValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C24Keyed> typeFormatValidator() {
		return new C24KeyedTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C24Keyed, Set<String>> onlyExistsValidator() {
		return new C24KeyedOnlyExistsValidator();
	}
}
