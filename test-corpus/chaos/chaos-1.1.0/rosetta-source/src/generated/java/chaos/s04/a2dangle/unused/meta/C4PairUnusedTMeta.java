package chaos.s04.a2dangle.unused.meta;

import chaos.s04.a2dangle.unused.C4PairUnusedT;
import chaos.s04.a2dangle.unused.validation.C4PairUnusedTTypeFormatValidator;
import chaos.s04.a2dangle.unused.validation.C4PairUnusedTValidator;
import chaos.s04.a2dangle.unused.validation.exists.C4PairUnusedTOnlyExistsValidator;
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


/**
 * @version 1.0.0
 */
@RosettaMeta(model=C4PairUnusedT.class)
public class C4PairUnusedTMeta implements RosettaMetaData<C4PairUnusedT> {

	@Override
	public List<Validator<? super C4PairUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C4PairUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C4PairUnusedT> validator(ValidatorFactory factory) {
		return factory.<C4PairUnusedT>create(C4PairUnusedTValidator.class);
	}

	@Override
	public Validator<? super C4PairUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C4PairUnusedT>create(C4PairUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C4PairUnusedT> validator() {
		return new C4PairUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C4PairUnusedT> typeFormatValidator() {
		return new C4PairUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C4PairUnusedT, Set<String>> onlyExistsValidator() {
		return new C4PairUnusedTOnlyExistsValidator();
	}
}
