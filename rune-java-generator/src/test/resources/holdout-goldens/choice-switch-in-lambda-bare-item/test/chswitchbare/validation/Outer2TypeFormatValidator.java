package test.chswitchbare.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.chswitchbare.Outer2;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class Outer2TypeFormatValidator implements Validator<Outer2> {

	private List<ComparisonResult> getComparisonResults(Outer2 o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Outer2 o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Outer2", ValidationResult.ValidationType.TYPE_FORMAT, "Outer2", path, "", res.getError());
				}
				return success("Outer2", ValidationResult.ValidationType.TYPE_FORMAT, "Outer2", path, "");
			})
			.collect(toList());
	}

}
