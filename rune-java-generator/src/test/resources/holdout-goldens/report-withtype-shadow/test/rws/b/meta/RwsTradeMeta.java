package test.rws.b.meta;

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
import test.rws.b.RwsTrade;
import test.rws.b.validation.RwsTradeTypeFormatValidator;
import test.rws.b.validation.RwsTradeValidator;
import test.rws.b.validation.exists.RwsTradeOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RwsTrade.class)
public class RwsTradeMeta implements RosettaMetaData<RwsTrade> {

	@Override
	public List<Validator<? super RwsTrade>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RwsTrade, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RwsTrade> validator(ValidatorFactory factory) {
		return factory.<RwsTrade>create(RwsTradeValidator.class);
	}

	@Override
	public Validator<? super RwsTrade> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RwsTrade>create(RwsTradeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RwsTrade> validator() {
		return new RwsTradeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RwsTrade> typeFormatValidator() {
		return new RwsTradeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RwsTrade, Set<String>> onlyExistsValidator() {
		return new RwsTradeOnlyExistsValidator();
	}
}
