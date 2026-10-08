package holdout.typenamedrosetta.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedrosetta.ComparisonResult;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ComparisonResultTypeFormatValidator implements Validator<ComparisonResult> {

	private List<com.rosetta.model.lib.expression.ComparisonResult> getComparisonResults(ComparisonResult o) {
		return Lists.<com.rosetta.model.lib.expression.ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, ComparisonResult o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("ComparisonResult", ValidationResult.ValidationType.TYPE_FORMAT, "ComparisonResult", path, "", res.getError());
				}
				return success("ComparisonResult", ValidationResult.ValidationType.TYPE_FORMAT, "ComparisonResult", path, "");
			})
			.collect(toList());
	}

}
