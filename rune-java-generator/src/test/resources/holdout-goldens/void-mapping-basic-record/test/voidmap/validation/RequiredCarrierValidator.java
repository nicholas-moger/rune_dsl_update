package test.voidmap.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.voidmap.RequiredCarrier;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RequiredCarrierValidator implements Validator<RequiredCarrier> {

	private List<ComparisonResult> getComparisonResults(RequiredCarrier o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("tok", (Void) o.getTok() != null ? 1 : 0, 1, 1), 
				checkCardinality("span", (Void) o.getSpan() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RequiredCarrier o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RequiredCarrier", ValidationResult.ValidationType.CARDINALITY, "RequiredCarrier", path, "", res.getError());
				}
				return success("RequiredCarrier", ValidationResult.ValidationType.CARDINALITY, "RequiredCarrier", path, "");
			})
			.collect(toList());
	}

}
