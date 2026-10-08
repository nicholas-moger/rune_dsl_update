package route.fixture.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import route.fixture.RouteAltB;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RouteAltBTypeFormatValidator implements Validator<RouteAltB> {

	private List<ComparisonResult> getComparisonResults(RouteAltB o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RouteAltB o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RouteAltB", ValidationResult.ValidationType.TYPE_FORMAT, "RouteAltB", path, "", res.getError());
				}
				return success("RouteAltB", ValidationResult.ValidationType.TYPE_FORMAT, "RouteAltB", path, "");
			})
			.collect(toList());
	}

}
