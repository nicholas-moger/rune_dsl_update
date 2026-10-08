package test.wmwrapedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.wmwrapedge.Keyed;
import test.wmwrapedge.KeyedHolder;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class KeyedHolderValidator implements Validator<KeyedHolder> {

	private List<ComparisonResult> getComparisonResults(KeyedHolder o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("keyed", (Keyed) o.getKeyed() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, KeyedHolder o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("KeyedHolder", ValidationResult.ValidationType.CARDINALITY, "KeyedHolder", path, "", res.getError());
				}
				return success("KeyedHolder", ValidationResult.ValidationType.CARDINALITY, "KeyedHolder", path, "");
			})
			.collect(toList());
	}

}
