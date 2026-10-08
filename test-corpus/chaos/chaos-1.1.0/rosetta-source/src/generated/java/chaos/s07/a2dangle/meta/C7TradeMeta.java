package chaos.s07.a2dangle.meta;

import chaos.s07.a2dangle.C7Trade;
import chaos.s07.a2dangle.validation.C7TradeTypeFormatValidator;
import chaos.s07.a2dangle.validation.C7TradeValidator;
import chaos.s07.a2dangle.validation.exists.C7TradeOnlyExistsValidator;
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
@RosettaMeta(model=C7Trade.class)
public class C7TradeMeta implements RosettaMetaData<C7Trade> {

	@Override
	public List<Validator<? super C7Trade>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C7Trade, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C7Trade> validator(ValidatorFactory factory) {
		return factory.<C7Trade>create(C7TradeValidator.class);
	}

	@Override
	public Validator<? super C7Trade> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C7Trade>create(C7TradeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C7Trade> validator() {
		return new C7TradeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C7Trade> typeFormatValidator() {
		return new C7TradeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C7Trade, Set<String>> onlyExistsValidator() {
		return new C7TradeOnlyExistsValidator();
	}
}
