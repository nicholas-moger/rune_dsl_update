package chaos.s15.a3hub.p1.meta;

import chaos.s15.a3hub.p1.C15Aux;
import chaos.s15.a3hub.p1.validation.C15AuxTypeFormatValidator;
import chaos.s15.a3hub.p1.validation.C15AuxValidator;
import chaos.s15.a3hub.p1.validation.exists.C15AuxOnlyExistsValidator;
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
@RosettaMeta(model=C15Aux.class)
public class C15AuxMeta implements RosettaMetaData<C15Aux> {

	@Override
	public List<Validator<? super C15Aux>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C15Aux, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C15Aux> validator(ValidatorFactory factory) {
		return factory.<C15Aux>create(C15AuxValidator.class);
	}

	@Override
	public Validator<? super C15Aux> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C15Aux>create(C15AuxTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C15Aux> validator() {
		return new C15AuxValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C15Aux> typeFormatValidator() {
		return new C15AuxTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C15Aux, Set<String>> onlyExistsValidator() {
		return new C15AuxOnlyExistsValidator();
	}
}
