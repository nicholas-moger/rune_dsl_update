package chaos.s13.a1o2.meta;

import chaos.s13.a1o2.C13Aux;
import chaos.s13.a1o2.validation.C13AuxTypeFormatValidator;
import chaos.s13.a1o2.validation.C13AuxValidator;
import chaos.s13.a1o2.validation.exists.C13AuxOnlyExistsValidator;
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
@RosettaMeta(model=C13Aux.class)
public class C13AuxMeta implements RosettaMetaData<C13Aux> {

	@Override
	public List<Validator<? super C13Aux>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C13Aux, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C13Aux> validator(ValidatorFactory factory) {
		return factory.<C13Aux>create(C13AuxValidator.class);
	}

	@Override
	public Validator<? super C13Aux> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C13Aux>create(C13AuxTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C13Aux> validator() {
		return new C13AuxValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C13Aux> typeFormatValidator() {
		return new C13AuxTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C13Aux, Set<String>> onlyExistsValidator() {
		return new C13AuxOnlyExistsValidator();
	}
}
