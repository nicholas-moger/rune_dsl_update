package holdout.typenamedlist.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedlist.HolderModelFirst;
import holdout.typenamedlist.validation.HolderModelFirstTypeFormatValidator;
import holdout.typenamedlist.validation.HolderModelFirstValidator;
import holdout.typenamedlist.validation.exists.HolderModelFirstOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=HolderModelFirst.class)
public class HolderModelFirstMeta implements RosettaMetaData<HolderModelFirst> {

	@Override
	public List<Validator<? super HolderModelFirst>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super HolderModelFirst, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super HolderModelFirst> validator(ValidatorFactory factory) {
		return factory.<HolderModelFirst>create(HolderModelFirstValidator.class);
	}

	@Override
	public Validator<? super HolderModelFirst> typeFormatValidator(ValidatorFactory factory) {
		return factory.<HolderModelFirst>create(HolderModelFirstTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super HolderModelFirst> validator() {
		return new HolderModelFirstValidator();
	}

	@Deprecated
	@Override
	public Validator<? super HolderModelFirst> typeFormatValidator() {
		return new HolderModelFirstTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super HolderModelFirst, Set<String>> onlyExistsValidator() {
		return new HolderModelFirstOnlyExistsValidator();
	}
}
