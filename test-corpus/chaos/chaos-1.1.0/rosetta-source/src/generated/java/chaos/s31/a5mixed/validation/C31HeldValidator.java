package chaos.s31.a5mixed.validation;

import chaos.s31.a5mixed.C31Held;
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

public class C31HeldValidator implements Validator<C31Held> {

	private List<ComparisonResult> getComparisonResults(C31Held o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("h", (String) o.getH() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C31Held o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C31Held", ValidationResult.ValidationType.CARDINALITY, "C31Held", path, "", res.getError());
				}
				return success("C31Held", ValidationResult.ValidationType.CARDINALITY, "C31Held", path, "");
			})
			.collect(toList());
	}

}
