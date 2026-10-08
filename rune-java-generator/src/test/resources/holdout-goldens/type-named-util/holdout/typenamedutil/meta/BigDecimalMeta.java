package holdout.typenamedutil.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedutil.BigDecimal;
import holdout.typenamedutil.validation.BigDecimalTypeFormatValidator;
import holdout.typenamedutil.validation.BigDecimalValidator;
import holdout.typenamedutil.validation.exists.BigDecimalOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=BigDecimal.class)
public class BigDecimalMeta implements RosettaMetaData<BigDecimal> {

	@Override
	public List<Validator<? super BigDecimal>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super BigDecimal, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super BigDecimal> validator(ValidatorFactory factory) {
		return factory.<BigDecimal>create(BigDecimalValidator.class);
	}

	@Override
	public Validator<? super BigDecimal> typeFormatValidator(ValidatorFactory factory) {
		return factory.<BigDecimal>create(BigDecimalTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super BigDecimal> validator() {
		return new BigDecimalValidator();
	}

	@Deprecated
	@Override
	public Validator<? super BigDecimal> typeFormatValidator() {
		return new BigDecimalTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super BigDecimal, Set<String>> onlyExistsValidator() {
		return new BigDecimalOnlyExistsValidator();
	}
}
