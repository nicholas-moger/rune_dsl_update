package test.wmwrapedge.meta;

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
import test.wmwrapedge.KeyedHolder;
import test.wmwrapedge.validation.KeyedHolderTypeFormatValidator;
import test.wmwrapedge.validation.KeyedHolderValidator;
import test.wmwrapedge.validation.exists.KeyedHolderOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=KeyedHolder.class)
public class KeyedHolderMeta implements RosettaMetaData<KeyedHolder> {

	@Override
	public List<Validator<? super KeyedHolder>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super KeyedHolder, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super KeyedHolder> validator(ValidatorFactory factory) {
		return factory.<KeyedHolder>create(KeyedHolderValidator.class);
	}

	@Override
	public Validator<? super KeyedHolder> typeFormatValidator(ValidatorFactory factory) {
		return factory.<KeyedHolder>create(KeyedHolderTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super KeyedHolder> validator() {
		return new KeyedHolderValidator();
	}

	@Deprecated
	@Override
	public Validator<? super KeyedHolder> typeFormatValidator() {
		return new KeyedHolderTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super KeyedHolder, Set<String>> onlyExistsValidator() {
		return new KeyedHolderOnlyExistsValidator();
	}
}
