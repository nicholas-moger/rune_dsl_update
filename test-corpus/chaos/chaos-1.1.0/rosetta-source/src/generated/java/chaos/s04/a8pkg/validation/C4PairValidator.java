package chaos.s04.a8pkg.validation;

import chaos.s04.a8pkg.C4Pair;
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

public class C4PairValidator implements Validator<C4Pair> {

	private List<ComparisonResult> getComparisonResults(C4Pair o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("left", (BigDecimal) o.getLeft() != null ? 1 : 0, 1, 1), 
				checkCardinality("right", (BigDecimal) o.getRight() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C4Pair o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C4Pair", ValidationResult.ValidationType.CARDINALITY, "C4Pair", path, "", res.getError());
				}
				return success("C4Pair", ValidationResult.ValidationType.CARDINALITY, "C4Pair", path, "");
			})
			.collect(toList());
	}

}
