package route.fixture.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import route.fixture.RouteBase;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkString;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class RouteBaseTypeFormatValidator implements Validator<RouteBase> {

	private List<ComparisonResult> getComparisonResults(RouteBase o) {
		return Lists.<ComparisonResult>newArrayList(
				checkString("name", o.getName(), 1, of(3), empty())
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RouteBase o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RouteBase", ValidationResult.ValidationType.TYPE_FORMAT, "RouteBase", path, "", res.getError());
				}
				return success("RouteBase", ValidationResult.ValidationType.TYPE_FORMAT, "RouteBase", path, "");
			})
			.collect(toList());
	}

}
