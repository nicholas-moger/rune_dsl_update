package holdout.typenamedrosetta.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedrosetta.ValidationResult;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ValidationResultValidator implements Validator<ValidationResult> {

	private List<ComparisonResult> getComparisonResults(ValidationResult o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("x", (String) o.getX() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<com.rosetta.model.lib.validation.ValidationResult<?>> getValidationResults(RosettaPath path, ValidationResult o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("ValidationResult", com.rosetta.model.lib.validation.ValidationResult.ValidationType.CARDINALITY, "ValidationResult", path, "", res.getError());
				}
				return success("ValidationResult", com.rosetta.model.lib.validation.ValidationResult.ValidationType.CARDINALITY, "ValidationResult", path, "");
			})
			.collect(toList());
	}

}
