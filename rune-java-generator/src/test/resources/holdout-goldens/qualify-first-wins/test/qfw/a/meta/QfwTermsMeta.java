package test.qfw.a.meta;

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
import test.qfw.a.QfwTerms;
import test.qfw.a.validation.QfwTermsTypeFormatValidator;
import test.qfw.a.validation.QfwTermsValidator;
import test.qfw.a.validation.exists.QfwTermsOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=QfwTerms.class)
public class QfwTermsMeta implements RosettaMetaData<QfwTerms> {

	@Override
	public List<Validator<? super QfwTerms>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super QfwTerms, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super QfwTerms> validator(ValidatorFactory factory) {
		return factory.<QfwTerms>create(QfwTermsValidator.class);
	}

	@Override
	public Validator<? super QfwTerms> typeFormatValidator(ValidatorFactory factory) {
		return factory.<QfwTerms>create(QfwTermsTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super QfwTerms> validator() {
		return new QfwTermsValidator();
	}

	@Deprecated
	@Override
	public Validator<? super QfwTerms> typeFormatValidator() {
		return new QfwTermsTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super QfwTerms, Set<String>> onlyExistsValidator() {
		return new QfwTermsOnlyExistsValidator();
	}
}
