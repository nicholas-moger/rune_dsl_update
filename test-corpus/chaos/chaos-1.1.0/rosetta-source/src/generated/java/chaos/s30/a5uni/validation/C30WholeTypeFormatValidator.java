package chaos.s30.a5uni.validation;

import chaos.s30.a5uni.C30Whole;
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

public class C30WholeTypeFormatValidator implements Validator<C30Whole> {

	private List<ComparisonResult> getComparisonResults(C30Whole o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C30Whole o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C30Whole", ValidationResult.ValidationType.TYPE_FORMAT, "C30Whole", path, "", res.getError());
				}
				return success("C30Whole", ValidationResult.ValidationType.TYPE_FORMAT, "C30Whole", path, "");
			})
			.collect(toList());
	}

}
