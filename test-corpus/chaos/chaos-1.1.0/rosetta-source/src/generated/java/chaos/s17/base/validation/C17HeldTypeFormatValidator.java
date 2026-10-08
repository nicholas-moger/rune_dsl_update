package chaos.s17.base.validation;

import chaos.s17.base.C17Held;
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

public class C17HeldTypeFormatValidator implements Validator<C17Held> {

	private List<ComparisonResult> getComparisonResults(C17Held o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C17Held o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C17Held", ValidationResult.ValidationType.TYPE_FORMAT, "C17Held", path, "", res.getError());
				}
				return success("C17Held", ValidationResult.ValidationType.TYPE_FORMAT, "C17Held", path, "");
			})
			.collect(toList());
	}

}
