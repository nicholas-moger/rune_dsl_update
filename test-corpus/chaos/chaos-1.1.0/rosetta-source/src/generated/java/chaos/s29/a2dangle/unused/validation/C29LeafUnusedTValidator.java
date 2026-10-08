package chaos.s29.a2dangle.unused.validation;

import chaos.s29.a2dangle.unused.C29LeafUnusedT;
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

public class C29LeafUnusedTValidator implements Validator<C29LeafUnusedT> {

	private List<ComparisonResult> getComparisonResults(C29LeafUnusedT o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("stub", (String) o.getStub() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C29LeafUnusedT o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C29LeafUnusedT", ValidationResult.ValidationType.CARDINALITY, "C29LeafUnusedT", path, "", res.getError());
				}
				return success("C29LeafUnusedT", ValidationResult.ValidationType.CARDINALITY, "C29LeafUnusedT", path, "");
			})
			.collect(toList());
	}

}
