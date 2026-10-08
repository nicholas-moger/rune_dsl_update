package holdout.onlyexistsitemroot.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.onlyexistsitemroot.OptA;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class OptAValidator implements Validator<OptA> {

	private List<ComparisonResult> getComparisonResults(OptA o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("av", (String) o.getAv() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, OptA o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("OptA", ValidationResult.ValidationType.CARDINALITY, "OptA", path, "", res.getError());
				}
				return success("OptA", ValidationResult.ValidationType.CARDINALITY, "OptA", path, "");
			})
			.collect(toList());
	}

}
