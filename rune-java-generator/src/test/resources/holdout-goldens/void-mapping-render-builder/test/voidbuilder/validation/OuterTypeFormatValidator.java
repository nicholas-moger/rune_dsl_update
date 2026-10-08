package test.voidbuilder.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.voidbuilder.Outer;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class OuterTypeFormatValidator implements Validator<Outer> {

	private List<ComparisonResult> getComparisonResults(Outer o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Outer o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Outer", ValidationResult.ValidationType.TYPE_FORMAT, "Outer", path, "", res.getError());
				}
				return success("Outer", ValidationResult.ValidationType.TYPE_FORMAT, "Outer", path, "");
			})
			.collect(toList());
	}

}
