package test.chswitchedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.chswitchedge.Both;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class BothTypeFormatValidator implements Validator<Both> {

	private List<ComparisonResult> getComparisonResults(Both o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Both o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Both", ValidationResult.ValidationType.TYPE_FORMAT, "Both", path, "", res.getError());
				}
				return success("Both", ValidationResult.ValidationType.TYPE_FORMAT, "Both", path, "");
			})
			.collect(toList());
	}

}
