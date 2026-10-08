package test.datetimeadd.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.time.LocalDateTime;
import java.util.List;
import test.datetimeadd.FoncOut;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class FoncOutValidator implements Validator<FoncOut> {

	private List<ComparisonResult> getComparisonResults(FoncOut o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("res1", (LocalDateTime) o.getRes1() != null ? 1 : 0, 1, 1), 
				checkCardinality("res2", (LocalDateTime) o.getRes2() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, FoncOut o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("FoncOut", ValidationResult.ValidationType.CARDINALITY, "FoncOut", path, "", res.getError());
				}
				return success("FoncOut", ValidationResult.ValidationType.CARDINALITY, "FoncOut", path, "");
			})
			.collect(toList());
	}

}
