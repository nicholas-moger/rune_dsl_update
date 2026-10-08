package holdout.typenamedannotations.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedannotations.RosettaDataType;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RosettaDataTypeTypeFormatValidator implements Validator<RosettaDataType> {

	private List<ComparisonResult> getComparisonResults(RosettaDataType o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RosettaDataType o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RosettaDataType", ValidationResult.ValidationType.TYPE_FORMAT, "RosettaDataType", path, "", res.getError());
				}
				return success("RosettaDataType", ValidationResult.ValidationType.TYPE_FORMAT, "RosettaDataType", path, "");
			})
			.collect(toList());
	}

}
