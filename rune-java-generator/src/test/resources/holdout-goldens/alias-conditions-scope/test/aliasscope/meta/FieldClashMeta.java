package test.aliasscope.meta;

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
import test.aliasscope.FieldClash;
import test.aliasscope.validation.FieldClashTypeFormatValidator;
import test.aliasscope.validation.FieldClashValidator;
import test.aliasscope.validation.exists.FieldClashOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=FieldClash.class)
public class FieldClashMeta implements RosettaMetaData<FieldClash> {

	@Override
	public List<Validator<? super FieldClash>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super FieldClash, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super FieldClash> validator(ValidatorFactory factory) {
		return factory.<FieldClash>create(FieldClashValidator.class);
	}

	@Override
	public Validator<? super FieldClash> typeFormatValidator(ValidatorFactory factory) {
		return factory.<FieldClash>create(FieldClashTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super FieldClash> validator() {
		return new FieldClashValidator();
	}

	@Deprecated
	@Override
	public Validator<? super FieldClash> typeFormatValidator() {
		return new FieldClashTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super FieldClash, Set<String>> onlyExistsValidator() {
		return new FieldClashOnlyExistsValidator();
	}
}
