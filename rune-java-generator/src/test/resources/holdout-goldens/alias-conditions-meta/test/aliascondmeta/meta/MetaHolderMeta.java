package test.aliascondmeta.meta;

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
import test.aliascondmeta.MetaHolder;
import test.aliascondmeta.validation.MetaHolderTypeFormatValidator;
import test.aliascondmeta.validation.MetaHolderValidator;
import test.aliascondmeta.validation.exists.MetaHolderOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=MetaHolder.class)
public class MetaHolderMeta implements RosettaMetaData<MetaHolder> {

	@Override
	public List<Validator<? super MetaHolder>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super MetaHolder, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super MetaHolder> validator(ValidatorFactory factory) {
		return factory.<MetaHolder>create(MetaHolderValidator.class);
	}

	@Override
	public Validator<? super MetaHolder> typeFormatValidator(ValidatorFactory factory) {
		return factory.<MetaHolder>create(MetaHolderTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super MetaHolder> validator() {
		return new MetaHolderValidator();
	}

	@Deprecated
	@Override
	public Validator<? super MetaHolder> typeFormatValidator() {
		return new MetaHolderTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super MetaHolder, Set<String>> onlyExistsValidator() {
		return new MetaHolderOnlyExistsValidator();
	}
}
