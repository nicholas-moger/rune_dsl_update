package holdout.typenamedrosetta.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.typenamedrosetta.RosettaMetaData;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class RosettaMetaDataTypeFormatValidator implements Validator<RosettaMetaData> {

	private List<ComparisonResult> getComparisonResults(RosettaMetaData o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, RosettaMetaData o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("RosettaMetaData", ValidationResult.ValidationType.TYPE_FORMAT, "RosettaMetaData", path, "", res.getError());
				}
				return success("RosettaMetaData", ValidationResult.ValidationType.TYPE_FORMAT, "RosettaMetaData", path, "");
			})
			.collect(toList());
	}

}
