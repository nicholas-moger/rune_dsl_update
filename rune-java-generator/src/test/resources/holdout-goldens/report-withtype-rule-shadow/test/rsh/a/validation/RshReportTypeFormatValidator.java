package test.rsh.a.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.rsh.a.RshReport;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RshReportTypeFormatValidator implements Validator<RshReport> {

	private List<ComparisonResult> getComparisonResults(RshReport o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RshReport o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RshReport", ValidationResult.ValidationType.TYPE_FORMAT, "RshReport", path, "", res.getError());
				}
				return success("RshReport", ValidationResult.ValidationType.TYPE_FORMAT, "RshReport", path, "");
			})
			.collect(toList());
	}

}
