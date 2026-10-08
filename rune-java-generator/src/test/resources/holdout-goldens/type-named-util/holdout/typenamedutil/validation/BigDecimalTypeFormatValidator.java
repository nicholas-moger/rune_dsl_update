package holdout.typenamedutil.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedutil.BigDecimal;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class BigDecimalTypeFormatValidator implements Validator<BigDecimal> {

	private List<ComparisonResult> getComparisonResults(BigDecimal o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("n", o.getN(), empty(), empty(), of(new java.math.BigDecimal("0")), of(new java.math.BigDecimal("1E+1")))
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, BigDecimal o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("BigDecimal", ValidationResult.ValidationType.TYPE_FORMAT, "BigDecimal", path, "", res.getError());
				}
				return success("BigDecimal", ValidationResult.ValidationType.TYPE_FORMAT, "BigDecimal", path, "");
			})
			.collect(toList());
	}

}
