package test.deeppathedgetypes.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.deeppathedgetypes.ROuter;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ROuterTypeFormatValidator implements Validator<ROuter> {

	private List<ComparisonResult> getComparisonResults(ROuter o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, ROuter o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("ROuter", ValidationResult.ValidationType.TYPE_FORMAT, "ROuter", path, "", res.getError());
				}
				return success("ROuter", ValidationResult.ValidationType.TYPE_FORMAT, "ROuter", path, "");
			})
			.collect(toList());
	}

}
