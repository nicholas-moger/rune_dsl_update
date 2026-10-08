package test.pojo.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaInteger;
import java.util.List;
import java.util.stream.Collectors;
import test.pojo.Child;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class ChildTypeFormatValidator implements Validator<Child> {

	private List<ComparisonResult> getComparisonResults(Child o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("attr", o.getAttrOverriddenAsFieldWithMetaInteger().stream().map(FieldWithMetaInteger::getValue).collect(Collectors.toList()), empty(), of(0), empty(), empty())
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Child o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Child", ValidationResult.ValidationType.TYPE_FORMAT, "Child", path, "", res.getError());
				}
				return success("Child", ValidationResult.ValidationType.TYPE_FORMAT, "Child", path, "");
			})
			.collect(toList());
	}

}
