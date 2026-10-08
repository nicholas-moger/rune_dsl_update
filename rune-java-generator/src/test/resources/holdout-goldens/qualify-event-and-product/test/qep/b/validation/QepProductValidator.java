package test.qep.b.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.qep.b.QepProduct;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class QepProductValidator implements Validator<QepProduct> {

	private List<ComparisonResult> getComparisonResults(QepProduct o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("kind", (String) o.getKind() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, QepProduct o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("QepProduct", ValidationResult.ValidationType.CARDINALITY, "QepProduct", path, "", res.getError());
				}
				return success("QepProduct", ValidationResult.ValidationType.CARDINALITY, "QepProduct", path, "");
			})
			.collect(toList());
	}

}
