package holdout.typenamedutil.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedutil.Collections;
import holdout.typenamedutil.validation.CollectionsTypeFormatValidator;
import holdout.typenamedutil.validation.CollectionsValidator;
import holdout.typenamedutil.validation.exists.CollectionsOnlyExistsValidator;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Collections.class)
public class CollectionsMeta implements RosettaMetaData<Collections> {

	@Override
	public List<Validator<? super Collections>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Collections, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return java.util.Collections.emptyList();
	}
	
	@Override
	public Validator<? super Collections> validator(ValidatorFactory factory) {
		return factory.<Collections>create(CollectionsValidator.class);
	}

	@Override
	public Validator<? super Collections> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Collections>create(CollectionsTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Collections> validator() {
		return new CollectionsValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Collections> typeFormatValidator() {
		return new CollectionsTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Collections, Set<String>> onlyExistsValidator() {
		return new CollectionsOnlyExistsValidator();
	}
}
