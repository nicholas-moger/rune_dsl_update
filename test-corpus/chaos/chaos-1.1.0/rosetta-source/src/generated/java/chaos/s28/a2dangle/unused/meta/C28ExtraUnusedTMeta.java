package chaos.s28.a2dangle.unused.meta;

import chaos.s28.a2dangle.unused.C28ExtraUnusedT;
import chaos.s28.a2dangle.unused.validation.C28ExtraUnusedTTypeFormatValidator;
import chaos.s28.a2dangle.unused.validation.C28ExtraUnusedTValidator;
import chaos.s28.a2dangle.unused.validation.exists.C28ExtraUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C28ExtraUnusedT.class)
public class C28ExtraUnusedTMeta implements RosettaMetaData<C28ExtraUnusedT> {

	@Override
	public List<Validator<? super C28ExtraUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C28ExtraUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C28ExtraUnusedT> validator(ValidatorFactory factory) {
		return factory.<C28ExtraUnusedT>create(C28ExtraUnusedTValidator.class);
	}

	@Override
	public Validator<? super C28ExtraUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C28ExtraUnusedT>create(C28ExtraUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C28ExtraUnusedT> validator() {
		return new C28ExtraUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C28ExtraUnusedT> typeFormatValidator() {
		return new C28ExtraUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C28ExtraUnusedT, Set<String>> onlyExistsValidator() {
		return new C28ExtraUnusedTOnlyExistsValidator();
	}
}
