package holdout.typenamedlist.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedlist.HolderJavaFirst;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class HolderJavaFirstTypeFormatValidator implements Validator<HolderJavaFirst> {

	private List<ComparisonResult> getComparisonResults(HolderJavaFirst o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, HolderJavaFirst o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("HolderJavaFirst", ValidationResult.ValidationType.TYPE_FORMAT, "HolderJavaFirst", path, "", res.getError());
				}
				return success("HolderJavaFirst", ValidationResult.ValidationType.TYPE_FORMAT, "HolderJavaFirst", path, "");
			})
			.collect(toList());
	}

}
