package test.deeppathedge.meta;

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
import test.deeppathedge.CashLeg;
import test.deeppathedge.validation.CashLegTypeFormatValidator;
import test.deeppathedge.validation.CashLegValidator;
import test.deeppathedge.validation.exists.CashLegOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=CashLeg.class)
public class CashLegMeta implements RosettaMetaData<CashLeg> {

	@Override
	public List<Validator<? super CashLeg>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super CashLeg, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super CashLeg> validator(ValidatorFactory factory) {
		return factory.<CashLeg>create(CashLegValidator.class);
	}

	@Override
	public Validator<? super CashLeg> typeFormatValidator(ValidatorFactory factory) {
		return factory.<CashLeg>create(CashLegTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super CashLeg> validator() {
		return new CashLegValidator();
	}

	@Deprecated
	@Override
	public Validator<? super CashLeg> typeFormatValidator() {
		return new CashLegTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super CashLeg, Set<String>> onlyExistsValidator() {
		return new CashLegOnlyExistsValidator();
	}
}
