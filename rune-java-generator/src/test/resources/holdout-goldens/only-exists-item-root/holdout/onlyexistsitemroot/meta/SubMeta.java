package holdout.onlyexistsitemroot.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.onlyexistsitemroot.Sub;
import holdout.onlyexistsitemroot.validation.SubTypeFormatValidator;
import holdout.onlyexistsitemroot.validation.SubValidator;
import holdout.onlyexistsitemroot.validation.exists.SubOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Sub.class)
public class SubMeta implements RosettaMetaData<Sub> {

	@Override
	public List<Validator<? super Sub>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Sub, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Sub> validator(ValidatorFactory factory) {
		return factory.<Sub>create(SubValidator.class);
	}

	@Override
	public Validator<? super Sub> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Sub>create(SubTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Sub> validator() {
		return new SubValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Sub> typeFormatValidator() {
		return new SubTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Sub, Set<String>> onlyExistsValidator() {
		return new SubOnlyExistsValidator();
	}
}
