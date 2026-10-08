package holdout.typenamedrosetta.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.RosettaMetaData;
import holdout.typenamedrosetta.validation.RosettaMetaDataTypeFormatValidator;
import holdout.typenamedrosetta.validation.RosettaMetaDataValidator;
import holdout.typenamedrosetta.validation.exists.RosettaMetaDataOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RosettaMetaData.class)
public class RosettaMetaDataMeta implements com.rosetta.model.lib.meta.RosettaMetaData<RosettaMetaData> {

	@Override
	public List<Validator<? super RosettaMetaData>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RosettaMetaData, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RosettaMetaData> validator(ValidatorFactory factory) {
		return factory.<RosettaMetaData>create(RosettaMetaDataValidator.class);
	}

	@Override
	public Validator<? super RosettaMetaData> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RosettaMetaData>create(RosettaMetaDataTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RosettaMetaData> validator() {
		return new RosettaMetaDataValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RosettaMetaData> typeFormatValidator() {
		return new RosettaMetaDataTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RosettaMetaData, Set<String>> onlyExistsValidator() {
		return new RosettaMetaDataOnlyExistsValidator();
	}
}
