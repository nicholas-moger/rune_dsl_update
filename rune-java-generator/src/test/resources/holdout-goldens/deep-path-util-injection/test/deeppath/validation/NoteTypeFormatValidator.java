package test.deeppath.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import java.util.List;
import test.deeppath.Note;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class NoteTypeFormatValidator implements Validator<Note> {

	private List<ComparisonResult> getComparisonResults(Note o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Note o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Note", ValidationResult.ValidationType.TYPE_FORMAT, "Note", path, "", res.getError());
				}
				return success("Note", ValidationResult.ValidationType.TYPE_FORMAT, "Note", path, "");
			})
			.collect(toList());
	}

}
