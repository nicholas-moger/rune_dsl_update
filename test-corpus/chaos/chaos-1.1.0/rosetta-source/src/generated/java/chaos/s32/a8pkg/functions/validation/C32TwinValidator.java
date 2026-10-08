package chaos.s32.a8pkg.functions.validation;

import chaos.s32.a8pkg.functions.C32Twin;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C32TwinValidator implements Validator<C32Twin> {

	private List<ComparisonResult> getComparisonResults(C32Twin o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("pathMark", (String) o.getPathMark() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C32Twin o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C32Twin", ValidationResult.ValidationType.CARDINALITY, "C32Twin", path, "", res.getError());
				}
				return success("C32Twin", ValidationResult.ValidationType.CARDINALITY, "C32Twin", path, "");
			})
			.collect(toList());
	}

}
