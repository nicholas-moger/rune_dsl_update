package chaos.s13.a1o3.validation;

import chaos.s13.a1o3.C13Fact;
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

public class C13FactTypeFormatValidator implements Validator<C13Fact> {

	private List<ComparisonResult> getComparisonResults(C13Fact o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C13Fact o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C13Fact", ValidationResult.ValidationType.TYPE_FORMAT, "C13Fact", path, "", res.getError());
				}
				return success("C13Fact", ValidationResult.ValidationType.TYPE_FORMAT, "C13Fact", path, "");
			})
			.collect(toList());
	}

}
