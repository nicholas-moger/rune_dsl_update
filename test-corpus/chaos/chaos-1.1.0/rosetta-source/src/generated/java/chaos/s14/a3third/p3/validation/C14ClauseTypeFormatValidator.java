package chaos.s14.a3third.p3.validation;

import chaos.s14.a3third.p3.C14Clause;
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

public class C14ClauseTypeFormatValidator implements Validator<C14Clause> {

	private List<ComparisonResult> getComparisonResults(C14Clause o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C14Clause o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C14Clause", ValidationResult.ValidationType.TYPE_FORMAT, "C14Clause", path, "", res.getError());
				}
				return success("C14Clause", ValidationResult.ValidationType.TYPE_FORMAT, "C14Clause", path, "");
			})
			.collect(toList());
	}

}
