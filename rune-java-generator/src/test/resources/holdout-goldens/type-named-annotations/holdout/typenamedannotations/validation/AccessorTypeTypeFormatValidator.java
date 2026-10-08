package holdout.typenamedannotations.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedannotations.AccessorType;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class AccessorTypeTypeFormatValidator implements Validator<AccessorType> {

	private List<ComparisonResult> getComparisonResults(AccessorType o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, AccessorType o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("AccessorType", ValidationResult.ValidationType.TYPE_FORMAT, "AccessorType", path, "", res.getError());
				}
				return success("AccessorType", ValidationResult.ValidationType.TYPE_FORMAT, "AccessorType", path, "");
			})
			.collect(toList());
	}

}
