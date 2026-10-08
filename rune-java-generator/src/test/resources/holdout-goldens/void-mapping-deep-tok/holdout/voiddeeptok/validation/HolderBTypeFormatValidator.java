package holdout.voiddeeptok.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.voiddeeptok.HolderB;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class HolderBTypeFormatValidator implements Validator<HolderB> {

	private List<ComparisonResult> getComparisonResults(HolderB o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, HolderB o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("HolderB", ValidationResult.ValidationType.TYPE_FORMAT, "HolderB", path, "", res.getError());
				}
				return success("HolderB", ValidationResult.ValidationType.TYPE_FORMAT, "HolderB", path, "");
			})
			.collect(toList());
	}

}
