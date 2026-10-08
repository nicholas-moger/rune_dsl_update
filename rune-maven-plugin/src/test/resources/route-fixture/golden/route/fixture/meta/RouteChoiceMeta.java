package route.fixture.meta;

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
import route.fixture.RouteChoice;
import route.fixture.validation.RouteChoiceTypeFormatValidator;
import route.fixture.validation.RouteChoiceValidator;
import route.fixture.validation.datarule.RouteChoiceChoice;
import route.fixture.validation.exists.RouteChoiceOnlyExistsValidator;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=RouteChoice.class)
public class RouteChoiceMeta implements RosettaMetaData<RouteChoice> {

	@Override
	public List<Validator<? super RouteChoice>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<RouteChoice>create(RouteChoiceChoice.class)
		);
	}
	
	@Override
	public List<Function<? super RouteChoice, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RouteChoice> validator(ValidatorFactory factory) {
		return factory.<RouteChoice>create(RouteChoiceValidator.class);
	}

	@Override
	public Validator<? super RouteChoice> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RouteChoice>create(RouteChoiceTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RouteChoice> validator() {
		return new RouteChoiceValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RouteChoice> typeFormatValidator() {
		return new RouteChoiceTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RouteChoice, Set<String>> onlyExistsValidator() {
		return new RouteChoiceOnlyExistsValidator();
	}
}
