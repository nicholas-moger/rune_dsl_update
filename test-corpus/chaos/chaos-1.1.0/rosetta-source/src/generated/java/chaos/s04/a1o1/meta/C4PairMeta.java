package chaos.s04.a1o1.meta;

import chaos.s04.a1o1.C4Pair;
import chaos.s04.a1o1.validation.C4PairTypeFormatValidator;
import chaos.s04.a1o1.validation.C4PairValidator;
import chaos.s04.a1o1.validation.exists.C4PairOnlyExistsValidator;
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
@RosettaMeta(model=C4Pair.class)
public class C4PairMeta implements RosettaMetaData<C4Pair> {

	@Override
	public List<Validator<? super C4Pair>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C4Pair, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C4Pair> validator(ValidatorFactory factory) {
		return factory.<C4Pair>create(C4PairValidator.class);
	}

	@Override
	public Validator<? super C4Pair> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C4Pair>create(C4PairTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C4Pair> validator() {
		return new C4PairValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C4Pair> typeFormatValidator() {
		return new C4PairTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C4Pair, Set<String>> onlyExistsValidator() {
		return new C4PairOnlyExistsValidator();
	}
}
