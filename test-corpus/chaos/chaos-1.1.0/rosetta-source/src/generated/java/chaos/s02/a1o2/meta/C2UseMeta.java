package chaos.s02.a1o2.meta;

import chaos.s02.a1o2.C2Use;
import chaos.s02.a1o2.validation.C2UseTypeFormatValidator;
import chaos.s02.a1o2.validation.C2UseValidator;
import chaos.s02.a1o2.validation.datarule.C2UseC2HasAlpha;
import chaos.s02.a1o2.validation.datarule.C2UseC2NotHold;
import chaos.s02.a1o2.validation.exists.C2UseOnlyExistsValidator;
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
@RosettaMeta(model=C2Use.class)
public class C2UseMeta implements RosettaMetaData<C2Use> {

	@Override
	public List<Validator<? super C2Use>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C2Use>create(C2UseC2NotHold.class),
			factory.<C2Use>create(C2UseC2HasAlpha.class)
		);
	}
	
	@Override
	public List<Function<? super C2Use, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C2Use> validator(ValidatorFactory factory) {
		return factory.<C2Use>create(C2UseValidator.class);
	}

	@Override
	public Validator<? super C2Use> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C2Use>create(C2UseTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C2Use> validator() {
		return new C2UseValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C2Use> typeFormatValidator() {
		return new C2UseTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C2Use, Set<String>> onlyExistsValidator() {
		return new C2UseOnlyExistsValidator();
	}
}
