package chaos.s04.a1o3.validation;

import chaos.s04.a1o3.C4Pair;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C4PairTypeFormatValidator implements Validator<C4Pair> {

	private List<ComparisonResult> getComparisonResults(C4Pair o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C4Pair o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C4Pair", ValidationResult.ValidationType.TYPE_FORMAT, "C4Pair", path, "", res.getError());
				}
				return success("C4Pair", ValidationResult.ValidationType.TYPE_FORMAT, "C4Pair", path, "");
			})
			.collect(toList());
	}

}
