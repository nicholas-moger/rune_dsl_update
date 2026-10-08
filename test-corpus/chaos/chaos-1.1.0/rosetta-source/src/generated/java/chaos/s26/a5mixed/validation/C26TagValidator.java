package chaos.s26.a5mixed.validation;

import chaos.s26.a5mixed.C26Tag;
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

public class C26TagValidator implements Validator<C26Tag> {

	private List<ComparisonResult> getComparisonResults(C26Tag o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("code", (String) o.getCode() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C26Tag o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C26Tag", ValidationResult.ValidationType.CARDINALITY, "C26Tag", path, "", res.getError());
				}
				return success("C26Tag", ValidationResult.ValidationType.CARDINALITY, "C26Tag", path, "");
			})
			.collect(toList());
	}

}
