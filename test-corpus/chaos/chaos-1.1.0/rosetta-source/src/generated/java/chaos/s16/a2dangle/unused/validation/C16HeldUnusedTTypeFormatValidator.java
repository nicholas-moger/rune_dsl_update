package chaos.s16.a2dangle.unused.validation;

import chaos.s16.a2dangle.unused.C16HeldUnusedT;
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

public class C16HeldUnusedTTypeFormatValidator implements Validator<C16HeldUnusedT> {

	private List<ComparisonResult> getComparisonResults(C16HeldUnusedT o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C16HeldUnusedT o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C16HeldUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C16HeldUnusedT", path, "", res.getError());
				}
				return success("C16HeldUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C16HeldUnusedT", path, "");
			})
			.collect(toList());
	}

}
