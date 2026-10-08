package chaos.s24.a2dangle.unused.validation;

import chaos.s24.a2dangle.unused.C24RefUnusedT;
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

public class C24RefUnusedTTypeFormatValidator implements Validator<C24RefUnusedT> {

	private List<ComparisonResult> getComparisonResults(C24RefUnusedT o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C24RefUnusedT o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C24RefUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C24RefUnusedT", path, "", res.getError());
				}
				return success("C24RefUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C24RefUnusedT", path, "");
			})
			.collect(toList());
	}

}
