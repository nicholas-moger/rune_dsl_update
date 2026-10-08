package holdout.typenamedrosetta.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.Processor;
import holdout.typenamedrosetta.validation.ProcessorTypeFormatValidator;
import holdout.typenamedrosetta.validation.ProcessorValidator;
import holdout.typenamedrosetta.validation.exists.ProcessorOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Processor.class)
public class ProcessorMeta implements RosettaMetaData<Processor> {

	@Override
	public List<Validator<? super Processor>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Processor, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Processor> validator(ValidatorFactory factory) {
		return factory.<Processor>create(ProcessorValidator.class);
	}

	@Override
	public Validator<? super Processor> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Processor>create(ProcessorTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Processor> validator() {
		return new ProcessorValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Processor> typeFormatValidator() {
		return new ProcessorTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Processor, Set<String>> onlyExistsValidator() {
		return new ProcessorOnlyExistsValidator();
	}
}
