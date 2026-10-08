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
import route.fixture.RouteBase;
import route.fixture.RouteChild;
import route.fixture.validation.RouteChildTypeFormatValidator;
import route.fixture.validation.RouteChildValidator;
import route.fixture.validation.datarule.RouteBaseNamePresent;
import route.fixture.validation.exists.RouteChildOnlyExistsValidator;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=RouteChild.class)
public class RouteChildMeta implements RosettaMetaData<RouteChild> {

	@Override
	public List<Validator<? super RouteChild>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<RouteBase>create(RouteBaseNamePresent.class)
		);
	}
	
	@Override
	public List<Function<? super RouteChild, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RouteChild> validator(ValidatorFactory factory) {
		return factory.<RouteChild>create(RouteChildValidator.class);
	}

	@Override
	public Validator<? super RouteChild> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RouteChild>create(RouteChildTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RouteChild> validator() {
		return new RouteChildValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RouteChild> typeFormatValidator() {
		return new RouteChildTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RouteChild, Set<String>> onlyExistsValidator() {
		return new RouteChildOnlyExistsValidator();
	}
}
