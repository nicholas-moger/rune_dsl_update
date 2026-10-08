package chaos.s01.x13half.p1.meta;

import chaos.s01.x13half.p1.C1Times;
import chaos.s01.x13half.p1.validation.C1TimesTypeFormatValidator;
import chaos.s01.x13half.p1.validation.C1TimesValidator;
import chaos.s01.x13half.p1.validation.exists.C1TimesOnlyExistsValidator;
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
@RosettaMeta(model=C1Times.class)
public class C1TimesMeta implements RosettaMetaData<C1Times> {

	@Override
	public List<Validator<? super C1Times>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C1Times, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C1Times> validator(ValidatorFactory factory) {
		return factory.<C1Times>create(C1TimesValidator.class);
	}

	@Override
	public Validator<? super C1Times> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C1Times>create(C1TimesTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C1Times> validator() {
		return new C1TimesValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C1Times> typeFormatValidator() {
		return new C1TimesTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C1Times, Set<String>> onlyExistsValidator() {
		return new C1TimesOnlyExistsValidator();
	}
}
