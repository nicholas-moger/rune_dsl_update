package holdout.typenamedrosetta.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedrosetta.ValidatorWithArg;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ValidatorWithArgTypeFormatValidator implements Validator<ValidatorWithArg> {

	private List<ComparisonResult> getComparisonResults(ValidatorWithArg o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, ValidatorWithArg o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("ValidatorWithArg", ValidationResult.ValidationType.TYPE_FORMAT, "ValidatorWithArg", path, "", res.getError());
				}
				return success("ValidatorWithArg", ValidationResult.ValidationType.TYPE_FORMAT, "ValidatorWithArg", path, "");
			})
			.collect(toList());
	}

}
