package chaos.s99.base.validation;

import chaos.s99.base.C99Holder;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C99HolderValidator implements Validator<C99Holder> {

	private List<ComparisonResult> getComparisonResults(C99Holder o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("n", (Integer) o.getN() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C99Holder o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C99Holder", ValidationResult.ValidationType.CARDINALITY, "C99Holder", path, "", res.getError());
				}
				return success("C99Holder", ValidationResult.ValidationType.CARDINALITY, "C99Holder", path, "");
			})
			.collect(toList());
	}

}
