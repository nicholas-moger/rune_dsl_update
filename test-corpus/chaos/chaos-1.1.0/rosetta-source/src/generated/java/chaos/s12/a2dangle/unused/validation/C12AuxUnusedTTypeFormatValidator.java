package chaos.s12.a2dangle.unused.validation;

import chaos.s12.a2dangle.unused.C12AuxUnusedT;
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

public class C12AuxUnusedTTypeFormatValidator implements Validator<C12AuxUnusedT> {

	private List<ComparisonResult> getComparisonResults(C12AuxUnusedT o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C12AuxUnusedT o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C12AuxUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C12AuxUnusedT", path, "", res.getError());
				}
				return success("C12AuxUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C12AuxUnusedT", path, "");
			})
			.collect(toList());
	}

}
