package test.reg.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.reg.Attribute;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class AttributeTypeFormatValidator implements Validator<Attribute> {

	private List<ComparisonResult> getComparisonResults(Attribute o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("heroInt", o.getHeroInt(), empty(), of(0), empty(), empty())
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Attribute o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Attribute", ValidationResult.ValidationType.TYPE_FORMAT, "Attribute", path, "", res.getError());
				}
				return success("Attribute", ValidationResult.ValidationType.TYPE_FORMAT, "Attribute", path, "");
			})
			.collect(toList());
	}

}
