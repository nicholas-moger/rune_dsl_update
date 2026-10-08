package holdout.typenamedannotations.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedannotations.RuneDataType;
import holdout.typenamedannotations.validation.RuneDataTypeTypeFormatValidator;
import holdout.typenamedannotations.validation.RuneDataTypeValidator;
import holdout.typenamedannotations.validation.exists.RuneDataTypeOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RuneDataType.class)
public class RuneDataTypeMeta implements RosettaMetaData<RuneDataType> {

	@Override
	public List<Validator<? super RuneDataType>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RuneDataType, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RuneDataType> validator(ValidatorFactory factory) {
		return factory.<RuneDataType>create(RuneDataTypeValidator.class);
	}

	@Override
	public Validator<? super RuneDataType> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RuneDataType>create(RuneDataTypeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RuneDataType> validator() {
		return new RuneDataTypeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RuneDataType> typeFormatValidator() {
		return new RuneDataTypeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RuneDataType, Set<String>> onlyExistsValidator() {
		return new RuneDataTypeOnlyExistsValidator();
	}
}
