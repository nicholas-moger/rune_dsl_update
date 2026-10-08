package chaos.s01.a1o1.validation;

import chaos.s01.a1o1.C1Times;
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

public class C1TimesTypeFormatValidator implements Validator<C1Times> {

	private List<ComparisonResult> getComparisonResults(C1Times o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C1Times o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C1Times", ValidationResult.ValidationType.TYPE_FORMAT, "C1Times", path, "", res.getError());
				}
				return success("C1Times", ValidationResult.ValidationType.TYPE_FORMAT, "C1Times", path, "");
			})
			.collect(toList());
	}

}
