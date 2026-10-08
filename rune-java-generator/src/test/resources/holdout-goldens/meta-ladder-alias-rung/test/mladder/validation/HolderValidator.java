package test.mladder.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.List;
import test.mladder.Holder;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class HolderValidator implements Validator<Holder> {

	private List<ComparisonResult> getComparisonResults(Holder o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("coded", (FieldWithMetaString) o.getCoded() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Holder o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Holder", ValidationResult.ValidationType.CARDINALITY, "Holder", path, "", res.getError());
				}
				return success("Holder", ValidationResult.ValidationType.CARDINALITY, "Holder", path, "");
			})
			.collect(toList());
	}

}
