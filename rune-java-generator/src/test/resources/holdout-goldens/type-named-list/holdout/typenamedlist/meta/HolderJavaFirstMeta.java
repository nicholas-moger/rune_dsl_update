package holdout.typenamedlist.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedlist.HolderJavaFirst;
import holdout.typenamedlist.validation.HolderJavaFirstTypeFormatValidator;
import holdout.typenamedlist.validation.HolderJavaFirstValidator;
import holdout.typenamedlist.validation.exists.HolderJavaFirstOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=HolderJavaFirst.class)
public class HolderJavaFirstMeta implements RosettaMetaData<HolderJavaFirst> {

	@Override
	public List<Validator<? super HolderJavaFirst>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super HolderJavaFirst, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super HolderJavaFirst> validator(ValidatorFactory factory) {
		return factory.<HolderJavaFirst>create(HolderJavaFirstValidator.class);
	}

	@Override
	public Validator<? super HolderJavaFirst> typeFormatValidator(ValidatorFactory factory) {
		return factory.<HolderJavaFirst>create(HolderJavaFirstTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super HolderJavaFirst> validator() {
		return new HolderJavaFirstValidator();
	}

	@Deprecated
	@Override
	public Validator<? super HolderJavaFirst> typeFormatValidator() {
		return new HolderJavaFirstTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super HolderJavaFirst, Set<String>> onlyExistsValidator() {
		return new HolderJavaFirstOnlyExistsValidator();
	}
}
