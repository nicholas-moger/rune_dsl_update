package holdout.typenamedutil.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedutil.Function;
import holdout.typenamedutil.validation.FunctionTypeFormatValidator;
import holdout.typenamedutil.validation.FunctionValidator;
import holdout.typenamedutil.validation.exists.FunctionOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Function.class)
public class FunctionMeta implements RosettaMetaData<Function> {

	@Override
	public List<Validator<? super Function>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<java.util.function.Function<? super Function, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Function> validator(ValidatorFactory factory) {
		return factory.<Function>create(FunctionValidator.class);
	}

	@Override
	public Validator<? super Function> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Function>create(FunctionTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Function> validator() {
		return new FunctionValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Function> typeFormatValidator() {
		return new FunctionTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Function, Set<String>> onlyExistsValidator() {
		return new FunctionOnlyExistsValidator();
	}
}
