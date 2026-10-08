package holdout.onlyexistsitemroot.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.onlyexistsitemroot.Paths;
import holdout.onlyexistsitemroot.Pick;
import holdout.onlyexistsitemroot.Sub;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class PathsValidator implements Validator<Paths> {

	private List<ComparisonResult> getComparisonResults(Paths o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("p", (String) o.getP() != null ? 1 : 0, 0, 1), 
				checkCardinality("q", (String) o.getQ() != null ? 1 : 0, 0, 1), 
				checkCardinality("sub", (Sub) o.getSub() != null ? 1 : 0, 0, 1), 
				checkCardinality("pick", (Pick) o.getPick() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Paths o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Paths", ValidationResult.ValidationType.CARDINALITY, "Paths", path, "", res.getError());
				}
				return success("Paths", ValidationResult.ValidationType.CARDINALITY, "Paths", path, "");
			})
			.collect(toList());
	}

}
