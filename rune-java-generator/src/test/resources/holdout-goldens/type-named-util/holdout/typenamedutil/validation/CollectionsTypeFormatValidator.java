package holdout.typenamedutil.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedutil.Collections;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class CollectionsTypeFormatValidator implements Validator<Collections> {

	private List<ComparisonResult> getComparisonResults(Collections o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Collections o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Collections", ValidationResult.ValidationType.TYPE_FORMAT, "Collections", path, "", res.getError());
				}
				return success("Collections", ValidationResult.ValidationType.TYPE_FORMAT, "Collections", path, "");
			})
			.collect(toList());
	}

}
