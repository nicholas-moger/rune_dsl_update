package chaos.s28.a1o2.validation;

import chaos.s28.a1o2.C28ChoiceReport;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C28ChoiceReportValidator implements Validator<C28ChoiceReport> {

	private List<ComparisonResult> getComparisonResults(C28ChoiceReport o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("utiField", (String) o.getUtiField() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C28ChoiceReport o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C28ChoiceReport", ValidationResult.ValidationType.CARDINALITY, "C28ChoiceReport", path, "", res.getError());
				}
				return success("C28ChoiceReport", ValidationResult.ValidationType.CARDINALITY, "C28ChoiceReport", path, "");
			})
			.collect(toList());
	}

}
