package holdout.typenamedutil.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedutil.UtilRefs;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class UtilRefsTypeFormatValidator implements Validator<UtilRefs> {

	private List<ComparisonResult> getComparisonResults(UtilRefs o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, UtilRefs o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("UtilRefs", ValidationResult.ValidationType.TYPE_FORMAT, "UtilRefs", path, "", res.getError());
				}
				return success("UtilRefs", ValidationResult.ValidationType.TYPE_FORMAT, "UtilRefs", path, "");
			})
			.collect(toList());
	}

}
