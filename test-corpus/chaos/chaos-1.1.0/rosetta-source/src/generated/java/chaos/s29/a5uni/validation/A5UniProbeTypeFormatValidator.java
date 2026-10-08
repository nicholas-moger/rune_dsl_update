package chaos.s29.a5uni.validation;

import chaos.s29.a5uni.A5UniProbe;
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

public class A5UniProbeTypeFormatValidator implements Validator<A5UniProbe> {

	private List<ComparisonResult> getComparisonResults(A5UniProbe o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, A5UniProbe o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("A5UniProbe", ValidationResult.ValidationType.TYPE_FORMAT, "A5UniProbe", path, "", res.getError());
				}
				return success("A5UniProbe", ValidationResult.ValidationType.TYPE_FORMAT, "A5UniProbe", path, "");
			})
			.collect(toList());
	}

}
