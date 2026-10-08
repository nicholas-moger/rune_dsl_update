package chaos.s07.a3hub.p1.meta;

import chaos.s07.a3hub.p1.C7Extra;
import chaos.s07.a3hub.p1.validation.C7ExtraTypeFormatValidator;
import chaos.s07.a3hub.p1.validation.C7ExtraValidator;
import chaos.s07.a3hub.p1.validation.exists.C7ExtraOnlyExistsValidator;
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
@RosettaMeta(model=C7Extra.class)
public class C7ExtraMeta implements RosettaMetaData<C7Extra> {

	@Override
	public List<Validator<? super C7Extra>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C7Extra, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C7Extra> validator(ValidatorFactory factory) {
		return factory.<C7Extra>create(C7ExtraValidator.class);
	}

	@Override
	public Validator<? super C7Extra> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C7Extra>create(C7ExtraTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C7Extra> validator() {
		return new C7ExtraValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C7Extra> typeFormatValidator() {
		return new C7ExtraTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C7Extra, Set<String>> onlyExistsValidator() {
		return new C7ExtraOnlyExistsValidator();
	}
}
