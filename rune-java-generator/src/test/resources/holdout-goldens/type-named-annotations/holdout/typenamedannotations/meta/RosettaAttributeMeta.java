package holdout.typenamedannotations.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.typenamedannotations.RosettaAttribute;
import holdout.typenamedannotations.validation.RosettaAttributeTypeFormatValidator;
import holdout.typenamedannotations.validation.RosettaAttributeValidator;
import holdout.typenamedannotations.validation.exists.RosettaAttributeOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=RosettaAttribute.class)
public class RosettaAttributeMeta implements RosettaMetaData<RosettaAttribute> {

	@Override
	public List<Validator<? super RosettaAttribute>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RosettaAttribute, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RosettaAttribute> validator(ValidatorFactory factory) {
		return factory.<RosettaAttribute>create(RosettaAttributeValidator.class);
	}

	@Override
	public Validator<? super RosettaAttribute> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RosettaAttribute>create(RosettaAttributeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RosettaAttribute> validator() {
		return new RosettaAttributeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RosettaAttribute> typeFormatValidator() {
		return new RosettaAttributeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RosettaAttribute, Set<String>> onlyExistsValidator() {
		return new RosettaAttributeOnlyExistsValidator();
	}
}
