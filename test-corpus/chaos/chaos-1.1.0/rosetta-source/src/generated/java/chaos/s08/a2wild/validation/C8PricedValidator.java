package chaos.s08.a2wild.validation;

import chaos.s08.a2wild.C8Priced;
import chaos.s08.a2wild.h.C8Box;
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

public class C8PricedValidator implements Validator<C8Priced> {

	private List<ComparisonResult> getComparisonResults(C8Priced o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("qty", (Integer) o.getQty() != null ? 1 : 0, 1, 1), 
				checkCardinality("ccy", (String) o.getCcy() != null ? 1 : 0, 0, 1), 
				checkCardinality("box", (C8Box) o.getBox() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C8Priced o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C8Priced", ValidationResult.ValidationType.CARDINALITY, "C8Priced", path, "", res.getError());
				}
				return success("C8Priced", ValidationResult.ValidationType.CARDINALITY, "C8Priced", path, "");
			})
			.collect(toList());
	}

}
