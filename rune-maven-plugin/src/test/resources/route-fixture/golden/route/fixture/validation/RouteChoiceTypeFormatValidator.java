package route.fixture.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import route.fixture.RouteChoice;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RouteChoiceTypeFormatValidator implements Validator<RouteChoice> {

	private List<ComparisonResult> getComparisonResults(RouteChoice o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RouteChoice o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RouteChoice", ValidationResult.ValidationType.TYPE_FORMAT, "RouteChoice", path, "", res.getError());
				}
				return success("RouteChoice", ValidationResult.ValidationType.TYPE_FORMAT, "RouteChoice", path, "");
			})
			.collect(toList());
	}

}
