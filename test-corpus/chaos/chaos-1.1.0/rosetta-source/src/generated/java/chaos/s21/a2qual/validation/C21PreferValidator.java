package chaos.s21.a2qual.validation;

import chaos.s21.a2qual.C21Prefer;
import chaos.s21.a2qual.h.C21Aux;
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

public class C21PreferValidator implements Validator<C21Prefer> {

	private List<ComparisonResult> getComparisonResults(C21Prefer o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("x", (String) o.getX() != null ? 1 : 0, 0, 1), 
				checkCardinality("y", (String) o.getY() != null ? 1 : 0, 0, 1), 
				checkCardinality("z", (String) o.getZ() != null ? 1 : 0, 0, 1), 
				checkCardinality("aux", (C21Aux) o.getAux() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C21Prefer o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C21Prefer", ValidationResult.ValidationType.CARDINALITY, "C21Prefer", path, "", res.getError());
				}
				return success("C21Prefer", ValidationResult.ValidationType.CARDINALITY, "C21Prefer", path, "");
			})
			.collect(toList());
	}

}
