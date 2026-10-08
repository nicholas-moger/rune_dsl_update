package chaos.s10.a2wild.validation;

import chaos.s10.a2wild.C10Marked;
import chaos.s10.a2wild.h.C10Aux;
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

public class C10MarkedValidator implements Validator<C10Marked> {

	private List<ComparisonResult> getComparisonResults(C10Marked o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("mid", (String) o.getMid() != null ? 1 : 0, 1, 1), 
				checkCardinality("aux", (C10Aux) o.getAux() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C10Marked o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C10Marked", ValidationResult.ValidationType.CARDINALITY, "C10Marked", path, "", res.getError());
				}
				return success("C10Marked", ValidationResult.ValidationType.CARDINALITY, "C10Marked", path, "");
			})
			.collect(toList());
	}

}
