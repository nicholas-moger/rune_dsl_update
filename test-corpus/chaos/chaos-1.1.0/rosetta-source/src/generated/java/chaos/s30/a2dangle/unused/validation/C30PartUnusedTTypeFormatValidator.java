package chaos.s30.a2dangle.unused.validation;

import chaos.s30.a2dangle.unused.C30PartUnusedT;
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

public class C30PartUnusedTTypeFormatValidator implements Validator<C30PartUnusedT> {

	private List<ComparisonResult> getComparisonResults(C30PartUnusedT o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C30PartUnusedT o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C30PartUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C30PartUnusedT", path, "", res.getError());
				}
				return success("C30PartUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C30PartUnusedT", path, "");
			})
			.collect(toList());
	}

}
