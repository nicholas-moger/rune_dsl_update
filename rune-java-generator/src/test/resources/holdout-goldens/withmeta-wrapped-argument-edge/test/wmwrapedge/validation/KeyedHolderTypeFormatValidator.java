package test.wmwrapedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.wmwrapedge.KeyedHolder;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class KeyedHolderTypeFormatValidator implements Validator<KeyedHolder> {

	private List<ComparisonResult> getComparisonResults(KeyedHolder o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, KeyedHolder o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("KeyedHolder", ValidationResult.ValidationType.TYPE_FORMAT, "KeyedHolder", path, "", res.getError());
				}
				return success("KeyedHolder", ValidationResult.ValidationType.TYPE_FORMAT, "KeyedHolder", path, "");
			})
			.collect(toList());
	}

}
