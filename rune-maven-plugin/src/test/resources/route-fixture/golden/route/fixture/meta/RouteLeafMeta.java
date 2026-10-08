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
import route.fixture.RouteLeaf;
import route.fixture.validation.RouteLeafTypeFormatValidator;
import route.fixture.validation.RouteLeafValidator;
import route.fixture.validation.exists.RouteLeafOnlyExistsValidator;


/**
 * @version 1.0.0
 */
@RosettaMeta(model=RouteLeaf.class)
public class RouteLeafMeta implements RosettaMetaData<RouteLeaf> {

	@Override
	public List<Validator<? super RouteLeaf>> dataRules(ValidatorFactory factory) {
		return Arrays.asList(
		);
	}
	
	@Override
	public List<Function<? super RouteLeaf, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
		return Collections.emptyList();
	}
	
	@Override
	public Validator<? super RouteLeaf> validator(ValidatorFactory factory) {
		return factory.<RouteLeaf>create(RouteLeafValidator.class);
	}

	@Override
	public Validator<? super RouteLeaf> typeFormatValidator(ValidatorFactory factory) {
		return factory.<RouteLeaf>create(RouteLeafTypeFormatValidator.class);
	}

	@Deprecated
	@Override
	public Validator<? super RouteLeaf> validator() {
		return new RouteLeafValidator();
	}

	@Deprecated
	@Override
	public Validator<? super RouteLeaf> typeFormatValidator() {
		return new RouteLeafTypeFormatValidator();
	}
	
	@Override
	public ValidatorWithArg<? super RouteLeaf, Set<String>> onlyExistsValidator() {
		return new RouteLeafOnlyExistsValidator();
	}
}
