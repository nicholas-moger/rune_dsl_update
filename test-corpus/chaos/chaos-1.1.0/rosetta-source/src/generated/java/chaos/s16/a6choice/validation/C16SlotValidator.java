package chaos.s16.a6choice.validation;

import chaos.s16.a6choice.C16Alpha;
import chaos.s16.a6choice.C16Held;
import chaos.s16.a6choice.C16Slot;
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

public class C16SlotValidator implements Validator<C16Slot> {

	private List<ComparisonResult> getComparisonResults(C16Slot o) {
		return Lists.<ComparisonResult>newArrayList(
				checkCardinality("firstPick", (C16Alpha) o.getFirstPick() != null ? 1 : 0, 0, 1), 
				checkCardinality("held", (C16Held) o.getHeld() != null ? 1 : 0, 0, 1)
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C16Slot o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C16Slot", ValidationResult.ValidationType.CARDINALITY, "C16Slot", path, "", res.getError());
				}
				return success("C16Slot", ValidationResult.ValidationType.CARDINALITY, "C16Slot", path, "");
			})
			.collect(toList());
	}

}
