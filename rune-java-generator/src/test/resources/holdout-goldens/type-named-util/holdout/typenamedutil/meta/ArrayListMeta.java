package holdout.typenamedutil.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedutil.ArrayList;
import holdout.typenamedutil.validation.ArrayListTypeFormatValidator;
import holdout.typenamedutil.validation.ArrayListValidator;
import holdout.typenamedutil.validation.exists.ArrayListOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=ArrayList.class)
public class ArrayListMeta implements RosettaMetaData<ArrayList> {

	@Override
	public List<Validator<? super ArrayList>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super ArrayList, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super ArrayList> validator(ValidatorFactory factory) {
		return factory.<ArrayList>create(ArrayListValidator.class);
	}

	@Override
	public Validator<? super ArrayList> typeFormatValidator(ValidatorFactory factory) {
		return factory.<ArrayList>create(ArrayListTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super ArrayList> validator() {
		return new ArrayListValidator();
	}

	@Deprecated
	@Override
	public Validator<? super ArrayList> typeFormatValidator() {
		return new ArrayListTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super ArrayList, Set<String>> onlyExistsValidator() {
		return new ArrayListOnlyExistsValidator();
	}
}
