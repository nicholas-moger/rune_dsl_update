package test.fmeta026.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaInteger;
import java.util.List;
import java.util.stream.Collectors;
import test.fmeta026.B;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class BTypeFormatValidator implements Validator<B> {

	private List<ComparisonResult> getComparisonResults(B o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("prop", o.getProp().stream().map(FieldWithMetaInteger::getValue).collect(Collectors.toList()), empty(), of(0), empty(), empty())
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, B o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("B", ValidationResult.ValidationType.TYPE_FORMAT, "B", path, "", res.getError());
				}
				return success("B", ValidationResult.ValidationType.TYPE_FORMAT, "B", path, "");
			})
			.collect(toList());
	}

}
