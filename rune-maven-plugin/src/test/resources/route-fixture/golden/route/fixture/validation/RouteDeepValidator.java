package route.fixture.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import route.fixture.RouteAltA;
import route.fixture.RouteAltB;
import route.fixture.RouteDeep;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RouteDeepValidator implements Validator<RouteDeep> {

	private List<ComparisonResult> getComparisonResults(RouteDeep o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("a", (RouteAltA) o.getA() != null ? 1 : 0, 0, 1), 
				checkCardinality("b", (RouteAltB) o.getB() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RouteDeep o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RouteDeep", ValidationResult.ValidationType.CARDINALITY, "RouteDeep", path, "", res.getError());
				}
				return success("RouteDeep", ValidationResult.ValidationType.CARDINALITY, "RouteDeep", path, "");
			})
			.collect(toList());
	}

}
