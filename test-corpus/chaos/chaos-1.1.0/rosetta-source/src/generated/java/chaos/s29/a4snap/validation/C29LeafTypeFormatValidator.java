package chaos.s29.a4snap.validation;

import chaos.s29.a4snap.C29Leaf;
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

public class C29LeafTypeFormatValidator implements Validator<C29Leaf> {

	private List<ComparisonResult> getComparisonResults(C29Leaf o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C29Leaf o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C29Leaf", ValidationResult.ValidationType.TYPE_FORMAT, "C29Leaf", path, "", res.getError());
				}
				return success("C29Leaf", ValidationResult.ValidationType.TYPE_FORMAT, "C29Leaf", path, "");
			})
			.collect(toList());
	}

}
