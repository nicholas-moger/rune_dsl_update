package route.fixture.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import route.fixture.RouteLeaf;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RouteLeafTypeFormatValidator implements Validator<RouteLeaf> {

	private List<ComparisonResult> getComparisonResults(RouteLeaf o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RouteLeaf o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RouteLeaf", ValidationResult.ValidationType.TYPE_FORMAT, "RouteLeaf", path, "", res.getError());
				}
				return success("RouteLeaf", ValidationResult.ValidationType.TYPE_FORMAT, "RouteLeaf", path, "");
			})
			.collect(toList());
	}

}
