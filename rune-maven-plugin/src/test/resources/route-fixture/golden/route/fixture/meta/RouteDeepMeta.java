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
import route.fixture.RouteDeep;
import route.fixture.validation.RouteDeepTypeFormatValidator;
import route.fixture.validation.RouteDeepValidator;
import route.fixture.validation.datarule.RouteDeepOneOf0;
import route.fixture.validation.exists.RouteDeepOnlyExistsValidator;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=RouteDeep.class)
public class RouteDeepMeta implements RosettaMetaData<RouteDeep> {

	@Override
	public List<Validator<? super RouteDeep>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<RouteDeep>create(RouteDeepOneOf0.class)
		);
	}
	
	@Override
	public List<Function<? super RouteDeep, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RouteDeep> validator(ValidatorFactory factory) {
		return factory.<RouteDeep>create(RouteDeepValidator.class);
	}

	@Override
	public Validator<? super RouteDeep> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RouteDeep>create(RouteDeepTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RouteDeep> validator() {
		return new RouteDeepValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RouteDeep> typeFormatValidator() {
		return new RouteDeepTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RouteDeep, Set<String>> onlyExistsValidator() {
		return new RouteDeepOnlyExistsValidator();
	}
}
