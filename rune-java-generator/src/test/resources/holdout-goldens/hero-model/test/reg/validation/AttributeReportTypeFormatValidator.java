package test.reg.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.reg.AttributeReport;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class AttributeReportTypeFormatValidator implements Validator<AttributeReport> {

	private List<ComparisonResult> getComparisonResults(AttributeReport o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("heroInt", o.getHeroInt(), empty(), of(0), empty(), empty())
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, AttributeReport o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("AttributeReport", ValidationResult.ValidationType.TYPE_FORMAT, "AttributeReport", path, "", res.getError());
				}
				return success("AttributeReport", ValidationResult.ValidationType.TYPE_FORMAT, "AttributeReport", path, "");
			})
			.collect(toList());
	}

}
