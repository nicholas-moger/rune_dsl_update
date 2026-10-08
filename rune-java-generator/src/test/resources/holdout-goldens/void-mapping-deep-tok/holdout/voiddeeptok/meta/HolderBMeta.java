package holdout.voiddeeptok.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.voiddeeptok.HolderB;
import holdout.voiddeeptok.validation.HolderBTypeFormatValidator;
import holdout.voiddeeptok.validation.HolderBValidator;
import holdout.voiddeeptok.validation.exists.HolderBOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=HolderB.class)
public class HolderBMeta implements RosettaMetaData<HolderB> {

	@Override
	public List<Validator<? super HolderB>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super HolderB, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super HolderB> validator(ValidatorFactory factory) {
		return factory.<HolderB>create(HolderBValidator.class);
	}

	@Override
	public Validator<? super HolderB> typeFormatValidator(ValidatorFactory factory) {
		return factory.<HolderB>create(HolderBTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super HolderB> validator() {
		return new HolderBValidator();
	}

	@Deprecated
	@Override
	public Validator<? super HolderB> typeFormatValidator() {
		return new HolderBTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super HolderB, Set<String>> onlyExistsValidator() {
		return new HolderBOnlyExistsValidator();
	}
}
