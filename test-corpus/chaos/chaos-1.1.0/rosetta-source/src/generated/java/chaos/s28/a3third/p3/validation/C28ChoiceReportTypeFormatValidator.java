package chaos.s28.a3third.p3.validation;

import chaos.s28.a3third.p3.C28ChoiceReport;
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

public class C28ChoiceReportTypeFormatValidator implements Validator<C28ChoiceReport> {

	private List<ComparisonResult> getComparisonResults(C28ChoiceReport o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C28ChoiceReport o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C28ChoiceReport", ValidationResult.ValidationType.TYPE_FORMAT, "C28ChoiceReport", path, "", res.getError());
				}
				return success("C28ChoiceReport", ValidationResult.ValidationType.TYPE_FORMAT, "C28ChoiceReport", path, "");
			})
			.collect(toList());
	}

}
