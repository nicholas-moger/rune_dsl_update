package chaos.s21.a1o2.validation;

import chaos.s21.a1o2.C21Aux;
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

public class C21AuxValidator implements Validator<C21Aux> {

	private List<ComparisonResult> getComparisonResults(C21Aux o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("ax", (String) o.getAx() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C21Aux o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C21Aux", ValidationResult.ValidationType.CARDINALITY, "C21Aux", path, "", res.getError());
				}
				return success("C21Aux", ValidationResult.ValidationType.CARDINALITY, "C21Aux", path, "");
			})
			.collect(toList());
	}

}
