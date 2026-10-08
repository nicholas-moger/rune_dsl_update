package test.fmeta003.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.List;
import test.fmeta003.Foo;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class FooValidator implements Validator<Foo> {

	private List<ComparisonResult> getComparisonResults(Foo o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("a", (String) o.getA() != null ? 1 : 0, 1, 1), 
				checkCardinality("b", (Integer) o.getB() != null ? 1 : 0, 0, 1), 
				checkCardinality("c", (BigDecimal) o.getC() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Foo o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Foo", ValidationResult.ValidationType.CARDINALITY, "Foo", path, "", res.getError());
				}
				return success("Foo", ValidationResult.ValidationType.CARDINALITY, "Foo", path, "");
			})
			.collect(toList());
	}

}
