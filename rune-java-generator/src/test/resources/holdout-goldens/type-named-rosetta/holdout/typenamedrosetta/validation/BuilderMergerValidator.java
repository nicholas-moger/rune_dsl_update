package holdout.typenamedrosetta.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedrosetta.BuilderMerger;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class BuilderMergerValidator implements Validator<BuilderMerger> {

	private List<ComparisonResult> getComparisonResults(BuilderMerger o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("x", (String) o.getX() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, BuilderMerger o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("BuilderMerger", ValidationResult.ValidationType.CARDINALITY, "BuilderMerger", path, "", res.getError());
				}
				return success("BuilderMerger", ValidationResult.ValidationType.CARDINALITY, "BuilderMerger", path, "");
			})
			.collect(toList());
	}

}
