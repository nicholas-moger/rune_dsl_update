package chaos.s33.a2wild.validation;

import chaos.s33.a2wild.C33Trade;
import chaos.s33.a2wild.h.C33Extra;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C33TradeValidator implements Validator<C33Trade> {

	private List<ComparisonResult> getComparisonResults(C33Trade o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("utid", (String) o.getUtid() != null ? 1 : 0, 1, 1), 
				checkCardinality("notional", (BigDecimal) o.getNotional() != null ? 1 : 0, 0, 1), 
				checkCardinality("extra", (C33Extra) o.getExtra() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C33Trade o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C33Trade", ValidationResult.ValidationType.CARDINALITY, "C33Trade", path, "", res.getError());
				}
				return success("C33Trade", ValidationResult.ValidationType.CARDINALITY, "C33Trade", path, "");
			})
			.collect(toList());
	}

}
