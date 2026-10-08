package reg.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import reg.RegReport;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RegReportTypeFormatValidator implements Validator<RegReport> {

	private List<ComparisonResult> getComparisonResults(RegReport o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RegReport o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RegReport", ValidationResult.ValidationType.TYPE_FORMAT, "RegReport", path, "", res.getError());
				}
				return success("RegReport", ValidationResult.ValidationType.TYPE_FORMAT, "RegReport", path, "");
			})
			.collect(toList());
	}

}
