package chaos.s03.a3hub.p2.validation;

import chaos.s03.a3hub.p2.C3Inner;
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

public class C3InnerTypeFormatValidator implements Validator<C3Inner> {

	private List<ComparisonResult> getComparisonResults(C3Inner o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C3Inner o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C3Inner", ValidationResult.ValidationType.TYPE_FORMAT, "C3Inner", path, "", res.getError());
				}
				return success("C3Inner", ValidationResult.ValidationType.TYPE_FORMAT, "C3Inner", path, "");
			})
			.collect(toList());
	}

}
