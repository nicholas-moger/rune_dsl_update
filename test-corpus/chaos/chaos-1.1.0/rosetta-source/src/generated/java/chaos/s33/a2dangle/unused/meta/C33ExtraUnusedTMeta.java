package chaos.s33.a2dangle.unused.meta;

import chaos.s33.a2dangle.unused.C33ExtraUnusedT;
import chaos.s33.a2dangle.unused.validation.C33ExtraUnusedTTypeFormatValidator;
import chaos.s33.a2dangle.unused.validation.C33ExtraUnusedTValidator;
import chaos.s33.a2dangle.unused.validation.exists.C33ExtraUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C33ExtraUnusedT.class)
public class C33ExtraUnusedTMeta implements RosettaMetaData<C33ExtraUnusedT> {

	@Override
	public List<Validator<? super C33ExtraUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C33ExtraUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C33ExtraUnusedT> validator(ValidatorFactory factory) {
		return factory.<C33ExtraUnusedT>create(C33ExtraUnusedTValidator.class);
	}

	@Override
	public Validator<? super C33ExtraUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C33ExtraUnusedT>create(C33ExtraUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C33ExtraUnusedT> validator() {
		return new C33ExtraUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C33ExtraUnusedT> typeFormatValidator() {
		return new C33ExtraUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C33ExtraUnusedT, Set<String>> onlyExistsValidator() {
		return new C33ExtraUnusedTOnlyExistsValidator();
	}
}
