package test.fmeta026.meta;

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
import test.fmeta026.ABase;
import test.fmeta026.validation.ABaseTypeFormatValidator;
import test.fmeta026.validation.ABaseValidator;
import test.fmeta026.validation.exists.ABaseOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=ABase.class)
public class ABaseMeta implements RosettaMetaData<ABase> {

	@Override
	public List<Validator<? super ABase>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super ABase, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super ABase> validator(ValidatorFactory factory) {
		return factory.<ABase>create(ABaseValidator.class);
	}

	@Override
	public Validator<? super ABase> typeFormatValidator(ValidatorFactory factory) {
		return factory.<ABase>create(ABaseTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super ABase> validator() {
		return new ABaseValidator();
	}

	@Deprecated
	@Override
	public Validator<? super ABase> typeFormatValidator() {
		return new ABaseTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super ABase, Set<String>> onlyExistsValidator() {
		return new ABaseOnlyExistsValidator();
	}
}
