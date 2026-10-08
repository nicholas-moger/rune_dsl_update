package chaos.s29.a3third.p1.meta;

import chaos.s29.a3third.p1.C29Inner;
import chaos.s29.a3third.p1.validation.C29InnerTypeFormatValidator;
import chaos.s29.a3third.p1.validation.C29InnerValidator;
import chaos.s29.a3third.p1.validation.datarule.C29InnerChoice;
import chaos.s29.a3third.p1.validation.exists.C29InnerOnlyExistsValidator;
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
@RosettaMeta(model=C29Inner.class)
public class C29InnerMeta implements RosettaMetaData<C29Inner> {

	@Override
	public List<Validator<? super C29Inner>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<C29Inner>create(C29InnerChoice.class)
		);
	}
	
	@Override
	public List<Function<? super C29Inner, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C29Inner> validator(ValidatorFactory factory) {
		return factory.<C29Inner>create(C29InnerValidator.class);
	}

	@Override
	public Validator<? super C29Inner> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C29Inner>create(C29InnerTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C29Inner> validator() {
		return new C29InnerValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C29Inner> typeFormatValidator() {
		return new C29InnerTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C29Inner, Set<String>> onlyExistsValidator() {
		return new C29InnerOnlyExistsValidator();
	}
}
