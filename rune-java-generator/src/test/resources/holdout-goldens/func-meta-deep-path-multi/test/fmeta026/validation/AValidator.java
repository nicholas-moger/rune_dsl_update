package test.fmeta026.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.fmeta026.A;
import test.fmeta026.B;
import test.fmeta026.C;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class AValidator implements Validator<A> {

	private List<ComparisonResult> getComparisonResults(A o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("B", (B) o.getB() != null ? 1 : 0, 0, 1), 
				checkCardinality("C", (C) o.getC() != null ? 1 : 0, 0, 1)
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
