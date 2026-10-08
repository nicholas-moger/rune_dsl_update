package chaos.s21.a2qual.validation;

import chaos.s21.a2qual.C21Exactly;
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

public class C21ExactlyTypeFormatValidator implements Validator<C21Exactly> {

	private List<ComparisonResult> getComparisonResults(C21Exactly o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C21Exactly o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C21Exactly", ValidationResult.ValidationType.TYPE_FORMAT, "C21Exactly", path, "", res.getError());
				}
				return success("C21Exactly", ValidationResult.ValidationType.TYPE_FORMAT, "C21Exactly", path, "");
			})
			.collect(toList());
	}

}
