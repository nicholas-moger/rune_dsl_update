package holdout.typenamedannotations.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedannotations.AccessorType;
import holdout.typenamedannotations.validation.AccessorTypeTypeFormatValidator;
import holdout.typenamedannotations.validation.AccessorTypeValidator;
import holdout.typenamedannotations.validation.exists.AccessorTypeOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=AccessorType.class)
public class AccessorTypeMeta implements RosettaMetaData<AccessorType> {

	@Override
	public List<Validator<? super AccessorType>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super AccessorType, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super AccessorType> validator(ValidatorFactory factory) {
		return factory.<AccessorType>create(AccessorTypeValidator.class);
	}

	@Override
	public Validator<? super AccessorType> typeFormatValidator(ValidatorFactory factory) {
		return factory.<AccessorType>create(AccessorTypeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super AccessorType> validator() {
		return new AccessorTypeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super AccessorType> typeFormatValidator() {
		return new AccessorTypeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super AccessorType, Set<String>> onlyExistsValidator() {
		return new AccessorTypeOnlyExistsValidator();
	}
}
