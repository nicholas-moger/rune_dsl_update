package holdout.onlyexistsitemroot.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.onlyexistsitemroot.OptB;
import holdout.onlyexistsitemroot.validation.OptBTypeFormatValidator;
import holdout.onlyexistsitemroot.validation.OptBValidator;
import holdout.onlyexistsitemroot.validation.exists.OptBOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=OptB.class)
public class OptBMeta implements RosettaMetaData<OptB> {

	@Override
	public List<Validator<? super OptB>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super OptB, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super OptB> validator(ValidatorFactory factory) {
		return factory.<OptB>create(OptBValidator.class);
	}

	@Override
	public Validator<? super OptB> typeFormatValidator(ValidatorFactory factory) {
		return factory.<OptB>create(OptBTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super OptB> validator() {
		return new OptBValidator();
	}

	@Deprecated
	@Override
	public Validator<? super OptB> typeFormatValidator() {
		return new OptBTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super OptB, Set<String>> onlyExistsValidator() {
		return new OptBOnlyExistsValidator();
	}
}
