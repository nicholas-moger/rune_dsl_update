package chaos.s01.a4none.validation;

import chaos.s01.a4none.C1Ref;
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

public class C1RefTypeFormatValidator implements Validator<C1Ref> {

	private List<ComparisonResult> getComparisonResults(C1Ref o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C1Ref o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C1Ref", ValidationResult.ValidationType.TYPE_FORMAT, "C1Ref", path, "", res.getError());
				}
				return success("C1Ref", ValidationResult.ValidationType.TYPE_FORMAT, "C1Ref", path, "");
			})
			.collect(toList());
	}

}
