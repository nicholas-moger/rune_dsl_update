package chaos.s03.a1o1.validation;

import chaos.s03.a1o1.C3Deep;
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

public class C3DeepTypeFormatValidator implements Validator<C3Deep> {

	private List<ComparisonResult> getComparisonResults(C3Deep o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C3Deep o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C3Deep", ValidationResult.ValidationType.TYPE_FORMAT, "C3Deep", path, "", res.getError());
				}
				return success("C3Deep", ValidationResult.ValidationType.TYPE_FORMAT, "C3Deep", path, "");
			})
			.collect(toList());
	}

}
