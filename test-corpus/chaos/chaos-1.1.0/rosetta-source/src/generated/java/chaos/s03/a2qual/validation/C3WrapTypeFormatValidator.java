package chaos.s03.a2qual.validation;

import chaos.s03.a2qual.C3Wrap;
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

public class C3WrapTypeFormatValidator implements Validator<C3Wrap> {

	private List<ComparisonResult> getComparisonResults(C3Wrap o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C3Wrap o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C3Wrap", ValidationResult.ValidationType.TYPE_FORMAT, "C3Wrap", path, "", res.getError());
				}
				return success("C3Wrap", ValidationResult.ValidationType.TYPE_FORMAT, "C3Wrap", path, "");
			})
			.collect(toList());
	}

}
