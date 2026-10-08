package chaos.s18.a5uni.validation;

import chaos.s18.a5uni.C18OptA;
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

public class C18OptAValidator implements Validator<C18OptA> {

	private List<ComparisonResult> getComparisonResults(C18OptA o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("av", (String) o.getAv() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C18OptA o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C18OptA", ValidationResult.ValidationType.CARDINALITY, "C18OptA", path, "", res.getError());
				}
				return success("C18OptA", ValidationResult.ValidationType.CARDINALITY, "C18OptA", path, "");
			})
			.collect(toList());
	}

}
