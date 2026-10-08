package test.reservednames.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.reservednames.FailureMessage;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class FailureMessageValidator implements Validator<FailureMessage> {

	private List<ComparisonResult> getComparisonResults(FailureMessage o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("text", (String) o.getText() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, FailureMessage o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("FailureMessage", ValidationResult.ValidationType.CARDINALITY, "FailureMessage", path, "", res.getError());
				}
				return success("FailureMessage", ValidationResult.ValidationType.CARDINALITY, "FailureMessage", path, "");
			})
			.collect(toList());
	}

}
