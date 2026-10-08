package chaos.s09.a2dangle.unused.validation;

import chaos.s09.a2dangle.unused.C9PlainUnusedT;
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

public class C9PlainUnusedTTypeFormatValidator implements Validator<C9PlainUnusedT> {

	private List<ComparisonResult> getComparisonResults(C9PlainUnusedT o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C9PlainUnusedT o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C9PlainUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C9PlainUnusedT", path, "", res.getError());
				}
				return success("C9PlainUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C9PlainUnusedT", path, "");
			})
			.collect(toList());
	}

}
