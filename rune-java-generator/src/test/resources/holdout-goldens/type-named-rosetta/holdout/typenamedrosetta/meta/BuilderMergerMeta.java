package holdout.typenamedrosetta.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.BuilderMerger;
import holdout.typenamedrosetta.validation.BuilderMergerTypeFormatValidator;
import holdout.typenamedrosetta.validation.BuilderMergerValidator;
import holdout.typenamedrosetta.validation.exists.BuilderMergerOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=BuilderMerger.class)
public class BuilderMergerMeta implements RosettaMetaData<BuilderMerger> {

	@Override
	public List<Validator<? super BuilderMerger>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super BuilderMerger, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super BuilderMerger> validator(ValidatorFactory factory) {
		return factory.<BuilderMerger>create(BuilderMergerValidator.class);
	}

	@Override
	public Validator<? super BuilderMerger> typeFormatValidator(ValidatorFactory factory) {
		return factory.<BuilderMerger>create(BuilderMergerTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super BuilderMerger> validator() {
		return new BuilderMergerValidator();
	}

	@Deprecated
	@Override
	public Validator<? super BuilderMerger> typeFormatValidator() {
		return new BuilderMergerTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super BuilderMerger, Set<String>> onlyExistsValidator() {
		return new BuilderMergerOnlyExistsValidator();
	}
}
