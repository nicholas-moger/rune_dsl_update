package chaos.s18.a5mixed.validation;

import chaos.s18.a5mixed.C18OptB;
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

public class C18OptBTypeFormatValidator implements Validator<C18OptB> {

	private List<ComparisonResult> getComparisonResults(C18OptB o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C18OptB o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C18OptB", ValidationResult.ValidationType.TYPE_FORMAT, "C18OptB", path, "", res.getError());
				}
				return success("C18OptB", ValidationResult.ValidationType.TYPE_FORMAT, "C18OptB", path, "");
			})
			.collect(toList());
	}

}
