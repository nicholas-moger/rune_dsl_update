package test.voidmapedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.voidmapedge.ParamCarrier;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ParamCarrierTypeFormatValidator implements Validator<ParamCarrier> {

	private List<ComparisonResult> getComparisonResults(ParamCarrier o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, ParamCarrier o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("ParamCarrier", ValidationResult.ValidationType.TYPE_FORMAT, "ParamCarrier", path, "", res.getError());
				}
				return success("ParamCarrier", ValidationResult.ValidationType.TYPE_FORMAT, "ParamCarrier", path, "");
			})
			.collect(toList());
	}

}
