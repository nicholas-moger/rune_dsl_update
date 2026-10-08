package test.deeppathedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.deeppathedge.Inner;
import test.deeppathedge.Wrap;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class WrapValidator implements Validator<Wrap> {

	private List<ComparisonResult> getComparisonResults(Wrap o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("inner", (Inner) o.getInner() != null ? 1 : 0, 1, 1), 
				checkCardinality("text", (String) o.getText() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Wrap o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Wrap", ValidationResult.ValidationType.CARDINALITY, "Wrap", path, "", res.getError());
				}
				return success("Wrap", ValidationResult.ValidationType.CARDINALITY, "Wrap", path, "");
			})
			.collect(toList());
	}

}
