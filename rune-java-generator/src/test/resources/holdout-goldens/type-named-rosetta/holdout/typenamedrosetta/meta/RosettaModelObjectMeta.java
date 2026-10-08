package holdout.typenamedrosetta.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.RosettaModelObject;
import holdout.typenamedrosetta.validation.RosettaModelObjectTypeFormatValidator;
import holdout.typenamedrosetta.validation.RosettaModelObjectValidator;
import holdout.typenamedrosetta.validation.exists.RosettaModelObjectOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RosettaModelObject.class)
public class RosettaModelObjectMeta implements RosettaMetaData<RosettaModelObject> {

	@Override
	public List<Validator<? super RosettaModelObject>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RosettaModelObject, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RosettaModelObject> validator(ValidatorFactory factory) {
		return factory.<RosettaModelObject>create(RosettaModelObjectValidator.class);
	}

	@Override
	public Validator<? super RosettaModelObject> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RosettaModelObject>create(RosettaModelObjectTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RosettaModelObject> validator() {
		return new RosettaModelObjectValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RosettaModelObject> typeFormatValidator() {
		return new RosettaModelObjectTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RosettaModelObject, Set<String>> onlyExistsValidator() {
		return new RosettaModelObjectOnlyExistsValidator();
	}
}
