package holdout.typenamedutil.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedutil.Set;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class SetTypeFormatValidator implements Validator<Set> {

	private List<ComparisonResult> getComparisonResults(Set o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Set o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Set", ValidationResult.ValidationType.TYPE_FORMAT, "Set", path, "", res.getError());
				}
				return success("Set", ValidationResult.ValidationType.TYPE_FORMAT, "Set", path, "");
			})
			.collect(toList());
	}

}
