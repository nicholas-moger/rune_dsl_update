package test.qep.b.meta;

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
import test.qep.b.QepProduct;
import test.qep.b.functions.Qualify_QepP;
import test.qep.b.validation.QepProductTypeFormatValidator;
import test.qep.b.validation.QepProductValidator;
import test.qep.b.validation.exists.QepProductOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=QepProduct.class)
public class QepProductMeta implements RosettaMetaData<QepProduct> {

	@Override
	public List<Validator<? super QepProduct>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super QepProduct, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Arrays.asList(
			factory.<QepProduct>create(Qualify_QepP.class)
		);
	}
	
	@Override
	public Validator<? super QepProduct> validator(ValidatorFactory factory) {
		return factory.<QepProduct>create(QepProductValidator.class);
	}

	@Override
	public Validator<? super QepProduct> typeFormatValidator(ValidatorFactory factory) {
		return factory.<QepProduct>create(QepProductTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super QepProduct> validator() {
		return new QepProductValidator();
	}

	@Deprecated
	@Override
	public Validator<? super QepProduct> typeFormatValidator() {
		return new QepProductTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super QepProduct, Set<String>> onlyExistsValidator() {
		return new QepProductOnlyExistsValidator();
	}
}
