package holdout.typenamedguava.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedguava.Lists;
import holdout.typenamedguava.validation.ListsTypeFormatValidator;
import holdout.typenamedguava.validation.ListsValidator;
import holdout.typenamedguava.validation.exists.ListsOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Lists.class)
public class ListsMeta implements RosettaMetaData<Lists> {

	@Override
	public List<Validator<? super Lists>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Lists, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Lists> validator(ValidatorFactory factory) {
		return factory.<Lists>create(ListsValidator.class);
	}

	@Override
	public Validator<? super Lists> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Lists>create(ListsTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Lists> validator() {
		return new ListsValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Lists> typeFormatValidator() {
		return new ListsTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Lists, Set<String>> onlyExistsValidator() {
		return new ListsOnlyExistsValidator();
	}
}
