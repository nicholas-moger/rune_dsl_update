package chaos.s28.a2dangle.unused.validation;

import chaos.s28.a2dangle.unused.C28ExtraUnusedT;
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

public class C28ExtraUnusedTTypeFormatValidator implements Validator<C28ExtraUnusedT> {

	private List<ComparisonResult> getComparisonResults(C28ExtraUnusedT o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C28ExtraUnusedT o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C28ExtraUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C28ExtraUnusedT", path, "", res.getError());
				}
				return success("C28ExtraUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C28ExtraUnusedT", path, "");
			})
			.collect(toList());
	}

}
