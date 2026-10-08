package chaos.s14.a2dangle.unused.validation;

import chaos.s14.a2dangle.unused.C14AuxUnusedT;
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

public class C14AuxUnusedTTypeFormatValidator implements Validator<C14AuxUnusedT> {

	private List<ComparisonResult> getComparisonResults(C14AuxUnusedT o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C14AuxUnusedT o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C14AuxUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C14AuxUnusedT", path, "", res.getError());
				}
				return success("C14AuxUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C14AuxUnusedT", path, "");
			})
			.collect(toList());
	}

}
