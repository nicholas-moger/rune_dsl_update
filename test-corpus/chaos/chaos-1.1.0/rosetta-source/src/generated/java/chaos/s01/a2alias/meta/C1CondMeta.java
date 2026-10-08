package chaos.s01.a2alias.meta;

import chaos.s01.a2alias.C1Cond;
import chaos.s01.a2alias.validation.C1CondTypeFormatValidator;
import chaos.s01.a2alias.validation.C1CondValidator;
import chaos.s01.a2alias.validation.datarule.C1CondC1Bounds;
import chaos.s01.a2alias.validation.datarule.C1CondDataRule1;
import chaos.s01.a2alias.validation.exists.C1CondOnlyExistsValidator;
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
@RosettaMeta(model=C1Cond.class)
public class C1CondMeta implements RosettaMetaData<C1Cond> {

	@Override
	public List<Validator<? super C1Cond>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C1Cond>create(C1CondC1Bounds.class),
			factory.<C1Cond>create(C1CondDataRule1.class)
		);
	}
	
	@Override
	public List<Function<? super C1Cond, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C1Cond> validator(ValidatorFactory factory) {
		return factory.<C1Cond>create(C1CondValidator.class);
	}

	@Override
	public Validator<? super C1Cond> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C1Cond>create(C1CondTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C1Cond> validator() {
		return new C1CondValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C1Cond> typeFormatValidator() {
		return new C1CondTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C1Cond, Set<String>> onlyExistsValidator() {
		return new C1CondOnlyExistsValidator();
	}
}
