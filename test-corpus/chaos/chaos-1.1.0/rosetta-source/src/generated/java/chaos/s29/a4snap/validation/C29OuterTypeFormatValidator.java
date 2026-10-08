package chaos.s29.a4snap.validation;

import chaos.s29.a4snap.C29Outer;
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

public class C29OuterTypeFormatValidator implements Validator<C29Outer> {

	private List<ComparisonResult> getComparisonResults(C29Outer o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C29Outer o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C29Outer", ValidationResult.ValidationType.TYPE_FORMAT, "C29Outer", path, "", res.getError());
				}
				return success("C29Outer", ValidationResult.ValidationType.TYPE_FORMAT, "C29Outer", path, "");
			})
			.collect(toList());
	}

}
