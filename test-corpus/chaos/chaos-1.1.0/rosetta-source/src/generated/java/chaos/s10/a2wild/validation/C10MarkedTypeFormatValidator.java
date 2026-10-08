package chaos.s10.a2wild.validation;

import chaos.s10.a2wild.C10Marked;
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

public class C10MarkedTypeFormatValidator implements Validator<C10Marked> {

	private List<ComparisonResult> getComparisonResults(C10Marked o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C10Marked o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C10Marked", ValidationResult.ValidationType.TYPE_FORMAT, "C10Marked", path, "", res.getError());
				}
				return success("C10Marked", ValidationResult.ValidationType.TYPE_FORMAT, "C10Marked", path, "");
			})
			.collect(toList());
	}

}
