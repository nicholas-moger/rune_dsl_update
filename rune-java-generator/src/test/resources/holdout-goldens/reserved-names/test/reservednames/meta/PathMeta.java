package test.reservednames.meta;

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
import test.reservednames.Path;
import test.reservednames.validation.PathTypeFormatValidator;
import test.reservednames.validation.PathValidator;
import test.reservednames.validation.datarule.PathNameExists;
import test.reservednames.validation.exists.PathOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Path.class)
public class PathMeta implements RosettaMetaData<Path> {

	@Override
	public List<Validator<? super Path>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<Path>create(PathNameExists.class)
		);
	}
	
	@Override
	public List<Function<? super Path, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Path> validator(ValidatorFactory factory) {
		return factory.<Path>create(PathValidator.class);
	}

	@Override
	public Validator<? super Path> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Path>create(PathTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Path> validator() {
		return new PathValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Path> typeFormatValidator() {
		return new PathTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Path, Set<String>> onlyExistsValidator() {
		return new PathOnlyExistsValidator();
	}
}
