package chaos.s17.a1o3.validation;

import chaos.s17.a1o3.C17Books;
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

public class C17BooksTypeFormatValidator implements Validator<C17Books> {

	private List<ComparisonResult> getComparisonResults(C17Books o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C17Books o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C17Books", ValidationResult.ValidationType.TYPE_FORMAT, "C17Books", path, "", res.getError());
				}
				return success("C17Books", ValidationResult.ValidationType.TYPE_FORMAT, "C17Books", path, "");
			})
			.collect(toList());
	}

}
