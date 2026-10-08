package chaos.s20.a1o2.validation;

import chaos.s20.a1o2.C20Trunk;
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

public class C20TrunkValidator implements Validator<C20Trunk> {

	private List<ComparisonResult> getComparisonResults(C20Trunk o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("title", (String) o.getTitle() != null ? 1 : 0, 1, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C20Trunk o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C20Trunk", ValidationResult.ValidationType.CARDINALITY, "C20Trunk", path, "", res.getError());
				}
				return success("C20Trunk", ValidationResult.ValidationType.CARDINALITY, "C20Trunk", path, "");
			})
			.collect(toList());
	}

}
