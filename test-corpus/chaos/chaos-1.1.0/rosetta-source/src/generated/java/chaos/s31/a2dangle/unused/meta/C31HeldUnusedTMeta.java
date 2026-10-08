package chaos.s31.a2dangle.unused.meta;

import chaos.s31.a2dangle.unused.C31HeldUnusedT;
import chaos.s31.a2dangle.unused.validation.C31HeldUnusedTTypeFormatValidator;
import chaos.s31.a2dangle.unused.validation.C31HeldUnusedTValidator;
import chaos.s31.a2dangle.unused.validation.exists.C31HeldUnusedTOnlyExistsValidator;
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
@RosettaMeta(model=C31HeldUnusedT.class)
public class C31HeldUnusedTMeta implements RosettaMetaData<C31HeldUnusedT> {

	@Override
	public List<Validator<? super C31HeldUnusedT>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C31HeldUnusedT, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C31HeldUnusedT> validator(ValidatorFactory factory) {
		return factory.<C31HeldUnusedT>create(C31HeldUnusedTValidator.class);
	}

	@Override
	public Validator<? super C31HeldUnusedT> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C31HeldUnusedT>create(C31HeldUnusedTTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C31HeldUnusedT> validator() {
		return new C31HeldUnusedTValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C31HeldUnusedT> typeFormatValidator() {
		return new C31HeldUnusedTTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C31HeldUnusedT, Set<String>> onlyExistsValidator() {
		return new C31HeldUnusedTOnlyExistsValidator();
	}
}
