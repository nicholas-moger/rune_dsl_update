package chaos.s19.a3hub.p2.validation;

import chaos.s19.a3hub.p2.C19Whole;
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

public class C19WholeTypeFormatValidator implements Validator<C19Whole> {

	private List<ComparisonResult> getComparisonResults(C19Whole o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C19Whole o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C19Whole", ValidationResult.ValidationType.TYPE_FORMAT, "C19Whole", path, "", res.getError());
				}
				return success("C19Whole", ValidationResult.ValidationType.TYPE_FORMAT, "C19Whole", path, "");
			})
			.collect(toList());
	}

}
