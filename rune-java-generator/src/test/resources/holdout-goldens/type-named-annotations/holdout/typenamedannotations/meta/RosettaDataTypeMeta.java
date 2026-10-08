package holdout.typenamedannotations.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedannotations.RosettaDataType;
import holdout.typenamedannotations.validation.RosettaDataTypeTypeFormatValidator;
import holdout.typenamedannotations.validation.RosettaDataTypeValidator;
import holdout.typenamedannotations.validation.exists.RosettaDataTypeOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RosettaDataType.class)
public class RosettaDataTypeMeta implements RosettaMetaData<RosettaDataType> {

	@Override
	public List<Validator<? super RosettaDataType>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RosettaDataType, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RosettaDataType> validator(ValidatorFactory factory) {
		return factory.<RosettaDataType>create(RosettaDataTypeValidator.class);
	}

	@Override
	public Validator<? super RosettaDataType> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RosettaDataType>create(RosettaDataTypeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RosettaDataType> validator() {
		return new RosettaDataTypeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RosettaDataType> typeFormatValidator() {
		return new RosettaDataTypeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RosettaDataType, Set<String>> onlyExistsValidator() {
		return new RosettaDataTypeOnlyExistsValidator();
	}
}
