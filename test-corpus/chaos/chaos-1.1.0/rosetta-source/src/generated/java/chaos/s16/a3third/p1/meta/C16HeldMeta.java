package chaos.s16.a3third.p1.meta;

import chaos.s16.a3third.p1.C16Held;
import chaos.s16.a3third.p1.validation.C16HeldTypeFormatValidator;
import chaos.s16.a3third.p1.validation.C16HeldValidator;
import chaos.s16.a3third.p1.validation.exists.C16HeldOnlyExistsValidator;
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
@RosettaMeta(model=C16Held.class)
public class C16HeldMeta implements RosettaMetaData<C16Held> {

	@Override
	public List<Validator<? super C16Held>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C16Held, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C16Held> validator(ValidatorFactory factory) {
		return factory.<C16Held>create(C16HeldValidator.class);
	}

	@Override
	public Validator<? super C16Held> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C16Held>create(C16HeldTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C16Held> validator() {
		return new C16HeldValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C16Held> typeFormatValidator() {
		return new C16HeldTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C16Held, Set<String>> onlyExistsValidator() {
		return new C16HeldOnlyExistsValidator();
	}
}
