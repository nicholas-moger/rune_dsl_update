package test.aliasfilescope.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.aliasfilescope.Own;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class OwnValidator implements Validator<Own> {

	private List<ComparisonResult> getComparisonResults(Own o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("v", (Integer) o.getV() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Own o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Own", ValidationResult.ValidationType.CARDINALITY, "Own", path, "", res.getError());
				}
				return success("Own", ValidationResult.ValidationType.CARDINALITY, "Own", path, "");
			})
			.collect(toList());
	}

}
