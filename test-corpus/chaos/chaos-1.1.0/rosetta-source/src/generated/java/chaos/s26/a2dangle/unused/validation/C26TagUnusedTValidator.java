package chaos.s26.a2dangle.unused.validation;

import chaos.s26.a2dangle.unused.C26TagUnusedT;
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

public class C26TagUnusedTValidator implements Validator<C26TagUnusedT> {

	private List<ComparisonResult> getComparisonResults(C26TagUnusedT o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("stub", (String) o.getStub() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C26TagUnusedT o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C26TagUnusedT", ValidationResult.ValidationType.CARDINALITY, "C26TagUnusedT", path, "", res.getError());
				}
				return success("C26TagUnusedT", ValidationResult.ValidationType.CARDINALITY, "C26TagUnusedT", path, "");
			})
			.collect(toList());
	}

}
