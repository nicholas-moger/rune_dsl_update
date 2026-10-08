package holdout.typenamedrosetta.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedrosetta.RosettaPath;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RosettaPathTypeFormatValidator implements Validator<RosettaPath> {

	private List<ComparisonResult> getComparisonResults(RosettaPath o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(com.rosetta.model.lib.path.RosettaPath path, RosettaPath o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RosettaPath", ValidationResult.ValidationType.TYPE_FORMAT, "RosettaPath", path, "", res.getError());
				}
				return success("RosettaPath", ValidationResult.ValidationType.TYPE_FORMAT, "RosettaPath", path, "");
			})
			.collect(toList());
	}

}
