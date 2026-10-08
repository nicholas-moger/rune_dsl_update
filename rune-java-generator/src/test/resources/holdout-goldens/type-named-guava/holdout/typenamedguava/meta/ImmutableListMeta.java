package holdout.typenamedguava.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedguava.ImmutableList;
import holdout.typenamedguava.validation.ImmutableListTypeFormatValidator;
import holdout.typenamedguava.validation.ImmutableListValidator;
import holdout.typenamedguava.validation.exists.ImmutableListOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=ImmutableList.class)
public class ImmutableListMeta implements RosettaMetaData<ImmutableList> {

	@Override
	public List<Validator<? super ImmutableList>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super ImmutableList, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super ImmutableList> validator(ValidatorFactory factory) {
		return factory.<ImmutableList>create(ImmutableListValidator.class);
	}

	@Override
	public Validator<? super ImmutableList> typeFormatValidator(ValidatorFactory factory) {
		return factory.<ImmutableList>create(ImmutableListTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super ImmutableList> validator() {
		return new ImmutableListValidator();
	}

	@Deprecated
	@Override
	public Validator<? super ImmutableList> typeFormatValidator() {
		return new ImmutableListTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super ImmutableList, Set<String>> onlyExistsValidator() {
		return new ImmutableListOnlyExistsValidator();
	}
}
