package test.aliasreserved.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.aliasreserved.Keywords;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class KeywordsValidator implements Validator<Keywords> {

	private List<ComparisonResult> getComparisonResults(Keywords o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Keywords o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Keywords", ValidationResult.ValidationType.CARDINALITY, "Keywords", path, "", res.getError());
				}
				return success("Keywords", ValidationResult.ValidationType.CARDINALITY, "Keywords", path, "");
			})
			.collect(toList());
	}

}
