package chaos.s07.a2dangle.unused.validation;

import chaos.s07.a2dangle.unused.C7ExtraUnusedT;
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

public class C7ExtraUnusedTTypeFormatValidator implements Validator<C7ExtraUnusedT> {

	private List<ComparisonResult> getComparisonResults(C7ExtraUnusedT o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C7ExtraUnusedT o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C7ExtraUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C7ExtraUnusedT", path, "", res.getError());
				}
				return success("C7ExtraUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C7ExtraUnusedT", path, "");
			})
			.collect(toList());
	}

}
