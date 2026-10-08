package test.datesubtract.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.datesubtract.Test;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class TestTypeFormatValidator implements Validator<Test> {

	private List<ComparisonResult> getComparisonResults(Test o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Test o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Test", ValidationResult.ValidationType.TYPE_FORMAT, "Test", path, "", res.getError());
				}
				return success("Test", ValidationResult.ValidationType.TYPE_FORMAT, "Test", path, "");
			})
			.collect(toList());
	}

}
