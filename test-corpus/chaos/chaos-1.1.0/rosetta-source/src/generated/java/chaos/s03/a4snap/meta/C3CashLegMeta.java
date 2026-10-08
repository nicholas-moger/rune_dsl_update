package chaos.s03.a4snap.meta;

import chaos.s03.a4snap.C3CashLeg;
import chaos.s03.a4snap.validation.C3CashLegTypeFormatValidator;
import chaos.s03.a4snap.validation.C3CashLegValidator;
import chaos.s03.a4snap.validation.exists.C3CashLegOnlyExistsValidator;
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
@RosettaMeta(model=C3CashLeg.class)
public class C3CashLegMeta implements RosettaMetaData<C3CashLeg> {

	@Override
	public List<Validator<? super C3CashLeg>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C3CashLeg, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C3CashLeg> validator(ValidatorFactory factory) {
		return factory.<C3CashLeg>create(C3CashLegValidator.class);
	}

	@Override
	public Validator<? super C3CashLeg> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C3CashLeg>create(C3CashLegTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C3CashLeg> validator() {
		return new C3CashLegValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C3CashLeg> typeFormatValidator() {
		return new C3CashLegTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C3CashLeg, Set<String>> onlyExistsValidator() {
		return new C3CashLegOnlyExistsValidator();
	}
}
