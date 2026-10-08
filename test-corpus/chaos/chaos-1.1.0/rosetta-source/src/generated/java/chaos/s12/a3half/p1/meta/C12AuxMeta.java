package chaos.s12.a3half.p1.meta;

import chaos.s12.a3half.p1.C12Aux;
import chaos.s12.a3half.p1.validation.C12AuxTypeFormatValidator;
import chaos.s12.a3half.p1.validation.C12AuxValidator;
import chaos.s12.a3half.p1.validation.exists.C12AuxOnlyExistsValidator;
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
@RosettaMeta(model=C12Aux.class)
public class C12AuxMeta implements RosettaMetaData<C12Aux> {

	@Override
	public List<Validator<? super C12Aux>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C12Aux, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C12Aux> validator(ValidatorFactory factory) {
		return factory.<C12Aux>create(C12AuxValidator.class);
	}

	@Override
	public Validator<? super C12Aux> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C12Aux>create(C12AuxTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C12Aux> validator() {
		return new C12AuxValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C12Aux> typeFormatValidator() {
		return new C12AuxTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C12Aux, Set<String>> onlyExistsValidator() {
		return new C12AuxOnlyExistsValidator();
	}
}
