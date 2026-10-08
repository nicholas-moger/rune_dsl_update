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
import route.fixture.RouteAltB;
import route.fixture.validation.RouteAltBTypeFormatValidator;
import route.fixture.validation.RouteAltBValidator;
import route.fixture.validation.datarule.RouteAltBOneOf0;
import route.fixture.validation.exists.RouteAltBOnlyExistsValidator;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=RouteAltB.class)
public class RouteAltBMeta implements RosettaMetaData<RouteAltB> {

	@Override
	public List<Validator<? super RouteAltB>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<RouteAltB>create(RouteAltBOneOf0.class)
		);
	}
	
	@Override
	public List<Function<? super RouteAltB, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RouteAltB> validator(ValidatorFactory factory) {
		return factory.<RouteAltB>create(RouteAltBValidator.class);
	}

	@Override
	public Validator<? super RouteAltB> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RouteAltB>create(RouteAltBTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RouteAltB> validator() {
		return new RouteAltBValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RouteAltB> typeFormatValidator() {
		return new RouteAltBTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RouteAltB, Set<String>> onlyExistsValidator() {
		return new RouteAltBOnlyExistsValidator();
	}
}
