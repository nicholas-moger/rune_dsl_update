package chaos.s14.a1o3.meta;

import chaos.s14.a1o3.C14Aux;
import chaos.s14.a1o3.validation.C14AuxTypeFormatValidator;
import chaos.s14.a1o3.validation.C14AuxValidator;
import chaos.s14.a1o3.validation.exists.C14AuxOnlyExistsValidator;
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
@RosettaMeta(model=C14Aux.class)
public class C14AuxMeta implements RosettaMetaData<C14Aux> {

	@Override
	public List<Validator<? super C14Aux>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C14Aux, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C14Aux> validator(ValidatorFactory factory) {
		return factory.<C14Aux>create(C14AuxValidator.class);
	}

	@Override
	public Validator<? super C14Aux> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C14Aux>create(C14AuxTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C14Aux> validator() {
		return new C14AuxValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C14Aux> typeFormatValidator() {
		return new C14AuxTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C14Aux, Set<String>> onlyExistsValidator() {
		return new C14AuxOnlyExistsValidator();
	}
}
