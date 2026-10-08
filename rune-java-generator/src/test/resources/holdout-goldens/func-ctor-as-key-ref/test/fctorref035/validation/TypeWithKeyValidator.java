package test.fctorref035.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.fctorref035.TypeWithKey;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class TypeWithKeyValidator implements Validator<TypeWithKey> {

	private List<ComparisonResult> getComparisonResults(TypeWithKey o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, TypeWithKey o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("TypeWithKey", ValidationResult.ValidationType.CARDINALITY, "TypeWithKey", path, "", res.getError());
				}
				return success("TypeWithKey", ValidationResult.ValidationType.CARDINALITY, "TypeWithKey", path, "");
			})
			.collect(toList());
	}

}
