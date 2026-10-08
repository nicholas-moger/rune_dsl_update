package holdout.typenamedguava.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedguava.ImmutableList;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ImmutableListTypeFormatValidator implements Validator<ImmutableList> {

	private List<ComparisonResult> getComparisonResults(ImmutableList o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, ImmutableList o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("ImmutableList", ValidationResult.ValidationType.TYPE_FORMAT, "ImmutableList", path, "", res.getError());
				}
				return success("ImmutableList", ValidationResult.ValidationType.TYPE_FORMAT, "ImmutableList", path, "");
			})
			.collect(toList());
	}

}
