package test.qcn.a.meta;

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
import test.qcn.a.QcnTerms;
import test.qcn.a.functions.Qualify_QcnA;
import test.qcn.a.validation.QcnTermsTypeFormatValidator;
import test.qcn.a.validation.QcnTermsValidator;
import test.qcn.a.validation.exists.QcnTermsOnlyExistsValidator;
import test.qcn.b.functions.Qualify_QcnB;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=QcnTerms.class)
public class QcnTermsMeta implements RosettaMetaData<QcnTerms> {

	@Override
	public List<Validator<? super QcnTerms>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super QcnTerms, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Arrays.asList(
			factory.<QcnTerms>create(Qualify_QcnA.class),
			factory.<QcnTerms>create(Qualify_QcnB.class)
		);
	}
	
	@Override
	public Validator<? super QcnTerms> validator(ValidatorFactory factory) {
		return factory.<QcnTerms>create(QcnTermsValidator.class);
	}

	@Override
	public Validator<? super QcnTerms> typeFormatValidator(ValidatorFactory factory) {
		return factory.<QcnTerms>create(QcnTermsTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super QcnTerms> validator() {
		return new QcnTermsValidator();
	}

	@Deprecated
	@Override
	public Validator<? super QcnTerms> typeFormatValidator() {
		return new QcnTermsTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super QcnTerms, Set<String>> onlyExistsValidator() {
		return new QcnTermsOnlyExistsValidator();
	}
}
