package chaos.s26.a1o2.validation;

import chaos.s26.a1o2.C26OptA;
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

public class C26OptATypeFormatValidator implements Validator<C26OptA> {

	private List<ComparisonResult> getComparisonResults(C26OptA o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C26OptA o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C26OptA", ValidationResult.ValidationType.TYPE_FORMAT, "C26OptA", path, "", res.getError());
				}
				return success("C26OptA", ValidationResult.ValidationType.TYPE_FORMAT, "C26OptA", path, "");
			})
			.collect(toList());
	}

}
