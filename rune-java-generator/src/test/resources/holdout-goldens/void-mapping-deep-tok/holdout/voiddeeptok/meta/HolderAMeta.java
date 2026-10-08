package holdout.voiddeeptok.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.voiddeeptok.HolderA;
import holdout.voiddeeptok.validation.HolderATypeFormatValidator;
import holdout.voiddeeptok.validation.HolderAValidator;
import holdout.voiddeeptok.validation.exists.HolderAOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=HolderA.class)
public class HolderAMeta implements RosettaMetaData<HolderA> {

	@Override
	public List<Validator<? super HolderA>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super HolderA, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super HolderA> validator(ValidatorFactory factory) {
		return factory.<HolderA>create(HolderAValidator.class);
	}

	@Override
	public Validator<? super HolderA> typeFormatValidator(ValidatorFactory factory) {
		return factory.<HolderA>create(HolderATypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super HolderA> validator() {
		return new HolderAValidator();
	}

	@Deprecated
	@Override
	public Validator<? super HolderA> typeFormatValidator() {
		return new HolderATypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super HolderA, Set<String>> onlyExistsValidator() {
		return new HolderAOnlyExistsValidator();
	}
}
