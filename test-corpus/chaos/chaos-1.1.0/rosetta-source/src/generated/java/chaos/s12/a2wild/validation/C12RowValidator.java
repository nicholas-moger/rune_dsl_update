package chaos.s12.a2wild.validation;

import chaos.s12.a2wild.C12Row;
import chaos.s12.a2wild.h.C12Aux;
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

public class C12RowValidator implements Validator<C12Row> {

	private List<ComparisonResult> getComparisonResults(C12Row o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("key", (String) o.getKey() != null ? 1 : 0, 1, 1), 
				checkCardinality("val", (BigDecimal) o.getVal() != null ? 1 : 0, 0, 1), 
				checkCardinality("aux", (C12Aux) o.getAux() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C12Row o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C12Row", ValidationResult.ValidationType.CARDINALITY, "C12Row", path, "", res.getError());
				}
				return success("C12Row", ValidationResult.ValidationType.CARDINALITY, "C12Row", path, "");
			})
			.collect(toList());
	}

}
