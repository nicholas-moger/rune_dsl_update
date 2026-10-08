package test.singletolistset.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.singletolistset.NumberList;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class NumberListValidator implements Validator<NumberList> {

	private List<ComparisonResult> getComparisonResults(NumberList o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, NumberList o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("NumberList", ValidationResult.ValidationType.CARDINALITY, "NumberList", path, "", res.getError());
				}
				return success("NumberList", ValidationResult.ValidationType.CARDINALITY, "NumberList", path, "");
			})
			.collect(toList());
	}

}
