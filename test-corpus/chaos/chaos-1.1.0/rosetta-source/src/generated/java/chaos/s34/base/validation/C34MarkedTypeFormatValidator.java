package chaos.s34.base.validation;

import chaos.s34.base.C34Marked;
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

public class C34MarkedTypeFormatValidator implements Validator<C34Marked> {

	private List<ComparisonResult> getComparisonResults(C34Marked o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C34Marked o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C34Marked", ValidationResult.ValidationType.TYPE_FORMAT, "C34Marked", path, "", res.getError());
				}
				return success("C34Marked", ValidationResult.ValidationType.TYPE_FORMAT, "C34Marked", path, "");
			})
			.collect(toList());
	}

}
