package test.rsh.a.meta;

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
import test.rsh.a.RshTrade;
import test.rsh.a.validation.RshTradeTypeFormatValidator;
import test.rsh.a.validation.RshTradeValidator;
import test.rsh.a.validation.exists.RshTradeOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RshTrade.class)
public class RshTradeMeta implements RosettaMetaData<RshTrade> {

	@Override
	public List<Validator<? super RshTrade>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RshTrade, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RshTrade> validator(ValidatorFactory factory) {
		return factory.<RshTrade>create(RshTradeValidator.class);
	}

	@Override
	public Validator<? super RshTrade> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RshTrade>create(RshTradeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RshTrade> validator() {
		return new RshTradeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RshTrade> typeFormatValidator() {
		return new RshTradeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RshTrade, Set<String>> onlyExistsValidator() {
		return new RshTradeOnlyExistsValidator();
	}
}
