package route.fixture.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import route.fixture.RouteAltB;
import route.fixture.RouteLeaf;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RouteAltBValidator implements Validator<RouteAltB> {

	private List<ComparisonResult> getComparisonResults(RouteAltB o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("shared", (RouteLeaf) o.getShared() != null ? 1 : 0, 0, 1), 
				checkCardinality("q", (String) o.getQ() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RouteAltB o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RouteAltB", ValidationResult.ValidationType.CARDINALITY, "RouteAltB", path, "", res.getError());
				}
				return success("RouteAltB", ValidationResult.ValidationType.CARDINALITY, "RouteAltB", path, "");
			})
			.collect(toList());
	}

}
