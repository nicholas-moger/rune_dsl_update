package test.fctorref035.meta;

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
import test.fctorref035.OtherType;
import test.fctorref035.validation.OtherTypeTypeFormatValidator;
import test.fctorref035.validation.OtherTypeValidator;
import test.fctorref035.validation.exists.OtherTypeOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=OtherType.class)
public class OtherTypeMeta implements RosettaMetaData<OtherType> {

	@Override
	public List<Validator<? super OtherType>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super OtherType, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super OtherType> validator(ValidatorFactory factory) {
		return factory.<OtherType>create(OtherTypeValidator.class);
	}

	@Override
	public Validator<? super OtherType> typeFormatValidator(ValidatorFactory factory) {
		return factory.<OtherType>create(OtherTypeTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super OtherType> validator() {
		return new OtherTypeValidator();
	}

	@Deprecated
	@Override
	public Validator<? super OtherType> typeFormatValidator() {
		return new OtherTypeTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super OtherType, Set<String>> onlyExistsValidator() {
		return new OtherTypeOnlyExistsValidator();
	}
}
