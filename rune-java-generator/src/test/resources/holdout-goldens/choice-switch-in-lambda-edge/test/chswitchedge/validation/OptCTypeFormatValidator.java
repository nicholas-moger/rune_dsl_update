package test.chswitchedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.chswitchedge.OptC;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class OptCTypeFormatValidator implements Validator<OptC> {

	private List<ComparisonResult> getComparisonResults(OptC o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, OptC o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("OptC", ValidationResult.ValidationType.TYPE_FORMAT, "OptC", path, "", res.getError());
				}
				return success("OptC", ValidationResult.ValidationType.TYPE_FORMAT, "OptC", path, "");
			})
			.collect(toList());
	}

}
