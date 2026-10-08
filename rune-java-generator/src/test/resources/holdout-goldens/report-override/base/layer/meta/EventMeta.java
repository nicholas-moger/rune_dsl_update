package base.layer.meta;

import base.layer.Event;
import base.layer.validation.EventTypeFormatValidator;
import base.layer.validation.EventValidator;
import base.layer.validation.exists.EventOnlyExistsValidator;
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
 * @version 0.0.0
 */
@RosettaMeta(model=Event.class)
public class EventMeta implements RosettaMetaData<Event> {

	@Override
	public List<Validator<? super Event>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super Event, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super Event> validator(ValidatorFactory factory) {
		return factory.<Event>create(EventValidator.class);
	}

	@Override
	public Validator<? super Event> typeFormatValidator(ValidatorFactory factory) {
		return factory.<Event>create(EventTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super Event> validator() {
		return new EventValidator();
	}

	@Deprecated
	@Override
	public Validator<? super Event> typeFormatValidator() {
		return new EventTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super Event, Set<String>> onlyExistsValidator() {
		return new EventOnlyExistsValidator();
	}
}
