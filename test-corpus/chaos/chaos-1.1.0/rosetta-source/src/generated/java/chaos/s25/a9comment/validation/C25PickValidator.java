package chaos.s25.a9comment.validation;

import chaos.s25.a9comment.C25OptA;
import chaos.s25.a9comment.C25OptB;
import chaos.s25.a9comment.C25Pick;
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

public class C25PickValidator implements Validator<C25Pick> {

	private List<ComparisonResult> getComparisonResults(C25Pick o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("C25OptA", (C25OptA) o.getC25OptA() != null ? 1 : 0, 0, 1), 
				checkCardinality("C25OptB", (C25OptB) o.getC25OptB() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C25Pick o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C25Pick", ValidationResult.ValidationType.CARDINALITY, "C25Pick", path, "", res.getError());
				}
				return success("C25Pick", ValidationResult.ValidationType.CARDINALITY, "C25Pick", path, "");
			})
			.collect(toList());
	}

}
