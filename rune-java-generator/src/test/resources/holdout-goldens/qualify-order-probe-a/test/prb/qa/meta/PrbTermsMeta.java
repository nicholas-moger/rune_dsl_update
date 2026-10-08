package test.prb.qa.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import test.prb.qa.PrbTerms;
import test.prb.qa.functions.Qualify_PrbQA;
import test.prb.qa.validation.PrbTermsTypeFormatValidator;
import test.prb.qa.validation.PrbTermsValidator;
import test.prb.qa.validation.exists.PrbTermsOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=PrbTerms.class)
public class PrbTermsMeta implements RosettaMetaData<PrbTerms> {

	@Override
	public List<Validator<? super PrbTerms>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super PrbTerms, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Arrays.asList(
			factory.<PrbTerms>create(Qualify_PrbQA.class)
		);
	}
	
	@Override
	public Validator<? super PrbTerms> validator(ValidatorFactory factory) {
		return factory.<PrbTerms>create(PrbTermsValidator.class);
	}

	@Override
	public Validator<? super PrbTerms> typeFormatValidator(ValidatorFactory factory) {
		return factory.<PrbTerms>create(PrbTermsTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super PrbTerms> validator() {
		return new PrbTermsValidator();
	}

	@Deprecated
	@Override
	public Validator<? super PrbTerms> typeFormatValidator() {
		return new PrbTermsTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super PrbTerms, Set<String>> onlyExistsValidator() {
		return new PrbTermsOnlyExistsValidator();
	}
}
