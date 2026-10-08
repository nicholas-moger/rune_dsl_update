package chaos.s25.a2dangle.unused.validation;

import chaos.s25.a2dangle.unused.C25SubUnusedT;
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

public class C25SubUnusedTValidator implements Validator<C25SubUnusedT> {

	private List<ComparisonResult> getComparisonResults(C25SubUnusedT o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("stub", (String) o.getStub() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C25SubUnusedT o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C25SubUnusedT", ValidationResult.ValidationType.CARDINALITY, "C25SubUnusedT", path, "", res.getError());
				}
				return success("C25SubUnusedT", ValidationResult.ValidationType.CARDINALITY, "C25SubUnusedT", path, "");
			})
			.collect(toList());
	}

}
