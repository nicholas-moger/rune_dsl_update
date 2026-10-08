package chaos.s18.a4snap.validation;

import chaos.s18.a4snap.C18Either;
import chaos.s18.a4snap.C18OptA;
import chaos.s18.a4snap.C18OptB;
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

public class C18EitherValidator implements Validator<C18Either> {

	private List<ComparisonResult> getComparisonResults(C18Either o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("C18OptA", (C18OptA) o.getC18OptA() != null ? 1 : 0, 0, 1), 
				checkCardinality("C18OptB", (C18OptB) o.getC18OptB() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C18Either o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C18Either", ValidationResult.ValidationType.CARDINALITY, "C18Either", path, "", res.getError());
				}
				return success("C18Either", ValidationResult.ValidationType.CARDINALITY, "C18Either", path, "");
			})
			.collect(toList());
	}

}
