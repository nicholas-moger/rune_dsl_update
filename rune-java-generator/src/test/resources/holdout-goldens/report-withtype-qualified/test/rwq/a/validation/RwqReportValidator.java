package test.rwq.a.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.List;
import test.rwq.a.RwqReport;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RwqReportValidator implements Validator<RwqReport> {

	private List<ComparisonResult> getComparisonResults(RwqReport o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("utiField", (String) o.getUtiField() != null ? 1 : 0, 1, 1), 
				checkCardinality("notionalField", (BigDecimal) o.getNotionalField() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RwqReport o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RwqReport", ValidationResult.ValidationType.CARDINALITY, "RwqReport", path, "", res.getError());
				}
				return success("RwqReport", ValidationResult.ValidationType.CARDINALITY, "RwqReport", path, "");
			})
			.collect(toList());
	}

}
