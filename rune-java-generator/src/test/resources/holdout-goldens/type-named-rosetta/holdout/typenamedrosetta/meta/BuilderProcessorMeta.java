package holdout.typenamedrosetta.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.BuilderProcessor;
import holdout.typenamedrosetta.validation.BuilderProcessorTypeFormatValidator;
import holdout.typenamedrosetta.validation.BuilderProcessorValidator;
import holdout.typenamedrosetta.validation.exists.BuilderProcessorOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=BuilderProcessor.class)
public class BuilderProcessorMeta implements RosettaMetaData<BuilderProcessor> {

	@Override
	public List<Validator<? super BuilderProcessor>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super BuilderProcessor, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super BuilderProcessor> validator(ValidatorFactory factory) {
		return factory.<BuilderProcessor>create(BuilderProcessorValidator.class);
	}

	@Override
	public Validator<? super BuilderProcessor> typeFormatValidator(ValidatorFactory factory) {
		return factory.<BuilderProcessor>create(BuilderProcessorTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super BuilderProcessor> validator() {
		return new BuilderProcessorValidator();
	}

	@Deprecated
	@Override
	public Validator<? super BuilderProcessor> typeFormatValidator() {
		return new BuilderProcessorTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super BuilderProcessor, Set<String>> onlyExistsValidator() {
		return new BuilderProcessorOnlyExistsValidator();
	}
}
