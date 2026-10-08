package test.reg.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import test.reg.AttributeReport;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class AttributeReportValidator implements Validator<AttributeReport> {

	private List<ComparisonResult> getComparisonResults(AttributeReport o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("heroInt", (Integer) o.getHeroInt() != null ? 1 : 0, 1, 1), 
				checkCardinality("heroNumber", (BigDecimal) o.getHeroNumber() != null ? 1 : 0, 1, 1), 
				checkCardinality("heroTime", (LocalTime) o.getHeroTime() != null ? 1 : 0, 1, 1), 
				checkCardinality("heroZonedDateTime", (ZonedDateTime) o.getHeroZonedDateTime() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, AttributeReport o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("AttributeReport", ValidationResult.ValidationType.CARDINALITY, "AttributeReport", path, "", res.getError());
				}
				return success("AttributeReport", ValidationResult.ValidationType.CARDINALITY, "AttributeReport", path, "");
			})
			.collect(toList());
	}

}
