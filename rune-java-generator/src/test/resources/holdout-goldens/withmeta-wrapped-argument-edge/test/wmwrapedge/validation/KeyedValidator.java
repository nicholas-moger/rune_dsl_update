package test.wmwrapedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.wmwrapedge.Keyed;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class KeyedValidator implements Validator<Keyed> {

	private List<ComparisonResult> getComparisonResults(Keyed o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("kid", (String) o.getKid() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Keyed o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Keyed", ValidationResult.ValidationType.CARDINALITY, "Keyed", path, "", res.getError());
				}
				return success("Keyed", ValidationResult.ValidationType.CARDINALITY, "Keyed", path, "");
			})
			.collect(toList());
	}

}
