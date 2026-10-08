package test.reg.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import test.reg.Attribute;
import test.reg.validation.AttributeTypeFormatValidator;
import test.reg.validation.AttributeValidator;
import test.reg.validation.exists.AttributeOnlyExistsValidator;


/**
 * @version test
 */
@RosettaMeta(model=Attribute.class)
public class AttributeMeta implements RosettaMetaData<Attribute> {

	@Override
	public List<Validator<? super Attribute>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Attribute, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Attribute> validator(ValidatorFactory factory) {
		return factory.<Attribute>create(AttributeValidator.class);
	}

	@Override
	public Validator<? super Attribute> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Attribute>create(AttributeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Attribute> validator() {
		return new AttributeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Attribute> typeFormatValidator() {
		return new AttributeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Attribute, Set<String>> onlyExistsValidator() {
		return new AttributeOnlyExistsValidator();
	}
}
