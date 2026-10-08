package chaos.s03.a5mixed.validation;

import chaos.s03.a5mixed.C3CashLeg;
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

public class C3CashLegValidator implements Validator<C3CashLeg> {

	private List<ComparisonResult> getComparisonResults(C3CashLeg o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("ccy", (String) o.getCcy() != null ? 1 : 0, 1, 1), 
				checkCardinality("common", (String) o.getCommon() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C3CashLeg o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C3CashLeg", ValidationResult.ValidationType.CARDINALITY, "C3CashLeg", path, "", res.getError());
				}
				return success("C3CashLeg", ValidationResult.ValidationType.CARDINALITY, "C3CashLeg", path, "");
			})
			.collect(toList());
	}

}
