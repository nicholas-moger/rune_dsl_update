package holdout.typenamedrosetta.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedrosetta.ListEquals;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ListEqualsTypeFormatValidator implements Validator<ListEquals> {

	private List<ComparisonResult> getComparisonResults(ListEquals o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, ListEquals o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("ListEquals", ValidationResult.ValidationType.TYPE_FORMAT, "ListEquals", path, "", res.getError());
				}
				return success("ListEquals", ValidationResult.ValidationType.TYPE_FORMAT, "ListEquals", path, "");
			})
			.collect(toList());
	}

}
