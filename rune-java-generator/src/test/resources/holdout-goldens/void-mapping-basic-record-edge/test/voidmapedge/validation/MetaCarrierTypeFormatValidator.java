package test.voidmapedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.voidmapedge.MetaCarrier;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class MetaCarrierTypeFormatValidator implements Validator<MetaCarrier> {

	private List<ComparisonResult> getComparisonResults(MetaCarrier o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, MetaCarrier o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("MetaCarrier", ValidationResult.ValidationType.TYPE_FORMAT, "MetaCarrier", path, "", res.getError());
				}
				return success("MetaCarrier", ValidationResult.ValidationType.TYPE_FORMAT, "MetaCarrier", path, "");
			})
			.collect(toList());
	}

}
