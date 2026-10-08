package holdout.typenamedannotations.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedannotations.RuneAttribute;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RuneAttributeTypeFormatValidator implements Validator<RuneAttribute> {

	private List<ComparisonResult> getComparisonResults(RuneAttribute o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RuneAttribute o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RuneAttribute", ValidationResult.ValidationType.TYPE_FORMAT, "RuneAttribute", path, "", res.getError());
				}
				return success("RuneAttribute", ValidationResult.ValidationType.TYPE_FORMAT, "RuneAttribute", path, "");
			})
			.collect(toList());
	}

}
