package test.rsp.p3.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.rsp.p3.RspReport;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RspReportTypeFormatValidator implements Validator<RspReport> {

	private List<ComparisonResult> getComparisonResults(RspReport o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RspReport o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RspReport", ValidationResult.ValidationType.TYPE_FORMAT, "RspReport", path, "", res.getError());
				}
				return success("RspReport", ValidationResult.ValidationType.TYPE_FORMAT, "RspReport", path, "");
			})
			.collect(toList());
	}

}
