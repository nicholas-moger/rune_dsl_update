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
import route.fixture.validation.RouteBaseTypeFormatValidator;
import route.fixture.validation.RouteBaseValidator;
import route.fixture.validation.datarule.RouteBaseNamePresent;
import route.fixture.validation.exists.RouteBaseOnlyExistsValidator;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=RouteBase.class)
public class RouteBaseMeta implements RosettaMetaData<RouteBase> {

	@Override
	public List<Validator<? super RouteBase>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
			factory.<RouteBase>create(RouteBaseNamePresent.class)
		);
	}
	
	@Override
	public List<Function<? super RouteBase, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RouteBase> validator(ValidatorFactory factory) {
		return factory.<RouteBase>create(RouteBaseValidator.class);
	}

	@Override
	public Validator<? super RouteBase> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RouteBase>create(RouteBaseTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RouteBase> validator() {
		return new RouteBaseValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RouteBase> typeFormatValidator() {
		return new RouteBaseTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RouteBase, Set<String>> onlyExistsValidator() {
		return new RouteBaseOnlyExistsValidator();
	}
}
