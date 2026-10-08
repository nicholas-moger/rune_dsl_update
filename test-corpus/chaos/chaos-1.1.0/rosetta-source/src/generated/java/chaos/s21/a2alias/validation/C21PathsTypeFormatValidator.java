package chaos.s21.a2alias.validation;

import chaos.s21.a2alias.C21Paths;
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

public class C21PathsTypeFormatValidator implements Validator<C21Paths> {

	private List<ComparisonResult> getComparisonResults(C21Paths o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C21Paths o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C21Paths", ValidationResult.ValidationType.TYPE_FORMAT, "C21Paths", path, "", res.getError());
				}
				return success("C21Paths", ValidationResult.ValidationType.TYPE_FORMAT, "C21Paths", path, "");
			})
			.collect(toList());
	}

}
