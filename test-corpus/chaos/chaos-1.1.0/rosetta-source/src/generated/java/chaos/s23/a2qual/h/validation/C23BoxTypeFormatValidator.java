package chaos.s23.a2qual.h.validation;

import chaos.s23.a2qual.h.C23Box;
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

public class C23BoxTypeFormatValidator implements Validator<C23Box> {

	private List<ComparisonResult> getComparisonResults(C23Box o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C23Box o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C23Box", ValidationResult.ValidationType.TYPE_FORMAT, "C23Box", path, "", res.getError());
				}
				return success("C23Box", ValidationResult.ValidationType.TYPE_FORMAT, "C23Box", path, "");
			})
			.collect(toList());
	}

}
