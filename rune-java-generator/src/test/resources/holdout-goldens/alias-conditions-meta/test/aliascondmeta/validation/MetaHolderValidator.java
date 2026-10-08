package test.aliascondmeta.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaInteger;
import java.util.List;
import test.aliascondmeta.MetaHolder;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class MetaHolderValidator implements Validator<MetaHolder> {

	private List<ComparisonResult> getComparisonResults(MetaHolder o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("schemed", (FieldWithMetaInteger) o.getSchemed() != null ? 1 : 0, 0, 1), 
				checkCardinality("plain", (Integer) o.getPlain() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, MetaHolder o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("MetaHolder", ValidationResult.ValidationType.CARDINALITY, "MetaHolder", path, "", res.getError());
				}
				return success("MetaHolder", ValidationResult.ValidationType.CARDINALITY, "MetaHolder", path, "");
			})
			.collect(toList());
	}

}
