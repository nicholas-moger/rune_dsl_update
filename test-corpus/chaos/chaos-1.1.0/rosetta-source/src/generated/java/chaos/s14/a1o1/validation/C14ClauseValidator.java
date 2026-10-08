package chaos.s14.a1o1.validation;

import chaos.s14.a1o1.C14Aux;
import chaos.s14.a1o1.C14Clause;
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

public class C14ClauseValidator implements Validator<C14Clause> {

	private List<ComparisonResult> getComparisonResults(C14Clause o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("cid", (String) o.getCid() != null ? 1 : 0, 1, 1), 
				checkCardinality("weight", (BigDecimal) o.getWeight() != null ? 1 : 0, 0, 1), 
				checkCardinality("aux", (C14Aux) o.getAux() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C14Clause o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C14Clause", ValidationResult.ValidationType.CARDINALITY, "C14Clause", path, "", res.getError());
				}
				return success("C14Clause", ValidationResult.ValidationType.CARDINALITY, "C14Clause", path, "");
			})
			.collect(toList());
	}

}
