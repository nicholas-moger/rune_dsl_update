package test.aliascond.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.List;
import test.aliascond.Bare;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class BareTypeFormatValidator implements Validator<Bare> {

	private List<ComparisonResult> getComparisonResults(Bare o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("qty", o.getQty(), empty(), empty(), of(new BigDecimal("0")), empty())
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Bare o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Bare", ValidationResult.ValidationType.TYPE_FORMAT, "Bare", path, "", res.getError());
				}
				return success("Bare", ValidationResult.ValidationType.TYPE_FORMAT, "Bare", path, "");
			})
			.collect(toList());
	}

}
