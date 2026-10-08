package test.aliasfilescope.meta;

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
import test.aliasfilescope.WrittenSingle;
import test.aliasfilescope.validation.WrittenSingleTypeFormatValidator;
import test.aliasfilescope.validation.WrittenSingleValidator;
import test.aliasfilescope.validation.exists.WrittenSingleOnlyExistsValidator;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=WrittenSingle.class)
public class WrittenSingleMeta implements RosettaMetaData<WrittenSingle> {

	@Override
	public List<Validator<? super WrittenSingle>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super WrittenSingle, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super WrittenSingle> validator(ValidatorFactory factory) {
		return factory.<WrittenSingle>create(WrittenSingleValidator.class);
	}

	@Override
	public Validator<? super WrittenSingle> typeFormatValidator(ValidatorFactory factory) {
		return factory.<WrittenSingle>create(WrittenSingleTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super WrittenSingle> validator() {
		return new WrittenSingleValidator();
	}

	@Deprecated
	@Override
	public Validator<? super WrittenSingle> typeFormatValidator() {
		return new WrittenSingleTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super WrittenSingle, Set<String>> onlyExistsValidator() {
		return new WrittenSingleOnlyExistsValidator();
	}
}
