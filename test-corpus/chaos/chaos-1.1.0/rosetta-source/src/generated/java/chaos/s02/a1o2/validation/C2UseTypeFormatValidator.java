package chaos.s02.a1o2.validation;

import chaos.s02.a1o2.C2Use;
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

public class C2UseTypeFormatValidator implements Validator<C2Use> {

	private List<ComparisonResult> getComparisonResults(C2Use o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C2Use o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C2Use", ValidationResult.ValidationType.TYPE_FORMAT, "C2Use", path, "", res.getError());
				}
				return success("C2Use", ValidationResult.ValidationType.TYPE_FORMAT, "C2Use", path, "");
			})
			.collect(toList());
	}

}
