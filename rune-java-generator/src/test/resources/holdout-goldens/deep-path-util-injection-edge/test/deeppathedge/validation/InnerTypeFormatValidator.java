package test.deeppathedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.deeppathedge.Inner;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class InnerTypeFormatValidator implements Validator<Inner> {

	private List<ComparisonResult> getComparisonResults(Inner o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Inner o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Inner", ValidationResult.ValidationType.TYPE_FORMAT, "Inner", path, "", res.getError());
				}
				return success("Inner", ValidationResult.ValidationType.TYPE_FORMAT, "Inner", path, "");
			})
			.collect(toList());
	}

}
