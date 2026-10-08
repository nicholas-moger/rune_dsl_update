package chaos.s21.a2alias.h.meta;

import chaos.s21.a2alias.h.C21Aux;
import chaos.s21.a2alias.h.validation.C21AuxTypeFormatValidator;
import chaos.s21.a2alias.h.validation.C21AuxValidator;
import chaos.s21.a2alias.h.validation.exists.C21AuxOnlyExistsValidator;
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
@RosettaMeta(model=C21Aux.class)
public class C21AuxMeta implements RosettaMetaData<C21Aux> {

	@Override
	public List<Validator<? super C21Aux>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C21Aux, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C21Aux> validator(ValidatorFactory factory) {
		return factory.<C21Aux>create(C21AuxValidator.class);
	}

	@Override
	public Validator<? super C21Aux> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C21Aux>create(C21AuxTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C21Aux> validator() {
		return new C21AuxValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C21Aux> typeFormatValidator() {
		return new C21AuxTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C21Aux, Set<String>> onlyExistsValidator() {
		return new C21AuxOnlyExistsValidator();
	}
}
