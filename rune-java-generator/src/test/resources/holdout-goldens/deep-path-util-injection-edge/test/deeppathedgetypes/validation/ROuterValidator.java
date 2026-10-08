package test.deeppathedgetypes.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.deeppathedgetypes.RNote;
import test.deeppathedgetypes.ROuter;
import test.deeppathedgetypes.RWrap;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkCardinality;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class ROuterValidator implements Validator<ROuter> {

	private List<ComparisonResult> getComparisonResults(ROuter o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("RWrap", (RWrap) o.getRWrap() != null ? 1 : 0, 0, 1), 
				checkCardinality("RNote", (RNote) o.getRNote() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, ROuter o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("ROuter", ValidationResult.ValidationType.CARDINALITY, "ROuter", path, "", res.getError());
				}
				return success("ROuter", ValidationResult.ValidationType.CARDINALITY, "ROuter", path, "");
			})
			.collect(toList());
	}

}
