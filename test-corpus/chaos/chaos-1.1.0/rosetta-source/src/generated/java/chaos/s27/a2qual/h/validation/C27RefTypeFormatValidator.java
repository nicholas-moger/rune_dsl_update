package chaos.s27.a2qual.h.validation;

import chaos.s27.a2qual.h.C27Ref;
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

public class C27RefTypeFormatValidator implements Validator<C27Ref> {

	private List<ComparisonResult> getComparisonResults(C27Ref o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C27Ref o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C27Ref", ValidationResult.ValidationType.TYPE_FORMAT, "C27Ref", path, "", res.getError());
				}
				return success("C27Ref", ValidationResult.ValidationType.TYPE_FORMAT, "C27Ref", path, "");
			})
			.collect(toList());
	}

}
