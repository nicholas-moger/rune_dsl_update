package test.fmeta026.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.fmeta026.ABase;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ABaseValidator implements Validator<ABase> {

	private List<ComparisonResult> getComparisonResults(ABase o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, ABase o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("ABase", ValidationResult.ValidationType.CARDINALITY, "ABase", path, "", res.getError());
				}
				return success("ABase", ValidationResult.ValidationType.CARDINALITY, "ABase", path, "");
			})
			.collect(toList());
	}

}
