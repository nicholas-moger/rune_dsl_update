package holdout.listliteraladditemcoerce.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.listliteraladditemcoerce.Box;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class BoxTypeFormatValidator implements Validator<Box> {

	private List<ComparisonResult> getComparisonResults(Box o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Box o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Box", ValidationResult.ValidationType.TYPE_FORMAT, "Box", path, "", res.getError());
				}
				return success("Box", ValidationResult.ValidationType.TYPE_FORMAT, "Box", path, "");
			})
			.collect(toList());
	}

}
