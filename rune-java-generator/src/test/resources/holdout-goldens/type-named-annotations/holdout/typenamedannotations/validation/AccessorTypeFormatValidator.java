package holdout.typenamedannotations.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedannotations.Accessor;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class AccessorTypeFormatValidator implements Validator<Accessor> {

	private List<ComparisonResult> getComparisonResults(Accessor o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Accessor o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Accessor", ValidationResult.ValidationType.TYPE_FORMAT, "Accessor", path, "", res.getError());
				}
				return success("Accessor", ValidationResult.ValidationType.TYPE_FORMAT, "Accessor", path, "");
			})
			.collect(toList());
	}

}
