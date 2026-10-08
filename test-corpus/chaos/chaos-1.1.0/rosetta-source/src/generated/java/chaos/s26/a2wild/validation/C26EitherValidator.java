package chaos.s26.a2wild.validation;

import chaos.s26.a2wild.C26Either;
import chaos.s26.a2wild.C26OptA;
import chaos.s26.a2wild.C26OptB;
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

public class C26EitherValidator implements Validator<C26Either> {

	private List<ComparisonResult> getComparisonResults(C26Either o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("C26OptA", (C26OptA) o.getC26OptA() != null ? 1 : 0, 0, 1), 
				checkCardinality("C26OptB", (C26OptB) o.getC26OptB() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C26Either o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C26Either", ValidationResult.ValidationType.CARDINALITY, "C26Either", path, "", res.getError());
				}
				return success("C26Either", ValidationResult.ValidationType.CARDINALITY, "C26Either", path, "");
			})
			.collect(toList());
	}

}
