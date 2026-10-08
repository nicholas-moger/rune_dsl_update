package chaos.s11.a2alias.h.meta;

import chaos.s11.a2alias.h.C11Aux;
import chaos.s11.a2alias.h.validation.C11AuxTypeFormatValidator;
import chaos.s11.a2alias.h.validation.C11AuxValidator;
import chaos.s11.a2alias.h.validation.exists.C11AuxOnlyExistsValidator;
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
@RosettaMeta(model=C11Aux.class)
public class C11AuxMeta implements RosettaMetaData<C11Aux> {

	@Override
	public List<Validator<? super C11Aux>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C11Aux, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C11Aux> validator(ValidatorFactory factory) {
		return factory.<C11Aux>create(C11AuxValidator.class);
	}

	@Override
	public Validator<? super C11Aux> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C11Aux>create(C11AuxTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C11Aux> validator() {
		return new C11AuxValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C11Aux> typeFormatValidator() {
		return new C11AuxTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C11Aux, Set<String>> onlyExistsValidator() {
		return new C11AuxOnlyExistsValidator();
	}
}
