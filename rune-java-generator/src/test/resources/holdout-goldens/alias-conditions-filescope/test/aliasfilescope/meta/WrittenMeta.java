package test.aliasfilescope.meta;

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
import test.aliasfilescope.Written;
import test.aliasfilescope.validation.WrittenTypeFormatValidator;
import test.aliasfilescope.validation.WrittenValidator;
import test.aliasfilescope.validation.exists.WrittenOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Written.class)
public class WrittenMeta implements RosettaMetaData<Written> {

	@Override
	public List<Validator<? super Written>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Written, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Written> validator(ValidatorFactory factory) {
		return factory.<Written>create(WrittenValidator.class);
	}

	@Override
	public Validator<? super Written> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Written>create(WrittenTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Written> validator() {
		return new WrittenValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Written> typeFormatValidator() {
		return new WrittenTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Written, Set<String>> onlyExistsValidator() {
		return new WrittenOnlyExistsValidator();
	}
}
