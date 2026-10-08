package test.rsh.a.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.List;
import test.rsh.a.RshTrade;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RshTradeValidator implements Validator<RshTrade> {

	private List<ComparisonResult> getComparisonResults(RshTrade o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("utid", (String) o.getUtid() != null ? 1 : 0, 1, 1), 
				checkCardinality("notional", (BigDecimal) o.getNotional() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RshTrade o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RshTrade", ValidationResult.ValidationType.CARDINALITY, "RshTrade", path, "", res.getError());
				}
				return success("RshTrade", ValidationResult.ValidationType.CARDINALITY, "RshTrade", path, "");
			})
			.collect(toList());
	}

}
