package chaos.s03.a2wild.validation;

import chaos.s03.a2wild.C3Outer;
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

public class C3OuterTypeFormatValidator implements Validator<C3Outer> {

	private List<ComparisonResult> getComparisonResults(C3Outer o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C3Outer o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C3Outer", ValidationResult.ValidationType.TYPE_FORMAT, "C3Outer", path, "", res.getError());
				}
				return success("C3Outer", ValidationResult.ValidationType.TYPE_FORMAT, "C3Outer", path, "");
			})
			.collect(toList());
	}

}
