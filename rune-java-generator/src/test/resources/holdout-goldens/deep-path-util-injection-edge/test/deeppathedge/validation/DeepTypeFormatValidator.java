package test.deeppathedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.deeppathedge.Deep;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class DeepTypeFormatValidator implements Validator<Deep> {

	private List<ComparisonResult> getComparisonResults(Deep o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Deep o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Deep", ValidationResult.ValidationType.TYPE_FORMAT, "Deep", path, "", res.getError());
				}
				return success("Deep", ValidationResult.ValidationType.TYPE_FORMAT, "Deep", path, "");
			})
			.collect(toList());
	}

}
