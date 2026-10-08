package test.reports.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.reports.Trade;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class TradeValidator implements Validator<Trade> {

	private List<ComparisonResult> getComparisonResults(Trade o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("tradeId", (String) o.getTradeId() != null ? 1 : 0, 1, 1), 
				checkCardinality("isActive", (Boolean) o.getIsActive() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Trade o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Trade", ValidationResult.ValidationType.CARDINALITY, "Trade", path, "", res.getError());
				}
				return success("Trade", ValidationResult.ValidationType.CARDINALITY, "Trade", path, "");
			})
			.collect(toList());
	}

}
