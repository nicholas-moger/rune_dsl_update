package chaos.s18.a3hub.p2.validation;

import chaos.s18.a3hub.p2.C18Either;
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

public class C18EitherTypeFormatValidator implements Validator<C18Either> {

	private List<ComparisonResult> getComparisonResults(C18Either o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C18Either o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C18Either", ValidationResult.ValidationType.TYPE_FORMAT, "C18Either", path, "", res.getError());
				}
				return success("C18Either", ValidationResult.ValidationType.TYPE_FORMAT, "C18Either", path, "");
			})
			.collect(toList());
	}

}
