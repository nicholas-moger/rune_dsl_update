package chaos.s32.a8pkg.meta;

import chaos.s32.a8pkg.C32Aux;
import chaos.s32.a8pkg.validation.C32AuxTypeFormatValidator;
import chaos.s32.a8pkg.validation.C32AuxValidator;
import chaos.s32.a8pkg.validation.exists.C32AuxOnlyExistsValidator;
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
@RosettaMeta(model=C32Aux.class)
public class C32AuxMeta implements RosettaMetaData<C32Aux> {

	@Override
	public List<Validator<? super C32Aux>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C32Aux, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C32Aux> validator(ValidatorFactory factory) {
		return factory.<C32Aux>create(C32AuxValidator.class);
	}

	@Override
	public Validator<? super C32Aux> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C32Aux>create(C32AuxTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C32Aux> validator() {
		return new C32AuxValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C32Aux> typeFormatValidator() {
		return new C32AuxTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C32Aux, Set<String>> onlyExistsValidator() {
		return new C32AuxOnlyExistsValidator();
	}
}
