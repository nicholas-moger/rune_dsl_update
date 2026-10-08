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
import route.fixture.RouteAltA;
import route.fixture.validation.RouteAltATypeFormatValidator;
import route.fixture.validation.RouteAltAValidator;
import route.fixture.validation.datarule.RouteAltAOneOf0;
import route.fixture.validation.exists.RouteAltAOnlyExistsValidator;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=RouteAltA.class)
public class RouteAltAMeta implements RosettaMetaData<RouteAltA> {

	@Override
	public List<Validator<? super RouteAltA>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<RouteAltA>create(RouteAltAOneOf0.class)
		);
	}
	
	@Override
	public List<Function<? super RouteAltA, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RouteAltA> validator(ValidatorFactory factory) {
		return factory.<RouteAltA>create(RouteAltAValidator.class);
	}

	@Override
	public Validator<? super RouteAltA> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RouteAltA>create(RouteAltATypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RouteAltA> validator() {
		return new RouteAltAValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RouteAltA> typeFormatValidator() {
		return new RouteAltATypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RouteAltA, Set<String>> onlyExistsValidator() {
		return new RouteAltAOnlyExistsValidator();
	}
}
