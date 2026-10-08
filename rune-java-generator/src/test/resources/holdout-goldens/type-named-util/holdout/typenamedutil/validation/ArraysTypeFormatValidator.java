package holdout.typenamedutil.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedutil.Arrays;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ArraysTypeFormatValidator implements Validator<Arrays> {

	private List<ComparisonResult> getComparisonResults(Arrays o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Arrays o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Arrays", ValidationResult.ValidationType.TYPE_FORMAT, "Arrays", path, "", res.getError());
				}
				return success("Arrays", ValidationResult.ValidationType.TYPE_FORMAT, "Arrays", path, "");
			})
			.collect(toList());
	}

}
