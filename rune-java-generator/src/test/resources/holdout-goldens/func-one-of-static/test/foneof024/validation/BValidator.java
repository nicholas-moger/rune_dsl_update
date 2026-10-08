package test.foneof024.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.foneof024.B;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class BValidator implements Validator<B> {

	private List<ComparisonResult> getComparisonResults(B o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("a1", (String) o.getA1() != null ? 1 : 0, 0, 1), 
				checkCardinality("a2", (String) o.getA2() != null ? 1 : 0, 0, 1), 
				checkCardinality("a3", (Boolean) o.getA3() != null ? 1 : 0, 0, 1), 
				checkCardinality("b1", (String) o.getB1() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, B o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("B", ValidationResult.ValidationType.CARDINALITY, "B", path, "", res.getError());
				}
				return success("B", ValidationResult.ValidationType.CARDINALITY, "B", path, "");
			})
			.collect(toList());
	}

}
