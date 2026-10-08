package holdout.typenamedannotations.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedannotations.Required;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RequiredTypeFormatValidator implements Validator<Required> {

	private List<ComparisonResult> getComparisonResults(Required o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Required o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Required", ValidationResult.ValidationType.TYPE_FORMAT, "Required", path, "", res.getError());
				}
				return success("Required", ValidationResult.ValidationType.TYPE_FORMAT, "Required", path, "");
			})
			.collect(toList());
	}

}
