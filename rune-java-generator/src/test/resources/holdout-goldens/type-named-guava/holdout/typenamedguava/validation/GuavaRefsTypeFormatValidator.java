package holdout.typenamedguava.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedguava.GuavaRefs;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class GuavaRefsTypeFormatValidator implements Validator<GuavaRefs> {

	private List<ComparisonResult> getComparisonResults(GuavaRefs o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, GuavaRefs o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("GuavaRefs", ValidationResult.ValidationType.TYPE_FORMAT, "GuavaRefs", path, "", res.getError());
				}
				return success("GuavaRefs", ValidationResult.ValidationType.TYPE_FORMAT, "GuavaRefs", path, "");
			})
			.collect(toList());
	}

}
