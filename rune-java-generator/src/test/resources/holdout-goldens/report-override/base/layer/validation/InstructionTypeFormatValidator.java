package base.layer.validation;

import base.layer.Instruction;
import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.expression.ExpressionOperatorsNullSafe.checkNumber;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

public class InstructionTypeFormatValidator implements Validator<Instruction> {

	private List<ComparisonResult> getComparisonResults(Instruction o) {
		return Lists.<ComparisonResult>newArrayList(
				checkNumber("id", o.getId(), empty(), of(0), empty(), empty())
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Instruction o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Instruction", ValidationResult.ValidationType.TYPE_FORMAT, "Instruction", path, "", res.getError());
				}
				return success("Instruction", ValidationResult.ValidationType.TYPE_FORMAT, "Instruction", path, "");
			})
			.collect(toList());
	}

}
