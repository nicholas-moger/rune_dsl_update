package test.reg.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.reg.SokoviaAccordsReport;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class SokoviaAccordsReportTypeFormatValidator implements Validator<SokoviaAccordsReport> {

	private List<ComparisonResult> getComparisonResults(SokoviaAccordsReport o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, SokoviaAccordsReport o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("SokoviaAccordsReport", ValidationResult.ValidationType.TYPE_FORMAT, "SokoviaAccordsReport", path, "", res.getError());
				}
				return success("SokoviaAccordsReport", ValidationResult.ValidationType.TYPE_FORMAT, "SokoviaAccordsReport", path, "");
			})
			.collect(toList());
	}

}
