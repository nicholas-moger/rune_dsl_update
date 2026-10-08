package test.rsr.a.meta;

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
import test.rsr.a.RsrTrade;
import test.rsr.a.validation.RsrTradeTypeFormatValidator;
import test.rsr.a.validation.RsrTradeValidator;
import test.rsr.a.validation.exists.RsrTradeOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RsrTrade.class)
public class RsrTradeMeta implements RosettaMetaData<RsrTrade> {

	@Override
	public List<Validator<? super RsrTrade>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RsrTrade, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RsrTrade> validator(ValidatorFactory factory) {
		return factory.<RsrTrade>create(RsrTradeValidator.class);
	}

	@Override
	public Validator<? super RsrTrade> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RsrTrade>create(RsrTradeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RsrTrade> validator() {
		return new RsrTradeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RsrTrade> typeFormatValidator() {
		return new RsrTradeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RsrTrade, Set<String>> onlyExistsValidator() {
		return new RsrTradeOnlyExistsValidator();
	}
}
