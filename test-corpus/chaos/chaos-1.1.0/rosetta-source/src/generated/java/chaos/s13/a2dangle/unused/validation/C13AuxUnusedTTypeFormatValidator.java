package chaos.s13.a2dangle.unused.validation;

import chaos.s13.a2dangle.unused.C13AuxUnusedT;
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

public class C13AuxUnusedTTypeFormatValidator implements Validator<C13AuxUnusedT> {

	private List<ComparisonResult> getComparisonResults(C13AuxUnusedT o) {
		return Lists.<ComparisonResult>newArrayList(
			);
	}

	@Override
	public List<ValidationResult<?>> getValidationResults(RosettaPath path, C13AuxUnusedT o) {
		return getComparisonResults(o)
			.stream()
			.map(res -> {
				if (!isNullOrEmpty(res.getError())) {
					return failure("C13AuxUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C13AuxUnusedT", path, "", res.getError());
				}
				return success("C13AuxUnusedT", ValidationResult.ValidationType.TYPE_FORMAT, "C13AuxUnusedT", path, "");
			})
			.collect(toList());
	}

}
