package chaos.s03.a4snap.meta;

import chaos.s03.a4snap.C3StockLeg;
import chaos.s03.a4snap.validation.C3StockLegTypeFormatValidator;
import chaos.s03.a4snap.validation.C3StockLegValidator;
import chaos.s03.a4snap.validation.exists.C3StockLegOnlyExistsValidator;
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
 * @version 1.0.0-SNAPSHOT
 */
@RosettaMeta(model=C3StockLeg.class)
public class C3StockLegMeta implements RosettaMetaData<C3StockLeg> {

	@Override
	public List<Validator<? super C3StockLeg>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C3StockLeg, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C3StockLeg> validator(ValidatorFactory factory) {
		return factory.<C3StockLeg>create(C3StockLegValidator.class);
	}

	@Override
	public Validator<? super C3StockLeg> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C3StockLeg>create(C3StockLegTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C3StockLeg> validator() {
		return new C3StockLegValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C3StockLeg> typeFormatValidator() {
		return new C3StockLegTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C3StockLeg, Set<String>> onlyExistsValidator() {
		return new C3StockLegOnlyExistsValidator();
	}
}
