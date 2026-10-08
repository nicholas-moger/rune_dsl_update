package holdout.voiddeeptok.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.voiddeeptok.HolderA;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class HolderATypeFormatValidator implements Validator<HolderA> {

	private List<ComparisonResult> getComparisonResults(HolderA o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, HolderA o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("HolderA", ValidationResult.ValidationType.TYPE_FORMAT, "HolderA", path, "", res.getError());
				}
				return success("HolderA", ValidationResult.ValidationType.TYPE_FORMAT, "HolderA", path, "");
			})
			.collect(toList());
	}

}
