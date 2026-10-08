package chaos.s29.a1o1.validation;

import chaos.s29.a1o1.C29Inner;
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

public class C29InnerTypeFormatValidator implements Validator<C29Inner> {

	private List<ComparisonResult> getComparisonResults(C29Inner o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C29Inner o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C29Inner", ValidationResult.ValidationType.TYPE_FORMAT, "C29Inner", path, "", res.getError());
				}
				return success("C29Inner", ValidationResult.ValidationType.TYPE_FORMAT, "C29Inner", path, "");
			})
			.collect(toList());
	}

}
