package chaos.s28.a1o3.meta;

import chaos.s28.a1o3.C28Event;
import chaos.s28.a1o3.functions.Qualify_C28Cancel;
import chaos.s28.a1o3.functions.Qualify_C28Trade;
import chaos.s28.a1o3.validation.C28EventTypeFormatValidator;
import chaos.s28.a1o3.validation.C28EventValidator;
import chaos.s28.a1o3.validation.exists.C28EventOnlyExistsValidator;
import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=C28Event.class)
public class C28EventMeta implements RosettaMetaData<C28Event> {

	@Override
	public List<Validator<? super C28Event>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super C28Event, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Arrays.asList(
			factory.<C28Event>create(Qualify_C28Trade.class),
			factory.<C28Event>create(Qualify_C28Cancel.class)
		);
	}
	
	@Override
	public Validator<? super C28Event> validator(ValidatorFactory factory) {
		return factory.<C28Event>create(C28EventValidator.class);
	}

	@Override
	public Validator<? super C28Event> typeFormatValidator(ValidatorFactory factory) {
		return factory.<C28Event>create(C28EventTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super C28Event> validator() {
		return new C28EventValidator();
	}

	@Deprecated
	@Override
	public Validator<? super C28Event> typeFormatValidator() {
		return new C28EventTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super C28Event, Set<String>> onlyExistsValidator() {
		return new C28EventOnlyExistsValidator();
	}
}
