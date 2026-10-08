package chaos.s16.a2qual.validation;

import chaos.s16.a2qual.C16Slot;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class C16SlotTypeFormatValidator implements Validator<C16Slot> {

	private List<ComparisonResult> getComparisonResults(C16Slot o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C16Slot o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C16Slot", ValidationResult.ValidationType.TYPE_FORMAT, "C16Slot", path, "", res.getError());
				}
				return success("C16Slot", ValidationResult.ValidationType.TYPE_FORMAT, "C16Slot", path, "");
			})
			.collect(toList());
	}

}
