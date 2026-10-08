package holdout.typenamedrosetta.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.ListEquals;
import holdout.typenamedrosetta.validation.ListEqualsTypeFormatValidator;
import holdout.typenamedrosetta.validation.ListEqualsValidator;
import holdout.typenamedrosetta.validation.exists.ListEqualsOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=ListEquals.class)
public class ListEqualsMeta implements RosettaMetaData<ListEquals> {

	@Override
	public List<Validator<? super ListEquals>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super ListEquals, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super ListEquals> validator(ValidatorFactory factory) {
		return factory.<ListEquals>create(ListEqualsValidator.class);
	}

	@Override
	public Validator<? super ListEquals> typeFormatValidator(ValidatorFactory factory) {
		return factory.<ListEquals>create(ListEqualsTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super ListEquals> validator() {
		return new ListEqualsValidator();
	}

	@Deprecated
	@Override
	public Validator<? super ListEquals> typeFormatValidator() {
		return new ListEqualsTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super ListEquals, Set<String>> onlyExistsValidator() {
		return new ListEqualsOnlyExistsValidator();
	}
}
