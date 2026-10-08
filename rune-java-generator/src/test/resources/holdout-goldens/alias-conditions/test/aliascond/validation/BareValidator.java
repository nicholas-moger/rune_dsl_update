package test.aliascond.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.math.BigDecimal;
import java.util.List;
import test.aliascond.Bare;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class BareValidator implements Validator<Bare> {

	private List<ComparisonResult> getComparisonResults(Bare o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("qty", (BigDecimal) o.getQty() != null ? 1 : 0, 0, 1), 
				checkCardinality("text", (FieldWithMetaString) o.getText() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Bare o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Bare", ValidationResult.ValidationType.CARDINALITY, "Bare", path, "", res.getError());
				}
				return success("Bare", ValidationResult.ValidationType.CARDINALITY, "Bare", path, "");
			})
			.collect(toList());
	}

}
