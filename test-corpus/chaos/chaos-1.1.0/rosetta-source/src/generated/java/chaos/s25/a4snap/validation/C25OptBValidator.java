package chaos.s25.a4snap.validation;

import chaos.s25.a4snap.C25OptB;
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

public class C25OptBValidator implements Validator<C25OptB> {

	private List<ComparisonResult> getComparisonResults(C25OptB o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("pb", (String) o.getPb() != null ? 1 : 0, 0, 1), 
				checkCardinality("shared", (String) o.getShared() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C25OptB o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C25OptB", ValidationResult.ValidationType.CARDINALITY, "C25OptB", path, "", res.getError());
				}
				return success("C25OptB", ValidationResult.ValidationType.CARDINALITY, "C25OptB", path, "");
			})
			.collect(toList());
	}

}
