package test.reservednames.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.reservednames.Path;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class PathTypeFormatValidator implements Validator<Path> {

	private List<ComparisonResult> getComparisonResults(Path o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Path o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Path", ValidationResult.ValidationType.TYPE_FORMAT, "Path", path, "", res.getError());
				}
				return success("Path", ValidationResult.ValidationType.TYPE_FORMAT, "Path", path, "");
			})
			.collect(toList());
	}

}
