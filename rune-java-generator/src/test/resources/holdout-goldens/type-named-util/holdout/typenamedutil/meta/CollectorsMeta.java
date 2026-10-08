package holdout.typenamedutil.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedutil.Collectors;
import holdout.typenamedutil.validation.CollectorsTypeFormatValidator;
import holdout.typenamedutil.validation.CollectorsValidator;
import holdout.typenamedutil.validation.exists.CollectorsOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Collectors.class)
public class CollectorsMeta implements RosettaMetaData<Collectors> {

	@Override
	public List<Validator<? super Collectors>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Collectors, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Collectors> validator(ValidatorFactory factory) {
		return factory.<Collectors>create(CollectorsValidator.class);
	}

	@Override
	public Validator<? super Collectors> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Collectors>create(CollectorsTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Collectors> validator() {
		return new CollectorsValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Collectors> typeFormatValidator() {
		return new CollectorsTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Collectors, Set<String>> onlyExistsValidator() {
		return new CollectorsOnlyExistsValidator();
	}
}
