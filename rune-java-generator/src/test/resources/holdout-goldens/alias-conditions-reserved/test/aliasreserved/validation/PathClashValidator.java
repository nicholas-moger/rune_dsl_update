package test.aliasreserved.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.aliasreserved.PathClash;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class PathClashValidator implements Validator<PathClash> {

	private List<ComparisonResult> getComparisonResults(PathClash o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("pa", (Integer) o.getPa() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, PathClash o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("PathClash", ValidationResult.ValidationType.CARDINALITY, "PathClash", path, "", res.getError());
				}
				return success("PathClash", ValidationResult.ValidationType.CARDINALITY, "PathClash", path, "");
			})
			.collect(toList());
	}

}
