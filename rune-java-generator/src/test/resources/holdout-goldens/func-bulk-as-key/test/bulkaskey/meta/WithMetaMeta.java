package test.bulkaskey.meta;

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
import test.bulkaskey.WithMeta;
import test.bulkaskey.validation.WithMetaTypeFormatValidator;
import test.bulkaskey.validation.WithMetaValidator;
import test.bulkaskey.validation.exists.WithMetaOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=WithMeta.class)
public class WithMetaMeta implements RosettaMetaData<WithMeta> {

	@Override
	public List<Validator<? super WithMeta>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super WithMeta, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super WithMeta> validator(ValidatorFactory factory) {
		return factory.<WithMeta>create(WithMetaValidator.class);
	}

	@Override
	public Validator<? super WithMeta> typeFormatValidator(ValidatorFactory factory) {
		return factory.<WithMeta>create(WithMetaTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super WithMeta> validator() {
		return new WithMetaValidator();
	}

	@Deprecated
	@Override
	public Validator<? super WithMeta> typeFormatValidator() {
		return new WithMetaTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super WithMeta, Set<String>> onlyExistsValidator() {
		return new WithMetaOnlyExistsValidator();
	}
}
