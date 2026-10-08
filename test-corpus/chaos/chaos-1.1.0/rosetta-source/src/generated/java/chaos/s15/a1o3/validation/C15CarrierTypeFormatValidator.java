package chaos.s15.a1o3.validation;

import chaos.s15.a1o3.C15Carrier;
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

public class C15CarrierTypeFormatValidator implements Validator<C15Carrier> {

	private List<ComparisonResult> getComparisonResults(C15Carrier o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C15Carrier o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C15Carrier", ValidationResult.ValidationType.TYPE_FORMAT, "C15Carrier", path, "", res.getError());
				}
				return success("C15Carrier", ValidationResult.ValidationType.TYPE_FORMAT, "C15Carrier", path, "");
			})
			.collect(toList());
	}

}
