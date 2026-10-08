package chaos.s11.a1o1.validation;

import chaos.s11.a1o1.C11Mapped;
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

public class C11MappedTypeFormatValidator implements Validator<C11Mapped> {

	private List<ComparisonResult> getComparisonResults(C11Mapped o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C11Mapped o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C11Mapped", ValidationResult.ValidationType.TYPE_FORMAT, "C11Mapped", path, "", res.getError());
				}
				return success("C11Mapped", ValidationResult.ValidationType.TYPE_FORMAT, "C11Mapped", path, "");
			})
			.collect(toList());
	}

}
