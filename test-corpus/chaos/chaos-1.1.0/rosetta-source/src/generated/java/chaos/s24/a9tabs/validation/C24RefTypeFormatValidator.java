package chaos.s24.a9tabs.validation;

import chaos.s24.a9tabs.C24Ref;
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

public class C24RefTypeFormatValidator implements Validator<C24Ref> {

	private List<ComparisonResult> getComparisonResults(C24Ref o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C24Ref o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C24Ref", ValidationResult.ValidationType.TYPE_FORMAT, "C24Ref", path, "", res.getError());
				}
				return success("C24Ref", ValidationResult.ValidationType.TYPE_FORMAT, "C24Ref", path, "");
			})
			.collect(toList());
	}

}
