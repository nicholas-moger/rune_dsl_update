package chaos.s01.a2dangle.unused.validation;

import chaos.s01.a2dangle.unused.C1RefUnusedT;
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

public class C1RefUnusedTTypeFormatValidator implements Validator<C1RefUnusedT> {

	private List<ComparisonResult> getComparisonResults(C1RefUnusedT o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C1RefUnusedT o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C1RefUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C1RefUnusedT", path, "", res.getError());
				}
				return success("C1RefUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C1RefUnusedT", path, "");
			})
			.collect(toList());
	}

}
