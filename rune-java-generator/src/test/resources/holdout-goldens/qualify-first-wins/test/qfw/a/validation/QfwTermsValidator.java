package test.qfw.a.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.List;
import test.qfw.a.QfwTerms;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class QfwTermsValidator implements Validator<QfwTerms> {

	private List<ComparisonResult> getComparisonResults(QfwTerms o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("kind", (String) o.getKind() != null ? 1 : 0, 0, 1), 
				checkCardinality("notional", (BigDecimal) o.getNotional() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, QfwTerms o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("QfwTerms", ValidationResult.ValidationType.CARDINALITY, "QfwTerms", path, "", res.getError());
				}
				return success("QfwTerms", ValidationResult.ValidationType.CARDINALITY, "QfwTerms", path, "");
			})
			.collect(toList());
	}

}
