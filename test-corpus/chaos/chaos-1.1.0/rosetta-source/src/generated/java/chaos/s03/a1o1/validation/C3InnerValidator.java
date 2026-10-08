package chaos.s03.a1o1.validation;

import chaos.s03.a1o1.C3CashLeg;
import chaos.s03.a1o1.C3Inner;
import chaos.s03.a1o1.C3StockLeg;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C3InnerValidator implements Validator<C3Inner> {

	private List<ComparisonResult> getComparisonResults(C3Inner o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("C3CashLeg", (C3CashLeg) o.getC3CashLeg() != null ? 1 : 0, 0, 1), 
				checkCardinality("C3StockLeg", (C3StockLeg) o.getC3StockLeg() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C3Inner o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C3Inner", ValidationResult.ValidationType.CARDINALITY, "C3Inner", path, "", res.getError());
				}
				return success("C3Inner", ValidationResult.ValidationType.CARDINALITY, "C3Inner", path, "");
			})
			.collect(toList());
	}

}
