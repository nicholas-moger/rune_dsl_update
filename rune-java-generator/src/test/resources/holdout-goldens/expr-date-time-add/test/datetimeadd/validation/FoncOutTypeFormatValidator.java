package test.datetimeadd.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.datetimeadd.FoncOut;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class FoncOutTypeFormatValidator implements Validator<FoncOut> {

	private List<ComparisonResult> getComparisonResults(FoncOut o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, FoncOut o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("FoncOut", ValidationResult.ValidationType.TYPE_FORMAT, "FoncOut", path, "", res.getError());
				}
				return success("FoncOut", ValidationResult.ValidationType.TYPE_FORMAT, "FoncOut", path, "");
			})
			.collect(toList());
	}

}
