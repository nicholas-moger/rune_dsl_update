package test.rwq.a.meta;

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
import test.rwq.a.RwqTrade;
import test.rwq.a.validation.RwqTradeTypeFormatValidator;
import test.rwq.a.validation.RwqTradeValidator;
import test.rwq.a.validation.exists.RwqTradeOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RwqTrade.class)
public class RwqTradeMeta implements RosettaMetaData<RwqTrade> {

	@Override
	public List<Validator<? super RwqTrade>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RwqTrade, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RwqTrade> validator(ValidatorFactory factory) {
		return factory.<RwqTrade>create(RwqTradeValidator.class);
	}

	@Override
	public Validator<? super RwqTrade> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RwqTrade>create(RwqTradeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RwqTrade> validator() {
		return new RwqTradeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RwqTrade> typeFormatValidator() {
		return new RwqTradeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RwqTrade, Set<String>> onlyExistsValidator() {
		return new RwqTradeOnlyExistsValidator();
	}
}
