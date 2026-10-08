package chaos.s01.a2wild.validation;

import chaos.s01.a2wild.C1Mid;
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

public class C1MidTypeFormatValidator implements Validator<C1Mid> {

	private List<ComparisonResult> getComparisonResults(C1Mid o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C1Mid o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C1Mid", ValidationResult.ValidationType.TYPE_FORMAT, "C1Mid", path, "", res.getError());
				}
				return success("C1Mid", ValidationResult.ValidationType.TYPE_FORMAT, "C1Mid", path, "");
			})
			.collect(toList());
	}

}
