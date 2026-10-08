package holdout.typenamedrosetta.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.RosettaPath;
import holdout.typenamedrosetta.validation.RosettaPathTypeFormatValidator;
import holdout.typenamedrosetta.validation.RosettaPathValidator;
import holdout.typenamedrosetta.validation.exists.RosettaPathOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RosettaPath.class)
public class RosettaPathMeta implements RosettaMetaData<RosettaPath> {

	@Override
	public List<Validator<? super RosettaPath>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RosettaPath, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RosettaPath> validator(ValidatorFactory factory) {
		return factory.<RosettaPath>create(RosettaPathValidator.class);
	}

	@Override
	public Validator<? super RosettaPath> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RosettaPath>create(RosettaPathTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RosettaPath> validator() {
		return new RosettaPathValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RosettaPath> typeFormatValidator() {
		return new RosettaPathTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RosettaPath, Set<String>> onlyExistsValidator() {
		return new RosettaPathOnlyExistsValidator();
	}
}
