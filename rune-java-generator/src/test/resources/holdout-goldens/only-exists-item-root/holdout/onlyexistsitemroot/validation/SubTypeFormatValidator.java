package holdout.onlyexistsitemroot.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.onlyexistsitemroot.Sub;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class SubTypeFormatValidator implements Validator<Sub> {

	private List<ComparisonResult> getComparisonResults(Sub o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Sub o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Sub", ValidationResult.ValidationType.TYPE_FORMAT, "Sub", path, "", res.getError());
				}
				return success("Sub", ValidationResult.ValidationType.TYPE_FORMAT, "Sub", path, "");
			})
			.collect(toList());
	}

}
