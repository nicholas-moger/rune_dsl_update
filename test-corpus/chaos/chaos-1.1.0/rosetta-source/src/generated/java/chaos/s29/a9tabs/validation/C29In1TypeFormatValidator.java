package chaos.s29.a9tabs.validation;

import chaos.s29.a9tabs.C29In1;
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

public class C29In1TypeFormatValidator implements Validator<C29In1> {

	private List<ComparisonResult> getComparisonResults(C29In1 o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C29In1 o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C29In1", ValidationResult.ValidationType.TYPE_FORMAT, "C29In1", path, "", res.getError());
				}
				return success("C29In1", ValidationResult.ValidationType.TYPE_FORMAT, "C29In1", path, "");
			})
			.collect(toList());
	}

}
