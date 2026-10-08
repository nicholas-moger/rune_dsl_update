package holdout.onlyexistsitemroot.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.onlyexistsitemroot.Paths;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class PathsTypeFormatValidator implements Validator<Paths> {

	private List<ComparisonResult> getComparisonResults(Paths o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Paths o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Paths", ValidationResult.ValidationType.TYPE_FORMAT, "Paths", path, "", res.getError());
				}
				return success("Paths", ValidationResult.ValidationType.TYPE_FORMAT, "Paths", path, "");
			})
			.collect(toList());
	}

}
