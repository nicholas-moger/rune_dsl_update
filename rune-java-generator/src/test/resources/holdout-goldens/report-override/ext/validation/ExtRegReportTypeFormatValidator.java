package ext.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import ext.ExtRegReport;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ExtRegReportTypeFormatValidator implements Validator<ExtRegReport> {

	private List<ComparisonResult> getComparisonResults(ExtRegReport o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, ExtRegReport o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("ExtRegReport", ValidationResult.ValidationType.TYPE_FORMAT, "ExtRegReport", path, "", res.getError());
				}
				return success("ExtRegReport", ValidationResult.ValidationType.TYPE_FORMAT, "ExtRegReport", path, "");
			})
			.collect(toList());
	}

}
