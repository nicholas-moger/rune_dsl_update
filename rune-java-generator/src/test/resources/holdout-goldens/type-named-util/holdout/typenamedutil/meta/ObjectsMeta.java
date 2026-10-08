package holdout.typenamedutil.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedutil.Objects;
import holdout.typenamedutil.validation.ObjectsTypeFormatValidator;
import holdout.typenamedutil.validation.ObjectsValidator;
import holdout.typenamedutil.validation.exists.ObjectsOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Objects.class)
public class ObjectsMeta implements RosettaMetaData<Objects> {

	@Override
	public List<Validator<? super Objects>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Objects, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Objects> validator(ValidatorFactory factory) {
		return factory.<Objects>create(ObjectsValidator.class);
	}

	@Override
	public Validator<? super Objects> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Objects>create(ObjectsTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Objects> validator() {
		return new ObjectsValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Objects> typeFormatValidator() {
		return new ObjectsTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Objects, Set<String>> onlyExistsValidator() {
		return new ObjectsOnlyExistsValidator();
	}
}
