package holdout.typenamedutil.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedutil.ArrayList;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ArrayListTypeFormatValidator implements Validator<ArrayList> {

	private List<ComparisonResult> getComparisonResults(ArrayList o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, ArrayList o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("ArrayList", ValidationResult.ValidationType.TYPE_FORMAT, "ArrayList", path, "", res.getError());
				}
				return success("ArrayList", ValidationResult.ValidationType.TYPE_FORMAT, "ArrayList", path, "");
			})
			.collect(toList());
	}

}
