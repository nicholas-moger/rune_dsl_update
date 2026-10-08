package chaos.s24.a2dangle.validation;

import chaos.s24.a2dangle.C24Out;
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

public class C24OutTypeFormatValidator implements Validator<C24Out> {

	private List<ComparisonResult> getComparisonResults(C24Out o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C24Out o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C24Out", ValidationResult.ValidationType.TYPE_FORMAT, "C24Out", path, "", res.getError());
				}
				return success("C24Out", ValidationResult.ValidationType.TYPE_FORMAT, "C24Out", path, "");
			})
			.collect(toList());
	}

}
