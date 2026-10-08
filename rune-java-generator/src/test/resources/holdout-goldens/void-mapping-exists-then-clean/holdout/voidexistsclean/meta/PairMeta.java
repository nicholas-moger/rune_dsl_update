package holdout.voidexistsclean.meta;

import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import holdout.voidexistsclean.Pair;
import holdout.voidexistsclean.validation.PairTypeFormatValidator;
import holdout.voidexistsclean.validation.PairValidator;
import holdout.voidexistsclean.validation.exists.PairOnlyExistsValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 0.0.0
 */
@RosettaMeta(model=Pair.class)
public class PairMeta implements RosettaMetaData<Pair> {

	@Override
	public List<Validator<? super Pair>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Pair, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Pair> validator(ValidatorFactory factory) {
		return factory.<Pair>create(PairValidator.class);
	}

	@Override
	public Validator<? super Pair> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Pair>create(PairTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Pair> validator() {
		return new PairValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Pair> typeFormatValidator() {
		return new PairTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Pair, Set<String>> onlyExistsValidator() {
		return new PairOnlyExistsValidator();
	}
}
