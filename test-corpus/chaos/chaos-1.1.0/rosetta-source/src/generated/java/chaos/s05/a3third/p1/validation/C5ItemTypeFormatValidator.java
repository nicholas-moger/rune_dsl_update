package chaos.s05.a3third.p1.validation;

import chaos.s05.a3third.p1.C5Item;
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

public class C5ItemTypeFormatValidator implements Validator<C5Item> {

	private List<ComparisonResult> getComparisonResults(C5Item o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C5Item o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C5Item", ValidationResult.ValidationType.TYPE_FORMAT, "C5Item", path, "", res.getError());
				}
				return success("C5Item", ValidationResult.ValidationType.TYPE_FORMAT, "C5Item", path, "");
			})
			.collect(toList());
	}

}
