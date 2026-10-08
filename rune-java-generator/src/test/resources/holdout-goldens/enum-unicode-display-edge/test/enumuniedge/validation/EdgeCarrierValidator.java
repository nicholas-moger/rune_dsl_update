package test.enumuniedge.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.enumuniedge.EdgeCarrier;
import test.enumuniedge.EscapeEnum;
import test.enumuniedge.SynonymEnum;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class EdgeCarrierValidator implements Validator<EdgeCarrier> {

	private List<ComparisonResult> getComparisonResults(EdgeCarrier o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("esc", (EscapeEnum) o.getEsc() != null ? 1 : 0, 0, 1), 
				checkCardinality("syn", (SynonymEnum) o.getSyn() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, EdgeCarrier o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("EdgeCarrier", ValidationResult.ValidationType.CARDINALITY, "EdgeCarrier", path, "", res.getError());
				}
				return success("EdgeCarrier", ValidationResult.ValidationType.CARDINALITY, "EdgeCarrier", path, "");
			})
			.collect(toList());
	}

}
