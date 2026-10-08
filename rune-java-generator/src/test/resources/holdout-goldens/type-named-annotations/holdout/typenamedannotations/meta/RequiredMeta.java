package holdout.typenamedannotations.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedannotations.Required;
import holdout.typenamedannotations.validation.RequiredTypeFormatValidator;
import holdout.typenamedannotations.validation.RequiredValidator;
import holdout.typenamedannotations.validation.exists.RequiredOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Required.class)
public class RequiredMeta implements RosettaMetaData<Required> {

	@Override
	public List<Validator<? super Required>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Required, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Required> validator(ValidatorFactory factory) {
		return factory.<Required>create(RequiredValidator.class);
	}

	@Override
	public Validator<? super Required> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Required>create(RequiredTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Required> validator() {
		return new RequiredValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Required> typeFormatValidator() {
		return new RequiredTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Required, Set<String>> onlyExistsValidator() {
		return new RequiredOnlyExistsValidator();
	}
}
