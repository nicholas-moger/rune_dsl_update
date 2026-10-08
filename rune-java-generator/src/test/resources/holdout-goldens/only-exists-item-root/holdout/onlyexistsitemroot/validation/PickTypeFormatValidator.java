package holdout.onlyexistsitemroot.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.onlyexistsitemroot.Pick;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class PickTypeFormatValidator implements Validator<Pick> {

	private List<ComparisonResult> getComparisonResults(Pick o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Pick o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Pick", ValidationResult.ValidationType.TYPE_FORMAT, "Pick", path, "", res.getError());
				}
				return success("Pick", ValidationResult.ValidationType.TYPE_FORMAT, "Pick", path, "");
			})
			.collect(toList());
	}

}
