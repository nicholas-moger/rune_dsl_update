package holdout.typenamedutil.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedutil.Arrays;
import holdout.typenamedutil.validation.ArraysTypeFormatValidator;
import holdout.typenamedutil.validation.ArraysValidator;
import holdout.typenamedutil.validation.exists.ArraysOnlyExistsValidator;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Arrays.class)
public class ArraysMeta implements RosettaMetaData<Arrays> {

	@Override
	public List<Validator<? super Arrays>> dataRules(ValidatorFactory factory) {
		return java.util.Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Arrays, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Arrays> validator(ValidatorFactory factory) {
		return factory.<Arrays>create(ArraysValidator.class);
	}

	@Override
	public Validator<? super Arrays> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Arrays>create(ArraysTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Arrays> validator() {
		return new ArraysValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Arrays> typeFormatValidator() {
		return new ArraysTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Arrays, Set<String>> onlyExistsValidator() {
		return new ArraysOnlyExistsValidator();
	}
}
