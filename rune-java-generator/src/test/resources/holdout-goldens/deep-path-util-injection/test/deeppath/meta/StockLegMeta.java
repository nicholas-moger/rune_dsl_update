package test.deeppath.meta;

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
import test.deeppath.StockLeg;
import test.deeppath.validation.StockLegTypeFormatValidator;
import test.deeppath.validation.StockLegValidator;
import test.deeppath.validation.exists.StockLegOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=StockLeg.class)
public class StockLegMeta implements RosettaMetaData<StockLeg> {

	@Override
	public List<Validator<? super StockLeg>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super StockLeg, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super StockLeg> validator(ValidatorFactory factory) {
		return factory.<StockLeg>create(StockLegValidator.class);
	}

	@Override
	public Validator<? super StockLeg> typeFormatValidator(ValidatorFactory factory) {
		return factory.<StockLeg>create(StockLegTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super StockLeg> validator() {
		return new StockLegValidator();
	}

	@Deprecated
	@Override
	public Validator<? super StockLeg> typeFormatValidator() {
		return new StockLegTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super StockLeg, Set<String>> onlyExistsValidator() {
		return new StockLegOnlyExistsValidator();
	}
}
