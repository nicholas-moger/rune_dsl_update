package chaos.s07.a1o3.validation;

import chaos.s07.a1o3.C7Report;
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

public class C7ReportTypeFormatValidator implements Validator<C7Report> {

	private List<ComparisonResult> getComparisonResults(C7Report o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C7Report o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C7Report", ValidationResult.ValidationType.TYPE_FORMAT, "C7Report", path, "", res.getError());
				}
				return success("C7Report", ValidationResult.ValidationType.TYPE_FORMAT, "C7Report", path, "");
			})
			.collect(toList());
	}

}
