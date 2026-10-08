package chaos.s07.a2dangle.unused.meta;

import chaos.s07.a2dangle.unused.C7ExtraUnusedT;
import chaos.s07.a2dangle.unused.validation.C7ExtraUnusedTTypeFormatValidator;
import chaos.s07.a2dangle.unused.validation.C7ExtraUnusedTValidator;
import chaos.s07.a2dangle.unused.validation.exists.C7ExtraUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C7ExtraUnusedT.class)
public class C7ExtraUnusedTMeta implements RosettaMetaData<C7ExtraUnusedT> {

	@Override
	public List<Validator<? super C7ExtraUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C7ExtraUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C7ExtraUnusedT> validator(ValidatorFactory factory) {
		return factory.<C7ExtraUnusedT>create(C7ExtraUnusedTValidator.class);
	}

	@Override
	public Validator<? super C7ExtraUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C7ExtraUnusedT>create(C7ExtraUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C7ExtraUnusedT> validator() {
		return new C7ExtraUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C7ExtraUnusedT> typeFormatValidator() {
		return new C7ExtraUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C7ExtraUnusedT, Set<String>> onlyExistsValidator() {
		return new C7ExtraUnusedTOnlyExistsValidator();
	}
}
