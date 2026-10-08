package test.aliasscope.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.aliasscope.FieldClash;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class FieldClashValidator implements Validator<FieldClash> {

	private List<ComparisonResult> getComparisonResults(FieldClash o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, FieldClash o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("FieldClash", ValidationResult.ValidationType.CARDINALITY, "FieldClash", path, "", res.getError());
				}
				return success("FieldClash", ValidationResult.ValidationType.CARDINALITY, "FieldClash", path, "");
			})
			.collect(toList());
	}

}
