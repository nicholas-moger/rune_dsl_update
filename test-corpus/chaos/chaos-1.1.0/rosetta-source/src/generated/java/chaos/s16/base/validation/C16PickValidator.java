package chaos.s16.base.validation;

import chaos.s16.base.C16Alpha;
import chaos.s16.base.C16Beta;
import chaos.s16.base.C16Pick;
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

public class C16PickValidator implements Validator<C16Pick> {

	private List<ComparisonResult> getComparisonResults(C16Pick o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("C16Alpha", (C16Alpha) o.getC16Alpha() != null ? 1 : 0, 0, 1), 
				checkCardinality("C16Beta", (C16Beta) o.getC16Beta() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C16Pick o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C16Pick", ValidationResult.ValidationType.CARDINALITY, "C16Pick", path, "", res.getError());
				}
				return success("C16Pick", ValidationResult.ValidationType.CARDINALITY, "C16Pick", path, "");
			})
			.collect(toList());
	}

}
