package test.deeppath.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.deeppath.StockLeg;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class StockLegValidator implements Validator<StockLeg> {

	private List<ComparisonResult> getComparisonResults(StockLeg o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("ticker", (String) o.getTicker() != null ? 1 : 0, 1, 1), 
				checkCardinality("common", (String) o.getCommon() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, StockLeg o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("StockLeg", ValidationResult.ValidationType.CARDINALITY, "StockLeg", path, "", res.getError());
				}
				return success("StockLeg", ValidationResult.ValidationType.CARDINALITY, "StockLeg", path, "");
			})
			.collect(toList());
	}

}
