package test.dispatchcollision.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.dispatchcollision.MathInput;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class MathInputTypeFormatValidator implements Validator<MathInput> {

	private List<ComparisonResult> getComparisonResults(MathInput o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, MathInput o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("MathInput", ValidationResult.ValidationType.TYPE_FORMAT, "MathInput", path, "", res.getError());
				}
				return success("MathInput", ValidationResult.ValidationType.TYPE_FORMAT, "MathInput", path, "");
			})
			.collect(toList());
	}

}
