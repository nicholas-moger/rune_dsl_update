package test.aliasreserved.meta;

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
import test.aliasreserved.Keywords;
import test.aliasreserved.validation.KeywordsTypeFormatValidator;
import test.aliasreserved.validation.KeywordsValidator;
import test.aliasreserved.validation.exists.KeywordsOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Keywords.class)
public class KeywordsMeta implements RosettaMetaData<Keywords> {

	@Override
	public List<Validator<? super Keywords>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Keywords, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Keywords> validator(ValidatorFactory factory) {
		return factory.<Keywords>create(KeywordsValidator.class);
	}

	@Override
	public Validator<? super Keywords> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Keywords>create(KeywordsTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Keywords> validator() {
		return new KeywordsValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Keywords> typeFormatValidator() {
		return new KeywordsTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Keywords, Set<String>> onlyExistsValidator() {
		return new KeywordsOnlyExistsValidator();
	}
}
