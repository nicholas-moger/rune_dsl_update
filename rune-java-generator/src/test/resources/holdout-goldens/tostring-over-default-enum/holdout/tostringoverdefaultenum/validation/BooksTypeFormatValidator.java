package holdout.tostringoverdefaultenum.validation;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.expression.ComparisonResult;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.ValidationResult;
import com.rosetta.model.lib.validation.Validator;
import holdout.tostringoverdefaultenum.Books;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.rosetta.model.lib.validation.ValidationResult.failure;
import static com.rosetta.model.lib.validation.ValidationResult.success;
import static java.util.stream.Collectors.toList;

public class BooksTypeFormatValidator implements Validator<Books> {

	private List<ComparisonResult> getComparisonResults(Books o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, Books o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("Books", ValidationResult.ValidationType.TYPE_FORMAT, "Books", path, "", res.getError());
				}
				return success("Books", ValidationResult.ValidationType.TYPE_FORMAT, "Books", path, "");
			})
			.collect(toList());
	}

}
