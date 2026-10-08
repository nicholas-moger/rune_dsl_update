package chaos.s02.a2alias.h.validation;

import chaos.s02.a2alias.h.C2Tag;
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

public class C2TagTypeFormatValidator implements Validator<C2Tag> {

	private List<ComparisonResult> getComparisonResults(C2Tag o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C2Tag o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C2Tag", ValidationResult.ValidationType.TYPE_FORMAT, "C2Tag", path, "", res.getError());
				}
				return success("C2Tag", ValidationResult.ValidationType.TYPE_FORMAT, "C2Tag", path, "");
			})
			.collect(toList());
	}

}
