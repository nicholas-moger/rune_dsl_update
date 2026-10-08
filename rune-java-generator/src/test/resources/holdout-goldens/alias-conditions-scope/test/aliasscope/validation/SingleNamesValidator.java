package test.aliasscope.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.aliasscope.SingleNames;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class SingleNamesValidator implements Validator<SingleNames> {

	private List<ComparisonResult> getComparisonResults(SingleNames o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("results", (Integer) o.getResults() != null ? 1 : 0, 0, 1), 
				checkCardinality("o", (Integer) o.getO() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, SingleNames o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("SingleNames", ValidationResult.ValidationType.CARDINALITY, "SingleNames", path, "", res.getError());
				}
				return success("SingleNames", ValidationResult.ValidationType.CARDINALITY, "SingleNames", path, "");
			})
			.collect(toList());
	}

}
