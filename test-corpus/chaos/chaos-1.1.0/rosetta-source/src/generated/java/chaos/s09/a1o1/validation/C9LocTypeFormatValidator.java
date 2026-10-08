package chaos.s09.a1o1.validation;

import chaos.s09.a1o1.C9Loc;
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

public class C9LocTypeFormatValidator implements Validator<C9Loc> {

	private List<ComparisonResult> getComparisonResults(C9Loc o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C9Loc o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C9Loc", ValidationResult.ValidationType.TYPE_FORMAT, "C9Loc", path, "", res.getError());
				}
				return success("C9Loc", ValidationResult.ValidationType.TYPE_FORMAT, "C9Loc", path, "");
			})
			.collect(toList());
	}

}
