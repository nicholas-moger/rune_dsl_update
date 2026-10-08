package test.mladder.meta;

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
import test.mladder.Keyed;
import test.mladder.validation.KeyedTypeFormatValidator;
import test.mladder.validation.KeyedValidator;
import test.mladder.validation.exists.KeyedOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Keyed.class)
public class KeyedMeta implements RosettaMetaData<Keyed> {

	@Override
	public List<Validator<? super Keyed>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Keyed, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Keyed> validator(ValidatorFactory factory) {
		return factory.<Keyed>create(KeyedValidator.class);
	}

	@Override
	public Validator<? super Keyed> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Keyed>create(KeyedTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Keyed> validator() {
		return new KeyedValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Keyed> typeFormatValidator() {
		return new KeyedTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Keyed, Set<String>> onlyExistsValidator() {
		return new KeyedOnlyExistsValidator();
	}
}
