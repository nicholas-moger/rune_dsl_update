package chaos.s32.a2qual.validation;

import chaos.s32.a2qual.List;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ListValidator implements Validator<List> {

	private java.util.List<ComparisonResult> getComparisonResults(List o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public java.util.List<ValidationResult<?>> getValidationResults(RosettaPath path, List o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("List", ValidationResult.ValidationType.CARDINALITY, "List", path, "", res.getError());
				}
				return success("List", ValidationResult.ValidationType.CARDINALITY, "List", path, "");
			})
			.collect(toList());
	}

}
