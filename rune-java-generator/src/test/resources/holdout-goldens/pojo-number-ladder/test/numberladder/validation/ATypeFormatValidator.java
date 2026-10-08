package test.numberladder.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.numberladder.A;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class ATypeFormatValidator implements Validator<A> {

	private List<ComparisonResult> getComparisonResults(A o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("integers", o.getIntegers(), of(8), of(0), empty(), empty()), 
				checkNumber("long", o.getLong(), of(10), of(0), empty(), empty()), 
				checkNumber("bigInteger", o.getBigInteger(), of(20), of(0), empty(), empty())
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, A o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("A", ValidationResult.ValidationType.TYPE_FORMAT, "A", path, "", res.getError());
				}
				return success("A", ValidationResult.ValidationType.TYPE_FORMAT, "A", path, "");
			})
			.collect(toList());
	}

}
