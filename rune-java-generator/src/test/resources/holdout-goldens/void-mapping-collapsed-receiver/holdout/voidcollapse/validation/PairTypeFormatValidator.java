package holdout.voidcollapse.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.voidcollapse.Pair;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class PairTypeFormatValidator implements Validator<Pair> {

	private List<ComparisonResult> getComparisonResults(Pair o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Pair o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Pair", ValidationResult.ValidationType.TYPE_FORMAT, "Pair", path, "", res.getError());
				}
				return success("Pair", ValidationResult.ValidationType.TYPE_FORMAT, "Pair", path, "");
			})
			.collect(toList());
	}

}
