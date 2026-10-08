package chaos.s31.a9tabs.meta;

import chaos.s31.a9tabs.C31Held;
import chaos.s31.a9tabs.validation.C31HeldTypeFormatValidator;
import chaos.s31.a9tabs.validation.C31HeldValidator;
import chaos.s31.a9tabs.validation.exists.C31HeldOnlyExistsValidator;
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
@RosettaMeta(model=C31Held.class)
public class C31HeldMeta implements RosettaMetaData<C31Held> {

	@Override
	public List<Validator<? super C31Held>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C31Held, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C31Held> validator(ValidatorFactory factory) {
		return factory.<C31Held>create(C31HeldValidator.class);
	}

	@Override
	public Validator<? super C31Held> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C31Held>create(C31HeldTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C31Held> validator() {
		return new C31HeldValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C31Held> typeFormatValidator() {
		return new C31HeldTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C31Held, Set<String>> onlyExistsValidator() {
		return new C31HeldOnlyExistsValidator();
	}
}
