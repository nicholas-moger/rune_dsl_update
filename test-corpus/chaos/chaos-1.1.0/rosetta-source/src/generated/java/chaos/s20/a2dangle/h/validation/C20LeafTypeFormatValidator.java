package chaos.s20.a2dangle.h.validation;

import chaos.s20.a2dangle.h.C20Leaf;
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

public class C20LeafTypeFormatValidator implements Validator<C20Leaf> {

	private List<ComparisonResult> getComparisonResults(C20Leaf o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C20Leaf o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C20Leaf", ValidationResult.ValidationType.TYPE_FORMAT, "C20Leaf", path, "", res.getError());
				}
				return success("C20Leaf", ValidationResult.ValidationType.TYPE_FORMAT, "C20Leaf", path, "");
			})
			.collect(toList());
	}

}
