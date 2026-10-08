package chaos.s33.a5mixed.meta;

import chaos.s33.a5mixed.C33Trade;
import chaos.s33.a5mixed.validation.C33TradeTypeFormatValidator;
import chaos.s33.a5mixed.validation.C33TradeValidator;
import chaos.s33.a5mixed.validation.exists.C33TradeOnlyExistsValidator;
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


/**
 * @version 1.0.0
 */
@RosettaMeta(model=C33Trade.class)
public class C33TradeMeta implements RosettaMetaData<C33Trade> {

	@Override
	public List<Validator<? super C33Trade>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C33Trade, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C33Trade> validator(ValidatorFactory factory) {
		return factory.<C33Trade>create(C33TradeValidator.class);
	}

	@Override
	public Validator<? super C33Trade> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C33Trade>create(C33TradeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C33Trade> validator() {
		return new C33TradeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C33Trade> typeFormatValidator() {
		return new C33TradeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C33Trade, Set<String>> onlyExistsValidator() {
		return new C33TradeOnlyExistsValidator();
	}
}
