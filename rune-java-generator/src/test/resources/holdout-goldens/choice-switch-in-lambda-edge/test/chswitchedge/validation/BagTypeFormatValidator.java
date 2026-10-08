package test.chswitchedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.chswitchedge.Bag;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class BagTypeFormatValidator implements Validator<Bag> {

	private List<ComparisonResult> getComparisonResults(Bag o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Bag o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Bag", ValidationResult.ValidationType.TYPE_FORMAT, "Bag", path, "", res.getError());
				}
				return success("Bag", ValidationResult.ValidationType.TYPE_FORMAT, "Bag", path, "");
			})
			.collect(toList());
	}

}
