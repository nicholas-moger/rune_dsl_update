package chaos.s23.a2dangle.unused.validation;

import chaos.s23.a2dangle.unused.C23BoxUnusedT;
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

public class C23BoxUnusedTValidator implements Validator<C23BoxUnusedT> {

	private List<ComparisonResult> getComparisonResults(C23BoxUnusedT o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("stub", (String) o.getStub() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C23BoxUnusedT o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C23BoxUnusedT", ValidationResult.ValidationType.CARDINALITY, "C23BoxUnusedT", path, "", res.getError());
				}
				return success("C23BoxUnusedT", ValidationResult.ValidationType.CARDINALITY, "C23BoxUnusedT", path, "");
			})
			.collect(toList());
	}

}
