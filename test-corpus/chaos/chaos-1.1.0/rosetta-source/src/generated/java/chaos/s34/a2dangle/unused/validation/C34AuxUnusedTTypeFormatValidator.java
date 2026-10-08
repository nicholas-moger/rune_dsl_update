package chaos.s34.a2dangle.unused.validation;

import chaos.s34.a2dangle.unused.C34AuxUnusedT;
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

public class C34AuxUnusedTTypeFormatValidator implements Validator<C34AuxUnusedT> {

	private List<ComparisonResult> getComparisonResults(C34AuxUnusedT o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C34AuxUnusedT o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C34AuxUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C34AuxUnusedT", path, "", res.getError());
				}
				return success("C34AuxUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C34AuxUnusedT", path, "");
			})
			.collect(toList());
	}

}
