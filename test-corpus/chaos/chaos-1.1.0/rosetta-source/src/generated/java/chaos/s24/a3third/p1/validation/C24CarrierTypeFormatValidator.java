package chaos.s24.a3third.p1.validation;

import chaos.s24.a3third.p1.C24Carrier;
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

public class C24CarrierTypeFormatValidator implements Validator<C24Carrier> {

	private List<ComparisonResult> getComparisonResults(C24Carrier o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C24Carrier o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C24Carrier", ValidationResult.ValidationType.TYPE_FORMAT, "C24Carrier", path, "", res.getError());
				}
				return success("C24Carrier", ValidationResult.ValidationType.TYPE_FORMAT, "C24Carrier", path, "");
			})
			.collect(toList());
	}

}
