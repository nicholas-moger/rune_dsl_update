package chaos.s03.base.validation;

import chaos.s03.base.C3Note;
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

public class C3NoteTypeFormatValidator implements Validator<C3Note> {

	private List<ComparisonResult> getComparisonResults(C3Note o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C3Note o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C3Note", ValidationResult.ValidationType.TYPE_FORMAT, "C3Note", path, "", res.getError());
				}
				return success("C3Note", ValidationResult.ValidationType.TYPE_FORMAT, "C3Note", path, "");
			})
			.collect(toList());
	}

}
