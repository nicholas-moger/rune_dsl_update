package holdout.typenamedannotations.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedannotations.Multi;
import holdout.typenamedannotations.validation.MultiTypeFormatValidator;
import holdout.typenamedannotations.validation.MultiValidator;
import holdout.typenamedannotations.validation.exists.MultiOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Multi.class)
public class MultiMeta implements RosettaMetaData<Multi> {

	@Override
	public List<Validator<? super Multi>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Multi, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Multi> validator(ValidatorFactory factory) {
		return factory.<Multi>create(MultiValidator.class);
	}

	@Override
	public Validator<? super Multi> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Multi>create(MultiTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Multi> validator() {
		return new MultiValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Multi> typeFormatValidator() {
		return new MultiTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Multi, Set<String>> onlyExistsValidator() {
		return new MultiOnlyExistsValidator();
	}
}
