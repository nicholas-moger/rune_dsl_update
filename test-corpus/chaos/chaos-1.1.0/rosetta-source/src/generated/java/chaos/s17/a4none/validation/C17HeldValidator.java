package chaos.s17.a4none.validation;

import chaos.s17.a4none.C17Held;
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

public class C17HeldValidator implements Validator<C17Held> {

	private List<ComparisonResult> getComparisonResults(C17Held o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("h", (String) o.getH() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C17Held o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C17Held", ValidationResult.ValidationType.CARDINALITY, "C17Held", path, "", res.getError());
				}
				return success("C17Held", ValidationResult.ValidationType.CARDINALITY, "C17Held", path, "");
			})
			.collect(toList());
	}

}
