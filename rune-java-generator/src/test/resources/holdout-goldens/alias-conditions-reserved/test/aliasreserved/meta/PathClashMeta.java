package test.aliasreserved.meta;

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
import test.aliasreserved.PathClash;
import test.aliasreserved.validation.PathClashTypeFormatValidator;
import test.aliasreserved.validation.PathClashValidator;
import test.aliasreserved.validation.exists.PathClashOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=PathClash.class)
public class PathClashMeta implements RosettaMetaData<PathClash> {

	@Override
	public List<Validator<? super PathClash>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super PathClash, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super PathClash> validator(ValidatorFactory factory) {
		return factory.<PathClash>create(PathClashValidator.class);
	}

	@Override
	public Validator<? super PathClash> typeFormatValidator(ValidatorFactory factory) {
		return factory.<PathClash>create(PathClashTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super PathClash> validator() {
		return new PathClashValidator();
	}

	@Deprecated
	@Override
	public Validator<? super PathClash> typeFormatValidator() {
		return new PathClashTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super PathClash, Set<String>> onlyExistsValidator() {
		return new PathClashOnlyExistsValidator();
	}
}
