package holdout.typenamedrosetta.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedrosetta.RosettaModelObject;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RosettaModelObjectTypeFormatValidator implements Validator<RosettaModelObject> {

	private List<ComparisonResult> getComparisonResults(RosettaModelObject o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RosettaModelObject o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RosettaModelObject", ValidationResult.ValidationType.TYPE_FORMAT, "RosettaModelObject", path, "", res.getError());
				}
				return success("RosettaModelObject", ValidationResult.ValidationType.TYPE_FORMAT, "RosettaModelObject", path, "");
			})
			.collect(toList());
	}

}
