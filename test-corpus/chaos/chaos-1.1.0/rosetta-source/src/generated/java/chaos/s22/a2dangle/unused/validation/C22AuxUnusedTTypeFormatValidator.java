package chaos.s22.a2dangle.unused.validation;

import chaos.s22.a2dangle.unused.C22AuxUnusedT;
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

public class C22AuxUnusedTTypeFormatValidator implements Validator<C22AuxUnusedT> {

	private List<ComparisonResult> getComparisonResults(C22AuxUnusedT o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C22AuxUnusedT o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C22AuxUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C22AuxUnusedT", path, "", res.getError());
				}
				return success("C22AuxUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C22AuxUnusedT", path, "");
			})
			.collect(toList());
	}

}
