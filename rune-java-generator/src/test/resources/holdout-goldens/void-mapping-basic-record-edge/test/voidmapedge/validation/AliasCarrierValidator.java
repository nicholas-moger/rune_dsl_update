package test.voidmapedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.voidmapedge.AliasCarrier;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class AliasCarrierValidator implements Validator<AliasCarrier> {

	private List<ComparisonResult> getComparisonResults(AliasCarrier o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("tok", (Void) o.getTok() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, AliasCarrier o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("AliasCarrier", ValidationResult.ValidationType.CARDINALITY, "AliasCarrier", path, "", res.getError());
				}
				return success("AliasCarrier", ValidationResult.ValidationType.CARDINALITY, "AliasCarrier", path, "");
			})
			.collect(toList());
	}

}
