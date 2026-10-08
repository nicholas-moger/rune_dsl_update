package chaos.s01.base.validation;

import chaos.s01.base.C1Leaf;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkString;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class C1LeafTypeFormatValidator implements Validator<C1Leaf> {

	private List<ComparisonResult> getComparisonResults(C1Leaf o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("ratio", o.getRatio(), empty(), empty(), of(new BigDecimal("0")), of(new BigDecimal("1E+2"))), 
				checkString("code", o.getCode(), 3, of(3), empty())
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C1Leaf o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C1Leaf", ValidationResult.ValidationType.TYPE_FORMAT, "C1Leaf", path, "", res.getError());
				}
				return success("C1Leaf", ValidationResult.ValidationType.TYPE_FORMAT, "C1Leaf", path, "");
			})
			.collect(toList());
	}

}
