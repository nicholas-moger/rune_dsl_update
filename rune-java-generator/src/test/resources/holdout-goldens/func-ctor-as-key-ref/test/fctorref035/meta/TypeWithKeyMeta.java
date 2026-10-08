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
import test.fctorref035.TypeWithKey;
import test.fctorref035.validation.TypeWithKeyTypeFormatValidator;
import test.fctorref035.validation.TypeWithKeyValidator;
import test.fctorref035.validation.exists.TypeWithKeyOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=TypeWithKey.class)
public class TypeWithKeyMeta implements RosettaMetaData<TypeWithKey> {

	@Override
	public List<Validator<? super TypeWithKey>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super TypeWithKey, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super TypeWithKey> validator(ValidatorFactory factory) {
		return factory.<TypeWithKey>create(TypeWithKeyValidator.class);
	}

	@Override
	public Validator<? super TypeWithKey> typeFormatValidator(ValidatorFactory factory) {
		return factory.<TypeWithKey>create(TypeWithKeyTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super TypeWithKey> validator() {
		return new TypeWithKeyValidator();
	}

	@Deprecated
	@Override
	public Validator<? super TypeWithKey> typeFormatValidator() {
		return new TypeWithKeyTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super TypeWithKey, Set<String>> onlyExistsValidator() {
		return new TypeWithKeyOnlyExistsValidator();
	}
}
