package test.chswitchedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.chswitchedge.OptB;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class OptBTypeFormatValidator implements Validator<OptB> {

	private List<ComparisonResult> getComparisonResults(OptB o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, OptB o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("OptB", ValidationResult.ValidationType.TYPE_FORMAT, "OptB", path, "", res.getError());
				}
				return success("OptB", ValidationResult.ValidationType.TYPE_FORMAT, "OptB", path, "");
			})
			.collect(toList());
	}

}
