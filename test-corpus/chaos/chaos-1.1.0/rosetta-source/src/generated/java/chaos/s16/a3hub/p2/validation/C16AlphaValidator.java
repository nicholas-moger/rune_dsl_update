package chaos.s16.a3hub.p2.validation;

import chaos.s16.a3hub.p2.C16Alpha;
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

public class C16AlphaValidator implements Validator<C16Alpha> {

	private List<ComparisonResult> getComparisonResults(C16Alpha o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("a", (String) o.getA() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C16Alpha o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C16Alpha", ValidationResult.ValidationType.CARDINALITY, "C16Alpha", path, "", res.getError());
				}
				return success("C16Alpha", ValidationResult.ValidationType.CARDINALITY, "C16Alpha", path, "");
			})
			.collect(toList());
	}

}
