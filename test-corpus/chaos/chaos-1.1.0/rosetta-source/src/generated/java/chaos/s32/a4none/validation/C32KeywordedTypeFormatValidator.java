package chaos.s32.a4none.validation;

import chaos.s32.a4none.C32Keyworded;
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

public class C32KeywordedTypeFormatValidator implements Validator<C32Keyworded> {

	private List<ComparisonResult> getComparisonResults(C32Keyworded o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C32Keyworded o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C32Keyworded", ValidationResult.ValidationType.TYPE_FORMAT, "C32Keyworded", path, "", res.getError());
				}
				return success("C32Keyworded", ValidationResult.ValidationType.TYPE_FORMAT, "C32Keyworded", path, "");
			})
			.collect(toList());
	}

}
