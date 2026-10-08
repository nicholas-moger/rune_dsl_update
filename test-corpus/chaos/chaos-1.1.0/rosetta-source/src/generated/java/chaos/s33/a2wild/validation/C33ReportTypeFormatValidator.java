package chaos.s33.a2wild.validation;

import chaos.s33.a2wild.C33Report;
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

public class C33ReportTypeFormatValidator implements Validator<C33Report> {

	private List<ComparisonResult> getComparisonResults(C33Report o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C33Report o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C33Report", ValidationResult.ValidationType.TYPE_FORMAT, "C33Report", path, "", res.getError());
				}
				return success("C33Report", ValidationResult.ValidationType.TYPE_FORMAT, "C33Report", path, "");
			})
			.collect(toList());
	}

}
