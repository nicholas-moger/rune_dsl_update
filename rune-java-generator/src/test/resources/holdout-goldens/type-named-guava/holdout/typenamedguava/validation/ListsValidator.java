package holdout.typenamedguava.validation;

import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedguava.Lists;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ListsValidator implements Validator<Lists> {

	private List<ComparisonResult> getComparisonResults(Lists o) {
		return com.google.common.collect.Lists.<ComparisonResult>newArrayList(
				checkCardinality("x", (String) o.getX() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Lists o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Lists", ValidationResult.ValidationType.CARDINALITY, "Lists", path, "", res.getError());
				}
				return success("Lists", ValidationResult.ValidationType.CARDINALITY, "Lists", path, "");
			})
			.collect(toList());
	}

}
