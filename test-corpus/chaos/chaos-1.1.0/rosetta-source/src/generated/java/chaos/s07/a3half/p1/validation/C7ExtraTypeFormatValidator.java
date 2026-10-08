package chaos.s07.a3half.p1.validation;

import chaos.s07.a3half.p1.C7Extra;
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

public class C7ExtraTypeFormatValidator implements Validator<C7Extra> {

	private List<ComparisonResult> getComparisonResults(C7Extra o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C7Extra o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C7Extra", ValidationResult.ValidationType.TYPE_FORMAT, "C7Extra", path, "", res.getError());
				}
				return success("C7Extra", ValidationResult.ValidationType.TYPE_FORMAT, "C7Extra", path, "");
			})
			.collect(toList());
	}

}
