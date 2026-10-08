package chaos.s25.a9comment.validation;

import chaos.s25.a9comment.C25OptA;
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

public class C25OptATypeFormatValidator implements Validator<C25OptA> {

	private List<ComparisonResult> getComparisonResults(C25OptA o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C25OptA o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C25OptA", ValidationResult.ValidationType.TYPE_FORMAT, "C25OptA", path, "", res.getError());
				}
				return success("C25OptA", ValidationResult.ValidationType.TYPE_FORMAT, "C25OptA", path, "");
			})
			.collect(toList());
	}

}
