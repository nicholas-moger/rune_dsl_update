package holdout.typenamedguava.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedguava.ImmutableMap;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ImmutableMapTypeFormatValidator implements Validator<ImmutableMap> {

	private List<ComparisonResult> getComparisonResults(ImmutableMap o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, ImmutableMap o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("ImmutableMap", ValidationResult.ValidationType.TYPE_FORMAT, "ImmutableMap", path, "", res.getError());
				}
				return success("ImmutableMap", ValidationResult.ValidationType.TYPE_FORMAT, "ImmutableMap", path, "");
			})
			.collect(toList());
	}

}
