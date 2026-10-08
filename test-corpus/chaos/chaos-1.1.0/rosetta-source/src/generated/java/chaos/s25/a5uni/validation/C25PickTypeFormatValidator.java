package chaos.s25.a5uni.validation;

import chaos.s25.a5uni.C25Pick;
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

public class C25PickTypeFormatValidator implements Validator<C25Pick> {

	private List<ComparisonResult> getComparisonResults(C25Pick o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C25Pick o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C25Pick", ValidationResult.ValidationType.TYPE_FORMAT, "C25Pick", path, "", res.getError());
				}
				return success("C25Pick", ValidationResult.ValidationType.TYPE_FORMAT, "C25Pick", path, "");
			})
			.collect(toList());
	}

}
