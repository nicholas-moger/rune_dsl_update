package holdout.typenamedutil.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedutil.Consumer;
import holdout.typenamedutil.Objects;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ConsumerValidator implements Validator<Consumer> {

	private List<ComparisonResult> getComparisonResults(Consumer o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("x", (String) o.getX() != null ? 1 : 0, 0, 1), 
				checkCardinality("inner", (Objects) o.getInner() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Consumer o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Consumer", ValidationResult.ValidationType.CARDINALITY, "Consumer", path, "", res.getError());
				}
				return success("Consumer", ValidationResult.ValidationType.CARDINALITY, "Consumer", path, "");
			})
			.collect(toList());
	}

}
