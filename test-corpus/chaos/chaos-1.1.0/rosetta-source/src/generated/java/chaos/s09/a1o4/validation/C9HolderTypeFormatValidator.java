package chaos.s09.a1o4.validation;

import chaos.s09.a1o4.C9Holder;
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

public class C9HolderTypeFormatValidator implements Validator<C9Holder> {

	private List<ComparisonResult> getComparisonResults(C9Holder o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C9Holder o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C9Holder", ValidationResult.ValidationType.TYPE_FORMAT, "C9Holder", path, "", res.getError());
				}
				return success("C9Holder", ValidationResult.ValidationType.TYPE_FORMAT, "C9Holder", path, "");
			})
			.collect(toList());
	}

}
