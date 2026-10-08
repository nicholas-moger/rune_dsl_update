package holdout.typenamedutil.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedutil.Pattern;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class PatternValidator implements Validator<Pattern> {

	private List<ComparisonResult> getComparisonResults(Pattern o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("p", (String) o.getP() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Pattern o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Pattern", ValidationResult.ValidationType.CARDINALITY, "Pattern", path, "", res.getError());
				}
				return success("Pattern", ValidationResult.ValidationType.CARDINALITY, "Pattern", path, "");
			})
			.collect(toList());
	}

}
