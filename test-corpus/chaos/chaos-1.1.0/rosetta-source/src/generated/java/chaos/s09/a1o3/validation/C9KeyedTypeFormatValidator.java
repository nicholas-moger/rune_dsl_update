package chaos.s09.a1o3.validation;

import chaos.s09.a1o3.C9Keyed;
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

public class C9KeyedTypeFormatValidator implements Validator<C9Keyed> {

	private List<ComparisonResult> getComparisonResults(C9Keyed o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C9Keyed o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C9Keyed", ValidationResult.ValidationType.TYPE_FORMAT, "C9Keyed", path, "", res.getError());
				}
				return success("C9Keyed", ValidationResult.ValidationType.TYPE_FORMAT, "C9Keyed", path, "");
			})
			.collect(toList());
	}

}
