package chaos.s07.a3third.p2.validation;

import chaos.s07.a3third.p1.C7Extra;
import chaos.s07.a3third.p2.C7Trade;
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

public class C7TradeValidator implements Validator<C7Trade> {

	private List<ComparisonResult> getComparisonResults(C7Trade o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("utid", (String) o.getUtid() != null ? 1 : 0, 1, 1), 
				checkCardinality("notional", (BigDecimal) o.getNotional() != null ? 1 : 0, 0, 1), 
				checkCardinality("extra", (C7Extra) o.getExtra() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C7Trade o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C7Trade", ValidationResult.ValidationType.CARDINALITY, "C7Trade", path, "", res.getError());
				}
				return success("C7Trade", ValidationResult.ValidationType.CARDINALITY, "C7Trade", path, "");
			})
			.collect(toList());
	}

}
