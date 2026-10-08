package chaos.s02.a2dangle.unused.meta;

import chaos.s02.a2dangle.unused.C2TagUnusedT;
import chaos.s02.a2dangle.unused.validation.C2TagUnusedTTypeFormatValidator;
import chaos.s02.a2dangle.unused.validation.C2TagUnusedTValidator;
import chaos.s02.a2dangle.unused.validation.exists.C2TagUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C2TagUnusedT.class)
public class C2TagUnusedTMeta implements RosettaMetaData<C2TagUnusedT> {

	@Override
	public List<Validator<? super C2TagUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C2TagUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C2TagUnusedT> validator(ValidatorFactory factory) {
		return factory.<C2TagUnusedT>create(C2TagUnusedTValidator.class);
	}

	@Override
	public Validator<? super C2TagUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C2TagUnusedT>create(C2TagUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C2TagUnusedT> validator() {
		return new C2TagUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C2TagUnusedT> typeFormatValidator() {
		return new C2TagUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C2TagUnusedT, Set<String>> onlyExistsValidator() {
		return new C2TagUnusedTOnlyExistsValidator();
	}
}
