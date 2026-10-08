package test.aliasreserved.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.aliasreserved.ResultsClash;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ResultsClashValidator implements Validator<ResultsClash> {

	private List<ComparisonResult> getComparisonResults(ResultsClash o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, ResultsClash o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("ResultsClash", ValidationResult.ValidationType.CARDINALITY, "ResultsClash", path, "", res.getError());
				}
				return success("ResultsClash", ValidationResult.ValidationType.CARDINALITY, "ResultsClash", path, "");
			})
			.collect(toList());
	}

}
