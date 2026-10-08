package test.aliasfilescope.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.aliasfilescope.Written;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class WrittenValidator implements Validator<Written> {

	private List<ComparisonResult> getComparisonResults(Written o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Written o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Written", ValidationResult.ValidationType.CARDINALITY, "Written", path, "", res.getError());
				}
				return success("Written", ValidationResult.ValidationType.CARDINALITY, "Written", path, "");
			})
			.collect(toList());
	}

}
