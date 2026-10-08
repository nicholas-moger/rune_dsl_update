package chaos.s28.a1o4.meta;

import chaos.s28.a1o4.C28Trade;
import chaos.s28.a1o4.validation.C28TradeTypeFormatValidator;
import chaos.s28.a1o4.validation.C28TradeValidator;
import chaos.s28.a1o4.validation.exists.C28TradeOnlyExistsValidator;
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
@RosettaMeta(model=C28Trade.class)
public class C28TradeMeta implements RosettaMetaData<C28Trade> {

	@Override
	public List<Validator<? super C28Trade>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C28Trade, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C28Trade> validator(ValidatorFactory factory) {
		return factory.<C28Trade>create(C28TradeValidator.class);
	}

	@Override
	public Validator<? super C28Trade> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C28Trade>create(C28TradeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C28Trade> validator() {
		return new C28TradeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C28Trade> typeFormatValidator() {
		return new C28TradeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C28Trade, Set<String>> onlyExistsValidator() {
		return new C28TradeOnlyExistsValidator();
	}
}
