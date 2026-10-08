package chaos.s25.a2qual.validation;

import chaos.s25.a2qual.C25OptA;
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

public class C25OptAValidator implements Validator<C25OptA> {

	private List<ComparisonResult> getComparisonResults(C25OptA o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("pa", (String) o.getPa() != null ? 1 : 0, 0, 1), 
				checkCardinality("shared", (String) o.getShared() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C25OptA o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C25OptA", ValidationResult.ValidationType.CARDINALITY, "C25OptA", path, "", res.getError());
				}
				return success("C25OptA", ValidationResult.ValidationType.CARDINALITY, "C25OptA", path, "");
			})
			.collect(toList());
	}

}
