package test.aliasheader.meta;

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
import test.aliasheader.Injected;
import test.aliasheader.validation.InjectedTypeFormatValidator;
import test.aliasheader.validation.InjectedValidator;
import test.aliasheader.validation.exists.InjectedOnlyExistsValidator;


/**
 * @version 0.0.0.test
 */
@RosettaMeta(model=Injected.class)
public class InjectedMeta implements RosettaMetaData<Injected> {

	@Override
	public List<Validator<? super Injected>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Injected, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Injected> validator(ValidatorFactory factory) {
		return factory.<Injected>create(InjectedValidator.class);
	}

	@Override
	public Validator<? super Injected> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Injected>create(InjectedTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Injected> validator() {
		return new InjectedValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Injected> typeFormatValidator() {
		return new InjectedTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Injected, Set<String>> onlyExistsValidator() {
		return new InjectedOnlyExistsValidator();
	}
}
