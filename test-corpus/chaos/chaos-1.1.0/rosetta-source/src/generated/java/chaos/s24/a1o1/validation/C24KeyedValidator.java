package chaos.s24.a1o1.validation;

import chaos.s24.a1o1.C24Keyed;
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

public class C24KeyedValidator implements Validator<C24Keyed> {

	private List<ComparisonResult> getComparisonResults(C24Keyed o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("kid", (String) o.getKid() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C24Keyed o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C24Keyed", ValidationResult.ValidationType.CARDINALITY, "C24Keyed", path, "", res.getError());
				}
				return success("C24Keyed", ValidationResult.ValidationType.CARDINALITY, "C24Keyed", path, "");
			})
			.collect(toList());
	}

}
