package chaos.s06.a2wild.validation;

import chaos.s06.a2wild.C6Event;
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

public class C6EventTypeFormatValidator implements Validator<C6Event> {

	private List<ComparisonResult> getComparisonResults(C6Event o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C6Event o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C6Event", ValidationResult.ValidationType.TYPE_FORMAT, "C6Event", path, "", res.getError());
				}
				return success("C6Event", ValidationResult.ValidationType.TYPE_FORMAT, "C6Event", path, "");
			})
			.collect(toList());
	}

}
