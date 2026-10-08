package test.datesubtract.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.records.Date;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.datesubtract.Test;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class TestValidator implements Validator<Test> {

	private List<ComparisonResult> getComparisonResults(Test o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("one", (Date) o.getOne() != null ? 1 : 0, 1, 1), 
				checkCardinality("two", (Date) o.getTwo() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Test o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Test", ValidationResult.ValidationType.CARDINALITY, "Test", path, "", res.getError());
				}
				return success("Test", ValidationResult.ValidationType.CARDINALITY, "Test", path, "");
			})
			.collect(toList());
	}

}
