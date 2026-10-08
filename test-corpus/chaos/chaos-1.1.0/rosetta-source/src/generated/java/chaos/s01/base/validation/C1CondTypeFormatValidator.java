package chaos.s01.base.validation;

import chaos.s01.base.C1Cond;
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

public class C1CondTypeFormatValidator implements Validator<C1Cond> {

	private List<ComparisonResult> getComparisonResults(C1Cond o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C1Cond o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C1Cond", ValidationResult.ValidationType.TYPE_FORMAT, "C1Cond", path, "", res.getError());
				}
				return success("C1Cond", ValidationResult.ValidationType.TYPE_FORMAT, "C1Cond", path, "");
			})
			.collect(toList());
	}

}
