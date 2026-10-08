package chaos.s25.a2qual.validation;

import chaos.s25.a2qual.C25Paths;
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

public class C25PathsTypeFormatValidator implements Validator<C25Paths> {

	private List<ComparisonResult> getComparisonResults(C25Paths o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C25Paths o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C25Paths", ValidationResult.ValidationType.TYPE_FORMAT, "C25Paths", path, "", res.getError());
				}
				return success("C25Paths", ValidationResult.ValidationType.TYPE_FORMAT, "C25Paths", path, "");
			})
			.collect(toList());
	}

}
