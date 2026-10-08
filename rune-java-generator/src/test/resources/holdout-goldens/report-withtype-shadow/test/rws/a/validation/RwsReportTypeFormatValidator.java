package test.rws.a.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.rws.a.RwsReport;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RwsReportTypeFormatValidator implements Validator<RwsReport> {

	private List<ComparisonResult> getComparisonResults(RwsReport o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RwsReport o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RwsReport", ValidationResult.ValidationType.TYPE_FORMAT, "RwsReport", path, "", res.getError());
				}
				return success("RwsReport", ValidationResult.ValidationType.TYPE_FORMAT, "RwsReport", path, "");
			})
			.collect(toList());
	}

}
