package chaos.s26.a2wild.validation;

import chaos.s26.a2wild.C26OptA;
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

public class C26OptAValidator implements Validator<C26OptA> {

	private List<ComparisonResult> getComparisonResults(C26OptA o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("av", (String) o.getAv() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C26OptA o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C26OptA", ValidationResult.ValidationType.CARDINALITY, "C26OptA", path, "", res.getError());
				}
				return success("C26OptA", ValidationResult.ValidationType.CARDINALITY, "C26OptA", path, "");
			})
			.collect(toList());
	}

}
