package holdout.typenamedrosetta.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.QualifyFunctionFactory;
import holdout.typenamedrosetta.validation.QualifyFunctionFactoryTypeFormatValidator;
import holdout.typenamedrosetta.validation.QualifyFunctionFactoryValidator;
import holdout.typenamedrosetta.validation.exists.QualifyFunctionFactoryOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=QualifyFunctionFactory.class)
public class QualifyFunctionFactoryMeta implements RosettaMetaData<QualifyFunctionFactory> {

	@Override
	public List<Validator<? super QualifyFunctionFactory>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super QualifyFunctionFactory, QualifyResult>> getQualifyFunctions(com.rosetta.model.lib.qualify.QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super QualifyFunctionFactory> validator(ValidatorFactory factory) {
		return factory.<QualifyFunctionFactory>create(QualifyFunctionFactoryValidator.class);
	}

	@Override
	public Validator<? super QualifyFunctionFactory> typeFormatValidator(ValidatorFactory factory) {
		return factory.<QualifyFunctionFactory>create(QualifyFunctionFactoryTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super QualifyFunctionFactory> validator() {
		return new QualifyFunctionFactoryValidator();
	}

	@Deprecated
	@Override
	public Validator<? super QualifyFunctionFactory> typeFormatValidator() {
		return new QualifyFunctionFactoryTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super QualifyFunctionFactory, Set<String>> onlyExistsValidator() {
		return new QualifyFunctionFactoryOnlyExistsValidator();
	}
}
