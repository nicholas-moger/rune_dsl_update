package holdout.typenamedlist.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedlist.List;
import holdout.typenamedlist.validation.ListTypeFormatValidator;
import holdout.typenamedlist.validation.ListValidator;
import holdout.typenamedlist.validation.datarule.ListNonEmpty;
import holdout.typenamedlist.validation.exists.ListOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=List.class)
public class ListMeta implements RosettaMetaData<List> {

	@Override
	public java.util.List<Validator<? super List>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<List>create(ListNonEmpty.class)
		);
	}
	
	@Override
	public java.util.List<Function<? super List, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super List> validator(ValidatorFactory factory) {
		return factory.<List>create(ListValidator.class);
	}

	@Override
	public Validator<? super List> typeFormatValidator(ValidatorFactory factory) {
		return factory.<List>create(ListTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super List> validator() {
		return new ListValidator();
	}

	@Deprecated
	@Override
	public Validator<? super List> typeFormatValidator() {
		return new ListTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super List, Set<String>> onlyExistsValidator() {
		return new ListOnlyExistsValidator();
	}
}
