package chaos.s25.a3third.p2.meta;

import chaos.s25.a3third.p2.C25Pick;
import chaos.s25.a3third.p2.validation.C25PickTypeFormatValidator;
import chaos.s25.a3third.p2.validation.C25PickValidator;
import chaos.s25.a3third.p2.validation.datarule.C25PickChoice;
import chaos.s25.a3third.p2.validation.exists.C25PickOnlyExistsValidator;
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
@RosettaMeta(model=C25Pick.class)
public class C25PickMeta implements RosettaMetaData<C25Pick> {

	@Override
	public List<Validator<? super C25Pick>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C25Pick>create(C25PickChoice.class)
		);
	}
	
	@Override
	public List<Function<? super C25Pick, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C25Pick> validator(ValidatorFactory factory) {
		return factory.<C25Pick>create(C25PickValidator.class);
	}

	@Override
	public Validator<? super C25Pick> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C25Pick>create(C25PickTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C25Pick> validator() {
		return new C25PickValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C25Pick> typeFormatValidator() {
		return new C25PickTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C25Pick, Set<String>> onlyExistsValidator() {
		return new C25PickOnlyExistsValidator();
	}
}
