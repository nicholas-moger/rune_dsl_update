package holdout.typenamedannotations.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedannotations.RuneDataType;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RuneDataTypeTypeFormatValidator implements Validator<RuneDataType> {

	private List<ComparisonResult> getComparisonResults(RuneDataType o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RuneDataType o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RuneDataType", ValidationResult.ValidationType.TYPE_FORMAT, "RuneDataType", path, "", res.getError());
				}
				return success("RuneDataType", ValidationResult.ValidationType.TYPE_FORMAT, "RuneDataType", path, "");
			})
			.collect(toList());
	}

}
