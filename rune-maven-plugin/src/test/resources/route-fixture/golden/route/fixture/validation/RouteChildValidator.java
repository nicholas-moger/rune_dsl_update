package route.fixture.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.List;
import route.fixture.RouteChild;
import route.fixture.RouteColour;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RouteChildValidator implements Validator<RouteChild> {

	private List<ComparisonResult> getComparisonResults(RouteChild o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("name", (String) o.getName() != null ? 1 : 0, 1, 1), 
				checkCardinality("market", (FieldWithMetaString) o.getMarket() != null ? 1 : 0, 0, 1), 
				checkCardinality("colour", (RouteColour) o.getColour() != null ? 1 : 0, 0, 1), 
				checkCardinality("extra", (String) o.getExtra() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RouteChild o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RouteChild", ValidationResult.ValidationType.CARDINALITY, "RouteChild", path, "", res.getError());
				}
				return success("RouteChild", ValidationResult.ValidationType.CARDINALITY, "RouteChild", path, "");
			})
			.collect(toList());
	}

}
