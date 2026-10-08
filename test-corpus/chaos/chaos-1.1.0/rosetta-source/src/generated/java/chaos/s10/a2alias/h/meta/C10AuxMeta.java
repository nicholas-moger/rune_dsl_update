package chaos.s10.a2alias.h.meta;

import chaos.s10.a2alias.h.C10Aux;
import chaos.s10.a2alias.h.validation.C10AuxTypeFormatValidator;
import chaos.s10.a2alias.h.validation.C10AuxValidator;
import chaos.s10.a2alias.h.validation.exists.C10AuxOnlyExistsValidator;
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
@RosettaMeta(model=C10Aux.class)
public class C10AuxMeta implements RosettaMetaData<C10Aux> {

	@Override
	public List<Validator<? super C10Aux>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C10Aux, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C10Aux> validator(ValidatorFactory factory) {
		return factory.<C10Aux>create(C10AuxValidator.class);
	}

	@Override
	public Validator<? super C10Aux> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C10Aux>create(C10AuxTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C10Aux> validator() {
		return new C10AuxValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C10Aux> typeFormatValidator() {
		return new C10AuxTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C10Aux, Set<String>> onlyExistsValidator() {
		return new C10AuxOnlyExistsValidator();
	}
}
