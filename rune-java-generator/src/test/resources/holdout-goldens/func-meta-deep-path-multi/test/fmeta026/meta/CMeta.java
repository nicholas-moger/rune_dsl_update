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
import test.fmeta026.C;
import test.fmeta026.validation.CTypeFormatValidator;
import test.fmeta026.validation.CValidator;
import test.fmeta026.validation.exists.COnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=C.class)
public class CMeta implements RosettaMetaData<C> {

	@Override
	public List<Validator<? super C>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C> validator(ValidatorFactory factory) {
		return factory.<C>create(CValidator.class);
	}

	@Override
	public Validator<? super C> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C>create(CTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C> validator() {
		return new CValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C> typeFormatValidator() {
		return new CTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C, Set<String>> onlyExistsValidator() {
		return new COnlyExistsValidator();
	}
}
