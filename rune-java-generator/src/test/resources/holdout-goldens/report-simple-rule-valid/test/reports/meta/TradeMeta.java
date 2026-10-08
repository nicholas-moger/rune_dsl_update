package test.reports.meta;

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
import test.reports.Trade;
import test.reports.validation.TradeTypeFormatValidator;
import test.reports.validation.TradeValidator;
import test.reports.validation.exists.TradeOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Trade.class)
public class TradeMeta implements RosettaMetaData<Trade> {

	@Override
	public List<Validator<? super Trade>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Trade, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Trade> validator(ValidatorFactory factory) {
		return factory.<Trade>create(TradeValidator.class);
	}

	@Override
	public Validator<? super Trade> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Trade>create(TradeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Trade> validator() {
		return new TradeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Trade> typeFormatValidator() {
		return new TradeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Trade, Set<String>> onlyExistsValidator() {
		return new TradeOnlyExistsValidator();
	}
}
