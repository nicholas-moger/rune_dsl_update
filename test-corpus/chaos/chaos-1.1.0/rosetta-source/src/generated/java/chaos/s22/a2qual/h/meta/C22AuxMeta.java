package chaos.s22.a2qual.h.meta;

import chaos.s22.a2qual.h.C22Aux;
import chaos.s22.a2qual.h.validation.C22AuxTypeFormatValidator;
import chaos.s22.a2qual.h.validation.C22AuxValidator;
import chaos.s22.a2qual.h.validation.exists.C22AuxOnlyExistsValidator;
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
@RosettaMeta(model=C22Aux.class)
public class C22AuxMeta implements RosettaMetaData<C22Aux> {

	@Override
	public List<Validator<? super C22Aux>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C22Aux, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C22Aux> validator(ValidatorFactory factory) {
		return factory.<C22Aux>create(C22AuxValidator.class);
	}

	@Override
	public Validator<? super C22Aux> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C22Aux>create(C22AuxTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C22Aux> validator() {
		return new C22AuxValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C22Aux> typeFormatValidator() {
		return new C22AuxTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C22Aux, Set<String>> onlyExistsValidator() {
		return new C22AuxOnlyExistsValidator();
	}
}
