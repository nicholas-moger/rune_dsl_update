package test.rwq.a.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.List;
import test.rwq.a.RwqTrade;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RwqTradeValidator implements Validator<RwqTrade> {

	private List<ComparisonResult> getComparisonResults(RwqTrade o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("utid", (String) o.getUtid() != null ? 1 : 0, 1, 1), 
				checkCardinality("notional", (BigDecimal) o.getNotional() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RwqTrade o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RwqTrade", ValidationResult.ValidationType.CARDINALITY, "RwqTrade", path, "", res.getError());
				}
				return success("RwqTrade", ValidationResult.ValidationType.CARDINALITY, "RwqTrade", path, "");
			})
			.collect(toList());
	}

}
