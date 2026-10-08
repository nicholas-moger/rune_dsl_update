package test.foneof024.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.foneof024.A;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class AValidator implements Validator<A> {

	private List<ComparisonResult> getComparisonResults(A o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("a1", (String) o.getA1() != null ? 1 : 0, 0, 1), 
				checkCardinality("a2", (String) o.getA2() != null ? 1 : 0, 0, 1), 
				checkCardinality("a3", (Boolean) o.getA3() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, A o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("A", ValidationResult.ValidationType.CARDINALITY, "A", path, "", res.getError());
				}
				return success("A", ValidationResult.ValidationType.CARDINALITY, "A", path, "");
			})
			.collect(toList());
	}

}
