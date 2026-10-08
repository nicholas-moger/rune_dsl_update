package test.rsr.b.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.rsr.b.RsrReport;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RsrReportTypeFormatValidator implements Validator<RsrReport> {

	private List<ComparisonResult> getComparisonResults(RsrReport o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RsrReport o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RsrReport", ValidationResult.ValidationType.TYPE_FORMAT, "RsrReport", path, "", res.getError());
				}
				return success("RsrReport", ValidationResult.ValidationType.TYPE_FORMAT, "RsrReport", path, "");
			})
			.collect(toList());
	}

}
