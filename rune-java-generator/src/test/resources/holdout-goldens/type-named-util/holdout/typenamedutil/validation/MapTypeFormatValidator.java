package holdout.typenamedutil.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedutil.Map;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class MapTypeFormatValidator implements Validator<Map> {

	private List<ComparisonResult> getComparisonResults(Map o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Map o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Map", ValidationResult.ValidationType.TYPE_FORMAT, "Map", path, "", res.getError());
				}
				return success("Map", ValidationResult.ValidationType.TYPE_FORMAT, "Map", path, "");
			})
			.collect(toList());
	}

}
