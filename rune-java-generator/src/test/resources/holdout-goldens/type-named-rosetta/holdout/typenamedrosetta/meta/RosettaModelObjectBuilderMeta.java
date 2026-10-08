package holdout.typenamedrosetta.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedrosetta.RosettaModelObjectBuilder;
import holdout.typenamedrosetta.validation.RosettaModelObjectBuilderTypeFormatValidator;
import holdout.typenamedrosetta.validation.RosettaModelObjectBuilderValidator;
import holdout.typenamedrosetta.validation.exists.RosettaModelObjectBuilderOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RosettaModelObjectBuilder.class)
public class RosettaModelObjectBuilderMeta implements RosettaMetaData<RosettaModelObjectBuilder> {

	@Override
	public List<Validator<? super RosettaModelObjectBuilder>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RosettaModelObjectBuilder, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RosettaModelObjectBuilder> validator(ValidatorFactory factory) {
		return factory.<RosettaModelObjectBuilder>create(RosettaModelObjectBuilderValidator.class);
	}

	@Override
	public Validator<? super RosettaModelObjectBuilder> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RosettaModelObjectBuilder>create(RosettaModelObjectBuilderTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RosettaModelObjectBuilder> validator() {
		return new RosettaModelObjectBuilderValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RosettaModelObjectBuilder> typeFormatValidator() {
		return new RosettaModelObjectBuilderTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RosettaModelObjectBuilder, Set<String>> onlyExistsValidator() {
		return new RosettaModelObjectBuilderOnlyExistsValidator();
	}
}
