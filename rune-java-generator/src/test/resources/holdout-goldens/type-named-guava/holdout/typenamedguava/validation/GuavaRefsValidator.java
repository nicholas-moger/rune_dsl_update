package holdout.typenamedguava.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedguava.GuavaRefs;
import holdout.typenamedguava.ImmutableList;
import holdout.typenamedguava.ImmutableMap;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class GuavaRefsValidator implements Validator<GuavaRefs> {

	private List<ComparisonResult> getComparisonResults(GuavaRefs o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("immutableList", (ImmutableList) o.getImmutableList() != null ? 1 : 0, 0, 1), 
				checkCardinality("immutableMap", (ImmutableMap) o.getImmutableMap() != null ? 1 : 0, 0, 1), 
				checkCardinality("lists", (holdout.typenamedguava.Lists) o.getLists() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, GuavaRefs o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("GuavaRefs", ValidationResult.ValidationType.CARDINALITY, "GuavaRefs", path, "", res.getError());
				}
				return success("GuavaRefs", ValidationResult.ValidationType.CARDINALITY, "GuavaRefs", path, "");
			})
			.collect(toList());
	}

}
