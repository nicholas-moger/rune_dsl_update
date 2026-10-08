package test.voidrender.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.voidrender.Carrier;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class CarrierTypeFormatValidator implements Validator<Carrier> {

	private List<ComparisonResult> getComparisonResults(Carrier o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Carrier o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Carrier", ValidationResult.ValidationType.TYPE_FORMAT, "Carrier", path, "", res.getError());
				}
				return success("Carrier", ValidationResult.ValidationType.TYPE_FORMAT, "Carrier", path, "");
			})
			.collect(toList());
	}

}
