package test.aliasscope.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.aliasscope.Loop;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class LoopValidator implements Validator<Loop> {

	private List<ComparisonResult> getComparisonResults(Loop o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Loop o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Loop", ValidationResult.ValidationType.CARDINALITY, "Loop", path, "", res.getError());
				}
				return success("Loop", ValidationResult.ValidationType.CARDINALITY, "Loop", path, "");
			})
			.collect(toList());
	}

}
