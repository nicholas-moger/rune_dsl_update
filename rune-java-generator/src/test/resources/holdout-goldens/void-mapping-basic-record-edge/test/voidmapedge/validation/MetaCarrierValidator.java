package test.voidmapedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaVoid;
import com.rosetta.model.metafields.ReferenceWithMetaVoid;
import java.util.List;
import test.voidmapedge.MetaCarrier;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class MetaCarrierValidator implements Validator<MetaCarrier> {

	private List<ComparisonResult> getComparisonResults(MetaCarrier o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("tok", (FieldWithMetaVoid) o.getTok() != null ? 1 : 0, 0, 1), 
				checkCardinality("ref", (ReferenceWithMetaVoid) o.getRef() != null ? 1 : 0, 0, 1), 
				checkCardinality("flag", (Boolean) o.getFlag() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, MetaCarrier o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("MetaCarrier", ValidationResult.ValidationType.CARDINALITY, "MetaCarrier", path, "", res.getError());
				}
				return success("MetaCarrier", ValidationResult.ValidationType.CARDINALITY, "MetaCarrier", path, "");
			})
			.collect(toList());
	}

}
