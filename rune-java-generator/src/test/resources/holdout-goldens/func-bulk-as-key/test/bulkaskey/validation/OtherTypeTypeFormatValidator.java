package test.bulkaskey.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.bulkaskey.OtherType;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class OtherTypeTypeFormatValidator implements Validator<OtherType> {

	private List<ComparisonResult> getComparisonResults(OtherType o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, OtherType o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("OtherType", ValidationResult.ValidationType.TYPE_FORMAT, "OtherType", path, "", res.getError());
				}
				return success("OtherType", ValidationResult.ValidationType.TYPE_FORMAT, "OtherType", path, "");
			})
			.collect(toList());
	}

}
