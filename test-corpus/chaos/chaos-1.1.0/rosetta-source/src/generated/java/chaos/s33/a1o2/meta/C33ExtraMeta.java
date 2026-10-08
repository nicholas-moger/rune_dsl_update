package chaos.s33.a1o2.meta;

import chaos.s33.a1o2.C33Extra;
import chaos.s33.a1o2.validation.C33ExtraTypeFormatValidator;
import chaos.s33.a1o2.validation.C33ExtraValidator;
import chaos.s33.a1o2.validation.exists.C33ExtraOnlyExistsValidator;
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
@RosettaMeta(model=C33Extra.class)
public class C33ExtraMeta implements RosettaMetaData<C33Extra> {

	@Override
	public List<Validator<? super C33Extra>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C33Extra, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C33Extra> validator(ValidatorFactory factory) {
		return factory.<C33Extra>create(C33ExtraValidator.class);
	}

	@Override
	public Validator<? super C33Extra> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C33Extra>create(C33ExtraTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C33Extra> validator() {
		return new C33ExtraValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C33Extra> typeFormatValidator() {
		return new C33ExtraTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C33Extra, Set<String>> onlyExistsValidator() {
		return new C33ExtraOnlyExistsValidator();
	}
}
