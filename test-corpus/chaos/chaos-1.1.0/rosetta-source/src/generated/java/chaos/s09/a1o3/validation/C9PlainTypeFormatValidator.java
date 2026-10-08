package chaos.s09.a1o3.validation;

import chaos.s09.a1o3.C9Plain;
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

public class C9PlainTypeFormatValidator implements Validator<C9Plain> {

	private List<ComparisonResult> getComparisonResults(C9Plain o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C9Plain o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C9Plain", ValidationResult.ValidationType.TYPE_FORMAT, "C9Plain", path, "", res.getError());
				}
				return success("C9Plain", ValidationResult.ValidationType.TYPE_FORMAT, "C9Plain", path, "");
			})
			.collect(toList());
	}

}
