package chaos.s34.a3half.p1.meta;

import chaos.s34.a3half.p1.C34Aux;
import chaos.s34.a3half.p1.validation.C34AuxTypeFormatValidator;
import chaos.s34.a3half.p1.validation.C34AuxValidator;
import chaos.s34.a3half.p1.validation.exists.C34AuxOnlyExistsValidator;
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
@RosettaMeta(model=C34Aux.class)
public class C34AuxMeta implements RosettaMetaData<C34Aux> {

	@Override
	public List<Validator<? super C34Aux>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C34Aux, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C34Aux> validator(ValidatorFactory factory) {
		return factory.<C34Aux>create(C34AuxValidator.class);
	}

	@Override
	public Validator<? super C34Aux> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C34Aux>create(C34AuxTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C34Aux> validator() {
		return new C34AuxValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C34Aux> typeFormatValidator() {
		return new C34AuxTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C34Aux, Set<String>> onlyExistsValidator() {
		return new C34AuxOnlyExistsValidator();
	}
}
