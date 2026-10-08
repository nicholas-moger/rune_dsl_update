package chaos.s22.a2alias.validation;

import chaos.s22.a2alias.C22Terms;
import chaos.s22.a2alias.h.C22Aux;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.math.BigDecimal;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C22TermsValidator implements Validator<C22Terms> {

	private List<ComparisonResult> getComparisonResults(C22Terms o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("kind", (String) o.getKind() != null ? 1 : 0, 0, 1), 
				checkCardinality("notional", (BigDecimal) o.getNotional() != null ? 1 : 0, 0, 1), 
				checkCardinality("aux", (C22Aux) o.getAux() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C22Terms o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C22Terms", ValidationResult.ValidationType.CARDINALITY, "C22Terms", path, "", res.getError());
				}
				return success("C22Terms", ValidationResult.ValidationType.CARDINALITY, "C22Terms", path, "");
			})
			.collect(toList());
	}

}
