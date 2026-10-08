package com.rosetta.test.model.agreement.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.test.model.agreement.Top;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class TopTypeFormatValidator implements Validator<Top> {

	private List<ComparisonResult> getComparisonResults(Top o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Top o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Top", ValidationResult.ValidationType.TYPE_FORMAT, "Top", path, "", res.getError());
				}
				return success("Top", ValidationResult.ValidationType.TYPE_FORMAT, "Top", path, "");
			})
			.collect(toList());
	}

}
