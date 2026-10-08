package chaos.s28.a3third.p3.validation;

import chaos.s28.a3third.p3.C28ChoiceReport;
import chaos.s28.a3third.p3.C28Report;
import chaos.s28.a3third.p3.C28ReportChoice;
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

public class C28ReportChoiceValidator implements Validator<C28ReportChoice> {

	private List<ComparisonResult> getComparisonResults(C28ReportChoice o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("C28ChoiceReport", (C28ChoiceReport) o.getC28ChoiceReport() != null ? 1 : 0, 0, 1), 
				checkCardinality("C28Report", (C28Report) o.getC28Report() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C28ReportChoice o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C28ReportChoice", ValidationResult.ValidationType.CARDINALITY, "C28ReportChoice", path, "", res.getError());
				}
				return success("C28ReportChoice", ValidationResult.ValidationType.CARDINALITY, "C28ReportChoice", path, "");
			})
			.collect(toList());
	}

}
