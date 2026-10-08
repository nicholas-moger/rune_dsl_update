package chaos.s28.a2alias.validation;

import chaos.s28.a2alias.C28OptA;
import chaos.s28.a2alias.C28OptB;
import chaos.s28.a2alias.C28Which;
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

public class C28WhichValidator implements Validator<C28Which> {

	private List<ComparisonResult> getComparisonResults(C28Which o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("C28OptA", (C28OptA) o.getC28OptA() != null ? 1 : 0, 0, 1), 
				checkCardinality("C28OptB", (C28OptB) o.getC28OptB() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C28Which o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C28Which", ValidationResult.ValidationType.CARDINALITY, "C28Which", path, "", res.getError());
				}
				return success("C28Which", ValidationResult.ValidationType.CARDINALITY, "C28Which", path, "");
			})
			.collect(toList());
	}

}
