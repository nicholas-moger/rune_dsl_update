package chaos.s13.a2wild.validation;

import chaos.s13.a2wild.C13Fact;
import chaos.s13.a2wild.h.C13Aux;
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

public class C13FactValidator implements Validator<C13Fact> {

	private List<ComparisonResult> getComparisonResults(C13Fact o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("fid", (String) o.getFid() != null ? 1 : 0, 1, 1), 
				checkCardinality("amt", (BigDecimal) o.getAmt() != null ? 1 : 0, 0, 1), 
				checkCardinality("aux", (C13Aux) o.getAux() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C13Fact o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C13Fact", ValidationResult.ValidationType.CARDINALITY, "C13Fact", path, "", res.getError());
				}
				return success("C13Fact", ValidationResult.ValidationType.CARDINALITY, "C13Fact", path, "");
			})
			.collect(toList());
	}

}
