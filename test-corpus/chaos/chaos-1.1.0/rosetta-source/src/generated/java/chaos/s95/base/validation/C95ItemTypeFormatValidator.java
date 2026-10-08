package chaos.s95.base.validation;

import chaos.s95.base.C95Item;
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

public class C95ItemTypeFormatValidator implements Validator<C95Item> {

	private List<ComparisonResult> getComparisonResults(C95Item o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C95Item o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C95Item", ValidationResult.ValidationType.TYPE_FORMAT, "C95Item", path, "", res.getError());
				}
				return success("C95Item", ValidationResult.ValidationType.TYPE_FORMAT, "C95Item", path, "");
			})
			.collect(toList());
	}

}
