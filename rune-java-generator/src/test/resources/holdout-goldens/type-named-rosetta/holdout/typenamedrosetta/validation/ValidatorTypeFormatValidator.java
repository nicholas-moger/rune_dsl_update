package holdout.typenamedrosetta.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ValidatorTypeFormatValidator implements Validator<holdout.typenamedrosetta.Validator> {

	private List<ComparisonResult> getComparisonResults(holdout.typenamedrosetta.Validator o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, holdout.typenamedrosetta.Validator o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Validator", ValidationResult.ValidationType.TYPE_FORMAT, "Validator", path, "", res.getError());
				}
				return success("Validator", ValidationResult.ValidationType.TYPE_FORMAT, "Validator", path, "");
			})
			.collect(toList());
	}

}
