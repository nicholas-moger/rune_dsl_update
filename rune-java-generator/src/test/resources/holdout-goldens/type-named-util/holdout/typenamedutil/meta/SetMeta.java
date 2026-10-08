package holdout.typenamedutil.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedutil.Set;
import holdout.typenamedutil.validation.SetTypeFormatValidator;
import holdout.typenamedutil.validation.SetValidator;
import holdout.typenamedutil.validation.exists.SetOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Set.class)
public class SetMeta implements RosettaMetaData<Set> {

	@Override
	public List<Validator<? super Set>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Set, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Set> validator(ValidatorFactory factory) {
		return factory.<Set>create(SetValidator.class);
	}

	@Override
	public Validator<? super Set> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Set>create(SetTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Set> validator() {
		return new SetValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Set> typeFormatValidator() {
		return new SetTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Set, java.util.Set<String>> onlyExistsValidator() {
		return new SetOnlyExistsValidator();
	}
}
