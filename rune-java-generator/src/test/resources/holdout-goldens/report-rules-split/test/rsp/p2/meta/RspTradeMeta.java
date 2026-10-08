package test.rsp.p2.meta;

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
import test.rsp.p2.RspTrade;
import test.rsp.p2.validation.RspTradeTypeFormatValidator;
import test.rsp.p2.validation.RspTradeValidator;
import test.rsp.p2.validation.exists.RspTradeOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RspTrade.class)
public class RspTradeMeta implements RosettaMetaData<RspTrade> {

	@Override
	public List<Validator<? super RspTrade>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RspTrade, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RspTrade> validator(ValidatorFactory factory) {
		return factory.<RspTrade>create(RspTradeValidator.class);
	}

	@Override
	public Validator<? super RspTrade> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RspTrade>create(RspTradeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RspTrade> validator() {
		return new RspTradeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RspTrade> typeFormatValidator() {
		return new RspTradeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RspTrade, Set<String>> onlyExistsValidator() {
		return new RspTradeOnlyExistsValidator();
	}
}
