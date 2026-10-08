package chaos.s27.a2dangle.unused.validation;

import chaos.s27.a2dangle.unused.C27RefUnusedT;
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

public class C27RefUnusedTValidator implements Validator<C27RefUnusedT> {

	private List<ComparisonResult> getComparisonResults(C27RefUnusedT o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("stub", (String) o.getStub() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C27RefUnusedT o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C27RefUnusedT", ValidationResult.ValidationType.CARDINALITY, "C27RefUnusedT", path, "", res.getError());
				}
				return success("C27RefUnusedT", ValidationResult.ValidationType.CARDINALITY, "C27RefUnusedT", path, "");
			})
			.collect(toList());
	}

}
