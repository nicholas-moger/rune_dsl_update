package chaos.s29.a1o2.validation;

import chaos.s29.a1o2.C29Note;
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

public class C29NoteTypeFormatValidator implements Validator<C29Note> {

	private List<ComparisonResult> getComparisonResults(C29Note o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C29Note o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C29Note", ValidationResult.ValidationType.TYPE_FORMAT, "C29Note", path, "", res.getError());
				}
				return success("C29Note", ValidationResult.ValidationType.TYPE_FORMAT, "C29Note", path, "");
			})
			.collect(toList());
	}

}
