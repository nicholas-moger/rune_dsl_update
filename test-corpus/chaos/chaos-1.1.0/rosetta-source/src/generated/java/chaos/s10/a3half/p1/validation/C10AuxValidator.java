package chaos.s10.a3half.p1.validation;

import chaos.s10.a3half.p1.C10Aux;
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

public class C10AuxValidator implements Validator<C10Aux> {

	private List<ComparisonResult> getComparisonResults(C10Aux o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("aux", (String) o.getAux() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C10Aux o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C10Aux", ValidationResult.ValidationType.CARDINALITY, "C10Aux", path, "", res.getError());
				}
				return success("C10Aux", ValidationResult.ValidationType.CARDINALITY, "C10Aux", path, "");
			})
			.collect(toList());
	}

}
