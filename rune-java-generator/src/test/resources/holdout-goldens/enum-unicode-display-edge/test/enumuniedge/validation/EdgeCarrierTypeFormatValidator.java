package test.enumuniedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.enumuniedge.EdgeCarrier;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class EdgeCarrierTypeFormatValidator implements Validator<EdgeCarrier> {

	private List<ComparisonResult> getComparisonResults(EdgeCarrier o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, EdgeCarrier o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("EdgeCarrier", ValidationResult.ValidationType.TYPE_FORMAT, "EdgeCarrier", path, "", res.getError());
				}
				return success("EdgeCarrier", ValidationResult.ValidationType.TYPE_FORMAT, "EdgeCarrier", path, "");
			})
			.collect(toList());
	}

}
