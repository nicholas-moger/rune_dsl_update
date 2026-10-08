package chaos.s29.a1o2.validation;

import chaos.s29.a1o2.C29Bag;
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

public class C29BagTypeFormatValidator implements Validator<C29Bag> {

	private List<ComparisonResult> getComparisonResults(C29Bag o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C29Bag o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C29Bag", ValidationResult.ValidationType.TYPE_FORMAT, "C29Bag", path, "", res.getError());
				}
				return success("C29Bag", ValidationResult.ValidationType.TYPE_FORMAT, "C29Bag", path, "");
			})
			.collect(toList());
	}

}
