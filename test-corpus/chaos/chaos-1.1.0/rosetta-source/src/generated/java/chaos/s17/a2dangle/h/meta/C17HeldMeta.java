package chaos.s17.a2dangle.h.meta;

import chaos.s17.a2dangle.h.C17Held;
import chaos.s17.a2dangle.h.validation.C17HeldTypeFormatValidator;
import chaos.s17.a2dangle.h.validation.C17HeldValidator;
import chaos.s17.a2dangle.h.validation.exists.C17HeldOnlyExistsValidator;
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
@RosettaMeta(model=C17Held.class)
public class C17HeldMeta implements RosettaMetaData<C17Held> {

	@Override
	public List<Validator<? super C17Held>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C17Held, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C17Held> validator(ValidatorFactory factory) {
		return factory.<C17Held>create(C17HeldValidator.class);
	}

	@Override
	public Validator<? super C17Held> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C17Held>create(C17HeldTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C17Held> validator() {
		return new C17HeldValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C17Held> typeFormatValidator() {
		return new C17HeldTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C17Held, Set<String>> onlyExistsValidator() {
		return new C17HeldOnlyExistsValidator();
	}
}
