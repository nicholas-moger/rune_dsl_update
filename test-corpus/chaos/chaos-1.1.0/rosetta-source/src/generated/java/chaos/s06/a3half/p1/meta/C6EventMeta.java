package chaos.s06.a3half.p1.meta;

import chaos.s06.a3half.p1.C6Event;
import chaos.s06.a3half.p1.validation.C6EventTypeFormatValidator;
import chaos.s06.a3half.p1.validation.C6EventValidator;
import chaos.s06.a3half.p1.validation.exists.C6EventOnlyExistsValidator;
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
@RosettaMeta(model=C6Event.class)
public class C6EventMeta implements RosettaMetaData<C6Event> {

	@Override
	public List<Validator<? super C6Event>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C6Event, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super C6Event> validator(ValidatorFactory factory) {
		return factory.<C6Event>create(C6EventValidator.class);
	}

	@Override
	public Validator<? super C6Event> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C6Event>create(C6EventTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C6Event> validator() {
		return new C6EventValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C6Event> typeFormatValidator() {
		return new C6EventTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C6Event, Set<String>> onlyExistsValidator() {
		return new C6EventOnlyExistsValidator();
	}
}
