package chaos.s28.a2alias.validation;

import chaos.s28.a2alias.C28OptB;
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

public class C28OptBTypeFormatValidator implements Validator<C28OptB> {

	private List<ComparisonResult> getComparisonResults(C28OptB o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C28OptB o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C28OptB", ValidationResult.ValidationType.TYPE_FORMAT, "C28OptB", path, "", res.getError());
				}
				return success("C28OptB", ValidationResult.ValidationType.TYPE_FORMAT, "C28OptB", path, "");
			})
			.collect(toList());
	}

}
